package org.example.project.presentation.state

import org.example.project.domain.model.AlertRule
import org.example.project.domain.model.AlertTriggered

data class AlertsState(
    val rules: List<AlertRule> = emptyList(),
    val triggered: List<AlertTriggered> = emptyList()
)
