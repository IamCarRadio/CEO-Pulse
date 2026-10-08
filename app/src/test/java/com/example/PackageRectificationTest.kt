package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PackageValuesRepository
import com.example.data.UploadRepository
import com.example.model.PackageValueSetting
import com.example.model.RawClinicRow
import com.example.model.ValuationMode
import com.example.util.MetricsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PackageRectificationTest {

  private lateinit var context: Context
  private lateinit var uploadRepository: UploadRepository
  private lateinit var packageValuesRepository: PackageValuesRepository
  private lateinit var allRawRows: List<RawClinicRow>

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext<Context>()
    uploadRepository = UploadRepository(context)
    packageValuesRepository = PackageValuesRepository(context)
    allRawRows = uploadRepository.getAllRawRows()
    assertTrue("Raw rows must exist in repository", allRawRows.isNotEmpty())
  }

  @Test
  fun `all zero rupee bills are detected and flagged as Package or Enrolled sessions`() {
    // 1. Detect package sessions across all rows
    val packageRows = MetricsCalculator.detectPackageRows(allRawRows)
    assertTrue("Repository baseline must contain Rs. 0 package sessions", packageRows.isNotEmpty())

    // 2. Verify every detected package session is indeed Rs. 0
    packageRows.forEach { row ->
      assertTrue(
        "Row ${row.invoiceNo} amount must be 0.0, but was ${row.amount}",
        row.amount <= 0.0001
      )
      assertTrue("Row isPackageSession must evaluate to true", row.isPackageSession)
    }

    // 3. Verify non-package sessions have amount > 0
    val paidRows = allRawRows.filter { !it.isPackageSession }
    paidRows.forEach { row ->
      assertTrue("Paid session amount must be > 0", row.amount > 0.0)
    }
  }

  @Test
  fun `package values repository persists and retrieves per-session values`() {
    val testServiceName = "Custom Fractional Resurfacing VIP"
    val testRate = 425.50
    val testCategory = "Skin & Dermatology"
    val testNotes = "VIP 8-Session Package Rule"

    // 1. Save new custom package value rule
    packageValuesRepository.savePackageValue(
      serviceName = testServiceName,
      perSessionValue = testRate,
      category = testCategory,
      notes = testNotes
    )

    // 2. Retrieve map and list
    val rulesMap = packageValuesRepository.getPackageValuesMap()
    assertTrue("Rules map must contain newly added service", rulesMap.containsKey(testServiceName))
    assertEquals(testRate, rulesMap[testServiceName] ?: 0.0, 0.001)

    val rulesList = packageValuesRepository.getPackageValueSettingsList()
    val savedSetting = rulesList.find { it.serviceName == testServiceName }
    assertNotNull("Saved setting must be in list", savedSetting)
    assertEquals(testRate, savedSetting!!.perSessionValue, 0.001)
    assertEquals(testCategory, savedSetting.category)
    assertEquals(testNotes, savedSetting.notes)

    // 3. Clean up by deleting
    packageValuesRepository.deletePackageValue(testServiceName)
    val afterDeleteMap = packageValuesRepository.getPackageValuesMap()
    assertFalse("Deleted service must not be present in map", afterDeleteMap.containsKey(testServiceName))
  }

  @Test
  fun `reconciliation line formula holds exactly Collected + Package Value equals Service-Value Total`() {
    val packageMap = packageValuesRepository.getPackageValuesMap()
    val reconciliation = MetricsCalculator.calculateReconciliation(allRawRows, packageMap)

    val collected = reconciliation.collectedRevenue
    val packageRectified = reconciliation.packageRectifiedValue
    val serviceValueTotal = reconciliation.serviceValueTotal

    // Equation: Collected + Package Rectified = Service-Value Total
    assertEquals(
      "Collected + Package Rectified must exactly equal Service-Value Total",
      collected + packageRectified,
      serviceValueTotal,
      0.001
    )

    assertTrue("Reconciliation status flag must be true", reconciliation.isReconciled)
    assertEquals(
      reconciliation.totalSessionCount,
      reconciliation.paidSessionCount + reconciliation.packageSessionCount
    )
  }

  @Test
  fun `dashboard revenue responds accurately to valuation mode toggle`() {
    val packageMap = packageValuesRepository.getPackageValuesMap()

    // Mode 1: Collected Revenue (Default)
    val collectedKpis = MetricsCalculator.calculateKpis(
      currentRows = allRawRows,
      previousRows = emptyList(),
      valuationMode = ValuationMode.COLLECTED,
      packageValues = packageMap
    )

    // Mode 2: Service-Value Revenue (Package sessions valued at defined rates)
    val serviceValueKpis = MetricsCalculator.calculateKpis(
      currentRows = allRawRows,
      previousRows = emptyList(),
      valuationMode = ValuationMode.SERVICE_VALUE,
      packageValues = packageMap
    )

    val rawSumCollected = allRawRows.sumOf { it.amount }
    assertEquals(
      "In Collected mode, total revenue must equal raw cash invoiced",
      rawSumCollected,
      collectedKpis.totalRevenue.value,
      0.001
    )

    assertTrue(
      "Service-Value revenue must be greater than Collected revenue when package sessions exist",
      serviceValueKpis.totalRevenue.value > collectedKpis.totalRevenue.value
    )

    val reconciliation = MetricsCalculator.calculateReconciliation(allRawRows, packageMap)
    assertEquals(
      "Service-Value revenue must equal reconciliation serviceValueTotal",
      reconciliation.serviceValueTotal,
      serviceValueKpis.totalRevenue.value,
      0.001
    )
  }

  @Test
  fun `unmapped Rs 0 package services are detected so nothing is silently valued at zero`() {
    // Empty map simulates no package rules defined yet
    val unmapped = MetricsCalculator.findUnmappedPackageServices(allRawRows, emptyMap())

    val packageRows = MetricsCalculator.detectPackageRows(allRawRows)
    assertTrue("Must have package rows", packageRows.isNotEmpty())
    assertTrue("All package services should be flagged as unmapped when rules map is empty", unmapped.isNotEmpty())

    // Sum of unmapped sessions should equal total package sessions
    val totalUnmappedSessions = unmapped.sumOf { it.sessionCount }
    assertEquals(
      packageRows.sumOf { it.quantity },
      totalUnmappedSessions
    )

    // With default package map, check that unmapped services (e.g. Enrolled Fractional Microneedling) are reported
    val currentMap = packageValuesRepository.getPackageValuesMap()
    val partialUnmapped = MetricsCalculator.findUnmappedPackageServices(allRawRows, currentMap)
    assertTrue(
      "Services not in default rules (like Enrolled Fractional Microneedling) must be detected as unmapped",
      partialUnmapped.any { it.serviceName.contains("Fractional Microneedling", ignoreCase = true) }
    )
  }
}
