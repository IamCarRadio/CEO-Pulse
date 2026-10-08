package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.UploadRepository
import com.example.model.IssueSeverity
import com.example.model.ReportType
import com.example.util.ExcelParser
import com.example.util.ReportValidator
import com.example.util.SampleExportData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UploadScreenTest {

  private lateinit var context: Context
  private lateinit var uploadRepository: UploadRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext<Context>()
    uploadRepository = UploadRepository(context)
  }

  @Test
  fun `parses real headers from Service Sales export format`() {
    val csvData = SampleExportData.getCsvSampleString("2026-10-04", "Downtown Executive Clinic")
    val inputStream = ByteArrayInputStream(csvData.toByteArray(Charsets.UTF_8))
    val parsed = ExcelParser.parseCsv(inputStream, "Service_Sales_20261004.csv")

    assertEquals(12, parsed.headers.size)
    assertTrue(parsed.headers.contains("InvoiceNo"))
    assertTrue(parsed.headers.contains("Date"))
    assertTrue(parsed.headers.contains("Doctor"))
    assertTrue(parsed.headers.contains("ServiceName"))
    assertTrue(parsed.headers.contains("NetTotal"))

    assertEquals(10, parsed.totalRowCount)
    assertEquals(10, parsed.rows.size)
    assertEquals("INV-2026-8091", parsed.rows[0]["InvoiceNo"])
    assertEquals("350.00", parsed.rows[0]["NetTotal"])
  }

  @Test
  fun `validator passes clean Service Sales data without errors`() {
    val rows = SampleExportData.getServiceSalesSampleRows("2026-10-04", "Downtown Executive Clinic")
    val report = ReportValidator.validate(ReportType.SERVICE_SALES, SampleExportData.HEADERS, rows)

    assertFalse("Expected clean data to have no errors", report.hasErrors)
    assertEquals(10, report.totalRows)
    assertEquals(10, report.validRows)
    assertEquals(0, report.errorCount)
  }

  @Test
  fun `validator catches blank dates and non-numeric amounts without failing silently`() {
    val dirtyRows = listOf(
      mapOf(
        "InvoiceNo" to "INV-101",
        "Date" to "", // BLANK DATE
        "ServiceName" to "Consultation",
        "NetTotal" to "120.00"
      ),
      mapOf(
        "InvoiceNo" to "INV-102",
        "Date" to "2026-10-04",
        "ServiceName" to "X-Ray",
        "NetTotal" to "NOT_A_NUMBER" // NON-NUMERIC AMOUNT
      )
    )

    val report = ReportValidator.validate(ReportType.SERVICE_SALES, listOf("InvoiceNo", "Date", "ServiceName", "NetTotal"), dirtyRows)

    assertTrue("Expected validation errors to be caught", report.hasErrors)
    assertEquals(2, report.errorCount)

    val blankDateIssue = report.issues.find { it.issue.contains("Blank date") }
    assertNotNull(blankDateIssue)
    assertEquals(IssueSeverity.ERROR, blankDateIssue?.severity)

    val nonNumericIssue = report.issues.find { it.issue.contains("Non-numeric amount") }
    assertNotNull(nonNumericIssue)
    assertEquals(IssueSeverity.ERROR, nonNumericIssue?.severity)
  }

  @Test
  fun `duplicate check identifies existing uploads and prevents silent double counting`() = runTest {
    val reportDate = "2026-10-04"
    val branch = "Downtown Executive Clinic"
    val rows = SampleExportData.getServiceSalesSampleRows(reportDate, branch)

    // Save initial batch
    val initialSave = uploadRepository.saveUpload(
      fileName = "Service_Sales_20261004.xlsx",
      reportType = ReportType.SERVICE_SALES,
      branch = branch,
      reportDate = reportDate,
      headers = SampleExportData.HEADERS,
      rows = rows,
      userEmail = "ceo@clinicgroup.com"
    )
    assertTrue(initialSave.success)

    // Check duplicate
    val duplicateResult = uploadRepository.checkDuplicate(ReportType.SERVICE_SALES, branch, reportDate)
    assertTrue("Should detect existing upload for same date and branch", duplicateResult.exists)
    assertEquals("Service_Sales_20261004.xlsx", duplicateResult.existingUpload?.fileName)

    // Verify row counts parsed vs saved
    assertEquals(rows.size, initialSave.parsedRows)
    assertEquals(rows.size, initialSave.savedRows)
  }

  @Test
  fun `replacing duplicate upload replaces previous records and prevents double-counting`() = runTest {
    val reportDate = "2026-10-05"
    val branch = "Northside Medical Plaza"
    val originalRows = SampleExportData.getServiceSalesSampleRows(reportDate, branch).take(4)

    val firstSave = uploadRepository.saveUpload(
      fileName = "Original_Export.xlsx",
      reportType = ReportType.SERVICE_SALES,
      branch = branch,
      reportDate = reportDate,
      headers = SampleExportData.HEADERS,
      rows = originalRows,
      userEmail = "ceo@clinicgroup.com"
    )

    val updatedRows = SampleExportData.getServiceSalesSampleRows(reportDate, branch) // 10 rows
    val replaceSave = uploadRepository.saveUpload(
      fileName = "Replaced_Export.xlsx",
      reportType = ReportType.SERVICE_SALES,
      branch = branch,
      reportDate = reportDate,
      headers = SampleExportData.HEADERS,
      rows = updatedRows,
      userEmail = "ceo@clinicgroup.com",
      replaceExistingUploadId = firstSave.uploadId
    )

    assertTrue(replaceSave.success)
    val history = uploadRepository.getUploadHistory()
    val matchingRecords = history.filter { it.reportDate == reportDate && it.branch == branch && it.reportType == ReportType.SERVICE_SALES }
    assertEquals("Must only have 1 active upload record after replacement", 1, matchingRecords.size)
    assertEquals(10, matchingRecords.first().savedRowCount)
  }

  @Test
  fun `delete upload removes record from history`() = runTest {
    val reportDate = "2026-10-06"
    val branch = "West End Health Center"
    val rows = SampleExportData.getServiceSalesSampleRows(reportDate, branch).take(3)

    val saveResult = uploadRepository.saveUpload(
      fileName = "To_Delete.xlsx",
      reportType = ReportType.CONSULTATIONS,
      branch = branch,
      reportDate = reportDate,
      headers = SampleExportData.HEADERS,
      rows = rows,
      userEmail = "ceo@clinicgroup.com"
    )

    assertTrue(uploadRepository.getUploadHistory().any { it.uploadId == saveResult.uploadId })

    uploadRepository.deleteUpload(saveResult.uploadId)
    assertFalse(uploadRepository.getUploadHistory().any { it.uploadId == saveResult.uploadId })
  }
}
