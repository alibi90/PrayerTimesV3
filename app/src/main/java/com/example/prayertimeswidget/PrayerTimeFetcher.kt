
package com.example.prayertimeswidget

import android.content.Context
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PrayerTimeFetcher(private val context: Context) {

    private val client = OkHttpClient()
    private val baseUrl = "https://api.aladhan.com/v1/timingsByDate"

    suspend fun fetchPrayerTimes(latitude: Double, longitude: Double): Map<String, String>? =
        withContext(Dispatchers.IO) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val calculationMethod = prefs.getString("calc_method", "ISNA") ?: "ISNA"

            val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)

            val urlBuilder = baseUrl.toHttpUrlOrNull()?.newBuilder()?.apply {
                addQueryParameter("latitude", latitude.toString())
                addQueryParameter("longitude", longitude.toString())
                addQueryParameter("date", currentDate)
                when (calculationMethod) {
                    "MWL" -> addQueryParameter("method", "3")
                    "ISNA" -> addQueryParameter("method", "2")
                    "EGYPT" -> addQueryParameter("method", "5")
                    "MAKKAH" -> addQueryParameter("method", "4")
                    "KARACHI" -> addQueryParameter("method", "1")
                    "HANAFI" -> {
                        addQueryParameter("method", "1")
                        addQueryParameter("school", "1")
                    }
                    else -> addQueryParameter("method", "2")
                }
            }?.build()

            if (urlBuilder == null) return@withContext null

            val request = Request.Builder().url(urlBuilder).build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { body ->
                        return@withContext parsePrayerTimes(JSONObject(body))
                    }
                }
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private fun parsePrayerTimes(jsonResponse: JSONObject): Map<String, String> {
        val timings = jsonResponse.getJSONObject("data").getJSONObject("timings")
        return mapOf(
            "Fajr" to formatTime(timings.getString("Fajr")),
            "Sunrise" to formatTime(timings.getString("Sunrise")),
            "Dhuhr" to formatTime(timings.getString("Dhuhr")),
            "Asr" to formatTime(timings.getString("Asr")),
            "Maghrib" to formatTime(timings.getString("Maghrib")),
            "Isha" to formatTime(timings.getString("Isha"))
        )
    }

    private fun formatTime(time: String): String {
        val parts = time.split(":")
        var hour = parts[0].toInt()
        val minute = parts[1]
        hour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%02d:%s", hour, minute)
    }
}
