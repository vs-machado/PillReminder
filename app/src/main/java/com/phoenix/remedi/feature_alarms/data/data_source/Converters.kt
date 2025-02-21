package com.phoenix.remedi.feature_alarms.data.data_source

import androidx.room.TypeConverter

// Used to convert specific days selected by user to trigger alarms and store it in the database.
class Converters {

    @TypeConverter
    fun fromMutableSet(value: MutableSet<Int>?): String?{
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toMutableSet(value: String?): MutableSet<Int>?{
        return value?.split(",")?.mapTo(mutableSetOf()){it.toInt()}
    }
}