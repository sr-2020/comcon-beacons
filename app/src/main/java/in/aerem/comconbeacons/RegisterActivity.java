package in.aerem.comconbeacons;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import in.aerem.comconbeacons.models.LoginResponse;
import in.aerem.comconbeacons.models.RegisterRequest;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "ComConBeacons";

    // Keep track of the login task to ensure we can cancel it if requested.
    private UserRegisterTask mRegisterTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mNameView;
    private EditText mPasswordView;
    private EditText mPasswordRepeatView;
    private View mProgressView;
    private View mLoginFormView;

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Set up the login form.
        mEmailView = findViewById(R.id.email);
        mNameView = findViewById(R.id.name);
        mPasswordView = findViewById(R.id.password);
        mPasswordRepeatView = findViewById(R.id.password_repeat);
        mPasswordRepeatView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptRegister();
                    return true;
                }
                return false;
            }
        });

        Button registerButton = findViewById(R.id.email_register_button);
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        mSharedPreferences = ((ComConBeaconsApplication) getApplication()).getGlobalSharedPreferences();
    }

    private void attemptRegister() {
        if (mRegisterTask != null) {
            return;
        }
        RegisterFormData formData = getRegisterFormData();
        if (formData != null) {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mRegisterTask = new UserRegisterTask(formData);
            mRegisterTask.execute((Void) null);
        }
    }

    private class RegisterFormData {
        public String email;
        public String name;
        public String password;

        public  RegisterFormData(String email, String name, String password) {
            this.email = email;
            this.name = name;
            this.password = password;
        }
    }

    private RegisterFormData getRegisterFormData() {
        mEmailView.setError(null);
        mNameView.setError(null);
        mPasswordView.setError(null);
        mPasswordRepeatView.setError(null);

        String email = mEmailView.getText().toString();
        String name = mNameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String passwordRepeat = mPasswordRepeatView.getText().toString();

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            mEmailView.requestFocus();
            return null;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(name)) {
            mNameView.setError(getString(R.string.error_field_required));
            mNameView.requestFocus();
            return null;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_empty_password));
            mPasswordView.requestFocus();
            return null;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(passwordRepeat)) {
            mPasswordRepeatView.setError(getString(R.string.error_empty_password));
            mPasswordRepeatView.requestFocus();
            return null;
        }

        // Check for a valid password, if the user entered one.
        if (!TextUtils.equals(password, passwordRepeat)) {
            mPasswordRepeatView.setError(getString(R.string.error_password_not_repeated));
            mPasswordRepeatView.requestFocus();
            return null;
        }

        return new RegisterFormData(email, name, password);
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    protected void onSuccessfulRegister(String token) {
        Log.i(TAG,"Successful registration, token = " + token);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(getString(R.string.token_preference_key), token);
        editor.commit();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private class UserRegisterTask extends AsyncTask<Void, Void, String> {
        protected final RegisterRequest mRegisterRequest;
        PositionsWebService mService;

        UserRegisterTask(RegisterFormData mFormData) {
            mRegisterRequest = new RegisterRequest(mFormData.email, mFormData.name, mFormData.password);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(getString(R.string.backend_url))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            mService = retrofit.create(PositionsWebService.class);
        }
        @Override
        protected String doInBackground(Void... voids) {
            Call<LoginResponse> c = mService.register(mRegisterRequest);
            try {
                Response<LoginResponse> response = c.execute();
                if (response.isSuccessful()) {
                    return response.body().getApi_key();
                }
                Log.e(TAG,"Unsuccessful response: " + response.errorBody());
            } catch (IOException e) {
                Log.e(TAG,"IOException: " + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(final String apiKey) {
            onFinish();

            if (apiKey == null) {
                mEmailView.setError(getString(R.string.error_user_already_exist));
                mEmailView.requestFocus();
            } else {
                onSuccessfulRegister(apiKey);
            }
        }

        @Override
        protected void onCancelled() {
            onFinish();
        }

        private void onFinish() {
            mRegisterTask = null;
            showProgress(false);
        }
    }
}

