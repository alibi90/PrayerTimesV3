package com.example.prayertimeswidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PrayerTimesWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        scheduleDailyUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleDailyUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelDailyUpdate(context)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.app_widget)

        // Get the latest prayer times from SharedPreferences
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val fajr = prefs.getString(Constants.PREF_FAJR, "N/A")
        val sunrise = prefs.getString(Constants.PREF_SUNRISE, "N/A")
        val dhuhr = prefs.getString(Constants.PREF_DHUHR, "N/A")
        val asr = prefs.getString(Constants.PREF_ASR, "N/A")
        val maghrib = prefs.getString(Constants.PREF_MAGHRIB, "N/A")
        val isha = prefs.getString(Constants.PREF_ISHA, "N/A")
        // val lastUpdated = prefs.getLong(Constants.PREF_LAST_UPDATED, 0L) // Removed unused variable

        // val sdf = SimpleDateFormat("hh:mm", Locale.getDefault()) // Removed unused variable
        // val lastUpdatedTime = if (lastUpdated > 0) { // Removed unused block
        //     sdf.format(Date(lastUpdated))
        // } else {
        //     "N/A"
        // }

        views.setTextViewText(R.id.appwidget_fajr_time, fajr)
        views.setTextViewText(R.id.appwidget_sunrise_time, sunrise)
        views.setTextViewText(R.id.appwidget_dhuhr_time, dhuhr)
        views.setTextViewText(R.id.appwidget_asr_time, asr)
        views.setTextViewText(R.id.appwidget_maghrib_time, maghrib)
        views.setTextViewText(R.id.appwidget_isha_time, isha)
        // views.setTextViewText(R.id.appwidget_last_updated, context.getString(R.string.last_updated, lastUpdatedTime)) // Removed line that updates last updated

        // Set up a refresh button click listener
        val refreshIntent = Intent(context, PrayerTimesService::class.java)
        refreshIntent.action = Constants.ACTION_REFRESH_WIDGET
        val refreshPendingIntent = PendingIntent.getService(
            context,
            0,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.appwidget_refresh_button, refreshPendingIntent)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Start the service to fetch new data and update the widget
        val prayerTimesIntent = Intent(context, PrayerTimesService::class.java)
        context.startService(prayerTimesIntent)
    }

    private fun scheduleDailyUpdate(context: Context) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, PrayerTimesService::class.java).let { intent ->
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val calendar = Calendar.getInstance(Locale.getDefault()).apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0) // Set the hour to 0 (midnight)
            set(Calendar.MINUTE, 0)    // Set the minute to 0
            set(Calendar.SECOND, 0)    // Set the second to 0
            set(Calendar.MILLISECOND, 0) // Set the millisecond to 0

            // If the current time is already past midnight, set the alarm for the next day
            if (System.currentTimeMillis() >= timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmMgr.setRepeating(
            AlarmManager.RTC,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY, // Trigger every 24 hours
            alarmIntent
        )

        Log.d("PrayerTimesWidget", "Daily update scheduled for ${calendar.time}")
    }

    private fun cancelDailyUpdate(context: Context) {
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, PrayerTimesService::class.java).let { intent ->
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE // Use FLAG_NO_CREATE to avoid creating if it doesn't exist
            )
        }
        alarmMgr.cancel(alarmIntent)
        Log.d("PrayerTimesWidget", "Daily update cancelled")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, PrayerTimesWidget::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }
}
