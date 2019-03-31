package `in`.aerem.comconbeacons.models

import `in`.aerem.comconbeacons.PositionsWebService
import `in`.aerem.comconbeacons.UserListItem
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.os.Handler
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

// TODO: Consider adding Repository/Room for local caching
// See Android Jetpack architecture guide for details:
// https://developer.android.com/jetpack/docs/guide
// Also see concrete example: https://medium.com/@guendouz/room-livedata-and-recyclerview-d8e96fb31dfe
class UserListViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ComConBeacons"
    private val mRawUserList = MutableLiveData<List<UserListItem>>()
    private val mUserList = Transformations.map(mRawUserList) { item -> sortAndFilterResults(item) }!!
    private var mFilterString = ""

    enum class SortBy {
        NAME,
        LOCATION,
        STATUS,
        FRESHNESS,
    }
    private var mSortBy: SortBy = SortBy.FRESHNESS

    private val mService: PositionsWebService = Retrofit.Builder()
        .baseUrl(getBackendUrl(getApplication(), getApplication()))
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(PositionsWebService::class.java)
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
                    mRawUserList.postValue(recentEntries)
                }

                override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                    Log.e(TAG, "Http request failed: $t")
                }
            })
            mHandler.removeCallbacks(this)
            mHandler.postDelayed(this, 10000)
        }
    }

    fun getUsersList(): LiveData<List<UserListItem>?> {
        return mUserList
    }

    fun setFilter(filter: String) {
        mFilterString = filter;
        rePostLiveData()
    }

    fun pauseUpdates() {
        mHandler.removeCallbacks(mListUpdateRunnable)
    }

    fun updateAndResumeUpdates() {
        mListUpdateRunnable.run()
    }

    fun getSortBy(): SortBy {
        return mSortBy
    }

    fun setSortBy(sortBy: SortBy) {
        mSortBy = sortBy
        rePostLiveData()
    }

    private fun sortAndFilterResults(lines: List<UserListItem>?): List<UserListItem>? {
        if (lines == null) return null
        return sortedResults(filteredResults(lines))
    }

    private fun filteredResults(lines: List<UserListItem>): List<UserListItem> {
        return lines.filter { it ->
            it.username.toLowerCase().contains(mFilterString) ||
                    it.location.toLowerCase().contains(mFilterString)
        }
    }

    private fun sortedResults(lines: List<UserListItem>): List<UserListItem> {
        return when (mSortBy) {
            SortBy.FRESHNESS -> lines.sortedBy { item -> item.date }.reversed()
            SortBy.LOCATION -> lines.sortedBy { item -> item.location }
            SortBy.NAME -> lines.sortedBy { item -> item.username.toLowerCase() }
            SortBy.STATUS -> lines.sortedBy { item -> item.status }
        }
    }

    private fun rePostLiveData() {
        mRawUserList.postValue(mRawUserList.value)
    }
}