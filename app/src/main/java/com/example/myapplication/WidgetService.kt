package com.example.myapplication

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.myapplication.ui.favorites.SavedStop

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        Log.d("WidgetService", "onGetViewFactory called with intent: $intent")
        return WidgetRemoteViewsFactory(this.applicationContext, intent)
    }
}

class WidgetRemoteViewsFactory(private val context: Context, intent: Intent) : RemoteViewsService.RemoteViewsFactory {

    private var data: List<SavedStop> = emptyList()
    private val appWidgetId: Int = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

    init {
        Log.d("WidgetRemoteViewsFactory", "init called")
        loadData()
    }

    private fun loadData() {
        val sharedPref = context.getSharedPreferences("com.example.myapplication", 0)
        val savedStopsJson = sharedPref.getString("SAVED_STOPS_JSON", "[]")

        val type = object : TypeToken<List<SavedStop>>() {}.type
        val stops: List<SavedStop> = Gson().fromJson(savedStopsJson, type)

        data = stops
        Log.d("WidgetRemoteViewsFactory", "Loaded stops: $data")
    }

    override fun onCreate() {
        Log.d("WidgetRemoteViewsFactory", "onCreate called")
    }

    override fun onDataSetChanged() {
        Log.d("WidgetRemoteViewsFactory", "onDataSetChanged called")
        loadData()
    }

    override fun onDestroy() {
        Log.d("WidgetRemoteViewsFactory", "onDestroy called")
    }

    override fun getCount(): Int {
        val count = data.size
        Log.d("WidgetRemoteViewsFactory", "getCount: $count")
        return count
    }

    override fun getViewAt(position: Int): RemoteViews {
        Log.d("WidgetRemoteViewsFactory", "getViewAt position: $position")
        val views = RemoteViews(context.packageName, R.layout.widget_list_item)
        val item = data[position]

        views.setTextViewText(R.id.stop_info, "Ligne: ${item.line}, Direction: ${item.direction}, Arrêt: ${item.stop}")
        views.setTextViewText(R.id.arrival_time, "Prochains passages : ${item.time}")

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        Log.d("WidgetRemoteViewsFactory", "getLoadingView called")
        return null
    }

    override fun getViewTypeCount(): Int {
        Log.d("WidgetRemoteViewsFactory", "getViewTypeCount called")
        return 1
    }

    override fun getItemId(position: Int): Long {
        Log.d("WidgetRemoteViewsFactory", "getItemId position: $position")
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        Log.d("WidgetRemoteViewsFactory", "hasStableIds called")
        return true
    }
}
