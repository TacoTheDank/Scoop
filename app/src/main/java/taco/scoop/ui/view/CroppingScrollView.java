package taco.scoop.ui.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class CroppingScrollView extends MergedScrollView {

    private boolean mCropHorizontally;

    public CroppingScrollView(Context context) {
        this(context, null);
    }

    public CroppingScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CroppingScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mCropHorizontally = false;
    }

    public void setCropHorizontally(boolean crop) {
        if (mCropHorizontally != crop) {
            mCropHorizontally = crop;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mCropHorizontally) return;

        if (getChildCount() > 0) {
            final View child = getChildAt(0);
            final int widthPadding;
            final int targetSdkVersion = getContext().getApplicationInfo().targetSdkVersion;
            final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                widthPadding = getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin;
            } else {
                widthPadding = getPaddingLeft() + getPaddingRight();
            }
            final int desiredWidth = getMeasuredWidth() - widthPadding;

            if (mCropHorizontally && child.getMeasuredWidth() > desiredWidth) {
                child.measure(
                        MeasureSpec.makeMeasureSpec(desiredWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            }
        }
    }
}
