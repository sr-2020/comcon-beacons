package in.aerem.comconbeacons;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;
import org.altbeacon.beacon.*;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.Collection;

public class ComConBeaconsApplication extends Application implements BeaconConsumer {
    private static final String TAG = "ComConBeacons";
    private BackgroundPowerSaver backgroundPowerSaver;
    private BeaconManager beaconManager;
    private MainActivity monitoringActivity;
    private boolean haveDetectedBeaconsSinceBoot;

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ComConBeaconsApplication::onCreate");
        beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);

        beaconManager.getBeaconParsers().clear();
        // Only detect nRF (5900) iBeacons (0215).
        // Example advertising data of ble_app_beacon nRF example
        // 0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24
        //59 00 02 15 01 12 23 34 45 56 67 78 89 9A AB BC CD DE EF F0 01 02 03 04 C3
        beaconManager.getBeaconParsers().add(new BeaconParser().
                setBeaconLayout("m:0-3=59000215,i:4-19,i:20-21,i:22-23,p:24-24"));
        beaconManager.setDebug(true);

        Notification.Builder builder = new Notification.Builder(this);
        // builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setContentTitle("Scanning for Beacons");
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
        beaconManager.enableForegroundServiceScanning(builder.build(), 1713);

        beaconManager.setEnableScheduledScanJobs(false);
        beaconManager.setBackgroundBetweenScanPeriod(0);
        beaconManager.setBackgroundScanPeriod(1100);

        Log.d(TAG, "setting up background monitoring for beacons and power saving");

        backgroundPowerSaver = new BackgroundPowerSaver(this);

        beaconManager.bind(this);
    }

    @Override
    public void onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect");
        RangeNotifier rangeNotifier = new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                Log.d(TAG, "didRangeBeaconsInRegion called with beacon count:  "+beacons.size());
                if (beacons.size() > 0) {
                    Beacon firstBeacon = beacons.iterator().next();
                    Log.d(TAG, "The first beacon " + firstBeacon.toString() + " is about " + firstBeacon.getDistance() + " meters away.");
                }
            }

        };
        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
            beaconManager.addRangeNotifier(rangeNotifier);
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
            beaconManager.addRangeNotifier(rangeNotifier);
        } catch (RemoteException e) {   }
    }
}
