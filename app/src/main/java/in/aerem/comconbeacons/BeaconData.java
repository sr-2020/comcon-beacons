package in.aerem.comconbeacons;

public class BeaconData {
    String ssid;
    String bssid;
    int level;

    public BeaconData(String ssid, String bssid, int level) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.level = level;
    }
}
