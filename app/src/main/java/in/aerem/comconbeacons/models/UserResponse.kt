package `in`.aerem.comconbeacons.models

class UserResponse (
    var id: Int,
    var router_id: Int,
    var beacon_id: Int,
    var name: String?,
    var updated_at: String,
    var beacon: Beacon?,
    var status: String
) {
    class Beacon(
        var id: Int,
        var ssid: String,
        var bssid: String,
        var label: String?
    )
}
