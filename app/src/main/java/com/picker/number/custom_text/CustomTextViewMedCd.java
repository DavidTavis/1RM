package com.picker.number.custom_text;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.util.AttributeSet;

/**
 * Created by TechnoA on 16.05.2017.
 */

public class CustomTextViewMedCd extends android.support.v7.widget.AppCompatTextView {

    public CustomTextViewMedCd(Context context) {
        super(context);
        Typeface face = Typeface.createFromAsset(context.getAssets(), "fonts/HelveticaMedCd.ttf");
        this.setTypeface(face);
    }

    public CustomTextViewMedCd(Context context, AttributeSet attrs) {
        super(context, attrs);
        Typeface face = Typeface.createFromAsset(context.getAssets(), "fonts/HelveticaMedCd.ttf");
        this.setTypeface(face);
    }

    public CustomTextViewMedCd(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Typeface face = Typeface.createFromAsset(context.getAssets(), "fonts/HelveticaMedCd.ttf");
        this.setTypeface(face);
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


    }
}
