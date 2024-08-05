package com.example.myapplication

import com.example.myapplication.ui.favorites.SavedStop
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.repository.*
import com.example.myapplication.repository.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.Duration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

class MyAppWidgetProvider : AppWidgetProvider() {
    companion object {
        const val TAG = "MyAppWidgetProvider"
        private const val ACTION_REFRESH = "com.example.myapplication.ACTION_REFRESH"
        private var isFetchingData = false

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, currentDateTime: String? = null) {
            Log.d(TAG, "updateAppWidget called for widget ID $appWidgetId")
            val intent = Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }

            val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
                setRemoteAdapter(R.id.widget_list_view, intent)
                setEmptyView(R.id.widget_list_view, R.id.empty_view)

                val dateTimeText = currentDateTime ?: java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                setTextViewText(R.id.widget_last_update, "Dernière MAJ : $dateTimeText")

                val refreshIntent = Intent(context, MyAppWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val pendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, flags)
                setOnClickPendingIntent(R.id.refresh_button, pendingIntent)

                // Intent to launch the main activity when the widget is clicked
                val launchAppIntent = Intent(context, MainActivity::class.java)
                val launchAppPendingIntent = PendingIntent.getActivity(context, 0, launchAppIntent, flags)
                setOnClickPendingIntent(R.id.widget_layout, launchAppPendingIntent)
            }

            Log.d(TAG, "Updating widget views for widget ID $appWidgetId")
            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
            Log.d(TAG, "Widget views updated and data changed notified for widget ID $appWidgetId")
        }


        fun fetchRealTimeData(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            if (isFetchingData) return
            isFetchingData = true

            RetrofitClient.instance.downloadGTFSZip().enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    isFetchingData = false
                    if (response.isSuccessful) {
                        response.body()?.let { responseBody ->
                            val outputDir = File(context.filesDir, "GTFS")
                            if (!outputDir.exists()) outputDir.mkdirs()
                            ZipManager.unzip(responseBody.byteStream(), outputDir)

                            val tripsFile = File(outputDir, "trips.txt")
                            val stopsFile = File(outputDir, "stops.txt")
                            val stopTimesFile = File(outputDir, "stop_times.txt")
                            val calendarFile = File(outputDir, "calendar.txt")
                            val calendarDatesFile = File(outputDir, "calendar_dates.txt")

                            val trips = CsvParser.parseTrips(tripsFile)
                            val stops = CsvParser.parseStops(stopsFile)
                            val stopTimes = CsvParser.parseStopTimes(stopTimesFile)
                            val calendars = CsvParser.parseCalendar(calendarFile)
                            val calendarDates = CsvParser.parseCalendarDates(calendarDatesFile)

                            logLargeList("Trips", trips)
                            logLargeList("Stops", stops)
                            logLargeList("StopTimes", stopTimes)
                            logLargeList("Calendars", calendars)
                            logLargeList("CalendarDates", calendarDates)

                            saveRealTimeData(context, appWidgetId, trips, stops, stopTimes, calendars, calendarDates)

                            val currentDateTime = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                            updateAppWidget(context, appWidgetManager, appWidgetId, currentDateTime)

                            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch data: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    isFetchingData = false
                    Log.e(TAG, "Failed to fetch data", t)
                }
            })
        }

        private fun logLargeList(tag: String, list: List<Any>) {
            if (list.size > 100) {
                Log.d(TAG, "$tag size: ${list.size}, first 100 items: ${list.take(100)}")
            } else {
                Log.d(TAG, "$tag: $list")
            }
        }

        private fun correctTimeFormat(timeStr: String): String {
            val (hours, minutes, seconds) = timeStr.split(":").map { it.toInt() }
            return if (hours >= 24) {
                val correctedHours = hours - 24
                "%02d:%02d:%02d".format(correctedHours, minutes, seconds)
            } else {
                timeStr
            }
        }

        private fun saveRealTimeData(context: Context, appWidgetId: Int, trips: List<Trip>, stops: List<Stop>, stopTimes: List<StopTime>, calendars: List<CalendarEntry>, calendarDates: List<CalendarDateEntry>) {
            CoroutineScope(Dispatchers.IO).launch {
                val sharedPref = context.getSharedPreferences("com.example.myapplication", Context.MODE_PRIVATE)
                val savedStopsJson = sharedPref.getString("SAVED_STOPS_JSON", "[]")
                val type = object : TypeToken<MutableList<SavedStop>>() {}.type
                val savedStops: MutableList<SavedStop> = Gson().fromJson(savedStopsJson, type)

                Log.d(TAG, "Saved stops: $savedStops")

                // Filtrer les serviceIds valides pour la date actuelle
                val validServiceIds = getValidServiceIdsForToday(context, calendars, calendarDates)
                Log.d(TAG, "Valid service IDs for today: $validServiceIds")

                val updatedStops = savedStops.map { savedStop ->
                    // Récupérer les trips valides pour les routeIds, direction et serviceIds
                    val tripIds = trips.filter { it.routeId in savedStop.routeIds && it.tripHeadSign == savedStop.direction && it.serviceId in validServiceIds }.map { it.tripId }
                    Log.d(TAG, "Trip IDs for ${savedStop.line} - ${savedStop.direction}: $tripIds")

                    // Récupérer les stop times pertinents
                    val relevantStopTimes = stopTimes.filter { it.stopId in savedStop.stopIds && it.tripId in tripIds }.map {
                        it.copy(
                            arrivalTime = correctTimeFormat(it.arrivalTime),
                            departureTime = correctTimeFormat(it.departureTime)
                        )
                    }
                    Log.d(TAG, "Relevant stop times for stopIds ${savedStop.stopIds} (${savedStop.direction}) in trip IDs: $relevantStopTimes")

                    // Filtrer les temps futurs uniquement
                    val currentTime = LocalTime.now()
                    val nextStopTimes = relevantStopTimes.filter { LocalTime.parse(it.departureTime).isAfter(currentTime) }
                        .sortedBy { it.departureTime }
                        .take(3)
                    Log.d(TAG, "Next stop times for stop ${savedStop.stop}: $nextStopTimes")

                    // Calculer le temps d'attente en minutes
                    val times = nextStopTimes.joinToString(", ") {
                        val departureTime = LocalTime.parse(it.departureTime)
                        val minutesUntilArrival = Duration.between(currentTime, departureTime).toMinutes()
                        "$minutesUntilArrival min"
                    }

                    SavedStop(
                        line = savedStop.line,
                        direction = savedStop.direction,
                        stop = savedStop.stop,
                        stopIds = savedStop.stopIds,
                        tripId = savedStop.tripId,
                        routeIds = savedStop.routeIds,
                        time = times
                    )
                }

                Log.d(TAG, "Updated stops: $updatedStops")

                // Sauvegarder les arrêts mis à jour dans SharedPreferences
                val newSavedStopsJson = Gson().toJson(updatedStops)
                with(sharedPref.edit()) {
                    putString("SAVED_STOPS_JSON", newSavedStopsJson)
                    commit()
                }

                withContext(Dispatchers.Main) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val views = RemoteViews(context.packageName, R.layout.widget_layout)

                    // Configure RemoteViewsService
                    val intent = Intent(context, WidgetService::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    views.setRemoteAdapter(R.id.widget_list_view, intent)
                    views.setEmptyView(R.id.widget_list_view, R.id.empty_view)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
                }
            }
        }

        private fun getValidServiceIdsForToday(context: Context, calendars: List<CalendarEntry>, calendarDates: List<CalendarDateEntry>): List<String> {
            val currentDate = LocalDate.now()
            val currentDayOfWeek = currentDate.dayOfWeek.value
            val currentDateString = currentDate.format(DateTimeFormatter.BASIC_ISO_DATE).toString()

            // Vérifiez dans les entrées du calendrier régulier
            val validServiceIds = calendars.filter { calendarEntry ->
                calendarEntry.startDate <= currentDateString && calendarEntry.endDate >= currentDateString &&
                        ((currentDayOfWeek == 1 && calendarEntry.monday == 1) ||
                                (currentDayOfWeek == 2 && calendarEntry.tuesday == 1) ||
                                (currentDayOfWeek == 3 && calendarEntry.wednesday == 1) ||
                                (currentDayOfWeek == 4 && calendarEntry.thursday == 1) ||
                                (currentDayOfWeek == 5 && calendarEntry.friday == 1) ||
                                (currentDayOfWeek == 6 && calendarEntry.saturday == 1) ||
                                (currentDayOfWeek == 7 && calendarEntry.sunday == 1))
            }.map { it.serviceId }

            // Vérifiez dans les entrées du calendrier des dates exceptionnelles si aucune correspondance trouvée
            if (validServiceIds.isEmpty()) {
                val validServiceIdsFromDates = calendarDates.filter { it.date == currentDateString && it.exceptionType == 1 }
                    .map { it.serviceId }

                if (validServiceIdsFromDates.isEmpty()) {
                    // Si aucune date valide n'est trouvée, supprimez les fichiers et retéléchargez-les
                    deleteGTFSFiles(context)
                    fetchAndUnzipGTFSData(context)  // Vous devrez implémenter cette fonction pour re-télécharger et décompresser les fichiers
                    Log.d(TAG, "No valid service IDs found for today. GTFS files deleted and re-downloaded.")
                    return emptyList()
                } else {
                    return validServiceIdsFromDates
                }
            }

            return validServiceIds
        }


        private fun deleteGTFSFiles(context: Context) {
            val outputDir = File(context.filesDir, "GTFS")
            if (outputDir.exists()) {
                outputDir.listFiles()?.forEach { it.delete() }
            }
            Log.d(TAG, "GTFS files deleted.")
        }

        private fun fetchAndUnzipGTFSData(context: Context) {
            RetrofitClient.instance.downloadGTFSZip().enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        response.body()?.let { responseBody ->
                            val outputDir = File(context.filesDir, "GTFS")
                            if (!outputDir.exists()) outputDir.mkdirs()
                            ZipManager.unzip(responseBody.byteStream(), outputDir)
                            Log.d(TAG, "GTFS files re-downloaded and unzipped.")
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch data: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e(TAG, "Failed to fetch data", t)
                }
            })
        }


    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive called with action: ${intent.action}")
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.d(TAG, "Received refresh action for widget ID $appWidgetId")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                fetchRealTimeData(context, appWidgetManager, appWidgetId)
            }
        } else if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MyAppWidgetProvider::class.java))
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }


    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "in onUpdate")

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}
