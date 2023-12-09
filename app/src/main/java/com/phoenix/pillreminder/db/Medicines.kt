package com.phoenix.pillreminder.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines_data_table")
data class Medicines(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "medicine_id")
    var id:Int,
    @ColumnInfo(name = "medicine_name")
    var name:String,
    @ColumnInfo(name = "medicine_strength")
    var strength:String,
    @ColumnInfo(name = "medicine_quantity")
    var quantity:Int
)
