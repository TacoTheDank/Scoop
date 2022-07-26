package taco.scoop.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.FocusFinder;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AnimationUtils;
import android.widget.EdgeEffect;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.OverScroller;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Layout container for a view hierarchy that can be scrolled by the user,
 * allowing it to be larger than the physical display.  A MergedScrollView
 * is a {@link FrameLayout}, meaning you should place one child in it
 * containing the entire contents to scroll; this child may itself be a layout
 * manager with a complex hierarchy of objects.  A child that is often used
 * is a {@link LinearLayout} in a vertical orientation, presenting a vertical
 * array of top-level items that the user can scroll through.
 * <p>You should never use a MergedScrollView with a {@link ListView}, because
 * ListView takes care of its own vertical scrolling.  Most importantly, doing this
 * defeats all of the important optimizations in ListView for dealing with
 * large lists, since it effectively forces the ListView to display its entire
 * list of items to fill up the infinite container supplied by MergedScrollView.
 * <p>The {@link TextView} class also
 * takes care of its own scrolling, so does not require a MergedScrollView, but
 * using the two together is possible to achieve the effect of a text view
 * within a larger container.
 *
 * <p>MergedScrollView supports scrolling in both directions.
 *
 * @attr ref android.R.styleable#ScrollView_fillViewport
 */
@SuppressWarnings({"JavaDoc", "unused"})
public class MergedScrollView extends FrameLayout {
    static final int ANIMATED_SCROLL_GAP = 250;

    static final float MAX_SCROLL_FACTOR = 0.5f;

    private static final String TAG = "MergedScrollView";
    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;
    /**
     * Always return a size of 0 for MeasureSpec values with a mode of UNSPECIFIED
     */
    static boolean sUseZeroUnspecifiedMeasureSpec = false;
    /**
     * Signals that compatibility booleans have been initialized according to
     * target SDK versions.
     */
    private static boolean sCompatibilityDone = false;
    private final Rect mTempRect = new Rect();
    private long mLastScroll;
    private OverScroller mScroller;
    private EdgeEffect mEdgeGlowTop;
    private EdgeEffect mEdgeGlowBottom;
    private EdgeEffect mEdgeGlowLeft;
    private EdgeEffect mEdgeGlowRight;
    /**
     * Position of the last motion event.
     */
    private int mLastMotionY;
    private int mLastMotionX;
    /**
     * True when the layout has changed but the traversal has not come through yet.
     * Ideally the view hierarchy would keep track of this for us.
     */
    private boolean mIsLayoutDirty = true;
    /**
     * The child to give focus to in the event that a child has requested focus while the
     * layout is dirty. This prevents the scroll from being wrong if the child has not been
     * laid out before requesting focus.
     */
    private View mChildToScrollTo = null;
    /**
     * True if the user is currently dragging this MergedScrollView around. This is
     * not the same as 'is being flinged', which can be checked by
     * mScroller.isFinished() (flinging begins when the user lifts his finger).
     */
    private boolean mIsBeingDragged = false;
    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    /**
     * When set to true, the scroll view measure its child to make it fill the currently
     * visible area.
     */
    @ViewDebug.ExportedProperty(category = "layout")
    private boolean mFillViewport;
    /**
     * Whether arrow scrolling is animated.
     */
    private boolean mSmoothScrollingEnabled = true;
    private int mTouchSlop;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;
    /**
     * Vertical scroll factor cached by {@link #getVerticalScrollFactor}.
     */
    private float mVerticalScrollFactor;

    private SavedState mSavedState;

    public MergedScrollView(Context context) {
        this(context, null);
        if (!sCompatibilityDone && context != null) {
            final int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;

            // In M and newer, our widgets can pass a "hint" value in the size
            // for UNSPECIFIED MeasureSpecs. This lets child views of scrolling containers
            // know what the expected parent size is going to be, so e.g. list items can size
            // themselves at 1/3 the size of their container. It breaks older apps though,
            // specifically apps that use some popular open source libraries.
            sUseZeroUnspecifiedMeasureSpec = targetSdkVersion < Build.VERSION_CODES.M;

            sCompatibilityDone = true;
        }
    }

