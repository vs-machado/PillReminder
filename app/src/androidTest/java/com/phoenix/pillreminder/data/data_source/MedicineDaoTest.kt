package com.phoenix.pillreminder.data.data_source

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDao
import com.phoenix.pillreminder.feature_alarms.data.data_source.MedicineDatabase
import com.phoenix.pillreminder.feature_alarms.domain.model.Medicine
import com.phoenix.pillreminder.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class MedicineDaoTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: MedicineDatabase
    private lateinit var dao: MedicineDao

    private val medicinesList = listOf(Medicine(
        id = 1,
        name = "a",
        quantity = 3f,
        unit = "pill",
        form = "pill",
        alarmsPerDay = 1,
        alarmInMillis = 123456789L,
        alarmHour = 21,
        alarmMinute = 55,
        selectedDaysOfWeek = null,
        startDate = 123456789L,
        endDate = 987654321L,
        medicineWasTaken = false,
        wasSkipped = false,
        medicineFrequency = "Every day",
        interval = 1L,
        medicinePeriodSet = true,
        medicineNeedsReschedule = false,
        rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
    ),
        Medicine(
            id = 2,
            name ="a",
            quantity = 4f,
            unit = "pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456785L,
            alarmHour = 20,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 123456788L,
            endDate = 887654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        ),
        Medicine(
            id = 3,
            name ="a",
            quantity = 2f,
            unit = "application",
            form = "pomade",
            alarmsPerDay = 1,
            alarmInMillis = 123436785L,
            alarmHour = 15,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 113456788L,
            endDate = 837654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = "",))

    @Before
    fun setup(){
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MedicineDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.medicineDao()
    }

    @After
    fun teardown(){
        database.close()
    }
    @Test
    fun insertMedicines() = runTest{
        val medicine = Medicine(
            id = 1,
            name = "a",
            quantity = 3f,
            unit = "pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456789L,
            alarmHour = 21,
            alarmMinute = 55,
            selectedDaysOfWeek = null,
            startDate = 123456789L,
            endDate = 987654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicinesList = listOf(medicine)
        val emptyDb = dao.getMedicines()
        assertThat(emptyDb).isEmpty()

        dao.insertMedicines(medicinesList)
        val medicinesInDatabase = dao.getMedicines()
        assertThat(medicinesInDatabase).contains(medicine)
    }

    @Test
    fun updateMedicine() = runTest{
        val medicine = Medicine(
            id = 1,
            name = "a",
            quantity = 3f,
            unit = "pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456789L,
            alarmHour = 21,
            alarmMinute = 55,
            selectedDaysOfWeek = null,
            startDate = 123456789L,
            endDate = 987654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicinesList = listOf(medicine)

        dao.insertMedicines(medicinesList)

        val updatedMedicine = medicine.copy(
            id = 1,
            name = "a",
            quantity = 3f,
            unit = "pomade",
            form = "application",
            alarmsPerDay = 1,
            alarmInMillis = 123456789L,
            alarmHour = 22,
            alarmMinute = 55,
            selectedDaysOfWeek = null,
            startDate = 123456789L,
            endDate = 987654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        dao.updateMedicine(updatedMedicine)

        val updatedMedicineFromDb = dao.getMedicineById(1)
        assertThat(updatedMedicine).isEqualTo(updatedMedicineFromDb)
    }

    @Test
    fun deleteMedicine() = runTest {
        val medicine = Medicine(
            id = 1,
            name ="a",
            quantity = 3f,
            unit = "pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456789L,
            alarmHour = 21,
            alarmMinute = 55,
            selectedDaysOfWeek = null,
            startDate = 123456789L,
            endDate = 987654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicinesList = listOf(medicine)
        dao.insertMedicines(medicinesList)

        val medicinesAfterInsert = dao.getMedicines()
        assertThat(medicinesAfterInsert).isNotEmpty()

        dao.deleteMedicine(medicine)
        val medicinesAfterRemoval = dao.getMedicines()
        assertThat(medicinesAfterRemoval).isEmpty()
    }

    @Test
    fun deleteAllSelectedMedicines() = runTest {
        dao.insertMedicines(medicinesList)

        val databaseAfterInsertList = dao.getMedicines()
        assertThat(databaseAfterInsertList).isNotEmpty()

        val medicineItem = listOf(Medicine(
            id = 4,
            name = "a",
            quantity = 2f,
            unit = "application",
            form = "pomade",
            alarmsPerDay = 1,
            alarmInMillis = 123436785L,
            alarmHour = 15,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 113456788L,
            endDate = 837654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        ))

        dao.insertMedicines(medicineItem)

        val databaseAfterInsertItem = dao.getMedicines()
        assertThat(databaseAfterInsertItem).containsExactlyElementsIn(medicinesList + medicineItem)

        dao.deleteAllSelectedMedicines(medicinesList)
        val databaseAfterListRemoval = dao.getMedicines()
        assertThat(databaseAfterListRemoval).isNotEmpty()
    }

    @Test
    fun getAllMedicines() = runTest{
        dao.insertMedicines(medicinesList)

        val dbLiveData = dao.getAllMedicines().getOrAwaitValue()
        assertThat(dbLiveData).containsExactlyElementsIn(medicinesList)
    }

    @Test
    fun getAllMedicinesWithSameName() = runTest{
        dao.insertMedicines(medicinesList)

        val medicinesWithAnotherNames = listOf(
            Medicine(
                id = 4,
                name ="hsdaui",
                quantity = 3f,
                unit = "pill",
                form = "pill",
                alarmsPerDay = 1,
                alarmInMillis = 123456789L,
                alarmHour = 21,
                alarmMinute = 55,
                selectedDaysOfWeek = null,
                startDate = 123456789L,
                endDate = 987654321L,
                medicineWasTaken = false,
                wasSkipped = false,
                medicineFrequency = "Every day",
                interval = 1L,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                rescheduleWorkerID = "a", lastEdited = 0L, isActive = true, treatmentID = ""
            ),
            Medicine(
                id = 5,
                name ="rgsejuiho8",
                quantity = 3f,
                unit ="pill",
                form = "pill",
                alarmsPerDay = 1,
                alarmInMillis = 123456789L,
                alarmHour = 21,
                alarmMinute = 55,
                selectedDaysOfWeek = null,
                startDate = 123456789L,
                endDate = 987654321L,
                medicineWasTaken = false,
                wasSkipped = false,
                medicineFrequency = "Every day",
                interval = 1L,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                rescheduleWorkerID = "b", lastEdited = 0L, isActive = true, treatmentID = ""
            ),
            Medicine(
                id = 6,
                name = "gorjei",
                quantity = 3f,
                unit ="pill",
                form = "pill",
                alarmsPerDay = 1,
                alarmInMillis = 123456789L,
                alarmHour = 21,
                alarmMinute = 55,
                selectedDaysOfWeek = null,
                startDate = 123456789L,
                endDate = 987654321L,
                medicineWasTaken = false,
                wasSkipped = false,
                medicineFrequency = "Every day",
                interval = 1L,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                rescheduleWorkerID = "c", lastEdited = 0L, isActive = true, treatmentID = ""
            ))

        dao.insertMedicines(medicinesWithAnotherNames)
        val medicinesWithSameName = dao.getAllMedicinesWithSameName("a")

        assertThat(medicinesWithSameName).containsExactlyElementsIn(medicinesList)
    }

    @Test
    fun getMedicines() = runTest{
        dao.insertMedicines(medicinesList)
        val dbMedicineList = dao.getMedicines()
        assertThat(dbMedicineList).isEqualTo(medicinesList)
    }

    @Test
    fun getWorkerID() = runTest {
        dao.insertMedicines(medicinesList)

        val medicinesWithDifferentWorkerID = listOf(
            Medicine(
                id = 4,
                name = "hsdaui",
                quantity = 3f,
                unit ="pill",
                form = "pill",
                alarmsPerDay = 1,
                alarmInMillis = 123456789L,
                alarmHour = 21,
                alarmMinute = 55,
                selectedDaysOfWeek = null,
                startDate = 123456789L,
                endDate = 987654321L,
                medicineWasTaken = false,
                wasSkipped = false,
                medicineFrequency = "Every day",
                interval = 1L,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                rescheduleWorkerID = "a", lastEdited = 0L, isActive = true, treatmentID = ""
            ),
            Medicine(
                id = 5,
                name = "rgsejuiho8",
                quantity = 3f,
                unit = "pill",
                form = "pill",
                alarmsPerDay = 1,
                alarmInMillis = 123456789L,
                alarmHour = 21,
                alarmMinute = 55,
                selectedDaysOfWeek = null,
                startDate = 123456789L,
                endDate = 987654321L,
                medicineWasTaken = false,
                wasSkipped = false,
                medicineFrequency = "Every day",
                interval = 1L,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                rescheduleWorkerID = "b", lastEdited = 0L, isActive = true, treatmentID = ""
            ),
            Medicine(
                id = 6,
                name ="gorjei",
                quantity = 3f,
                unit = "pill",
                form = "pill",
                alarmsPerDay = 1,
                alarmInMillis = 123456789L,
                alarmHour = 21,
                alarmMinute = 55,
                selectedDaysOfWeek = null,
                startDate = 123456789L,
                endDate = 987654321L,
                medicineWasTaken = false,
                wasSkipped = false,
                medicineFrequency = "Every day",
                interval = 1L,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                rescheduleWorkerID = "c", lastEdited = 0L, isActive = true, treatmentID = ""
            ))
        dao.insertMedicines(medicinesWithDifferentWorkerID)

        val workerID = dao.getWorkerID("rgsejuiho8", "")
        assertThat(workerID).isEqualTo("b")
    }

    @Test
    fun getCurrentAlarmData() = runTest{
        val medicine1 = Medicine(
            id = 1,
            name ="a",
            quantity = 3f,
            unit ="pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456789L,
            alarmHour = 21,
            alarmMinute = 55,
            selectedDaysOfWeek = null,
            startDate = 123456789L,
            endDate = 987654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicine2 = Medicine(
            id = 2,
            name ="a",
            quantity = 4f,
            unit = "pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456785L,
            alarmHour = 20,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 123456788L,
            endDate = 887654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        dao.insertMedicines(listOf(medicine1, medicine2))

        val alarmInMillis = 123456785L
        val queryMedicine2 = dao.getCurrentAlarmData(alarmInMillis)

        assertThat(queryMedicine2).isEqualTo(medicine2)
    }

    @Test
    fun getNextAlarmData() = runTest{
        val medicine1 = Medicine(
            id = 1,
            name ="a",
            quantity = 3f,
            unit = "pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456789L,
            alarmHour = 21,
            alarmMinute = 55,
            selectedDaysOfWeek = null,
            startDate = 123456789L,
            endDate = 987654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicine2 = Medicine(
            id = 2,
            name ="a",
            quantity = 4f,
            unit = "pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456791L,
            alarmHour = 20,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 123456788L,
            endDate = 887654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )
        val medicine3 = Medicine(
            id = 3,
            name ="a",
            quantity = 4f,
            unit = "pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456790L,
            alarmHour = 20,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 123456788L,
            endDate = 887654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicineList = listOf(medicine1, medicine2, medicine3)

        dao.insertMedicines(medicineList)

        val nextMedicineAlarmData = dao.getNextAlarmData("a", 123456789L)

        assertThat(nextMedicineAlarmData).isEqualTo(medicine3)
    }

    @Test
    fun hasNextAlarmData() = runTest {
        val medicine1 = Medicine(
            id = 1,
            name ="a",
            quantity = 3f,
            unit = "pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456789L,
            alarmHour = 21,
            alarmMinute = 55,
            selectedDaysOfWeek = null,
            startDate =  123456789L,
            endDate = 987654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicine2 = Medicine(
            id = 2,
            name ="a",
            quantity = 4f,
            unit ="pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456791L,
            alarmHour = 20,
            alarmMinute = 33,
            selectedDaysOfWeek =  null,
            startDate = 123456788L,
            endDate = 887654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        dao.insertMedicines(listOf(medicine1, medicine2))

        val hasNextAlarm = dao.hasNextAlarmData("a", 123456788L)
        assertThat(hasNextAlarm).isEqualTo(true)

        dao.deleteMedicine(medicine1)
        val hasNextAlarmAfterRemoval = dao.hasNextAlarmData("a", 123456788L)
        assertThat(hasNextAlarmAfterRemoval).isEqualTo(false)
    }

    @Test
    fun getAlarmsToRescheduleAfterReboot() = runTest{
        val medicine1 = Medicine(
            id = 2,
            name ="a",
            quantity = 4f,
            unit = "pill",
            form = "pill",
            alarmsPerDay = 5,
            alarmInMillis = 123456785L,
            alarmHour = 20,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 123456788L,
            endDate = 887654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = false,
            medicineNeedsReschedule = true,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicine2 = Medicine(
            id = 3,
            name ="b",
            quantity = 2f,
            unit = "application",
            form = "pomade",
            alarmsPerDay = 1,
            alarmInMillis = 123436785L,
            alarmHour = 15,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 113456788L,
            endDate = 837654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = false,
            medicineNeedsReschedule = true,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicineList = listOf(medicine1, medicine2)
        val medicine1List = listOf(medicine1)

        dao.insertMedicines(medicineList)

        val medicinesInserted = dao.getMedicines()
        assertThat(medicinesInserted).hasSize(2)

        val alarmsToRescheduleEveryMonth =
            dao.getMedicineById(2)
                ?.let { dao.getAlarmsToRescheduleEveryMonth(it.name, it.alarmsPerDay) }
        assertThat(alarmsToRescheduleEveryMonth).hasSize(1)
        assertThat(alarmsToRescheduleEveryMonth).containsExactlyElementsIn(medicine1List)

    }

    @Test
    fun getLastAlarmFromAllDistinctMedicines() = runTest{
        val medicine1 = Medicine(
            id = 1,
            name ="a",
            quantity = 3f,
            unit ="pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456789L,
            alarmHour = 21,
            alarmMinute = 55,
            selectedDaysOfWeek = null,
            startDate = 123456789L,
            endDate = 987654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )
        val medicine2 = Medicine(
            id = 2,
            name = "b",
            quantity = 2f,
            unit = "puff",
            form = "inhaler",
            alarmsPerDay = 1,
            alarmInMillis = 123456785L,
            alarmHour = 20,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 123456788L,
            endDate = 887654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicine3 = Medicine(
            id = 3,
            name = "c",
            quantity = 2f,
            unit = "application",
            form = "pomade",
            alarmsPerDay = 1,
            alarmInMillis = 123436785L,
            alarmHour = 15,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 113456788L,
            endDate = 837654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicine4 = Medicine(
            id = 4,
            name ="c",
            quantity = 2f,
            unit = "application",
            form = "pomade",
            alarmsPerDay = 1,
            alarmInMillis = 123439785L,
            alarmHour = 18,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 113456788L,
            endDate = 837654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicine5 = Medicine(
            id = 5,
            name ="c",
            quantity = 2f,
            unit = "application",
            form = "pomade",
            alarmsPerDay = 1,
            alarmInMillis = 123443785L,
            alarmHour = 21,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate =  113456788L,
            endDate = 837654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicineList = listOf(medicine1, medicine2, medicine3, medicine4, medicine5)
        dao.insertMedicines(medicineList)

        val allDistinctMedicines = dao.getLastAlarmFromAllDistinctMedicines()
        assertThat(allDistinctMedicines).hasSize(3)

        val medicineNameList = allDistinctMedicines.map{it.name}.toList()

        assertThat(medicineNameList).containsExactlyElementsIn(listOf("a", "b", "c"))
    }

    @Test
    fun getMedicineById() = runTest{
        val medicine1 = Medicine(
            id = 1,
            name ="a",
            quantity = 3f,
            unit = "pill",
            form = "pill",
            alarmsPerDay = 1,
            alarmInMillis = 123456789L,
            alarmHour = 21,
            alarmMinute = 55,
            selectedDaysOfWeek = null,
            startDate = 123456789L,
            endDate = 987654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )
        val medicine2 = Medicine(
            id = 2,
            name ="b",
            quantity = 2f,
            unit = "puff",
            form = "inhaler",
            alarmsPerDay = 1,
            alarmInMillis = 123456785L,
            alarmHour = 20,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 123456788L,
            endDate = 887654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicine3 = Medicine(
            id = 3,
            name = "c",
            quantity = 2f,
            unit = "application",
            form = "pomade",
            alarmsPerDay = 1,
            alarmInMillis = 123436785L,
            alarmHour = 15,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 113456788L,
            endDate = 837654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicine4 = Medicine(
            id = 4,
            name ="c",
            quantity = 2f,
            unit = "application",
            form = "pomade",
            alarmsPerDay = 1,
            alarmInMillis = 123439785L,
            alarmHour = 18,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 113456788L,
            endDate = 837654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicine5 = Medicine(
            id = 5,
            name ="c",
            quantity = 2f,
            form = "application",
            unit = "pomade",
            alarmsPerDay = 1,
            alarmInMillis = 123443785L,
            alarmHour = 21,
            alarmMinute = 33,
            selectedDaysOfWeek = null,
            startDate = 113456788L,
            endDate = 837654321L,
            medicineWasTaken = false,
            wasSkipped = false,
            medicineFrequency = "Every day",
            interval = 1L,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = ""
        )

        val medicineList = listOf(medicine1, medicine2, medicine3, medicine4, medicine5)

        dao.insertMedicines(medicineList)

        val medicineId2 = dao.getMedicineById(2)
        assertThat(medicineId2?.name).isEqualTo("b")
        assertThat(medicineId2?.form).isEqualTo("inhaler")
    }

    @Test
    fun updateMedicinesAsSkipped_marksOldUntakenMedicinesAsSkipped() = runTest {
        val currentTime = System.currentTimeMillis()
        val treatmentId = "123"
        val medicinesList1 = listOf(
            Medicine(
                id = 8,
                name ="c",
                quantity = 2f,
                form = "application",
                unit = "pomade",
                alarmsPerDay = 1,
                alarmInMillis = currentTime - 2000,
                alarmHour = 21,
                alarmMinute = 33,
                selectedDaysOfWeek = null,
                startDate = 113456788L,
                endDate = 837654321L,
                medicineWasTaken = true, // Medicine used
                wasSkipped = false,
                medicineFrequency = "Every day",
                interval = 1L,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = treatmentId
            ),
            Medicine(
                id = 9,
                name ="c",
                quantity = 2f,
                form = "application",
                unit = "pomade",
                alarmsPerDay = 1,
                alarmInMillis = currentTime - 1000,
                alarmHour = 21,
                alarmMinute = 33,
                selectedDaysOfWeek = null,
                startDate = 113456788L,
                endDate = 837654321L,
                medicineWasTaken = false,
                wasSkipped = false, // Medicine will be skipped
                medicineFrequency = "Every day",
                interval = 1L,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = treatmentId
            ),
            Medicine(
                id = 10,
                name ="c",
                quantity = 2f,
                form = "application",
                unit = "pomade",
                alarmsPerDay = 1,
                alarmInMillis = currentTime,
                alarmHour = 21,
                alarmMinute = 33,
                selectedDaysOfWeek = null,
                startDate = 113456788L,
                endDate = 837654321L,
                medicineWasTaken = false, // Medicine will be used
                wasSkipped = false,
                medicineFrequency = "Every day",
                interval = 1L,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = treatmentId
            ),
            Medicine(
                id = 11,
                name = "c",
                quantity = 2f,
                form = "application",
                unit = "pomade",
                alarmsPerDay = 1,
                alarmInMillis = currentTime + 1000,
                alarmHour = 21,
                alarmMinute = 33,
                selectedDaysOfWeek = null,
                startDate = 113456788L,
                endDate = 837654321L,
                medicineWasTaken = false, // Future alarm, don't do nothing
                wasSkipped = false,
                medicineFrequency = "Every day",
                interval = 1L,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                rescheduleWorkerID = "noID", lastEdited = 0L, isActive = true, treatmentID = treatmentId
            )
        )

        dao.insertMedicines(medicinesList1)

        // Marking medicine usage
        medicinesList1[2].medicineWasTaken = true

        // When: Updating medicines as skipped
        dao.updateMedicinesAsSkipped(treatmentId, currentTime)

        // Updating the database
        dao.updateMedicine(medicinesList1[2])

        // Then: Only past untaken medicines should be marked as skipped
        val medicine0 = dao.getMedicineById(8) // Medicine used
        val medicine1 = dao.getMedicineById(9) // Medicine skipped
        val medicine2 = dao.getMedicineById(10) // Medicine used
        val medicine3 = dao.getMedicineById(11) // Future alarm

        assertThat(medicine0?.medicineWasTaken).isTrue()
        assertThat(medicine0?.wasSkipped).isFalse()

        assertThat(medicine1?.wasSkipped).isTrue()
        assertThat(medicine1?.medicineWasTaken).isFalse()

        assertThat(medicine2?.medicineWasTaken).isTrue()
        assertThat(medicine2?.wasSkipped).isFalse()

        assertThat(medicine3?.medicineWasTaken).isFalse()
        assertThat(medicine3?.wasSkipped).isFalse()
    }
}