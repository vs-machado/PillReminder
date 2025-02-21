package com.phoenix.remedi.feature_alarms.domain.model

data class ExpiredMedicinesInfo(
    val treatmentID: String,
    val name: String,
    val quantity: Float,
    val form: String,
    val endDate: Long,
    val frequency: String,
    val currentTime: Long
)