    public MergedScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.scrollViewStyle);
    }

    public MergedScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initScrollView();
    }

    /**
     * Like {@link MeasureSpec#makeMeasureSpec(int, int)}, but any spec with a mode of UNSPECIFIED
     * will automatically get a size of 0. Older apps expect this.
     */
    public static int makeSafeMeasureSpec(int size, int mode) {
        if (sUseZeroUnspecifiedMeasureSpec && mode == MeasureSpec.UNSPECIFIED) {
            return 0;
        }
        return MeasureSpec.makeMeasureSpec(size, mode);
    }

    /**
     * Return true if child is a descendant of parent, (or equal to the parent).
     */
    private static boolean isViewDescendantOf(View child, View parent) {
        if (child == parent) {
            return true;
        }

        final ViewParent theParent = child.getParent();
        return theParent instanceof ViewGroup && isViewDescendantOf((View) theParent, parent);
    }

    private static int clamp(int n, int my, int child) {
        if (my >= child || n < 0) {
            /* my >= child is this case:
             *                    |--------------- me ---------------|
             *     |------ child ------|
             * or
             *     |--------------- me ---------------|
             *            |------ child ------|
             * or
             *     |--------------- me ---------------|
             *                                  |------ child ------|
             *
             * n < 0 is this case:
             *     |------ me ------|
             *                    |-------- child --------|
             *     |-- mScrollX --|
             */
            return 0;
        }
        if (my + n > child) {
            /* this case:
             *                    |------ me ------|
             *     |------ child ------|
             *     |-- mScrollX --|
             */
            return child - my;
        }
        return n;
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        final int length = getVerticalFadingEdgeLength();
        if (getScrollY() < length) {
            return getScrollY() / (float) length;
        }

        return 1.0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        final int length = getVerticalFadingEdgeLength();
        final int bottomEdge = getHeight() - getPaddingBottom();
        final int span = getChildAt(0).getBottom() - getScrollY() - bottomEdge;
        if (span < length) {
            return span / (float) length;
        }

        return 1.0f;
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        final int length = getHorizontalFadingEdgeLength();
        if (getScrollX() < length) {
            return getScrollX() / (float) length;
        }

        return 1.0f;
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        if (getChildCount() == 0) {
            return 0.0f;
        }

        final int length = getHorizontalFadingEdgeLength();
        final int rightEdge = getWidth() - getPaddingRight();
        final int span = getChildAt(0).getRight() - getScrollX() - rightEdge;
        if (span < length) {
            return span / (float) length;
        }

        return 1.0f;
    }

    /**
     * @return The maximum amount this scroll view will scroll in response to
     * an arrow event.
     */
    //public int getMaxScrollAmount() {
    //    return (int) (MAX_SCROLL_FACTOR * (getBottom() - getTop()));
    //}
    public int getMaxScrollAmountVertical() {
        return (int) (MAX_SCROLL_FACTOR * getHeight());
    }

    public int getMaxScrollAmountHorizontal() {
        return (int) (MAX_SCROLL_FACTOR * getWidth());
    }

    private void initScrollView() {
        mScroller = new OverScroller(getContext());
        setFocusable(true);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setWillNotDraw(false);
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    public void addView(View child) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("MergedScrollView can host only one direct child");
        }

        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("MergedScrollView can host only one direct child");
        }

        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("MergedScrollView can host only one direct child");
        }

        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0) {
            throw new IllegalStateException("MergedScrollView can host only one direct child");
        }

        super.addView(child, index, params);
    }

    /**
     * @return Returns true this MergedScrollView can be scrolled
     */
    private boolean canScroll() {
        View child = getChildAt(0);
        if (child != null) {
            int childHeight = child.getHeight();
            int childWidth = child.getWidth();
            return getHeight() < childHeight + getPaddingTop() + getPaddingBottom() ||
                    getWidth() < childWidth + getPaddingLeft() + getPaddingRight();
        }
        return false;
    }

    /**
     * Indicates whether this MergedScrollView's content is stretched to fill the viewport.
     *
     * @return True if the content fills the viewport, false otherwise.
     * @attr ref android.R.styleable#ScrollView_fillViewport
     */
    public boolean isFillViewport() {
        return mFillViewport;
    }

    /**
     * Indicates this MergedScrollView whether it should stretch its content height to fill
     * the viewport or not.
     *
     * @param fillViewport True to stretch the content's height to the viewport's
     *                     boundaries, false otherwise.
     * @attr ref android.R.styleable#ScrollView_fillViewport
     */
    public void setFillViewport(boolean fillViewport) {
        if (fillViewport != mFillViewport) {
            mFillViewport = fillViewport;
            requestLayout();
        }
    }

    /**
     * @return Whether arrow scrolling will animate its transition.
     */
    public boolean isSmoothScrollingEnabled() {
        return mSmoothScrollingEnabled;
    }

    /**
     * Set whether arrow scrolling will animate its transition.
     *
     * @param smoothScrollingEnabled whether arrow scrolling will animate its transition
     */
    public void setSmoothScrollingEnabled(boolean smoothScrollingEnabled) {
        mSmoothScrollingEnabled = smoothScrollingEnabled;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mFillViewport) {
            return;
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.UNSPECIFIED) {
            return;
        }

        if (getChildCount() > 0) {
            final View child = getChildAt(0);
            final int widthPadding;
            final int heightPadding;
            final int targetSdkVersion = getContext().getApplicationInfo().targetSdkVersion;
            final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                widthPadding = getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin;
                heightPadding = getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin;
            } else {
                widthPadding = getPaddingLeft() + getPaddingRight();
                heightPadding = getPaddingTop() + getPaddingBottom();
            }

            final int desiredHeight = getMeasuredHeight() - heightPadding;
            final int desiredWidth = getMeasuredWidth() - widthPadding;

            if (child.getMeasuredHeight() < desiredHeight || child.getMeasuredWidth() < desiredWidth) {
                final int childWidthMeasureSpec;
                final int childHeightMeasureSpec;
                if (child.getMeasuredHeight() < desiredHeight) {
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            desiredHeight, MeasureSpec.EXACTLY);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(
                            heightMeasureSpec, heightPadding, lp.height);
                }
                if (child.getMeasuredWidth() < desiredWidth) {
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            desiredWidth, MeasureSpec.EXACTLY);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(
                            widthMeasureSpec, widthPadding, lp.width);
                }
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     *
     * @param event The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    public boolean executeKeyEvent(KeyEvent event) {
        mTempRect.setEmpty();

        if (!canScroll()) {
            if (isFocused() && event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
                View currentFocused = findFocus();
                if (currentFocused == this) currentFocused = null;
                View nextFocused = FocusFinder.getInstance().findNextFocus(this,
                        currentFocused, View.FOCUS_DOWN);
                return nextFocused != null
                        && nextFocused != this
                        && nextFocused.requestFocus(View.FOCUS_DOWN);
            }
            return false;
        }

        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_UP);
                    } else {
                        handled = fullScroll(0, View.FOCUS_UP);
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_DOWN);
                    } else {
                        handled = fullScroll(0, View.FOCUS_DOWN);
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_LEFT);
                    } else {
                        handled = fullScroll(View.FOCUS_LEFT, 0);
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (!event.isAltPressed()) {
                        handled = arrowScroll(View.FOCUS_RIGHT);
                    } else {
                        handled = fullScroll(View.FOCUS_RIGHT, 0);
                    }
                    break;
                case KeyEvent.KEYCODE_SPACE:
                    pageScroll(0, event.isShiftPressed() ? View.FOCUS_UP : View.FOCUS_DOWN);
                    break;
            }
        }

        return handled;
    }

    private boolean inChild(int x, int y) {
        if (getChildCount() > 0) {
            final int scrollY = getScrollY();
            final int scrollX = getScrollX();
            final View child = getChildAt(0);
            return !(y < child.getTop() - scrollY
                    || y >= child.getBottom() - scrollY
                    || x < child.getLeft() - scrollX
                    || x >= child.getRight() - scrollY);
        }
        return false;
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        /*
         * Shortcut the most recurring case: the user is in the dragging
         * state and he is moving his finger.  We want to intercept this
         * motion.
         */
        final int action = ev.getAction();
        if (action == MotionEvent.ACTION_MOVE && mIsBeingDragged) {
            return true;
        }

        if (super.onInterceptTouchEvent(ev)) {
            return true;
        }

        /*
         * Don't try to intercept touch if we can't scroll anyway.
         */
        if (getScrollY() == 0 && !canScrollVertically(1)
                || getScrollX() == 0 && !canScrollHorizontally(1)
        ) {
            return false;
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                 * Locally do absolute value. mLastMotionX/Y is set to the x/y value
                 * of the down event.
                 */
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final int pointerIndex = ev.findPointerIndex(activePointerId);
                if (pointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + activePointerId
                            + " in onInterceptTouchEvent");
                    break;
                }

                final int x = (int) ev.getX(pointerIndex);
                final int y = (int) ev.getY(pointerIndex);
                final int xDiff = Math.abs(x - mLastMotionX);
                final int yDiff = Math.abs(y - mLastMotionY);
                if (xDiff > mTouchSlop || yDiff > mTouchSlop) {
                    mIsBeingDragged = true;
                    mLastMotionX = x;
                    mLastMotionY = y;
                    initVelocityTrackerIfNotExists();
                    mVelocityTracker.addMovement(ev);
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN:
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                if (!inChild(x, y)) {
                    mIsBeingDragged = false;
                    recycleVelocityTracker();
                    break;
                }

                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionX = x;
                mLastMotionY = y;
                mActivePointerId = ev.getPointerId(0);

                initOrResetVelocityTracker();
                mVelocityTracker.addMovement(ev);
                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't. mScroller.isFinished should be false when
                 * being flinged. We need to call computeScrollOffset() first so that
                 * isFinished() is correct.
                 */
                mScroller.computeScrollOffset();
                mIsBeingDragged = !mScroller.isFinished();
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                /* Release the drag */
                mIsBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = ev.getActionIndex();
                mLastMotionX = (int) ev.getX(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionX = (int) ev.getX(ev.findPointerIndex(mActivePointerId));
                break;
        }

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (getChildCount() == 0) {
                    return false;
                }
                if (mIsBeingDragged = !mScroller.isFinished()) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                // Remember where the motion event started
                mLastMotionX = (int) ev.getX();
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;
            case MotionEvent.ACTION_MOVE:
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                if (activePointerIndex == -1) {
                    Log.e(TAG, "Invalid pointerId=" + mActivePointerId + " in onTouchEvent");
                    break;
                }

                final int x = (int) ev.getX(activePointerIndex);
                final int y = (int) ev.getY(activePointerIndex);
                int deltaX = mLastMotionX - x;
                int deltaY = mLastMotionY - y;
                if (!mIsBeingDragged && (
                        Math.abs(deltaX) > mTouchSlop || Math.abs(deltaY) > mTouchSlop)
                ) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                    mIsBeingDragged = true;
                    if (deltaX > 0) {
                        deltaX -= mTouchSlop;
                    } else {
                        deltaX += mTouchSlop;
                    }
                    if (deltaY > 0) {
                        deltaY -= mTouchSlop;
                    } else {
                        deltaY += mTouchSlop;
                    }
                }
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    mLastMotionX = x;
                    mLastMotionY = y;

                    final int oldX = getScrollX();
                    final int oldY = getScrollY();
                    final int rangeX = getScrollRangeHorizontal();
                    final int rangeY = getScrollRangeVertical();
                    final int overscrollMode = getOverScrollMode();
                    final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                            overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS
                                    && (rangeX > 0 || rangeY > 0);

                    scrollBy(deltaX, deltaY);
                    final int newX = getScrollX();
                    final int newY = getScrollY();
                    // In corners
                    if (newX == 0 && newY == 0 ||
                            newX == rangeX && newY == rangeY ||
                            newX == 0 && newY == rangeY ||
                            newX == rangeX && newY == 0) {
                        mVelocityTracker.clear();
                    }

                    if (canOverscroll) {
                        final int pulledToX = oldX + deltaX;
                        final int pulledToY = oldY + deltaY;
                        if (pulledToX < 0) {
                            mEdgeGlowLeft.onPull((float) deltaX / getWidth(),
                                    1.f - ev.getY(activePointerIndex) / getHeight());
                            if (!mEdgeGlowRight.isFinished()) {
                                mEdgeGlowRight.onRelease();
                            }
                        } else if (pulledToX > rangeX) {
                            mEdgeGlowRight.onPull((float) deltaX / getWidth(),
                                    ev.getY(activePointerIndex) / getHeight());
                            if (!mEdgeGlowLeft.isFinished()) {
                                mEdgeGlowLeft.onRelease();
                            }
                        }
                        if (pulledToY < 0) {
                            mEdgeGlowTop.onPull((float) deltaY / getHeight(),
                                    ev.getX(activePointerIndex) / getWidth());
                            if (!mEdgeGlowBottom.isFinished()) {
                                mEdgeGlowBottom.onRelease();
                            }
                        } else if (pulledToY > rangeY) {
                            mEdgeGlowBottom.onPull((float) deltaY / getHeight(),
                                    1.f - ev.getX(activePointerIndex) / getWidth());
                            if (!mEdgeGlowTop.isFinished()) {
                                mEdgeGlowTop.onRelease();
                            }
                        }
                        if (mEdgeGlowTop != null
                                && (!mEdgeGlowTop.isFinished() || !mEdgeGlowBottom.isFinished()
                                || !mEdgeGlowLeft.isFinished() || !mEdgeGlowRight.isFinished())) {
                            postInvalidateOnAnimation();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsBeingDragged) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocityX = (int) velocityTracker.getXVelocity(mActivePointerId);
                    int initialVelocityY = (int) velocityTracker.getYVelocity(mActivePointerId);

                    if (Math.abs(initialVelocityX) > mMinimumVelocity
                            || Math.abs(initialVelocityY) > mMinimumVelocity
                    ) {
                        fling(-initialVelocityX, -initialVelocityY);
                    }

                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged && getChildCount() > 0) {
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = (int) ev.getX(newPointerIndex);
            mLastMotionY = (int) ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0) {
            if (event.getAction() == MotionEvent.ACTION_SCROLL) {
                if (!mIsBeingDragged) {
                    final float hscroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
                    final float vscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                    if (vscroll != 0) {
                        final int delta = (int) (vscroll * getVerticalScrollFactor());
                        final int range = getScrollRangeVertical();
                        int oldScrollY = getScrollY();
                        int newScrollY = oldScrollY - delta;
                        if (newScrollY < 0) {
                            newScrollY = 0;
                        } else if (newScrollY > range) {
                            newScrollY = range;
                        }
                        if (newScrollY != oldScrollY) {
                            super.scrollTo(getScrollX(), newScrollY);
                            return true;
                        }
                    }
                    if (hscroll != 0) {
                        final int delta = (int) (hscroll * getHorizontalScrollFactor());
                        final int range = getScrollRangeHorizontal();
                        int oldScrollX = getScrollX();
                        int newScrollX = oldScrollX - delta;
                        if (newScrollX < 0) {
                            newScrollX = 0;
                        } else if (newScrollX > range) {
                            newScrollX = range;
                        }
                        if (newScrollX != oldScrollX) {
                            super.scrollTo(newScrollX, getScrollY());
                            return true;
                        }
                    }
                }
            }
        }
        return super.onGenericMotionEvent(event);
    }

    /**
     * Gets a scale factor that determines the distance the view should scroll
     * vertically in response to {@link MotionEvent#ACTION_SCROLL}.
     *
     * @return The vertical scroll scale factor.
     * @hide
     */
    protected float getVerticalScrollFactor() {
        if (mVerticalScrollFactor == 0) {
            TypedValue outValue = new TypedValue();
            if (!getContext().getTheme().resolveAttribute(
                    android.R.attr.listPreferredItemHeight, outValue, true)) {
                throw new IllegalStateException(
                        "Expected theme to define listPreferredItemHeight.");
            }
            mVerticalScrollFactor = outValue.getDimension(
                    getContext().getResources().getDisplayMetrics());
        }
        return mVerticalScrollFactor;
    }

    /**
     * Gets a scale factor that determines the distance the view should scroll
     * horizontally in response to {@link MotionEvent#ACTION_SCROLL}.
     *
     * @return The horizontal scroll scale factor.
     * @hide
     */
    protected float getHorizontalScrollFactor() {
        // TODO: Should use something else.
        return getVerticalScrollFactor();
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY,
                                  boolean clampedX, boolean clampedY) {
        // Treat animating scrolls differently; see #computeScroll() for why.
        if (!mScroller.isFinished()) {
            final int oldX = getScrollX();
            final int oldY = getScrollY();
            super.scrollTo(scrollX, scrollY);
            onScrollChanged(getScrollX(), getScrollY(), oldX, oldY);
        } else {
            super.scrollTo(scrollX, scrollY);
        }

        awakenScrollBars();
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return MergedScrollView.class.getName();
    }

    private int getScrollRangeHorizontal() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            scrollRange = Math.max(0,
                    child.getWidth() - (getWidth() - getPaddingLeft() - getPaddingRight()));
        }
        return scrollRange;
    }

    private int getScrollRangeVertical() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            scrollRange = Math.max(0,
                    child.getHeight() - (getHeight() - getPaddingBottom() - getPaddingTop()));
        }
        return scrollRange;
    }

    private View findFocusableViewInMyBoundsHorizontal(final boolean leftFocus,
                                                       final int left, View preferredFocusable) {
        /*
         * The fading edge's transparent side should be considered for focus
         * since it's mostly visible, so we divide the actual fading edge length
         * by 2.
         */
        final int fadingEdgeLength = getHorizontalFadingEdgeLength() / 2;
        final int leftWithoutFadingEdge = left + fadingEdgeLength;
        final int rightWithoutFadingEdge = left + getWidth() - fadingEdgeLength;

        if (preferredFocusable != null
                && preferredFocusable.getLeft() < rightWithoutFadingEdge
                && preferredFocusable.getRight() > leftWithoutFadingEdge) {
            return preferredFocusable;
        }

        return findFocusableViewInBoundsHorizontal(leftFocus, leftWithoutFadingEdge,
                rightWithoutFadingEdge);
    }

    private View findFocusableViewInBoundsHorizontal(boolean leftFocus, int left, int right) {

        List<View> focusables = getFocusables(View.FOCUS_FORWARD);
        View focusCandidate = null;

        /*
         * A fully contained focusable is one where its left is below the bound's
         * left, and its right is above the bound's right. A partially
         * contained focusable is one where some part of it is within the
         * bounds, but it also has some part that is not within bounds.  A fully contained
         * focusable is preferred to a partially contained focusable.
         */
        boolean foundFullyContainedFocusable = false;

        int count = focusables.size();
        for (int i = 0; i < count; i++) {
            View view = focusables.get(i);
            int viewLeft = view.getLeft();
            int viewRight = view.getRight();

            if (left < viewRight && viewLeft < right) {
                /*
                 * the focusable is in the target area, it is a candidate for
                 * focusing
                 */

                final boolean viewIsFullyContained = left < viewLeft &&
                        viewRight < right;

                if (focusCandidate == null) {
                    /* No candidate, take this one */
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                } else {
                    final boolean viewIsCloserToBoundary =
                            leftFocus ? viewLeft < focusCandidate.getLeft()
                                    : viewRight > focusCandidate.getRight();

                    if (foundFullyContainedFocusable) {
                        if (viewIsFullyContained && viewIsCloserToBoundary) {
                            /*
                             * We're dealing with only fully contained views, so
                             * it has to be closer to the boundary to beat our
                             * candidate
                             */
                            focusCandidate = view;
                        }
                    } else {
                        if (viewIsFullyContained) {
                            /* Any fully contained view beats a partially contained view */
                            focusCandidate = view;
                            foundFullyContainedFocusable = true;
                        } else if (viewIsCloserToBoundary) {
                            /*
                             * Partially contained view beats another partially
                             * contained view if it's closer
                             */
                            focusCandidate = view;
                        }
                    }
                }
            }
        }

        return focusCandidate;
    }

    /**
     * <p>
     * Finds the next focusable component that fits in the specified bounds.
     * </p>
     *
     * @param topFocus look for a candidate is the one at the top of the bounds
     *                 if topFocus is true, or at the bottom of the bounds if topFocus is
     *                 false
     * @param top      the top offset of the bounds in which a focusable must be
     *                 found
     * @param bottom   the bottom offset of the bounds in which a focusable must
     *                 be found
     * @return the next focusable component in the bounds or null if none can
     * be found
     */
    private View findFocusableViewInBoundsVertical(boolean topFocus, int top, int bottom) {

        List<View> focusables = getFocusables(View.FOCUS_FORWARD);
        View focusCandidate = null;

        /*
         * A fully contained focusable is one where its top is below the bound's
         * top, and its bottom is above the bound's bottom. A partially
         * contained focusable is one where some part of it is within the
         * bounds, but it also has some part that is not within bounds.  A fully contained
         * focusable is preferred to a partially contained focusable.
         */
        boolean foundFullyContainedFocusable = false;

        int count = focusables.size();
        for (int i = 0; i < count; i++) {
            View view = focusables.get(i);
            int viewTop = view.getTop();
            int viewBottom = view.getBottom();

            if (top < viewBottom && viewTop < bottom) {
                /*
                 * the focusable is in the target area, it is a candidate for
                 * focusing
                 */

                final boolean viewIsFullyContained = top < viewTop &&
                        viewBottom < bottom;

                if (focusCandidate == null) {
                    /* No candidate, take this one */
                    focusCandidate = view;
                    foundFullyContainedFocusable = viewIsFullyContained;
                } else {
                    final boolean viewIsCloserToBoundary =
                            topFocus ? viewTop < focusCandidate.getTop()
                                    : viewBottom > focusCandidate.getBottom();

                    if (foundFullyContainedFocusable) {
                        if (viewIsFullyContained && viewIsCloserToBoundary) {
                            /*
                             * We're dealing with only fully contained views, so
                             * it has to be closer to the boundary to beat our
                             * candidate
                             */
                            focusCandidate = view;
                        }
                    } else {
                        if (viewIsFullyContained) {
                            /* Any fully contained view beats a partially contained view */
                            focusCandidate = view;
                            foundFullyContainedFocusable = true;
                        } else if (viewIsCloserToBoundary) {
                            /*
                             * Partially contained view beats another partially
                             * contained view if it's closer
                             */
                            focusCandidate = view;
                        }
                    }
                }
            }
        }

        return focusCandidate;
    }

    /**
     * <p>Handles scrolling in response to a "page up/down" shortcut press. This
     * method will scroll the view by one page up or down and give the focus
     * to the topmost/bottommost component in the new visible area. If no
     * component is a good candidate for focus, this MergedScrollView reclaims the
     * focus.</p>
     *
     * @param directionY the scroll direction: {@link View#FOCUS_UP}
     *                   to go one page up or
     *                   {@link View#FOCUS_DOWN} to go one page down
     */
    public void pageScroll(int directionX, int directionY) {
        boolean right = directionX == View.FOCUS_RIGHT;
        boolean down = directionY == View.FOCUS_DOWN;
        int width = getWidth();
        int height = getHeight();

        if (right) {
            mTempRect.left = getScrollX() + width;
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(0);
                if (mTempRect.left + width > view.getRight()) {
                    mTempRect.left = view.getRight() - width;
                }
            }
        } else {
            mTempRect.left = getScrollX() - width;
            if (mTempRect.left < 0) {
                mTempRect.left = 0;
            }
        }
        if (down) {
            mTempRect.top = getScrollY() + height;
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                if (mTempRect.top + height > view.getBottom()) {
                    mTempRect.top = view.getBottom() - height;
                }
            }
        } else {
            mTempRect.top = getScrollY() - height;
            if (mTempRect.top < 0) {
                mTempRect.top = 0;
            }
        }
        mTempRect.right = mTempRect.left + width;
        mTempRect.bottom = mTempRect.top + height;

        scrollAndFocus(directionX, directionY, mTempRect);
    }

    /**
     * <p>Handles scrolling in response to a "home/end" shortcut press. This
     * method will scroll the view to the top or bottom and give the focus
     * to the topmost/bottommost component in the new visible area. If no
     * component is a good candidate for focus, this MergedScrollView reclaims the
     * focus.</p>
     *
     * @param directionY the scroll direction: {@link android.view.View#FOCUS_UP}
     *                   to go the top of the view or
     *                   {@link android.view.View#FOCUS_DOWN} to go the bottom
     * @return true if the key event is consumed by this method, false otherwise
     */
    public boolean fullScroll(int directionX, int directionY) {
        boolean horizontal = directionX == View.FOCUS_RIGHT || directionX == View.FOCUS_LEFT;
        boolean vertical = directionY == View.FOCUS_DOWN || directionY == View.FOCUS_UP;
        boolean right = directionX == View.FOCUS_RIGHT;
        boolean down = directionY == View.FOCUS_DOWN;
        int width = getWidth();
        int height = getHeight();

        mTempRect.left = 0;
        mTempRect.right = width;
        mTempRect.top = 0;
        mTempRect.bottom = height;

        if (horizontal && right) {
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(0);
                mTempRect.right = view.getRight();
                mTempRect.left = mTempRect.right - width;
            }
        }
        if (vertical && down) {
            int count = getChildCount();
            if (count > 0) {
                View view = getChildAt(count - 1);
                mTempRect.bottom = view.getBottom() + getPaddingBottom();
                mTempRect.top = mTempRect.bottom - height;
            }
        }

        return scrollAndFocus(directionX, directionY, mTempRect);
    }

    /**
     * <p>Scrolls the view to make the area defined by <code>top</code> and
     * <code>bottom</code> visible. This method attempts to give the focus
     * to a component visible in this area. If no component can be focused in
     * the new visible area, the focus is reclaimed by this MergedScrollView.</p>
     *
     * @param directionY the scroll direction: {@link android.view.View#FOCUS_UP}
     *                   to go upward, {@link android.view.View#FOCUS_DOWN} to downward
     * @param bounds     the bounds (offset) of the new area to be made visible
     * @return true if the key event is consumed by this method, false otherwise
     */
    private boolean scrollAndFocus(int directionX, int directionY, Rect bounds) {
        boolean handled = true;
        int top = bounds.top;
        int bottom = bounds.bottom;
        int left = bounds.left;
        int right = bounds.right;

        int width = getWidth();
        int containerLeft = getScrollX();
        int containerRight = containerLeft + width;
        boolean goLeft = directionX == View.FOCUS_LEFT;
        int height = getHeight();
        int containerTop = getScrollY();
        int containerBottom = containerTop + height;
        boolean up = directionY == View.FOCUS_UP;

        View newFocusedH = findFocusableViewInBoundsHorizontal(goLeft, left, right);
        View newFocusedV = findFocusableViewInBoundsVertical(up, top, bottom);
        if (newFocusedH == null) {
            newFocusedH = this;
        }
        if (newFocusedV == null) {
            newFocusedV = this;
        }

        if (left >= containerLeft && right <= containerRight) {
            handled = false;
        } else {
            int delta = goLeft ? left - containerLeft : right - containerRight;
            doScroll(delta, 0);
        }
        if (top >= containerTop && bottom <= containerBottom) {
            handled = false;
        } else {
            int delta = up ? top - containerTop : bottom - containerBottom;
            doScroll(0, delta);
        }

        if (newFocusedH != findFocus()) newFocusedH.requestFocus(directionX);
        if (newFocusedV != findFocus()) newFocusedV.requestFocus(directionY);

        return handled;
    }

    /**
     * Handle scrolling in response to an up/down/left/right arrow click.
     *
     * @param direction The direction corresponding to the arrow key that was
     *                  pressed
     * @return True if we consumed the event, false otherwise
     */
    public boolean arrowScroll(int direction) {

        View currentFocused = findFocus();
        if (currentFocused == this) currentFocused = null;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);

        final int maxJumpX = getMaxScrollAmountHorizontal();
        final int maxJumpY = getMaxScrollAmountVertical();

        if (nextFocused != null && isWithinDeltaOfScreen(nextFocused, maxJumpY, getHeight())) {
            nextFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(nextFocused, mTempRect);
            int scrollDeltaX = computeScrollDeltaToGetChildRectOnScreenX(mTempRect);
            int scrollDeltaY = computeScrollDeltaToGetChildRectOnScreenY(mTempRect);
            doScroll(scrollDeltaX, scrollDeltaY);
            nextFocused.requestFocus(direction);
        } else {
            // no new focus
            int scrollDeltaX = maxJumpX;
            int scrollDeltaY = maxJumpY;

            if (direction == View.FOCUS_UP && getScrollY() < scrollDeltaY) {
                scrollDeltaY = getScrollY();
            } else if (direction == View.FOCUS_DOWN) {
                if (getChildCount() > 0) {
                    int daBottom = getChildAt(0).getBottom();
                    int screenBottom = getScrollY() + getHeight() - getPaddingBottom();
                    if (daBottom - screenBottom < maxJumpY) {
                        scrollDeltaY = daBottom - screenBottom;
                    }
                }
            } else if (direction == View.FOCUS_LEFT && getScrollX() < scrollDeltaX) {
                scrollDeltaX = getScrollX();
            } else if (direction == View.FOCUS_RIGHT && getChildCount() > 0) {

                int daRight = getChildAt(0).getRight();

                int screenRight = getScrollX() + getWidth();

                if (daRight - screenRight < maxJumpX) {
                    scrollDeltaX = daRight - screenRight;
                }
            }
            if (scrollDeltaX == 0 && scrollDeltaY == 0) {
                return false;
            }
            doScroll(direction == View.FOCUS_RIGHT ? scrollDeltaX : -scrollDeltaX,
                    direction == View.FOCUS_DOWN ? scrollDeltaY : -scrollDeltaY);
        }

        if (currentFocused != null && currentFocused.isFocused()
                && isOffScreen(currentFocused)) {
            // previously focused item still has focus and is off screen, give
            // it up (take it back to ourselves)
            // (also, need to temporarily force FOCUS_BEFORE_DESCENDANTS so we are
            // sure to
            // get it)
            final int descendantFocusability = getDescendantFocusability();  // save
            setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
            requestFocus();
            setDescendantFocusability(descendantFocusability);  // restore
        }
        return true;
    }

    /**
     * @return whether the descendant of this scroll view is scrolled off
     * screen.
     */
    private boolean isOffScreen(View descendant) {
        return !isWithinDeltaOfScreen(descendant, 0, getHeight());
    }

    /**
     * @return whether the descendant of this scroll view is within delta
     * pixels of being on the screen.
     */
    private boolean isWithinDeltaOfScreen(View descendant, int delta, int height) {
        descendant.getDrawingRect(mTempRect);
        offsetDescendantRectToMyCoords(descendant, mTempRect);

        return mTempRect.bottom + delta >= getScrollY()
                && mTempRect.top - delta <= getScrollY() + height
                && mTempRect.right + delta >= getScrollX()
                && mTempRect.left - delta <= getScrollX() + getWidth();
    }

    private void doScroll(int x, int y) {
        if (x != 0 || y != 0) {
            if (mSmoothScrollingEnabled) {
                smoothScrollBy(x, y);
            } else {
                scrollBy(x, y);
            }
        }
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param dx the number of pixels to scroll by on the X axis
     * @param dy the number of pixels to scroll by on the Y axis
     */
    public final void smoothScrollBy(int dx, int dy) {
        if (getChildCount() == 0) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP) {
            final int width = getWidth() - getPaddingRight() - getPaddingLeft();
            final int right = getChildAt(0).getWidth();
            final int maxX = Math.max(0, right - width);
            final int scrollX = getScrollX();
            dx = Math.max(0, Math.min(scrollX + dx, maxX)) - scrollX;
            final int height = getHeight() - getPaddingBottom() - getPaddingTop();
            final int bottom = getChildAt(0).getHeight();
            final int maxY = Math.max(0, bottom - height);
            final int scrollY = getScrollY();
            dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;

            mScroller.startScroll(scrollX, scrollY, dx, dy);
            postInvalidateOnAnimation();
        } else {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    /**
     * Like {@link #scrollTo}, but scroll smoothly instead of immediately.
     *
     * @param x the position where to scroll on the X axis
     * @param y the position where to scroll on the Y axis
     */
    public final void smoothScrollTo(int x, int y) {
        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }

    /**
     * <p>The scroll range of a scroll view is the overall width of all of its
     * children.</p>
     */
    @Override
    protected int computeHorizontalScrollRange() {
        final int count = getChildCount();
        final int contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        if (count == 0) {
            return contentWidth;
        }

        int scrollRange = getChildAt(0).getRight();
        final int scrollX = getScrollX();
        final int overscrollRight = Math.max(0, scrollRange - contentWidth);
        if (scrollX < 0) {
            scrollRange -= scrollX;
        } else if (scrollX > overscrollRight) {
            scrollRange += scrollX - overscrollRight;
        }

        return scrollRange;
    }

    /**
     * <p>The scroll range of a scroll view is the overall height of all of its
     * children.</p>
     */
    @Override
    protected int computeVerticalScrollRange() {
        final int count = getChildCount();
        final int contentHeight = getHeight() - getPaddingBottom() - getPaddingTop();
        if (count == 0) {
            return contentHeight;
        }

        int scrollRange = getChildAt(0).getBottom();
        final int scrollY = getScrollY();
        final int overscrollBottom = Math.max(0, scrollRange - contentHeight);
        if (scrollY < 0) {
            scrollRange -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRange += scrollY - overscrollBottom;
        }

        return scrollRange;
    }

    @Override
    protected int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec,
                                int parentHeightMeasureSpec) {
        final int horizontalPadding = getPaddingLeft() + getPaddingRight();
        final int verticalPadding = getPaddingTop() + getPaddingBottom();

        final int childWidthMeasureSpec = makeSafeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentWidthMeasureSpec) - horizontalPadding),
                MeasureSpec.UNSPECIFIED);

        final int childHeightMeasureSpec = makeSafeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - verticalPadding),
                MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed,
                                           int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int usedTotal = getPaddingTop() + getPaddingBottom()
                + lp.topMargin + lp.bottomMargin + heightUsed;
        final int childHeightMeasureSpec = makeSafeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentHeightMeasureSpec) - usedTotal),
                MeasureSpec.UNSPECIFIED);
        final int childWidthMeasureSpec = makeSafeMeasureSpec(
                Math.max(0, MeasureSpec.getSize(parentWidthMeasureSpec) - usedTotal),
                MeasureSpec.UNSPECIFIED);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            // This is called at drawing time by ViewGroup.  We don't want to
            // re-show the scrollbars at this point, which scrollTo will do,
            // so we replicate most of scrollTo here.
            //
            //         It's a little odd to call onScrollChanged from inside the drawing.
            //
            //         It is, except when you remember that computeScroll() is used to
            //         animate scrolling. So unless we want to defer the onScrollChanged()
            //         until the end of the animated scrolling, we don't really have a
            //         choice here.
            //
            //         I agree.  The alternative, which I think would be worse, is to post
            //         something and tell the subclasses later.  This is bad because there
            //         will be a window where mScrollX/Y is different from what the app
            //         thinks it is.
            //
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();

            if (oldX != x || oldY != y) {
                final int rangeX = getScrollRangeHorizontal();
                final int rangeY = getScrollRangeVertical();
                final int overscrollMode = getOverScrollMode();
                final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                        overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS
                                && (rangeX > 0 || rangeY > 0);

                overScrollBy(x - oldX, y - oldY, oldX, oldY, rangeX, rangeY,
                        0, 0, false);

                if (canOverscroll) {
                    if (x < 0 && oldX >= 0) {
                        mEdgeGlowLeft.onAbsorb((int) mScroller.getCurrVelocity());
                    } else if (x > rangeX && oldX <= rangeX) {
                        mEdgeGlowRight.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                    if (y < 0 && oldY >= 0) {
                        mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                    } else if (y > rangeY && oldY <= rangeY) {
                        mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                }
            }

            if (!awakenScrollBars()) {
                // Keep on drawing until the animation has finished.
                postInvalidateOnAnimation();
            }
        }
    }

    /**
     * Scrolls the view to the given child.
     *
     * @param child the View to scroll to
     */
    private void scrollToChild(View child) {
        child.getDrawingRect(mTempRect);

        /* Offset from child's local coordinates to MergedScrollView coordinates */
        offsetDescendantRectToMyCoords(child, mTempRect);

        int scrollDeltaX = computeScrollDeltaToGetChildRectOnScreenX(mTempRect);
        int scrollDeltaY = computeScrollDeltaToGetChildRectOnScreenY(mTempRect);

        if (scrollDeltaX != 0 || scrollDeltaY != 0) {
            scrollBy(scrollDeltaX, scrollDeltaY);
        }
    }

    /**
     * If rect is off screen, scroll just enough to get it (or at least the
     * first screen size chunk of it) on screen.
     *
     * @param rect      The rectangle.
     * @param immediate True to scroll immediately without animation
     * @return true if scrolling was performed
     */
    private boolean scrollToChildRect(Rect rect, boolean immediate) {
        final int deltaX = computeScrollDeltaToGetChildRectOnScreenX(rect);
        final int deltaY = computeScrollDeltaToGetChildRectOnScreenY(rect);
        final boolean scroll = deltaX != 0 || deltaY != 0;
        if (scroll) {
            if (immediate) {
                scrollBy(deltaX, deltaY);
            } else {
                smoothScrollBy(deltaX, deltaY);
            }
        }
        return scroll;
    }

    /**
     * Compute the amount to scroll in the X direction in order to get
     * a rectangle completely on the screen (or, if taller than the screen,
     * at least the first screen size chunk of it).
     *
     * @param rect The rect.
     * @return The scroll delta.
     */
    protected int computeScrollDeltaToGetChildRectOnScreenX(Rect rect) {
        if (getChildCount() == 0) return 0;

        int width = getWidth();
        int screenLeft = getScrollX();
        int screenRight = screenLeft + width;

        int fadingEdge = getHorizontalFadingEdgeLength();

        // leave room for left fading edge as long as rect isn't at very left
        if (rect.left > 0) {
            screenLeft += fadingEdge;
        }

        // leave room for right fading edge as long as rect isn't at very right
        if (rect.right < getChildAt(0).getWidth()) {
            screenRight -= fadingEdge;
        }

        int scrollXDelta = 0;

        if (rect.right > screenRight && rect.left > screenLeft) {
            // need to move right to get it in view: move right just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            if (rect.width() > width) {
                // just enough to get screen size chunk on
                scrollXDelta += rect.left - screenLeft;
            } else {
                // get entire rect at right of screen
                scrollXDelta += rect.right - screenRight;
            }

            // make sure we aren't scrolling beyond the end of our content
            int right = getChildAt(0).getRight();
            int distanceToRight = right - screenRight;
            scrollXDelta = Math.min(scrollXDelta, distanceToRight);

        } else if (rect.left < screenLeft && rect.right < screenRight) {
            // need to move right to get it in view: move right just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            if (rect.width() > width) {
                // screen size chunk
                scrollXDelta -= screenRight - rect.right;
            } else {
                // entire rect at left
                scrollXDelta -= screenLeft - rect.left;
            }

            // make sure we aren't scrolling any further than the left our content
            scrollXDelta = Math.max(scrollXDelta, -getScrollX());
        }
        return scrollXDelta;
    }

    /**
     * Compute the amount to scroll in the Y direction in order to get
     * a rectangle completely on the screen (or, if taller than the screen,
     * at least the first screen size chunk of it).
     *
     * @param rect The rect.
     * @return The scroll delta.
     */
    protected int computeScrollDeltaToGetChildRectOnScreenY(Rect rect) {
        if (getChildCount() == 0) return 0;

        int height = getHeight();
        int screenTop = getScrollY();
        int screenBottom = screenTop + height;

        int fadingEdge = getVerticalFadingEdgeLength();

        // leave room for top fading edge as long as rect isn't at very top
        if (rect.top > 0) {
            screenTop += fadingEdge;
        }

        // leave room for bottom fading edge as long as rect isn't at very bottom
        if (rect.bottom < getChildAt(0).getHeight()) {
            screenBottom -= fadingEdge;
        }

        int scrollYDelta = 0;

        if (rect.bottom > screenBottom && rect.top > screenTop) {
            // need to move down to get it in view: move down just enough so
            // that the entire rectangle is in view (or at least the first
            // screen size chunk).

            if (rect.height() > height) {
                // just enough to get screen size chunk on
                scrollYDelta += rect.top - screenTop;
            } else {
                // get entire rect at bottom of screen
                scrollYDelta += rect.bottom - screenBottom;
            }

            // make sure we aren't scrolling beyond the end of our content
            int bottom = getChildAt(0).getBottom();
            int distanceToBottom = bottom - screenBottom;
            scrollYDelta = Math.min(scrollYDelta, distanceToBottom);

        } else if (rect.top < screenTop && rect.bottom < screenBottom) {
            // need to move up to get it in view: move up just enough so that
            // entire rectangle is in view (or at least the first screen
            // size chunk of it).

            if (rect.height() > height) {
                // screen size chunk
                scrollYDelta -= screenBottom - rect.bottom;
            } else {
                // entire rect at top
                scrollYDelta -= screenTop - rect.top;
            }

            // make sure we aren't scrolling any further than the top our content
            scrollYDelta = Math.max(scrollYDelta, -getScrollY());
        }
        return scrollYDelta;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        if (!mIsLayoutDirty) {
            scrollToChild(focused);
        } else {
            // The child may not be laid out yet, we can't compute the scroll yet
            mChildToScrollTo = focused;
        }
        super.requestChildFocus(child, focused);
    }

    /**
     * When looking for focus in children of a scroll view, need to be a little
     * more careful not to give focus to something that is scrolled off screen.
     * <p>
     * This is more expensive than the default {@link android.view.ViewGroup}
     * implementation, otherwise this behavior might have been made the default.
     */
    @Override
    protected boolean onRequestFocusInDescendants(int direction,
                                                  Rect previouslyFocusedRect) {

        // convert from forward / backward notation to up / down / left / right
        // (ugh).
        if (direction == View.FOCUS_FORWARD) {
            direction = View.FOCUS_DOWN;
        } else if (direction == View.FOCUS_BACKWARD) {
            direction = View.FOCUS_UP;
        }

        final View nextFocus = previouslyFocusedRect == null ?
                FocusFinder.getInstance().findNextFocus(this, null, direction) :
                FocusFinder.getInstance().findNextFocusFromRect(this,
                        previouslyFocusedRect, direction);

        return nextFocus != null
                && !isOffScreen(nextFocus)
                && nextFocus.requestFocus(direction, previouslyFocusedRect);
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle,
                                                 boolean immediate) {
        // offset into coordinate space of this scroll view
        rectangle.offset(child.getLeft() - child.getScrollX(),
                child.getTop() - child.getScrollY());

        return scrollToChildRect(rectangle, immediate);
    }

    @Override
    public void requestLayout() {
        mIsLayoutDirty = true;
        super.requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mIsLayoutDirty = false;
        // Give a child focus if it needs it
        if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo, this)) {
            scrollToChild(mChildToScrollTo);
        }
        mChildToScrollTo = null;

        if (!this.isLaidOut()) {
            final int childWidth = getChildCount() > 0 ? getChildAt(0).getMeasuredWidth() : 0;
            final int childHeight = getChildCount() > 0 ? getChildAt(0).getMeasuredHeight() : 0;
            final int scrollRangeX = Math.max(0,
                    childWidth - (r - l - getPaddingLeft() - getPaddingRight()));
            if (mSavedState != null) {
                //noinspection ResourceType
                setScrollX(isLayoutRtl()
                        ? scrollRangeX - mSavedState.scrollOffsetFromStart
                        : mSavedState.scrollOffsetFromStart);
                setScrollY(mSavedState.scrollPositionY);
                mSavedState = null;
            } else {
                if (isLayoutRtl()) {
                    setScrollX(scrollRangeX - getScrollX());
                }
            }

            final int scrollRangeY = Math.max(0,
                    childHeight - (b - t - getPaddingBottom() - getPaddingTop()));

            // Don't forget to clamp
            if (getScrollX() > scrollRangeX) {
                setScrollX(scrollRangeX);
            } else if (getScrollX() < 0) {
                setScrollX(0);
            }
            if (getScrollY() > scrollRangeY) {
                setScrollY(scrollRangeY);
            } else if (getScrollY() < 0) {
                setScrollY(0);
            }
        }

        // Calling this with the present values causes it to re-claim them
        scrollTo(getScrollX(), getScrollY());
    }

    public boolean isLayoutRtl() {
        return this.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        View currentFocused = findFocus();
        if (null == currentFocused || this == currentFocused)
            return;

        final int maxJump = getWidth();

        // If the currently-focused view was visible on the screen when the
        // screen was at the old height, then scroll the screen to make that
        // view visible with the new screen height.
        if (isWithinDeltaOfScreen(currentFocused, maxJump, oldh)) {
            currentFocused.getDrawingRect(mTempRect);
            offsetDescendantRectToMyCoords(currentFocused, mTempRect);
            int scrollDeltaX = computeScrollDeltaToGetChildRectOnScreenX(mTempRect);
            int scrollDeltaY = computeScrollDeltaToGetChildRectOnScreenY(mTempRect);
            doScroll(scrollDeltaX, scrollDeltaY);
        }
    }

    /**
     * Fling the scroll view
     *
     * @param velocityX The initial velocity in the X direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the left.
     * @param velocityY The initial velocity in the X direction. Positive
     *                  numbers mean that the finger/cursor is moving down the screen,
     *                  which means we want to scroll towards the top.
     */
    public void fling(int velocityX, int velocityY) {
        if (getChildCount() > 0) {
            int height = getHeight() - getPaddingBottom() - getPaddingTop();
            int width = getWidth() - getPaddingRight() - getPaddingLeft();
            int right = getChildAt(0).getWidth();
            int bottom = getChildAt(0).getHeight();

            mScroller.fling(getScrollX(), getScrollY(), velocityX, velocityY, 0,
                    Math.max(0, right - width), 0, Math.max(0, bottom - height));

            postInvalidateOnAnimation();
        }
    }

    private void endDrag() {
        mIsBeingDragged = false;

        recycleVelocityTracker();

        if (mEdgeGlowTop != null) {
            mEdgeGlowLeft.onRelease();
            mEdgeGlowRight.onRelease();
            mEdgeGlowTop.onRelease();
            mEdgeGlowBottom.onRelease();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This version also clamps the scrolling to the bounds of our child.
     */
    @Override
    public void scrollTo(int x, int y) {
        // we rely on the fact the View.scrollBy calls scrollTo.
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            x = clamp(x, getWidth() - getPaddingRight() - getPaddingLeft(), child.getWidth());
            y = clamp(y, getHeight() - getPaddingBottom() - getPaddingTop(), child.getHeight());
            if (x != getScrollX() || y != getScrollY()) {
                super.scrollTo(x, y);
            }
        }
    }

    @Override
    public void setOverScrollMode(int mode) {
        if (mode != OVER_SCROLL_NEVER) {
            if (mEdgeGlowTop == null) {
                Context context = getContext();
                mEdgeGlowTop = new EdgeEffect(context);
                mEdgeGlowBottom = new EdgeEffect(context);
                mEdgeGlowLeft = new EdgeEffect(context);
                mEdgeGlowRight = new EdgeEffect(context);
            }
        } else {
            mEdgeGlowTop = null;
            mEdgeGlowBottom = null;
            mEdgeGlowLeft = null;
            mEdgeGlowRight = null;
        }
        super.setOverScrollMode(mode);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mEdgeGlowTop != null) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            if (!mEdgeGlowLeft.isFinished()) {
                final int restoreCount = canvas.save();
                final int height = getHeight() - getPaddingTop() - getPaddingBottom();

                canvas.rotate(270);
                canvas.translate(-height + getPaddingTop() - scrollY, Math.min(0, scrollX));
                mEdgeGlowLeft.setSize(height, getWidth());
                if (mEdgeGlowLeft.draw(canvas)) {
                    postInvalidateOnAnimation();
                }
                canvas.restoreToCount(restoreCount);
            }
            if (!mEdgeGlowRight.isFinished()) {
                final int restoreCount = canvas.save();
                final int width = getWidth();
                final int height = getHeight() - getPaddingTop() - getPaddingBottom();

                canvas.rotate(90);
                canvas.translate(
                        -getPaddingTop() + scrollY,
                        -(Math.max(getScrollRangeHorizontal(), scrollX) + width));
                mEdgeGlowRight.setSize(height, width);
                if (mEdgeGlowRight.draw(canvas)) {
                    postInvalidateOnAnimation();
                }
                canvas.restoreToCount(restoreCount);
            }

            if (!mEdgeGlowTop.isFinished()) {
                final int restoreCount = canvas.save();
                final int width;
                final int height;
                final float translateX;
                final float translateY;
                if (getClipToPadding()) {
                    width = getWidth() - getPaddingLeft() - getPaddingRight();
                    height = getHeight() - getPaddingTop() - getPaddingBottom();
                    translateX = getPaddingLeft();
                    translateY = getPaddingTop();
                } else {
                    width = getWidth();
                    height = getHeight();
                    translateX = 0;
                    translateY = 0;
                }
                canvas.translate(translateX + scrollX, Math.min(0, scrollY) + translateY);
                mEdgeGlowTop.setSize(width, height);
                if (mEdgeGlowTop.draw(canvas)) {
                    postInvalidateOnAnimation();
                }
                canvas.restoreToCount(restoreCount);
            }
            if (!mEdgeGlowBottom.isFinished()) {
                final int restoreCount = canvas.save();
                final int width;
                final int height;
                final float translateX;
                final float translateY;
                if (getClipToPadding()) {
                    width = getWidth() - getPaddingLeft() - getPaddingRight();
                    height = getHeight() - getPaddingTop() - getPaddingBottom();
                    translateX = getPaddingLeft();
                    translateY = getPaddingTop();
                } else {
                    width = getWidth();
                    height = getHeight();
                    translateX = 0;
                    translateY = 0;
                }
                canvas.translate(
                        -width + translateX + scrollX,
                        Math.max(getScrollRangeVertical(), scrollY) + height + translateY);
                canvas.rotate(180, width, 0);
                mEdgeGlowBottom.setSize(width, height);
                if (mEdgeGlowBottom.draw(canvas)) {
                    postInvalidateOnAnimation();
                }
                canvas.restoreToCount(restoreCount);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (getContext().getApplicationInfo().targetSdkVersion <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // Some old apps reused IDs in ways they shouldn't have.
            // Don't break them, but they don't get scroll state restoration.
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        mSavedState = ss;
        requestLayout();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        if (getContext().getApplicationInfo().targetSdkVersion <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // Some old apps reused IDs in ways they shouldn't have.
            // Don't break them, but they don't get scroll state restoration.
            return super.onSaveInstanceState();
        }
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.scrollOffsetFromStart = isLayoutRtl() ? -getScrollX() : getScrollX();
        ss.scrollPositionY = getScrollY();
        return ss;
    }

    private static class SavedState extends BaseSavedState {
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int scrollOffsetFromStart;
        int scrollPositionY;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel source) {
            super(source);
            scrollOffsetFromStart = source.readInt();
            scrollPositionY = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(scrollOffsetFromStart);
            dest.writeInt(scrollPositionY);
        }

        @NonNull
        @Override
        public String toString() {
            return "MergedScrollView.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " scrollPositionX=" + scrollOffsetFromStart + "}"
                    + " scrollPositionY=" + scrollPositionY + "}";
        }
    }
}
