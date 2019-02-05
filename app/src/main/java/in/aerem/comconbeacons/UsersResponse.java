package in.aerem.comconbeacons;

public class UsersResponse {
    public class Beacon {
        public int id;
        public String ssid;
        public String bssid;
    }

    public int id;
    public int router_id;
    public int beacon_id;
    public String name;
    public String email;
    public String updated_at;
    public Beacon beacon;
}
