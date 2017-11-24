package com.picker.number;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;

import com.picker.number.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.content.Context.VIBRATOR_SERVICE;

public class LoopView extends View {

    private static final String TAG = "MyTAG";
    public static final int MSG_INVALIDATE = 1000;
    public static final int MSG_SCROLL_LOOP = 2000;
    public static final int MSG_SELECTED_ITEM = 3000;

    private Context mContext;

    private AudioManager audioManager;
    private Vibrator vibrator;

    private int changingLeftY;
    private int translateY;
    private int mDrawnCenterY;
    private int mWheelCenterY;
    private int alpha;
    private int mHalfDrawnItemCount;
    private int mSelectedItem;
    private int changeItem;
    private int mTotalScrollY;
    private int mTextSize;
    private int mMaxTextWidth;
    private int mMaxTextHeight;
    private int mTopBottomTextColor;
    private int mCenterTextColor;
    private int mCenterLineColor;
    private int mTopLineY;
    private int mBottomLineY;
    private int mCurrentIndex;
    private int mInitPosition;
    private int mPaddingLeftRight;
    private int mPaddingTopBottom;
    private int mDrawItemsCount;
    private int mCircularDiameter;
    private int mCircularRadius;
    private int mWidgetHeight;
    private int mWidgetWidth;

    private float mItemHeight;
    private float mDrawnItemCenterY;
    private float lineSpacingMultiplier;

    private boolean canVibrate;
    private boolean canVolume;
    private boolean hasAtmospheric = true;
    private boolean mCanLoop;

    private ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mScheduledFuture;
    private LoopScrollListener mLoopListener;
    private GestureDetector mGestureDetector;
    private GestureDetector.SimpleOnGestureListener mOnGestureListener;

    private Rect mRectDrawn;
    private Paint mTopBottomTextPaint;  //paint that draw top and bottom text
    private Paint mCenterTextPaint;  // paint that draw center text
    private Paint mCenterLinePaint;  // paint that draw line besides center text
    private Typeface typeface;
    private ArrayList mDataList;

    public Handler mHandler;

    public LoopView(Context context) {
        this(context, null);
    }

