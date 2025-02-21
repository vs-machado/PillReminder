package com.phoenix.remedi.feature_alarms.data.data_source

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.phoenix.remedi.feature_alarms.domain.model.Medicine

@Database(entities = [Medicine::class], version = 15, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MedicineDatabase : RoomDatabase() {

    abstract fun medicineDao(): MedicineDao

    companion object{
        @Volatile
        private var INSTANCE : MedicineDatabase? = null
        fun getInstance(context: Context): MedicineDatabase {
            synchronized(this){
                var instance = INSTANCE
                if(instance == null){
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        MedicineDatabase::class.java,
                        "medicine_data_database"
                    ).build()
                }
                return instance
            }
        }
    }
}