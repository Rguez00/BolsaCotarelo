package org.example.project.domain.strategy

import kotlinx.coroutines.flow.StateFlow

interface StrategiesRepository {
    val rules: StateFlow<List<StrategyRule>>

    fun upsert(rule: StrategyRule)
    fun remove(id: Int)
    fun setEnabled(id: Int, enabled: Boolean)

    fun nextId(): Int   // ❌ NO suspend
}

