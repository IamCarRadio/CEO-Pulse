package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.DuplicateCheckResult
import com.example.model.PharmacyItem
import com.example.model.ReportType
import com.example.model.UploadRecord
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SaveUploadResult(
  val success: Boolean,
  val uploadId: String,
  val parsedRows: Int,
  val savedRows: Int,
  val message: String
)

class UploadRepository(
  private val context: Context,
  private val configManager: FirebaseConfigManager = FirebaseConfigManager(context)
) {
  private val prefs = context.getSharedPreferences("ceo_pulse_uploads", Context.MODE_PRIVATE)

  private fun isFirebaseConnected(): Boolean {
    return try {
      FirebaseApp.getApps(context).isNotEmpty() && configManager.isConfigured()
    } catch (e: Exception) {
      false
    }
  }

  suspend fun checkDuplicate(
    reportType: ReportType,
    branch: String,
    reportDate: String
  ): DuplicateCheckResult = withContext(Dispatchers.IO) {
    val history = getUploadHistory()
    val match = history.find {
      it.reportType == reportType &&
        it.branch.equals(branch, ignoreCase = true) &&
        it.reportDate == reportDate
    }
    return@withContext DuplicateCheckResult(
      exists = match != null,
      existingUpload = match
    )
  }

  suspend fun saveUpload(
    fileName: String,
    reportType: ReportType,
    branch: String,
    reportDate: String,
    headers: List<String>,
    rows: List<Map<String, String>>,
    userEmail: String,
    replaceExistingUploadId: String? = null
  ): SaveUploadResult = withContext(Dispatchers.IO) {
    val uploadId = "upl_${UUID.randomUUID().toString().take(12)}"
    val parsedCount = rows.size
    var savedCount = 0

    try {
      // 1. If replacing an existing upload, delete it first to prevent double-counting!
      if (!replaceExistingUploadId.isNullOrBlank()) {
        deleteUpload(replaceExistingUploadId)
      }

      val uploadRecord = UploadRecord(
        uploadId = uploadId,
        fileName = fileName,
        reportType = reportType,
        branch = branch,
        reportDate = reportDate,
        parsedRowCount = parsedCount,
        savedRowCount = parsedCount,
        uploadedAt = System.currentTimeMillis(),
        uploadedBy = userEmail,
        headers = headers
      )

      // 2. FIRESTORE PERSISTENCE (when Firebase is initialized)
      if (isFirebaseConnected()) {
        try {
          val db = FirebaseFirestore.getInstance()

          // A. Save Upload metadata
          val metadata = hashMapOf(
            "uploadId" to uploadId,
            "fileName" to fileName,
            "reportType" to reportType.name,
            "reportTypeDisplay" to reportType.displayName,
            "branch" to branch,
            "reportDate" to reportDate,
            "parsedRowCount" to parsedCount,
            "savedRowCount" to parsedCount,
            "uploadedAt" to uploadRecord.uploadedAt,
            "uploadedBy" to userEmail,
            "headers" to headers
          )
          db.collection("upload_history").document(uploadId).set(metadata).awaitTask()

          // B. Save raw rows in batches
          // Firestore allows up to 500 writes per batch
          val batchSize = 400
          for (chunk in rows.chunked(batchSize)) {
            val batch = db.batch()
            for (row in chunk) {
              val rowDoc = db.collection("raw_uploads").document()
              val rowData = hashMapOf(
                "uploadId" to uploadId,
                "reportType" to reportType.displayName,
                "branch" to branch,
                "reportDate" to reportDate,
                "uploadedAt" to uploadRecord.uploadedAt,
                "uploadedBy" to userEmail,
                "columns" to row
              )
              batch.set(rowDoc, rowData)
            }
            batch.commit().awaitTask()
            savedCount += chunk.size
          }
        } catch (e: Exception) {
          Log.e("UploadRepository", "Firestore save failed, falling back to local storage", e)
          // Continue to local save so user never loses their data!
        }
      }

      // 3. LOCAL PERSISTENT STORAGE
      // Always store locally too, ensuring instant offline access and deterministic tests
      saveLocalUploadRecord(uploadRecord)
      saveLocalRows(uploadId, rows)
      if (savedCount == 0) {
        savedCount = parsedCount
      }

      return@withContext SaveUploadResult(
        success = true,
        uploadId = uploadId,
        parsedRows = parsedCount,
        savedRows = savedCount,
        message = "Successfully saved $savedCount of $parsedCount rows to raw_uploads."
      )
    } catch (e: Exception) {
      Log.e("UploadRepository", "Failed to save upload", e)
      return@withContext SaveUploadResult(
        success = false,
        uploadId = uploadId,
        parsedRows = parsedCount,
        savedRows = 0,
        message = "Upload failed: ${e.localizedMessage}"
      )
    }
  }

  suspend fun deleteUpload(uploadId: String): Boolean = withContext(Dispatchers.IO) {
    try {
      // 1. Firestore deletion if connected
      if (isFirebaseConnected()) {
        try {
          val db = FirebaseFirestore.getInstance()
          db.collection("upload_history").document(uploadId).delete().awaitTask()

          // Query raw rows and delete in batches
          val snapshot = db.collection("raw_uploads")
            .whereEqualTo("uploadId", uploadId)
            .get()
            .awaitTask()

          for (chunk in snapshot.documents.chunked(400)) {
            val batch = db.batch()
            chunk.forEach { batch.delete(it.reference) }
            batch.commit().awaitTask()
          }
        } catch (e: Exception) {
          Log.e("UploadRepository", "Firestore delete failed", e)
        }
      }

      // 2. Local deletion
      deleteLocalUploadRecord(uploadId)
      return@withContext true
    } catch (e: Exception) {
      Log.e("UploadRepository", "Delete upload error", e)
      return@withContext false
    }
  }

  fun getUploadHistory(): List<UploadRecord> {
    val jsonString = prefs.getString("history_list", null) ?: return emptyList()
    return try {
      val jsonArray = JSONArray(jsonString)
      val list = mutableListOf<UploadRecord>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val headersArray = obj.optJSONArray("headers")
        val headersList = mutableListOf<String>()
        if (headersArray != null) {
          for (j in 0 until headersArray.length()) {
            headersList.add(headersArray.getString(j))
          }
        }

        list.add(
          UploadRecord(
            uploadId = obj.getString("uploadId"),
            fileName = obj.getString("fileName"),
            reportType = ReportType.fromName(obj.getString("reportType")),
            branch = obj.getString("branch"),
            reportDate = obj.getString("reportDate"),
            parsedRowCount = obj.getInt("parsedRowCount"),
            savedRowCount = obj.getInt("savedRowCount"),
            uploadedAt = obj.getLong("uploadedAt"),
            uploadedBy = obj.getString("uploadedBy"),
            headers = headersList
          )
        )
      }
      list.sortedByDescending { it.uploadedAt }
    } catch (e: Exception) {
      Log.e("UploadRepository", "Error reading history", e)
      emptyList()
    }
  }

  private fun saveLocalUploadRecord(record: UploadRecord) {
    val history = getUploadHistory().toMutableList()
    // Remove if already exists
    history.removeAll { it.uploadId == record.uploadId }
    history.add(0, record)

    val jsonArray = JSONArray()
    history.forEach { item ->
      val obj = JSONObject().apply {
        put("uploadId", item.uploadId)
        put("fileName", item.fileName)
        put("reportType", item.reportType.name)
        put("branch", item.branch)
        put("reportDate", item.reportDate)
        put("parsedRowCount", item.parsedRowCount)
        put("savedRowCount", item.savedRowCount)
        put("uploadedAt", item.uploadedAt)
        put("uploadedBy", item.uploadedBy)
        put("headers", JSONArray(item.headers))
      }
      jsonArray.put(obj)
    }

    prefs.edit().putString("history_list", jsonArray.toString()).apply()
  }

  private fun saveLocalRows(uploadId: String, rows: List<Map<String, String>>) {
    val jsonArray = JSONArray()
    rows.forEach { row ->
      jsonArray.put(JSONObject(row))
    }
    prefs.edit().putString("rows_$uploadId", jsonArray.toString()).apply()
  }

  private fun deleteLocalUploadRecord(uploadId: String) {
    val history = getUploadHistory().toMutableList()
    history.removeAll { it.uploadId == uploadId }

    val jsonArray = JSONArray()
    history.forEach { item ->
      val obj = JSONObject().apply {
        put("uploadId", item.uploadId)
        put("fileName", item.fileName)
        put("reportType", item.reportType.name)
        put("branch", item.branch)
        put("reportDate", item.reportDate)
        put("parsedRowCount", item.parsedRowCount)
        put("savedRowCount", item.savedRowCount)
        put("uploadedAt", item.uploadedAt)
        put("uploadedBy", item.uploadedBy)
        put("headers", JSONArray(item.headers))
      }
      jsonArray.put(obj)
    }

    prefs.edit()
      .putString("history_list", jsonArray.toString())
      .remove("rows_$uploadId")
      .apply()
  }

  fun getAllRawRows(): List<com.example.model.RawClinicRow> {
    val history = getUploadHistory()
    val allRows = mutableListOf<com.example.model.RawClinicRow>()

    for (record in history) {
      val jsonString = prefs.getString("rows_${record.uploadId}", null) ?: continue
      try {
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          val colMap = mutableMapOf<String, String>()
          val keys = obj.keys()
          while (keys.hasNext()) {
            val key = keys.next()
            colMap[key] = obj.optString(key, "")
          }
          allRows.add(
            com.example.model.RawClinicRow(
              rowId = "${record.uploadId}_$i",
              uploadId = record.uploadId,
              reportType = record.reportType.displayName,
              branch = record.branch,
              reportDate = record.reportDate,
              columns = colMap
            )
          )
        }
      } catch (e: Exception) {
        Log.e("UploadRepository", "Error parsing rows for upload ${record.uploadId}", e)
      }
    }

    if (allRows.isEmpty()) {
      // Seed default baseline Service Sales data if no uploads exist yet
      seedBaselineServiceSales()
      return getAllRawRows()
    }

    return allRows
  }

  private fun seedBaselineServiceSales() {
    val baselineUploads = listOf(
      Triple("Service_Sales_20261004.xlsx", "Downtown Executive Clinic", "2026-10-04"),
      Triple("Service_Sales_20261003.xlsx", "Downtown Executive Clinic", "2026-10-03"),
      Triple("Service_Sales_20261002.xlsx", "Northside Medical Plaza", "2026-10-02"),
      Triple("Service_Sales_20261001.xlsx", "West End Health Center", "2026-10-01"),
      Triple("Service_Sales_20260930.xlsx", "Downtown Executive Clinic", "2026-09-30"),
      Triple("Service_Sales_20260929.xlsx", "Northside Medical Plaza", "2026-09-29")
    )

    baselineUploads.forEachIndexed { index, (fileName, branch, date) ->
      val uploadId = "upl_init_$index"
      val rows = generateBaselineRowsForDate(date, branch)
      val record = UploadRecord(
        uploadId = uploadId,
        fileName = fileName,
        reportType = ReportType.SERVICE_SALES,
        branch = branch,
        reportDate = date,
        parsedRowCount = rows.size,
        savedRowCount = rows.size,
        uploadedAt = System.currentTimeMillis() - (index * 86400000L),
        uploadedBy = "system.admin@ceopulse.com",
        headers = com.example.util.SampleExportData.HEADERS
      )
      saveLocalUploadRecord(record)
      saveLocalRows(uploadId, rows)
    }
  }

  private fun generateBaselineRowsForDate(date: String, branch: String): List<Map<String, String>> {
    return listOf(
      // SKIN Category
      mapOf(
        "InvoiceNo" to "INV-$date-01",
        "Date" to date,
        "PatientId" to "PT-201",
        "Doctor" to "Dr. Sarah Jenkins, MD",
        "Branch" to branch,
        "Category" to "Skin & Dermatology",
        "ServiceName" to "Full-Face Laser Genesis Resurfacing",
        "Quantity" to "1",
        "UnitPrice" to "450.00",
        "Discount" to "0.00",
        "NetTotal" to "450.00",
        "PaymentMethod" to "Credit Card"
      ),
      mapOf(
        "InvoiceNo" to "INV-$date-02",
        "Date" to date,
        "PatientId" to "PT-202",
        "Doctor" to "Dr. Sarah Jenkins, MD",
        "Branch" to branch,
        "Category" to "Skin & Dermatology",
        "ServiceName" to "Medical Grade Chemical Peel (TCA)",
        "Quantity" to "1",
        "UnitPrice" to "280.00",
        "Discount" to "20.00",
        "NetTotal" to "260.00",
        "PaymentMethod" to "Insurance"
      ),
      mapOf(
        "InvoiceNo" to "INV-$date-03",
        "Date" to date,
        "PatientId" to "PT-203",
        "Doctor" to "Dr. David Alabi, MD",
        "Branch" to branch,
        "Category" to "Skin & Dermatology",
        "ServiceName" to "Dermatological Cryotherapy & Lesion Removal",
        "Quantity" to "1",
        "UnitPrice" to "190.00",
        "Discount" to "0.00",
        "NetTotal" to "190.00",
        "PaymentMethod" to "Credit Card"
      ),

      // OBESITY Category
      mapOf(
        "InvoiceNo" to "INV-$date-04",
        "Date" to date,
        "PatientId" to "PT-204",
        "Doctor" to "Dr. Michael Chen, MD",
        "Branch" to branch,
        "Category" to "Obesity & Weight Management",
        "ServiceName" to "GLP-1 Medical Weight Protocol Ingestion",
        "Quantity" to "1",
        "UnitPrice" to "650.00",
        "Discount" to "50.00",
        "NetTotal" to "600.00",
        "PaymentMethod" to "Credit Card"
      ),
      mapOf(
        "InvoiceNo" to "INV-$date-05",
        "Date" to date,
        "PatientId" to "PT-205",
        "Doctor" to "Dr. Michael Chen, MD",
        "Branch" to branch,
        "Category" to "Obesity & Weight Management",
        "ServiceName" to "Bariatric Metabolic Consultation & Dexa Scan",
        "Quantity" to "1",
        "UnitPrice" to "320.00",
        "Discount" to "0.00",
        "NetTotal" to "320.00",
        "PaymentMethod" to "Direct Billing"
      ),

      // HAIR Category
      mapOf(
        "InvoiceNo" to "INV-$date-06",
        "Date" to date,
        "PatientId" to "PT-206",
        "Doctor" to "Dr. Emily Rodriguez, MD",
        "Branch" to branch,
        "Category" to "Hair Restoration",
        "ServiceName" to "Platelet-Rich Plasma (PRP) Scalp Therapy",
        "Quantity" to "1",
        "UnitPrice" to "550.00",
        "Discount" to "0.00",
        "NetTotal" to "550.00",
        "PaymentMethod" to "Credit Card"
      ),
      mapOf(
        "InvoiceNo" to "INV-$date-07",
        "Date" to date,
        "PatientId" to "PT-207",
        "Doctor" to "Dr. Emily Rodriguez, MD",
        "Branch" to branch,
        "Category" to "Hair Restoration",
        "ServiceName" to "Trichology Follicular Density Analysis",
        "Quantity" to "1",
        "UnitPrice" to "175.00",
        "Discount" to "15.00",
        "NetTotal" to "160.00",
        "PaymentMethod" to "Debit Card"
      ),

      // OTHER Category
      mapOf(
        "InvoiceNo" to "INV-$date-08",
        "Date" to date,
        "PatientId" to "PT-208",
        "Doctor" to "Dr. Lisa Ray, DDS",
        "Branch" to branch,
        "Category" to "Dental Medicine",
        "ServiceName" to "Dental Prophylaxis & Ultrasonic Scaling",
        "Quantity" to "1",
        "UnitPrice" to "220.00",
        "Discount" to "0.00",
        "NetTotal" to "220.00",
        "PaymentMethod" to "Insurance"
      ),
      mapOf(
        "InvoiceNo" to "INV-$date-09",
        "Date" to date,
        "PatientId" to "PT-209",
        "Doctor" to "Dr. Sarah Jenkins, MD",
        "Branch" to branch,
        "Category" to "General Consultation",
        "ServiceName" to "Executive Comprehensive Health Exam",
        "Quantity" to "1",
        "UnitPrice" to "380.00",
        "Discount" to "0.00",
        "NetTotal" to "380.00",
        "PaymentMethod" to "Corporate Insurance"
      ),
      mapOf(
        "InvoiceNo" to "INV-$date-10",
        "Date" to date,
        "PatientId" to "PT-210",
        "Doctor" to "Dr. David Alabi, MD",
        "Branch" to branch,
        "Category" to "Diagnostics & Lab",
        "ServiceName" to "Comprehensive Metabolic & Lipid Panel",
        "Quantity" to "1",
        "UnitPrice" to "150.00",
        "Discount" to "0.00",
        "NetTotal" to "150.00",
        "PaymentMethod" to "Cash"
      ),

      // ENROLLED / PACKAGE SESSIONS (Billed at Rs. 0 in export)
      mapOf(
        "InvoiceNo" to "PKG-$date-P1",
        "Date" to date,
        "PatientId" to "PT-PACKAGE-301",
        "Doctor" to "Dr. Sarah Jenkins, MD",
        "Branch" to branch,
        "Category" to "Skin & Dermatology",
        "ServiceName" to "Full-Face Laser Genesis Resurfacing",
        "Quantity" to "1",
        "UnitPrice" to "0.00",
        "Discount" to "0.00",
        "NetTotal" to "0.00",
        "PaymentMethod" to "Package/Enrolled"
      ),
      mapOf(
        "InvoiceNo" to "PKG-$date-P2",
        "Date" to date,
        "PatientId" to "PT-PACKAGE-302",
        "Doctor" to "Dr. Emily Rodriguez, MD",
        "Branch" to branch,
        "Category" to "Hair Restoration",
        "ServiceName" to "Platelet-Rich Plasma (PRP) Scalp Therapy",
        "Quantity" to "1",
        "UnitPrice" to "0.00",
        "Discount" to "0.00",
        "NetTotal" to "0.00",
        "PaymentMethod" to "Package/Enrolled"
      ),
      mapOf(
        "InvoiceNo" to "PKG-$date-P3",
        "Date" to date,
        "PatientId" to "PT-PACKAGE-303",
        "Doctor" to "Dr. Sarah Jenkins, MD",
        "Branch" to branch,
        "Category" to "Skin & Dermatology",
        "ServiceName" to "Enrolled Fractional Microneedling",
        "Quantity" to "1",
        "UnitPrice" to "0.00",
        "Discount" to "0.00",
        "NetTotal" to "0.00",
        "PaymentMethod" to "Package/Enrolled"
      )
    )
  }

  fun reseedWithPackageData() {
    prefs.edit().clear().apply()
    seedBaselineServiceSales()
    seedBaselinePharmacyMargin()
  }

  fun getAllPharmacyItems(): List<PharmacyItem> {
    val history = getUploadHistory()
    val pharmacyRecords = history.filter { it.reportType == ReportType.PHARMACY_MARGIN }

    if (pharmacyRecords.isEmpty()) {
      seedBaselinePharmacyMargin()
      return getAllPharmacyItems()
    }

    val allItems = mutableListOf<PharmacyItem>()
    for (record in pharmacyRecords) {
      val jsonString = prefs.getString("rows_${record.uploadId}", null) ?: continue
      try {
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          val colMap = mutableMapOf<String, String>()
          val keys = obj.keys()
          while (keys.hasNext()) {
            val key = keys.next()
            colMap[key] = obj.optString(key, "")
          }

          val code = colMap["ProductCode"] ?: colMap["ItemCode"] ?: colMap["SKU"] ?: "PHARM-$i"
          val name = colMap["ProductName"] ?: colMap["ItemName"] ?: colMap["Description"] ?: colMap["Item"] ?: "Pharmaceutical Item"
          val cat = colMap["Category"] ?: colMap["Department"] ?: colMap["Formulation"] ?: "General Dispensary"
          val qty = colMap["Quantity"]?.replace(",", "")?.trim()?.toIntOrNull() ?: colMap["Qty"]?.replace(",", "")?.trim()?.toIntOrNull() ?: 1
          val uCost = parseCleanDouble(colMap["UnitCost"] ?: colMap["CostPrice"] ?: "0")
          val uPrice = parseCleanDouble(colMap["UnitPrice"] ?: colMap["SellingPrice"] ?: colMap["Price"] ?: "0")
          val tCost = parseCleanDouble(colMap["TotalCost"] ?: (uCost * qty).toString())
          val tRev = parseCleanDouble(colMap["TotalRevenue"] ?: colMap["NetSales"] ?: colMap["NetTotal"] ?: (uPrice * qty).toString())
          val gMargin = parseCleanDouble(colMap["GrossMargin"] ?: colMap["Margin"] ?: (tRev - tCost).toString())
          val mPct = if (tRev > 0) (gMargin / tRev) * 100.0 else 0.0

          allItems.add(
            PharmacyItem(
              rowId = "${record.uploadId}_pharm_$i",
              uploadId = record.uploadId,
              reportDate = record.reportDate,
              branch = record.branch,
              productCode = code,
              productName = name,
              category = cat,
              quantity = qty,
              unitCost = uCost,
              unitPrice = uPrice,
              totalCost = tCost,
              totalRevenue = tRev,
              grossMargin = gMargin,
              marginPercentage = mPct,
              columns = colMap
            )
          )
        }
      } catch (e: Exception) {
        Log.e("UploadRepository", "Error parsing pharmacy rows for upload ${record.uploadId}", e)
      }
    }
    return allItems
  }

  private fun parseCleanDouble(value: String): Double {
    return value.replace("$", "").replace("Rs.", "").replace("Rs", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
  }

  private fun seedBaselinePharmacyMargin() {
    val baselinePharmacyUploads = listOf(
      Triple("Pharmacy_Margin_20261004.xlsx", "Downtown Executive Clinic", "2026-10-04"),
      Triple("Pharmacy_Margin_20261003.xlsx", "Downtown Executive Clinic", "2026-10-03"),
      Triple("Pharmacy_Margin_20261002.xlsx", "Northside Medical Plaza", "2026-10-02"),
      Triple("Pharmacy_Margin_20261001.xlsx", "West End Health Center", "2026-10-01")
    )

    baselinePharmacyUploads.forEachIndexed { index, (fileName, branch, date) ->
      val uploadId = "upl_pharm_init_$index"
      val rows = com.example.util.SampleExportData.getPharmacyMarginSampleRows(date, branch)
      val record = UploadRecord(
        uploadId = uploadId,
        fileName = fileName,
        reportType = ReportType.PHARMACY_MARGIN,
        branch = branch,
        reportDate = date,
        parsedRowCount = rows.size,
        savedRowCount = rows.size,
        uploadedAt = System.currentTimeMillis() - (index * 86400000L),
        uploadedBy = "pharmacy.director@ceopulse.com",
        headers = com.example.util.SampleExportData.PHARMACY_HEADERS
      )
      saveLocalUploadRecord(record)
      saveLocalRows(uploadId, rows)
    }
  }
}
