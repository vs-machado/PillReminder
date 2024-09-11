package com.phoenix.pillreminder.feature_alarms.domain.util

sealed class MedicineFrequency {
    data object EveryDay: MedicineFrequency()
    data object EveryOtherDay: MedicineFrequency()
    data object SpecificDaysOfWeek: MedicineFrequency()
    data object EveryXDays: MedicineFrequency()
    data object EveryXWeeks: MedicineFrequency()
    data object EveryXMonths: MedicineFrequency()

}
