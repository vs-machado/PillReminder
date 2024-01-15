package com.phoenix.pillreminder.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AlarmSettingsSharedViewModel : ViewModel() {
    private val _numberOfAlarms = MutableLiveData<Int>()
    val numberOfAlarms: LiveData<Int> = _numberOfAlarms

    private val _medicineName = MutableLiveData("")
    val medicineName: LiveData<String> = _medicineName

    private var _currentAlarmNumber = MutableLiveData<Int>()
    val currentAlarmNumber: LiveData<Int> = _currentAlarmNumber

    init{
        _currentAlarmNumber.value = 1
    }

    fun setMedicineName(userInput: String){
        _medicineName.value = userInput
    }

    fun updateCurrentAlarmNumber(){
        _currentAlarmNumber.value = (_currentAlarmNumber.value)?.plus(1)
    }

    fun setNumberOfAlarms(newNumberOfAlarms: Int){
        _numberOfAlarms.value = newNumberOfAlarms
    }



    fun checkSelectedOption(position: Int){
        when (position){
            0 -> {
                // Should pass the user option to the database in the future

            }

            1 -> {
                // Should pass the user option to the database in the future
            }

            2 -> {
                // Should pass the user option to the database in the future
            }

            3 -> {
                // Should pass the user option to the database in the future
            }

            4 -> {
                // Should pass the user option to the database in the future
            }

            5 -> {
                // Should pass the user option to the database in the future
            }

            6 -> {
                // Should pass the user option to the database in the future
            }
        }
    }

}