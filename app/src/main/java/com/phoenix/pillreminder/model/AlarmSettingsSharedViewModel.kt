package com.phoenix.pillreminder.model

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.phoenix.pillreminder.alarmscheduler.AlarmItem
import com.phoenix.pillreminder.alarmscheduler.AlarmReceiver
import com.phoenix.pillreminder.alarmscheduler.AlarmScheduler
import com.phoenix.pillreminder.alarmscheduler.AndroidAlarmScheduler
import com.phoenix.pillreminder.db.Medicine
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class AlarmSettingsSharedViewModel : ViewModel() {
    private var medicineName = ""

    private var _numberOfAlarms = MutableLiveData<Int>()
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
    private var alarmInMillis = Array<Long?>(10){null}

    //Variables to set treatment period
    private var treatmentStartDate: Long = 0L
    private var treatmentEndDate: Long = 0L

    private var treatmentPeriodInMillis: Long = 0L
    private var treatmentPeriodInDays: Long = 0L

    private lateinit var pendingIntent: PendingIntent

    var position = 0


    init{
        _currentAlarmNumber.value = 1
        _numberOfAlarms.value = 1
    }

    fun allAlarmsOfTreatment(interval: Long): List<Medicine> {
        val name = medicineName
        val quantity = getMedicineQuantity()
        val form = getMedicineForm()
        val alarmInMillis = getAlarmInMillisList()
        val alarmHours = getAlarmHoursList()
        val alarmMinutes = getAlarmMinutesList()
        val startDate = treatmentStartDate
        val endDate = treatmentEndDate
        val medicineWasTaken = false
        val frequency = medicineFrequency

        val alarms = mutableListOf<Medicine>()


        //Satisfied when user sets a treatment period
        if(treatmentStartDate != 0L && treatmentEndDate != 0L){
            setTreatmentPeriodInMillis()
            setTreatmentPeriodInDays()

            for (day in 0 .. treatmentPeriodInDays step interval){
                for (i in alarmHours.indices) {
                    alarms.add( // One day has 86400000 milliseconds
                        Medicine(
                            0,
                            name,
                            quantity,
                            form!!,
                            alarmInMillis[i] + (86400000 * day),
                            alarmHours[i],
                            alarmMinutes[i],
                            startDate,
                            endDate,
                            medicineWasTaken,
                            frequency
                        )
                    )
                }
            }
        }
        //User did not set a treatment period
        else {

        }

        /*for (i in alarmHours.indices){
            alarms.add(
                Medicine(0, name, quantity, form!!, alarmInMillis[i], alarmHours[i], alarmMinutes[i], startDate, endDate, medicineWasTaken)
            )
        }*/
        return alarms
    }

    fun createAlarmItemAndSchedule(context: Context, interval: Long){
        val alarmScheduler : AlarmScheduler = AndroidAlarmScheduler(context)
        var alarmScheduled = false

        for(day in 0 .. getTreatmentPeriodInDays() step interval){
            for(i in 0 until getAlarmHoursList().size){
                val alarmItem = AlarmItem (
                    time = millisToDateTime(getAlarmInMillis(i)),
                    medicineName = "${getMedicineName()}",
                    medicineForm = "${getMedicineForm()}",
                    medicineQuantity = "${getMedicineQuantity()}",
                    alarmHour = "${getAlarmHour(i)}",
                    alarmMinute = "${getAlarmMinute(i)}"
                )

                // It schedules only the first alarm. The next alarm will be set when the first alarm is triggered.
                if (!alarmScheduled){
                    alarmItem.let(alarmScheduler::scheduleAlarm)
                    alarmScheduled = true
                }
            }
        }
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
    fun getMedicineName(): String {
        return medicineName
    }

    private fun setTreatmentPeriod(startDate: Long, endDate: Long){
        Log.i("DATES", "$startDate $endDate")
        val timeZone = TimeZone.getTimeZone("UTC")
        val timeZoneDefault = TimeZone.getDefault()

        var endAlarmHour = 0
        var endAlarmMinute = 0

        //Sets the treatment start date
        val calendarStart = Calendar.getInstance(timeZone)
        calendarStart.timeInMillis = startDate
        Log.i("DATES HOUR", "${alarmHour[0]} ${alarmMinute[0]}")
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

    private fun setTreatmentStartDate(date: Long){
        treatmentStartDate = date
    }
    private fun setTreatmentEndDate(date: Long){
        treatmentEndDate = date
    }
    fun setMedicineName(userInput: String){
        medicineName = userInput
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

    fun getTreatmentPeriodInDays(): Long{
        return treatmentPeriodInDays
    }

    private fun setTreatmentPeriodInMillis(){
        treatmentPeriodInMillis = treatmentEndDate - treatmentStartDate
    }

    private fun setTreatmentPeriodInDays(){
        treatmentPeriodInDays = TimeUnit.MILLISECONDS.toDays(treatmentPeriodInMillis)
    }

}