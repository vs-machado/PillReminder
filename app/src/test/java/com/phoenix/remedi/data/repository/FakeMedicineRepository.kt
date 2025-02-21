//package com.phoenix.pillreminder.data.repository
//
//import androidx.lifecycle.LiveData
//import androidx.lifecycle.MediatorLiveData
//import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
//import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
//
//class FakeMedicineRepository(
//    var medicinesList: MutableList<Medicine>
//): MedicineRepository {
//
//    override suspend fun insertMedicines(medicines: List<Medicine>) {
//        medicinesList = medicines.toMutableList()
//    }
//
//    override suspend fun getAlarmsAfterProvidedMillis(
//        medicineName: String,
//        millis: Long
//    ): List<Medicine> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getAlarmTimeSinceMidnight(medicineName: String): Long {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getFirstMedicineOfNextDay(nextDayInMillis: Long): Medicine? {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getFirstMedicineOfTheDay(millis: Long): Medicine? {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun updateMedicine(medicine: Medicine) {
//        val index = medicinesList.indexOfFirst { it.id == medicine.id }
//
//        if(index != -1){
//            medicinesList[index] = medicine
//        } else {
//            throw IllegalArgumentException("Medicine not found in the repository")
//        }
//    }
//
//    override suspend fun deleteMedicine(medicine: Medicine) {
//        medicinesList.removeIf{ it.id == medicine.id}
//    }
//
//    override suspend fun deleteAllSelectedMedicines(medicines: List<Medicine>) {
//        medicinesList.removeAll(medicines)
//    }
//
//    override fun getAllMedicines(): LiveData<List<Medicine>> {
//        val liveDataList = MediatorLiveData<List<Medicine>>()
//        liveDataList.value = medicinesList
//        return liveDataList
//    }
//
//    override fun getAllMedicinesWithSameName(medicineName: String): List<Medicine> {
//        return medicinesList.filter {it.name == medicineName}
//    }
//
//    override fun getMedicines(): List<Medicine> {
//        return medicinesList
//    }
//
//    override fun getWorkerID(medicineName: String): String {
//        for (medicine in medicinesList){
//            if(medicine.name == medicineName){
//                return medicine.rescheduleWorkerID
//            }
//        }
//        return ""
//    }
//
//    override fun getCurrentAlarmData(alarmInMillis: Long): Medicine? {
//        for(medicine in medicinesList){
//            if(medicine.alarmInMillis == alarmInMillis){
//                return medicine
//            }
//        }
//        return null
//    }
//
//    override suspend fun getNextAlarmData(
//        medicineName: String,
//        currentTimeMillis: Long
//    ): Medicine? {
//        return medicinesList
//            .filter{ it.name == medicineName && it.alarmInMillis > currentTimeMillis}
//            .minByOrNull { it.alarmInMillis }
//    }
//
//    override suspend fun hasNextAlarmData(medicineName: String, currentTimeMillis: Long): Boolean {
//        val hasNextAlarmQueryResult = medicinesList.filter{it.name == medicineName && it.alarmInMillis > currentTimeMillis}
//        if(hasNextAlarmQueryResult.isNotEmpty()){
//            return true
//        }
//        return false
//    }
//
//    override fun getAlarmsToRescheduleAfterReboot(currentTimeMillis: Long): List<Medicine> {
//        return medicinesList
//            .filter { it.alarmInMillis > currentTimeMillis && !it.medicineWasTaken }
//            .groupBy { it.name }
//            .flatMap { (_, medicines) ->
//                medicines.sortedBy { kotlin.math.abs(it.alarmInMillis - currentTimeMillis) }
//            }
//    }
//
//    override fun getAlarmsToRescheduleEveryMonth(
//        medicineName: String,
//        alarmsPerDay: Int
//    ): List<Medicine> {
//        return medicinesList.filter {
//            it.name == medicineName && !it.medicinePeriodSet && it.medicineNeedsReschedule
//        }
//            .sortedByDescending { it.alarmInMillis }
//            .take(alarmsPerDay)
//    }
//
////     fun getAllDistinctMedicines(): List<Medicine> {
////        return medicinesList
////            .groupBy { it.name }
////            .mapValues { (_, medicines) -> medicines.first() }
////            .values.toList()
////    }
//
//    override suspend fun updateMedicinesActiveStatus(
//        medicineName: String,
//        currentTimeMillis: Long,
//        isActive: Boolean
//    ) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getAlarmTimesForMedicine(
//        medicineName: String,
//        cutoffTime: Long,
//        treatmentID: String
//    ): List<String> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun deleteUpcomingAlarms(medicineName: String, currentTimeMillis: Long) {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getDailyAlarms(medicineName: String, alarmsPerDay: Int): List<Long> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun getMedicineEditTimestamp(medicineName: String): Long {
//        TODO("Not yet implemented")
//    }
//
//    override fun getLastAlarmFromAllDistinctMedicines(): List<Medicine> {
//        TODO("Not yet implemented")
//    }
//}
//
