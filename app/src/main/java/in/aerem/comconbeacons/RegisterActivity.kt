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
import `in`.aerem.comconbeacons.models.LoginResponse
import `in`.aerem.comconbeacons.models.LoginResult
import `in`.aerem.comconbeacons.models.RegisterRequest
import android.view.Gravity
import android.widget.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import java.io.IOException

class RegisterActivity : AppCompatActivity() {
    private val TAG = "ComConBeacons"

    // Keep track of the login task to ensure we can cancel it if requested.
    private var mRegisterTask: UserRegisterTask? = null

    // UI references.
    private lateinit var mEmailView: AutoCompleteTextView
    private lateinit var mNameView: EditText
    private lateinit var mPasswordView: EditText
    private lateinit var mPasswordRepeatView: EditText
    private lateinit var mProgressView: View
    private lateinit var mLoginFormView: View

    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Set up the login form.
        mEmailView = findViewById(R.id.email)
        mNameView = findViewById(R.id.name)
        mPasswordView = findViewById(R.id.password)
        mPasswordRepeatView = findViewById(R.id.password_repeat)
        mPasswordRepeatView.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptRegister()
                return@OnEditorActionListener true
            }
            false
        })

        val registerButton = findViewById<Button>(R.id.email_register_button)
        registerButton.setOnClickListener { attemptRegister() }

        mLoginFormView = findViewById(R.id.login_form)
        mProgressView = findViewById(R.id.login_progress)

        mSharedPreferences = (application as ComConBeaconsApplication).getGlobalSharedPreferences()
    }

    private fun attemptRegister() {
        if (mRegisterTask != null) {
            return
        }
        val formData = getRegisterFormData()
        if (formData != null) {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            mRegisterTask = UserRegisterTask(formData)
            mRegisterTask!!.execute(null as Void?)
        }
    }

    private inner class RegisterFormData(var email: String, var name: String, var password: String)

    private fun getRegisterFormData(): RegisterFormData? {
        mEmailView.error = null
        mNameView.error = null
        mPasswordView.error = null
        mPasswordRepeatView.error = null

        val email = mEmailView.text.toString()
        val name = mNameView.text.toString()
        val password = mPasswordView.text.toString()
        val passwordRepeat = mPasswordRepeatView.text.toString()
        if (TextUtils.isEmpty(email)) {
            mEmailView.error = getString(R.string.error_field_required)
            mEmailView.requestFocus()
            return null
        }
        if (TextUtils.isEmpty(name)) {
            mNameView.error = getString(R.string.error_field_required)
            mNameView.requestFocus()
            return null
        }
        if (TextUtils.isEmpty(password)) {
            mPasswordView.error = getString(R.string.error_empty_password)
            mPasswordView.requestFocus()
            return null
        }
        if (TextUtils.isEmpty(passwordRepeat)) {
            mPasswordRepeatView.error = getString(R.string.error_empty_password)
            mPasswordRepeatView.requestFocus()
            return null
        }
        if (!TextUtils.equals(password, passwordRepeat)) {
            mPasswordRepeatView.error = getString(R.string.error_password_not_repeated)
            mPasswordRepeatView.requestFocus()
            return null
        }

        return RegisterFormData(email, name, password)
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

    protected fun onSuccessfulRegister(token: String) {
        Log.i(TAG, "Successful registration, token = $token")
        val editor = mSharedPreferences.edit()
        editor.putString(getString(R.string.token_preference_key), token)
        editor.commit()

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private inner class UserRegisterTask internal constructor(mFormData: RegisterFormData) :
        AsyncTask<Void, Void, LoginResult>() {
        protected val mRegisterRequest: RegisterRequest = RegisterRequest(mFormData.email, mFormData.name, mFormData.password)
        internal var mService: PositionsWebService = Retrofit.Builder()
            .baseUrl(getString(R.string.backend_url))
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(PositionsWebService::class.java)

        override fun doInBackground(vararg voids: Void): LoginResult {
            val c = mService.register(mRegisterRequest)
            try {
                val response = c.execute()
                if (response.isSuccessful) {
                    return LoginResult(true, false, response.body()!!.api_key)
                }
                Log.e(TAG, "Unsuccessful response: " + response.errorBody())
                return LoginResult(false, false, "")
            } catch (e: IOException) {
                Log.e(TAG, "IOException: $e")
                return LoginResult(false, true, "")
            }
        }

        override fun onPostExecute(result: LoginResult) {
            onFinish()
            if (result.success) {
                onSuccessfulRegister(result.apiKey)
            } else if (result.noConnection) {
                val toast = Toast.makeText(this@RegisterActivity, "Сервер недоступен", Toast.LENGTH_LONG)
                toast.setGravity(Gravity.TOP, 0, 0)
                toast.show()
            } else {
                mEmailView.error = getString(R.string.error_user_already_exist)
                mEmailView.requestFocus()
            }
        }

        override fun onCancelled() {
            onFinish()
        }

        private fun onFinish() {
            mRegisterTask = null
            showProgress(false)
        }
    }
}

