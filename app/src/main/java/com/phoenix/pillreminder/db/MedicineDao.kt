package com.phoenix.pillreminder.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MedicineDao {

    @Insert
    suspend fun insertMedicines(medicines: List<Medicine>)

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("SELECT * FROM medicines_data_table")
    fun getAllMedicines():LiveData<List<Medicine>>

    @Query("SELECT * FROM medicines_data_table")
    fun getMedicines(): List<Medicine>

    @Query("SELECT * "+
            "FROM medicines_data_table " +
            "WHERE medicine_alarm_in_millis = :alarmInMillis")
    fun getCurrentAlarmData(alarmInMillis: Long): Medicine?

    @Query("SELECT *" +
            "FROM medicines_data_table " +
            "WHERE medicine_alarm_in_millis > :currentTimeMillis " +
            "AND medicine_name == :medicineName " +
            "ORDER BY medicine_alarm_in_millis " +
            "ASC LIMIT 1")
    suspend fun getNextAlarmData(medicineName: String, currentTimeMillis: Long): Medicine?

    @Query("SELECT *" +
            " FROM medicines_data_table" +
            " WHERE medicine_alarm_in_millis > :currentTimeMillis" +
            " AND medicine_was_taken = 0" +
            " GROUP BY medicine_name" +
            " ORDER BY ABS(medicine_alarm_in_millis - :currentTimeMillis)")
    fun getAlarmsToRescheduleAfterReboot(currentTimeMillis: Long): List<Medicine>

    @Query("SELECT * " +
            "FROM medicines_data_table " +
            "WHERE medicine_name = :medicineName " +
            "AND medicine_treatment_period_set = 0 " +
            "AND medicine_needs_reschedule = 1 " +
            "ORDER BY medicine_alarm_in_millis " +
            "DESC LIMIT :alarmsPerDay ")
    fun getAlarmsToRescheduleEveryMonth(medicineName: String, alarmsPerDay: Int): List<Medicine>

    @Query("SELECT * " +
            "FROM medicines_data_table " +
            "WHERE medicine_id IN " +
            "(SELECT MIN (medicine_id) FROM medicines_data_table GROUP BY medicine_name)")
    fun getAllDistinctMedicines(): List<Medicine>

}