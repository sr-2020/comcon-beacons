package `in`.aerem.comconbeacons

import `in`.aerem.comconbeacons.models.ProfileRequest
import `in`.aerem.comconbeacons.models.UserListViewModel
import `in`.aerem.comconbeacons.models.UserResponse
import `in`.aerem.comconbeacons.models.getBackendUrl
import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.animation.OvershootInterpolator
import android.widget.SearchView
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private val TAG = "ComConBeacons"
    private val PERMISSION_REQUEST_COARSE_LOCATION = 1
    private val REQUEST_ENABLE_BT = 2
    private lateinit var mStatusMenu: FloatingActionMenu
    private lateinit var mSecurityToken: String
    private lateinit var mService: PositionsWebService
    private lateinit var mModel: UserListViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mService = Retrofit.Builder()
            .baseUrl(getBackendUrl(application,this))
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(PositionsWebService::class.java)

        mSecurityToken = (application as ComConBeaconsApplication).getGlobalSharedPreferences()
            .getString(getString(R.string.token_preference_key), "")!!

        // See RecyclerView guide for details if needed
        // https://developer.android.com/guide/topics/ui/layout/recyclerview
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = UsersPositionsAdapter()
        mModel = ViewModelProviders.of(this).get(UserListViewModel::class.java)
        mModel.getUsersList().observe(this,
            Observer { data: List<UserListItem>? -> if (data != null) adapter.setData(data) })

        recyclerView.adapter = adapter

        mStatusMenu = findViewById(R.id.menu_status)

        findViewById<FloatingActionButton>(R.id.menu_status_free).setOnClickListener { setStatus("free") }
        findViewById<FloatingActionButton>(R.id.menu_status_adventure).setOnClickListener { setStatus("adventure") }
        findViewById<FloatingActionButton>(R.id.menu_status_busy).setOnClickListener { setStatus("busy") }

        createCustomAnimation()
    }


    public override fun onPause() {
        super.onPause()
        mModel.pauseUpdates()
    }

    private fun setStatus(s: String) {
        val r = ProfileRequest(s)
        val c = mService.profile(mSecurityToken, r)
        c.enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                mModel.updateAndResumeUpdates()
                mStatusMenu.close(true)
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                Log.e(TAG, "Http request failed: $t")
            }
        })
     }

    private fun createCustomAnimation() {
        val set = AnimatorSet()

        val scaleOutX = ObjectAnimator.ofFloat(mStatusMenu.menuIconView, "scaleX", 1.0f, 0.2f)
        val scaleOutY = ObjectAnimator.ofFloat(mStatusMenu.menuIconView, "scaleY", 1.0f, 0.2f)

        val scaleInX = ObjectAnimator.ofFloat(mStatusMenu.menuIconView, "scaleX", 0.2f, 1.0f)
        val scaleInY = ObjectAnimator.ofFloat(mStatusMenu.menuIconView, "scaleY", 0.2f, 1.0f)

        scaleOutX.duration = 50
        scaleOutY.duration = 50

        scaleInX.duration = 150
        scaleInY.duration = 150

        scaleInX.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                mStatusMenu.menuIconView.setImageResource(
                    if (mStatusMenu.isOpened) R.drawable.status_white else R.drawable.close)
            }
        })

        set.play(scaleOutX).with(scaleOutY)
        set.play(scaleInX).with(scaleInY).after(scaleOutX)
        set.interpolator = OvershootInterpolator(2.0f)

        mStatusMenu.iconToggleAnimatorSet = set
    }

    override fun onResume() {
        super.onResume()
        checkEverythingEnabled()
        this.startService(Intent(this, BeaconsScanner::class.java))
        mModel.updateAndResumeUpdates()
    }

    private fun checkEverythingEnabled() {
        checkBluetoothEnabled()
        checkLocationPermission()
        checkLocationService()
    }

    private fun checkBluetoothEnabled() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    private fun checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    PERMISSION_REQUEST_COARSE_LOCATION
                )
            }
        }
    }

    private fun checkLocationService() {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.enable_location))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    startActivityForResult(Intent (Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0)
                }
                .create()
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.toolbar, menu)
        val searchView = (menu.findItem(R.id.action_search).actionView as SearchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(query: String): Boolean {
                mModel.setFilter(query)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                mModel.setFilter(query)
                return true
            }

        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_exit -> exit()
            R.id.action_sort -> showSortByDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("ApplySharedPref")
    private fun exit() {
        val preferences = (application as ComConBeaconsApplication).getGlobalSharedPreferences()
        preferences.edit().remove(getString(R.string.token_preference_key)).commit()
        this.stopService(Intent(this, BeaconsScanner::class.java))
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun showSortByDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.sort_by))
            .setSingleChoiceItems(
                arrayOf(
                    getString(R.string.sort_by_name),
                    getString(R.string.sort_by_location),
                    getString(R.string.sort_by_status),
                    getString(R.string.sort_by_freshness)),
                mModel.getSortBy().ordinal
            ) { dialog: DialogInterface, which: Int ->
                mModel.setSortBy(UserListViewModel.SortBy.values()[which])
                dialog.dismiss()
            }
            .create()
            .show()
    }
}
