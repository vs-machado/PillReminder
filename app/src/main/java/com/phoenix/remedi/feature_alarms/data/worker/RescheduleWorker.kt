package com.phoenix.remedi.feature_alarms.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.phoenix.remedi.feature_alarms.domain.model.AlarmItem
import com.phoenix.remedi.feature_alarms.domain.model.Medicine
import com.phoenix.remedi.feature_alarms.domain.repository.MedicineRepository
import com.phoenix.remedi.feature_alarms.presentation.AlarmScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar

/**
 * Worker used to reschedule alarms without a treatment period set.
 *
 * @property repository Medicine data repository for storing alarms data
 * @property alarmScheduler Responsible for scheduling and cancelling alarms
 */
@HiltWorker
class RescheduleWorker @AssistedInject constructor(
    private val repository: MedicineRepository,
    private val alarmScheduler: AlarmScheduler,
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    /**
     *  The worker gets all distinct medicines and checks which medicines require a reschedule.
     *  The medicines that require a reschedule are medicines without treatment period set.
     *  After getting the distinct medicines, the new alarms are calculated and inserted in the medicine database.
     *  Then, the worker proceeds to schedule new alarms.
     *
     *  @return if the work was completed successfully or not
     */
    override suspend fun doWork(): Result {

        val distinctMedicines = repository.getLastAlarmFromAllDistinctMedicines()

        val alarmsToReschedule = distinctMedicines.flatMap{ medicine ->
            repository.getAlarmsToRescheduleEveryMonth(medicine.name, medicine.alarmsPerDay)
        }

        alarmsToReschedule.forEach { medicine ->
            val interval = medicine.interval
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
                    doseUnit = medicine.unit,
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
        val name = medicine.name
        val quantity = medicine.quantity
        val doseUnit = medicine.unit
        val form = medicine.form
        val alarmsPerDay = medicine.alarmsPerDay
        val alarmHour = medicine.alarmHour
        val alarmMinute = medicine.alarmMinute
        val selectedDaysOfWeek = medicine.selectedDaysOfWeek
        val startDate = medicine.startDate
        val endDate = medicine.endDate
        val medicineWasTaken = false
        val frequency = medicine.medicineFrequency
        val periodSet = medicine.medicinePeriodSet
        val needsReschedule = medicine.medicineNeedsReschedule
        val workerID = medicine.rescheduleWorkerID
        val lastEdited = medicine.lastEdited
        val treatmentID = medicine.treatmentID

        val scheduleInterval = when(frequency){
            "EveryXWeeks" -> { interval * 7 }
            "EveryXMonths" -> { interval * 30 }
            else -> { interval }
        }

        for(day in interval.toInt()..treatmentPeriodInDays step scheduleInterval.toInt()){
            val alarmInMillis = (medicine.alarmInMillis + (86400000L * day))

            if((alarmInMillis + (86400000L * day)) > System.currentTimeMillis()){
                alarms.add(
                    Medicine(
                        0,
                        name,
                        quantity,
                        doseUnit,
                        form,
                        alarmsPerDay,
                        alarmInMillis,
                        alarmHour,
                        alarmMinute,
                        selectedDaysOfWeek,
                        startDate,
                        endDate,
                        medicineWasTaken,
                        false,
                        frequency,
                        scheduleInterval,
                        periodSet,
                        needsReschedule,
                        workerID,
                        lastEdited,
                        true,
                        treatmentID
                    )
                )
            }
        }

        return alarms
    }
}