package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AiSummaryRepository
import com.example.data.GeminiSummaryService
import com.example.model.AiSummaryData
import com.example.model.CategoryGroup
import com.example.model.CategoryMetric
import com.example.model.DailyTrendPoint
import com.example.model.DashboardKpis
import com.example.model.DoctorRevenueMetric
import com.example.model.DualStreamRevenue
import com.example.model.FilterState
import com.example.model.KpiMetric
import com.example.model.ReconciliationSummary
import com.example.model.ServiceItemMetric
import com.example.model.ServiceRankings
import com.example.model.ValuationMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiSummaryTest {

  private lateinit var context: Context
  private lateinit var repository: AiSummaryRepository
  private lateinit var service: GeminiSummaryService

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    repository = AiSummaryRepository(context)
    repository.clearAllCache()
    service = GeminiSummaryService()
  }

  private fun createSampleKpis(revCurrent: Double, revPrior: Double): DashboardKpis {
    val pct = if (revPrior > 0) ((revCurrent - revPrior) / revPrior) * 100.0 else 0.0
    return DashboardKpis(
      totalRevenue = KpiMetric(
        value = revCurrent,
        formattedValue = "Rs. 2,175,000.00",
        previousValue = revPrior,
        percentageChange = pct,
        isPositive = pct >= 0,
        isNeutral = false
      ),
      patientBillsCount = KpiMetric(
        value = 142.0,
        formattedValue = "142",
        previousValue = 135.0,
        percentageChange = 5.2,
        isPositive = true,
        isNeutral = false
      ),
      averageBillValue = KpiMetric(
        value = 15316.90,
        formattedValue = "Rs. 15,316.90",
        previousValue = 14666.67,
        percentageChange = 4.4,
        isPositive = true,
        isNeutral = false
      ),
      totalProcedureUnits = 188
    )
  }

  private fun createSampleRankings(): ServiceRankings {
    val top = listOf(
      ServiceItemMetric("Laser Resurfacing", "Skin", 620000.0, 34, 18235.29),
      ServiceItemMetric("PRP Hair Therapy", "Hair", 450000.0, 20, 22500.0)
    )
    val bottom = listOf(
      ServiceItemMetric("General Follow-up", "Consultation", 15000.0, 10, 1500.0)
    )
    return ServiceRankings(top, bottom, top, bottom)
  }

  private fun createSampleCategories(): List<CategoryMetric> {
    return listOf(
      CategoryMetric(CategoryGroup.SKIN, 1120000.0, 51.5, 78),
      CategoryMetric(CategoryGroup.HAIR, 450000.0, 20.7, 20)
    )
  }

  private fun createSampleDoctors(): List<DoctorRevenueMetric> {
    return listOf(
      DoctorRevenueMetric("Dr. Sarah Jenkins", 840000.0, 38.6, 42, 20000.0)
    )
  }

  private fun createSampleDualStream(): DualStreamRevenue {
    return DualStreamRevenue(
      clinicalServiceRevenue = 2175000.0,
      pharmacyRevenue = 485250.0,
      totalCombinedRevenue = 2660250.0,
      clinicalSharePercentage = 81.8,
      pharmacySharePercentage = 18.2,
      clinicalBillCount = 142,
      pharmacyUnitsSold = 310
    )
  }

  private fun createSampleReconciliation(): ReconciliationSummary {
    return ReconciliationSummary(
      collectedRevenue = 2175000.0,
      packageRectifiedValue = 138500.0,
      serviceValueTotal = 2313500.0,
      totalSessionCount = 170,
      paidSessionCount = 142,
      packageSessionCount = 28,
      unmappedServiceCount = 0,
      unmappedSessionCount = 0
    )
  }

  @Test
  fun testPromptContainsOnlyPreComputedMetricsAndNoRawRowsOrPatientNames() {
    val filter = FilterState()
    val kpis = createSampleKpis(2175000.0, 1980000.0)
    val prompt = service.buildMetricsOnlyPrompt(
      filterState = filter,
      valuationMode = ValuationMode.SERVICE_VALUE,
      kpis = kpis,
      dailyTrend = emptyList(),
      serviceRankings = createSampleRankings(),
      categorySplit = createSampleCategories(),
      doctorRevenue = createSampleDoctors(),
      dualStreamRevenue = createSampleDualStream(),
      reconciliationSummary = createSampleReconciliation()
    )

    // Verify it contains computed totals
    assertTrue("Should include total revenue", prompt.contains("Rs. 2,175,000.00"))
    assertTrue("Should include patient bills count", prompt.contains("142"))
    assertTrue("Should include average bill value", prompt.contains("Rs. 15,316.90"))
    assertTrue("Should include category breakdown", prompt.contains("Skin"))
    assertTrue("Should include doctor breakdown", prompt.contains("Dr. Sarah Jenkins"))
    assertTrue("Should include dual-stream turnover", prompt.contains("485,250.00"))
    assertTrue("Should include package rectified total", prompt.contains("2,313,500.00"))

    // CRITICAL PRIVACY & SECURITY AUDIT:
    // Never send raw rows, never patient names or invoice identifiers
    assertFalse("Prompt must NEVER contain patient identifier keyword", prompt.contains("PatientName"))
    assertFalse("Prompt must NEVER contain PatientId keyword", prompt.contains("PatientId"))
    assertFalse("Prompt must NEVER contain raw invoice list", prompt.contains("INV-"))
    assertFalse("Prompt must NEVER contain raw BillNo list", prompt.contains("BillNo"))
  }

  @Test
  fun testDeterministicFallbackRulesCompliance() {
    val filter = FilterState()
    val kpis = createSampleKpis(2175000.0, 1980000.0)
    val summary = service.generateCompliantFallback(
      cacheKey = "test_key",
      filterState = filter,
      valuationMode = ValuationMode.SERVICE_VALUE,
      kpis = kpis,
      serviceRankings = createSampleRankings(),
      categorySplit = createSampleCategories(),
      doctorRevenue = createSampleDoctors(),
      dualStreamRevenue = createSampleDualStream(),
      reconciliationSummary = createSampleReconciliation()
    )

    // Verify exactly 5 lines in the CEO summary
    assertEquals("Must have exactly 5 lines in CEO summary", 5, summary.fiveLineSummary.size)

    // Verify all 5 lines quote supplied values
    assertTrue("Line 1 must quote revenue", summary.fiveLineSummary[0].contains("Rs. 2,175,000.00"))
    assertTrue("Line 2 must quote bills", summary.fiveLineSummary[1].contains("142"))
    assertTrue("Line 3 must quote average bill value", summary.fiveLineSummary[2].contains("Rs. 15,316.90"))
    assertTrue("Line 4 must quote top specialty", summary.fiveLineSummary[3].contains("Skin"))
    assertTrue("Line 5 must quote service-value recognition", summary.fiveLineSummary[4].contains("2,313,500.00"))

    // Verify What Improved, What Dropped, and Why
    assertTrue("What Improved should mention revenue growth", summary.whatImproved.contains("2,175,000.00"))
    assertNotNull("Why analysis must be present", summary.whyAnalysis)
    assertTrue("Why analysis must cite drivers", summary.whyAnalysis.contains("Laser Resurfacing") || summary.whyAnalysis.contains("Skin"))

    // Verify exactly 3 suggested actions
    assertEquals("Must have exactly 3 suggested actions", 3, summary.suggestedActions.size)
  }

  @Test
  fun testMissingDataIsExplicitlyStated() {
    val filter = FilterState()
    // Prior revenue is 0.0 -> missing prior period baseline
    val kpis = createSampleKpis(500000.0, 0.0)
    val summary = service.generateCompliantFallback(
      cacheKey = "missing_data_key",
      filterState = filter,
      valuationMode = ValuationMode.COLLECTED,
      kpis = kpis,
      serviceRankings = ServiceRankings(emptyList(), emptyList(), emptyList(), emptyList()),
      categorySplit = emptyList(),
      doctorRevenue = emptyList(),
      dualStreamRevenue = DualStreamRevenue(500000.0, 0.0, 500000.0, 100.0, 0.0, 20, 0),
      reconciliationSummary = ReconciliationSummary(500000.0, 0.0, 500000.0, 20, 20, 0, 0, 0)
    )

    // The summary must explicitly state when data is missing
    val allText = summary.fiveLineSummary.joinToString(" ") + " " + summary.whatImproved + " " + summary.whatDropped
    assertTrue(
      "Must explicitly state missing prior period baseline",
      allText.contains("Missing prior period baseline") || allText.contains("missing")
    )
  }

  @Test
  fun testCacheSummaryPerDateDoesNotRegenerateOnEveryOpen() {
    val period = "Past 7 Days (Sep 27 - Oct 3)"
    val mode = ValuationMode.COLLECTED
    val branch = "Downtown Executive Clinic"
    val cacheKey = repository.buildCacheKey(period, mode, branch)

    // Initially no cache
    assertNull("Cache must be empty initially", repository.getCachedSummary(cacheKey))

    val summaryData = AiSummaryData(
      cacheKey = cacheKey,
      formattedPeriod = period,
      valuationMode = mode,
      branchName = branch,
      generatedAtMillis = 1728000000000L,
      fiveLineSummary = listOf("Line 1", "Line 2", "Line 3", "Line 4", "Line 5"),
      whatImproved = "Revenue rose by +9.8%",
      whatDropped = "No drops detected",
      whyAnalysis = "Anchored by laser resurfacing",
      suggestedActions = listOf("Action A", "Action B", "Action C"),
      rawResponseText = "Raw AI response",
      isCached = false,
      isLiveGeminiCall = true
    )

    // Save to cache
    repository.saveSummary(summaryData)

    // Subsequent retrieval
    val cached = repository.getCachedSummary(cacheKey)
    assertNotNull("Cached summary must be present", cached)
    assertTrue("Retrieved object must have isCached = true", cached!!.isCached)
    assertEquals("Period must match", period, cached.formattedPeriod)
    assertEquals("5 lines must be preserved", 5, cached.fiveLineSummary.size)
    assertEquals("Line 1 preserved", "Line 1", cached.fiveLineSummary[0])
    assertEquals("What improved preserved", "Revenue rose by +9.8%", cached.whatImproved)
    assertEquals("3 actions preserved", 3, cached.suggestedActions.size)
    assertEquals("Action A preserved", "Action A", cached.suggestedActions[0])

    // Clearing cache for regeneration
    repository.clearCacheForDate(cacheKey)
    assertNull("Cache must be empty after clearing for that date", repository.getCachedSummary(cacheKey))
  }

  @Test
  fun testParsingGeminiStructuredResponse() {
    val rawGeminiOutput = """
      SECTION 1: 5-LINE CEO SUMMARY
      1. Total clinic turnover reached Rs. 2,175,000.00 representing positive top-line traction.
      2. Clinical throughput recorded 142 patient bills with 188 procedure units fulfilled.
      3. Average billing yield held solid at Rs. 15,316.90 per completed invoice.
      4. Skin & Dermatology specialty anchored growth contributing Rs. 1,120,000.00 (51.5% share).
      5. Multi-stream pharmacy sales delivered Rs. 485,250.00, capturing 18.2% of combined revenue.

      SECTION 2: WHAT IMPROVED, WHAT DROPPED, AND WHY
      - What Improved: Total revenue increased by +9.8% to Rs. 2,175,000.00 and patient bills expanded by +5.2% to 142.
      - What Dropped: No major declines recorded across core service lines; comparison data for follow-ups is missing.
      - Why: High patient demand for Full-Face Laser Genesis Resurfacing (34 units) and strong physician throughput by Dr. Sarah Jenkins (Rs. 840,000.00).

      SECTION 3: 3 SUGGESTED ACTIONS
      1. Expand booking slots for Laser Genesis Resurfacing to capitalize on high volume demand.
      2. Scale pharmacy cross-dispensing protocols to elevate pharmacy revenue beyond the current 18.2% share.
      3. Review clinical pricing for bottom procedures generating under Rs. 20,000.00.
    """.trimIndent()

    val parsed = service.parseGeminiResponse(
      rawText = rawGeminiOutput,
      cacheKey = "test_key",
      filterState = FilterState(),
      valuationMode = ValuationMode.COLLECTED,
      isLiveGemini = true
    )

    assertEquals("Must parse 5 lines", 5, parsed.fiveLineSummary.size)
    assertTrue("Line 1 contains turnover", parsed.fiveLineSummary[0].contains("Rs. 2,175,000.00"))
    assertTrue("What Improved contains +9.8%", parsed.whatImproved.contains("+9.8%"))
    assertTrue("Why contains Laser Genesis", parsed.whyAnalysis.contains("Laser Genesis"))
    assertEquals("Must parse 3 actions", 3, parsed.suggestedActions.size)
    assertTrue("Action 1 contains Laser", parsed.suggestedActions[0].contains("Laser"))
    assertTrue("isLiveGeminiCall must be true", parsed.isLiveGeminiCall)
  }
}
