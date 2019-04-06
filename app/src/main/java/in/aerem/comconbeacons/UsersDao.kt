package `in`.aerem.comconbeacons

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query


@Dao
interface UsersDao {
    @Insert(onConflict = REPLACE)
    fun saveAll(users: List<UserListItem>)

    @Query("SELECT * FROM userListItem")
    fun load(): LiveData<List<UserListItem>>
}