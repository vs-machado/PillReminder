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


    private var alarmHour = IntArray(10)
    private var alarmMinute = IntArray(10)


    var position = 0

    init{
        _currentAlarmNumber.value = 1
        _numberOfAlarms.value = 1
    }

    fun setMedicineName(userInput: String){
        _medicineName.value = userInput
    }

    fun updateCurrentAlarmNumber(){
        _currentAlarmNumber.value = (_currentAlarmNumber.value)?.plus(1)
    }

    fun decreaseCurrentAlarmNumber(){
        _currentAlarmNumber.value = (_currentAlarmNumber.value)?.minus(1)
    }

    fun setNumberOfAlarms(newNumberOfAlarms: Int){
        _numberOfAlarms.value = newNumberOfAlarms
    }

    fun saveAlarmHour(position: Int, hourOfDay: Int, minute :Int){
        alarmHour[position] = hourOfDay
        alarmMinute[position] = minute
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