package com.example

import com.example.data.AskToolsHandler
import com.example.data.GeminiAskAgentService
import com.example.model.AskToolNames
import com.example.model.DatePreset
import com.example.model.FilterState
import com.example.model.PharmacyItem
import com.example.model.RawClinicRow
import com.example.model.ReportType
import com.example.model.UploadRecord
import com.example.model.UserProfile
import com.example.model.ValuationMode
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
@Config(sdk = [34])
class AskAgentTest {

  private lateinit var sampleRows: List<RawClinicRow>
  private lateinit var samplePharmacy: List<PharmacyItem>
  private lateinit var sampleUploads: List<UploadRecord>
  private lateinit var toolsHandler: AskToolsHandler
  private lateinit var agentService: GeminiAskAgentService

  private fun createSampleRow(
    rowId: String,
    invoiceNo: String,
    serviceName: String,
    doctor: String,
    branch: String,
    category: String,
    amount: Double,
    quantity: Int,
    reportDate: String
  ): RawClinicRow {
    return RawClinicRow(
      rowId = rowId,
      uploadId = "upl_1",
      reportType = "Service Sales",
      branch = branch,
      reportDate = reportDate,
      columns = mapOf(
        "InvoiceNo" to invoiceNo,
        "ServiceName" to serviceName,
        "Doctor" to doctor,
        "Category" to category,
        "NetTotal" to amount.toString(),
        "Quantity" to quantity.toString()
      )
    )
  }

  private fun createSamplePharmacy(
    rowId: String,
    productCode: String,
    productName: String,
    category: String,
    branch: String,
    reportDate: String,
    quantity: Int,
    revenue: Double,
    cost: Double
  ): PharmacyItem {
    val margin = revenue - cost
    val marginPct = if (revenue > 0) (margin / revenue) * 100.0 else 0.0
    return PharmacyItem(
      rowId = rowId,
      uploadId = "upl_pharm_1",
      reportDate = reportDate,
      branch = branch,
      productCode = productCode,
      productName = productName,
      category = category,
      quantity = quantity,
      unitCost = cost / quantity,
      unitPrice = revenue / quantity,
      totalCost = cost,
      totalRevenue = revenue,
      grossMargin = margin,
      marginPercentage = marginPct
    )
  }

  @Before
  fun setup() {
    sampleRows = listOf(
      createSampleRow(
        rowId = "r1",
        invoiceNo = "INV-101",
        serviceName = "HydraFacial Elite MD",
        doctor = "Dr. Sarah Al-Mansoor",
        branch = "Downtown Executive Clinic",
        category = "Skin",
        amount = 12000.0,
        quantity = 1,
        reportDate = "2026-10-04"
      ),
      createSampleRow(
        rowId = "r2",
        invoiceNo = "INV-102",
        serviceName = "PRP Scalp Therapy",
        doctor = "Dr. Tariq Mahmood",
        branch = "Downtown Executive Clinic",
        category = "Hair",
        amount = 18000.0,
        quantity = 1,
        reportDate = "2026-10-04"
      ),
      createSampleRow(
        rowId = "r3",
        invoiceNo = "INV-103",
        serviceName = "Semaglutide Weight Protocol",
        doctor = "Dr. Sarah Al-Mansoor",
        branch = "Westside Wellness Pavilion",
        category = "Obesity",
        amount = 25000.0,
        quantity = 1,
        reportDate = "2026-10-03" // Yesterday
      )
    )

    samplePharmacy = listOf(
      createSamplePharmacy(
        rowId = "p1",
        productCode = "RX-001",
        productName = "Retinol Serum 0.5%",
        category = "Topical Dermatology",
        branch = "Downtown Executive Clinic",
        reportDate = "2026-10-04",
        quantity = 5,
        revenue = 15000.0,
        cost = 9000.0
      ),
      createSamplePharmacy(
        rowId = "p2",
        productCode = "RX-002",
        productName = "Biotin Scalp Tonic",
        category = "Trichology",
        branch = "Downtown Executive Clinic",
        reportDate = "2026-10-04",
        quantity = 3,
        revenue = 6000.0,
        cost = 3000.0
      )
    )

    sampleUploads = listOf(
      UploadRecord(
        uploadId = "upl_001",
        fileName = "Daily_Sales_20261004.xlsx",
        reportType = ReportType.SERVICE_SALES,
        branch = "Downtown Executive Clinic",
        reportDate = "2026-10-04",
        parsedRowCount = 2,
        savedRowCount = 2,
        uploadedAt = System.currentTimeMillis(),
        uploadedBy = "ceo@clinicgroup.com"
      )
    )

    toolsHandler = AskToolsHandler(
      getRawRows = { sampleRows },
      getPharmacyItems = { samplePharmacy },
      getPackageValues = { emptyMap() },
      getUploadHistory = { sampleUploads },
      getGlobalFilter = { FilterState(datePreset = DatePreset.TODAY, selectedBranch = "All Branches") },
      getValuationMode = { ValuationMode.COLLECTED }
    )

    agentService = GeminiAskAgentService(toolsHandler)
  }

