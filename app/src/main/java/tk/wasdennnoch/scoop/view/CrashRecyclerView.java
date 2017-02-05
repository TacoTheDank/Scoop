package tk.wasdennnoch.scoop.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

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
        addItemDecoration(new DividerItemDecoration(context, mManager.getOrientation()));
    }

    public void setReverseOrder(boolean reverse) {
        mManager.setReverseLayout(reverse);
        mManager.setStackFromEnd(reverse);
    }

}
