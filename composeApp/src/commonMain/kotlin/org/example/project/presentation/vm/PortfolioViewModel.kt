package org.example.project.presentation.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.example.project.data.repository.PortfolioRepository
import org.example.project.data.repository.TransactionPreview
import org.example.project.domain.model.Transaction

class PortfolioViewModel(
    private val repo: PortfolioRepository,
    private val externalScope: CoroutineScope? = null,

    /**
     * ✅ Hook para persistir al confirmar una operación (BUY/SELL) con éxito.
     * - En AppRoot le pasamos una lambda suspend que escribe el JSON.
     */
    private val onPersistNow: (suspend () -> Unit)? = null,

    private val canTradeProvider: () -> Boolean = { true }
) {
    private val vmJob = SupervisorJob()
    private val scope: CoroutineScope =
        externalScope ?: CoroutineScope(Dispatchers.Main.immediate + vmJob)

    val portfolioState = repo.portfolioState

    enum class Mode { BUY, SELL }

    var dialogOpen by mutableStateOf(false)
        private set

    var mode by mutableStateOf(Mode.BUY)
        private set

    var ticker by mutableStateOf("")
        private set

    var quantityText by mutableStateOf("1")
        private set

    var preview by mutableStateOf<TransactionPreview?>(null)
        private set

    var lastTx by mutableStateOf<Transaction?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var isBusy by mutableStateOf(false)
        private set

    private var previewJob: Job? = null
    private var confirmJob: Job? = null

    fun openTrade(ticker: String, mode: Mode) {
        if (!canTradeProvider()) return

        this.ticker = normalizeTicker(ticker)
        this.mode = mode

        quantityText = "1"
        preview = null
        lastTx = null
        error = null
        isBusy = false

        dialogOpen = true
        refreshPreview()
    }

    fun closeTrade() {
        dialogOpen = false
        previewJob?.cancel(); previewJob = null
        confirmJob?.cancel(); confirmJob = null
        isBusy = false
        error = null
        preview = null
        lastTx = null
    }

    fun updateQuantityText(text: String) {
        quantityText = text
        lastTx = null
        error = null
        refreshPreview()
    }

    fun refreshPreview() {
        if (!dialogOpen) return

        if (!canTradeProvider()) {
            previewJob?.cancel(); previewJob = null
            preview = null
            error = "Mercado no disponible (cerrado o pausado)"
            return
        }

        if (quantityText.isBlank()) {
            preview = null
            error = null
            return
        }

        val qty = parseQuantityOrNull(quantityText)
        if (qty == null || qty <= 0) {
            preview = null
            error = "Cantidad inválida"
            return
        }

        previewJob?.cancel()
        previewJob = scope.launch {
            val result = when (mode) {
                Mode.BUY -> repo.previewBuy(ticker, qty)
                Mode.SELL -> repo.previewSell(ticker, qty)
            }

            result.fold(
                onSuccess = {
                    preview = it
                    error = null
                },
                onFailure = { e ->
                    preview = null
                    error = e.message ?: "Error"
                }
            )
        }
    }

    fun confirm() {
        if (!dialogOpen || isBusy) return

        if (!canTradeProvider()) {
            lastTx = null
            preview = null
            error = "Mercado no disponible (cerrado o pausado)"
            return
        }

        if (quantityText.isBlank()) return

        val qty = parseQuantityOrNull(quantityText)
        if (qty == null || qty <= 0) {
            lastTx = null
            preview = null
            error = "Cantidad inválida"
            return
        }

        previewJob?.cancel()
        previewJob = null
        confirmJob?.cancel()

        isBusy = true
        error = null
        lastTx = null

        confirmJob = scope.launch {
            try {
                if (!canTradeProvider()) {
                    lastTx = null
                    preview = null
                    error = "Mercado no disponible (cerrado o pausado)"
                    return@launch
                }

                val result = when (mode) {
                    Mode.BUY -> repo.buy(ticker, qty)
                    Mode.SELL -> repo.sell(ticker, qty)
                }

                result.fold(
                    onSuccess = { tx ->
                        lastTx = tx
                        error = null
                        preview = null

                        // ✅ PERSISTE YA MISMO (lo más fiable contra “swipe kill”)
                        runCatching { onPersistNow?.invoke() }.onFailure { it.printStackTrace() }
                    },
                    onFailure = { e ->
                        lastTx = null
                        preview = null
                        error = e.message ?: "Error"
                        refreshPreview()
                    }
                )
            } finally {
                isBusy = false
            }
        }
    }

    fun close() {
        previewJob?.cancel()
        confirmJob?.cancel()
        if (externalScope == null) vmJob.cancel()
    }

    private fun normalizeTicker(raw: String): String = raw.trim().uppercase()
    private fun parseQuantityOrNull(text: String): Int? = text.trim().toIntOrNull()
}
