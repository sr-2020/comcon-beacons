package in.aerem.comconbeacons;

import java.util.List;

public class PositionsResponse {
    public List<BeaconData> beacons;
    public List<String> routers;
    public String created_at;
    public int id;
    public int user_id;


    public PositionsResponse(List<BeaconData> beacons, List<String> routers, String created_at, int id, int user_id) {
        this.beacons = beacons;
        this.routers = routers;
        this.created_at = created_at;
        this.id = id;
        this.user_id = user_id;
    }
}
