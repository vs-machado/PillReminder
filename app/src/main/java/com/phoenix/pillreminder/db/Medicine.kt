package com.phoenix.pillreminder.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines_data_table")
data class Medicine(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "medicine_id")
    var id:Int,
    @ColumnInfo(name = "medicine_name")
    var name:String,
    @ColumnInfo(name = "medicine_quantity")
    var quantity:Float,
    @ColumnInfo(name = "medicine_form")
    var form:String,
    @ColumnInfo(name = "medicine_alarm_hour")
    var alarmHour :Int,
    @ColumnInfo(name = "medicine_alarm_minute")
    var alarmMinute:Int
)
