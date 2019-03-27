package `in`.aerem.comconbeacons

import `in`.aerem.comconbeacons.models.LoginRequest
import `in`.aerem.comconbeacons.models.LoginResult
import `in`.aerem.comconbeacons.models.getBackendUrl
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


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

        findViewById<ImageButton>(R.id.settings_button).setOnClickListener { showSettings() }


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

    private fun showSettings() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.choose_server_address_title))

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setText(getBackendUrl(application, this))
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.ok)) { _, _ -> saveServerAddress(input.text.toString()) }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun saveServerAddress(address: String) {
        val editor = mSharedPreferences.edit()
        editor.putString(getString(R.string.backend_url_key), address)
        editor.commit()
    }

    private inner class LoginFormData(var email: String, var password: String)

    private fun loginFormData(): LoginFormData? {
        mEmailView.error = null
        mPasswordView.error = null
        val email = mEmailView.text.toString().trim()
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

    inner class UserLoginTask internal constructor(email: String, password: String) : AsyncTask<Void, Void, LoginResult>() {
        private val mLoginRequest: LoginRequest = LoginRequest(email, password)

        override fun doInBackground(vararg voids: Void): LoginResult {
            try {
                val service = Retrofit.Builder()
                    .baseUrl(getBackendUrl(application, this@LoginActivity))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(PositionsWebService::class.java)
                val c = service.login(mLoginRequest)
                val response = c.execute()
                if (response.isSuccessful) {
                    return LoginResult(true, false, response.body()!!.api_key)
                }
                Log.e(TAG, "Unsuccessful response: " + response.errorBody())
                return LoginResult(false, false, "")
            } catch (e: Exception) {
                Log.e(TAG, "IOException: $e")
                return LoginResult(false, true, "")
            }
        }

        override fun onPostExecute(result: LoginResult) {
            onFinish()
            if (result.success) {
                onSuccessfulLogin(result.apiKey)
            } else if (result.noConnection) {
                val toast = Toast.makeText(this@LoginActivity, "Сервер недоступен", Toast.LENGTH_LONG)
                toast.setGravity(Gravity.TOP, 0, 0)
                toast.show()
            } else {
                mPasswordView.error = getString(R.string.error_incorrect_password)
                mPasswordView.requestFocus()
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

