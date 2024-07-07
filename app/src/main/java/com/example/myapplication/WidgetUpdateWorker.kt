package com.example.myapplication

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class WidgetUpdateWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.d("WidgetUpdateWorker", "doWork called. Fetching new data and updating widget...")

        val context = applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MyAppWidgetProvider::class.java))

        appWidgetIds.forEach { appWidgetId ->
            // Call fetchRealTimeData to download new data and update the widget
            MyAppWidgetProvider.fetchRealTimeData(context, appWidgetManager, appWidgetId)
        }

        val intent = Intent(context, MyAppWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        }
        context.sendBroadcast(intent)

        Log.d("WidgetUpdateWorker", "Widget update broadcast sent.")

        return Result.success()
    }
}
