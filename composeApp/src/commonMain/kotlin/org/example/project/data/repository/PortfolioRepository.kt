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
}
