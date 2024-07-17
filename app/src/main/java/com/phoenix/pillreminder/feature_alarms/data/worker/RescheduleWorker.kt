package com.phoenix.pillreminder.feature_alarms.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phoenix.pillreminder.feature_alarms.domain.model.AlarmItem
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.pillreminder.feature_alarms.presentation.AndroidAlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

@HiltWorker
class RescheduleWorker @AssistedInject constructor(
    private val repository: MedicineRepository,
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        val distinctMedicines = repository.getAllDistinctMedicines()
        val alarmScheduler = AndroidAlarmScheduler(repository, applicationContext)

        val alarmsToReschedule = distinctMedicines.flatMap{ medicine ->
            repository.getAlarmsToRescheduleEveryMonth(medicine.name, medicine.alarmsPerDay)
        }

        alarmsToReschedule.forEach { medicine ->
            val interval = medicine.medicineFrequency.toLong()
            val treatmentPeriodInDays = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)

            withContext(Dispatchers.IO){
                // Reinserts medicines alarms into the database for another month
                repository.insertMedicines(allAlarmsOfTreatment(medicine, interval, treatmentPeriodInDays))
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
                        false,
                        frequency,
                        interval,
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