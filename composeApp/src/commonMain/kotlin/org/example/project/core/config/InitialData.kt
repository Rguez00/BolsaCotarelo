package org.example.project.core.config

import org.example.project.domain.model.Sector
import org.example.project.domain.model.Stock

object InitialData {

    fun defaultStocks(): List<Stock> = listOf(
        // TECHNOLOGY
        Stock("NebulaSoft", "NBS", Sector.TECHNOLOGY, 125.0, 1.2),
        Stock("QuantumApps", "QAP", Sector.TECHNOLOGY, 78.0, 1.5),
        Stock("BlueGrid AI", "BGA", Sector.TECHNOLOGY, 52.0, 1.8),

        // ENERGY
        Stock("HelioPower", "HLP", Sector.ENERGY, 41.0, 1.1),
        Stock("NordOil", "NDO", Sector.ENERGY, 63.0, 1.4),
        Stock("EcoWatt", "ECW", Sector.ENERGY, 29.0, 0.9),

        // BANKING
        Stock("IberBank", "IBK", Sector.BANKING, 18.0, 0.8),
        Stock("CapitalNova", "CPN", Sector.BANKING, 36.0, 1.0),
        Stock("UnionCredit", "UCR", Sector.BANKING, 22.0, 0.7),

        // RETAIL
        Stock("MegaMart", "MGM", Sector.RETAIL, 54.0, 1.0),
        Stock("UrbanTrade", "UBT", Sector.RETAIL, 33.0, 1.2),
        Stock("FreshCart", "FRC", Sector.RETAIL, 26.0, 1.3),

        // HEALTHCARE
        Stock("VitaCare", "VTC", Sector.HEALTHCARE, 92.0, 0.9),
        Stock("BioPulse", "BPL", Sector.HEALTHCARE, 67.0, 1.1),
        Stock("MediCore", "MDC", Sector.HEALTHCARE, 48.0, 1.0)
    )
}
