package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.UploadRepository
import com.example.model.CategoryGroup
import com.example.model.DatePreset
import com.example.model.FilterState
import com.example.model.RawClinicRow
import com.example.util.MetricsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DashboardMetricsAuditTest {

  private lateinit var context: Context
  private lateinit var uploadRepository: UploadRepository
  private lateinit var allRawRows: List<RawClinicRow>

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext<Context>()
    uploadRepository = UploadRepository(context)
    allRawRows = uploadRepository.getAllRawRows()
    assertTrue("Repository must have seeded raw rows", allRawRows.isNotEmpty())
  }

  @Test
  fun `independent ground truth raw row revenue calculation matches dashboard figure for specific date`() {
    val targetDate = "2026-10-04"

    // 1. Filter raw rows directly for the test date
    val dateRows = allRawRows.filter { it.reportDate == targetDate }
    assertTrue("Must have raw rows for $targetDate", dateRows.isNotEmpty())

    // 2. Independently compute total revenue by looping through raw rows directly
    var independentCalculatedRevenue = 0.0
    for (row in dateRows) {
      val rawAmount = row.columns["NetTotal"]
        ?.replace("$", "")
        ?.replace(",", "")
        ?.trim()
        ?.toDouble() ?: 0.0
      independentCalculatedRevenue += rawAmount
    }

    // 3. Compute dashboard KPI metrics using pure function module
    val filter = FilterState(datePreset = DatePreset.CUSTOM, customStartDate = targetDate, customEndDate = targetDate)
    val scopedRows = MetricsCalculator.filterRows(allRawRows, filter)
    val kpiMetrics = MetricsCalculator.calculateKpis(scopedRows, emptyList())

    // 4. Compare independent calculation against the dashboard figure
    assertEquals(
      "Independently calculated raw revenue must match the dashboard figure exactly",
      independentCalculatedRevenue,
      kpiMetrics.totalRevenue.value,
      0.001
    )

    // Verify bills count matches distinct invoices
    val independentUniqueBills = dateRows.map { it.invoiceNo }.distinct().size
    assertEquals(
      independentUniqueBills.toDouble(),
      kpiMetrics.patientBillsCount.value,
      0.001
    )

    // Verify average bill value
    val independentAbv = independentCalculatedRevenue / independentUniqueBills
    assertEquals(
      independentAbv,
      kpiMetrics.averageBillValue.value,
      0.001
    )
  }

  @Test
  fun `category split sums exactly to total revenue across Skin, Obesity, Hair, and Other`() {
    val targetDate = "2026-10-04"
    val dateRows = allRawRows.filter { it.reportDate == targetDate }
    val totalRevenue = dateRows.sumOf { it.amount }

    val categorySplit = MetricsCalculator.calculateCategorySplit(dateRows)

    // Ensure all 4 requested categories exist
    assertEquals(4, categorySplit.size)
    val skinMetric = categorySplit.find { it.category == CategoryGroup.SKIN }
    val obesityMetric = categorySplit.find { it.category == CategoryGroup.OBESITY }
    val hairMetric = categorySplit.find { it.category == CategoryGroup.HAIR }
    val otherMetric = categorySplit.find { it.category == CategoryGroup.OTHER }

    assertNotNull("Skin category must exist", skinMetric)
    assertNotNull("Obesity category must exist", obesityMetric)
    assertNotNull("Hair category must exist", hairMetric)
    assertNotNull("Other category must exist", otherMetric)

    // Sum of category revenues must equal total revenue
    val sumCategoryRevenues = categorySplit.sumOf { it.revenue }
    assertEquals(
      "Sum of category revenues must match total group revenue",
      totalRevenue,
      sumCategoryRevenues,
      0.001
    )

    // Sum of percentages must equal 100%
    val sumPercentages = categorySplit.sumOf { it.percentage }
    assertEquals(100.0, sumPercentages, 0.01)
  }

  @Test
  fun `daily trend point sums match raw row daily aggregates`() {
    val dailyTrend = MetricsCalculator.calculateDailyRevenueTrend(allRawRows)
    assertTrue("Daily trend must contain multiple data points", dailyTrend.isNotEmpty())

    dailyTrend.forEach { point ->
      val expectedDayRevenue = allRawRows.filter { it.reportDate == point.date }.sumOf { it.amount }
      assertEquals(
        "Trend point for ${point.date} must equal raw row sum",
        expectedDayRevenue,
        point.revenue,
        0.001
      )
    }
  }

  @Test
  fun `top and bottom services rankings correctly identify highest and lowest performers`() {
    val rankings = MetricsCalculator.calculateTopAndBottomServices(allRawRows)

    assertTrue("Top 10 by revenue must not be empty", rankings.top10ByRevenue.isNotEmpty())
    assertTrue("Bottom 10 by revenue must not be empty", rankings.bottom10ByRevenue.isNotEmpty())

    // First in top 10 must have highest revenue
    val highestRevenue = rankings.top10ByRevenue.first().revenue
    val lowestInTop = rankings.top10ByRevenue.last().revenue
    assertTrue(highestRevenue >= lowestInTop)

    // Bottom 10 first must have lower revenue than top 10 first
    val lowestOverall = rankings.bottom10ByRevenue.first().revenue
    assertTrue(lowestOverall <= highestRevenue)
  }
}
