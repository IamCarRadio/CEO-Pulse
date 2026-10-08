package com.example.util

import com.example.model.CategoryGroup
import com.example.model.CategoryMetric
import com.example.model.DailyTrendPoint
import com.example.model.DashboardKpis
import com.example.model.DatePreset
import com.example.model.DoctorRevenueMetric
import com.example.model.DominantUndersoldAnalysis
import com.example.model.DualStreamRevenue
import com.example.model.FilterState
import com.example.model.KpiMetric
import com.example.model.PharmacyItem
import com.example.model.PharmacyKpis
import com.example.model.PharmacyProductRanking
import com.example.model.PharmacyRankings
import com.example.model.PharmacyReconciliation
import com.example.model.PharmacySortOption
import com.example.model.RawClinicRow
import com.example.model.ReconciliationSummary
import com.example.model.ServiceItemMetric
import com.example.model.ServiceRankings
import com.example.model.UnmappedPackageService
import com.example.model.ValuationMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * PURE-FUNCTION MODULE (metrics.js in Kotlin)
 * All business and mathematical calculations live exclusively here.
 * Zero UI dependencies. All functions are pure, deterministic, and testable.
 */
object MetricsCalculator {

  private val numberFormat = DecimalFormat("#,##0.00")
  private val countFormat = DecimalFormat("#,##0")
  private val percentFormat = DecimalFormat("+0.0%;-0.0%")

  fun formatCurrency(amount: Double): String = "Rs. " + numberFormat.format(amount)

  /**
   * Filter raw rows based on active FilterState
   */
  fun filterRows(rows: List<RawClinicRow>, filter: FilterState): List<RawClinicRow> {
    return rows.filter { row ->
      // 1. Branch filter
      val branchMatch = filter.selectedBranch == "All Branches" ||
        row.branch.equals(filter.selectedBranch, ignoreCase = true)

      // 2. Doctor filter
      val doctorMatch = filter.selectedDoctor == "All Doctors" ||
        row.doctor.contains(filter.selectedDoctor.substringBefore(","), ignoreCase = true)

      // 3. Category filter
      val categoryMatch = filter.selectedCategory == "All Categories" ||
        row.category.equals(filter.selectedCategory, ignoreCase = true) ||
        getCategoryGroup(row).displayName.equals(filter.selectedCategory, ignoreCase = true)

      // 4. Date filter
      val dateMatch = isDateInFilterScope(row.reportDate, filter)

      branchMatch && doctorMatch && categoryMatch && dateMatch
    }
  }

  /**
   * Determines rows that fall into the immediately preceding comparison period
   */
  fun getPreviousPeriodRows(allRows: List<RawClinicRow>, filter: FilterState): List<RawClinicRow> {
    return allRows.filter { row ->
      val branchMatch = filter.selectedBranch == "All Branches" ||
        row.branch.equals(filter.selectedBranch, ignoreCase = true)

      val doctorMatch = filter.selectedDoctor == "All Doctors" ||
        row.doctor.contains(filter.selectedDoctor.substringBefore(","), ignoreCase = true)

      val categoryMatch = filter.selectedCategory == "All Categories" ||
        row.category.equals(filter.selectedCategory, ignoreCase = true)

      val isPrevDate = isDateInPreviousPeriodScope(row.reportDate, filter)

      branchMatch && doctorMatch && categoryMatch && isPrevDate
    }
  }

  // ------------------------------------------------------------------
  // PACKAGE RECTIFICATION PURE FUNCTIONS
  // ------------------------------------------------------------------

  /**
   * Detects all Rs. 0 rows and flags them as Package/Enrolled sessions
   */
  fun detectPackageRows(rows: List<RawClinicRow>): List<RawClinicRow> {
    return rows.filter { it.isPackageSession }
  }

  /**
   * Identifies Rs. 0 package sessions that have NO defined per-session value in packageValues,
   * so nothing is silently valued at zero!
   */
  fun findUnmappedPackageServices(
    rows: List<RawClinicRow>,
    packageValues: Map<String, Double>
  ): List<UnmappedPackageService> {
    val packageRows = detectPackageRows(rows)
    val unmappedRows = packageRows.filter { row ->
      val definedVal = packageValues[row.serviceName.trim()]
      definedVal == null || definedVal <= 0.0
    }

    return unmappedRows.groupBy { it.serviceName.trim() }.map { (serviceName, sRows) ->
      UnmappedPackageService(
        serviceName = serviceName,
        category = sRows.firstOrNull()?.category ?: "Clinical",
        sessionCount = sRows.sumOf { it.quantity },
        lastSeenDate = sRows.maxOfOrNull { it.reportDate } ?: "",
        samplePatientId = sRows.firstOrNull()?.columns?.get("PatientId") ?: "",
        affectedRows = sRows
      )
    }.sortedByDescending { it.sessionCount }
  }

