package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.phoenix.pillreminder.feature_alarms.data.worker.RescheduleWorker
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.AlarmScheduler
import com.phoenix.pillreminder.feature_alarms.presentation.AndroidAlarmScheduler
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
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
                                false,
                                frequency,
                                periodSet,
                                needsReschedule,
                                "noID"
                            )
                        )
                    }
                }
            }
        }

        return alarms
    }

    fun allAlarmsOfTreatment(interval: Long, workerID: UUID): List<Medicine> {
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
                                false,
                                frequency,
                                periodSet,
                                needsReschedule,
                                workerID.toString()
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
    private fun getMedicineName(): String {
        return medicineName
    }

    private fun setTreatmentPeriod(startDate: Long, endDate: Long) {
        val timeZoneDefault = TimeZone.getDefault()
        val calendarStart = Calendar.getInstance(timeZoneDefault)
        val calendarEnd = Calendar.getInstance(timeZoneDefault)

        // Set treatment start date
        calendarStart.timeInMillis = startDate
        calendarStart.set(Calendar.HOUR_OF_DAY, alarmHour[0]!!)
        calendarStart.set(Calendar.MINUTE, alarmMinute[0]!!)
        calendarStart.set(Calendar.SECOND, 0)
        calendarStart.set(Calendar.MILLISECOND, 0)

        var endAlarmHour = 0
        var endAlarmMinute = 0

        // Find the last alarm's hour and minute
        for (i in alarmHour.indices) {
            if (alarmHour[i] != null && alarmMinute[i] != null) {
                endAlarmHour = alarmHour[i]!!
                endAlarmMinute = alarmMinute[i]!!
            }
        }

        // Set treatment end date
        calendarEnd.timeInMillis = endDate
        calendarEnd.set(Calendar.HOUR_OF_DAY, endAlarmHour)
        calendarEnd.set(Calendar.MINUTE, endAlarmMinute)
        calendarEnd.set(Calendar.SECOND, 0)
        calendarEnd.set(Calendar.MILLISECOND, 0)

        setTreatmentStartDate(calendarStart.timeInMillis)
        setTreatmentEndDate(calendarEnd.timeInMillis)

        // Update alarmInMillis array with the adjusted treatment start date
        for (i in alarmHour.indices) {
            if (alarmHour[i] != null && alarmMinute[i] != null) {
                alarmInMillis[i] = getUserDate(i)
            }
        }
    }

    private fun getUserDate(i: Int): Long {
        val timeZoneDefault = TimeZone.getDefault()
        val userDate = Calendar.getInstance(timeZoneDefault)

        if (getTreatmentStartDate() != 0L) {
            userDate.timeInMillis = getTreatmentStartDate()
            userDate.set(Calendar.HOUR_OF_DAY, alarmHour[i]!!)
            userDate.set(Calendar.MINUTE, alarmMinute[i]!!)
            userDate.set(Calendar.SECOND, 0)
            userDate.set(Calendar.MILLISECOND, 0)
        } else {
            userDate.set(Calendar.HOUR_OF_DAY, alarmHour[i]!!)
            userDate.set(Calendar.MINUTE, alarmMinute[i]!!)
            userDate.set(Calendar.SECOND, 0)
            userDate.set(Calendar.MILLISECOND, 0)
        }

        return userDate.timeInMillis
    }

    fun createRescheduleWorker(context: Context): UUID {
        val rescheduleRequest = PeriodicWorkRequestBuilder<RescheduleWorker>(32, TimeUnit.DAYS)
            .setInitialDelay(32, TimeUnit.DAYS)
            .build()
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(rescheduleRequest)

        return rescheduleRequest.id
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

    private fun getMedicineQuantity(): Float {
        return medicineQuantity
    }

    private fun getTreatmentStartDate(): Long{
        return treatmentStartDate
    }

    fun getAlarmsPerDay(): Int{
        return alarmsPerDay
    }

    private fun getMedicineForm(): String? {
        return medicineForm.value
    }

    fun getMedicineFrequency(): Int{
        return medicineFrequency
    }

    private fun getAlarmHoursList(): List<Int>{
        return alarmHour.toList().filterNotNull()
    }

    private fun getAlarmMinutesList(): List<Int>{
        return alarmMinute.toList().filterNotNull()
    }

    private fun getAlarmInMillisList(): List<Long>{
        return alarmInMillis.toList().filterNotNull()
    }

    private fun getAlarmInMillis(i: Int): Long{
        return alarmInMillis[i]!!
    }

    private fun getAlarmHour(arrayPosition: Int): Int{
        return alarmHour[arrayPosition]!!
    }

    private fun getAlarmMinute(arrayPosition: Int): Int{
        return alarmMinute[arrayPosition]!!
    }

    private fun millisToDateTime(date: Long): LocalDateTime {
        return Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDateTime()
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