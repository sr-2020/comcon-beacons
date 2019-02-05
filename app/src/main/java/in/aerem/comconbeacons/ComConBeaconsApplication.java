package in.aerem.comconbeacons;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ComConBeaconsApplication extends Application {
    public SharedPreferences getGlobalSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }
}
