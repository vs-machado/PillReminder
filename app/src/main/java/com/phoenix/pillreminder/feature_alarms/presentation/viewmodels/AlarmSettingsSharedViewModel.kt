package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.phoenix.pillreminder.feature_alarms.data.worker.RescheduleWorker
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmHour
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.ExpiredMedicinesInfo
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.domain.util.MedicineFrequency
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Calendar
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.DAY_OF_WEEK
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.MILLISECOND
import java.util.Calendar.MINUTE
import java.util.Calendar.MONTH
import java.util.Calendar.SECOND
import java.util.Calendar.YEAR
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AlarmSettingsSharedViewModel @Inject constructor(
    private val repository: MedicineRepository,
    private val workManager: WorkManager,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {
    private var medicineName = ""

    private var alarmsPerDay = 1

    private var _currentAlarmNumber = MutableLiveData<Int>()
    val currentAlarmNumber: LiveData<Int> = _currentAlarmNumber

    private var _medicineForm = MutableLiveData("")
    val medicineForm: LiveData<String> = _medicineForm

    private lateinit var doseUnit: String

    private var medicineQuantity = 0F
    private lateinit var medicineFrequency: MedicineFrequency

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

    private lateinit var selectedDaysList: MutableSet<Int>
    private lateinit var treatmentID: String

    private var interval: Int = 0

    var position = 0

    private lateinit var workRequestID: UUID

    init{
        _currentAlarmNumber.postValue(1)
    }

    // Used with medicine frequency every day or every other day with treatment period set.
    fun getAlarmsList(interval: Long, editTimestamp: Long?, generateTreatmentID: Boolean): List<Medicine> {
        val name = medicineName
        val quantity = getMedicineQuantity()
        val doseUnit = getDoseUnit()
        val form = getMedicineForm()
        val alarmsPerDay = getAlarmsPerDay()
        val alarmInMillis = getAlarmInMillisList()
        val alarmHours = getAlarmHoursList()
        val alarmMinutes = getAlarmMinutesList()
        val startDate = treatmentStartDate
        val endDate = treatmentEndDate
        val medicineWasTaken = false
        val frequency = medicineFrequency.toString()
        val periodSet = medicinePeriodSet
        val needsReschedule = medicineNeedsReschedule
        val alarms = mutableListOf<Medicine>()
        val treatmentID = when(generateTreatmentID){
            true -> { UUID.randomUUID().toString() }
            false -> { getTreatmentID() }
        }

        val scheduleInterval = when(frequency){
            "EveryXWeeks" -> { interval * 7 }
            "EveryXMonths" -> { interval * 30 }
            else -> { interval }
        }

        if (treatmentStartDate != 0L && treatmentEndDate != 0L) {
            setTreatmentPeriodInMillis()
            setTreatmentPeriodInDays()

            for (day in 0..treatmentPeriodInDays step scheduleInterval) {
                for (i in alarmHours.indices) {
                    if ((alarmInMillis[i] + (86400000 * day)) > System.currentTimeMillis()) {
                        alarms.add( // One day has 86400000 milliseconds
                            Medicine(
                                0,
                                name,
                                quantity,
                                doseUnit,
                                form!!,
                                alarmsPerDay,
                                alarmInMillis[i] + (86400000 * day),
                                alarmHours[i],
                                alarmMinutes[i],
                                null,
                                startDate,
                                endDate,
                                medicineWasTaken,
                                false,
                                frequency,
                                interval,
                                periodSet,
                                needsReschedule,
                                "noID",
                                0L,
                                true,
                                treatmentID
                            ).let { medicine ->
                                // Adds an edit timestamp when user edit medicines. Used to filter the medicine alarms list in editmedicinesfragment.
                                if(editTimestamp != null){
                                    medicine.copy(lastEdited = editTimestamp)
                                } else {
                                    medicine
                                }
                            }
                        )
                    }
                }
            }
        }

        return alarms
    }

    // If user does not specify the treatment period,
    // a worker will be used to reinsert the medicines in the database
    // used with medicine frequency every day or every other day
    fun getAlarmsList(interval: Long, workerID: UUID, editTimestamp: Long?, generateTreatmentID: Boolean): List<Medicine> {
        val name = medicineName
        val quantity = getMedicineQuantity()
        val doseUnit = getDoseUnit()
        val form = getMedicineForm()
        val alarmsPerDay = getAlarmsPerDay()
        val alarmInMillis = getAlarmInMillisList()
        val alarmHours = getAlarmHoursList()
        val alarmMinutes = getAlarmMinutesList()
        val startDate = treatmentStartDate
        val endDate = treatmentEndDate
        val medicineWasTaken = false
        val frequency = medicineFrequency.toString()
        val periodSet = medicinePeriodSet
        val needsReschedule = medicineNeedsReschedule
        val treatmentID = when(generateTreatmentID){
            true -> { UUID.randomUUID().toString() }
            false -> { getTreatmentID() }
        }

        val alarms = mutableListOf<Medicine>()

        val scheduleInterval = when(frequency){
            "EveryXWeeks" -> { interval * 7 }
            "EveryXMonths" -> { interval * 30 }
            else -> { interval }
        }

        if(treatmentStartDate != 0L && treatmentEndDate != 0L){
            setTreatmentPeriodInMillis()
            setTreatmentPeriodInDays()

            for (day in 0 .. treatmentPeriodInDays step scheduleInterval){
                for (i in alarmHours.indices) {
                    if((alarmInMillis[i] + (86400000 * day)) > System.currentTimeMillis()){
                        alarms.add( // One day has 86400000 milliseconds
                            Medicine(
                                0,
                                name,
                                quantity,
                                doseUnit,
                                form!!,
                                alarmsPerDay,
                                alarmInMillis[i] + (86400000 * day),
                                alarmHours[i],
                                alarmMinutes[i],
                                null,
                                startDate,
                                endDate,
                                medicineWasTaken,
                                false,
                                frequency,
                                interval,
                                periodSet,
                                needsReschedule,
                                workerID.toString(),
                                0L,
                                true,
                                treatmentID
                            ).let { medicine ->
                                // Adds an edit timestamp when user edit medicines. Used to filter the medicine alarms list in editmedicinesfragment.
                                if(editTimestamp != null){
                                    medicine.copy(lastEdited = editTimestamp)
                                } else {
                                    medicine
                                }
                            }
                        )
                    }
                }
            }

        }

        return alarms
    }

    // Used with medicine frequency Specific days of week
    fun getAlarmsListForSpecificDays(workerID: UUID, editTimestamp: Long?, generateTreatmentID: Boolean): List<Medicine> {
        val alarms = mutableListOf<Medicine>()
        val selectedDays = getSelectedDaysList()
        val processedDays = mutableSetOf<Long>()
        val treatmentID = when(generateTreatmentID){
            true -> { UUID.randomUUID().toString() }
            false -> { getTreatmentID() }
        }


        if (treatmentStartDate != 0L && treatmentEndDate != 0L) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = treatmentStartDate

            while (calendar.timeInMillis <= treatmentEndDate) {
                for (selectedDay in selectedDays) {
                    val daysToAdd = (selectedDay - calendar.get(DAY_OF_WEEK) + 7) % 7
                    val alarmCalendar = calendar.clone() as Calendar
                    alarmCalendar.add(DAY_OF_WEEK, daysToAdd)

                    val dayKey = alarmCalendar.get(YEAR) * 10000L +
                            alarmCalendar.get(MONTH) * 100 +
                            alarmCalendar.get(DAY_OF_MONTH)

                    if (alarmCalendar.timeInMillis <= treatmentEndDate &&
                        !processedDays.contains(dayKey)) {

                        processedDays.add(dayKey)

                        val alarmHours = getAlarmHoursList()
                        val alarmMinutes = getAlarmMinutesList()

                        for (i in 0 until alarmsPerDay) {
                            if (i < alarmHours.size && i < alarmMinutes.size) {
                                alarmCalendar.set(HOUR_OF_DAY, alarmHours[i])
                                alarmCalendar.set(MINUTE, alarmMinutes[i])
                                alarmCalendar.set(SECOND, 0)
                                alarmCalendar.set(MILLISECOND, 0)

                                if (alarmCalendar.timeInMillis > System.currentTimeMillis()) {
                                    alarms.add(
                                        Medicine(
                                            0,
                                            medicineName,
                                            getMedicineQuantity(),
                                            getDoseUnit(),
                                            getMedicineForm()!!,
                                            alarmsPerDay,
                                            alarmCalendar.timeInMillis,
                                            alarmHours[i],
                                            alarmMinutes[i],
                                            selectedDays,
                                            treatmentStartDate,
                                            treatmentEndDate,
                                            medicineWasTaken = false,
                                            wasSkipped = false,
                                            medicineFrequency = medicineFrequency.toString(),
                                            interval = 0, // interval is no longer used
                                            medicinePeriodSet = medicinePeriodSet,
                                            medicineNeedsReschedule = medicineNeedsReschedule,
                                            rescheduleWorkerID = workerID.toString(),
                                            0L,
                                            true,
                                            treatmentID
                                        ).let { medicine ->
                                            // Adds an edit timestamp when user edit medicines. Used to filter the medicine alarms list in editmedicinesfragment.
                                            if(editTimestamp != null){
                                                medicine.copy(lastEdited = editTimestamp)
                                            } else {
                                                medicine
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                calendar.add(DAY_OF_WEEK, 1)
            }
        }

        return alarms
    }

    // Same as above, but without worker
    fun getAlarmsListForSpecificDays(editTimestamp: Long?, generateTreatmentID: Boolean): List<Medicine> {
        val alarms = mutableListOf<Medicine>()
        val selectedDays = getSelectedDaysList()
        val processedDays = mutableSetOf<Long>()
        val treatmentID = when(generateTreatmentID){
            true -> { UUID.randomUUID().toString() }
            false -> { getTreatmentID() }
        }


        if (treatmentStartDate != 0L && treatmentEndDate != 0L) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = treatmentStartDate

            while (calendar.timeInMillis <= treatmentEndDate) {
                for (selectedDay in selectedDays) {
                    val daysToAdd = (selectedDay - calendar.get(DAY_OF_WEEK) + 7) % 7
                    val alarmCalendar = calendar.clone() as Calendar
                    alarmCalendar.add(DAY_OF_WEEK, daysToAdd)

                    val dayKey = alarmCalendar.get(YEAR) * 10000L +
                            alarmCalendar.get(MONTH) * 100 +
                            alarmCalendar.get(DAY_OF_MONTH)

                    if (alarmCalendar.timeInMillis <= treatmentEndDate &&
                        !processedDays.contains(dayKey)) {

                        processedDays.add(dayKey)

                        val alarmHours = getAlarmHoursList()
                        val alarmMinutes = getAlarmMinutesList()

                        for (i in 0 until alarmsPerDay) {
                            if (i < alarmHours.size && i < alarmMinutes.size) {
                                alarmCalendar.set(HOUR_OF_DAY, alarmHours[i])
                                alarmCalendar.set(MINUTE, alarmMinutes[i])
                                alarmCalendar.set(SECOND, 0)
                                alarmCalendar.set(MILLISECOND, 0)

                                if (alarmCalendar.timeInMillis > System.currentTimeMillis()) {
                                    alarms.add(
                                        Medicine(
                                            0,
                                            medicineName,
                                            getMedicineQuantity(),
                                            getDoseUnit(),
                                            getMedicineForm()!!,
                                            alarmsPerDay,
                                            alarmCalendar.timeInMillis,
                                            alarmHours[i],
                                            alarmMinutes[i],
                                            selectedDays,
                                            treatmentStartDate,
                                            treatmentEndDate,
                                            medicineWasTaken = false,
                                            wasSkipped = false,
                                            medicineFrequency = medicineFrequency.toString(),
                                            interval = 0,
                                            medicinePeriodSet = medicinePeriodSet,
                                            medicineNeedsReschedule = medicineNeedsReschedule,
                                            rescheduleWorkerID = "noID",
                                            0L,
                                            true,
                                            treatmentID
                                        ).let { medicine ->
                                            // Adds an edit timestamp when user edit medicines. Used to filter the medicine alarms list in editmedicinesfragment.
                                            if(editTimestamp != null){
                                                medicine.copy(lastEdited = editTimestamp)
                                            } else {
                                                medicine
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                calendar.add(DAY_OF_WEEK, 1)
            }
        }

        return alarms
    }

    fun createAlarmItemAndSchedule(interval: Long) {
        val scheduleInterval = when (getMedicineFrequency()) {
            MedicineFrequency.EveryXWeeks -> interval * 7
            MedicineFrequency.EveryXMonths -> interval * 30
            else -> interval
        }

        val alarmItems = mutableListOf<AlarmItem>()

        for (day in 0..getTreatmentPeriodInDays() step scheduleInterval) {
            for (i in getAlarmHoursList().indices) {
                val alarmTimeMillis = getAlarmInMillis(i) + day * 24 * 60 * 60 * 1000 // Add days to alarm time

                val alarmItem = AlarmItem(
                    time = millisToDateTime(alarmTimeMillis),
                    medicineName = getMedicineName(),
                    medicineForm = "${getMedicineForm()}",
                    medicineQuantity = getMedicineQuantity().toString(),
                    doseUnit = getDoseUnit(),
                    alarmHour = getAlarmHour(i).toString(),
                    alarmMinute = getAlarmMinute(i).toString()
                )

                alarmItems.add(alarmItem)
            }
        }

        // Sort alarms by time
        alarmItems.sortBy { it.time }

        // Find and schedule the first future alarm
        val currentTime = System.currentTimeMillis()
        val firstFutureAlarm = alarmItems.find {
           it.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() > currentTime
        }

        firstFutureAlarm?.let { alarmScheduler.scheduleAlarm(it) }
    }

    fun setTreatmentID(treatmentID: String) {
        this.treatmentID = treatmentID
    }

    fun getTreatmentID(): String {
        return treatmentID
    }

    //Used with frequency specific days of week
    fun createAlarmItemAndSchedule() {
        val alarmItems = mutableListOf<AlarmItem>()

        for (i in getAlarmHoursList().indices) {
            val alarmTimeMillis = getAlarmInMillis(i)

            val alarmItem = AlarmItem(
                time = millisToDateTime(alarmTimeMillis),
                medicineName = getMedicineName(),
                medicineForm = "${getMedicineForm()}",
                medicineQuantity = "${getMedicineQuantity()}",
                doseUnit = getDoseUnit(),
                alarmHour = "${getAlarmHour(i)}",
                alarmMinute ="${getAlarmMinute(i)}"
            )

            alarmItems.add(alarmItem)
        }

        // Sort alarms by time
        alarmItems.sortBy { it.time }

        // Find and schedule the first future alarm
        val currentTime = System.currentTimeMillis()
        val firstFutureAlarm = alarmItems.find {
            it.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() > currentTime
        }

        firstFutureAlarm?.let { alarmScheduler.scheduleAlarm(it) }
    }

    fun getExpiredMedicinesUpdatedInfo(medicine: Medicine): ExpiredMedicinesInfo {
        return ExpiredMedicinesInfo(
            medicine.treatmentID,
            getMedicineName(),
            getMedicineQuantity(),
            getMedicineForm().toString(),
            getTreatmentEndDate(),
            getMedicineFrequency().toString(),
            System.currentTimeMillis()
        )
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
    fun clearRemainingAlarmArrayPositions(){
        for(i in currentAlarmNumber.value!! - 1 until alarmHour.indices.last){
            alarmHour[i] = null
            alarmMinute[i] = null
        }
    }

    fun clearAlarmArray(){
        alarmHour = Array(10){null}
        alarmMinute = Array(10){null}
    }

    fun extractDateComponents(firstDate: Long, secondDate: Long, userSetPeriod: Boolean){
        when(userSetPeriod){
            true -> {
                val userTimeZone = TimeZone.getDefault()
                val timeZoneOffset = userTimeZone.getOffset(firstDate)

                val startDate = Calendar.getInstance().apply { timeInMillis = firstDate - timeZoneOffset }
                val endDate = Calendar.getInstance().apply { timeInMillis = secondDate - timeZoneOffset }

                setTreatmentPeriod(startDate.timeInMillis, endDate.timeInMillis)
            }
            false -> {
                val startDate = Calendar.getInstance().apply { timeInMillis = firstDate }
                val endDate = Calendar.getInstance().apply { timeInMillis = secondDate }

                setTreatmentPeriod(startDate.timeInMillis, endDate.timeInMillis)
            }
        }

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

    fun setMedicineForm(medicineForm: String){
        _medicineForm.value = medicineForm
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
        calendarStart.set(HOUR_OF_DAY, alarmHour[0]!!)
        calendarStart.set(MINUTE, alarmMinute[0]!!)
        calendarStart.set(SECOND, 0)
        calendarStart.set(MILLISECOND, 0)

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
        calendarEnd.set(HOUR_OF_DAY, endAlarmHour)
        calendarEnd.set(MINUTE, endAlarmMinute)
        calendarEnd.set(SECOND, 0)
        calendarEnd.set(MILLISECOND, 0)

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
            userDate.set(HOUR_OF_DAY, alarmHour[i]!!)
            userDate.set(MINUTE, alarmMinute[i]!!)
            userDate.set(SECOND, 0)
            userDate.set(MILLISECOND, 0)
        } else {
            userDate.set(HOUR_OF_DAY, alarmHour[i]!!)
            userDate.set(MINUTE, alarmMinute[i]!!)
            userDate.set(SECOND, 0)
            userDate.set(MILLISECOND, 0)
        }

        return userDate.timeInMillis
    }

    fun createRescheduleWorker(context: Context): UUID {
        val repeatInterval = when(medicineFrequency){
            MedicineFrequency.EveryDay, MedicineFrequency.SpecificDaysOfWeek,
            MedicineFrequency.EveryOtherDay -> 32
            MedicineFrequency.EveryXDays -> getInterval() * 6
            MedicineFrequency.EveryXWeeks -> getInterval() * 13
            MedicineFrequency.EveryXMonths -> getInterval() * 32
        }
        val rescheduleRequest = PeriodicWorkRequestBuilder<RescheduleWorker>(repeatInterval.toLong(), TimeUnit.DAYS)
            .setInitialDelay(repeatInterval.toLong(), TimeUnit.DAYS)
            .build()
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(rescheduleRequest)

        return rescheduleRequest.id
    }

    fun cancelWork(medicine: Medicine){
        val workerID = medicine.rescheduleWorkerID

        if(workerID != "noID"){
            workRequestID = UUID.fromString(workerID)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val hasNextAlarm = repository.hasNextAlarmData(medicine.name, System.currentTimeMillis())

            withContext(Dispatchers.Default){
                if(!hasNextAlarm && workerID != "noID"){
                    workManager.cancelWorkById(workRequestID)
                    Log.d("debug", "cancel work")
                }
            }
        }
    }

    fun convertTimeListToArrays(timeList: List<AlarmHour>) {
        timeList.forEachIndexed { index, timeString ->
            // Split the time string into time and period (AM/PM)
            // If there's no period (24-hour format), use an empty string
            val (time, period) = timeString.alarmHour.split(' ', limit = 2).let {
                if (it.size == 2) it else listOf(it[0], "")
            }

            // Split the time into hour and minute
            val (hour, minute) = time.split(':').map(String::toInt)

            // Adjust the hour based on the period (AM/PM)
            val adjustedHour = when {
                // If it's PM and not 12, add 12 to convert to 24-hour format
                period.equals("PM", ignoreCase = true) && hour != 12 -> hour + 12
                // If it's 12 AM, set to 0 (midnight in 24-hour format)
                period.equals("AM", ignoreCase = true) && hour == 12 -> 0
                // For all other cases (AM times, 12 PM, or 24-hour format), use the hour as is
                else -> hour
            }

            // Store the adjusted hour and minute in their respective arrays
            alarmHour[index] = adjustedHour
            alarmMinute[index] = minute
        }
    }

    fun setSelectedDaysList(mutableList: MutableSet<Int>){
        this.selectedDaysList = mutableList
    }

    private fun getSelectedDaysList(): MutableSet<Int>{
        return selectedDaysList
    }

    fun setTemporaryPeriod(){
        medicinePeriodSet = false
        medicineNeedsReschedule = true
    }

    fun userWillSetPeriod(){
        medicinePeriodSet = true
        medicineNeedsReschedule = false
    }
    fun setTreatmentStartDate(date: Long){
        treatmentStartDate = date
    }
    fun setTreatmentEndDate(date: Long){
        treatmentEndDate = date
    }
    fun setMedicineName(userInput: String){
        medicineName = userInput
    }

    fun setMedicineQuantity(quantity: Float){
        medicineQuantity = quantity
    }

    fun setMedicineFrequency(frequency: MedicineFrequency){
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

    fun setDoseUnit(unit: String){
        this.doseUnit = unit
    }

    fun getDoseUnit(): String {
        return this.doseUnit
    }

    fun getAlarmsPerDay(): Int{
        return alarmsPerDay
    }

    fun getMedicineForm(): String? {
        return medicineForm.value
    }

    fun getMedicineFrequency(): MedicineFrequency {
        return medicineFrequency
    }

    fun getAlarmHoursList(): List<Int>{
        return alarmHour.toList().filterNotNull()
    }

    fun getAlarmMinutesList(): List<Int>{
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

    fun setInterval(interval: Int){
        this.interval = interval
    }

    fun getInterval(): Int {
        return interval
    }

    fun getTreatmentEndDate(): Long{
        return treatmentEndDate
    }

    fun setTemporaryTreatmentEndDate(startDateMillis: Long): Long {
        // A day has 87400000 milliseconds
        val repeatInterval = when(medicineFrequency){
            MedicineFrequency.EveryDay, MedicineFrequency.SpecificDaysOfWeek,
            MedicineFrequency.EveryOtherDay -> 33
            MedicineFrequency.EveryXDays -> getInterval() * 7
            MedicineFrequency.EveryXWeeks -> getInterval() * 14
            MedicineFrequency.EveryXMonths -> getInterval() * 33
        }

        return (startDateMillis + (repeatInterval * 86400000L))
    }
}