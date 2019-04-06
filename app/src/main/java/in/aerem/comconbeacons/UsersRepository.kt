package `in`.aerem.comconbeacons

import `in`.aerem.comconbeacons.models.UserResponse
import android.arch.lifecycle.LiveData
import android.os.Handler
import android.util.Log
import org.jetbrains.anko.doAsync
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class UsersRepository(private val mService: PositionsWebService, private val mUsersDao:  UsersDao) {
    private val TAG = "ComConBeacons"
    private val mHandler = Handler()
    private val mListUpdateRunnable = object : Runnable {
        override fun run() {
            mService.users().enqueue(object : Callback<List<UserResponse>> {
                override fun onResponse(call: Call<List<UserResponse>>, response: Response<List<UserResponse>>) {
                    Log.i(TAG, "Http request succeeded, response = " + response.body())
                    var lines = ArrayList<UserInfo>()
                    for (u in response.body()!!) {
                        lines.add(fromResponse(u))
                    }

                    // Last seven days
                    var recentEntries = lines.filter { item -> Date().time - item.date.time < 1000 * 60 * 60 * 24 * 7 }
                    doAsync {
                        mUsersDao.saveUsers(recentEntries)
                    }
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
        return mUsersDao.load()
    }

    fun pauseUpdates() {
        mHandler.removeCallbacks(mListUpdateRunnable)
    }

    fun updateAndResumeUpdates() {
        mListUpdateRunnable.run()
    }

    fun setIsFavorite(id: Int, isFavorite: Boolean) {
        doAsync {
            mUsersDao.saveFavorite(UserIsFavorite(id, isFavorite))
        }
    }
}