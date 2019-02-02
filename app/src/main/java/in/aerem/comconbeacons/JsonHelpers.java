package in.aerem.comconbeacons;

import android.util.Log;
import org.altbeacon.beacon.Beacon;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

public class JsonHelpers {
    private static final String TAG = "ComConBeacons";

    static JSONObject loginPayload(String email, String password) {
        JSONObject result = new JSONObject();
        try {
            result.put("email", email);
            result.put("password", password);
        } catch (JSONException e) {
            // TODO: get rid of it by using JSONObject constructor
            Log.e(TAG,"JSON creation failed: " + e);
        }
        return result;
    }

    static JSONObject positionsPayload() {
        JSONObject result = new JSONObject();
        try {
            JSONObject beaconData = new JSONObject();
            beaconData.put("ssid", "Unknown");
            beaconData.put("bssid", "b0:0a:95:9d:00:0a");
            beaconData.put("level", -50);

            JSONArray beacons = new JSONArray();
            beacons.put(beaconData);

            result.put("beacons", beacons);
        } catch (JSONException e) {
            // TODO: get rid of it by using JSONObject constructor
            Log.e(TAG,"JSON creation failed: " + e);
        }
        return result;
    }
}



