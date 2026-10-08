package com.example.model

enum class ReportType(val displayName: String, val description: String) {
  SERVICE_SALES("Service Sales", "Procedures, diagnostic tests, therapy & specialty sales"),
  PHARMACY_MARGIN("Pharmacy Margin", "Dispensary sales, drug formulations, Rx costs & margins"),
  CONSULTATIONS("Consultations", "Attending physician appointment volumes & encounters");

  companion object {
    fun fromName(name: String): ReportType {
      return values().find { it.displayName.equals(name, ignoreCase = true) || it.name.equals(name, ignoreCase = true) }
        ?: SERVICE_SALES
    }
  }
}

enum class IssueSeverity {
  ERROR,
  WARNING
}

data class ValidationIssue(
  val rowNumber: Int,
  val columnName: String,
  val issue: String,
  val severity: IssueSeverity = IssueSeverity.ERROR
)

data class ValidationReport(
  val totalRows: Int,
  val validRows: Int,
  val detectedColumns: List<String>,
  val issues: List<ValidationIssue> = emptyList()
) {
  val hasErrors: Boolean
    get() = issues.any { it.severity == IssueSeverity.ERROR }

  val errorCount: Int
    get() = issues.count { it.severity == IssueSeverity.ERROR }

  val warningCount: Int
    get() = issues.count { it.severity == IssueSeverity.WARNING }
}

data class UploadRecord(
  val uploadId: String,
  val fileName: String,
  val reportType: ReportType,
  val branch: String,
  val reportDate: String,
  val parsedRowCount: Int,
  val savedRowCount: Int,
  val uploadedAt: Long,
  val uploadedBy: String,
  val headers: List<String> = emptyList()
)

data class DuplicateCheckResult(
  val exists: Boolean,
  val existingUpload: UploadRecord? = null
)
