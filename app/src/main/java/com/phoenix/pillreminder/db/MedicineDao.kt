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

    @Query("SELECT * "+
            "FROM medicines_data_table " +
            "WHERE medicine_alarm_hour = :alarmHour")
    fun getCurrentAlarmData(alarmHour: Long): Medicine?
}