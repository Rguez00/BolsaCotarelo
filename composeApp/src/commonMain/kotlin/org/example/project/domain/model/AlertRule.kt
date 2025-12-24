package org.example.project.domain.model

sealed class AlertRule(
    open val id: Long = 0,
    open val ticker: String,
    open val enabled: Boolean = true,
    open val triggered: Boolean = false,
    open val triggeredAtMillis: Long? = null
) {
    data class PriceAbove(
        override val id: Long = 0,
        override val ticker: String,
        val threshold: Double,
        override val enabled: Boolean = true,
        override val triggered: Boolean = false,
        override val triggeredAtMillis: Long? = null
    ) : AlertRule(id, ticker, enabled, triggered, triggeredAtMillis)

    data class PriceBelow(
        override val id: Long = 0,
        override val ticker: String,
        val threshold: Double,
        override val enabled: Boolean = true,
        override val triggered: Boolean = false,
        override val triggeredAtMillis: Long? = null
    ) : AlertRule(id, ticker, enabled, triggered, triggeredAtMillis)

    data class PercentChangeAbove(
        override val id: Long = 0,
        override val ticker: String,
        val thresholdPercent: Double,
        override val enabled: Boolean = true,
        override val triggered: Boolean = false,
        override val triggeredAtMillis: Long? = null
    ) : AlertRule(id, ticker, enabled, triggered, triggeredAtMillis)

    data class PercentChangeBelow(
        override val id: Long = 0,
        override val ticker: String,
        val thresholdPercent: Double,
        override val enabled: Boolean = true,
        override val triggered: Boolean = false,
        override val triggeredAtMillis: Long? = null
    ) : AlertRule(id, ticker, enabled, triggered, triggeredAtMillis)
}
