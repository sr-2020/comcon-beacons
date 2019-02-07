package `in`.aerem.comconbeacons.models

class UsersResponse (
    var id: Int,
    var router_id: Int,
    var beacon_id: Int,
    var name: String,
    var email: String,
    var updated_at: String,
    var beacon: Beacon
) {

    class Beacon(
        var id: Int,
        var ssid: String,
        var bssid: String,
        var label: String
    )
}