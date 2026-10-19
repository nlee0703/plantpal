package ca.gbc.comp3074.nicholas.plantpal

import android.content.Context

/** Reads the version name from the package (BuildConfig is off by default in AGP 9). */
object BuildConfigInfo {
    fun versionName(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }
}
