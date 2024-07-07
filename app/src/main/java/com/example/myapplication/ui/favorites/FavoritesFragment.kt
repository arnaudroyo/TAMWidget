package com.example.myapplication.ui.favorites

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavoritesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var savedStopsAdapter: SavedStopsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)

        val savedStops = getSavedStops()
        savedStopsAdapter = SavedStopsAdapter(requireContext(), savedStops.toMutableList())
        recyclerView.adapter = savedStopsAdapter

        return view
    }

    private fun getSavedStops(): List<SavedStop> {
        val sharedPref = activity?.getSharedPreferences("com.example.myapplication", Context.MODE_PRIVATE) ?: return emptyList()
        val savedStopsJson = sharedPref.getString("SAVED_STOPS_JSON", "[]")

        // Convert the JSON to a list of stops
        val type = object : TypeToken<MutableList<SavedStop>>() {}.type
        val savedStops: List<SavedStop> = Gson().fromJson(savedStopsJson, type)

        // Log loaded stops
        Log.d("SharedPreferencesDebug", "Loaded stops: $savedStops")

        return savedStops
    }

}