  /**
   * Computes the exact reconciliation line:
   * Collected Revenue + Package Rectified Value = Service-Value Total
   */
  fun calculateReconciliation(
    rows: List<RawClinicRow>,
    packageValues: Map<String, Double>
  ): ReconciliationSummary {
    val collectedRev = rows.sumOf { it.amount }
    val packageRows = detectPackageRows(rows)
    val packageValue = packageRows.sumOf { row ->
      packageValues[row.serviceName.trim()] ?: 0.0
    }
    val serviceValueTotal = collectedRev + packageValue
    val unmapped = findUnmappedPackageServices(rows, packageValues)

    val totalSessions = rows.sumOf { it.quantity }
    val packageSessions = packageRows.sumOf { it.quantity }
    val paidSessions = totalSessions - packageSessions

    return ReconciliationSummary(
      collectedRevenue = collectedRev,
      packageRectifiedValue = packageValue,
      serviceValueTotal = serviceValueTotal,
      totalSessionCount = totalSessions,
      paidSessionCount = paidSessions,
      packageSessionCount = packageSessions,
      unmappedServiceCount = unmapped.size,
      unmappedSessionCount = unmapped.sumOf { it.sessionCount },
      unmappedServices = unmapped,
      packageRows = packageRows
    )
  }

  // ------------------------------------------------------------------
  // DASHBOARD KPIS & CALCULATIONS (Responds to ValuationMode)
  // ------------------------------------------------------------------

  /**
   * KPI calculation pure function: Total Revenue, Patients/Bills, Average Bill Value
   */
  fun calculateKpis(
    currentRows: List<RawClinicRow>,
    previousRows: List<RawClinicRow>,
    valuationMode: ValuationMode = ValuationMode.COLLECTED,
    packageValues: Map<String, Double> = emptyMap()
  ): DashboardKpis {
    val currentRevenue = currentRows.sumOf { it.getEffectiveAmount(valuationMode, packageValues) }
    val previousRevenue = previousRows.sumOf { it.getEffectiveAmount(valuationMode, packageValues) }

    // Unique bills count
    val currentBillsCount = currentRows.map { it.invoiceNo }.distinct().size.coerceAtLeast(if (currentRows.isNotEmpty()) 1 else 0)
    val previousBillsCount = previousRows.map { it.invoiceNo }.distinct().size.coerceAtLeast(if (previousRows.isNotEmpty()) 1 else 0)

    val currentAbv = if (currentBillsCount > 0) currentRevenue / currentBillsCount else 0.0
    val previousAbv = if (previousBillsCount > 0) previousRevenue / previousBillsCount else 0.0

    val totalProcedureUnits = currentRows.sumOf { it.quantity }

    return DashboardKpis(
      totalRevenue = createKpiMetric(
        current = currentRevenue,
        previous = previousRevenue,
        formatter = { formatCurrency(it) }
      ),
      patientBillsCount = createKpiMetric(
        current = currentBillsCount.toDouble(),
        previous = previousBillsCount.toDouble(),
        formatter = { countFormat.format(it.toLong()) }
      ),
      averageBillValue = createKpiMetric(
        current = currentAbv,
        previous = previousAbv,
        formatter = { formatCurrency(it) }
      ),
      totalProcedureUnits = totalProcedureUnits
    )
  }

  private fun createKpiMetric(
    current: Double,
    previous: Double,
    formatter: (Double) -> String
  ): KpiMetric {
    val deltaPercent = if (previous > 0.0) {
      ((current - previous) / previous) * 100.0
    } else if (current > 0.0) {
      100.0
    } else {
      0.0
    }

    return KpiMetric(
      value = current,
      formattedValue = formatter(current),
      previousValue = previous,
      percentageChange = deltaPercent,
      isPositive = deltaPercent >= 0.05,
      isNeutral = kotlin.math.abs(deltaPercent) < 0.05
    )
  }

