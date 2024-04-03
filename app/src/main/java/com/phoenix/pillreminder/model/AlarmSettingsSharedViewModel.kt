package com.phoenix.pillreminder.model

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.phoenix.pillreminder.alarmscheduler.AlarmItem
import com.phoenix.pillreminder.alarmscheduler.AlarmScheduler
import com.phoenix.pillreminder.alarmscheduler.AndroidAlarmScheduler
import com.phoenix.pillreminder.db.Medicine
import com.phoenix.pillreminder.worker.RescheduleWorker
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class AlarmSettingsSharedViewModel : ViewModel() {
    private var medicineName = ""

    private var alarmsPerDay = 1

    private var _currentAlarmNumber = MutableLiveData<Int>()
    val currentAlarmNumber: LiveData<Int> = _currentAlarmNumber

    private var _medicineForm = MutableLiveData("")
    val medicineForm: LiveData<String> = _medicineForm

    private var medicineQuantity = 0F
    private var medicineFrequency = 0

    //Variables to store as many alarms as the user wants
    private var alarmHour = Array<Int?>(10){null}
    private var alarmMinute = Array<Int?>(10){null}
    private var alarmInMillis = Array<Long?>(10){null}

    //Variables to set treatment period
    private var treatmentStartDate: Long = 0L
    private var treatmentEndDate: Long = 0L

    private var treatmentPeriodInMillis: Long = 0L
    private var treatmentPeriodInDays: Long = 0L

    //Used for alarms rescheduling if user does not set a treatment period
    private var medicinePeriodSet = true
    private var medicineNeedsReschedule = false

    var position = 0


    init{
        _currentAlarmNumber.value = 1
    }

    fun allAlarmsOfTreatment(interval: Long): List<Medicine> {
        val name = medicineName
        val quantity = getMedicineQuantity()
        val form = getMedicineForm()
        val alarmsPerDay = getAlarmsPerDay()
        val alarmInMillis = getAlarmInMillisList()
        val alarmHours = getAlarmHoursList()
        val alarmMinutes = getAlarmMinutesList()
        val startDate = treatmentStartDate
        val endDate = treatmentEndDate
        val medicineWasTaken = false
        val frequency = medicineFrequency
        val periodSet = medicinePeriodSet
        val needsReschedule = medicineNeedsReschedule

        val alarms = mutableListOf<Medicine>()


        if(treatmentStartDate != 0L && treatmentEndDate != 0L){
            setTreatmentPeriodInMillis()
            setTreatmentPeriodInDays()

            for (day in 0 .. treatmentPeriodInDays step interval){
                for (i in alarmHours.indices) {
                    if((alarmInMillis[i] + (86400000 * day)) > System.currentTimeMillis()){
                        alarms.add( // One day has 86400000 milliseconds
                            Medicine(
                                0,
                                name,
                                quantity,
                                form!!,
                                alarmsPerDay,
                                alarmInMillis[i] + (86400000 * day),
                                alarmHours[i],
                                alarmMinutes[i],
                                startDate,
                                endDate,
                                medicineWasTaken,
                                frequency,
                                periodSet,
                                needsReschedule
                            )
                        )
                    }
                }
            }
        }

        return alarms
    }

    fun createAlarmItemAndSchedule(context: Context, interval: Long){
        val alarmScheduler : AlarmScheduler = AndroidAlarmScheduler(context)
        var alarmScheduled = false

        for(day in 0 .. getTreatmentPeriodInDays() step interval){
            for(i in 0 until getAlarmHoursList().size){
                if(getAlarmInMillis(i) > System.currentTimeMillis()){
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

    fun createRescheduleWorker(context: Context){
        val rescheduleRequest = PeriodicWorkRequestBuilder<RescheduleWorker>(27, TimeUnit.DAYS)
            .setInitialDelay(27, TimeUnit.DAYS)
            .build()
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            "unique_reschedule_work",
            ExistingPeriodicWorkPolicy.KEEP,
            rescheduleRequest)
    }

    fun setTemporaryPeriod(){
        medicinePeriodSet = false
        medicineNeedsReschedule = true
    }

    fun userWillSetPeriod(){
        medicinePeriodSet = true
        medicineNeedsReschedule = false
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

    fun setMedicineFrequency(frequency: Int){
        medicineFrequency = frequency
    }

    fun setNumberOfAlarms(newNumberOfAlarms: Int){
        alarmsPerDay = newNumberOfAlarms
    }

    fun getMedicineQuantity(): Float {
        return medicineQuantity
    }

    fun getTreatmentStartDate(): Long{
        return treatmentStartDate
    }

    fun getAlarmsPerDay(): Int{
        return alarmsPerDay
    }

    fun getMedicineForm(): String? {
        return medicineForm.value
    }

    fun getMedicineFrequency(): Int{
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

    private fun getTreatmentPeriodInDays(): Long{
        return treatmentPeriodInDays
    }

    private fun setTreatmentPeriodInMillis(){
        treatmentPeriodInMillis = treatmentEndDate - treatmentStartDate
    }

    private fun setTreatmentPeriodInDays(){
        treatmentPeriodInDays = TimeUnit.MILLISECONDS.toDays(treatmentPeriodInMillis)
    }

}