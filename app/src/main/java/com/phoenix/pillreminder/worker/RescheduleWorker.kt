package com.phoenix.pillreminder.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phoenix.pillreminder.alarmscheduler.AlarmItem
import com.phoenix.pillreminder.alarmscheduler.AndroidAlarmScheduler
import com.phoenix.pillreminder.db.Medicine
import com.phoenix.pillreminder.db.MedicineDao
import com.phoenix.pillreminder.db.MedicineDatabase
import com.phoenix.pillreminder.model.MedicinesViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

class RescheduleWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext,
    workerParams
) {
    private lateinit var factory: MedicinesViewModelFactory
    private lateinit var dao: MedicineDao

    override suspend fun doWork(): Result {
        dao = MedicineDatabase.getInstance(applicationContext).medicineDao()
        factory = MedicinesViewModelFactory(dao)


        val distinctMedicines = dao.getAllDistinctMedicines()
        val alarmScheduler = AndroidAlarmScheduler(applicationContext)

        val alarmsToReschedule = distinctMedicines.flatMap{ medicine ->
            dao.getAlarmsToRescheduleEveryMonth(medicine.name, medicine.alarmsPerDay)
        }

        alarmsToReschedule.forEach { medicine ->
            val interval = medicine.medicineFrequency.toLong()
            val treatmentPeriodInDays = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)

            withContext(Dispatchers.IO){
                // Reinserts medicines alarms into the database for another month
                dao.insertMedicines(allAlarmsOfTreatment(medicine, interval, treatmentPeriodInDays))
            }

            //Keeps track of alarms that have already been scheduled (only sets 1 alarm per medicine)
            val processedMedicineNames = HashSet<String>()

            if(!processedMedicineNames.contains(medicine.name)){
                val alarmItem = AlarmItem(
                    time = Instant.ofEpochMilli(medicine.alarmInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    medicineName = medicine.name,
                    medicineForm = medicine.form,
                    medicineQuantity = medicine.quantity.toString(),
                    alarmHour = medicine.alarmHour.toString(),
                    alarmMinute = medicine.alarmMinute.toString()
                )

                alarmItem.let(alarmScheduler::scheduleAlarm)
                processedMedicineNames.add(medicine.name)
            }
        }

        return Result.success()
    }

    private fun allAlarmsOfTreatment(medicine: Medicine, interval: Long, treatmentPeriodInDays: Int): List<Medicine> {
        val alarms = mutableListOf<Medicine>()

        for(day in interval.toInt()..treatmentPeriodInDays step interval.toInt()){
            val name = medicine.name
            val quantity = medicine.quantity
            val form = medicine.form
            val alarmsPerDay = medicine.alarmsPerDay
            val alarmInMillis = (medicine.alarmInMillis + (86400000L * day))
            val alarmHour = medicine.alarmHour
            val alarmMinute = medicine.alarmMinute
            val startDate = medicine.startDate
            val endDate = medicine.endDate
            val medicineWasTaken = false
            val frequency = medicine.medicineFrequency
            val periodSet = medicine.medicinePeriodSet
            val needsReschedule = medicine.medicineNeedsReschedule
            val workerID = medicine.rescheduleWorkerID

            if((alarmInMillis + (86400000L * day)) > System.currentTimeMillis()){
                alarms.add(
                    Medicine(
                        0,
                        name,
                        quantity,
                        form,
                        alarmsPerDay,
                        alarmInMillis,
                        alarmHour,
                        alarmMinute,
                        startDate,
                        endDate,
                        medicineWasTaken,
                        frequency,
                        periodSet,
                        needsReschedule,
                        workerID
                    )
                )
            }
        }

        return alarms
    }
}