  /**
   * Daily revenue trend aggregation
   */
  fun calculateDailyRevenueTrend(
    rows: List<RawClinicRow>,
    valuationMode: ValuationMode = ValuationMode.COLLECTED,
    packageValues: Map<String, Double> = emptyMap()
  ): List<DailyTrendPoint> {
    if (rows.isEmpty()) return emptyList()

    val displayDateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val groupedByDate = rows.groupBy { it.reportDate }
    val sortedDates = groupedByDate.keys.sorted()

    return sortedDates.map { dateStr ->
      val dateRows = groupedByDate[dateStr].orEmpty()
      val rev = dateRows.sumOf { it.getEffectiveAmount(valuationMode, packageValues) }
      val bills = dateRows.map { it.invoiceNo }.distinct().size

      val displayDate = try {
        val parsed = isoDateFormatter.parse(dateStr)
        if (parsed != null) displayDateFormatter.format(parsed) else dateStr
      } catch (e: Exception) {
        dateStr
      }

      DailyTrendPoint(
        date = dateStr,
        displayDate = displayDate,
        revenue = rev,
        billsCount = bills
      )
    }
  }

  /**
   * Top 10 and Bottom 10 services by revenue and by count
   */
  fun calculateTopAndBottomServices(
    rows: List<RawClinicRow>,
    valuationMode: ValuationMode = ValuationMode.COLLECTED,
    packageValues: Map<String, Double> = emptyMap()
  ): ServiceRankings {
    if (rows.isEmpty()) {
      return ServiceRankings(emptyList(), emptyList(), emptyList(), emptyList())
    }

    val grouped = rows.groupBy { it.serviceName.trim() }
    val metricsList = grouped.map { (name, serviceRows) ->
      val rev = serviceRows.sumOf { it.getEffectiveAmount(valuationMode, packageValues) }
      val totalCount = serviceRows.sumOf { it.quantity }
      val avgPrice = if (totalCount > 0) rev / totalCount else 0.0
      val category = serviceRows.firstOrNull()?.category ?: "Clinical"

      ServiceItemMetric(
        serviceName = name,
        category = category,
        revenue = rev,
        count = totalCount,
        averagePrice = avgPrice
      )
    }

    val byRevenue = metricsList.sortedByDescending { it.revenue }
    val byCount = metricsList.sortedByDescending { it.count }

    return ServiceRankings(
      top10ByRevenue = byRevenue.take(10),
      bottom10ByRevenue = byRevenue.takeLast(10).reversed(),
      top10ByCount = byCount.take(10),
      bottom10ByCount = byCount.takeLast(10).reversed()
    )
  }

  /**
   * Category split (Skin / Obesity / Hair / Other) pure calculation
   */
  fun calculateCategorySplit(
    rows: List<RawClinicRow>,
    valuationMode: ValuationMode = ValuationMode.COLLECTED,
    packageValues: Map<String, Double> = emptyMap()
  ): List<CategoryMetric> {
    val totalRevenue = rows.sumOf { it.getEffectiveAmount(valuationMode, packageValues) }

    // Map each row into one of the 4 required buckets
    val grouped = rows.groupBy { getCategoryGroup(it) }

    return CategoryGroup.values().map { cat ->
      val catRows = grouped[cat].orEmpty()
      val rev = catRows.sumOf { it.getEffectiveAmount(valuationMode, packageValues) }
      val count = catRows.sumOf { it.quantity }
      val percentage = if (totalRevenue > 0) (rev / totalRevenue) * 100.0 else 0.0

      CategoryMetric(
        category = cat,
        revenue = rev,
        percentage = percentage,
        count = count
      )
    }
  }

  /**
   * Classify any clinic row into Skin, Obesity, Hair, or Other
   */
  fun getCategoryGroup(row: RawClinicRow): CategoryGroup {
    val combinedText = "${row.category} ${row.serviceName}".lowercase(Locale.getDefault())

    return when {
      combinedText.contains("skin") || combinedText.contains("derma") ||
        combinedText.contains("facial") || combinedText.contains("acne") ||
        combinedText.contains("laser") || combinedText.contains("peel") ||
        combinedText.contains("botox") || combinedText.contains("filler") ||
        combinedText.contains("cryo") || combinedText.contains("biopsy") -> CategoryGroup.SKIN

      combinedText.contains("obesity") || combinedText.contains("weight") ||
        combinedText.contains("bariatric") || combinedText.contains("glp") ||
        combinedText.contains("semaglutide") || combinedText.contains("metabolic") ||
        combinedText.contains("nutrition") || combinedText.contains("body contour") -> CategoryGroup.OBESITY

      combinedText.contains("hair") || combinedText.contains("prp") ||
        combinedText.contains("trichology") || combinedText.contains("scalp") ||
        combinedText.contains("follic") || combinedText.contains("alopecia") -> CategoryGroup.HAIR

      else -> CategoryGroup.OTHER
    }
  }

