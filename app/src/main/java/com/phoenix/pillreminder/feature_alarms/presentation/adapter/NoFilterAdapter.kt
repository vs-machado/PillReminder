package com.phoenix.pillreminder.feature_alarms.presentation.adapter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

/* Class used to get rid of the filter in autocompletetextview.
   When user rotates the screen the filter is applied again using an ArrayAdapter
 standard instance. This class fix this by removing the filter. */
class NoFilterAdapter(
    context: Context,
    layout: Int,
    array: Array<String>
): ArrayAdapter<String>(
    context, layout, array
){
    private val noOpFilter = object: Filter() {
        private val noOpResult = FilterResults()
        override fun performFiltering(constraint: CharSequence?) = noOpResult
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {}
    }

    override fun getFilter() = noOpFilter
}