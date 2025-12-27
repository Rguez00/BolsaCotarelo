package org.example.project.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.example.project.domain.model.PortfolioSnapshot
import org.example.project.domain.model.Transaction
import org.example.project.presentation.state.PortfolioState

/**
 * Repositorio del portfolio del usuario.
 *
 * Fuente de verdad:
 * - portfolioState: lo recalcula el repo al cambiar precios y al operar.
 *
 * Reglas:
 * - cash inicial 10.000€
 * - quantity > 0
 * - BUY: cash suficiente para (gross + comisión)
 * - SELL: holdings suficientes
 * - comisión: 0.5% (0.005)
 *
 * ✅ Persistencia (JSON) - contrato:
 * - Un repo PUEDE soportar persistencia.
 * - Si no la soporta, debe devolver Result.failure(UnsupportedOperationException).
 *
 * Importante:
 * - El JSON guarda el estado "source of truth" (cash, holdings, transactions, nextTxId...).
 * - NO guarda datos derivados (positions, PnL), porque se recalculan con el market al cargar.
 */
interface PortfolioRepository {

    /** Estado “live” para UI (cash, holdings, positions, transacciones, PnL...). */
    val portfolioState: StateFlow<PortfolioState>

    // ===== Confirmación previa (NO modifica estado) =====
    suspend fun previewBuy(ticker: String, quantity: Int): Result<TransactionPreview>
    suspend fun previewSell(ticker: String, quantity: Int): Result<TransactionPreview>

    // ===== Operación confirmada (modifica estado) =====
    suspend fun buy(ticker: String, quantity: Int): Result<Transaction>
    suspend fun sell(ticker: String, quantity: Int): Result<Transaction>

    // ===== Extra =====
    suspend fun getTransactions(): List<Transaction>
    suspend fun exportTransactionsCsv(): String
    suspend fun getSnapshot(): PortfolioSnapshot

    // ============================================================
    // ✅ Persistencia (JSON)
    // ============================================================

    /**
     * Exporta el estado persistible del portfolio (cash, holdings, transactions, contadores...)
     * a un JSON (string). No incluye datos derivados como PnL/positions.
     */
    suspend fun exportStateJson(): Result<String> =
        Result.failure(UnsupportedOperationException("Persistencia no soportada por este PortfolioRepository"))

    /**
     * Importa un estado persistible desde JSON y lo aplica como fuente de verdad.
     * Debe:
     * - validar el payload,
     * - restaurar cash/holdings/transactions/ids,
     * - recalcular portfolioState al finalizar.
     */
    suspend fun importStateJson(json: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Persistencia no soportada por este PortfolioRepository"))

    /**
     * Borra el estado persistido y/o resetea a estado inicial (cash inicial, sin posiciones).
     * Útil para "Reset" desde UI.
     */
    suspend fun clearState(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Persistencia no soportada por este PortfolioRepository"))
}