  /**
   * Revenue by Doctor aggregation
   */
  fun calculateRevenueByDoctor(
    rows: List<RawClinicRow>,
    valuationMode: ValuationMode = ValuationMode.COLLECTED,
    packageValues: Map<String, Double> = emptyMap()
  ): List<DoctorRevenueMetric> {
    if (rows.isEmpty()) return emptyList()

    val totalRev = rows.sumOf { it.getEffectiveAmount(valuationMode, packageValues) }
    val grouped = rows.groupBy { it.doctor.trim() }

    return grouped.map { (doctor, dRows) ->
      val rev = dRows.sumOf { it.getEffectiveAmount(valuationMode, packageValues) }
      val bills = dRows.map { it.invoiceNo }.distinct().size
      val pct = if (totalRev > 0) (rev / totalRev) * 100.0 else 0.0
      val avg = if (bills > 0) rev / bills else 0.0

      DoctorRevenueMetric(
        doctorName = doctor,
        revenue = rev,
        percentage = pct,
        billCount = bills,
        averageBill = avg
      )
    }.sortedByDescending { it.revenue }
  }

  /**
   * Dominant vs Under-Sold Service Lines analysis
   */
  fun calculateDominantVsUndersold(
    rows: List<RawClinicRow>,
    valuationMode: ValuationMode = ValuationMode.COLLECTED,
    packageValues: Map<String, Double> = emptyMap()
  ): DominantUndersoldAnalysis {
    if (rows.isEmpty()) {
      return DominantUndersoldAnalysis(emptyList(), emptyList())
    }

    val totalGroupRev = rows.sumOf { it.getEffectiveAmount(valuationMode, packageValues) }
    val rankings = calculateTopAndBottomServices(rows, valuationMode, packageValues).top10ByRevenue

    // Dominant: Top revenue contributors accounting for significant volume or revenue
    val dominant = rankings.filter { it.revenue >= totalGroupRev * 0.10 || rankings.indexOf(it) < 3 }

    // Under-sold: High target procedure price (>= Rs. 140) but lower transaction volume (<= 3 count)
    val grouped = rows.groupBy { it.serviceName.trim() }
    val undersold = grouped.mapNotNull { (name, sRows) ->
      val rev = sRows.sumOf { it.getEffectiveAmount(valuationMode, packageValues) }
      val count = sRows.sumOf { it.quantity }
      val avgPrice = if (count > 0) rev / count else 0.0

      if (avgPrice >= 140.0 && count <= 3 && !dominant.any { it.serviceName == name }) {
        ServiceItemMetric(
          serviceName = name,
          category = sRows.firstOrNull()?.category ?: "Clinical",
          revenue = rev,
          count = count,
          averagePrice = avgPrice
        )
      } else null
    }.sortedByDescending { it.averagePrice }.take(5)

    return DominantUndersoldAnalysis(
      dominantServices = dominant.take(4),
      underSoldServices = undersold
    )
  }

  private fun isDateInFilterScope(dateStr: String, filter: FilterState): Boolean {
    return when (filter.datePreset) {
      DatePreset.TODAY -> dateStr == "2026-10-04"
      DatePreset.YESTERDAY -> dateStr == "2026-10-03"
      DatePreset.SEVEN_DAYS -> dateStr in "2026-09-28".."2026-10-04"
      DatePreset.THIRTY_DAYS -> dateStr in "2026-09-04".."2026-10-04"
      DatePreset.THIS_MONTH -> dateStr.startsWith("2026-10")
      DatePreset.CUSTOM -> dateStr in filter.customStartDate..filter.customEndDate
    }
  }

  private fun isDateInPreviousPeriodScope(dateStr: String, filter: FilterState): Boolean {
    return when (filter.datePreset) {
      DatePreset.TODAY -> dateStr == "2026-10-03" // Yesterday as previous period
      DatePreset.YESTERDAY -> dateStr == "2026-10-02"
      DatePreset.SEVEN_DAYS -> dateStr in "2026-09-21".."2026-09-27"
      DatePreset.THIRTY_DAYS -> dateStr in "2026-08-05".."2026-09-03"
      DatePreset.THIS_MONTH -> dateStr.startsWith("2026-09") // September 2026
      DatePreset.CUSTOM -> {
        // Compare against prior month window
        dateStr in "2026-09-01".."2026-09-30"
      }
    }
  }

