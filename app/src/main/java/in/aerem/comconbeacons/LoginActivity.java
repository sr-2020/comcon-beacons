package in.aerem.comconbeacons;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
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
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "ComConBeacons";


     // Keep track of the login task to ensure we can cancel it if requested.
    private UserTask mAuthTask = null;
    private RequestQueue mHttpRequestQueue;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Set up the login form.
        mEmailView = findViewById(R.id.email);
        mPasswordView = findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button mEmailRegisterButton = findViewById(R.id.email_register_button);
        mEmailRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });


        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        mHttpRequestQueue = Volley.newRequestQueue(this);

        mSharedPreferences = getPreferences(Context.MODE_PRIVATE);
        String maybeToken = mSharedPreferences.getString(getString(R.string.token_preference_key), null);
        if (maybeToken != null) {
            onSuccessfulLogin(maybeToken);
        }

    }
    private void attemptRegister() {
        if (mAuthTask != null) {
            return;
        }
        LoginFormData loginFormData = getLoginFormData();
        if (loginFormData != null) {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserRegisterTask(loginFormData.email, loginFormData.password);
            mAuthTask.execute((Void) null);
        }
    }

    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }
        LoginFormData loginFormData = getLoginFormData();
        if (loginFormData != null) {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(loginFormData.email, loginFormData.password);
            mAuthTask.execute((Void) null);
        }
    }

    private class LoginFormData {
        public String email;
        public String password;

        public  LoginFormData(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    private LoginFormData getLoginFormData() {
        mEmailView.setError(null);
        mPasswordView.setError(null);

        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_empty_password));
            mPasswordView.requestFocus();
            return null;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            mEmailView.requestFocus();
            return null;
        }

        return new LoginFormData(email, password);
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

    protected void onSuccessfulLogin(String token) {
        Log.i(TAG,"Successful login, token = " + token);
        // TODO: Save token and navigate to next activity
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(getString(R.string.token_preference_key), token);
        editor.commit();

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public abstract class UserTask extends AsyncTask<Void, Void, String> {
        private final String mEmail;
        private final String mPassword;

        UserTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }
        @Override
        protected String doInBackground(Void... voids) {
            RequestFuture<JSONObject> future = RequestFuture.newFuture();
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST,
                    getUrl(),
                    JsonHelpers.loginPayload(mEmail, mPassword),
                    future, future);
            mHttpRequestQueue.add(request);

            try {
                JSONObject response = future.get();
                return (String) response.get("api_key");
            } catch (InterruptedException e) {
                Log.e(TAG,"InterruptedException: " + e);
            } catch (ExecutionException e) {
                Log.e(TAG,"ExecutionException: " + e);
            } catch (JSONException e) {
                Log.e(TAG,"Invalid JSON received from server: " + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(final String apiKey) {
            onFinish();

            if (apiKey == null) {
                onFailure();
            } else {
                onSuccessfulLogin(apiKey);
            }
        }

        @Override
        protected void onCancelled() {
            onFinish();
        }

        private void onFinish() {
            mAuthTask = null;
            showProgress(false);
        }

        protected abstract String getUrl();
        protected abstract void onFailure();
    }

    public class UserLoginTask extends UserTask {
        public  UserLoginTask(String email, String password) {
            super(email, password);
        }

        @Override
        protected String getUrl() {
            return getString(R.string.backend_url) + "/login";
        }

        @Override
        protected void onFailure() {
            mPasswordView.setError(getString(R.string.error_incorrect_password));
            mPasswordView.requestFocus();
        }
    }

    public class UserRegisterTask extends UserTask {
        public  UserRegisterTask(String email, String password) {
            super(email, password);
        }

        @Override
        protected String getUrl() {
            return getString(R.string.backend_url) + "/register";
        }

        @Override
        protected void onFailure() {
            mEmailView.setError(getString(R.string.error_user_already_exist));
            mEmailView.requestFocus();
        }
    }
}

