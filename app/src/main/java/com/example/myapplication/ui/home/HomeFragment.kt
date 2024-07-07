package com.example.myapplication.ui.home

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.MainActivity
import com.example.myapplication.MyAppWidgetProvider
import com.example.myapplication.MyAppWidgetProvider.Companion
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.network.RetrofitClient
import com.example.myapplication.repository.ZipManager
import com.example.myapplication.repository.model.*
import com.example.myapplication.ui.favorites.SavedStop
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var trips: List<Trip> = emptyList()
    private var routes: List<Route> = emptyList()
    private var stops: List<Stop> = emptyList()
    private var stopTimes: List<StopTime> = emptyList()

    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchGTFSData()
        setupSpinnerListeners()

        binding.btnSave.setOnClickListener {
            saveSelections()
        }
    }

    private fun fetchGTFSData() {
        val errorHandler = CoroutineExceptionHandler { _, exception ->
            Log.e(TAG, "Error fetching GTFS data", exception)
            Toast.makeText(context, "Error fetching GTFS data", Toast.LENGTH_LONG).show()
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + errorHandler) {
            val outputDir = File(context?.filesDir, "GTFS")
            Log.d(TAG, "Checking directory: ${outputDir.absolutePath}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Checking GTFS data...", Toast.LENGTH_SHORT).show()
            }
            if (!outputDir.exists()) {
                outputDir.mkdirs()
                Log.d(TAG, "Directory created")
            }
            val tripsFile = File(outputDir, "trips.txt")
            val routesFile = File(outputDir, "routes.txt")
            val stopsFile = File(outputDir, "stops.txt")
            val stopTimesFile = File(outputDir, "stop_times.txt")

            if (!tripsFile.exists() || !routesFile.exists() || !stopsFile.exists() || !stopTimesFile.exists()) {
                Log.e(TAG, "Files are not present. Need to download or check paths.")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Downloading GTFS data...", Toast.LENGTH_LONG).show()
                }
                fetchAndUnzipGTFSData(requireContext())
            } else {
                Log.d(TAG, "Files are present. Parsing files.")
                trips = CsvParser.parseTrips(tripsFile)
                routes = CsvParser.parseRoutes(routesFile)
                stops = CsvParser.parseStops(stopsFile)
                stopTimes = CsvParser.parseStopTimes(stopTimesFile)

                withContext(Dispatchers.Main) {
                    setupRouteSpinner()
                }
            }
        }
    }

    private fun fetchAndUnzipGTFSData(context: Context) {
        RetrofitClient.instance.downloadGTFSZip().enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    response.body()?.let { responseBody ->
                        val outputDir = File(context.filesDir, "GTFS")
                        if (!outputDir.exists()) outputDir.mkdirs()
                        Toast.makeText(context, "Unzipping GTFS data...", Toast.LENGTH_LONG).show()
                        ZipManager.unzip(responseBody.byteStream(), outputDir)
                        Log.d(TAG, "GTFS files re-downloaded and unzipped.")
                        Toast.makeText(context, "GTFS data ready.", Toast.LENGTH_LONG).show()

                        initializeGTFSData(context)
                    }
                } else {
                    Log.e(TAG, "Failed to fetch data: ${response.errorBody()?.string()}")
                    Toast.makeText(context, "Failed to fetch GTFS data", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e(TAG, "Failed to fetch data", t)
                Toast.makeText(context, "Failed to fetch GTFS data", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun initializeGTFSData(context: Context) {
        val outputDir = File(context.filesDir, "GTFS")
        val tripsFile = File(outputDir, "trips.txt")
        val routesFile = File(outputDir, "routes.txt")
        val stopsFile = File(outputDir, "stops.txt")
        val stopTimesFile = File(outputDir, "stop_times.txt")

        trips = CsvParser.parseTrips(tripsFile)
        routes = CsvParser.parseRoutes(routesFile)
        stops = CsvParser.parseStops(stopsFile)
        stopTimes = CsvParser.parseStopTimes(stopTimesFile)

        activity?.runOnUiThread {
            setupRouteSpinner()
        }
    }




    private fun setupSpinnerListeners() {
        binding.spinnerLine.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedRoute = routes[position]
                updateDirectionSpinner(selectedRoute.routeId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        binding.spinnerDirection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedDirection = binding.spinnerDirection.selectedItem as String
                val selectedRoute = routes[binding.spinnerLine.selectedItemPosition]

                // Filtrer les trips en fonction de la route et de la direction sélectionnées
                val relevantTrips = trips.filter { it.routeId == selectedRoute.routeId && it.tripHeadSign == selectedDirection }

                // Mettre à jour le spinner des arrêts avec les trips filtrés
                updateStopSpinner(relevantTrips)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }


    private fun setupRouteSpinner() {
        if (routes.isNotEmpty()) {
            Log.d(TAG, "Setting up route spinner with ${routes.size} routes.")
            val routeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, routes.map { it.routeShortName })
            routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerLine.adapter = routeAdapter
        } else {
            Log.e(TAG, "Routes are not initialized.")
            Toast.makeText(context, "Routes data is not available.", Toast.LENGTH_LONG).show()
        }
    }


    private fun updateDirectionSpinner(routeId: String) {
        val errorHandler = CoroutineExceptionHandler { _, exception ->
            Log.e(TAG, "Error updating direction spinner", exception)
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + errorHandler) {
            val relevantTrips = trips.filter { it.routeId == routeId }
            val directions = relevantTrips.map { it.tripHeadSign }.distinct()

            withContext(Dispatchers.Main) {
                val directionAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, directions)
                directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerDirection.adapter = directionAdapter
            }
        }
    }

    private fun updateStopSpinner(relevantTrips: List<Trip>) {
        val errorHandler = CoroutineExceptionHandler { _, exception ->
            Log.e(TAG, "Error updating stop spinner", exception)
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO + errorHandler) {
            val relevantStopIds = stopTimes.filter { it.tripId in relevantTrips.map { it.tripId } }.map { it.stopId }.distinct()
            val filteredStops = stops.filter { it.stopId in relevantStopIds }

            withContext(Dispatchers.Main) {
                val stopAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filteredStops.map { it.stopName })
                stopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerStop.adapter = stopAdapter
            }
        }
    }


    private fun saveSelections() {
        val selectedLine = binding.spinnerLine.selectedItem?.toString()
        val selectedDirection = binding.spinnerDirection.selectedItem?.toString()
        val selectedStop = binding.spinnerStop.selectedItem?.toString()

        if (selectedLine != null && selectedDirection != null && selectedStop != null) {
            val sharedPref = activity?.getSharedPreferences("com.example.myapplication", Context.MODE_PRIVATE) ?: return
            val savedStopsJson = sharedPref.getString("SAVED_STOPS_JSON", "[]")

            // Convert the JSON to a list of stops
            val type = object : TypeToken<MutableList<SavedStop>>() {}.type
            val savedStops: MutableList<SavedStop> = Gson().fromJson(savedStopsJson, type)

            // Find the selected stop objects
            val selectedStopObjects = stops.filter { it.stopName == selectedStop }
            if (selectedStopObjects.isEmpty()) {
                Log.e("SaveSelections", "No stop found with name: $selectedStop")
                return
            }

            val selectedStopIds = selectedStopObjects.map { it.stopId }
            Log.d("SaveSelections", "Selected stop: $selectedStop, Stop IDs: $selectedStopIds")

            // Find the routeIds using the selectedLine
            val selectedRouteIds = routes.filter { it.routeShortName == selectedLine }.map { it.routeId }
            Log.d("SaveSelections", "Selected line: $selectedLine, Route IDs: $selectedRouteIds")

            // Find the tripId using the selectedRouteIds and direction
            val selectedTrips = trips.filter { it.routeId in selectedRouteIds && it.tripHeadSign == selectedDirection }
            val selectedTripIds = selectedTrips.map { it.tripId }
            Log.d("SaveSelections", "Selected direction: $selectedDirection, Trip IDs: $selectedTripIds")

            // Add the new stop to the list
            val newStop = SavedStop(
                line = selectedLine,
                direction = selectedDirection,
                stop = selectedStop,
                stopIds = selectedStopIds,
                tripId = selectedTripIds.firstOrNull() ?: "", // Prend le premier tripId ou une chaîne vide si aucun trouvé
                routeIds = selectedRouteIds, // Mettre à jour ici avec la liste des routeIds trouvés
                time = ""
            )
            savedStops.add(newStop)

            // Convert the list back to JSON
            val newSavedStopsJson = Gson().toJson(savedStops)

            with(sharedPref.edit()) {
                putString("SAVED_STOPS_JSON", newSavedStopsJson)
                val success = commit()  // Use commit() to ensure data is saved immediately
                Log.d("SharedPreferencesDebug", "Save successful: $success")
                Log.d("SharedPreferencesDebug", "Saved stops after saving: $newSavedStopsJson")
            }

            // Send broadcast to update the widget
            val context = context ?: return
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, MyAppWidgetProvider::class.java))
            val intent = Intent(context, MyAppWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
