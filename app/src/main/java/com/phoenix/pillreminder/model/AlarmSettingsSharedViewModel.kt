package com.phoenix.pillreminder.model

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.phoenix.pillreminder.alarmscheduler.AlarmItem
import com.phoenix.pillreminder.alarmscheduler.AlarmReceiver
import com.phoenix.pillreminder.db.Medicine
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone

class AlarmSettingsSharedViewModel : ViewModel() {
    private var _medicineName = MutableLiveData("")

    private var _numberOfAlarms = MutableLiveData<Int>()
    val numberOfAlarms: LiveData<Int> = _numberOfAlarms

    private var _currentAlarmNumber = MutableLiveData<Int>()
    val currentAlarmNumber: LiveData<Int> = _currentAlarmNumber

    private var _medicineForm = MutableLiveData("")
    val medicineForm: LiveData<String> = _medicineForm

    private var alarmItemList = mutableListOf<AlarmItem>()
    private var medicineQuantity = 0F
    private var medicineFrequency = ""

    //Variables to store as many alarms as the user wants
    private var alarmHour = Array<Int?>(10){null}
    private var alarmMinute = Array<Int?>(10){null}
    private var alarmInMillis = Array<Long?>(10){null}

    //Variables to set treatment period
    private var treatmentStartDate: Long = 0L
    private var treatmentEndDate: Long = 0L

    private lateinit var pendingIntent: PendingIntent

    var position = 0


    init{
        _currentAlarmNumber.value = 1
        _numberOfAlarms.value = 1
    }

    fun createMedicineAlarm(): List<Medicine> {
        val name = _medicineName.value!!
        val quantity = getMedicineQuantity()
        val form = getMedicineForm()
        val alarmInMillis = getAlarmInMillisList()
        val alarmHours = getAlarmHoursList()
        val alarmMinutes = getAlarmMinutesList()
        val startDate = treatmentStartDate
        val endDate = treatmentEndDate
        val medicineWasTaken = false

        val alarms = mutableListOf<Medicine>()

        for (i in alarmHours.indices){
            alarms.add(
                Medicine(0, name, quantity, form!!, alarmInMillis[i], alarmHours[i], alarmMinutes[i], startDate, endDate, medicineWasTaken)
            )
        }
        return alarms
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
        val timeZone = TimeZone.getTimeZone("UTC")
        val startDate = Calendar.getInstance(timeZone).apply { timeInMillis = firstDate }
        val endDate = Calendar.getInstance(timeZone).apply { timeInMillis = secondDate }

        setTreatmentPeriod(startDate.timeInMillis, endDate.timeInMillis)
    }

    @RequiresApi(Build.VERSION_CODES.S)
     fun setTimer(startDate: Long, context: Context, requestCode: Int){
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)

        pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)

        if(!alarmManager.canScheduleExactAlarms()){
            //Needs to explain to user why he can't use the app
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startDate, pendingIntent)
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
    fun getMedicineName(): String? {
        return _medicineName.value
    }

    private fun setTreatmentPeriod(startDate: Long, endDate: Long){
        val timeZone = TimeZone.getTimeZone("UTC")
        val timeZoneDefault = TimeZone.getDefault()

        var endAlarmHour = 0
        var endAlarmMinute = 0

        //Sets the treatment start date
        val calendarStart = Calendar.getInstance(timeZone)
        calendarStart.timeInMillis = startDate
        calendarStart.set(Calendar.HOUR_OF_DAY, alarmHour[0]!!)
        calendarStart.set(Calendar.MINUTE, alarmMinute[0]!!)
        calendarStart.set(Calendar.SECOND, 0)
        calendarStart.set(Calendar.MILLISECOND, 0)
        calendarStart.timeInMillis = (calendarStart.timeInMillis - timeZoneDefault.getOffset(startDate))


        //The for loop will search for the last value of the array. This is necessary to set the end treatment date (last alarm).
        for(i in alarmHour.indices){
            if(alarmHour[i] != null && alarmMinute[i] != null){
                endAlarmHour = alarmHour[i]!!
                endAlarmMinute = alarmMinute[i]!!
            }
        }

        //Sets the treatment end date
        val calendarEnd = Calendar.getInstance(timeZone)
        calendarEnd.timeInMillis = endDate
        calendarEnd.set(Calendar.HOUR_OF_DAY, endAlarmHour)
        calendarEnd.set(Calendar.MINUTE, endAlarmMinute)
        calendarEnd.set(Calendar.SECOND, 0)
        calendarEnd.set(Calendar.MILLISECOND, 0)
        calendarEnd.timeInMillis = (calendarEnd.timeInMillis - timeZoneDefault.getOffset(startDate))

        
        setTreatmentStartDate(calendarStart.timeInMillis)
        setTreatmentEndDate(calendarEnd.timeInMillis)

        for (i in alarmHour.indices){
            if(alarmHour[i] != null && alarmMinute[i] != null) {
                alarmInMillis[i] = getUserDate(i)
            }
        }
    }

    fun getUserDate(i: Int): Long{
        val timeZone = TimeZone.getTimeZone("UTC")
        val timeZoneDefault = TimeZone.getDefault()
        val userDate = Calendar.getInstance(timeZone)

        if(getTreatmentStartDate() != 0L){
            userDate.timeInMillis = getTreatmentStartDate()
            userDate.set(Calendar.HOUR_OF_DAY, alarmHour[i]!!)
            userDate.set(Calendar.MINUTE, alarmMinute[i]!!)
            userDate.set(Calendar.SECOND, 0)
            userDate.set(Calendar.MILLISECOND, 0)
        }else{
            userDate.set(Calendar.HOUR_OF_DAY, alarmHour[i]!!)
            userDate.set(Calendar.MINUTE, alarmMinute[i]!!)
            userDate.set(Calendar.SECOND, 0)
            userDate.set(Calendar.MILLISECOND, 0)
        }

        userDate.timeInMillis = (userDate.timeInMillis - timeZoneDefault.getOffset(userDate.timeInMillis))

        return userDate.timeInMillis
    }

    fun addAlarmItem(alarmItem: AlarmItem){
        alarmItemList.add(alarmItem)
    }

    fun removeAlarmItem(alarmItem: Int){
        alarmItemList.removeAt(alarmItem)
    }

    private fun setTreatmentStartDate(date: Long){
        treatmentStartDate = date
    }
    private fun setTreatmentEndDate(date: Long){
        treatmentEndDate = date
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

    fun getAlarmItemList(): MutableList<AlarmItem>{
        return alarmItemList
    }

    fun getMedicineQuantity(): Float {
        return medicineQuantity
    }

    fun getTreatmentStartDate(): Long{
        return treatmentStartDate
    }

    fun getMedicineForm(): String? {
        return medicineForm.value
    }

    fun getMedicineFrequency(): String{
        return medicineFrequency
    }

    fun getAlarmHoursList(): List<Int>{
        return alarmHour.toList().filterNotNull()
    }

    fun getAlarmMinutesList(): List<Int>{
        return alarmMinute.toList().filterNotNull()
    }

    fun getAlarmInMillisList(): List<Long>{
        return alarmInMillis.toList().filterNotNull()
    }

    fun getAlarmInMillis(i: Int): Long{
        return alarmInMillis[i]!!
    }

    fun getAlarmHour(arrayPosition: Int): Int{
        return alarmHour[arrayPosition]!!
    }

    fun getAlarmMinute(arrayPosition: Int): Int{
        return alarmMinute[arrayPosition]!!
    }

    fun millisToDateTime(date: Long): LocalDateTime {
        return Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    fun localDateTimeToMillis(localDateTime: LocalDateTime): Long{
        var millis = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
        millis -= TimeZone.getDefault().getOffset(millis)

        return millis
    }

    fun clearTreatmentPeriod(){
        treatmentStartDate = 0L
        treatmentEndDate = 0L
    }

}