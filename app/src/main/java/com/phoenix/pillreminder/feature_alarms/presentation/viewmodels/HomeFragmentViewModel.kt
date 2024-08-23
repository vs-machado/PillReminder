package com.phoenix.pillreminder.feature_alarms.presentation.viewmodels

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.domain.repository.SharedPreferencesRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    private val workManager: WorkManager,
    @ApplicationContext private val appContext: Context
): ViewModel() {

    private var date: Date = Calendar.getInstance().time

    fun setDate(selectedDate: Date){
        date = selectedDate
    }

    fun getDate(): Date{
        return date
    }

    fun schedulePillboxReminder(hours: Int, minutes: Int){
        alarmScheduler.schedulePillboxReminder(hours, minutes)
    }

    fun markMedicineUsage(medicine: Medicine) = viewModelScope.launch{
        medicine.medicineWasTaken = true

        withContext(Dispatchers.IO){
            repository.updateMedicine(medicine)
        }
    }

    fun markMedicinesAsSkipped(medicine: Medicine){
        val alarmTime = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

        val alarmItem = AlarmItem(
            alarmTime,
            medicine.name,
            medicine.form,
            medicine.quantity.toString(),
            medicine.alarmHour.toString(),
            medicine.alarmMinute.toString()
        )

        medicine.wasSkipped = true

        viewModelScope.launch(Dispatchers.IO){
            repository.updateMedicine(medicine)

            withContext(Dispatchers.Main){
                /*Checks if the alarm was already triggered. If so, there is no need to cancel the broadcast.
                cancelAlarm() will cancel the alarm and check if there is another alarm to be scheduled*/
                if(medicine.alarmInMillis > System.currentTimeMillis()){
                    alarmScheduler.cancelAlarm(alarmItem, false)
                }
            }

            val hasNextAlarm = repository.hasNextAlarmData(medicine.name, System.currentTimeMillis())

            withContext(Dispatchers.Main){
                if(!hasNextAlarm){
                    val workRequestID = UUID.fromString(repository.getWorkerID(medicine.name))
                    WorkManager.getInstance(appContext).cancelWorkById(workRequestID)
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

    fun isWorkerActive(): Boolean{
        val workInfoList = workManager.getWorkInfosForUniqueWork("PillboxReminder").get()

        for(workInfo in workInfoList){
            if(workInfo.state == WorkInfo.State.ENQUEUED) {
                return true
            }
        }
        return false
    }

    fun deleteAllMedicinesWithSameName(name: String) =  viewModelScope.launch(Dispatchers.IO){
            val alarmsToDelete = repository.getAllMedicinesWithSameName(name)
            repository.deleteAllSelectedMedicines(alarmsToDelete)
    }
//14:10

    fun cancelReminderNotifications(context: Context){
        WorkManager.getInstance(context).cancelUniqueWork("PillboxReminder")
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(999)
    }

    fun cancelAlarm(medicine: Medicine, cancelAll: Boolean){
        val alarmTime = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

        val alarmItem = AlarmItem(
            alarmTime,
            medicine.name,
            medicine.form,
            medicine.quantity.toString(),
            medicine.alarmHour.toString(),
            medicine.alarmMinute.toString()
        )

        viewModelScope.launch(Dispatchers.Default) {
            alarmScheduler.cancelAlarm(alarmItem, cancelAll)
        }
    }

    fun setPermissionRequestPreferences(boolean: Boolean){
        spRepository.setPermissionRequestPreferences(boolean)
    }

    fun setPillboxPreferences(boolean: Boolean){
        spRepository.setPillboxPreferences(boolean)
    }

    fun getPermissionRequestPreferences(): Boolean{
        return spRepository.getPermissionRequestPreferences()
    }

    fun getPillboxPreferences(): Boolean{
        return spRepository.getPillboxPreferences()
    }

}