package tk.wasdennnoch.scoop.data;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import tk.wasdennnoch.scoop.R;
import tk.wasdennnoch.scoop.view.RelativeTimeTextView;

public class CrashAdapter extends RecyclerView.Adapter<CrashAdapter.CrashViewHolder> {

    private List<Crash> mItems = new ArrayList<>();
    private OnCrashClickListener mListener;

    public CrashAdapter(OnCrashClickListener listener) {
        mListener = listener;
    }

    public void setCrashes(List<Crash> crashes) {
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
        holder.time.setReferenceTime(crash.time);
        holder.crashText.setText(crash.description);
        holder.itemView.setOnClickListener(holder);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public interface OnCrashClickListener {
        void onCrashClicked(Crash crash);
    }

    class CrashViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        Crash crash;
        ImageView icon;
        TextView title;
        RelativeTimeTextView time;
        TextView crashText;

        CrashViewHolder(View v) {
            super(v);
            icon = (ImageView) v.findViewById(R.id.icon);
            title = (TextView) v.findViewById(R.id.title);
            time = (RelativeTimeTextView) v.findViewById(R.id.time);
            crashText = (TextView) v.findViewById(R.id.crash);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null)
                mListener.onCrashClicked(crash);
        }
    }

}
