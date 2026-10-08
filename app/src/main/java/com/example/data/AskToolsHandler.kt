package com.example.data

import com.example.model.AskToolExecution
import com.example.model.AskToolNames
import com.example.model.DatePreset
import com.example.model.FilterState
import com.example.model.PharmacyItem
import com.example.model.RawClinicRow
import com.example.model.ReportType
import com.example.model.UploadRecord
import com.example.model.ValuationMode
import com.example.util.MetricsCalculator
import org.json.JSONArray
import org.json.JSONObject

/**
 * Pure, read-only executor for the 6 Ask Agent tools backed by the existing metrics module.
 * Cannot edit or delete any data.
 */
class AskToolsHandler(
  private val getRawRows: () -> List<RawClinicRow>,
  private val getPharmacyItems: () -> List<PharmacyItem>,
  private val getPackageValues: () -> Map<String, Double>,
  private val getUploadHistory: () -> List<UploadRecord>,
  private val getGlobalFilter: () -> FilterState,
  private val getValuationMode: () -> ValuationMode
) {

  fun executeTool(toolName: String, rawArgs: Map<String, String>): AskToolExecution {
    return when (toolName) {
      AskToolNames.GET_REVENUE -> executeGetRevenue(rawArgs)
      AskToolNames.GET_TOP_SERVICES -> executeGetTopServices(rawArgs)
      AskToolNames.GET_DOCTOR_SPLIT -> executeGetDoctorSplit(rawArgs)
      AskToolNames.GET_PHARMACY_MARGIN -> executeGetPharmacyMargin(rawArgs)
      AskToolNames.COMPARE_PERIODS -> executeComparePeriods(rawArgs)
      AskToolNames.LIST_UPLOADS -> executeListUploads(rawArgs)
      else -> {
        val outJson = JSONObject().put("error", "Unknown tool: $toolName")
        AskToolExecution(
          toolName = toolName,
          arguments = rawArgs,
          filtersUsedDisplay = "None",
          rawOutputJson = outJson.toString(),
          summaryOutput = "Error: Tool '$toolName' is not recognized."
        )
      }
    }
  }

  // 1. getRevenue
  private fun executeGetRevenue(args: Map<String, String>): AskToolExecution {
    val branchArg = args["branch"]?.takeIf { it.isNotBlank() } ?: getGlobalFilter().selectedBranch
    val periodArg = args["period"]?.takeIf { it.isNotBlank() } ?: "today"
    val modeArg = args["valuationMode"]?.takeIf { it.isNotBlank() }

    val filter = buildFilterForScope(branchArg, periodArg)
    val mode = when (modeArg?.lowercase()) {
      "service_value", "service-value", "servicevalue" -> ValuationMode.SERVICE_VALUE
      "collected", "collected_revenue" -> ValuationMode.COLLECTED
      else -> getValuationMode()
    }

    val rows = MetricsCalculator.filterRows(getRawRows(), filter)
    val pharm = MetricsCalculator.filterPharmacyItems(getPharmacyItems(), filter)
    val packageMap = getPackageValues()

    val dualStream = MetricsCalculator.calculateDualStreamRevenue(rows, pharm, mode, packageMap)
    val kpis = MetricsCalculator.calculateKpis(rows, emptyList(), mode, packageMap)

    val json = JSONObject().apply {
      put("branch", filter.selectedBranch)
      put("period", filter.formattedDateDisplay)
      put("valuationMode", if (mode == ValuationMode.SERVICE_VALUE) "Service-Value Revenue" else "Collected Revenue")
      put("totalCombinedRevenue", dualStream.totalCombinedRevenue)
      put("clinicalRevenue", dualStream.clinicalServiceRevenue)
      put("pharmacyRevenue", dualStream.pharmacyRevenue)
      put("patientBillsCount", kpis.patientBillsCount.value.toInt())
      put("averageBillValue", kpis.averageBillValue.value)
      put("procedureUnitsCompleted", kpis.totalProcedureUnits)
      put("clinicalSharePercent", String.format("%.1f%%", dualStream.clinicalSharePercentage))
      put("pharmacySharePercent", String.format("%.1f%%", dualStream.pharmacySharePercentage))
    }

    val filtersDisplay = "Branch: ${filter.selectedBranch} | Period: ${filter.formattedDateDisplay} | Valuation: ${if (mode == ValuationMode.SERVICE_VALUE) "Service-Value" else "Collected"}"
    val summary = "Total Revenue: ${MetricsCalculator.formatCurrency(dualStream.totalCombinedRevenue)} (Clinical: ${MetricsCalculator.formatCurrency(dualStream.clinicalServiceRevenue)}, Pharmacy: ${MetricsCalculator.formatCurrency(dualStream.pharmacyRevenue)}) across ${kpis.patientBillsCount.value.toInt()} bills (ABV: ${MetricsCalculator.formatCurrency(kpis.averageBillValue.value)})"

    return AskToolExecution(
      toolName = AskToolNames.GET_REVENUE,
      arguments = args,
      filtersUsedDisplay = filtersDisplay,
      rawOutputJson = json.toString(),
      summaryOutput = summary
    )
  }

  // 2. getTopServices
  private fun executeGetTopServices(args: Map<String, String>): AskToolExecution {
    val branchArg = args["branch"]?.takeIf { it.isNotBlank() } ?: getGlobalFilter().selectedBranch
    val periodArg = args["period"]?.takeIf { it.isNotBlank() } ?: "today"
    val sortBy = args["sortBy"] ?: "revenue"
    val limit = args["limit"]?.toIntOrNull() ?: 5

    val filter = buildFilterForScope(branchArg, periodArg)
    val mode = getValuationMode()
    val packageMap = getPackageValues()

    val rows = MetricsCalculator.filterRows(getRawRows(), filter)
    val rankings = MetricsCalculator.calculateTopAndBottomServices(rows, mode, packageMap)

    val items = if (sortBy.equals("count", ignoreCase = true)) {
      rankings.top10ByCount.take(limit)
    } else {
      rankings.top10ByRevenue.take(limit)
    }

    val itemsArray = JSONArray()
    items.forEachIndexed { idx, itm ->
      itemsArray.put(JSONObject().apply {
        put("rank", idx + 1)
        put("serviceName", itm.serviceName)
        put("category", itm.category)
        put("revenue", itm.revenue)
        put("count", itm.count)
        put("averagePrice", itm.averagePrice)
      })
    }

    val json = JSONObject().apply {
      put("branch", filter.selectedBranch)
      put("period", filter.formattedDateDisplay)
      put("sortBy", sortBy)
      put("limit", limit)
      put("totalUniqueServices", rankings.top10ByRevenue.size)
      put("services", itemsArray)
    }

    val filtersDisplay = "Branch: ${filter.selectedBranch} | Period: ${filter.formattedDateDisplay} | Sort By: $sortBy (Top $limit)"
    val summary = if (items.isNotEmpty()) {
      "Top service: ${items.first().serviceName} (${MetricsCalculator.formatCurrency(items.first().revenue)}, ${items.first().count} units) followed by ${items.drop(1).joinToString(", ") { "${it.serviceName} (${MetricsCalculator.formatCurrency(it.revenue)})" }}"
    } else {
      "No services found for the selected branch/period."
    }

    return AskToolExecution(
      toolName = AskToolNames.GET_TOP_SERVICES,
      arguments = args,
      filtersUsedDisplay = filtersDisplay,
      rawOutputJson = json.toString(),
      summaryOutput = summary
    )
  }

  // 3. getDoctorSplit
  private fun executeGetDoctorSplit(args: Map<String, String>): AskToolExecution {
    val branchArg = args["branch"]?.takeIf { it.isNotBlank() } ?: getGlobalFilter().selectedBranch
    val periodArg = args["period"]?.takeIf { it.isNotBlank() } ?: "today"

    val filter = buildFilterForScope(branchArg, periodArg)
    val mode = getValuationMode()
    val packageMap = getPackageValues()

    val rows = MetricsCalculator.filterRows(getRawRows(), filter)
    val doctorMetrics = MetricsCalculator.calculateRevenueByDoctor(rows, mode, packageMap)

    val doctorsArray = JSONArray()
    doctorMetrics.forEach { doc ->
      doctorsArray.put(JSONObject().apply {
        put("doctorName", doc.doctorName)
        put("revenue", doc.revenue)
        put("percentageOfTotal", String.format("%.1f%%", doc.percentage))
        put("billCount", doc.billCount)
        put("averageBill", doc.averageBill)
      })
    }

    val json = JSONObject().apply {
      put("branch", filter.selectedBranch)
      put("period", filter.formattedDateDisplay)
      put("doctorCount", doctorMetrics.size)
      put("doctors", doctorsArray)
    }

    val filtersDisplay = "Branch: ${filter.selectedBranch} | Period: ${filter.formattedDateDisplay}"
    val summary = if (doctorMetrics.isNotEmpty()) {
      doctorMetrics.joinToString("; ") {
        "${it.doctorName}: ${MetricsCalculator.formatCurrency(it.revenue)} (${String.format("%.1f%%", it.percentage)}, ${it.billCount} bills)"
      }
    } else {
      "No doctor revenue recorded for this period."
    }

    return AskToolExecution(
      toolName = AskToolNames.GET_DOCTOR_SPLIT,
      arguments = args,
      filtersUsedDisplay = filtersDisplay,
      rawOutputJson = json.toString(),
      summaryOutput = summary
    )
  }

  // 4. getPharmacyMargin
  private fun executeGetPharmacyMargin(args: Map<String, String>): AskToolExecution {
    val branchArg = args["branch"]?.takeIf { it.isNotBlank() } ?: getGlobalFilter().selectedBranch
    val periodArg = args["period"]?.takeIf { it.isNotBlank() } ?: "today"
    val query = args["searchQuery"] ?: ""

    val filter = buildFilterForScope(branchArg, periodArg)
    val allPharm = getPharmacyItems()
    val filteredPharm = MetricsCalculator.filterPharmacyItems(allPharm, filter, query)
    val kpis = MetricsCalculator.calculatePharmacyKpis(filteredPharm)
    val rankings = MetricsCalculator.calculatePharmacyRankings(filteredPharm)

    val topItemsArr = JSONArray()
    rankings.top10ByMargin.take(5).forEach { itm ->
      topItemsArr.put(JSONObject().apply {
        put("productName", itm.productName)
        put("revenue", itm.revenue)
        put("grossMargin", itm.margin)
        put("marginPercentage", String.format("%.1f%%", itm.marginPercentage))
        put("unitsSold", itm.unitsSold)
      })
    }

    val json = JSONObject().apply {
      put("branch", filter.selectedBranch)
      put("period", filter.formattedDateDisplay)
      put("totalRevenue", kpis.totalRevenue.value)
      put("totalCost", kpis.totalCost.value)
      put("grossMargin", kpis.grossMargin.value)
      put("grossMarginPercentage", String.format("%.1f%%", kpis.marginPercentage.value))
      put("totalUnitsSold", kpis.totalUnitsSold)
      put("distinctProducts", kpis.distinctProductCount)
      put("topMarginItems", topItemsArr)
    }

    val filtersDisplay = "Branch: ${filter.selectedBranch} | Period: ${filter.formattedDateDisplay}${if (query.isNotBlank()) " | Search: $query" else ""}"
    val summary = "Pharmacy Revenue: ${MetricsCalculator.formatCurrency(kpis.totalRevenue.value)}, Cost: ${MetricsCalculator.formatCurrency(kpis.totalCost.value)}, Margin: ${MetricsCalculator.formatCurrency(kpis.grossMargin.value)} (${String.format("%.1f%%", kpis.marginPercentage.value)}) across ${kpis.totalUnitsSold} units sold."

    return AskToolExecution(
      toolName = AskToolNames.GET_PHARMACY_MARGIN,
      arguments = args,
      filtersUsedDisplay = filtersDisplay,
      rawOutputJson = json.toString(),
      summaryOutput = summary
    )
  }

  // 5. comparePeriods
  private fun executeComparePeriods(args: Map<String, String>): AskToolExecution {
    val branchArg = args["branch"]?.takeIf { it.isNotBlank() } ?: getGlobalFilter().selectedBranch
    val period1Arg = args["period1"]?.takeIf { it.isNotBlank() } ?: "today"
    val period2Arg = args["period2"]?.takeIf { it.isNotBlank() } ?: "yesterday"

    val filter1 = buildFilterForScope(branchArg, period1Arg)
    val filter2 = buildFilterForScope(branchArg, period2Arg)
    val mode = getValuationMode()
    val packageMap = getPackageValues()

    val rows1 = MetricsCalculator.filterRows(getRawRows(), filter1)
    val rows2 = MetricsCalculator.filterRows(getRawRows(), filter2)

    val kpis1 = MetricsCalculator.calculateKpis(rows1, emptyList(), mode, packageMap)
    val kpis2 = MetricsCalculator.calculateKpis(rows2, emptyList(), mode, packageMap)

    val rev1 = kpis1.totalRevenue.value
    val rev2 = kpis2.totalRevenue.value
    val revDiff = rev1 - rev2
    val revPct = if (rev2 > 0) ((rev1 - rev2) / rev2) * 100.0 else 0.0

    val bills1 = kpis1.patientBillsCount.value.toInt()
    val bills2 = kpis2.patientBillsCount.value.toInt()
    val billsDiff = bills1 - bills2

    val abv1 = kpis1.averageBillValue.value
    val abv2 = kpis2.averageBillValue.value
    val abvDiff = abv1 - abv2

    val json = JSONObject().apply {
      put("branch", branchArg)
      put("period1", JSONObject().apply {
        put("name", filter1.formattedDateDisplay)
        put("revenue", rev1)
        put("bills", bills1)
        put("abv", abv1)
      })
      put("period2", JSONObject().apply {
        put("name", filter2.formattedDateDisplay)
        put("revenue", rev2)
        put("bills", bills2)
        put("abv", abv2)
      })
      put("comparison", JSONObject().apply {
        put("revenueDifference", revDiff)
        put("revenueGrowthPercentage", String.format("%+.1f%%", revPct))
        put("billsDifference", billsDiff)
        put("abvDifference", abvDiff)
      })
    }

    val filtersDisplay = "Branch: $branchArg | Period 1: ${filter1.formattedDateDisplay} vs Period 2: ${filter2.formattedDateDisplay}"
    val summary = "${filter1.formattedDateDisplay} Revenue: ${MetricsCalculator.formatCurrency(rev1)} vs ${filter2.formattedDateDisplay}: ${MetricsCalculator.formatCurrency(rev2)} (Change: ${MetricsCalculator.formatCurrency(revDiff)}, ${String.format("%+.1f%%", revPct)}). Bills: $bills1 vs $bills2 (Δ $billsDiff)."

    return AskToolExecution(
      toolName = AskToolNames.COMPARE_PERIODS,
      arguments = args,
      filtersUsedDisplay = filtersDisplay,
      rawOutputJson = json.toString(),
      summaryOutput = summary
    )
  }

  // 6. listUploads
  private fun executeListUploads(args: Map<String, String>): AskToolExecution {
    val branchArg = args["branch"]?.takeIf { it.isNotBlank() && it != "All Branches" }
    val reportTypeArg = args["reportType"]?.takeIf { it.isNotBlank() && !it.equals("all", ignoreCase = true) }

    val history = getUploadHistory()
    val filtered = history.filter { rec ->
      val bMatch = branchArg == null || rec.branch.equals(branchArg, ignoreCase = true)
      val rMatch = reportTypeArg == null ||
        rec.reportType.name.equals(reportTypeArg, ignoreCase = true) ||
        rec.reportType.displayName.equals(reportTypeArg, ignoreCase = true)
      bMatch && rMatch
    }

    val uploadsArray = JSONArray()
    filtered.forEach { up ->
      uploadsArray.put(JSONObject().apply {
        put("uploadId", up.uploadId)
        put("fileName", up.fileName)
        put("reportType", up.reportType.displayName)
        put("branch", up.branch)
        put("reportDate", up.reportDate)
        put("savedRowCount", up.savedRowCount)
        put("uploadedBy", up.uploadedBy)
      })
    }

    val json = JSONObject().apply {
      put("totalUploadsCount", filtered.size)
      put("uploads", uploadsArray)
    }

    val filtersDisplay = "Report Type: ${reportTypeArg ?: "All"} | Branch: ${branchArg ?: "All Branches"}"
    val summary = if (filtered.isNotEmpty()) {
      "Found ${filtered.size} upload records: " + filtered.take(3).joinToString(", ") { "${it.fileName} (${it.reportType.displayName}, ${it.reportDate}, ${it.savedRowCount} rows)" } + if (filtered.size > 3) " and ${filtered.size - 3} more." else ""
    } else {
      "No matching upload records found."
    }

    return AskToolExecution(
      toolName = AskToolNames.LIST_UPLOADS,
      arguments = args,
      filtersUsedDisplay = filtersDisplay,
      rawOutputJson = json.toString(),
      summaryOutput = summary
    )
  }

  private fun buildFilterForScope(branchStr: String, periodStr: String): FilterState {
    val cleanBranch = when {
      branchStr.contains("Downtown", ignoreCase = true) -> "Downtown Executive Clinic"
      branchStr.contains("Westside", ignoreCase = true) -> "Westside Wellness Pavilion"
      branchStr.contains("North", ignoreCase = true) -> "North Heights Specialty Center"
      branchStr.contains("All", ignoreCase = true) -> "All Branches"
      else -> branchStr
    }

    val preset = when (periodStr.lowercase().trim()) {
      "today", "current", "day" -> DatePreset.TODAY
      "yesterday", "prior_day", "previous_day" -> DatePreset.YESTERDAY
      "7d", "7_days", "seven_days", "week", "past_week" -> DatePreset.SEVEN_DAYS
      "30d", "30_days", "thirty_days", "month", "past_month" -> DatePreset.THIRTY_DAYS
      "this_month", "current_month", "october" -> DatePreset.THIS_MONTH
      else -> {
        // If it looks like a specific date e.g. "2026-10-04"
        if (periodStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
          return FilterState(
            datePreset = DatePreset.CUSTOM,
            customStartDate = periodStr,
            customEndDate = periodStr,
            selectedBranch = cleanBranch
          )
        }
        DatePreset.TODAY
      }
    }

    return FilterState(
      datePreset = preset,
      selectedBranch = cleanBranch
    )
  }
}
