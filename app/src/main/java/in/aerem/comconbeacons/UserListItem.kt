package `in`.aerem.comconbeacons

import `in`.aerem.comconbeacons.models.UserResponse
import java.text.SimpleDateFormat
import java.util.*

class UserListItem {
    val id: Number
    val username: String
    val location: String
    val date: Date

    enum class Status {
        ADVENTURE,
        FREE,
        BUSY,
        UNKNOWN
    }

    val status: Status

    constructor(r: UserResponse) {
        id = r.id
        username = valueOr(r.name, "Anonymous")
        status = when (r.status) {
            "adventure" -> Status.ADVENTURE
            "free" -> Status.FREE
            "busy" -> Status.BUSY
            else -> Status.UNKNOWN
        }
        val l = r.location;
        location = l?.label ?: "None"
        date = getDate(r.location_updated_at)
    }

    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private fun valueOr(value: String?, defaultValue: String): String {
        return if (value == null || value.isEmpty()) defaultValue else value
    }

    private fun getDate(rawDate: String): Date {
        format.timeZone = TimeZone.getTimeZone("Europe/Moscow")
        return format.parse(rawDate)
    }
}
