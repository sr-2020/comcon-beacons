package in.aerem.comconbeacons;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import in.aerem.comconbeacons.models.BeaconData;
import in.aerem.comconbeacons.models.PositionsRequest;
import in.aerem.comconbeacons.models.PositionsResponse;
import org.altbeacon.beacon.*;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.Collection;

public class BeaconsScanner extends Service implements BeaconConsumer {
    private static final String TAG = "ComConBeacons";
    private BackgroundPowerSaver mBackgroundPowerSaver;
    private BeaconManager mBeaconManager;
    private PositionsWebService mService;
    private String mSecurityToken;

    @Override
    public void onCreate() {
        Log.d(TAG, "BeaconsScanner::onCreate");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.backend_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mService = retrofit.create(PositionsWebService.class);

        mBeaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);
        mBeaconManager.getBeaconParsers().clear();
        // Detect iBeacons (0215).
        // Example advertising data of ble_app_beacon nRF example
        //  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24
        // 59 00 02 15 01 12 23 34 45 56 67 78 89 9A AB BC CD DE EF F0 01 02 03 04 C3
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));

        // Detect weird Ksotar's iTags
        // Example advertising data:
        //  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26
        // 00 03 19 C1 03 03 02 E0 FF 11 09 69 54 41 47 20 20 20 20 20 20 20 20 20 20 20 20
        mBeaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:2-4=19C103,i:5-20,i:21-22,i:23-24,p:25-25"));

        // mBeaconManager.setDebug(true);

        Notification.Builder builder = new Notification.Builder(this);
        // builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentTitle(getString(R.string.notification_content_title));
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(pendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("My Notification Channel ID",
                    "My Notification Name", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("My Notification Channel Description");
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(channel.getId());
        }
        mBeaconManager.enableForegroundServiceScanning(builder.build(), 1713);

        mBeaconManager.setEnableScheduledScanJobs(false);
        mBeaconManager.setBackgroundBetweenScanPeriod(10000);
        mBeaconManager.setBackgroundScanPeriod(1100);
        mBeaconManager.bind(this);

        mBackgroundPowerSaver = new BackgroundPowerSaver(this);

        mSecurityToken = ((ComConBeaconsApplication) getApplication()).getGlobalSharedPreferences()
                .getString(getString(R.string.token_preference_key), null);
    }

    @Override
    public void onDestroy() {
        try {
            // TODO: Figure out proper order of "unsubscribe" operations here.
            // Current one doesn't trigger any warnings/errors/crashes from AltBeacon library,
            // but for some reason after subscribe-unsubscribe-subscribe every event is generated twice
            // ( as if there were 2 addRangeNotifier calls).
            // Also see https://github.com/AltBeacon/android-beacon-library/issues/764
            mBeaconManager.removeAllRangeNotifiers();
            mBeaconManager.stopRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
            mBeaconManager.unbind(this);
            mBeaconManager.disableForegroundServiceScanning();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException: " + e);
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect");
        RangeNotifier rangeNotifier = (beacons, region) -> {
            Log.d(TAG, "didRangeBeaconsInRegion called with beacon count:  " + beacons.size());
            sendBeacons(beacons);
        };
        try {
            mBeaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
            mBeaconManager.addRangeNotifier(rangeNotifier);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException: " + e);
        }
    }

    private void sendBeacons(Collection<Beacon> beacons) {
        PositionsRequest req = new PositionsRequest(new ArrayList<>());
        for (Beacon b : beacons) {
            req.getBeacons().add(new BeaconData(b.getId1().toString(), b.getBluetoothAddress(), b.getRssi()));
        }

        Call<PositionsResponse> c = mService.positions(mSecurityToken, req);
        c.enqueue(new Callback<PositionsResponse>() {
            @Override
            public void onResponse(Call<PositionsResponse> call, Response<PositionsResponse> response) {
                Log.i(TAG, "Http request succeeded, response = " + response.body());
            }

            @Override
            public void onFailure(Call<PositionsResponse> call, Throwable t) {
                Log.e(TAG, "Http request failed: " + t);
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
