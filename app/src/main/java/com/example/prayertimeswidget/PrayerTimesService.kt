
package com.example.prayertimeswidget

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class PrayerTimesService : IntentService("PrayerTimesService") {

    override fun onHandleIntent(intent: Intent?) {
        val prefs = getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE)
        val lastUpdated = prefs.getString(Constants.PREF_LAST_UPDATED, "")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Only update once per day
        if (lastUpdated == today) {
            Log.d("PrayerTimesService", "Already updated today.")
            return
        }

        // Replace with actual lat/lon, or use location detection logic
        val latitude = 24.8607
        val longitude = 67.0011
        val method = 2

        val api = ApiClient.retrofit.create(PrayerTimesApi::class.java)
        api.getPrayerTimesByCoords(latitude, longitude, method).enqueue(object : Callback<PrayerTimesResponse> {
            override fun onResponse(call: Call<PrayerTimesResponse>, response: Response<PrayerTimesResponse>) {
                if (response.isSuccessful) {
                    val timings = response.body()?.data?.timings
                    if (timings != null) {
                        with(prefs.edit()) {
                            putString(Constants.PREF_FAJR, timings.Fajr)
                            putString(Constants.PREF_SUNRISE, timings.Sunrise)
                            putString(Constants.PREF_DHUHR, timings.Dhuhr)
                            putString(Constants.PREF_ASR, timings.Asr)
                            putString(Constants.PREF_MAGHRIB, timings.Maghrib)
                            putString(Constants.PREF_ISHA, timings.Isha)
                            putString(Constants.PREF_LAST_UPDATED, today)
                            apply()
                        }
                        Log.d("PrayerTimesService", "Prayer times updated.")
                    }
                } else {
                    Log.e("PrayerTimesService", "API response unsuccessful")
                }
            }

            override fun onFailure(call: Call<PrayerTimesResponse>, t: Throwable) {
                Log.e("PrayerTimesService", "API call failed", t)
            }
        })
    }
}
