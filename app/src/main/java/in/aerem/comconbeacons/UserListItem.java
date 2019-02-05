package in.aerem.comconbeacons;

import org.ocpsoft.prettytime.PrettyTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class UserListItem {
    static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public String username;
    public String location;
    public String time;

    public UserListItem(UsersResponse r) {
        username = r.email;
        location = r.beacon == null ? "None" : r.beacon.bssid;
        time = humanReadableDateInfo(r.updated_at);
    }

    private static String humanReadableDateInfo(String rawDate) {
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            PrettyTime p = new PrettyTime(new Locale("ru"));
            Date date = format.parse(rawDate);
            return p.format(date);
        } catch (ParseException e) {
            return "";
        }
    }
}
