package tk.wasdennnoch.scoop.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import tk.wasdennnoch.scoop.R;

public class CrashRecyclerView extends RecyclerView {

    private LinearLayoutManager mManager;

    public CrashRecyclerView(Context context) {
        this(context, null);
    }

    public CrashRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CrashRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mManager = new LinearLayoutManager(context);
        setLayoutManager(mManager);
        addItemDecoration(new VerticalSpaceItemDecoration(getResources().getDimensionPixelSize(R.dimen.crash_list_divider_size)));
        addItemDecoration(new DividerItemDecoration(context));
    }

    public void setReverseOrder(boolean reverse) {
        mManager.setReverseLayout(reverse);
        mManager.setStackFromEnd(reverse);
    }

    private static class VerticalSpaceItemDecoration extends RecyclerView.ItemDecoration {

        private final int mVerticalSpaceHeight;

        VerticalSpaceItemDecoration(int mVerticalSpaceHeight) {
            this.mVerticalSpaceHeight = mVerticalSpaceHeight;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {
            outRect.bottom = mVerticalSpaceHeight;
        }
    }

    private static class DividerItemDecoration extends RecyclerView.ItemDecoration {

        private Drawable mDivider;

        DividerItemDecoration(Context context) {
            final TypedArray styledAttributes = context.obtainStyledAttributes(new int[]{android.R.attr.listDivider});
            mDivider = styledAttributes.getDrawable(0);
            styledAttributes.recycle();
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }

}
