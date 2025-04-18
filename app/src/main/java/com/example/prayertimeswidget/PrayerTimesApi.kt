
package com.example.prayertimeswidget

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface PrayerTimesApi {
    @GET("timings")
    fun getPrayerTimesByCoords(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int
    ): Call<PrayerTimesResponse>
}
