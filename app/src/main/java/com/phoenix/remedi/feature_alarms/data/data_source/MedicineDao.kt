package com.phoenix.remedi.feature_alarms.data.data_source

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.phoenix.remedi.feature_alarms.domain.model.AlarmTimeData
import com.phoenix.remedi.feature_alarms.domain.model.Medicine

@Dao
interface MedicineDao {

    @Insert
    suspend fun insertMedicines(medicines: List<Medicine>)

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Query("UPDATE medicines_data_table SET is_active = :isActive WHERE name = :medicineName AND alarm_in_millis <= :currentTimeMillis")
    suspend fun updateMedicinesActiveStatus(medicineName: String, currentTimeMillis: Long, isActive: Boolean)

    @Query("UPDATE medicines_data_table SET name = :name, quantity = :quantity, form = :form, end_date = :endDate, " +
            "frequency = :frequency, is_active = 0 " +
            "WHERE treatment_id = :treatmentID AND name = :name AND alarm_in_millis < :currentTime")
    suspend fun updateExpiredMedicines(treatmentID: String, name: String, quantity: Float, form: String,
                                       endDate: Long, frequency: String, currentTime: Long)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("DELETE FROM medicines_data_table WHERE name = :medicineName AND alarm_in_millis > :currentTimeMillis")
    suspend fun deleteUpcomingAlarms(medicineName: String, currentTimeMillis: Long)

    @Delete
    suspend fun deleteAllSelectedMedicines(medicines: List<Medicine>)

    @Query("SELECT selected_days_of_week FROM medicines_data_table WHERE name = :medicineName AND treatment_id = :treatmentID ORDER BY alarm_in_millis DESC LIMIT 1")
    suspend fun getSelectedDaysList(medicineName: String, treatmentID: String): String

    @Query("SELECT * FROM medicines_data_table WHERE name = :medicineName AND alarm_in_millis > :millis ORDER BY alarm_in_millis ASC")
    suspend fun getAlarmsAfterProvidedMillis(medicineName: String, millis: Long): List<Medicine>

    @Query("""SELECT (alarm_in_millis - (alarm_in_millis / 86400000) * 86400000) as time_since_midnight
            FROM medicines_data_table
            WHERE name = :medicineName""")
    suspend fun getAlarmTimeSinceMidnight(medicineName: String): Long

    @Query("SELECT * FROM medicines_data_table")
    fun getAllMedicines():LiveData<List<Medicine>>

    @Query("SELECT DISTINCT alarm_hour, alarm_minute FROM medicines_data_table WHERE " +
            "name = :medicineName AND last_edited = (" +
            "    SELECT MAX(last_edited) FROM medicines_data_table " +
            "    WHERE name = :medicineName AND treatment_id = :treatmentID" +
            ") AND is_active = 1 " +
            "ORDER BY alarm_in_millis ASC " +
            "LIMIT :alarmsPerDay")
    suspend fun getDailyAlarms(medicineName: String, alarmsPerDay: Int, treatmentID: String): List<AlarmTimeData>


    @Query("SELECT last_edited FROM medicines_data_table " +
            "WHERE name = :medicineName  ORDER BY last_edited DESC LIMIT 1")
    suspend fun getMedicineEditTimestamp(medicineName: String): Long

    @Query("SELECT *" +
            "FROM medicines_data_table " +
            "WHERE name = :medicineName")
    fun getAllMedicinesWithSameName(medicineName: String): List<Medicine>

    @Query("SELECT * FROM medicines_data_table")
    fun getMedicines(): List<Medicine>

    @Query("SELECT reschedule_worker_id FROM medicines_data_table WHERE name = :medicineName AND treatment_id = :treatmentID ORDER BY alarm_in_millis DESC LIMIT 1")
    fun getWorkerID(medicineName: String, treatmentID: String): String

    @Query("SELECT * "+
            "FROM medicines_data_table " +
            "WHERE alarm_in_millis = :alarmInMillis")
    fun getCurrentAlarmData(alarmInMillis: Long): Medicine?

