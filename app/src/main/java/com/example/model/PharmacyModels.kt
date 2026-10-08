package com.example.model

enum class PharmacySortOption(val displayName: String) {
  REVENUE_DESC("Highest Revenue"),
  MARGIN_DESC("Highest Margin (Rs.)"),
  MARGIN_PCT_DESC("Highest Margin %"),
  QUANTITY_DESC("Units Sold (Volume)"),
  NAME_ASC("Product Name (A-Z)")
}

data class PharmacyItem(
  val rowId: String,
  val uploadId: String,
  val reportDate: String,
  val branch: String,
  val productCode: String,
  val productName: String,
  val category: String,
  val quantity: Int,
  val unitCost: Double,
  val unitPrice: Double,
  val totalCost: Double,
  val totalRevenue: Double,
  val grossMargin: Double,
  val marginPercentage: Double,
  val columns: Map<String, String> = emptyMap()
)

data class PharmacyProductRanking(
  val productCode: String,
  val productName: String,
  val category: String,
  val unitsSold: Int,
  val revenue: Double,
  val cost: Double,
  val margin: Double,
  val marginPercentage: Double,
  val averageSellingPrice: Double
)

data class PharmacyRankings(
  val top10ByRevenue: List<PharmacyProductRanking> = emptyList(),
  val top10ByMargin: List<PharmacyProductRanking> = emptyList(),
  val bottomPerformers: List<PharmacyProductRanking> = emptyList()
)

data class PharmacyKpis(
  val totalRevenue: KpiMetric = KpiMetric(),
  val totalCost: KpiMetric = KpiMetric(),
  val grossMargin: KpiMetric = KpiMetric(),
  val marginPercentage: KpiMetric = KpiMetric(formattedValue = "0.0%"),
  val totalUnitsSold: Int = 0,
  val distinctProductCount: Int = 0
)

data class PharmacyReconciliation(
  val fileTotalRevenue: Double = 0.0,
  val fileTotalCost: Double = 0.0,
  val fileTotalMargin: Double = 0.0,
  val appTotalRevenue: Double = 0.0,
  val appTotalCost: Double = 0.0,
  val appTotalMargin: Double = 0.0,
  val isRevenueMatched: Boolean = true,
  val isCostMatched: Boolean = true,
  val isMarginMatched: Boolean = true,
  val totalItemsAudited: Int = 0,
  val revenueVariance: Double = 0.0,
  val costVariance: Double = 0.0,
  val marginVariance: Double = 0.0
) {
  val isFullyReconciled: Boolean
    get() = isRevenueMatched && isCostMatched && isMarginMatched
}

data class DualStreamRevenue(
  val clinicalServiceRevenue: Double,
  val pharmacyRevenue: Double,
  val totalCombinedRevenue: Double,
  val clinicalSharePercentage: Double,
  val pharmacySharePercentage: Double,
  val clinicalBillCount: Int,
  val pharmacyUnitsSold: Int
)
