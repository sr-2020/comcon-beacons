package `in`.aerem.comconbeacons.models

import `in`.aerem.comconbeacons.PositionsWebService
import `in`.aerem.comconbeacons.UserListItem
import `in`.aerem.comconbeacons.UsersDatabase
import `in`.aerem.comconbeacons.UsersRepository
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.persistence.room.Room
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


// TODO: Consider adding Repository/Room for local caching
// See Android Jetpack architecture guide for details:
// https://developer.android.com/jetpack/docs/guide
// Also see concrete example: https://medium.com/@guendouz/room-livedata-and-recyclerview-d8e96fb31dfe
class UserListViewModel(application: Application) : AndroidViewModel(application) {
    private var mFilterString = ""

    enum class SortBy {
        NAME,
        LOCATION,
        STATUS,
        FRESHNESS,
    }
    private var mSortBy: SortBy = SortBy.FRESHNESS

    private val mUsersRepository = UsersRepository(Retrofit.Builder()
        .baseUrl(getBackendUrl(getApplication(), getApplication()))
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(PositionsWebService::class.java),
        Room.databaseBuilder(
            getApplication(),
            UsersDatabase::class.java, "users-db"
        ).build().usersDao())

    private val mUsersList = mUsersRepository.getUsers()

    private val mLiveDataMerger = MediatorLiveData<List<UserListItem>>()
    private val mLiveDataSortingFilteringChanged = MutableLiveData<Number>()

    init {
        mLiveDataMerger.addSource(mUsersList)
            { item -> mLiveDataMerger.value = sortAndFilterResults(item)}
        mLiveDataMerger.addSource(mLiveDataSortingFilteringChanged)
            { mLiveDataMerger.value = sortAndFilterResults(mUsersList.value) }
    }

    fun getUsersList(): LiveData<List<UserListItem>?> {
        return mLiveDataMerger
    }

    fun setFilter(filter: String) {
        mFilterString = filter;
        resortAndRefilter()
    }

    fun getSortBy(): SortBy {
        return mSortBy
    }

    fun setSortBy(sortBy: SortBy) {
        mSortBy = sortBy
        resortAndRefilter()
    }

    fun pauseUpdates() = mUsersRepository.pauseUpdates()
    fun updateAndResumeUpdates() = mUsersRepository.updateAndResumeUpdates()

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

    private fun resortAndRefilter() {
        // Hack to trigger mLiveDataMerger update, see init { ... }
        mLiveDataSortingFilteringChanged.value = 0
    }
}