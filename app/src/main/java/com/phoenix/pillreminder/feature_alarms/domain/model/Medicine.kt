package com.phoenix.pillreminder.feature_alarms.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "medicines_data_table")
data class Medicine(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "medicine_id")
    var id:Int,
    @ColumnInfo(name = "name")
    var name:String,
    @ColumnInfo(name = "quantity")
    var quantity:Float,
    @ColumnInfo(name = "dose_unit")
    var unit: String,
    @ColumnInfo(name = "form")
    var form:String,
    @ColumnInfo(name = "alarms_per_day")
    var alarmsPerDay: Int,
    @ColumnInfo(name = "alarm_in_millis")
    var alarmInMillis: Long,
    @ColumnInfo(name = "alarm_hour")
    var alarmHour :Int,
    @ColumnInfo(name = "alarm_minute")
    var alarmMinute:Int,
    @ColumnInfo(name = "selected_days_of_week")
    var selectedDaysOfWeek: MutableSet<Int>?,
    @ColumnInfo(name = "start_date")
    var startDate: Long,
    @ColumnInfo(name = "end_date")
    var endDate: Long,
    @ColumnInfo(name = "was_taken")
    var medicineWasTaken: Boolean,
    @ColumnInfo(name = "was_skipped")
    var wasSkipped: Boolean,
    @ColumnInfo(name = "frequency")
    var medicineFrequency: String,
    @ColumnInfo(name = "interval")
    var interval: Long,
    @ColumnInfo(name = "treatment_period_set")
    var medicinePeriodSet: Boolean,
    @ColumnInfo(name = "needs_reschedule")
    var medicineNeedsReschedule: Boolean,
    @ColumnInfo(name = "reschedule_worker_id")
    var rescheduleWorkerID: String,
    @ColumnInfo(name = "last_edited")
    var lastEdited: Long = 0L,
    @ColumnInfo(name = "is_active")
    var isActive: Boolean = true,
    @ColumnInfo(name = "treatment_id")
    var treatmentID: String

) : Parcelable

