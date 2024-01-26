package com.phoenix.pillreminder.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.phoenix.pillreminder.db.Medicine
import java.util.Calendar
import java.util.TimeZone

class AlarmSettingsSharedViewModel : ViewModel() {
    private val _medicineName = MutableLiveData("")
    val medicineName: LiveData<String> = _medicineName

    private val _numberOfAlarms = MutableLiveData<Int>()
    val numberOfAlarms: LiveData<Int> = _numberOfAlarms

    private var _currentAlarmNumber = MutableLiveData<Int>()
    val currentAlarmNumber: LiveData<Int> = _currentAlarmNumber

    private var medicineQuantity = ""
    private var medicineStrength = ""
    private var medicineForm = ""
    private var medicineFrequency = ""

    //Variables to store as many alarms as the user wants
    private var alarmHour = Array<Int?>(10){null}
    private var alarmMinute = Array<Int?>(10){null}

    //Variables to set treatment period
    private var beginDay = 1
    private var beginMonth = 1
    private var beginYear = 1
    private var endDay = 1
    private var endMonth = 1
    private var endYear = 1

    var position = 0

    init{
        _currentAlarmNumber.value = 1
        _numberOfAlarms.value = 1
    }

    fun saveMedicineData(){
        val name = _medicineName.value.toString()
        val quantity = getMedicineQuantity()
        val strength = getMedicineStrength()
        val form = getMedicineForm()
        val alarmHour = getAlarmHour()
        val alarmMinute = getAlarmMinute()

        val medicine = Medicine(0, name, quantity, strength, form, alarmHour, alarmMinute)
    }

    fun updateCurrentAlarmNumber(){
        _currentAlarmNumber.value = (_currentAlarmNumber.value)?.plus(1)
    }

    fun decreaseCurrentAlarmNumber(){
        _currentAlarmNumber.value = (_currentAlarmNumber.value)?.minus(1)
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

    fun extractDateComponents(firstDate: Long, secondDate: Long){
        val timeZone = TimeZone.getTimeZone("GMT")

        val startDate = Calendar.getInstance(timeZone).apply { timeInMillis = firstDate }
        val endDate = Calendar.getInstance(timeZone).apply { timeInMillis = secondDate }

        beginDay = startDate.get(Calendar.DAY_OF_MONTH)
        beginMonth = startDate.get(Calendar.MONTH) + 1 // Calendar.MONTH is zero-based
        beginYear = startDate.get(Calendar.YEAR)

        endDay = endDate.get(Calendar.DAY_OF_MONTH)
        endMonth = endDate.get(Calendar.MONTH) + 1 // Calendar.MONTH is zero-based
        endYear = endDate.get(Calendar.YEAR)
    }

    fun saveMedicineForm(position: Int){
        when (position){
            0 -> {
                setMedicineForm("Pill")
            }

            1 -> {
                setMedicineForm("Injection")
            }

            2 -> {
                setMedicineForm("Liquid")
            }

            3 -> {
                setMedicineForm("Drops")
            }

            4 -> {
                setMedicineForm("Inhaler")
            }

            5 -> {
                setMedicineForm("Powder")
            }
        }
    }

    //Getters and setters
    fun setMedicineName(userInput: String){
        _medicineName.value = userInput
    }

    fun setMedicineQuantity(quantity: String){
        medicineQuantity = quantity
    }

    fun setMedicineStrength(strength: String){
        medicineStrength = strength
    }

    private fun setMedicineForm(type: String){
        medicineForm = type
    }

    fun setMedicineFrequency(frequency: String){
        medicineFrequency = frequency
    }

    fun setNumberOfAlarms(newNumberOfAlarms: Int){
        _numberOfAlarms.value = newNumberOfAlarms
    }

    fun getMedicineQuantity(): String {
        return medicineQuantity
    }

    fun getMedicineStrength(): String{
        return medicineStrength
    }

    fun getMedicineForm(): String{
        return medicineForm
    }

    fun getMedicineFrequency(): String{
        return medicineFrequency
    }

    fun getAlarmHour(): Int{
        return alarmHour[1]!!
    }

    fun getAlarmMinute(): Int{
        return alarmMinute[1]!!
    }

}