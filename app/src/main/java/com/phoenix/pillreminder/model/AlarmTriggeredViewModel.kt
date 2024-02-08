package com.phoenix.pillreminder.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phoenix.pillreminder.db.Medicine
import com.phoenix.pillreminder.db.MedicineDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmTriggeredViewModel(private val medicine: MedicineDao): ViewModel() {
    fun getCurrentAlarmData(alarmHourMillis: Long, callback: (Medicine?) -> Unit){
        viewModelScope.launch(Dispatchers.IO){
            val medicineAlarmData = medicine.getCurrentAlarmData(alarmHourMillis)
            callback(medicineAlarmData)
        }
    }

    fun markCurrentMedicineAsTaken(){

    }
}