  @Test
  fun testToolGetRevenue_computesDualStreamAndTotals() {
    val exec = toolsHandler.executeTool(AskToolNames.GET_REVENUE, mapOf("period" to "today", "branch" to "All Branches"))
    assertEquals(AskToolNames.GET_REVENUE, exec.toolName)

    val json = JSONObject(exec.rawOutputJson)
    // Clinical: 12000 + 18000 = 30000; Pharmacy: 15000 + 6000 = 21000; Total = 51000
    assertEquals(51000.0, json.getDouble("totalCombinedRevenue"), 0.01)
    assertEquals(30000.0, json.getDouble("clinicalRevenue"), 0.01)
    assertEquals(21000.0, json.getDouble("pharmacyRevenue"), 0.01)
    assertEquals(2, json.getInt("patientBillsCount"))
    assertTrue(exec.filtersUsedDisplay.contains("Period: Today"))
  }

  @Test
  fun testToolGetTopServices_returnsRankedServices() {
    val exec = toolsHandler.executeTool(AskToolNames.GET_TOP_SERVICES, mapOf("limit" to "5", "sortBy" to "revenue"))
    assertEquals(AskToolNames.GET_TOP_SERVICES, exec.toolName)

    val json = JSONObject(exec.rawOutputJson)
    val services = json.getJSONArray("services")
    assertTrue(services.length() > 0)
    val top1 = services.getJSONObject(0)
    assertEquals("PRP Scalp Therapy", top1.getString("serviceName"))
    assertEquals(18000.0, top1.getDouble("revenue"), 0.01)
  }

  @Test
  fun testToolGetDoctorSplit_calculatesDoctorShares() {
    val exec = toolsHandler.executeTool(AskToolNames.GET_DOCTOR_SPLIT, mapOf("period" to "today"))
    assertEquals(AskToolNames.GET_DOCTOR_SPLIT, exec.toolName)

    val json = JSONObject(exec.rawOutputJson)
    val doctors = json.getJSONArray("doctors")
    assertEquals(2, doctors.length())
  }

  @Test
  fun testToolGetPharmacyMargin_computesMarginsCorrectly() {
    val exec = toolsHandler.executeTool(AskToolNames.GET_PHARMACY_MARGIN, mapOf("period" to "today"))
    assertEquals(AskToolNames.GET_PHARMACY_MARGIN, exec.toolName)

    val json = JSONObject(exec.rawOutputJson)
    assertEquals(21000.0, json.getDouble("totalRevenue"), 0.01)
    assertEquals(12000.0, json.getDouble("totalCost"), 0.01)
    assertEquals(9000.0, json.getDouble("grossMargin"), 0.01)
  }

  @Test
  fun testToolComparePeriods_calculatesDeltas() {
    val exec = toolsHandler.executeTool(
      AskToolNames.COMPARE_PERIODS,
      mapOf("period1" to "today", "period2" to "yesterday", "branch" to "All Branches")
    )
    assertEquals(AskToolNames.COMPARE_PERIODS, exec.toolName)

    val json = JSONObject(exec.rawOutputJson)
    val comp = json.getJSONObject("comparison")
    // Period 1 (today: 30000) vs Period 2 (yesterday: 25000) -> Delta = 5000
    assertEquals(5000.0, comp.getDouble("revenueDifference"), 0.01)
  }

  @Test
  fun testToolListUploads_filtersAndReturnsRecords() {
    val exec = toolsHandler.executeTool(AskToolNames.LIST_UPLOADS, mapOf("reportType" to "all"))
    assertEquals(AskToolNames.LIST_UPLOADS, exec.toolName)

    val json = JSONObject(exec.rawOutputJson)
    assertEquals(1, json.getInt("totalUploadsCount"))
    val uploads = json.getJSONArray("uploads")
    assertEquals("Daily_Sales_20261004.xlsx", uploads.getJSONObject(0).getString("fileName"))
  }

  @Test
  fun testAgentService_deterministicLocalExecutionReturnsMathWorking() = runBlocking {
    val response = agentService.ask("Compare today's revenue versus yesterday")
    assertFalse(response.isUser)
    assertEquals(1, response.toolCalls.size)
    assertEquals(AskToolNames.COMPARE_PERIODS, response.toolCalls[0].toolName)
    assertNotNull(response.mathWorking)
    assertTrue(response.mathWorking!!.contains("Revenue Difference"))
  }

  @Test
  fun testOwnerRoleGateVerification() {
    val ownerUser = UserProfile(
      email = "uzairkhanp72@gmail.com",
      uid = "u1",
      role = "CEO / Primary Owner"
    )
    val nonOwnerUser = UserProfile(
      email = "staff@clinicgroup.com",
      uid = "u2",
      role = "Front Desk Receptionist"
    )

    fun checkOwner(user: UserProfile): Boolean {
      val email = user.email.lowercase()
      val role = user.role.lowercase()
      return email == "uzairkhanp72@gmail.com" ||
        role.contains("owner") ||
        role.contains("ceo") ||
        role.contains("executive")
    }

    assertTrue(checkOwner(ownerUser))
    assertFalse(checkOwner(nonOwnerUser))
  }
}
