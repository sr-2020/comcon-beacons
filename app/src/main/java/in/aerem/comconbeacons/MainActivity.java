package in.aerem.comconbeacons;

import android.Manifest;
import android.arch.lifecycle.MutableLiveData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ComConBeacons";
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private PositionsWebService mService;
    private Handler mHandler = new Handler();
    private Runnable mListUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getString(R.string.backend_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        mService = retrofit.create(PositionsWebService.class);

        // See RecyclerView guide for details if needed
        // https://developer.android.com/guide/topics/ui/layout/recyclerview
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        MutableLiveData<List<String>> liveData = new MutableLiveData<>();
        UsersPositionsAdapter adapter = new UsersPositionsAdapter();
        liveData.observe(this, data -> adapter.setData(data));

        mListUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                mService.users().enqueue(new Callback<List<UsersResponse>>() {
                    @Override
                    public void onResponse(Call<List<UsersResponse>> call, Response<List<UsersResponse>> response) {
                        Log.i(TAG, "Http request succeeded, response = " + response.body());
                        List<String> lines = new ArrayList<>();
                        for (UsersResponse u : response.body()) {
                            lines.add(u.email + " --> " + bssidOnNone(u.beacon) + " @" + u.updated_at);
                        }
                        liveData.postValue(lines);
                    }

                    @Override
                    public void onFailure(Call<List<UsersResponse>> call, Throwable t) {
                        Log.e(TAG, "Http request failed: " + t);
                    }
                });
                mHandler.postDelayed(this, 10000);
            }
        };
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mListUpdateRunnable);
    }

    private String bssidOnNone(UsersResponse.Beacon b) {
        if (b == null) return "None";
        return b.bssid;
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.startService(new Intent(this, BeaconsScanner.class));
        mListUpdateRunnable.run();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_exit) {
            SharedPreferences preferences = ((ComConBeaconsApplication) getApplication()).getGlobalSharedPreferences();
            preferences.edit().remove(getString(R.string.token_preference_key)).commit();
            this.stopService(new Intent(this, BeaconsScanner.class));
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