    public LoopView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoopView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context,attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LoopView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context,attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        this.mContext = context;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.LoopView);
        if (array != null) {
            mTopBottomTextColor = array.getColor(R.styleable.LoopView_topBottomTextColor, 0xffefefef);
            mCenterTextColor = array.getColor(R.styleable.LoopView_centerTextColor, 0xffdfdfdf);
            mCenterLineColor = array.getColor(R.styleable.LoopView_lineColor, 0xff3d3d3d);
            mCanLoop = array.getBoolean(R.styleable.LoopView_canLoop, true);
            mInitPosition = array.getInt(R.styleable.LoopView_initPosition, -1);
            mTextSize = array.getDimensionPixelSize(R.styleable.LoopView_textSize, sp2px(context, 16));
            mDrawItemsCount = array.getInt(R.styleable.LoopView_drawItemCount, 11);
            array.recycle();
        }

        //This variable is responsible for the distance between items
        lineSpacingMultiplier = 1.7F;

        mRectDrawn = new Rect();

        mTopBottomTextPaint = new Paint();
        mCenterTextPaint = new Paint();
        mCenterLinePaint = new Paint();

        typeface = Typeface.createFromAsset(mContext.getAssets(), "fonts/Helvetica-Roman-SemiB.ttf");

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        mOnGestureListener = new LoopViewGestureListener();
        mGestureDetector = new GestureDetector(context, mOnGestureListener);
        mGestureDetector.setIsLongpressEnabled(false);

        vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

    }

    private void initData() {

        if (mDataList == null) {
            throw new IllegalArgumentException("data list must not be null!");
        }

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == MSG_INVALIDATE)
                    invalidate();
                if (msg.what == MSG_SCROLL_LOOP)
                    startSmoothScrollTo(true);
                else if (msg.what == MSG_SELECTED_ITEM)
                    itemSelected();
                return false;
            }
        });

        mHalfDrawnItemCount = mDrawItemsCount / 2; //The count of top and bottom items

        mTopBottomTextPaint.setColor(mTopBottomTextColor);
        mTopBottomTextPaint.setAntiAlias(true);
        mTopBottomTextPaint.setTypeface(typeface);
        mTopBottomTextPaint.setTextSize(mTextSize);

        mCenterTextPaint.setColor(mCenterTextColor);
        mCenterTextPaint.setAntiAlias(true);
        mCenterTextPaint.setTextScaleX(1.25F);
        mCenterTextPaint.setTypeface(typeface);
        mCenterTextPaint.setTextSize(mTextSize);

        mCenterLinePaint.setColor(mCenterLineColor);
        mCenterLinePaint.setAntiAlias(true);
        mCenterLinePaint.setTypeface(Typeface.MONOSPACE);
        mCenterLinePaint.setTextSize(mTextSize);

        measureTextWidthHeight();

        // the length of circle
        int mCircumference = (int) ((mMaxTextHeight * lineSpacingMultiplier * (mDrawItemsCount - 1)))*2;
        //the diameter of circle 2πr = cir
        mCircularDiameter = (int) (mCircumference / Math.PI);
        //the radius of circular
        mCircularRadius = (int) ((mCircumference/2) / Math.PI);

        if (mInitPosition == -1) {
            if (mCanLoop) {
                mInitPosition = (mDataList.size() + 1) / 2;
            } else {
                mInitPosition = 0;
            }
        }
        mCurrentIndex = mInitPosition;
        invalidate();
    }

    private void measureTextWidthHeight() {

        for (int i = 0; i < mDataList.size(); i++) {
            String s1 = (String) mDataList.get(i);
            mCenterTextPaint.getTextBounds(s1, 0, s1.length(), mRectDrawn);
            int textWidth = mRectDrawn.width();
            if (textWidth > mMaxTextWidth) {
                mMaxTextWidth = textWidth;
            }
            int textHeight = mRectDrawn.height();
            if (textHeight > mMaxTextHeight) {
                mMaxTextHeight = textHeight;
            }
        }

    }

     private void computeDrawnCenter() {
        mDrawnCenterY = (int) (mWheelCenterY - ((mCenterTextPaint.ascent() + mCenterTextPaint.descent()) / 2));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        mRectDrawn.set(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());

        mWheelCenterY = mRectDrawn.centerY();
        // Correct item drawn center
        computeDrawnCenter();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mWidgetWidth = getMeasuredWidth();
        mWidgetHeight = MeasureSpec.getSize(heightMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        mItemHeight = lineSpacingMultiplier * mMaxTextHeight;

        //auto calculate the text's left/right value when draw
//        mPaddingLeftRight = (mWidgetWidth - mMaxTextWidth) / 2;
        mPaddingTopBottom = (mWidgetHeight - mCircularDiameter) / 2;

        //topLineY = diameter/2 - itemHeight(mItemHeight)/2 + mPaddingTopBottom
        mTopLineY = (int) ((mCircularDiameter - mItemHeight) / 2.0F) + mPaddingTopBottom;
        mBottomLineY = (int) ((mCircularDiameter + mItemHeight) / 2.0F) + mPaddingTopBottom;
    }

    private void vibrationReaction() {
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) mContext.getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(5,2));
        } else {
            ((Vibrator) mContext.getSystemService(VIBRATOR_SERVICE)).vibrate(5);
        }
    }

    private void soundReaction() {

        audioManager.playSoundEffect(SoundEffectConstants.CLICK, 0.6F);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDataList == null) {
            super.onDraw(canvas);
            return;
        }

        super.onDraw(canvas);

        //the length of single item is mItemHeight
        int mChangingItem = (int) (mTotalScrollY / (mItemHeight));

        if(changeItem != mChangingItem){
            changeItem = mChangingItem;
            if(canVibrate){
                vibrationReaction();
            }
            if(canVolume){
                soundReaction();
            }

        }

        mCurrentIndex = mInitPosition + mChangingItem % mDataList.size();
        if (!mCanLoop) { // can loop
            if (mCurrentIndex < 0) {
                mCurrentIndex = 0;
            }
            if (mCurrentIndex > mDataList.size() - 1) {
                mCurrentIndex = mDataList.size() - 1;
            }
        } else { //can not loop
            if (mCurrentIndex < 0) {
                mCurrentIndex = mDataList.size() + mCurrentIndex;
            }
            if (mCurrentIndex > mDataList.size() - 1) {
                mCurrentIndex = mCurrentIndex - mDataList.size();
            }
        }

        int count = 0;
        String itemCount[] = new String[mDrawItemsCount];
        //reconfirm each item's value from dataList according to currentIndex,
        while (count < mDrawItemsCount) {
            int templateItem = mCurrentIndex - (mDrawItemsCount / 2 - count);
            if (mCanLoop) {
                if (templateItem < 0) {
                    templateItem = templateItem + mDataList.size();
                }
                if (templateItem > mDataList.size() - 1) {
                    templateItem = templateItem - mDataList.size();
                }
                itemCount[count] = (String) mDataList.get(templateItem);
            } else if (templateItem < 0) {
                itemCount[count] = "";
            } else if (templateItem > mDataList.size() - 1) {
                itemCount[count] = "";
            } else {
                itemCount[count] = (String) mDataList.get(templateItem);
            }
            count++;
        }

        //draw top line
        canvas.drawLine(0.0F, mTopLineY, mWidgetWidth, mTopLineY, mCenterLinePaint);
        canvas.drawLine(0.0F, mTopLineY + 1.0F, mWidgetWidth, mTopLineY + 1.0F, mCenterLinePaint);
        //draw bottom line
        canvas.drawLine(0.0F, mBottomLineY, mWidgetWidth, mBottomLineY, mCenterLinePaint);
        canvas.drawLine(0.0F, mBottomLineY + 1.0F, mWidgetWidth, mBottomLineY + 1.0F, mCenterLinePaint);

        int drawnOffsetPos = -mHalfDrawnItemCount;

        double radian;
        float angle;
        count = 0;
        changingLeftY = (int) (mTotalScrollY % (mItemHeight));

        while (count < mDrawItemsCount) {
            canvas.save();

            mDrawnItemCenterY = mDrawnCenterY + (drawnOffsetPos * mItemHeight) - mTotalScrollY % mItemHeight;
            drawnOffsetPos++;
            radian = (mItemHeight * count - changingLeftY) / mCircularRadius;
            //get angle
            angle = (float) (radian * 180 / Math.PI);

            //when angle >= 180 || angle <= 0 don't draw
            if (angle >= 180F || angle <= 0F) {
                canvas.restore();

            } else {
                // translateY = r - r*cos(å) -
                //(Math.sin(radian) * mMaxTextHeight) / 2 this is text offset
                translateY = (int) (mCircularRadius - Math.cos(radian) * mCircularRadius - (Math.sin(radian) * mMaxTextHeight) / 2) + mPaddingTopBottom;
                if (hasAtmospheric) {
                    alpha = (int) ((mDrawnCenterY - Math.abs(mDrawnCenterY - mDrawnItemCenterY )) * 1.0F / mDrawnCenterY * 255);

                    alpha = alpha <= 0 ? 0 : alpha;
                    alpha = alpha + 50;
                    alpha = Math.round(alpha*0.5F);

                    mTopBottomTextPaint.setAlpha(alpha);
                }

                canvas.translate(0.0F, translateY);
                //scale offset = Math.sin(radian) -> 0 - 1
                canvas.scale(1.0F, (float) Math.sin(radian));
                if (translateY <= mTopLineY) {
                    //draw text y between 0 -> mTopLineY,include incomplete text
                    //it responses for top part
                    canvas.save();
                    canvas.clipRect(0, 0, mWidgetWidth, mTopLineY - translateY);
                    canvas.drawText(itemCount[count], mPaddingLeftRight, mMaxTextHeight, mTopBottomTextPaint);
                    canvas.restore();
                    //it responses for top central part in moving
                    if (translateY + mMaxTextHeight >= mTopLineY) {
                        canvas.save();
                        canvas.clipRect(0, mTopLineY - translateY, mWidgetWidth, (int) (mItemHeight));
                        canvas.drawText(itemCount[count], mPaddingLeftRight, mMaxTextHeight, mCenterTextPaint);
                        canvas.restore();
                    }
                } else if (mMaxTextHeight + translateY >= mBottomLineY) {
                    //draw text y between  mTopLineY -> mBottomLineY ,include incomplete text
                    //it responses for bottom central part in moving
                    if(translateY<=mBottomLineY) {
                        canvas.save();
                        canvas.clipRect(0, 0, mWidgetWidth, mBottomLineY - translateY);
                        canvas.drawText(itemCount[count], mPaddingLeftRight, mMaxTextHeight, mCenterTextPaint);
                        canvas.restore();
                    }
                    //it responses for bottom part
                    canvas.save();
                    canvas.clipRect(0, mBottomLineY - translateY, mWidgetWidth, (int) (mItemHeight));
                    canvas.drawText(itemCount[count], mPaddingLeftRight, mMaxTextHeight, mTopBottomTextPaint);
                    canvas.restore();
                } else if (translateY >= mTopLineY && mMaxTextHeight + translateY <= mBottomLineY) {
                    //draw center complete text
                    canvas.clipRect(0, 0, mWidgetWidth, (int) (mItemHeight));
                    canvas.drawText(itemCount[count], mPaddingLeftRight, mMaxTextHeight, mCenterTextPaint);
                    //center one indicate selected item
                    mSelectedItem = mDataList.indexOf(itemCount[count]);
                }
                canvas.restore();
            }
            count++;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionevent) {
        switch (motionevent.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mGestureDetector.onTouchEvent(motionevent)) {
                    startSmoothScrollTo(true);

                }
                break;
            default:
                if (!mGestureDetector.onTouchEvent(motionevent)) {
                    Log.d(TAG,"onTouchEvent default motionevent = " + motionevent.toString());;
                    startSmoothScrollTo(false);
                }
        }
        return true;
    }

    public final void setCanLoop(boolean canLoop) {
        mCanLoop = canLoop;
        invalidate();
    }

    public void setCanVibrate(boolean vibration){
        this.canVibrate = vibration;
    }

    public void setCanVolume(boolean vol){
        this.canVolume = vol;
    }

    /**
     * set text size
     *
     * @param size size indicate sp,not px
     */
    public final void setTextSize(float size) {
        if (size > 0) {
            mTextSize = sp2px(mContext, size);
        }
    }

    public final void setPaddingLeftRight(int padding){
        this.mPaddingLeftRight = padding;
    }

    public void setInitPosition(int initPosition) {
        this.mInitPosition = initPosition;
        invalidate();
    }

    public void setLoopListener(LoopScrollListener LoopListener) {
        mLoopListener = LoopListener;
    }

    /**
     * All public method must be called before this method
     * @param list data list
     */
    public final void setDataList(List<String> list) {
        this.mDataList = (ArrayList) list;
        initData();
    }

    public int getSelectedItem() {
        return mSelectedItem;
    }

    private void itemSelected() {
        if (mLoopListener != null) {
            postDelayed(new SelectedRunnable(), 200L);
        }
    }

    private void cancelSchedule() {

        if (mScheduledFuture != null && !mScheduledFuture.isCancelled()) {
            mScheduledFuture.cancel(true);
            mScheduledFuture = null;
        }
    }

    private void startSmoothScrollTo(Boolean properlyPlaced) {
        Log.d(TAG,"startSmoothScrollTo(Boolean properlyPlaced)");
        int offset = (int) (mTotalScrollY % (mItemHeight));
        cancelSchedule();
        //Third parameter response about speed of returning value on its properly place (center between top and bottom line)
        mScheduledFuture = mExecutor.scheduleWithFixedDelay(new HalfHeightRunnable(offset,properlyPlaced), 0, 20, TimeUnit.MILLISECONDS);
    }

    private void startSmoothScrollTo(float velocityY) {
        Log.d(TAG,"startSmoothScrollTo(float velocityY)");
        cancelSchedule();
        int velocityFling = 20;
        mScheduledFuture = mExecutor.scheduleWithFixedDelay(new FlingRunnable(velocityY), 0, velocityFling, TimeUnit.MILLISECONDS);
    }

    class LoopViewGestureListener extends android.view.GestureDetector.SimpleOnGestureListener {

        public LoopViewGestureListener() {
            super();

        }

        @Override
        public final boolean onDown(MotionEvent motionevent) {
            cancelSchedule();
            return true;
        }

        @Override
        public final boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG,"LoopViewGestureListener onFling");
            startSmoothScrollTo(velocityY);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d(TAG,"LoopViewGestureListener onScroll");
            mTotalScrollY = (int) ((float) mTotalScrollY + distanceY);
            if (!mCanLoop) {
                int initPositionCircleLength = (int) (mInitPosition * (mItemHeight));
                int initPositionStartY = -1 * initPositionCircleLength;
                if (mTotalScrollY < initPositionStartY) {
                    mTotalScrollY = initPositionStartY;
                }

                int circleLength = (int) ((float) (mDataList.size() - 1 - mInitPosition) * (mItemHeight));
                if (mTotalScrollY >= circleLength) {
                    mTotalScrollY = circleLength;
                }
            }
            invalidate();
            return true;
        }
    }

    public int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    class SelectedRunnable implements Runnable {

        @Override
        public final void run() {
            LoopScrollListener listener = LoopView.this.mLoopListener;
            int selectedItem = getSelectedItem();
            mDataList.get(selectedItem);
            listener.onItemSelect(selectedItem);
        }
    }

    /**
     * Use in ACTION_UP
     */
    class HalfHeightRunnable implements Runnable {

        int realTotalOffset;
        int realOffset;
        int offset;
        boolean properlyPlaced;

        public HalfHeightRunnable(int offset, boolean properlyPlaced) {
            Log.d(TAG,"HalfHeightRunnable");
            this.offset = offset;
            realTotalOffset = Integer.MAX_VALUE;
            realOffset = 0;
            this.properlyPlaced = properlyPlaced;
        }

        @Override
        public void run() {
            Log.d(TAG,"run()");
            //first in
            if (realTotalOffset == Integer.MAX_VALUE) {
                if ((float) offset > mItemHeight / 2.0F) {
                    //move to next item
                    realTotalOffset = (int) (mItemHeight - (float) offset);
                } else {
                    //move to pre item
                    realTotalOffset = -offset;
                }
            }

            realOffset = (int) ((float) realTotalOffset * 0.1F);

            if (realOffset == 0) {

                if (realTotalOffset < 0) {
                    realOffset = -1;
                } else {
                    realOffset = 1;
                }
            }
            if (Math.abs(realTotalOffset) <= 0) {
                cancelSchedule();
                mHandler.sendEmptyMessage(MSG_SELECTED_ITEM);
                return;
            } else {
                if(properlyPlaced) {
                    mTotalScrollY = mTotalScrollY + realOffset;
                    mHandler.sendEmptyMessage(MSG_INVALIDATE);

                    realTotalOffset = realTotalOffset - realOffset;
                }
                return;
            }
        }
    }

    /**
     * Use in {@link LoopViewGestureListener#onFling(MotionEvent, MotionEvent, float, float)}
     */
    class FlingRunnable implements Runnable {

        float velocity;
        final float velocityY;

        FlingRunnable(float velocityY) {
            this.velocityY = velocityY;
            velocity = Integer.MAX_VALUE;
        }

        @Override
        public void run() {

            if (velocity == Integer.MAX_VALUE) {
                if (Math.abs(velocityY) > 2400F) {
                    if (velocityY > 0.0F) {
                        velocity = 2400F;
                    } else {
                        velocity = -2400F;
                    }
                } else {
                    velocity = velocityY;
                }
            }
            if (Math.abs(velocity) >= 0.0F && Math.abs(velocity) <= 15F) {
                cancelSchedule();
                mHandler.sendEmptyMessage(MSG_SCROLL_LOOP);
                return;
            }
            int i = (int) ((velocity * 10F) / 1000F);

            mTotalScrollY = mTotalScrollY - i;

            if (!mCanLoop) {
                float itemHeight = lineSpacingMultiplier * mMaxTextHeight;

                if (mTotalScrollY <= (int) ((float) (-mInitPosition) * itemHeight)) {
                    velocity = 40F;
                    mTotalScrollY = (int) ((float) (-mInitPosition) * itemHeight);
                } else if (mTotalScrollY >= (int) ((float) (mDataList.size() - 1 - mInitPosition) * itemHeight)) {
                    mTotalScrollY = (int) ((float) (mDataList.size() - 1 - mInitPosition) * itemHeight);
                    velocity = -40F;
                }
            }
            if (velocity < 0.0F) {
                velocity = velocity + 20F;
            } else {
                velocity = velocity - 20F;
            }
            mHandler.sendEmptyMessage(MSG_INVALIDATE);
        }
    }
}
