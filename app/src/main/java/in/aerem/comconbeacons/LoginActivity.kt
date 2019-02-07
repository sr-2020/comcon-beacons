package `in`.aerem.comconbeacons

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import `in`.aerem.comconbeacons.models.LoginRequest
import `in`.aerem.comconbeacons.models.LoginResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import java.io.IOException

class LoginActivity : AppCompatActivity() {
    private val TAG = "ComConBeacons"

    // Keep track of the login task to ensure we can cancel it if requested.
    private var mAuthTask: UserLoginTask? = null

    // UI references.
    private lateinit var mEmailView: AutoCompleteTextView
    private lateinit var mPasswordView: EditText
    private lateinit var mProgressView: View
    private lateinit var mLoginFormView: View

    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Set up the login form.
        mEmailView = findViewById(R.id.email)
        mPasswordView = findViewById(R.id.password)
        mPasswordView.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        val mEmailSignInButton = findViewById<Button>(R.id.email_sign_in_button)
        mEmailSignInButton.setOnClickListener { attemptLogin() }

        val mEmailRegisterButton = findViewById<Button>(R.id.email_register_button)
        mEmailRegisterButton.setOnClickListener { attemptRegister() }


        mLoginFormView = findViewById(R.id.login_form)
        mProgressView = findViewById(R.id.login_progress)

        mSharedPreferences = (application as ComConBeaconsApplication).getGlobalSharedPreferences()
        val maybeToken = mSharedPreferences.getString(getString(R.string.token_preference_key), null)
        if (maybeToken != null) {
            onSuccessfulLogin(maybeToken)
        }

    }

    private fun attemptRegister() {
        if (mAuthTask != null) {
            return
        }
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun attemptLogin() {
        if (mAuthTask != null) {
            return
        }
        val loginFormData = loginFormData()
        if (loginFormData != null) {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            mAuthTask = UserLoginTask(loginFormData.email, loginFormData.password)
            mAuthTask!!.execute(null as Void?)
        }
    }

    private inner class LoginFormData(var email: String, var password: String)

    private fun loginFormData(): LoginFormData? {
        mEmailView.error = null
        mPasswordView.error = null
        val email = mEmailView.text.toString()
        val password = mPasswordView.text.toString()
        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.error = getString(R.string.error_empty_password)
            mPasswordView.requestFocus()
            return null
        }
        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.error = getString(R.string.error_field_required)
            mEmailView.requestFocus()
            return null
        }

        return LoginFormData(email, password)
    }

    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime)

        mLoginFormView.visibility = if (show) View.GONE else View.VISIBLE
        mLoginFormView.animate().setDuration(shortAnimTime.toLong()).alpha(
            (if (show) 0 else 1).toFloat()
        ).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mLoginFormView.visibility = if (show) View.GONE else View.VISIBLE
            }
        })

        mProgressView.visibility = if (show) View.VISIBLE else View.GONE
        mProgressView.animate().setDuration(shortAnimTime.toLong()).alpha(
            (if (show) 1 else 0).toFloat()
        ).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mProgressView.visibility = if (show) View.VISIBLE else View.GONE
            }
        })
    }

    protected fun onSuccessfulLogin(token: String) {
        Log.i(TAG, "Successful login, token = $token")
        val editor = mSharedPreferences.edit()
        editor.putString(getString(R.string.token_preference_key), token)
        editor.commit()

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    inner class UserLoginTask internal constructor(email: String, password: String) : AsyncTask<Void, Void, String>() {
        protected val mLoginRequest: LoginRequest
        internal var mService: PositionsWebService

        init {
            mLoginRequest = LoginRequest(email, password)

            val retrofit = Retrofit.Builder()
                .baseUrl(getString(R.string.backend_url))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            mService = retrofit.create(PositionsWebService::class.java)
        }

        override fun doInBackground(vararg voids: Void): String? {
            val c = mService.login(mLoginRequest)
            try {
                val response = c.execute()
                if (response.isSuccessful) {
                    return response.body()!!.api_key
                }
                Log.e(TAG, "Unsuccessful response: " + response.errorBody())
            } catch (e: IOException) {
                Log.e(TAG, "IOException: $e")
            }

            return null
        }

        override fun onPostExecute(apiKey: String?) {
            onFinish()

            if (apiKey == null) {
                mPasswordView.error = getString(R.string.error_incorrect_password)
                mPasswordView.requestFocus()
            } else {
                onSuccessfulLogin(apiKey)
            }
        }

        override fun onCancelled() {
            onFinish()
        }

        private fun onFinish() {
            mAuthTask = null
            showProgress(false)
        }
    }
}

