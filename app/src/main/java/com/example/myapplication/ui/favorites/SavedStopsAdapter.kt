package com.example.myapplication.ui.favorites

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.google.gson.Gson

class SavedStopsAdapter(private val context: Context, private val savedStops: MutableList<SavedStop>) :
    RecyclerView.Adapter<SavedStopsAdapter.SavedStopViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedStopViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_saved_stop, parent, false)
        return SavedStopViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedStopViewHolder, position: Int) {
        val savedStop = savedStops[position]
        holder.lineTextView.text = savedStop.line
        holder.directionTextView.text = savedStop.direction
        holder.stopTextView.text = savedStop.stop

        holder.deleteButton.setOnClickListener {
            removeItem(position)
        }
    }

    override fun getItemCount(): Int {
        return savedStops.size
    }

    private fun removeItem(position: Int) {
        savedStops.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, savedStops.size)

        // Mettre à jour les SharedPreferences
        val sharedPref = context.getSharedPreferences("com.example.myapplication", Context.MODE_PRIVATE) ?: return
        val newSavedStopsJson = Gson().toJson(savedStops)
        with(sharedPref.edit()) {
            putString("SAVED_STOPS_JSON", newSavedStopsJson)
            commit()
        }
    }

    class SavedStopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lineTextView: TextView = itemView.findViewById(R.id.line)
        val directionTextView: TextView = itemView.findViewById(R.id.direction)
        val stopTextView: TextView = itemView.findViewById(R.id.stop)
        val deleteButton: ImageButton = itemView.findViewById(R.id.delete_button) // Utilisez ImageButton ici
    }
}
