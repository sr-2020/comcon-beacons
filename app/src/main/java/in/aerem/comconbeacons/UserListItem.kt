package `in`.aerem.comconbeacons

import `in`.aerem.comconbeacons.models.UsersResponse
import org.ocpsoft.prettytime.PrettyTime
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class UserListItem {
    var username: String
    var location: String
    var time: String
    var status: String

    constructor(r: UsersResponse) {
        username = valueOr(r.name, r.email)
        status = r.status
        val b = r.beacon;
        location = if (b == null) {
            "None"
        } else {
            valueOr(b.label, b.bssid)
        }
        time = humanReadableDateInfo(r.updated_at)
    }

    private var format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private fun valueOr(value: String?, defaultValue: String): String {
        return if (value == null || value.isEmpty()) defaultValue else value
    }

    private fun humanReadableDateInfo(rawDate: String): String {
        format.timeZone = TimeZone.getTimeZone("UTC")
        try {
            val p = PrettyTime(Locale("ru"))
            val date = format.parse(rawDate)
            return p.format(date)
        } catch (e: ParseException) {
            return ""
        }

    }
}
