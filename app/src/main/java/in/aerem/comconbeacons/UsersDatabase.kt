package `in`.aerem.comconbeacons

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters


@Database(entities = [UserInfo::class, UserIsFavorite::class], version = 2)
@TypeConverters(Converters::class)
abstract class UsersDatabase : RoomDatabase() {
    abstract fun usersDao(): UsersDao
}