package com.phoenix.pillreminder.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlarmSettingsSharedViewModel : ViewModel() {
    private val _medicineName = MutableLiveData("")
    val medicineName: LiveData<String> = _medicineName

    private val _numberOfAlarms = MutableLiveData<Int>()
    val numberOfAlarms: LiveData<Int> = _numberOfAlarms

    private var _currentAlarmNumber = MutableLiveData<Int>()
    val currentAlarmNumber: LiveData<Int> = _currentAlarmNumber


    private var alarmHour = Array<Int?>(10){null}
    private var alarmMinute = Array<Int?>(10){null}


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

    fun clearAlarmArray(){
        for(i in currentAlarmNumber.value!! - 1 until alarmHour.indices.last){
            alarmHour[i] = null
            alarmMinute[i] = null
        }
    }

    fun saveTreatmentPeriod(startDateString: String, endDateString: String){
        val startDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(startDateString)
        val endDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).parse(endDateString)

        // AI generated code
        // Use a Calendar instance to extract day, month, and year
        val startCalendar = Calendar.getInstance()
        startCalendar.time = startDate!!

        val endCalendar = Calendar.getInstance()
        endCalendar.time = endDate!!

        // Extract individual components
        val startDay = startCalendar.get(Calendar.DAY_OF_MONTH)
        val startMonth = startCalendar.get(Calendar.MONTH) + 1 // Months are zero-based
        val startYear = startCalendar.get(Calendar.YEAR)

        val endDay = endCalendar.get(Calendar.DAY_OF_MONTH)
        val endMonth = endCalendar.get(Calendar.MONTH) + 1 // Months are zero-based
        val endYear = endCalendar.get(Calendar.YEAR)

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