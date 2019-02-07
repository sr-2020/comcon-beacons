package `in`.aerem.comconbeacons

import android.app.Application
import android.content.SharedPreferences
import android.preference.PreferenceManager

class ComConBeaconsApplication : Application() {
    fun getGlobalSharedPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(this)
    }
}
