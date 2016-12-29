package tk.wasdennnoch.scoop.data;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import tk.wasdennnoch.scoop.R;
import tk.wasdennnoch.scoop.view.RelativeTimeTextView;

public class CrashAdapter extends RecyclerView.Adapter<CrashAdapter.CrashViewHolder> {

    private ArrayList<Crash> mItems = new ArrayList<>();
    private ArrayList<Crash> mSearchedItems = new ArrayList<>();
    private boolean mSearchActive = false;
    private boolean mSearchPackageName = true;
    private String mLastSearchTerm;
    private boolean mSelectionEnabled = false;
    private int mSelectedCount;
    private Listener mListener;
    private int mSelectedColor;

    public CrashAdapter(Context context, Listener listener) {
        mListener = listener;
        mSelectedColor = ContextCompat.getColor(context, R.color.selectedBgColor);
    }

    public void setSearchPackageName(Context context, boolean searchPkg) {
        mSearchPackageName = searchPkg;
        search(context, mLastSearchTerm);
    }

    public void setCrashes(ArrayList<Crash> crashes) {
        if (crashes == null)
            mItems.clear();
        else
            mItems = crashes;
        notifyDataSetChanged();
    }

    public void addCrash(Crash c) {
        mItems.add(c);
        notifyItemInserted(mItems.size());
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    public void search(Context context, String text) {
        mLastSearchTerm = text;
        mSearchedItems.clear();
        mSearchActive = !TextUtils.isEmpty(text);
        if (mSearchActive) {
            for (Crash c : mItems) { // Search app name and package (if configured)
                if ((mSearchPackageName && c.packageName.toLowerCase(Locale.ENGLISH).contains(text))
                        || CrashLoader.getAppName(context, c.packageName, false).toLowerCase(Locale.ENGLISH).contains(text)) {
                    mSearchedItems.add(c);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setSelectionEnabled(boolean enabled) {
        if (enabled == mSelectionEnabled) return;
        mSelectionEnabled = enabled;
        if (!enabled) {
            for (int i = 0; i < mItems.size(); i++) {
                setItemSelected(i, false);
            }
        }
        mListener.onToggleSelectionMode(enabled);
    }

    public ArrayList<Crash> getSelectedItems() {
        ArrayList<Crash> items = new ArrayList<>();
        for (Crash c : mItems)
            if (c.selected)
                items.add(c);
        return items;
    }

    private void setItemSelected(int item, boolean selected) {
        Crash c = mItems.get(item);
        if (c.selected == selected) return;
        c.selected = selected;
        if (c.selected) {
            mSelectedCount++;
        } else {
            mSelectedCount--;
        }
        notifyItemChanged(item);
        mListener.onItemSelected(mSelectedCount);
    }

    public void saveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("mItems", mItems);
        outState.putParcelableArrayList("mSearchedItems", mSearchedItems);
        outState.putBoolean("mSearchActive", mSearchActive);
    }

    public void restoreInstanceState(Bundle outState) {
        mItems = outState.getParcelableArrayList("mItems");
        mSearchedItems = outState.getParcelableArrayList("mSearchedItems");
        mSearchActive = outState.getBoolean("mSearchActive");
    }

    @Override
    public CrashViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CrashViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_crash, parent, false));
    }

    @Override
    public void onBindViewHolder(CrashViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        Crash crash = mItems.get(position);
        String pkg = crash.packageName;
        holder.crash = crash;
        holder.icon.setImageDrawable(CrashLoader.getAppIcon(context, pkg));
        holder.title.setText(CrashLoader.getAppName(context, pkg, false));
        holder.count.setVisibility(crash.count > 1 ? View.VISIBLE : View.GONE);
        holder.count.setText(context.getString(R.string.crash_count, crash.count));
        holder.time.setReferenceTime(crash.time);
        holder.crashText.setText(crash.description);
        holder.itemView.setOnClickListener(holder);
        holder.itemView.setOnLongClickListener(holder);
        holder.itemView.setBackgroundColor(crash.selected ? mSelectedColor : Color.TRANSPARENT);
    }

    @Override
    public int getItemCount() {
        return mSearchActive ? mSearchedItems.size() : mItems.size();
    }

    public interface Listener {
        void onCrashClicked(Crash crash);

        void onToggleSelectionMode(boolean enabled);

        void onItemSelected(int totalCount);
    }

    class CrashViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        Crash crash;
        ImageView icon;
        TextView title;
        TextView count;
        RelativeTimeTextView time;
        TextView crashText;

        CrashViewHolder(View v) {
            super(v);
            icon = (ImageView) v.findViewById(R.id.icon);
            title = (TextView) v.findViewById(R.id.title);
            count = (TextView) v.findViewById(R.id.count);
            time = (RelativeTimeTextView) v.findViewById(R.id.time);
            crashText = (TextView) v.findViewById(R.id.crash);
        }

        @Override
        public void onClick(View v) {
            if (mSelectionEnabled) {
                setItemSelected(getAdapterPosition(), !crash.selected);
                if (mSelectedCount == 0)
                    setSelectionEnabled(false);
            } else {
                mListener.onCrashClicked(crash);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (!mSelectionEnabled) {
                setSelectionEnabled(true);
                setItemSelected(getAdapterPosition(), !crash.selected);
            }
            return false;
        }
    }

}