  // ------------------------------------------------------------------
  // PHARMACY ITEM-WISE MARGIN PURE FUNCTIONS
  // ------------------------------------------------------------------

  fun filterPharmacyItems(
    items: List<PharmacyItem>,
    filter: FilterState,
    searchQuery: String = "",
    sortOption: PharmacySortOption = PharmacySortOption.REVENUE_DESC
  ): List<PharmacyItem> {
    val filtered = items.filter { item ->
      val branchMatch = filter.selectedBranch == "All Branches" ||
        item.branch.equals(filter.selectedBranch, ignoreCase = true)

      val categoryMatch = filter.selectedCategory == "All Categories" ||
        item.category.equals(filter.selectedCategory, ignoreCase = true)

      val dateMatch = isDateInFilterScope(item.reportDate, filter)

      val searchMatch = searchQuery.isBlank() ||
        item.productName.contains(searchQuery, ignoreCase = true) ||
        item.productCode.contains(searchQuery, ignoreCase = true) ||
        item.category.contains(searchQuery, ignoreCase = true)

      branchMatch && categoryMatch && dateMatch && searchMatch
    }

    return when (sortOption) {
      PharmacySortOption.REVENUE_DESC -> filtered.sortedByDescending { it.totalRevenue }
      PharmacySortOption.MARGIN_DESC -> filtered.sortedByDescending { it.grossMargin }
      PharmacySortOption.MARGIN_PCT_DESC -> filtered.sortedByDescending { it.marginPercentage }
      PharmacySortOption.QUANTITY_DESC -> filtered.sortedByDescending { it.quantity }
      PharmacySortOption.NAME_ASC -> filtered.sortedBy { it.productName.lowercase(Locale.getDefault()) }
    }
  }

  fun getPreviousPeriodPharmacyItems(allItems: List<PharmacyItem>, filter: FilterState): List<PharmacyItem> {
    return allItems.filter { item ->
      val branchMatch = filter.selectedBranch == "All Branches" ||
        item.branch.equals(filter.selectedBranch, ignoreCase = true)

      val categoryMatch = filter.selectedCategory == "All Categories" ||
        item.category.equals(filter.selectedCategory, ignoreCase = true)

      val isPrevDate = isDateInPreviousPeriodScope(item.reportDate, filter)

      branchMatch && categoryMatch && isPrevDate
    }
  }

  fun calculatePharmacyKpis(
    currentItems: List<PharmacyItem>,
    previousItems: List<PharmacyItem> = emptyList()
  ): PharmacyKpis {
    val currentRevenue = currentItems.sumOf { it.totalRevenue }
    val previousRevenue = previousItems.sumOf { it.totalRevenue }

    val currentCost = currentItems.sumOf { it.totalCost }
    val previousCost = previousItems.sumOf { it.totalCost }

    val currentMargin = currentItems.sumOf { it.grossMargin }
    val previousMargin = previousItems.sumOf { it.grossMargin }

    val currentMarginPct = if (currentRevenue > 0.0) (currentMargin / currentRevenue) * 100.0 else 0.0
    val previousMarginPct = if (previousRevenue > 0.0) (previousMargin / previousRevenue) * 100.0 else 0.0

    val totalUnits = currentItems.sumOf { it.quantity }
    val distinctSkus = currentItems.map { it.productCode }.distinct().size

    val pctFormatter = DecimalFormat("#,##0.0'%'")

    return PharmacyKpis(
      totalRevenue = createKpiMetric(
        current = currentRevenue,
        previous = previousRevenue,
        formatter = { formatCurrency(it) }
      ),
      totalCost = createKpiMetric(
        current = currentCost,
        previous = previousCost,
        formatter = { formatCurrency(it) }
      ),
      grossMargin = createKpiMetric(
        current = currentMargin,
        previous = previousMargin,
        formatter = { formatCurrency(it) }
      ),
      marginPercentage = createKpiMetric(
        current = currentMarginPct,
        previous = previousMarginPct,
        formatter = { pctFormatter.format(it) }
      ),
      totalUnitsSold = totalUnits,
      distinctProductCount = distinctSkus
    )
  }

