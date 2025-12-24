package org.example.project.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.example.project.domain.model.AlertRule
import org.example.project.presentation.state.AlertsState

interface AlertsRepository {
    val alertsState: StateFlow<AlertsState>

    /** Inserta o actualiza una regla. Devuelve la regla final (con id si se asignó). */
    suspend fun upsertRule(rule: AlertRule): AlertRule

    suspend fun removeRule(ruleId: Long)

    /**
     * Activa/desactiva una regla.
     * Si enabled=true, se rearma (triggered=false y triggeredAtMillis=null).
     */
    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean)

    /** Limpia solo el histórico de disparos (NO rearma reglas). */
    suspend fun clearTriggered()

    /** Limpia reglas + histórico. */
    suspend fun clearAll()

    // Compatibilidad / comodidad (por si en UI te resulta más cómodo)
    suspend fun addRule(rule: AlertRule): AlertRule = upsertRule(rule)
    suspend fun removeRule(rule: AlertRule) = removeRule(rule.id)
}
