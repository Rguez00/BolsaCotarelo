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
     * ✅ Nuevo:
     * Proveedor de “¿se puede tradear ahora mismo?”
     * - true => mercado abierto y NO pausado
     * - false => cerrado o pausado
     *
     * Se lo pasas desde App() leyendo engine.marketState.value.
     */
    private val canTradeProvider: () -> Boolean = { true }
) {
    private val vmJob = SupervisorJob()

    // Main.immediate va bien para Compose (Android/Desktop) si tenéis dispatcher Main configurado.
    private val scope: CoroutineScope =
        externalScope ?: CoroutineScope(Dispatchers.Main.immediate + vmJob)

    val portfolioState = repo.portfolioState

    enum class Mode { BUY, SELL }

    // UI state
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

    // Busy SOLO para confirm (no para preview), así no “molesta” mientras escribes.
    var isBusy by mutableStateOf(false)
        private set

    // Jobs para evitar resultados fuera de orden
    private var previewJob: Job? = null
    private var confirmJob: Job? = null

    // ============================================================
    // API
    // ============================================================

    fun openTrade(ticker: String, mode: Mode) {
        // ✅ Guardia: si mercado NO disponible, NO abrimos el diálogo
        if (!canTradeProvider()) {
            // opcional: si quieres mostrar algo fuera del dialog, aquí podrías exponer otro estado global
            return
        }

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

        previewJob?.cancel()
        previewJob = null

        confirmJob?.cancel()
        confirmJob = null

        // Por si acaso (aunque el finally lo cubriría)
        isBusy = false
        error = null
        preview = null
        lastTx = null
    }

    fun updateQuantityText(text: String) {
        quantityText = text
        lastTx = null // opcional: al cambiar cantidad, “limpia” el último OK
        refreshPreview()
    }

    fun refreshPreview() {
        if (!dialogOpen) return

        // ✅ Si el mercado se cierra/pausa mientras el diálogo está abierto:
        if (!canTradeProvider()) {
            previewJob?.cancel()
            previewJob = null
            preview = null
            error = "Mercado no disponible (cerrado o pausado)"
            return
        }

        // ✅ Estado neutro: si está vacío, NO mostramos error y NO preview.
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

        // Cancelamos el preview anterior para que no pise el último
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
        if (!dialogOpen) return
        if (isBusy) return

        // ✅ Guardia extra: si mercado NO disponible, no confirmamos
        if (!canTradeProvider()) {
            lastTx = null
            preview = null
            error = "Mercado no disponible (cerrado o pausado)"
            return
        }

        // Si está vacío, no hacemos nada (el botón ya debería estar deshabilitado)
        if (quantityText.isBlank()) return

        val qty = parseQuantityOrNull(quantityText)
        if (qty == null || qty <= 0) {
            lastTx = null
            preview = null
            error = "Cantidad inválida"
            return
        }

        // Evita que un preview “antiguo” pise el estado tras confirmar
        previewJob?.cancel()
        previewJob = null

        // Cancela confirm anterior si existiese (por seguridad)
        confirmJob?.cancel()

        isBusy = true
        error = null
        lastTx = null

        confirmJob = scope.launch {
            try {
                // ✅ Volvemos a comprobar justo antes de ejecutar (por si cambió en medio)
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
                        // Refresca preview para reflejar nuevo cash/holdings tras la operación
                        refreshPreview()
                    },
                    onFailure = { e ->
                        lastTx = null
                        preview = null
                        error = e.message ?: "Error"
                        // Recalcula preview para que el usuario vea el estado actual
                        refreshPreview()
                    }
                )
            } finally {
                // ✅ Pase lo que pase (éxito, fallo o cancelación), volvemos a no-busy.
                isBusy = false
            }
        }
    }

    fun close() {
        previewJob?.cancel()
        confirmJob?.cancel()
        if (externalScope == null) vmJob.cancel()
    }

    // ============================================================
    // Helpers
    // ============================================================

    private fun normalizeTicker(raw: String): String =
        raw.trim().uppercase()

    private fun parseQuantityOrNull(text: String): Int? =
        text.trim().toIntOrNull()
}
