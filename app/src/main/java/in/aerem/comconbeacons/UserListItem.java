package in.aerem.comconbeacons;

public class UserListItem {
    public String username;
    public String location;
    public String time;

    public UserListItem(UsersResponse r) {
        username = r.email;
        location = r.beacon == null ? "None" : r.beacon.bssid;
        time = r.updated_at;
    }
}
