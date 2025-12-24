package org.example.project.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import org.example.project.domain.model.AlertRule
import org.example.project.domain.model.AlertTriggered
import org.example.project.presentation.state.AlertsState
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Repo + evaluador de alertas en memoria.
 *
 * One-shot:
 * - Cuando dispara: triggered=true y enabled=false.
 * - Para rearmar: setRuleEnabled(id, true)
 */
class InMemoryAlertsRepository(
    private val marketRepo: MarketRepository,
    private val externalScope: CoroutineScope? = null
) : AlertsRepository {

    private val job: Job? = if (externalScope == null) SupervisorJob() else null
    private val scope: CoroutineScope = externalScope ?: CoroutineScope(Dispatchers.Default + job!!)
    private val mutex = Mutex()

    private val _alertsState = MutableStateFlow(AlertsState())
    override val alertsState: StateFlow<AlertsState> = _alertsState

    private var nextRuleId: Long = 1L
    private var nextTriggeredId: Int = 1

    private var pricesJob: Job? = null

    init {
        pricesJob = scope.launch {
            marketRepo.priceUpdates.collect { snap ->
                val t = normalizeTicker(snap.ticker)

                mutex.withLock {
                    val currentState = _alertsState.value
                    val candidates = currentState.rules.filter { r ->
                        normalizeTicker(r.ticker) == t && r.enabled && !r.triggered
                    }
                    if (candidates.isEmpty()) return@withLock

                    var newRules = currentState.rules
                    var newTriggered = currentState.triggered

                    for (rule in candidates) {
                        if (isFired(rule, snap.currentPrice, snap.changePercent)) {
                            val firedRule = markTriggered(rule)

                            newRules = newRules.map { if (it.id == rule.id) firedRule else it }

                            val trig = AlertTriggered(
                                id = nextTriggeredId++,
                                timestamp = Clock.System.now(),
                                rule = firedRule,
                                currentPrice = snap.currentPrice,
                                currentChangePercent = snap.changePercent,
                                message = buildMessage(firedRule, snap.currentPrice, snap.changePercent)
                            )
                            newTriggered = newTriggered + trig
                        }
                    }

                    _alertsState.value = currentState.copy(
                        rules = newRules,
                        triggered = newTriggered
                    )
                }
            }
        }
    }

    override suspend fun upsertRule(rule: AlertRule): AlertRule {
        val normalized = normalizeRule(rule)
        val withId = ensureId(normalized)

        mutex.withLock {
            val rules = _alertsState.value.rules.toMutableList()
            val idx = rules.indexOfFirst { it.id == withId.id }
            if (idx >= 0) rules[idx] = withId else rules.add(withId)
            _alertsState.value = _alertsState.value.copy(rules = rules)
        }

        return withId
    }

    override suspend fun removeRule(ruleId: Long) {
        mutex.withLock {
            val rules = _alertsState.value.rules.filterNot { it.id == ruleId }
            _alertsState.value = _alertsState.value.copy(rules = rules)
        }
    }

    override suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean) {
        mutex.withLock {
            val rules = _alertsState.value.rules.map { r ->
                if (r.id != ruleId) r
                else if (enabled) resetAndEnable(r) else disable(r)
            }
            _alertsState.value = _alertsState.value.copy(rules = rules)
        }
    }

    override suspend fun clearTriggered() {
        mutex.withLock {
            _alertsState.value = _alertsState.value.copy(triggered = emptyList())
        }
    }

    override suspend fun clearAll() {
        mutex.withLock {
            _alertsState.value = AlertsState()
        }
    }

    fun close() {
        pricesJob?.cancel()
        pricesJob = null
        job?.cancel()
    }

    // ============================================================
    // Internals
    // ============================================================

    private fun isFired(rule: AlertRule, currentPrice: Double, changePercent: Double): Boolean =
        when (rule) {
            is AlertRule.PriceAbove -> currentPrice >= rule.threshold
            is AlertRule.PriceBelow -> currentPrice <= rule.threshold
            is AlertRule.PercentChangeAbove -> changePercent >= rule.thresholdPercent
            is AlertRule.PercentChangeBelow -> changePercent <= rule.thresholdPercent
        }

    private fun markTriggered(rule: AlertRule): AlertRule {
        val now = System.currentTimeMillis()
        return when (rule) {
            is AlertRule.PriceAbove -> rule.copy(enabled = false, triggered = true, triggeredAtMillis = now)
            is AlertRule.PriceBelow -> rule.copy(enabled = false, triggered = true, triggeredAtMillis = now)
            is AlertRule.PercentChangeAbove -> rule.copy(enabled = false, triggered = true, triggeredAtMillis = now)
            is AlertRule.PercentChangeBelow -> rule.copy(enabled = false, triggered = true, triggeredAtMillis = now)
        }
    }

    private fun disable(rule: AlertRule): AlertRule =
        when (rule) {
            is AlertRule.PriceAbove -> rule.copy(enabled = false)
            is AlertRule.PriceBelow -> rule.copy(enabled = false)
            is AlertRule.PercentChangeAbove -> rule.copy(enabled = false)
            is AlertRule.PercentChangeBelow -> rule.copy(enabled = false)
        }

    private fun resetAndEnable(rule: AlertRule): AlertRule =
        when (rule) {
            is AlertRule.PriceAbove -> rule.copy(enabled = true, triggered = false, triggeredAtMillis = null)
            is AlertRule.PriceBelow -> rule.copy(enabled = true, triggered = false, triggeredAtMillis = null)
            is AlertRule.PercentChangeAbove -> rule.copy(enabled = true, triggered = false, triggeredAtMillis = null)
            is AlertRule.PercentChangeBelow -> rule.copy(enabled = true, triggered = false, triggeredAtMillis = null)
        }

    private fun ensureId(rule: AlertRule): AlertRule {
        if (rule.id > 0L) return rule
        val id = nextRuleId++
        return when (rule) {
            is AlertRule.PriceAbove -> rule.copy(id = id)
            is AlertRule.PriceBelow -> rule.copy(id = id)
            is AlertRule.PercentChangeAbove -> rule.copy(id = id)
            is AlertRule.PercentChangeBelow -> rule.copy(id = id)
        }
    }

    private fun normalizeRule(rule: AlertRule): AlertRule {
        val t = normalizeTicker(rule.ticker)
        return when (rule) {
            is AlertRule.PriceAbove -> rule.copy(ticker = t)
            is AlertRule.PriceBelow -> rule.copy(ticker = t)
            is AlertRule.PercentChangeAbove -> rule.copy(ticker = t)
            is AlertRule.PercentChangeBelow -> rule.copy(ticker = t)
        }
    }

    private fun normalizeTicker(raw: String): String = raw.trim().uppercase()

    private fun buildMessage(rule: AlertRule, currentPrice: Double, changePercent: Double): String =
        when (rule) {
            is AlertRule.PriceAbove ->
                "🔔 ${rule.ticker}: Precio ≥ ${fmt2(rule.threshold)} (ahora ${fmt2(currentPrice)})"
            is AlertRule.PriceBelow ->
                "🔔 ${rule.ticker}: Precio ≤ ${fmt2(rule.threshold)} (ahora ${fmt2(currentPrice)})"
            is AlertRule.PercentChangeAbove ->
                "🔔 ${rule.ticker}: % ≥ ${fmt2(rule.thresholdPercent)}% (ahora ${fmt2(changePercent)}%)"
            is AlertRule.PercentChangeBelow ->
                "🔔 ${rule.ticker}: % ≤ ${fmt2(rule.thresholdPercent)}% (ahora ${fmt2(changePercent)}%)"
        }

    private fun fmt2(value: Double): String {
        val sign = if (value < 0) "-" else ""
        val absValue = abs(value)

        val scaled = (absValue * 100.0).roundToLong()
        val integer = scaled / 100
        val frac = (scaled % 100).toInt()

        return "$sign$integer.${frac.toString().padStart(2, '0')}"
    }
}
