package com.example.util

import com.example.model.IssueSeverity
import com.example.model.ReportType
import com.example.model.ValidationIssue
import com.example.model.ValidationReport

object ReportValidator {

  fun validate(
    reportType: ReportType,
    headers: List<String>,
    rows: List<Map<String, String>>
  ): ValidationReport {
    val issues = mutableListOf<ValidationIssue>()

    // 1. Column Header Validation
    val normalizedHeaders = headers.map { it.trim().lowercase() }

    val dateCol = headers.find {
      val h = it.trim().lowercase()
      h.contains("date")
    }

    val amountCols = headers.filter {
      val h = it.trim().lowercase()
      h.contains("amount") || h.contains("total") || h.contains("price") ||
        h.contains("rate") || h.contains("sales") || h.contains("margin") ||
        h.contains("cogs") || h.contains("discount")
    }

    val nameCol = headers.find {
      val h = it.trim().lowercase()
      h.contains("service") || h.contains("item") || h.contains("description") ||
        h.contains("product") || h.contains("procedure") || h.contains("consult")
    }

    if (dateCol == null) {
      issues.add(
        ValidationIssue(
          rowNumber = 0,
          columnName = "Date Column",
          issue = "Missing recommended 'Date' column in spreadsheet headers.",
          severity = IssueSeverity.WARNING
        )
      )
    }

    if (amountCols.isEmpty()) {
      issues.add(
        ValidationIssue(
          rowNumber = 0,
          columnName = "Amount Column",
          issue = "No numeric amount or sales column detected in headers (e.g. Net Total, Amount, Price).",
          severity = IssueSeverity.ERROR
        )
      )
    }

    if (reportType == ReportType.SERVICE_SALES && nameCol == null) {
      issues.add(
        ValidationIssue(
          rowNumber = 0,
          columnName = "Service Name Column",
          issue = "Missing 'Service Name' or 'Description' column for Service Sales export.",
          severity = IssueSeverity.WARNING
        )
      )
    }

    // 2. Row Data Validation
    var validRowCount = 0

    rows.forEachIndexed { index, row ->
      val rowNum = index + 2 // 1-based index considering header is row 1
      var rowHasErrors = false

      // Check date
      if (dateCol != null) {
        val dateVal = row[dateCol]?.trim() ?: ""
        if (dateVal.isBlank()) {
          issues.add(
            ValidationIssue(
              rowNumber = rowNum,
              columnName = dateCol,
              issue = "Blank date encountered.",
              severity = IssueSeverity.ERROR
            )
          )
          rowHasErrors = true
        }
      }

      // Check numeric amount columns
      for (col in amountCols) {
        val rawVal = row[col]?.trim() ?: ""
        if (rawVal.isNotBlank()) {
          // Clean currency symbols, commas, spaces
          val cleaned = rawVal
            .replace("$", "")
            .replace("£", "")
            .replace("€", "")
            .replace(",", "")
            .trim()

          val parsed = cleaned.toDoubleOrNull()
          if (parsed == null) {
            issues.add(
              ValidationIssue(
                rowNumber = rowNum,
                columnName = col,
                issue = "Non-numeric amount '$rawVal' in column '$col'.",
                severity = IssueSeverity.ERROR
              )
            )
            rowHasErrors = true
          }
        }
      }

      // Check empty required description
      if (nameCol != null) {
        val nameVal = row[nameCol]?.trim() ?: ""
        if (nameVal.isBlank()) {
          issues.add(
            ValidationIssue(
              rowNumber = rowNum,
              columnName = nameCol,
              issue = "Missing service or item name.",
              severity = IssueSeverity.WARNING
            )
          )
        }
      }

      if (!rowHasErrors) {
        validRowCount++
      }
    }

    return ValidationReport(
      totalRows = rows.size,
      validRows = validRowCount,
      detectedColumns = headers,
      issues = issues
    )
  }
}
