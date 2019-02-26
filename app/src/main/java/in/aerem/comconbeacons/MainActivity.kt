package `in`.aerem.comconbeacons

import android.Manifest
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.content.SharedPreferences
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
import `in`.aerem.comconbeacons.models.UsersResponse
import android.annotation.SuppressLint
import android.app.SearchManager
import android.arch.lifecycle.Observer
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SearchView
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
                mService.users().enqueue(object : Callback<List<UsersResponse>> {
                    override fun onResponse(call: Call<List<UsersResponse>>, response: Response<List<UsersResponse>>) {
                        Log.i(TAG, "Http request succeeded, response = " + response.body())
                        val lines = ArrayList<UserListItem>()
                        for (u in response.body()!!) {
                            // Hack to filter loadtesting accounts
                            if (!u.email.contains("@grr.la")) {
                                lines.add(UserListItem(u))
                            }
                        }
                        mLiveData.postValue(lines.sortedBy { item -> item.username })
                    }

                    override fun onFailure(call: Call<List<UsersResponse>>, t: Throwable) {
                        Log.e(TAG, "Http request failed: $t")
                    }
                })
                mHandler.postDelayed(this, 10000)
            }
        }
        recyclerView.adapter = adapter
    }

    public override fun onPause() {
        super.onPause()
        mHandler.removeCallbacks(mListUpdateRunnable)
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

    private fun bssidOnNone(b: UsersResponse.Beacon?): String {
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
