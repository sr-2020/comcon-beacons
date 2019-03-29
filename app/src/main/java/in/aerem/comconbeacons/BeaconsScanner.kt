package `in`.aerem.comconbeacons

import `in`.aerem.comconbeacons.models.BeaconData
import `in`.aerem.comconbeacons.models.PositionsRequest
import `in`.aerem.comconbeacons.models.PositionsResponse
import `in`.aerem.comconbeacons.models.getBackendUrl
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import org.altbeacon.beacon.*
import org.altbeacon.beacon.powersave.BackgroundPowerSaver
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory




class BeaconsScanner : Service(), BeaconConsumer {
    private val TAG = "ComConBeacons"
    private lateinit var mBackgroundPowerSaver: BackgroundPowerSaver
    private lateinit var mBeaconManager: BeaconManager
    private lateinit var mService: PositionsWebService
    private lateinit var mSecurityToken: String

    override fun onCreate() {
        Log.d(TAG, "BeaconsScanner::onCreate")
        mService = Retrofit.Builder()
            .baseUrl(getBackendUrl(application, this))
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(PositionsWebService::class.java)

        mSecurityToken = (application as ComConBeaconsApplication).getGlobalSharedPreferences()
            .getString(getString(R.string.token_preference_key), "")!!

        setUpBeaconManager()
    }

    private fun setUpBeaconManager() {
        Beacon.setHardwareEqualityEnforced(true)

        mBeaconManager = BeaconManager.getInstanceForApplication(this)
        mBeaconManager.beaconParsers.clear()
        // Detect BLE beacons (02).
        // Example advertising data of ble_app_beacon nRF example
        //  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24
        // 59 00 02 15 01 12 23 34 45 56 67 78 89 9A AB BC CD DE EF F0 01 02 03 04 C3
        var parser = BeaconParser().setBeaconLayout("m:2-2=02,i:4-19,i:20-21,i:22-23,p:24-24");
        // 0x0059 is Apple manufacturer code (default for iBeacons).
        // 0x0059 is Nordic Semiconductors manufacturer code.
        parser.setHardwareAssistManufacturerCodes(intArrayOf(0x59))
        mBeaconManager.beaconParsers.add(parser)

        BeaconManager.setDebug(true);

        mBeaconManager.foregroundScanPeriod = 3000 // 3 seconds

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = Notification.Builder(this)
            .setSmallIcon(R.drawable.abc_ic_star_black_48dp)
            .setContentTitle(getString(R.string.notification_content_title))
            .setContentIntent(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "My Notification Channel ID",
                "My Notification Name", NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "My Notification Channel Description"
            val notificationManager = getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId(channel.id)
        }
        mBeaconManager.enableForegroundServiceScanning(builder.build(), 1713)
        mBeaconManager.setEnableScheduledScanJobs(false)
        mBeaconManager.backgroundBetweenScanPeriod = 10000
        mBeaconManager.backgroundScanPeriod = 3000 // 3 seconds
        mBeaconManager.bind(this)

        mBackgroundPowerSaver = BackgroundPowerSaver(this)
    }

    override fun onDestroy() {
        try {
            // TODO: Figure out proper order of "unsubscribe" operations here.
            // Current one doesn't trigger any warnings/errors/crashes from AltBeacon library,
            // but for some reason after subscribe-unsubscribe-subscribe every event is generated twice
            // ( as if there were 2 addRangeNotifier calls).
            // Also see https://github.com/AltBeacon/android-beacon-library/issues/764
            mBeaconManager.removeAllRangeNotifiers()
            mBeaconManager.stopRangingBeaconsInRegion(Region("myRangingUniqueId", null, null, null))
            mBeaconManager.unbind(this)
            mBeaconManager.disableForegroundServiceScanning()
        } catch (e: RemoteException) {
            Log.e(TAG, "RemoteException: $e")
        }

    }

    override fun onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect")
        val rangeNotifier = { beacons: Collection<Beacon>, _: Region ->
            Log.d(TAG, "didRangeBeaconsInRegion called with beacon count:  " + beacons.size)
            sendBeacons(beacons)
        }
        try {
            mBeaconManager.startRangingBeaconsInRegion(Region("myRangingUniqueId", null, null, null))
            mBeaconManager.addRangeNotifier(rangeNotifier)
        } catch (e: RemoteException) {
            Log.e(TAG, "RemoteException: $e")
        }

    }

    private fun sendBeacons(beacons: Collection<Beacon>) {
        var beaconsList = mutableListOf<BeaconData>()
        for (b in beacons) {
            Log.d(TAG, "Beacon id = " + b.id1 + "-" + b.id2 + "-" + b.id3 + " RSSI = " + b.rssi)
            beaconsList.add(BeaconData(b.id1.toString(), b.bluetoothAddress, b.rssi))
        }
        val req = PositionsRequest(beaconsList)

        val c = mService.positions(mSecurityToken, req)
        c.enqueue(object : Callback<PositionsResponse> {
            override fun onResponse(call: Call<PositionsResponse>, response: Response<PositionsResponse>) {
                Log.i(TAG, "Http request succeeded, response = " + response.body())
            }

            override fun onFailure(call: Call<PositionsResponse>, t: Throwable) {
                Log.e(TAG, "Http request failed: $t")
            }
        })
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
