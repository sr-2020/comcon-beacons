package in.aerem.comconbeacons;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class UsersPositionsAdapter extends RecyclerView.Adapter<UsersPositionsAdapter.ViewHolder> {
    private List<UserListItem> mDataset = new ArrayList<>();

    public void setData(List<UserListItem> newData) {
        this.mDataset = newData;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        UserListItem u = mDataset.get(position);
        holder.mUsernameView.setText(u.username);
        holder.mLocationView.setText(u.location);
        holder.mTimeView.setText(u.time);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mUsernameView;
        public TextView mLocationView;
        public TextView mTimeView;

        public ViewHolder(View v) {
            super(v);
            mUsernameView = itemView.findViewById(R.id.username);
            mLocationView = itemView.findViewById(R.id.location);
            mTimeView = itemView.findViewById(R.id.time);
        }
    }
}
