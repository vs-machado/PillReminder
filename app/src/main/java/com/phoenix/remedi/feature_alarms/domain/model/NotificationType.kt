package com.phoenix.remedi.feature_alarms.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class NotificationType : Parcelable {
    NORMAL,
    FOLLOWUP,
    PILLBOX_REMINDER
}