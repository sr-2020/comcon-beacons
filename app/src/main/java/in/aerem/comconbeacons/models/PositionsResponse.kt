package `in`.aerem.comconbeacons.models

class PositionsResponse(
    var beacons: List<BeaconData>,
    var routers: List<String>,
    var created_at: String,
    var id: Int,
    var user_id: Int
)
