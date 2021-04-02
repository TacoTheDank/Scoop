package tk.wasdennnoch.scoop.data.app;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tk.wasdennnoch.scoop.databinding.ItemBlacklistBinding;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    private final ArrayList<App> mSearchedItems = new ArrayList<>();
    private ArrayList<App> mItems = new ArrayList<>();
    private boolean mSearchActive = false;

    public void setApps(ArrayList<App> items, List<String> blacklisted) {
        mItems = items;
        for (App a : mItems)
            for (String pkg : blacklisted)
                if (a.getPackageName().equals(pkg)) {
                    a.selected = true;
                    break;
                }
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    public ArrayList<String> getSelectedPackages() {
        ArrayList<String> items = new ArrayList<>();
        for (App a : mItems)
            if (a.selected)
                items.add(a.getPackageName());
        return items;
    }

    public void search(String text) {
        if (text != null)
            text = text.toLowerCase(Locale.ENGLISH);
        mSearchedItems.clear();
        mSearchActive = !TextUtils.isEmpty(text);
        if (mSearchActive) {
            for (App a : mItems) {
                if (a.getPackageName().toLowerCase(Locale.ENGLISH).contains(text)
                        || a.getName().toLowerCase(Locale.ENGLISH).contains(text)
                ) {
                    mSearchedItems.add(a);
                }
            }
        }
        notifyDataSetChanged();
    }

    private void toggleItemSelection(int index) {
        App a = (mSearchActive ? mSearchedItems : mItems).get(index);
        a.selected = !a.selected;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AppViewHolder(
                ItemBlacklistBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(AppViewHolder holder, int position) {
        App app = (mSearchActive ? mSearchedItems : mItems).get(position);
        holder.app = app;
        holder.appIcon.setImageDrawable(app.getIcon());
        holder.appName.setText(app.getName());
        holder.packageName.setText(app.getPackageName());
        holder.check.setChecked(app.selected);
        holder.itemView.setOnClickListener(holder);
    }

    @Override
    public int getItemCount() {
        return (mSearchActive ? mSearchedItems : mItems).size();
    }

    class AppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final ImageView appIcon;
        final TextView appName;
        final TextView packageName;
        final CheckBox check;
        final ItemBlacklistBinding binding;
        App app;

        AppViewHolder(final ItemBlacklistBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            appIcon = binding.blacklistItemAppIcon;
            appName = binding.blacklistItemAppName;
            packageName = binding.blacklistItemPackageName;
            check = binding.blacklistItemCheckBox;
        }

        @Override
        public void onClick(View v) {
            toggleItemSelection(getAdapterPosition());
            check.setChecked(!check.isChecked());
        }
    }
}
