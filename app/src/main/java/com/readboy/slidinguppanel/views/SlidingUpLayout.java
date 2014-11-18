package com.readboy.slidinguppanel.views;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

import com.readboy.slidinguppanel.R;

/**
 * Created by kwd on 2014/11/17.
 */
public class SlidingUpLayout extends ViewGroup implements View.OnTouchListener {

    public static final String TAG = SlidingUpLayout.class.getSimpleName();

    protected static final int DEFAULT_DRAGGER_RESOURCE = R.drawable.selector_btn_dragger;
    protected static final int DEFAULT_ANIMATOR_DURATION = 250;
    protected static final float DEFAULT_DIFFERENCE = 50.0f;

    protected static final int MAX_CHILD_COUNT = 3;

    protected View mUpperView;
    protected View mSlideView;

    Button mDraggerBtn;
    Drawable mDraggerDrawable;
    int mDraggerResource;
    int mDraggerWidth;
    int mDraggerHeight;

    int mMaxHeight;
    int mMinHeight;

    private int mTotalLength;

    private int mGravity = Gravity.START | Gravity.TOP;

    private boolean isBeingDragged = false;
    protected int lastY;
    private int originalTop;

    protected boolean hasDragger = false;

    public SlidingUpLayout(Context context) {
        this(context, null);
    }

