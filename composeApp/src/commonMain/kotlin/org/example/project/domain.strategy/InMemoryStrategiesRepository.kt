package org.example.project.domain.strategy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryStrategiesRepository : StrategiesRepository {

    private val idMutex = Mutex()
    private var idCounter = 1

    private val _rules = MutableStateFlow<List<StrategyRule>>(emptyList())
    override val rules: StateFlow<List<StrategyRule>> = _rules

    override fun nextId(): Int {
        var id = 0
        // ⚠ Mutex requiere suspend → usamos runBlocking SOLO aquí
        runBlocking {
            idMutex.withLock {
                id = idCounter++
            }
        }
        return id
    }

    override fun upsert(rule: StrategyRule) {
        val current = _rules.value
        _rules.value = if (current.any { it.id == rule.id }) {
            current.map { if (it.id == rule.id) rule else it }
        } else {
            current + rule
        }
    }

    override fun remove(id: Int) {
        _rules.value = _rules.value.filterNot { it.id == id }
    }

    override fun setEnabled(id: Int, enabled: Boolean) {
        _rules.value = _rules.value.map { r ->
            if (r.id != id) r else when (r) {
                is StrategyRule.AutoBuyDip -> r.copy(enabled = enabled)
                is StrategyRule.TakeProfit -> r.copy(enabled = enabled)
                is StrategyRule.StopLoss -> r.copy(enabled = enabled)
            }
        }
    }
}

