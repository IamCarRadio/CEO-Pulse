package com.example.model

enum class ValuationMode(val displayName: String, val shortLabel: String, val description: String) {
  COLLECTED(
    displayName = "Collected Revenue",
    shortLabel = "Collected",
    description = "Actual invoiced cash collected (Rs. 0 package bills count as Rs. 0)"
  ),
  SERVICE_VALUE(
    displayName = "Service-Value Revenue",
    shortLabel = "Service-Value",
    description = "Package & enrolled sessions valued at defined per-session rate"
  )
}

data class PackageValueSetting(
  val serviceName: String,
  val category: String = "Clinical",
  val perSessionValue: Double,
  val notes: String = ""
)

data class UnmappedPackageService(
  val serviceName: String,
  val category: String,
  val sessionCount: Int,
  val lastSeenDate: String,
  val samplePatientId: String,
  val affectedRows: List<RawClinicRow> = emptyList()
)

data class ReconciliationSummary(
  val collectedRevenue: Double,
  val packageRectifiedValue: Double,
  val serviceValueTotal: Double,
  val totalSessionCount: Int,
  val paidSessionCount: Int,
  val packageSessionCount: Int,
  val unmappedServiceCount: Int,
  val unmappedSessionCount: Int,
  val unmappedServices: List<UnmappedPackageService> = emptyList(),
  val packageRows: List<RawClinicRow> = emptyList()
) {
  val isReconciled: Boolean
    get() = kotlin.math.abs((collectedRevenue + packageRectifiedValue) - serviceValueTotal) < 0.01
}
