package `in`.aerem.comconbeacons

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.bugfender.sdk.Bugfender

class ComConBeaconsApplication : Application() {
    fun getGlobalSharedPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreate() {
        super.onCreate()
        Bugfender.init(this, "LqR51nrg2rygHhlHefCwf0HIgG4F04DB", BuildConfig.DEBUG)
        Bugfender.enableCrashReporting()
        Bugfender.enableUIEventLogging(this)
        Bugfender.enableLogcatLogging()
    }
}
