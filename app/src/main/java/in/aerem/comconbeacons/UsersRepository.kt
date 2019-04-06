package `in`.aerem.comconbeacons

import `in`.aerem.comconbeacons.models.UserResponse
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.Handler
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class UsersRepository(val mService: PositionsWebService) {
    private val TAG = "ComConBeacons"
    private val mUserList = MutableLiveData<List<UserListItem>>()
    private val mHandler = Handler()
    private val mListUpdateRunnable = object : Runnable {
        override fun run() {
            mService.users().enqueue(object : Callback<List<UserResponse>> {
                override fun onResponse(call: Call<List<UserResponse>>, response: Response<List<UserResponse>>) {
                    Log.i(TAG, "Http request succeeded, response = " + response.body())
                    var lines = ArrayList<UserListItem>()
                    for (u in response.body()!!) {
                        lines.add(UserListItem(u))
                    }

                    // Last seven days
                    var recentEntries = lines.filter { item -> Date().time - item.date.time < 1000 * 60 * 60 * 24 * 7 }
                    // More recent entries first
                    mUserList.postValue(recentEntries)
                }

                override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                    Log.e(TAG, "Http request failed: $t")
                }
            })
            mHandler.removeCallbacks(this)
            mHandler.postDelayed(this, 10000)
        }
    }

    fun getUsers(): LiveData<List<UserListItem>> {
        return mUserList
    }

    fun pauseUpdates() {
        mHandler.removeCallbacks(mListUpdateRunnable)
    }

    fun updateAndResumeUpdates() {
        mListUpdateRunnable.run()
    }
}