    public SlidingUpLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingUpLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlidingUpLayout, defStyleAttr, 0);
        mDraggerDrawable = a.getDrawable(R.styleable.SlidingUpLayout_dragger);
        if (mDraggerDrawable == null) mDraggerDrawable = getResources().getDrawable(DEFAULT_DRAGGER_RESOURCE);

        mMaxHeight = a.getDimensionPixelSize(R.styleable.SlidingUpLayout_maxHeight, -1);
        mMinHeight = a.getDimensionPixelSize(R.styleable.SlidingUpLayout_minHeight, 0);
        mDraggerWidth = a.getDimensionPixelSize(R.styleable.SlidingUpLayout_draggerWidth, -1);
        mDraggerHeight = a.getDimensionPixelSize(R.styleable.SlidingUpLayout_draggerHeight, -1);

        int index = a.getInt(R.styleable.SlidingUpLayout_gravity, -1);
        if (index >= 0) {
            setGravity(index);
        }

        a.recycle();
        init();
    }

    protected void init() {
        /**
         * Initialize dragger button.
         */
        mDraggerBtn = new Button(getContext());
        mDraggerBtn.setBackground(mDraggerDrawable);
        measureDragger();
        mDraggerBtn.setOnTouchListener(this);

        if (mMaxHeight != -1 && mMaxHeight < mMinHeight) mMaxHeight = mMinHeight;
    }

    int dy;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int deltaY = 0;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isBeingDragged = false;
                originalTop = v.getTop();
                lastY = (int) event.getRawY();
                deltaY = originalTop;
                break;

            case MotionEvent.ACTION_MOVE:
                isBeingDragged =  true;
                dy = ((int) event.getRawY()) - lastY;
                slideUp(dy);
                lastY = (int) event.getRawY();
                break;

            case MotionEvent.ACTION_UP:
                isBeingDragged = false;
                playAnimation(deltaY - v.getTop());
                break;
            default:
                isBeingDragged = false;
                break;
        }

        return false;
    }

    /**
     * Re-calculate the top position relative to its parent
     * and its height, then call {@link #requestLayout()}.
     *
     * @param dy difference in y-axis
     */
    void slideUp(int dy) {
        originalTop += dy;

        // originalTop 最大值
        if (originalTop > mSlideView.getBottom() - mMinHeight - mDraggerHeight)
            originalTop = mSlideView.getBottom() - mMinHeight - mDraggerHeight;

        if (mMaxHeight != -1 && originalTop < mSlideView.getBottom() - mMaxHeight - mDraggerHeight)
            originalTop = mSlideView.getBottom() - mMaxHeight - mDraggerHeight;

        // originalTop 最小值
        if (originalTop < 0) originalTop = 0;

        LayoutParams slideLp = (LayoutParams) mSlideView.getLayoutParams();
        slideLp.height = mSlideView.getBottom() - originalTop - mDraggerHeight;

        if (mUpperView != null)
            mDraggerBtn.setTop(originalTop);
        else
            mSlideView.setTop(originalTop + mDraggerHeight);

        requestLayout();
    }

    /**
     * 动画效果
     */
    private void playAnimation(int dy) {
        if (dy != 0) {
            final float diff = dy / Math.abs(dy) * DEFAULT_DIFFERENCE;

            ObjectAnimator previousDraggerAnim = ObjectAnimator.ofFloat(mDraggerBtn, "translationY", 0f, diff).
                    setDuration(DEFAULT_ANIMATOR_DURATION);
            previousDraggerAnim.setInterpolator(new OvershootInterpolator(1.2f));

            ObjectAnimator posteriorDraggerAnim = ObjectAnimator.ofFloat(mDraggerBtn, "translationY", diff, 0).
                    setDuration(DEFAULT_ANIMATOR_DURATION);
            posteriorDraggerAnim.setInterpolator(new OvershootInterpolator(1.2f));

            ObjectAnimator previousSlideAnim = ObjectAnimator.ofFloat(mSlideView, "translationY", 0f, diff).
                    setDuration(DEFAULT_ANIMATOR_DURATION);
            previousSlideAnim.setInterpolator(new OvershootInterpolator(1.2f));

            ObjectAnimator posteriorSlideAnim = ObjectAnimator.ofFloat(mSlideView, "translationY", diff, 0).
                    setDuration(DEFAULT_ANIMATOR_DURATION);
            posteriorSlideAnim.setInterpolator(new OvershootInterpolator(1.2f));

            AnimatorSet draggerSet = new AnimatorSet();
            draggerSet.play(previousDraggerAnim).before(posteriorDraggerAnim);

            AnimatorSet slideSet = new AnimatorSet();
            slideSet.play(previousSlideAnim).before(posteriorSlideAnim);

            draggerSet.start();
            slideSet.start();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (!hasDragger && getChildCount() > 0) {
            hasDragger = true;
            mDraggerBtn.setId(getChildCount() - 1);
            addView(mDraggerBtn, getChildCount() - 1);

            mSlideView = getChildAt(getChildCount() - 1);
            if (getChildCount() == MAX_CHILD_COUNT)
                mUpperView = getChildAt(0);
        }

        measureVertical(widthMeasureSpec, heightMeasureSpec);
    }

    void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        mTotalLength = 0;
        int maxWidth = 0;
        int childState = 0;
        int alternativeMaxWidth = 0;
        int weightedMaxWidth = 0;
        boolean allFillParent = true;
        float totalWeight = 0;

        final int count = getChildCount();

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        boolean matchWidth = false;
        boolean skippedMeasure = false;

        for (int i = 0; i < count; i++) {
            if (i == mDraggerBtn.getId() && mUpperView != null)
                continue;

            final View child = getChildAt(i);

            if (child == null) {
                mTotalLength += measureNullChild(i);
                continue;
            }

            if (child.getVisibility() == View.GONE) {
                i += getChildrenSkippedCount(child, i);
                continue;
            }

            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            totalWeight += lp.weight;

            if (heightMode == MeasureSpec.EXACTLY && lp.height == 0 && lp.weight > 0) {
                // Optimization: don't bother measuring children who are going to use
                // leftover space. These views will get measured again down below if
                // there is any leftover space.
                final int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + lp.topMargin + lp.bottomMargin);
                skippedMeasure = true;
            } else {
                int oldHeight = Integer.MIN_VALUE;

                if (lp.height == 0 && lp.weight > 0) {
                    // heightMode is either UNSPECIFIED or AT_MOST, and this
                    // child wanted to stretch to fill available space.
                    // Translate that to WRAP_CONTENT so that is does not end up
                    // with a height of 0.
                    oldHeight = 0;
                    lp.height = LayoutParams.WRAP_CONTENT;
                }


                // Determine how big this child would like to be. If this or
                // previous children have given a weight, then we allow it to
                // use all available space (and we will shrink things later
                // if needed). The dragger button has already been measured.
                if (i != mDraggerBtn.getId()) {
                    measureChildBeforeLayout(
                            child, i, widthMeasureSpec, 0, heightMeasureSpec,
                            totalWeight == 0 ? mTotalLength : 0);
                }

                if (oldHeight != Integer.MIN_VALUE) {
                    lp.height = oldHeight;
                }

                final int childHeight = child.getMeasuredHeight();
                final int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + childHeight + lp.topMargin +
                        lp.bottomMargin + getNextLocationOffset(child));

            }

            boolean matchWidthLocally = false;
            if (widthMeasureSpec != MeasureSpec.EXACTLY && lp.width == LayoutParams.MATCH_PARENT) {
                // The width of the sliding up layout will scale, and at least one
                // child said it wanted to match our width. Set a flat
                // indicating that we need to remeasure at least that view when
                // we know our width.
                matchWidth = true;
                matchWidthLocally = true;
            }

            final int margin = lp.leftMargin + lp.rightMargin;
            final int measuredWidth = child.getMeasuredWidth() + margin;
            maxWidth = Math.max(maxWidth, measuredWidth);
            childState = combineMeasuredStates(childState, child.getMeasuredState());

            allFillParent = allFillParent && lp.width == LayoutParams.MATCH_PARENT;
            if (lp.weight > 0) {
                /**
                 * Width of weighted Views are bogus if we end up
                 * remeasuring, so keep them separate.
                 */
                weightedMaxWidth = Math.max(weightedMaxWidth,
                        matchWidthLocally ? margin : measuredWidth);
            } else {
                alternativeMaxWidth = Math.max(alternativeMaxWidth,
                        matchWidthLocally ? margin : measuredWidth);
            }

            i += getChildrenSkippedCount(child, i);
        }

        // Add in our padding
        mTotalLength += getPaddingTop() + getPaddingBottom();

        int heightSize = mTotalLength;

        // Check against our minimum height
        heightSize = Math.max(heightSize, getSuggestedMinimumHeight());

        // Reconcile our calculated size with the heightMeasureSpec
        int heightSizeAndState = resolveSizeAndState(heightSize, heightMeasureSpec, 0);
        heightSize = heightSizeAndState & MEASURED_SIZE_MASK;

        // Either expand children with weight to take up available space or
        // shrink them if they extend beyond our current bounds. If we skipped
        // measurement on any children, we need to measure them now.
        int delta = heightSize - mTotalLength;
        if (skippedMeasure || delta != 0 && totalWeight > 0.0f) {
            float weightSum = totalWeight;

            mTotalLength = 0;

            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);

                if (child.getVisibility() == View.GONE) {
                    continue;
                }

                LayoutParams lp = (LayoutParams) child.getLayoutParams();

                float childExtra = lp.weight;
                if (childExtra > 0) {
                    // Child said it could absorb extra space -- give him his share
                    int share = (int) (childExtra * delta / weightSum);
                    weightSum -= childExtra;
                    delta -= share;
                    final int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            getPaddingLeft() + getPaddingRight() +
                                    lp.leftMargin + lp.rightMargin, lp.width);

                    // TODO: Use a field like lp.isMeasured to figure out if this
                    // child has been previously measured
                    if ((lp.height != 0) || (heightMode != MeasureSpec.EXACTLY)) {
                        // child was measured once already above...
                        // base new measurement on stored values
                        int childHeight = child.getMeasuredHeight() + share;
                        if (childHeight < 0) {
                            childHeight = 0;
                        }

                        child.measure(childWidthMeasureSpec,
                                MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
                    } else {
                        // child was skipped in the loop above.
                        // Measure for this first time here
                        child.measure(childWidthMeasureSpec,
                                MeasureSpec.makeMeasureSpec(share > 0 ? share : 0,
                                        MeasureSpec.EXACTLY));
                    }

                    childState = combineMeasuredStates(childState, child.getMeasuredState()
                            & (MEASURED_STATE_MASK >> MEASURED_HEIGHT_STATE_SHIFT));
                }

                final int margin = lp.leftMargin + lp.rightMargin;
                final int measuredWidth = child.getMeasuredWidth() + margin;
                maxWidth = Math.max(maxWidth, measuredWidth);

                boolean matchWidthLocally = widthMode != MeasureSpec.EXACTLY &&
                        lp.width == LayoutParams.MATCH_PARENT;

                alternativeMaxWidth = Math.max(alternativeMaxWidth,
                        matchWidthLocally ? margin : measuredWidth);

                allFillParent = allFillParent && lp.width == LayoutParams.MATCH_PARENT;

                final int totalLength = mTotalLength;
                mTotalLength = Math.max(totalLength, totalLength + child.getMeasuredHeight() +
                        lp.topMargin + lp.bottomMargin + getNextLocationOffset(child));

            }

            // Add in our padding
            mTotalLength += getPaddingTop() + getPaddingBottom();
        } else {
            alternativeMaxWidth = Math.max(alternativeMaxWidth, weightedMaxWidth);
        }

        if (!allFillParent && widthMode != MeasureSpec.EXACTLY) {
            maxWidth = alternativeMaxWidth;
        }

        maxWidth += getPaddingLeft() + getPaddingRight();

        // Check against our minimum width
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                heightSizeAndState);

        if (matchWidth) {
            forceUniformWidth(count, heightMeasureSpec);
        }
    }

    private void forceUniformWidth(int count, int heightMeasuredSpec) {
        // Pretent that the linear layout has an exact size.
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(),
                MeasureSpec.EXACTLY);
        for (int i = 0; i < count ; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = ((LayoutParams) child.getLayoutParams());

                if (lp.width == LayoutParams.MATCH_PARENT) {
                    // Temporarily force children to reuse their old measured height
                    int oldHeight = lp.height;
                    lp.height = child.getMeasuredHeight();

                    // Remeasure with new dimensions
                    measureChildWithMargins(child, uniformMeasureSpec, 0, heightMeasuredSpec, 0);
                    lp.height = oldHeight;
                }
            }
        }
    }

    private void measureDragger() {
        LayoutParams lp = (LayoutParams) mDraggerBtn.getLayoutParams();

        if (lp == null)
            lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        lp.gravity = Gravity.CENTER_HORIZONTAL;

        Drawable defaultDraggerDrawable = getResources().getDrawable(DEFAULT_DRAGGER_RESOURCE);

        if (mDraggerWidth == -1) {
            mDraggerWidth = defaultDraggerDrawable.getIntrinsicWidth();
        } else {
            lp.width = mDraggerWidth;
        }
        if (mDraggerHeight == -1) {
            mDraggerHeight = defaultDraggerDrawable.getIntrinsicHeight();
        } else {
            lp.height = mDraggerHeight;
        }

        mDraggerBtn.setLayoutParams(lp);

        mDraggerBtn.measure(MeasureSpec.makeMeasureSpec(mDraggerWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mDraggerHeight, MeasureSpec.EXACTLY));
    }

    /**
     * <p>Returns the number of children to skip after measuring/laying out
     * the specified child.</p>
     *
     * @param child the child after which we want to skip children
     * @param index the index of the child after which we want to skip children
     * @return the number of children to skip, 0 by default.
     */
    private int getChildrenSkippedCount(View child, int index) {
        return 0;
    }

    /**
     * <p>Returns the size (width or height) that should be occupied by a null
     * child.</p>
     *
     * @param childIndex childIndex the index of the null child
     * @return the width or height of the child depending on the orientation
     */
    private int measureNullChild(int childIndex) {
        return 0;
    }

    /**
     * <p>Measure the child according to the parent's measure specs. This
     * method should be overriden by subclasses to force the sizing of
     * childern. This method is called by {@link #measureVertical(int, int)}.</p>
     *
     * @param child the child to measure
     * @param childIndex the index of the child in this view
     * @param widthMeasureSpec horizontal space requirements as imposed by the parent
     * @param totalWidth extra space that has been used up by the parent horizontally
     * @param heightMeasureSpec vertical space requirements as imposed by the parent
     * @param totalHeight extra space that has been used up by the parent vertically
     */
    void measureChildBeforeLayout(View child, int childIndex,
                                  int widthMeasureSpec, int totalWidth, int heightMeasureSpec,
                                  int totalHeight) {
        measureChildWithMargins(child, widthMeasureSpec, totalWidth,
                heightMeasureSpec, totalHeight);
    }

    /**
     * <p>Return the location offset of the specified child. This can be used
     * by subclasses to change the location of a given widget.</p>
     *
     * @param child the child for which to obtain the location offset
     * @return the location offset in pixels
     */
    int getLocationOffset(View child) {
        return 0;
    }

    /**
     * <p>Return the size offset of the next sibling of the specified child.
     * This can be used by subclasses to change the location of the widget
     * following<code>child</code>.</p>
     *
     * @param child the child whose next sibling will be moved
     * @return the location offset of the next child in pixels
     */
    int getNextLocationOffset(View child) {
        return 0;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutVertical(l, t, r, b);
    }

    /**
     * Position the children during a layout pass.
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    void layoutVertical(int left, int top, int right, int bottom) {
        final int paddingLeft = getPaddingLeft();

        int childTop;
        int childLeft;

        // Where right end of child should go
        final int width = right - left;
        int childRight = width - getPaddingRight();

        // Space available for child
        int childSpace = width - paddingLeft - getPaddingRight();

        final int count = getChildCount();

        final int majorGravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        final int minorGravity = mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;

        switch (majorGravity) {
            case Gravity.BOTTOM:
                // mTotalLength contains  the padding already
                childTop = getPaddingTop() + bottom - top - mTotalLength;
                break;

            case Gravity.CENTER_VERTICAL:
                // mTotalLength contains the padding already
                childTop = getPaddingTop() + (bottom - top - mTotalLength) / 2;
                break;

            case Gravity.TOP:
            default:
                childTop = getPaddingTop();
                break;
        }

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child == null) {
                childTop += measureNullChild(i);
            } else if (child.getVisibility() != GONE) {
                final int childWidth = child.getMeasuredWidth();
                final int childHeght = child.getMeasuredHeight();

                final LayoutParams lp = ((LayoutParams) child.getLayoutParams());

                int gravity = lp.gravity;
                if (gravity < 0) {
                    gravity = minorGravity;
                }
                final int layoutDirection = getLayoutDirection();
                final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = paddingLeft + ((childSpace - childWidth) / 2) +
                                lp.leftMargin - lp.rightMargin;
                        break;

                    case Gravity.RIGHT:
                        childLeft = childRight - childWidth - lp.rightMargin;
                        break;

                    case Gravity.LEFT:
                    default:
                        childLeft = paddingLeft + lp.leftMargin;
                        break;
                }

                childTop += lp.topMargin;

                if (mDraggerBtn.getId() == i && mUpperView != null) {
                    childTop -= mDraggerHeight;
                } else if (mDraggerBtn.getId() == i && mUpperView == null) {
                    continue;
                } else if (count - 1 == i) {
                    // slide view: 一直处于父容器的底部
                    childTop = bottom - top - getPaddingTop() - getPaddingBottom() - lp.bottomMargin - childHeght;
                    int draggerTop = childTop - mDraggerHeight;
                    int draggerLeft = paddingLeft + (childSpace - mDraggerWidth) / 2;
                    setChildFrame(mDraggerBtn, draggerLeft, draggerTop + getLocationOffset(mDraggerBtn),
                            mDraggerWidth, mDraggerHeight);
                }

                setChildFrame(child, childLeft, childTop + getLocationOffset(child),
                        childWidth, childHeght);
                childTop += childHeght + lp.bottomMargin + getNextLocationOffset(child);

                i += getChildrenSkippedCount(child, i);
            }
        }
    }

    private void setChildFrame(View child, int left, int top, int width, int height) {
        child.layout(left, top, left + width, top + height);
    }

    public void setDraggerBackgroundResource(int resId) {
        if (resId != 0 && resId == mDraggerResource) {
            return;
        }

        Drawable d = null;
        if (resId != 0) {
            d = getResources().getDrawable(resId);
        }
        setDraggerBackground(d);
        mDraggerResource = resId;
    }

    public void setDraggerBackground(Drawable drawable) {
        if (mDraggerDrawable != drawable) {
            mDraggerDrawable = drawable;
            mDraggerResource = 0;
            mDraggerWidth = drawable.getIntrinsicWidth();
            mDraggerHeight = drawable.getIntrinsicHeight();
            mDraggerBtn.setBackground(drawable);

            measureDragger();
        }
    }

    @Override
    public void addView(View child) {

        this.addView(child, -1);
    }

    @Override
    public void addView(View child, int index) {
        if (getChildCount() > MAX_CHILD_COUNT - 1)
            throw new IllegalStateException("SlidingUpLayout can only host two direct children (not counting the default dragger button)");

        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        this.addView(child, -1, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {

        if (getChildCount() > MAX_CHILD_COUNT)
            throw new IllegalStateException("SlidingUpLayout can only host two direct children (not counting the default dragger button)");

        super.addView(child, index, params);
    }

    @Override
    public void addView(View child, int width, int height) {
        if (getChildCount() > MAX_CHILD_COUNT)
            throw new IllegalStateException("SlidingUpLayout can only host two direct children (not counting the default dragger button)");

        super.addView(child, width, height);
    }

    public Drawable getDraggerBackground() {
        return mDraggerDrawable;
    }

    public void setMaxHeight(int height) {
        mMaxHeight = height > mDraggerHeight ? height : -1;
    }

    public int getMaxHeight() {
        return mMaxHeight;
    }

    public void setMinHeight(int height) {
        mMinHeight = height < mDraggerHeight ? mDraggerHeight : height;
    }

    public int getMinHeight() {
        return mMinHeight;
    }

    public void setGravity(int gravity) {
        if (mGravity != gravity) {
            if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
                gravity |= Gravity.START;
            }

            if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
                gravity |= Gravity.TOP;
            }

            mGravity = gravity;
            requestLayout();
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /**
     * Return a set of layout parameters with a width of
     * {@link android.view.ViewGroup.LayoutParams#MATCH_PARENT}
     * and a height of {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}.
     */
    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(SlidingUpLayout.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(SlidingUpLayout.class.getName());
    }

    /**
     * Per-child layout information associated with View SlidingUpLayout.
     *
     * @attr ref R.styleable#SlidingUpLayout_Layout_layout_weight
     * @attr ref R.styleable#SlidingUpLayout_Layout_layout_gravity
     */
    public static class LayoutParams extends MarginLayoutParams {
        /**
         * Indicates how much of the extra space n the SlidingUpLayout will be
         * allocated to the view associated with these LayoutParams. Specify
         * 0 if the view should not be stretched. Otherwise the extra pixels
         * will be pro-rated among all views whose weight is greater than 0.
         */
        @ViewDebug.ExportedProperty(category = "layout")
        public float weight;

        /**
         * Gravity for the view associated with these LayoutParams.
         */
        @ViewDebug.ExportedProperty(category = "layout", mapping= {
                @ViewDebug.IntToString(from = -1, to = "NONE"),
                @ViewDebug.IntToString(from = Gravity.NO_GRAVITY, to = "NONE"),
                @ViewDebug.IntToString(from = Gravity.LEFT, to = "LEFT"),
                @ViewDebug.IntToString(from = Gravity.RIGHT, to = "RIGHT"),
                @ViewDebug.IntToString(from = Gravity.CENTER_HORIZONTAL, to = "CENTER_HORIZONTAL")
        })
        public int gravity = -1;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.SlidingUpLayout_Layout);

            weight = a.getFloat(R.styleable.SlidingUpLayout_Layout_layout_weight, 0);
            gravity = a.getInt(R.styleable.SlidingUpLayout_Layout_layout_gravity, -1);

            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            weight = 0;
        }

        /**
         * Creates a new set of layout parameters with the specified width, height
         * and weight.
         *
         * @param width the width, either {@link #MATCH_PARENT},
         *              {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param height the height, either {@link #MATCH_PARENT},
         *              {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param weight the weight
         */
        public LayoutParams(int width, int height, float weight) {
            super(width, height);
            this.weight = weight;
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        /**
         * Copy constructor. Clones the width, height, margin values, weight,
         * and gravity of the source.
         *
         * @param source The layout params to copy from.
         */
        public LayoutParams(LayoutParams source) {
            super(source);

            this.weight = source.weight;
            this.gravity = source.gravity;
        }

        public String debug(String output) {
            return output + "SlidingUpLayout.LayoutParams={width=" + sizeToString(width) +
                    ", height=" + sizeToString(height) + ", weight=" + weight + "}";
        }

        /**
         * Converts the specified size to a readable String.
         *
         * @param size the size to convert
         * @return a String instance representing the supplied size
         *
         * @hide
         */
        protected static String sizeToString(int size) {
            if (size == WRAP_CONTENT) {
                return "wrap-content";
            }
            if (size == MATCH_PARENT) {
                return "match-parent";
            }
            return String.valueOf(size);
        }
    }
}
