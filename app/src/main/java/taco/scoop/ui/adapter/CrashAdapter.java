package taco.scoop.ui.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import taco.scoop.R;
import taco.scoop.core.data.crash.Crash;
import taco.scoop.core.data.crash.CrashLoader;
import taco.scoop.databinding.ItemCrashBinding;
import taco.scoop.ui.view.RelativeTimeTextView;
import taco.scoop.util.Utils;

public class CrashAdapter extends RecyclerView.Adapter<CrashAdapter.CrashViewHolder> {

    private final Listener mListener;
    private final int mSelectedColor;
    private ArrayList<Crash> mItems = new ArrayList<>();
    private ArrayList<Crash> mSearchedItems = new ArrayList<>();
    private boolean mSearchActive;
    private boolean mSearchPackageName = true;
    private String mLastSearchTerm;
    private boolean mSelectionEnabled;
    private int mSelectedCount;
    private boolean mCombineSameApps;

    public CrashAdapter(Context context, Listener listener) {
        mListener = listener;
        mSelectedColor = Utils.getAttrColor(context, android.R.attr.colorControlHighlight);
    }

    public void setSearchPackageName(Context context, boolean searchPkg) {
        mSearchPackageName = searchPkg;
        search(context, mLastSearchTerm);
    }

    public void setCombineSameApps(boolean combine) {
        mCombineSameApps = combine;
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

    public void removeCrash(Crash c) {
        int pos = getPosition(c);
        if (pos == -1) return;
        if (mItems.get(pos).selected) mSelectedCount--;
        mItems.remove(pos);
        notifyItemRemoved(pos);
    }

    private int getPosition(Crash c) {
        for (int i = 0; i < mItems.size(); i++) {
            if (mItems.get(i) == c) {
                return i;
            }
        }
        return -1;
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    public void search(Context context, String text) {
        if (text != null)
            text = text.toLowerCase(Locale.ENGLISH);
        mLastSearchTerm = text;
        mSearchedItems.clear();
        mSearchActive = !TextUtils.isEmpty(text);
        if (mSearchActive) {
            for (Crash c : mItems) { // Search app name and package (if configured)
                if (mSearchPackageName
                        && c.packageName.toLowerCase(Locale.ENGLISH).contains(text)
                        || CrashLoader.getAppName(context, c.packageName, false)
                        .toLowerCase(Locale.ENGLISH).contains(text)
                ) {
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

    // return immutable list
    public List<Crash> getItems() {
        return Collections.unmodifiableList(mItems);
    }

    public ArrayList<Crash> getSelectedItems() {
        ArrayList<Crash> items = new ArrayList<>();
        for (Crash c : mItems)
            if (c.selected)
                items.add(c);
        return items;
    }

    private void setItemSelected(int index, boolean selected) {
        Crash c = (mSearchActive ? mSearchedItems : mItems).get(index);
        if (c.selected == selected) return;
        c.selected = selected;
        if (c.selected) {
            mSelectedCount++;
        } else {
            mSelectedCount--;
        }
        notifyItemChanged(index);
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

    @NonNull
    @Override
    public CrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CrashViewHolder(
                ItemCrashBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(CrashViewHolder holder, int position) {
        Context context = holder.itemView.getContext();
        Crash crash = (mSearchActive ? mSearchedItems : mItems).get(position);
        String pkg = crash.packageName;
        CharSequence title;
        holder.crash = crash;
        holder.icon.setImageDrawable(CrashLoader.getAppIcon(context, pkg));
        holder.time.setReferenceTime(crash.time);
        String name = CrashLoader.getAppName(context, pkg, false);
        if (!mCombineSameApps) {
            if (crash.count > 1) {
                title = context.getString(R.string.crash_count, name, crash.count);
                title = new SpannableString(title);
                int textColorSecondary = Utils.getAttrColor(context, android.R.attr.textColorSecondary);
                ((Spannable) title).setSpan(new ForegroundColorSpan(textColorSecondary),
                        name.length(), title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                title = name;
            }
            holder.crashText.setText(crash.description);
        } else {
            title = name;
            holder.crashText.setText(context.getResources().getQuantityString(R.plurals.items_children_count, crash.displayCount, crash.displayCount));
        }
        holder.title.setText(title);
        holder.itemView.setOnClickListener(holder);
        holder.itemView.setOnLongClickListener(holder);
        holder.setSelected(crash.selected);
    }

    @Override
    public int getItemCount() {
        return (mSearchActive ? mSearchedItems : mItems).size();
    }

    public interface Listener {
        void onCrashClicked(Crash crash);

        void onToggleSelectionMode(boolean enabled);

        void onItemSelected(int totalCount);
    }

    class CrashViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        final ImageView icon;
        final TextView title;
        final RelativeTimeTextView time;
        final TextView crashText;
        final Drawable normalBackground;
        final ItemCrashBinding binding;
        Crash crash;

        CrashViewHolder(final ItemCrashBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            icon = binding.crashItemAppIcon;
            title = binding.crashItemAppName;
            time = binding.crashItemTime;
            crashText = binding.crashItemCrashLog;
            normalBackground = binding.getRoot().getBackground();
        }

        @Override
        public void onClick(View v) {
            if (mSelectionEnabled) {
                setItemSelected(getBindingAdapterPosition(), !crash.selected);
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
                setItemSelected(getBindingAdapterPosition(), !crash.selected);
            }
            return false;
        }

        void setSelected(boolean selected) {
            if (selected) {
                itemView.setBackgroundColor(mSelectedColor);
            } else {
                itemView.setBackground(normalBackground);
            }
        }
    }

}
