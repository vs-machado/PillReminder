package com.phoenix.pillreminder.model

import androidx.lifecycle.ViewModel

class HowManyPerDayViewModel : ViewModel() {
    private var numberOfAlarms = 1
    fun setNumberOfAlarms(newVal :Int){
        numberOfAlarms = newVal
    }


}