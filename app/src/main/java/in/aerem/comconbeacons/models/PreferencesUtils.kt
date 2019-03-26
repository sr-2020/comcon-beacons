package `in`.aerem.comconbeacons.models

import `in`.aerem.comconbeacons.ComConBeaconsApplication
import `in`.aerem.comconbeacons.R
import android.app.Application
import android.content.Context

fun getBackendUrl(application: Application, context: Context): String {
    val preferences = (application as ComConBeaconsApplication).getGlobalSharedPreferences()
    return preferences.getString(context.getString(R.string.backend_url_key),
        context.getString(R.string.default_backend_url))
}