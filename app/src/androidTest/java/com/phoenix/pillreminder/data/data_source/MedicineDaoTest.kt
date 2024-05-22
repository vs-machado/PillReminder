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

    val medicinesList = listOf(Medicine(1,
        "a",
        3f,
        "pill",
        1,
        123456789L,
        21,
        55,
        123456789L,
        987654321L,
        false,
        false,
        1,
        medicinePeriodSet = true,
        medicineNeedsReschedule = false,
        "noID"),
        Medicine(2,
            "a",
            4f,
            "pill",
            1,
            123456785L,
            20,
            33,
            123456788L,
            887654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID"),
        Medicine(3,
            "a",
            2f,
            "pomade",
            1,
            123436785L,
            15,
            33,
            113456788L,
            837654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID"))

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
        val medicine = Medicine(1,
            "a",
            3f,
            "pill",
            1,
            123456789L,
            21,
            55,
            123456789L,
            987654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        val medicinesList = listOf(medicine)
        val emptyDb = dao.getMedicines()
        assertThat(emptyDb).isEmpty()

        dao.insertMedicines(medicinesList)
        val medicinesInDatabase = dao.getMedicines()
        assertThat(medicinesInDatabase).contains(medicine)
    }

    @Test
    fun updateMedicine() = runTest{
        val medicine = Medicine(1,
            "a",
            3f,
            "pill",
            1,
            123456789L,
            21,
            55,
            123456789L,
            987654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        val medicinesList = listOf(medicine)

        dao.insertMedicines(medicinesList)

        val updatedMedicine = medicine.copy(1,
            "a",
            3f,
            "pomade",
            1,
            123456789L,
            22,
            55,
            123456789L,
            987654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID"
        )

        dao.updateMedicine(updatedMedicine)

        val updatedMedicineFromDb = dao.getMedicineById(1)
        assertThat(updatedMedicine).isEqualTo(updatedMedicineFromDb)
    }

    @Test
    fun deleteMedicine() = runTest {
        val medicine = Medicine(1,
            "a",
            3f,
            "pill",
            1,
            123456789L,
            21,
            55,
            123456789L,
            987654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

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

        val medicineItem = listOf(Medicine(4,
            "a",
            2f,
            "pomade",
            1,
            123436785L,
            15,
            33,
            113456788L,
            837654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID"))

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
            Medicine(4,
            "hsdaui",
            3f,
            "pill",
            1,
            123456789L,
            21,
            55,
            123456789L,
            987654321L,
            false,
                false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "a"),
            Medicine(5,
                "rgsejuiho8",
                3f,
                "pill",
                1,
                123456789L,
                21,
                55,
                123456789L,
                987654321L,
                false,
                false,
                1,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                "b"),
            Medicine(6,
                "gorjei",
                3f,
                "pill",
                1,
                123456789L,
                21,
                55,
                123456789L,
                987654321L,
                false,
                false,
                1,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                "c"))

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
            Medicine(4,
                "hsdaui",
                3f,
                "pill",
                1,
                123456789L,
                21,
                55,
                123456789L,
                987654321L,
                false,
                false,
                1,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                "a"),
            Medicine(5,
                "rgsejuiho8",
                3f,
                "pill",
                1,
                123456789L,
                21,
                55,
                123456789L,
                987654321L,
                false,
                false,
                1,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                "b"),
            Medicine(6,
                "gorjei",
                3f,
                "pill",
                1,
                123456789L,
                21,
                55,
                123456789L,
                987654321L,
                false,
                false,
                1,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                "c"))
        dao.insertMedicines(medicinesWithDifferentWorkerID)

        val workerID = dao.getWorkerID("a")
        assertThat(workerID).isEqualTo("noID")
    }

    @Test
    fun getCurrentAlarmData() = runTest{
        val medicine1 = Medicine(1,
            "a",
            3f,
            "pill",
            1,
            123456789L,
            21,
            55,
            123456789L,
            987654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        val medicine2 = Medicine(2,
            "a",
            4f,
            "pill",
            1,
            123456785L,
            20,
            33,
            123456788L,
            887654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        dao.insertMedicines(listOf(medicine1, medicine2))

        val alarmInMillis = 123456785L
        val queryMedicine2 = dao.getCurrentAlarmData(alarmInMillis)

        assertThat(queryMedicine2).isEqualTo(medicine2)
    }

    @Test
    fun getNextAlarmData() = runTest{
        val medicine1 = Medicine(1,
            "a",
            3f,
            "pill",
            1,
            123456789L,
            21,
            55,
            123456789L,
            987654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        val medicine2 = Medicine(2,
            "a",
            4f,
            "pill",
            1,
            123456791L,
            20,
            33,
            123456788L,
            887654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")
        val medicine3 = Medicine(3,
            "a",
            4f,
            "pill",
            1,
            123456790L,
            20,
            33,
            123456788L,
            887654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        val medicineList = listOf(medicine1, medicine2, medicine3)

        dao.insertMedicines(medicineList)

        val nextMedicineAlarmData = dao.getNextAlarmData("a", 123456789L)

        assertThat(nextMedicineAlarmData).isEqualTo(medicine3)
    }

    @Test
    fun hasNextAlarmData() = runTest {
        val medicine1 = Medicine(1,
            "a",
            3f,
            "pill",
            1,
            123456789L,
            21,
            55,
            123456789L,
            987654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        val medicine2 = Medicine(2,
            "a",
            4f,
            "pill",
            1,
            123456791L,
            20,
            33,
            123456788L,
            887654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        dao.insertMedicines(listOf(medicine1, medicine2))

        val hasNextAlarm = dao.hasNextAlarmData("a", 123456788L)
        assertThat(hasNextAlarm).isEqualTo(true)

        dao.deleteMedicine(medicine1)
        val hasNextAlarmAfterRemoval = dao.hasNextAlarmData("a", 123456788L)
        assertThat(hasNextAlarmAfterRemoval).isEqualTo(false)
    }

    @Test
    fun getAlarmsToRescheduleAfterReboot() = runTest{
        val medicine1 = Medicine(2,
            "a",
            4f,
            "pill",
            5,
            123456785L,
            20,
            33,
            123456788L,
            887654321L,
            false,
            false,
            1,
            medicinePeriodSet = false,
            medicineNeedsReschedule = true,
            "noID")

        val medicine2 = Medicine(3,
            "b",
            2f,
            "pomade",
            1,
            123436785L,
            15,
            33,
            113456788L,
            837654321L,
            false,
            false,
            1,
            medicinePeriodSet = false,
            medicineNeedsReschedule = true,
            "noID")

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
    fun getAllDistinctMedicines() = runTest{
        val medicine1 = Medicine(1,
            "a",
            3f,
            "pill",
            1,
            123456789L,
            21,
            55,
            123456789L,
            987654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")
        val medicine2 = Medicine(2,
                "b",
                2f,
                "inhaler",
                1,
                123456785L,
                20,
                33,
                123456788L,
                887654321L,
                false,
            false,
                1,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                "noID")

        val medicine3 = Medicine(3,
                "c",
                2f,
                "pomade",
                1,
                123436785L,
                15,
                33,
                113456788L,
                837654321L,
                false,
            false,
                1,
                medicinePeriodSet = true,
                medicineNeedsReschedule = false,
                "noID")

        val medicine4 = Medicine(4,
            "c",
            2f,
            "pomade",
            1,
            123439785L,
            18,
            33,
            113456788L,
            837654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        val medicine5 = Medicine(5,
            "c",
            2f,
            "pomade",
            1,
            123443785L,
            21,
            33,
            113456788L,
            837654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        val medicineList = listOf(medicine1, medicine2, medicine3, medicine4, medicine5)
        dao.insertMedicines(medicineList)

        val allDistinctMedicines = dao.getAllDistinctMedicines()
        assertThat(allDistinctMedicines).hasSize(3)

        val medicineNameList = allDistinctMedicines.map{it.name}.toList()

        assertThat(medicineNameList).containsExactlyElementsIn(listOf("a", "b", "c"))
    }

    @Test
    fun getMedicineById() = runTest{
        val medicine1 = Medicine(1,
            "a",
            3f,
            "pill",
            1,
            123456789L,
            21,
            55,
            123456789L,
            987654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")
        val medicine2 = Medicine(2,
            "b",
            2f,
            "inhaler",
            1,
            123456785L,
            20,
            33,
            123456788L,
            887654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        val medicine3 = Medicine(3,
            "c",
            2f,
            "pomade",
            1,
            123436785L,
            15,
            33,
            113456788L,
            837654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        val medicine4 = Medicine(4,
            "c",
            2f,
            "pomade",
            1,
            123439785L,
            18,
            33,
            113456788L,
            837654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        val medicine5 = Medicine(5,
            "c",
            2f,
            "pomade",
            1,
            123443785L,
            21,
            33,
            113456788L,
            837654321L,
            false,
            false,
            1,
            medicinePeriodSet = true,
            medicineNeedsReschedule = false,
            "noID")

        val medicineList = listOf(medicine1, medicine2, medicine3, medicine4, medicine5)

        dao.insertMedicines(medicineList)

        val medicineId2 = dao.getMedicineById(2)
        assertThat(medicineId2?.name).isEqualTo("b")
        assertThat(medicineId2?.form).isEqualTo("inhaler")
    }
}