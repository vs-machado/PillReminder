package com.phoenix.pillreminder.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.phoenix.pillreminder.alarmscheduler.AlarmItem
import com.phoenix.pillreminder.alarmscheduler.AndroidAlarmScheduler
import com.phoenix.pillreminder.db.Medicine
import java.time.LocalDateTime
import java.util.Calendar
import java.util.TimeZone

class AlarmSettingsSharedViewModel : ViewModel() {
    private val _medicineName = MutableLiveData("")
    val medicineName: LiveData<String> = _medicineName

    private val _numberOfAlarms = MutableLiveData<Int>()
    val numberOfAlarms: LiveData<Int> = _numberOfAlarms

    private var _currentAlarmNumber = MutableLiveData<Int>()
    val currentAlarmNumber: LiveData<Int> = _currentAlarmNumber

    private var _medicineForm = MutableLiveData("")
    val medicineForm: LiveData<String> = _medicineForm

    private var medicineQuantity = 0F
    private var medicineFrequency = ""

    //Variables to store as many alarms as the user wants
    private var alarmHour = Array<Int?>(10){null}
    private var alarmMinute = Array<Int?>(10){null}

    //Variables to set treatment period
    private var treatmentStartDate: Long = 0L
    private var treatmentEndDate: Long = 0L

    private var alarmItem: AlarmItem? = null

    var position = 0

    init{
        _currentAlarmNumber.value = 1
        _numberOfAlarms.value = 1
    }

    fun createMedicineAlarm(): Medicine {
        val name = _medicineName.value!!
        val quantity = getMedicineQuantity()
        val form = getMedicineForm()
        val alarmHour = getAlarmHour()
        val alarmMinute = getAlarmMinute()
        val startDate = treatmentStartDate
        val endDate = treatmentEndDate

        return Medicine(0, name, quantity, form!!, alarmHour, alarmMinute, startDate, endDate)
    }

    //Test
    fun scheduleAlarm(context: Context){
        val scheduler = AndroidAlarmScheduler(context)

        alarmItem = AlarmItem(
            time = LocalDateTime.now().plusSeconds(10),
            message = "${getMedicineName()}"
        )
        alarmItem?.let(scheduler::scheduleAlarm)
    }

    fun resetCurrentAlarmNumber(){
        _currentAlarmNumber.value = 1
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

        setTreatmentPeriod(startDate.timeInMillis, endDate.timeInMillis)
    }

    fun saveMedicineForm(position: Int){
        when (position){
            0 -> {
                _medicineForm.value = "pill"
            }

            1 -> {
                _medicineForm.value = "injection"
            }

            2 -> {
                _medicineForm.value = "liquid"
            }

            3 -> {
                _medicineForm.value = "drop"
            }

            4 -> {
                _medicineForm.value = "inhaler"
            }

            5 -> {
                _medicineForm.value = "pomade"
            }
        }
    }

    //Getters and setters
    private fun getMedicineName(): String? {
        return _medicineName.value
    }
    private fun setTreatmentPeriod(startDate: Long, endDate: Long){
        var endAlarmHour: Int = 0
        var endAlarmMinute: Int = 0

        //Sets the treatment start date
        val calendarStart = Calendar.getInstance()
        calendarStart.timeInMillis = startDate
        calendarStart.set(Calendar.HOUR_OF_DAY, alarmHour[0]!!)
        calendarStart.set(Calendar.MINUTE, alarmMinute[0]!!)
        calendarStart.set(Calendar.SECOND, 0)
        calendarStart.set(Calendar.MILLISECOND, 0)

        //The for loop will search for the last value of the array. This is necessary to set the end treatment date (last alarm).
        for(i in alarmHour.indices){
            if(alarmHour[i] != null && alarmMinute[i] != null){
                endAlarmHour = alarmHour[i]!!
                endAlarmMinute = alarmMinute[i]!!
            }
        }

        //Sets the treatment end date
        val calendarEnd = Calendar.getInstance()
        calendarEnd.timeInMillis = endDate
        calendarEnd.set(Calendar.HOUR_OF_DAY, endAlarmHour)
        calendarEnd.set(Calendar.MINUTE, endAlarmMinute)
        calendarEnd.set(Calendar.SECOND, 0)
        calendarEnd.set(Calendar.MILLISECOND, 0)

        treatmentStartDate = calendarStart.timeInMillis
        treatmentEndDate = calendarEnd.timeInMillis
    }
    fun setMedicineName(userInput: String){
        _medicineName.value = userInput
    }

    fun setMedicineQuantity(quantity: Float){
        medicineQuantity = quantity
    }

    fun setMedicineFrequency(frequency: String){
        medicineFrequency = frequency
    }

    fun setNumberOfAlarms(newNumberOfAlarms: Int){
        _numberOfAlarms.value = newNumberOfAlarms
    }

    fun getMedicineQuantity(): Float {
        return medicineQuantity
    }

    fun getMedicineForm(): String? {
        return medicineForm.value
    }

    fun getMedicineFrequency(): String{
        return medicineFrequency
    }

    fun getAlarmHour(): Int{
        return alarmHour[0]!!
    }

    fun getAlarmMinute(): Int{
        return alarmMinute[0]!!
    }

}