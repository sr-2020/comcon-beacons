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

    static JSONObject positionsPayload(Collection<Beacon> beacons) {
        JSONObject result = new JSONObject();
        try {
            JSONArray beaconsJson = new JSONArray();
            for (Beacon beacon: beacons) {
                JSONObject beaconJson = new JSONObject();
                beaconJson.put("ssid", beacon.getId1());
                beaconJson.put("bssid", beacon.getBluetoothAddress());
                beaconJson.put("level", beacon.getRssi());
                beaconsJson.put(beaconJson);
            }
            result.put("beacons", beaconsJson);
        } catch (JSONException e) {
            // TODO: get rid of it by using JSONObject constructor
            Log.e(TAG,"JSON creation failed: " + e);
        }
        return result;
    }
}



