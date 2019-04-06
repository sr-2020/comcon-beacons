package `in`.aerem.comconbeacons

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import android.arch.persistence.room.Query


@Dao
interface UsersDao {
    @Insert(onConflict = REPLACE)
    fun saveUsers(users: List<UserInfo>)

    @Insert(onConflict = REPLACE)
    fun saveFavorite(favorite: UserIsFavorite)

    @Query(
        "SELECT userInfo.id, userInfo.username, userInfo.location, userInfo.date, userInfo.status, userIsFavorite.favorite " +
                "FROM userInfo LEFT JOIN userIsFavorite ON userInfo.id = userIsFavorite.id")
    fun load(): LiveData<List<UserListItem>>
}
