package com.picker.number.numberpicker4;

import android.animation.Animator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private AnimationDrawable anim;

    private SharedPreferences pref;

    private FloatingActionMenu fabMenu;
    private FloatingActionButton fabVibration;
    private FloatingActionButton fabVolume;

    private LoopView loopViewHundreds;
    private LoopView loopViewDozens;
    private LoopView loopViewUnits;
    private LoopView loopViewReps;
    private TextView tvWeight;

    private YoYo.YoYoString rope;

    private HashMap<Integer,Float> weightPercentsMap = new HashMap<>();
    private int weightMax = 100;

    private int hundredsCount = 1;
    private int dozensCount = 0;
    private int unitsCount = 0;
    private int repsCount = 1;

    private boolean hundredsSelected = false;
    private boolean dozensSelected = false;
    private boolean unitsSelected = false;
    private boolean repsSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calculate_weight);

        pref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);

        RelativeLayout container = (RelativeLayout) findViewById(R.id.container);

        anim = (AnimationDrawable) container.getBackground();
        anim.setEnterFadeDuration(1000);
        anim.setExitFadeDuration(1000);

        tvWeight = (TextView) findViewById(R.id.tv_weight);
        fabVibration = (FloatingActionButton) findViewById(R.id.fab_vibration);
        fabVolume = (FloatingActionButton) findViewById(R.id.fab_volume);
        fabMenu = (FloatingActionMenu) findViewById(R.id.fab_menu);

        fabVolume.setOnClickListener(this);
        fabVibration.setOnClickListener(this);

        initMap();
        initHundreds();
        initDozens();
        initUnits();
        initReps();

        showWeight(String.valueOf(weightMax));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (anim != null && !anim.isRunning())
            anim.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (anim != null && anim.isRunning())
            anim.stop();
    }

    private void initMap() {
        weightPercentsMap.put(1,100.0F);
        weightPercentsMap.put(2,95.0F);
        weightPercentsMap.put(3,92.5F);
        weightPercentsMap.put(4,90.0F);
        weightPercentsMap.put(5,87.5F);
        weightPercentsMap.put(6,85.0F);
        weightPercentsMap.put(7,82.5F);
        weightPercentsMap.put(8,80.0F);
        weightPercentsMap.put(9,77.5F);
        weightPercentsMap.put(10,75.0F);
        weightPercentsMap.put(11,72.0F);
        weightPercentsMap.put(12,69.36F);
        weightPercentsMap.put(13,66.73F);
        weightPercentsMap.put(14,64.09F);
        weightPercentsMap.put(15,61.45F);
        weightPercentsMap.put(16,58.82F);
        weightPercentsMap.put(17,56.18F);
        weightPercentsMap.put(18,53.55F);
        weightPercentsMap.put(19,50.91F);
        weightPercentsMap.put(20,48.27F);
    }

    private void initReps() {
        loopViewReps = (LoopView) findViewById(R.id.count);
        loopViewReps.setInitPosition(0);
        loopViewReps.setCanLoop(true);
        loopViewReps.setLoopListener(new LoopScrollListener() {
            @Override
            public void onItemSelect(int item) {
                repsSelected = true;
                repsCount = item + 1; // item it's position of array. In our case we started an array with 1
                calculateWeight();
            }
        });
        loopViewReps.setPaddingLeftRight(10);
        loopViewReps.setTextSize(23);//must be called before setDateList
        loopViewReps.setDataList(getListReps());
        loopViewReps.setCanLoop(true);
    }

    private void initUnits() {
        loopViewUnits = (LoopView) findViewById(R.id.units);
        loopViewUnits.setInitPosition(0);
        loopViewUnits.setCanLoop(true);
        loopViewUnits.setLoopListener(new LoopScrollListener() {
            @Override
            public void onItemSelect(int item) {
                unitsSelected = true;
                unitsCount = item;
                calculateWeight();
            }
        });
        loopViewUnits.setPaddingLeftRight(12);
        loopViewUnits.setTextSize(23);//must be called before setDateList
        loopViewUnits.setDataList(getList(0,9));
    }

    private void initDozens() {
        loopViewDozens = (LoopView) findViewById(R.id.dozens);
        loopViewDozens.setInitPosition(0);
        loopViewDozens.setCanLoop(true);
        loopViewDozens.setLoopListener(new LoopScrollListener() {
            @Override
            public void onItemSelect(int item) {
                dozensSelected = true;
                dozensCount = item;
                calculateWeight();
            }
        });
        loopViewDozens.setPaddingLeftRight(12);
        loopViewDozens.setTextSize(23);//must be called before setDateList
        loopViewDozens.setDataList(getList(0,9));
    }

    private void initHundreds() {
        loopViewHundreds = (LoopView) findViewById(R.id.hundreds);
        loopViewHundreds.setInitPosition(1);
        loopViewHundreds.setCanLoop(true);
        loopViewHundreds.setLoopListener(new LoopScrollListener() {
            @Override
            public void onItemSelect(int item) {
                hundredsSelected = true;
                hundredsCount = item;
                calculateWeight();
            }
        });
        loopViewHundreds.setPaddingLeftRight(12);
        loopViewHundreds.setTextSize(23);//must be called before setDateList
        loopViewHundreds.setDataList(getList(0,9));
    }

    private void calculateWeight() {

        int weightHundreds;
        int weightDozens;
        int weightUnits;

        if(hundredsSelected)
            weightHundreds = hundredsCount * 100;
        else
            weightHundreds = 1 * 100;

        if(dozensSelected)
            weightDozens = dozensCount * 10;
        else
            weightDozens = 0;

        if(unitsSelected)
            weightUnits = unitsCount;
        else
            weightUnits = 0;

        weightMax = weightHundreds + weightDozens + weightUnits;
        if(repsSelected){
            float percentByReps = weightPercentsMap.get(repsCount);
            showWeight(String.valueOf(Math.round(weightMax *(percentByReps/100))));
        }else {
            showWeight(String.valueOf(weightMax));
        }
        setAnimation(tvWeight,10);

    }

    private void setAnimation(View view, int itemAnimation) {
        if (rope != null) {
            rope.stop(true);
        }
        Techniques technique = (Techniques) Techniques.values()[itemAnimation];
        rope = YoYo.with(technique)
                .duration(1200)
                .repeat(3)
//                .repeat(YoYo.INFINITE)
                .pivot(YoYo.CENTER_PIVOT, YoYo.CENTER_PIVOT)
                .interpolate(new AccelerateDecelerateInterpolator())
                .withListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                })
                .playOn(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (rope != null) {
                    rope.stop(true);
                }
            }
        });
    }

    private void showWeight(String weight) {
        tvWeight.setText(weight);
    }

    public ArrayList<String> getList(int min, int max){
        ArrayList<String> list = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            list.add("" + i);
        }
        return list;
    }

    public ArrayList<String> getListReps() {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            if(i<10){
                list.add("0" + i);
            }else{
                list.add("" + i);
            }

        }
        return list;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case  R.id.fab_vibration :
                fabMenu.close(true);
                if(pref.getBoolean("vibration",true)){
                    pref.edit().putBoolean("vibration",false).commit();
                    loopViewReps.setCanVibrate(false);
                    loopViewUnits.setCanVibrate(false);
                    loopViewDozens.setCanVibrate(false);
                    loopViewHundreds.setCanVibrate(false);
                }else{
                    pref.edit().putBoolean("vibration",true).commit();
                    loopViewReps.setCanVibrate(true);
                    loopViewUnits.setCanVibrate(true);
                    loopViewDozens.setCanVibrate(true);
                    loopViewHundreds.setCanVibrate(true);
                }
                break;
            case R.id.fab_volume :
                fabMenu.close(true);
                if(pref.getBoolean("volume",true)){
                    pref.edit().putBoolean("volume",false).commit();
                    loopViewReps.setCanVolume(false);
                    loopViewUnits.setCanVolume(false);
                    loopViewDozens.setCanVolume(false);
                    loopViewHundreds.setCanVolume(false);
                }else{
                    pref.edit().putBoolean("volume",true).commit();
                    loopViewReps.setCanVolume(true);
                    loopViewUnits.setCanVolume(true);
                    loopViewDozens.setCanVolume(true);
                    loopViewHundreds.setCanVolume(true);
                }
                break;
            default:
                break;
        }

    }
}
