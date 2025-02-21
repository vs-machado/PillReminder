package com.phoenix.remedi.feature_alarms.domain.util

sealed class DayOfWeek(val dayInt: Int) {
    data object Sunday : DayOfWeek(0)
    data object Monday : DayOfWeek(1)
    data object Tuesday : DayOfWeek(2)
    data object Wednesday : DayOfWeek(3)
    data object Thursday : DayOfWeek(4)
    data object Friday : DayOfWeek(5)
    data object Saturday : DayOfWeek(6)

    companion object {
        fun fromInt(dayInt: Int): DayOfWeek? = when(dayInt) {
            0 -> Sunday
            1 -> Monday
            2 -> Tuesday
            3 -> Wednesday
            4 -> Thursday
            5 -> Friday
            6 -> Saturday
            else -> null
        }
    }
}