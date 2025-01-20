package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.domain.repository.SharedPreferencesRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeFragmentViewModel @Inject constructor(
    private val spRepository: SharedPreferencesRepository,
    private val alarmScheduler: AlarmScheduler,
    private val repository: MedicineRepository,
    @ApplicationContext private val appContext: Context
): ViewModel() {

    private val _permissionRequestPreferences = MutableStateFlow(false)
    val permissionRequestPreferences: StateFlow<Boolean> = _permissionRequestPreferences

    private val _pillboxReminderPreferences = MutableStateFlow(false)
    val pillboxReminderPreferences: StateFlow<Boolean> = _pillboxReminderPreferences

    private val _alarmReschedulePreferences = MutableStateFlow(false)
    val alarmReschedulePreferences: StateFlow<Boolean> = _alarmReschedulePreferences

    init {
        _permissionRequestPreferences.value = spRepository.getPermissionRequestPreferences()
        _pillboxReminderPreferences.value = spRepository.getPillboxPreferences()
        _alarmReschedulePreferences.value = spRepository.getAlarmReschedulePreferences()
    }

    private var date: Date = Calendar.getInstance().time

    fun setDate(selectedDate: Date){
        date = selectedDate
    }

    fun getDate(): Date{
        return date
    }

    fun schedulePillboxReminder(hours: Int, minutes: Int){
        alarmScheduler.schedulePillboxReminder(hours, minutes)
        setPillboxPreferences(true)
    }

    fun markMedicineUsage(medicine: Medicine) = viewModelScope.launch{
        medicine.medicineWasTaken = true

        withContext(Dispatchers.IO){
            // Mark medicine usage
            repository.updateMedicine(medicine)

            // Mark previous not used medicines as skipped
            repository.updateMedicinesAsSkipped(medicine.treatmentID, medicine.alarmInMillis)
        }
    }

    fun markMedicinesAsSkipped(medicine: Medicine){
        val alarmTime = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

        val alarmItem = AlarmItem(
            alarmTime,
            medicine.name,
            medicine.form,
            medicine.quantity.toString(),
            medicine.unit,
            medicine.alarmHour.toString(),
            medicine.alarmMinute.toString()
        )

        medicine.wasSkipped = true

        viewModelScope.launch(Dispatchers.IO){
            repository.updateMedicine(medicine)

            withContext(Dispatchers.Default){
                /*Checks if the alarm was already triggered. If so, there is no need to cancel the broadcast.
                cancelAlarm() will cancel the alarm and check if there is another alarm to be scheduled*/
                if(medicine.alarmInMillis > System.currentTimeMillis()){
                    alarmScheduler.cancelAlarm(alarmItem, false)
                }
            }

            val hasNextAlarm = repository.hasNextAlarmData(medicine.name, System.currentTimeMillis())

            withContext(Dispatchers.IO){
                if(!hasNextAlarm){
                    val workRequestID = UUID.fromString(repository.getWorkerID(medicine.name, medicine.treatmentID))

                    withContext(Dispatchers.Main){
                        WorkManager.getInstance(appContext).cancelWorkById(workRequestID)
                    }
                }
            }
        }
    }

    fun isNextToAnotherDoseHour(selectedMedicine: Medicine, callback: (Boolean) -> Unit){
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val usageHour = selectedMedicine.alarmInMillis
                val nextAlarmHour = repository.getNextAlarmData(selectedMedicine.name, selectedMedicine.alarmInMillis)?.alarmInMillis

                if (nextAlarmHour != null) {
                    val intervalBetweenAlarms = nextAlarmHour - usageHour
                    val closeToNextAlarm = (System.currentTimeMillis() - usageHour) > ((2.0/3.0) * intervalBetweenAlarms)
                    val pastTheNextAlarmHour = System.currentTimeMillis() > nextAlarmHour

                    closeToNextAlarm || pastTheNextAlarmHour
                } else {
                    false
                }
            }
            callback(result)
        }
    }

    fun deleteAllMedicinesWithSameName(name: String) =  viewModelScope.launch(Dispatchers.IO){
            val alarmsToDelete = repository.getAllMedicinesWithSameName(name)
            repository.deleteAllSelectedMedicines(alarmsToDelete)
    }
//14:10

    fun cancelReminderNotifications(context: Context){
//        WorkManager.getInstance(context).cancelUniqueWork("PillboxReminder")
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(999)
        alarmScheduler.cancelPillboxReminder()
    }

    fun cancelAlarm(medicine: Medicine, cancelAll: Boolean){
        val alarmTime = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

        val alarmItem = AlarmItem(
            alarmTime,
            medicine.name,
            medicine.form,
            medicine.quantity.toString(),
            medicine.unit,
            medicine.alarmHour.toString(),
            medicine.alarmMinute.toString()
        )

        viewModelScope.launch(Dispatchers.Default) {
            alarmScheduler.cancelAlarm(alarmItem, cancelAll)
        }
    }

    fun setPermissionRequestPreferences(boolean: Boolean){
        viewModelScope.launch(Dispatchers.IO) {
            spRepository.setPermissionRequestPreferences(boolean)
        }
        _permissionRequestPreferences.value = boolean
    }

    fun setPillboxPreferences(boolean: Boolean){
        viewModelScope.launch(Dispatchers.IO){
            spRepository.setPillboxPreferences(boolean)
        }
        _pillboxReminderPreferences.value = boolean
    }

    fun setAlarmReschedulePreferences(boolean: Boolean){
        viewModelScope.launch(Dispatchers.IO) {
            spRepository.setAlarmReschedulePreferences(boolean)
        }
        _alarmReschedulePreferences.value = boolean
    }
}