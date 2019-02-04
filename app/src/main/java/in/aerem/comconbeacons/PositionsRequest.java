package in.aerem.comconbeacons;

import java.util.List;

public class PositionsRequest {
    public List<BeaconData> beacons;

    public PositionsRequest(List<BeaconData> beacons) {
        this.beacons = beacons;
    }
}
