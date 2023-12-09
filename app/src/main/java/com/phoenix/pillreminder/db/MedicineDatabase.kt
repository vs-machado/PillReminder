package com.phoenix.pillreminder.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Medicine::class], version = 1, exportSchema = false)
abstract class MedicineDatabase : RoomDatabase() {

    abstract fun medicineDao():MedicineDao

    companion object{
        @Volatile
        private var INSTANCE : MedicineDatabase? = null
        fun getInstance(context: Context): MedicineDatabase{
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