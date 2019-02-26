package `in`.aerem.comconbeacons

import `in`.aerem.comconbeacons.models.statusToResourceId
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import java.util.ArrayList

class UsersPositionsAdapter : RecyclerView.Adapter<UsersPositionsAdapter.ViewHolder>() {
    private var mDataset: List<UserListItem> = ArrayList()

    fun setData(newData: List<UserListItem>) {
        this.mDataset = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_view_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val u = mDataset[position]
        holder.mStatusIconView.setImageResource(statusToResourceId(u.status))
        holder.mUsernameView.text = u.username
        holder.mLocationView.text = u.location
        holder.mTimeView.text = u.time
    }

    override fun getItemCount(): Int {
        return mDataset.size
    }

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var mStatusIconView: ImageView = itemView.findViewById(R.id.statusIcon)
        var mUsernameView: TextView = itemView.findViewById(R.id.username)
        var mLocationView: TextView = itemView.findViewById(R.id.location)
        var mTimeView: TextView = itemView.findViewById(R.id.time)
    }
}
