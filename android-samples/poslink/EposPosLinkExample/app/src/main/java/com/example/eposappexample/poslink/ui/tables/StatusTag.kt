package com.example.eposappexample.poslink.ui.tables

import androidx.compose.runtime.Composable
import com.teya.lemonade.LemonadeUi
import com.teya.lemonade.Tag
import com.teya.lemonade.core.TagVoice
import com.teya.unifiedepossdk.poslink.models.tabs.TabStatus

@Composable
fun StatusTag(status: TabStatus) {
    val (voice, label) = when (status) {
        TabStatus.OPEN -> TagVoice.Positive to "Open"
        TabStatus.PAYING -> TagVoice.Info to "Paying"
        TabStatus.PAUSED -> TagVoice.Warning to "Paused"
        TabStatus.COMPLETED -> TagVoice.Positive to "Completed"
        TabStatus.CLOSED -> TagVoice.Neutral to "Closed"
        TabStatus.UNKNOWN -> TagVoice.Neutral to "Unknown"
    }
    LemonadeUi.Tag(label = label, voice = voice)
}