  fun calculatePharmacyRankings(items: List<PharmacyItem>): PharmacyRankings {
    if (items.isEmpty()) {
      return PharmacyRankings(emptyList(), emptyList(), emptyList())
    }

    val grouped = items.groupBy { it.productName.trim() }
    val rankingsList = grouped.map { (name, pItems) ->
      val rev = pItems.sumOf { it.totalRevenue }
      val cost = pItems.sumOf { it.totalCost }
      val margin = pItems.sumOf { it.grossMargin }
      val units = pItems.sumOf { it.quantity }
      val marginPct = if (rev > 0.0) (margin / rev) * 100.0 else 0.0
      val avgPrice = if (units > 0) rev / units else 0.0
      val firstItem = pItems.first()

      PharmacyProductRanking(
        productCode = firstItem.productCode,
        productName = name,
        category = firstItem.category,
        unitsSold = units,
        revenue = rev,
        cost = cost,
        margin = margin,
        marginPercentage = marginPct,
        averageSellingPrice = avgPrice
      )
    }

    val byRevenue = rankingsList.sortedByDescending { it.revenue }
    val byMargin = rankingsList.sortedByDescending { it.margin }
    val bottom = rankingsList.sortedBy { it.margin }.take(10)

    return PharmacyRankings(
      top10ByRevenue = byRevenue.take(10),
      top10ByMargin = byMargin.take(10),
      bottomPerformers = bottom
    )
  }

  fun calculatePharmacyReconciliation(items: List<PharmacyItem>): PharmacyReconciliation {
    var fileRevenue = 0.0
    var fileCost = 0.0
    var fileMargin = 0.0

    items.forEach { item ->
      val revRaw = item.columns["TotalRevenue"] ?: item.columns["NetSales"] ?: item.columns["NetTotal"] ?: item.columns["Amount"] ?: "0"
      val costRaw = item.columns["TotalCost"] ?: item.columns["Cost"] ?: "0"
      val marginRaw = item.columns["GrossMargin"] ?: item.columns["Margin"] ?: "0"

      fileRevenue += revRaw.replace("$", "").replace("Rs.", "").replace("Rs", "").replace(",", "").trim().toDoubleOrNull() ?: item.totalRevenue
      fileCost += costRaw.replace("$", "").replace("Rs.", "").replace("Rs", "").replace(",", "").trim().toDoubleOrNull() ?: item.totalCost
      fileMargin += marginRaw.replace("$", "").replace("Rs.", "").replace("Rs", "").replace(",", "").trim().toDoubleOrNull() ?: item.grossMargin
    }

    val appRevenue = items.sumOf { it.totalRevenue }
    val appCost = items.sumOf { it.totalCost }
    val appMargin = items.sumOf { it.grossMargin }

    val revDiff = kotlin.math.abs(fileRevenue - appRevenue)
    val costDiff = kotlin.math.abs(fileCost - appCost)
    val marginDiff = kotlin.math.abs(fileMargin - appMargin)

    return PharmacyReconciliation(
      fileTotalRevenue = fileRevenue,
      fileTotalCost = fileCost,
      fileTotalMargin = fileMargin,
      appTotalRevenue = appRevenue,
      appTotalCost = appCost,
      appTotalMargin = appMargin,
      isRevenueMatched = revDiff < 0.01,
      isCostMatched = costDiff < 0.01,
      isMarginMatched = marginDiff < 0.01,
      totalItemsAudited = items.size,
      revenueVariance = revDiff,
      costVariance = costDiff,
      marginVariance = marginDiff
    )
  }

  fun calculateDualStreamRevenue(
    serviceRows: List<RawClinicRow>,
    pharmacyItems: List<PharmacyItem>,
    valuationMode: ValuationMode = ValuationMode.COLLECTED,
    packageValues: Map<String, Double> = emptyMap()
  ): DualStreamRevenue {
    val serviceRev = serviceRows.sumOf { it.getEffectiveAmount(valuationMode, packageValues) }
    val pharmRev = pharmacyItems.sumOf { it.totalRevenue }
    val total = serviceRev + pharmRev

    val serviceShare = if (total > 0.0) (serviceRev / total) * 100.0 else 0.0
    val pharmShare = if (total > 0.0) (pharmRev / total) * 100.0 else 0.0

    val serviceBills = serviceRows.map { it.invoiceNo }.distinct().size
    val pharmUnits = pharmacyItems.sumOf { it.quantity }

    return DualStreamRevenue(
      clinicalServiceRevenue = serviceRev,
      pharmacyRevenue = pharmRev,
      totalCombinedRevenue = total,
      clinicalSharePercentage = serviceShare,
      pharmacySharePercentage = pharmShare,
      clinicalBillCount = serviceBills,
      pharmacyUnitsSold = pharmUnits
    )
  }
}
