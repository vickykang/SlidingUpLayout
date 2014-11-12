package com.readboy.slidinguppanel.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.readboy.slidinguppanel.R;

/**
 * Created by kwd on 2014/11/8.
 */
public class SlidingUpPanel extends LinearLayout implements View.OnTouchListener {

    public static final String TAG = SlidingUpPanel.class.getSimpleName();

    protected static final int DEFAULT_MIN_TOP = 20;

    private int minTop;

    private Button draggerBtn;

    protected int draggerBtnHeight;
    protected int lastY;
    private int originalTop;

    boolean isBeingDragged;

    public SlidingUpPanel(Context context) {
        this(context, null, 0);
    }

    public SlidingUpPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingUpPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SlidingUpPanel, 0, 0);

        try {
            minTop = a.getInt(R.styleable.SlidingUpPanel_minTop, DEFAULT_MIN_TOP);
        } finally {
            a.recycle();
        }

        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_sliding_up_panel, this);
        setOrientation(VERTICAL);

        draggerBtn = (Button) findViewById(R.id.btn_dragger);
        draggerBtn.setOnTouchListener(this);

        draggerBtn.measure(View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        draggerBtnHeight = draggerBtn.getMeasuredHeight();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isBeingDragged = false;
                originalTop = getTop();
                lastY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                isBeingDragged = true;
                int dy = (int) event.getRawY() - lastY;
                slide(dy);
                lastY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
                isBeingDragged = false;
                break;
        }

        invalidate();

        return false;
    }

    private void slide(int dy) {
        originalTop += dy;

        if (originalTop < minTop) {
            originalTop = minTop;
        }

        if (originalTop > getBottom() - draggerBtnHeight) {
            originalTop = getBottom() - draggerBtnHeight;
        }

        measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getBottom() - originalTop, MeasureSpec.EXACTLY));

        layout(getLeft(), originalTop, getRight(), getBottom());
    }

    public int getMinTop() {
        return minTop;
    }

    public void setMinTop(int top) {
        minTop = top > 0 ? top : 0;
    }
}