    @Query("SELECT *" +
            "FROM medicines_data_table " +
            "WHERE alarm_in_millis > :currentTimeMillis " +
            "AND name = :medicineName " +
            "ORDER BY alarm_in_millis " +
            "ASC LIMIT 1")
    suspend fun getNextAlarmData(medicineName: String, currentTimeMillis: Long): Medicine?


    @Query("SELECT *" +
            "FROM medicines_data_table " +
            "WHERE alarm_in_millis >= :nextDayInMillis " +
            "ORDER BY alarm_in_millis " +
            "ASC LIMIT 1")
    suspend fun getFirstMedicineOfNextDay(nextDayInMillis: Long): Medicine?

    @Query("SELECT * " +
            "FROM medicines_data_table " +
            "WHERE alarm_in_millis >= :millis " +
            "ORDER BY alarm_in_millis " +
            "ASC LIMIT 1")
    suspend fun getFirstMedicineOfTheDay(millis: Long): Medicine?

    @Query("SELECT COUNT(*) > 1 " +
            "FROM medicines_data_table " +
            "WHERE alarm_in_millis > :currentTimeMillis " +
            "AND name = :medicineName " +
            "GROUP BY name " +
            "HAVING COUNT(*) > 1")
    suspend fun hasNextAlarmData(medicineName: String, currentTimeMillis: Long): Boolean

    @Query("SELECT *" +
            " FROM medicines_data_table" +
            " WHERE alarm_in_millis > :currentTimeMillis" +
            " AND was_taken = 0" +
            " GROUP BY name" +
            " ORDER BY ABS(alarm_in_millis - :currentTimeMillis)")
    fun getAlarmsToRescheduleAfterReboot(currentTimeMillis: Long): List<Medicine>

    @Query("SELECT * " +
            "FROM medicines_data_table " +
            "WHERE name = :medicineName " +
            "AND treatment_period_set = 0 " +
            "AND needs_reschedule = 1 " +
            "ORDER BY alarm_in_millis DESC " +
            "LIMIT :alarmsPerDay ")
    fun getAlarmsToRescheduleEveryMonth(medicineName: String, alarmsPerDay: Int): List<Medicine>

    @Query("SELECT * " +
            "FROM medicines_data_table " +
            "WHERE medicine_id IN " +
            "(SELECT MAX (medicine_id) FROM medicines_data_table " +
            "GROUP BY name, treatment_id) ")
    fun getLastAlarmFromAllDistinctMedicines(): List<Medicine>

    @Query("SELECT * " +
            "FROM medicines_data_table " +
            "WHERE medicine_id = :id")
    fun getMedicineById(id: Int): Medicine?

    @Query("""
        SELECT DISTINCT printf('%02d:%02d', alarm_hour, alarm_minute) AS alarm_time
        FROM medicines_data_table
        WHERE name = :medicineName AND last_edited >= :cutoffTime AND treatment_id = :treatmentID
        ORDER BY alarm_hour, alarm_minute
    """)
    suspend fun getAlarmTimesForMedicine(medicineName: String, cutoffTime: Long, treatmentID: String): List<String>

    @Query("SELECT * FROM medicines_data_table WHERE name = :medicineName AND treatment_id = :treatmentID ORDER BY alarm_in_millis DESC LIMIT 1")
    suspend fun getLastAlarm(medicineName: String, treatmentID: String): Medicine

    @Query("UPDATE medicines_data_table SET was_skipped = 1 WHERE treatment_id = :treatmentID AND alarm_in_millis < :alarmInMillis AND was_taken = 0")
    suspend fun updateMedicinesAsSkipped(treatmentID: String, alarmInMillis: Long)

    @Query("SELECT COUNT(*) > 1 FROM medicines_data_table WHERE alarm_in_millis = :timeInMillis")
    suspend fun checkForMultipleAlarmsAtSameTime(timeInMillis: Long): Boolean

    @Query("SELECT * FROM medicines_data_table WHERE alarm_in_millis = :timeInMillis")
    suspend fun getMedicinesScheduledForTime(timeInMillis: Long): List<Medicine>

}