package com.phoenix.pillreminder.feature_alarms.domain.util

sealed class MedicineFrequency(val interval: Int?) {
    data object EveryDay: MedicineFrequency(null)
    data object EveryOtherDay: MedicineFrequency(null)
    data object SpecificDaysOfWeek: MedicineFrequency(null)
    class EveryXDays(interval: Int): MedicineFrequency(interval)
    class EveryXWeeks(interval: Int): MedicineFrequency(interval)
    class EveryXMonths(interval: Int): MedicineFrequency(interval)

}
