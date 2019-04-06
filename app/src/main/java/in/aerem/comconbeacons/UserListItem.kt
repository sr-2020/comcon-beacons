package `in`.aerem.comconbeacons

import `in`.aerem.comconbeacons.models.UserResponse
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverter
import java.text.SimpleDateFormat
import java.util.*



enum class Status {
    ADVENTURE,
    FREE,
    BUSY,
    UNKNOWN
}

@Entity
data class UserListItem(
    @PrimaryKey
    val id: Int,
    val username: String,
    val location: String,
    val date: Date,
    val status: Status
)

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long): Date {
        return Date(value)
    }

    @TypeConverter
    fun dateToTimestamp(date: Date): Long {
        return date.time
    }

    @TypeConverter
    fun fromString(value: String): Status {
        return when (value) {
            "adventure" -> Status.ADVENTURE
            "free" -> Status.FREE
            "busy" -> Status.BUSY
            else -> Status.UNKNOWN
        }
    }

    @TypeConverter
    fun toString(s: Status): String {
        return s.toString().toLowerCase()
    }
}

fun fromResponse(r: UserResponse): UserListItem {
    val  status = when (r.status) {
        "adventure" -> Status.ADVENTURE
        "free" -> Status.FREE
        "busy" -> Status.BUSY
        else -> Status.UNKNOWN
    }
    val l = r.location;
    val location = l?.label ?: "None"

    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    format.timeZone = TimeZone.getTimeZone("Europe/Moscow")
    val date = format.parse(r.updated_at)

    return UserListItem(r.id, r.name ?: "Anonymous", location, date, status)
}