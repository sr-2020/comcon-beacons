package `in`.aerem.comconbeacons.models

import `in`.aerem.comconbeacons.R

fun possibleStatuses(): List<String> = arrayListOf("free", "busy", "adventure")

fun statusToResourceId(status: String): Int {
    return when (status) {
        "free" -> R.drawable.free
        "busy" -> R.drawable.busy
        "adventure" -> R.drawable.adventure
        else -> R.drawable.unknown
    }
}
