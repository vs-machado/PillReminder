package com.phoenix.pillreminder.feature_alarms.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.phoenix.pillreminder.R

class DayPickerAdapter(
    context: Context,
    private val days: List<String>,
    private val backgroundColor: Int
): ArrayAdapter<String>(context, R.layout.adapter_day_picker, days) {

    private val selectedItems = mutableSetOf<Int>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.adapter_day_picker, parent, false)

        val tvDay = view.findViewById<TextView>(R.id.tvDay)
        tvDay.text = days[position]
        tvDay.setTextColor(ContextCompat.getColor(context, R.color.black))

        updateItemAppearance(view, position)

        return view
    }

    fun checkItemSelection(position: Int): Boolean{
        // The set goes from 1-7, corresponding to Sunday - Saturday. So position must be > 0
        if(selectedItems.contains(position + 1)){
            selectedItems.remove(position + 1)
        } else{
            selectedItems.add(position + 1)
        }
        notifyDataSetChanged()

        return selectedItems.isNotEmpty()
    }

    fun getSelectedDaysList(): MutableSet<Int> {
        return selectedItems
    }

    private fun updateItemAppearance(view: View, position: Int){
        val tvDay = view.findViewById<TextView>(R.id.tvDay)
        if(selectedItems.contains(position + 1)){
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.light_green))
            tvDay.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            view.setBackgroundColor(backgroundColor)
            tvDay.setTextColor(ContextCompat.getColor(context, R.color.black))
        }
    }
}