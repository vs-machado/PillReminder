package com.phoenix.pillreminder.model

import androidx.lifecycle.ViewModel
import java.util.Calendar
import java.util.Date

class HomeFragmentViewModel: ViewModel() {
    private var date: Date = Calendar.getInstance().time

    fun setDate(selectedDate: Date){
        date = selectedDate
    }

    fun getDate(): Date{
        return date
    }
}