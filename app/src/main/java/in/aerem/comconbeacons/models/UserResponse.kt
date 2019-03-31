package `in`.aerem.comconbeacons.models

class UserResponse (
    var id: Int,
    var router_id: Int,
    var beacon_id: Int,
    var name: String?,
    var location_updated_at: String,
    var location: Location?,
    var status: String
) {
    class Location (
        var id: Int,
        var label: String
    )
}
