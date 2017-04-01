package tk.wasdennnoch.scoop.data.app;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tk.wasdennnoch.scoop.R;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private ArrayList<App> mItems = new ArrayList<>();
    private ArrayList<App> mSearchedItems = new ArrayList<>();
    private boolean mSearchActive = false;

    public void setApps(ArrayList<App> items, List<String> blacklisted) {
        mItems = items;
        for (App a : mItems)
            for (String pkg : blacklisted)
                if (a.packageName.equals(pkg))
                    a.selected = true;
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    public ArrayList<String> getSelectedPackages() {
        ArrayList<String> items = new ArrayList<>();
        for (App a : mItems)
            if (a.selected)
                items.add(a.packageName);
        return items;
    }

    public void search(String text) {
        if (text != null)
            text = text.toLowerCase(Locale.ENGLISH);
        mSearchedItems.clear();
        mSearchActive = !TextUtils.isEmpty(text);
        if (mSearchActive) {
            for (App a : mItems) {
                if (a.packageName.toLowerCase(Locale.ENGLISH).contains(text)
                        || a.name.toLowerCase(Locale.ENGLISH).contains(text)) {
                    mSearchedItems.add(a);
                }
            }
        }
        notifyDataSetChanged();
    }

    private void toggleItemSelection(int index) {
        App a = mSearchActive ? mSearchedItems.get(index) : mItems.get(index);
        a.selected = !a.selected;
    }

    @Override
    public AppAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AppAdapter.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blacklist, parent, false));
    }

    @Override
    public void onBindViewHolder(AppAdapter.ViewHolder holder, int position) {
        App app = mSearchActive ? mSearchedItems.get(position) : mItems.get(position);
        holder.app = app;
        holder.appIcon.setImageDrawable(app.icon);
        holder.appName.setText(app.name);
        holder.packageName.setText(app.packageName);
        holder.check.setChecked(app.selected);
        holder.itemView.setOnClickListener(holder);
    }

    @Override
    public int getItemCount() {
        return mSearchActive ? mSearchedItems.size() : mItems.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        App app;
        ImageView appIcon;
        TextView appName;
        TextView packageName;
        CheckBox check;

        ViewHolder(View itemView) {
            super(itemView);
            appIcon = (ImageView) itemView.findViewById(R.id.appIcon);
            appName = (TextView) itemView.findViewById(R.id.appName);
            packageName = (TextView) itemView.findViewById(R.id.packageName);
            check = (CheckBox) itemView.findViewById(R.id.check);
        }

        @Override
        public void onClick(View v) {
            toggleItemSelection(getAdapterPosition());
            check.setChecked(!check.isChecked());
        }
    }

}