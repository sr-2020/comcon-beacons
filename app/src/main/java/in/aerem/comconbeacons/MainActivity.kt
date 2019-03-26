package `in`.aerem.comconbeacons

import `in`.aerem.comconbeacons.models.ProfileRequest
import android.Manifest
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import `in`.aerem.comconbeacons.models.UserResponse
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.SearchManager
import android.arch.lifecycle.Observer
import android.view.animation.OvershootInterpolator
import android.widget.SearchView
import com.github.clans.fab.FloatingActionButton
import com.github.clans.fab.FloatingActionMenu
import kotlinx.android.synthetic.main.activity_main.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import java.util.ArrayList

class MainActivity : AppCompatActivity() {
    private val TAG = "ComConBeacons"
    private val PERMISSION_REQUEST_COARSE_LOCATION = 1
    private lateinit var mService: PositionsWebService
    private val mHandler = Handler()
    private lateinit var mListUpdateRunnable: Runnable

    private var mFilterString = ""
    private val mLiveData = MutableLiveData<List<UserListItem>>()
    private val mFilteredLiveData = MutableLiveData<List<UserListItem>>()

    private lateinit var mStatusMenu: FloatingActionMenu
    private lateinit var mSecurityToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Verify the action and get the query
        if (Intent.ACTION_SEARCH == intent.action) {
            intent.getStringExtra(SearchManager.QUERY)?.also { query -> onSearchQuery(query) }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    PERMISSION_REQUEST_COARSE_LOCATION
                )
            }
        }


        val retrofit = Retrofit.Builder()
            .baseUrl(getString(R.string.backend_url))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        mService = retrofit.create(PositionsWebService::class.java)

        mSecurityToken = (application as ComConBeaconsApplication).getGlobalSharedPreferences()
            .getString(getString(R.string.token_preference_key), "")!!

        // See RecyclerView guide for details if needed
        // https://developer.android.com/guide/topics/ui/layout/recyclerview
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = UsersPositionsAdapter()
        mLiveData.observe(this, Observer { data: List<UserListItem>? -> if (data != null) mFilteredLiveData.postValue(filteredResults(data)) })
        mFilteredLiveData.observe(this, Observer { data: List<UserListItem>? -> if (data != null) adapter.setData(data) })

        // TODO: Consider using better approach when LiveData
        // (https://developer.android.com/topic/libraries/architecture/livedata)
        // is coming from ViewModel class
        // (https://developer.android.com/topic/libraries/architecture/viewmodel)
        // which in turn takes it from Repository. See Android Jetpack architecture guide for details:
        // https://developer.android.com/jetpack/docs/guide
        // Also see concrete example: https://medium.com/@guendouz/room-livedata-and-recyclerview-d8e96fb31dfe
        mListUpdateRunnable = object : Runnable {
            override fun run() {
                mService.users().enqueue(object : Callback<List<UserResponse>> {
                    override fun onResponse(call: Call<List<UserResponse>>, response: Response<List<UserResponse>>) {
                        Log.i(TAG, "Http request succeeded, response = " + response.body())
                        val lines = ArrayList<UserListItem>()
                        for (u in response.body()!!) {
                           lines.add(UserListItem(u))
                        }
                        mLiveData.postValue(lines.sortedBy { item -> item.username })
                    }

                    override fun onFailure(call: Call<List<UserResponse>>, t: Throwable) {
                        Log.e(TAG, "Http request failed: $t")
                    }
                })
                mHandler.postDelayed(this, 10000)
            }
        }
        recyclerView.adapter = adapter

        mStatusMenu = findViewById(R.id.menu_status)

        findViewById<FloatingActionButton>(R.id.menu_status_free).setOnClickListener { setStatus("free") }
        findViewById<FloatingActionButton>(R.id.menu_status_adventure).setOnClickListener { setStatus("adventure") }
        findViewById<FloatingActionButton>(R.id.menu_status_busy).setOnClickListener { setStatus("busy") }

        createCustomAnimation()
    }

    public override fun onPause() {
        super.onPause()
        mHandler.removeCallbacks(mListUpdateRunnable)
    }

    private fun setStatus(s: String) {
        val r = ProfileRequest(s)
        val c = mService.profile(mSecurityToken, r)
        c.enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                mStatusMenu.close(true)
                // Hack to instantly refresh data, as server seems not to be read-after-write consistent
                mLiveData.postValue((mLiveData.value!!.map { u: UserListItem ->
                    if (u.id == response.body()!!.id) {
                        u.status = s
                    }
                    u
                }))
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

    private fun onSearchQuery(filter: String) {
        mFilterString = filter.toLowerCase()
        mLiveData.postValue(mLiveData.value)
    }

    private fun filteredResults(lines: List<UserListItem>): List<UserListItem> {
        return lines.filter { it ->
                it.username.toLowerCase().contains(mFilterString) ||
                it.location.toLowerCase().contains(mFilterString)
        }
    }

    private fun bssidOnNone(b: UserResponse.Beacon?): String {
        return b?.bssid ?: "None"
    }

    override fun onResume() {
        super.onResume()
        this.startService(Intent(this, BeaconsScanner::class.java))
        mListUpdateRunnable.run()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.toolbar, menu)
        val searchView = (menu.findItem(R.id.action_search).actionView as SearchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(query: String): Boolean {
                onSearchQuery(query)
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                onSearchQuery(query)
                return true
            }

        })

        return true
    }
    @SuppressLint("ApplySharedPref")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_exit) {
            val preferences = (application as ComConBeaconsApplication).getGlobalSharedPreferences()
            preferences.edit().remove(getString(R.string.token_preference_key)).commit()
            this.stopService(Intent(this, BeaconsScanner::class.java))
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}
