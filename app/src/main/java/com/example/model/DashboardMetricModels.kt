package com.example.model

data class RawClinicRow(
  val rowId: String,
  val uploadId: String,
  val reportType: String,
  val branch: String,
  val reportDate: String,
  val columns: Map<String, String>
) {
  val invoiceNo: String
    get() = columns["InvoiceNo"] ?: columns["BillNo"] ?: columns["Invoice"] ?: rowId

  val doctor: String
    get() = columns["Doctor"] ?: columns["Practitioner"] ?: columns["Provider"] ?: "Unassigned"

  val category: String
    get() = columns["Category"] ?: columns["Department"] ?: columns["Specialty"] ?: "General"

  val serviceName: String
    get() = columns["ServiceName"] ?: columns["Description"] ?: columns["Service"] ?: columns["Item"] ?: "General Procedure"

  val amount: Double
    get() {
      val raw = columns["NetTotal"] ?: columns["TotalAmount"] ?: columns["Total"] ?: columns["Amount"] ?: columns["Price"] ?: "0"
      return raw.replace("$", "").replace("£", "").replace("€", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
    }

  val quantity: Int
    get() {
      val raw = columns["Quantity"] ?: columns["Qty"] ?: "1"
      return raw.replace(",", "").trim().toIntOrNull() ?: 1
    }

  val isPackageSession: Boolean
    get() = amount <= 0.0001 || columns["PaymentMethod"]?.contains("Package", ignoreCase = true) == true ||
      columns["InvoiceNo"]?.contains("PKG", ignoreCase = true) == true

  fun getEffectiveAmount(valuationMode: ValuationMode, packageValues: Map<String, Double>): Double {
    return when (valuationMode) {
      ValuationMode.COLLECTED -> amount
      ValuationMode.SERVICE_VALUE -> {
        if (isPackageSession) {
          packageValues[serviceName.trim()] ?: 0.0
        } else {
          amount
        }
      }
    }
  }
}

data class KpiMetric(
  val value: Double = 0.0,
  val formattedValue: String = "$0.00",
  val previousValue: Double = 0.0,
  val percentageChange: Double = 0.0,
  val isPositive: Boolean = true,
  val isNeutral: Boolean = true
)

data class DashboardKpis(
  val totalRevenue: KpiMetric,
  val patientBillsCount: KpiMetric,
  val averageBillValue: KpiMetric,
  val totalProcedureUnits: Int
)

data class DailyTrendPoint(
  val date: String,
  val displayDate: String,
  val revenue: Double,
  val billsCount: Int
)

data class ServiceItemMetric(
  val serviceName: String,
  val category: String,
  val revenue: Double,
  val count: Int,
  val averagePrice: Double
)

data class ServiceRankings(
  val top10ByRevenue: List<ServiceItemMetric>,
  val bottom10ByRevenue: List<ServiceItemMetric>,
  val top10ByCount: List<ServiceItemMetric>,
  val bottom10ByCount: List<ServiceItemMetric>
)

enum class CategoryGroup(val displayName: String, val colorHex: Long) {
  SKIN("Skin", 0xFF0D9488),       // Teal / Emerald
  OBESITY("Obesity", 0xFFF59E0B), // Amber / Gold
  HAIR("Hair", 0xFF8B5CF6),       // Violet / Purple
  OTHER("Other", 0xFF64748B)      // Slate Grey
}

data class CategoryMetric(
  val category: CategoryGroup,
  val revenue: Double,
  val percentage: Double,
  val count: Int
)

data class DoctorRevenueMetric(
  val doctorName: String,
  val revenue: Double,
  val percentage: Double,
  val billCount: Int,
  val averageBill: Double
)

data class DominantUndersoldAnalysis(
  val dominantServices: List<ServiceItemMetric>,
  val underSoldServices: List<ServiceItemMetric>
)

data class AuditInspectionData(
  val title: String,
  val formula: String,
  val computedValue: String,
  val rows: List<RawClinicRow>
)
