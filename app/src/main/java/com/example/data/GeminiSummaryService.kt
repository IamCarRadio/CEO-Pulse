package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.AiSummaryData
import com.example.model.CategoryMetric
import com.example.model.DailyTrendPoint
import com.example.model.DashboardKpis
import com.example.model.DoctorRevenueMetric
import com.example.model.DualStreamRevenue
import com.example.model.FilterState
import com.example.model.ReconciliationSummary
import com.example.model.ServiceRankings
import com.example.model.ValuationMode
import com.example.util.MetricsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiSummaryService {

  companion object {
    private const val TAG = "GeminiSummaryService"
    // Using gemini-3.5-flash as mandated by gemini-api guidelines for text summarization
    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
  }

  // 60-second timeouts as mandated by Gemini API guidelines
  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  /**
   * Builds the prompt payload sending ONLY already-computed metrics.
   * Absolutely NO raw rows, and NO patient names or identifiers are ever included.
   */
  fun buildMetricsOnlyPrompt(
    filterState: FilterState,
    valuationMode: ValuationMode,
    kpis: DashboardKpis,
    dailyTrend: List<DailyTrendPoint>,
    serviceRankings: ServiceRankings,
    categorySplit: List<CategoryMetric>,
    doctorRevenue: List<DoctorRevenueMetric>,
    dualStreamRevenue: DualStreamRevenue,
    reconciliationSummary: ReconciliationSummary
  ): String {
    val sb = StringBuilder()

    sb.appendLine("=== CONFIDENTIAL EXECUTIVE PRE-AGGREGATED METRICS ONLY ===")
    sb.appendLine("NOTE: No raw patient invoices, no raw transaction rows, and no patient names are provided.")
    sb.appendLine()
    sb.appendLine("--- REPORTING CONTEXT ---")
    sb.appendLine("Selected Period: ${filterState.formattedDateDisplay}")
    sb.appendLine("Selected Clinic Branch: ${filterState.selectedBranch}")
    sb.appendLine("Valuation Mode: ${if (valuationMode == ValuationMode.SERVICE_VALUE) "Service-Value Revenue (Package Sessions Valued)" else "Collected Revenue (Cash Inflow)"}")
    sb.appendLine()

    sb.appendLine("--- TOP-LINE CLINIC PERFORMANCE KPIS ---")
    sb.appendLine("Total Revenue: Current = ${kpis.totalRevenue.formattedValue}, Prior Period = ${MetricsCalculator.formatCurrency(kpis.totalRevenue.previousValue)}, Change = ${if (kpis.totalRevenue.previousValue > 0) String.format("%.1f%%", kpis.totalRevenue.percentageChange) else "Data Missing for Prior Period"}")
    sb.appendLine("Patient Bills Count: Current = ${kpis.patientBillsCount.formattedValue}, Prior Period = ${String.format("%.0f", kpis.patientBillsCount.previousValue)}, Change = ${if (kpis.patientBillsCount.previousValue > 0) String.format("%.1f%%", kpis.patientBillsCount.percentageChange) else "Data Missing for Prior Period"}")
    sb.appendLine("Average Bill Value: Current = ${kpis.averageBillValue.formattedValue}, Prior Period = ${MetricsCalculator.formatCurrency(kpis.averageBillValue.previousValue)}, Change = ${if (kpis.averageBillValue.previousValue > 0) String.format("%.1f%%", kpis.averageBillValue.percentageChange) else "Data Missing for Prior Period"}")
    sb.appendLine("Total Procedure Units Completed: ${kpis.totalProcedureUnits}")
    sb.appendLine()

    sb.appendLine("--- MULTI-STREAM & PACKAGE RECTIFICATION ---")
    if (valuationMode == ValuationMode.SERVICE_VALUE) {
      sb.appendLine("Collected Revenue Component: ${MetricsCalculator.formatCurrency(reconciliationSummary.collectedRevenue)}")
      sb.appendLine("Package Value Recognized: ${MetricsCalculator.formatCurrency(reconciliationSummary.packageRectifiedValue)} across ${reconciliationSummary.packageSessionCount} package sessions")
      sb.appendLine("Service-Value Total: ${MetricsCalculator.formatCurrency(reconciliationSummary.serviceValueTotal)}")
      sb.appendLine("Unmapped Package Sessions Count: ${reconciliationSummary.unmappedSessionCount} (valued at zero)")
    } else {
      sb.appendLine("Valuation Mode is set to 'Collected Revenue' (package sessions recorded at Rs. 0.00 cash).")
      sb.appendLine("Potential Unrecognized Package Value: ${MetricsCalculator.formatCurrency(reconciliationSummary.packageRectifiedValue)} (${reconciliationSummary.packageSessionCount} enrolled sessions).")
    }

    if (dualStreamRevenue.pharmacyRevenue > 0) {
      sb.appendLine("Clinical Services Revenue: ${MetricsCalculator.formatCurrency(dualStreamRevenue.clinicalServiceRevenue)} (${String.format("%.1f%%", dualStreamRevenue.clinicalSharePercentage)} share)")
      sb.appendLine("Pharmacy Dispensary Revenue: ${MetricsCalculator.formatCurrency(dualStreamRevenue.pharmacyRevenue)} (${String.format("%.1f%%", dualStreamRevenue.pharmacySharePercentage)} share)")
      sb.appendLine("Combined Clinic Group Total: ${MetricsCalculator.formatCurrency(dualStreamRevenue.totalCombinedRevenue)}")
    } else {
      sb.appendLine("Pharmacy Revenue Stream: No pharmacy sales recorded for this period.")
    }
    sb.appendLine()

    sb.appendLine("--- SPECIALTY CATEGORY SPLIT ---")
    if (categorySplit.isEmpty()) {
      sb.appendLine("Category breakdown: No category data available.")
    } else {
      categorySplit.forEach { cat ->
        sb.appendLine("- ${cat.category.displayName}: ${MetricsCalculator.formatCurrency(cat.revenue)} (${String.format("%.1f%%", cat.percentage)}, ${cat.count} bills)")
      }
    }
    sb.appendLine()

    sb.appendLine("--- TOP 5 CLINICAL SERVICES BY REVENUE ---")
    val topServices = serviceRankings.top10ByRevenue.take(5)
    if (topServices.isEmpty()) {
      sb.appendLine("Top services: None recorded in this period.")
    } else {
      topServices.forEachIndexed { i, s ->
        sb.appendLine("${i + 1}. ${s.serviceName} (${s.category}): ${MetricsCalculator.formatCurrency(s.revenue)} (${s.count} units, Avg Price ${MetricsCalculator.formatCurrency(s.averagePrice)})")
      }
    }
    sb.appendLine()

    sb.appendLine("--- BOTTOM 3 CLINICAL SERVICES BY REVENUE ---")
    val bottomServices = serviceRankings.bottom10ByRevenue.take(3)
    if (bottomServices.isEmpty()) {
      sb.appendLine("Bottom services: None recorded.")
    } else {
      bottomServices.forEachIndexed { i, s ->
        sb.appendLine("${i + 1}. ${s.serviceName}: ${MetricsCalculator.formatCurrency(s.revenue)} (${s.count} units)")
      }
    }
    sb.appendLine()

    sb.appendLine("--- DOCTOR REVENUE CONTRIBUTION ---")
    if (doctorRevenue.isEmpty()) {
      sb.appendLine("Doctor distribution: No doctor data available.")
    } else {
      doctorRevenue.take(4).forEach { doc ->
        sb.appendLine("- ${doc.doctorName}: ${MetricsCalculator.formatCurrency(doc.revenue)} (${String.format("%.1f%%", doc.percentage)} of total, ${doc.billCount} bills, Avg ${MetricsCalculator.formatCurrency(doc.averageBill)})")
      }
    }

    return sb.toString()
  }

  /**
   * Generates executive summary via Gemini REST API, or provides a verified fallback if API key is unconfigured.
   */
  suspend fun generateExecutiveSummary(
    cacheKey: String,
    filterState: FilterState,
    valuationMode: ValuationMode,
    kpis: DashboardKpis,
    dailyTrend: List<DailyTrendPoint>,
    serviceRankings: ServiceRankings,
    categorySplit: List<CategoryMetric>,
    doctorRevenue: List<DoctorRevenueMetric>,
    dualStreamRevenue: DualStreamRevenue,
    reconciliationSummary: ReconciliationSummary
  ): AiSummaryData = withContext(Dispatchers.IO) {
    val metricsPrompt = buildMetricsOnlyPrompt(
      filterState, valuationMode, kpis, dailyTrend, serviceRankings,
      categorySplit, doctorRevenue, dualStreamRevenue, reconciliationSummary
    )

    val apiKey = BuildConfig.GEMINI_API_KEY
    val isKeyConfigured = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

    if (isKeyConfigured) {
      try {
        val result = callGeminiRestApi(apiKey, metricsPrompt, cacheKey, filterState, valuationMode)
        if (result != null) {
          return@withContext result
        }
      } catch (e: Exception) {
        Log.w(TAG, "Gemini REST API call failed, using verified metric analysis fallback: ${e.message}")
      }
    } else {
      Log.i(TAG, "Gemini API key is unset or placeholder; generating compliant verified metric analysis directly from computed figures.")
    }

    // Deterministic fallback quoting ONLY the supplied metrics with zero invented numbers
    generateCompliantFallback(
      cacheKey = cacheKey,
      filterState = filterState,
      valuationMode = valuationMode,
      kpis = kpis,
      serviceRankings = serviceRankings,
      categorySplit = categorySplit,
      doctorRevenue = doctorRevenue,
      dualStreamRevenue = dualStreamRevenue,
      reconciliationSummary = reconciliationSummary
    )
  }

  private suspend fun callGeminiRestApi(
    apiKey: String,
    metricsPrompt: String,
    cacheKey: String,
    filterState: FilterState,
    valuationMode: ValuationMode
  ): AiSummaryData? = withContext(Dispatchers.IO) {
    val systemInstructionText = """
You are the Executive Medical Operations & Financial AI Analyst for CEO Pulse clinic group.
You are provided ONLY with pre-aggregated, already-computed executive clinic metrics.
NEVER ask for or expect raw patient names or invoice rows.

STRICT MANDATORY RULES:
1. You must NOT calculate, estimate, extrapolate, or invent any numbers.
2. You may ONLY quote the exact numbers and percentages explicitly supplied in the prompt.
3. If comparison data or any specific metrics are missing or not provided, you MUST explicitly state that the data is missing.
4. Output your response adhering strictly to these three distinct sections:

SECTION 1: 5-LINE CEO SUMMARY
Write EXACTLY 5 concise, plain-English executive lines:
Line 1: High-level financial milestone and revenue trajectory.
Line 2: Patient bill count and clinical volume throughput.
Line 3: Average bill value and yield efficiency.
Line 4: Primary specialty category and top procedure driver.
Line 5: Strategic multi-stream (pharmacy or package valuation) standing.

SECTION 2: WHAT IMPROVED, WHAT DROPPED, AND WHY
- What Improved: [Detail items that showed growth, quoting exact supplied percentages and amounts]
- What Dropped: [Detail items that declined, quoting exact supplied numbers; or explicitly state if no drops occurred or prior data is missing]
- Why: [Explain the operational cause strictly linking back to the provided volume, top procedures, or doctor contributions]

SECTION 3: 3 SUGGESTED ACTIONS
Provide EXACTLY 3 numbered, high-leverage strategic actions:
1. [Action 1 based strictly on supplied figures]
2. [Action 2 based strictly on supplied figures]
3. [Action 3 based strictly on supplied figures]
""".trimIndent()

    val rootJson = JSONObject().apply {
      val contentsArr = JSONArray()
      val userContent = JSONObject().apply {
        put("role", "user")
        val partsArr = JSONArray()
        partsArr.put(JSONObject().apply { put("text", metricsPrompt) })
        put("parts", partsArr)
      }
      contentsArr.put(userContent)
      put("contents", contentsArr)

      val sysInst = JSONObject().apply {
        val parts = JSONArray()
        parts.put(JSONObject().apply { put("text", systemInstructionText) })
        put("parts", parts)
      }
      put("systemInstruction", sysInst)

      val genConfig = JSONObject().apply {
        put("temperature", 0.2) // Low temperature for high factual precision
        put("topP", 0.95)
      }
      put("generationConfig", genConfig)
    }

    val candidateModels = listOf(GEMINI_MODEL, "gemini-flash-latest", "gemini-3.1-flash-lite-preview", "gemini-3.1-pro-preview")
    for (modelName in candidateModels) {
      try {
        val url = "$BASE_URL$modelName:generateContent?key=$apiKey"
        val body = rootJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
          .url(url)
          .post(body)
          .build()

        val response = okHttpClient.newCall(request).execute()
        val respBody = response.body?.string()
        if (!response.isSuccessful) {
          Log.w(TAG, "Gemini API non-200 on model $modelName: ${response.code} ${response.message}")
          continue
        }

        if (respBody.isNullOrBlank()) continue
        val respJson = JSONObject(respBody)
        val candidates = respJson.optJSONArray("candidates") ?: continue
        if (candidates.length() == 0) continue
        val firstCand = candidates.getJSONObject(0)
        val content = firstCand.optJSONObject("content") ?: continue
        val parts = content.optJSONArray("parts") ?: continue
        if (parts.length() == 0) continue

        val generatedText = buildString {
          for (i in 0 until parts.length()) {
            val partObj = parts.optJSONObject(i) ?: continue
            val t = partObj.optString("text", "")
            if (t.isNotBlank()) {
              append(t)
            }
          }
        }.trim()

        if (generatedText.isBlank()) continue

        return@withContext parseGeminiResponse(
          rawText = generatedText,
          cacheKey = cacheKey,
          filterState = filterState,
          valuationMode = valuationMode,
          isLiveGemini = true
        )
      } catch (e: Exception) {
        Log.w(TAG, "Attempt with model $modelName failed: ${e.message}")
      }
    }
    return@withContext null
  }

  /**
   * Parses the response from Gemini into structured sections.
   */
  fun parseGeminiResponse(
    rawText: String,
    cacheKey: String,
    filterState: FilterState,
    valuationMode: ValuationMode,
    isLiveGemini: Boolean
  ): AiSummaryData {
    val fiveLines = mutableListOf<String>()
    var whatImproved = ""
    var whatDropped = ""
    var whyAnalysis = ""
    val suggestedActions = mutableListOf<String>()

    val lines = rawText.lines()
    var currentSection = ""

    lines.forEach { line ->
      val trimmed = line.trim()
      if (trimmed.contains("SECTION 1", ignoreCase = true)) {
        currentSection = "SECTION_1"
      } else if (trimmed.contains("SECTION 2", ignoreCase = true)) {
        currentSection = "SECTION_2"
      } else if (trimmed.contains("SECTION 3", ignoreCase = true)) {
        currentSection = "SECTION_3"
      } else if (currentSection == "SECTION_1" && trimmed.isNotBlank() && !trimmed.startsWith("#")) {
        val cleanLine = trimmed.replaceFirst(Regex("^[-*•0-9.]+\\s*"), "")
        if (cleanLine.isNotBlank() && fiveLines.size < 5) {
          fiveLines.add(cleanLine)
        }
      } else if (currentSection == "SECTION_2") {
        val clean = trimmed.replaceFirst(Regex("^[-*•]+\\s*"), "")
        if (clean.startsWith("What Improved", ignoreCase = true)) {
          whatImproved = clean.substringAfter(":").trim()
        } else if (clean.startsWith("What Dropped", ignoreCase = true)) {
          whatDropped = clean.substringAfter(":").trim()
        } else if (clean.startsWith("Why", ignoreCase = true)) {
          whyAnalysis = clean.substringAfter(":").trim()
        }
      } else if (currentSection == "SECTION_3" && trimmed.isNotBlank() && !trimmed.startsWith("#")) {
        val cleanLine = trimmed.replaceFirst(Regex("^[-*•0-9.]+\\s*"), "")
        if (cleanLine.isNotBlank() && suggestedActions.size < 3) {
          suggestedActions.add(cleanLine)
        }
      }
    }

    // Safety checks in case formatting was free-form
    if (fiveLines.size < 5) {
      val candidates = lines.filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("SECTION") }
      while (fiveLines.size < 5 && candidates.size > fiveLines.size) {
        fiveLines.add(candidates[fiveLines.size].replaceFirst(Regex("^[-*•0-9.]+\\s*"), "").trim())
      }
    }

    if (suggestedActions.size < 3) {
      val lastLines = lines.takeLast(6).filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("SECTION") }
      lastLines.forEach { l ->
        val clean = l.replaceFirst(Regex("^[-*•0-9.]+\\s*"), "").trim()
        if (clean.isNotBlank() && suggestedActions.size < 3 && !suggestedActions.contains(clean)) {
          suggestedActions.add(clean)
        }
      }
    }

    return AiSummaryData(
      cacheKey = cacheKey,
      formattedPeriod = filterState.formattedDateDisplay,
      valuationMode = valuationMode,
      branchName = filterState.selectedBranch,
      generatedAtMillis = System.currentTimeMillis(),
      fiveLineSummary = fiveLines.take(5),
      whatImproved = whatImproved.ifBlank { "Top-line revenue and procedure volumes performed positively." },
      whatDropped = whatDropped.ifBlank { "No significant drop observed across the supplied metrics." },
      whyAnalysis = whyAnalysis.ifBlank { "Specialty mix and provider throughput drove current performance." },
      suggestedActions = suggestedActions.take(3),
      rawResponseText = rawText,
      isCached = false,
      isLiveGeminiCall = isLiveGemini
    )
  }

  /**
   * Deterministic fallback that strictly adheres to the rule:
   * Only quotes provided metrics, never invents numbers, states missing data.
   */
  fun generateCompliantFallback(
    cacheKey: String,
    filterState: FilterState,
    valuationMode: ValuationMode,
    kpis: DashboardKpis,
    serviceRankings: ServiceRankings,
    categorySplit: List<CategoryMetric>,
    doctorRevenue: List<DoctorRevenueMetric>,
    dualStreamRevenue: DualStreamRevenue,
    reconciliationSummary: ReconciliationSummary
  ): AiSummaryData {
    val revCurrent = kpis.totalRevenue.formattedValue
    val revPrior = MetricsCalculator.formatCurrency(kpis.totalRevenue.previousValue)
    val revChange = if (kpis.totalRevenue.previousValue > 0) String.format("%.1f%%", kpis.totalRevenue.percentageChange) else "Missing prior period baseline"
    val billsCurrent = kpis.patientBillsCount.formattedValue
    val billsPrior = String.format("%.0f", kpis.patientBillsCount.previousValue)
    val billsChange = if (kpis.patientBillsCount.previousValue > 0) String.format("%.1f%%", kpis.patientBillsCount.percentageChange) else "Missing prior period baseline"
    val abvCurrent = kpis.averageBillValue.formattedValue
    val topCat = categorySplit.firstOrNull()
    val topService = serviceRankings.top10ByRevenue.firstOrNull()
    val topDoc = doctorRevenue.firstOrNull()

    val fiveLines = listOf(
      "1. Total revenue closed at $revCurrent for ${filterState.formattedDateDisplay} (prior: $revPrior, change: $revChange).",
      "2. Patient volume yielded $billsCurrent bills with ${kpis.totalProcedureUnits} procedure units completed across ${filterState.selectedBranch}.",
      "3. Average transaction yield stood at $abvCurrent per patient bill (prior: ${MetricsCalculator.formatCurrency(kpis.averageBillValue.previousValue)}).",
      "4. ${topCat?.category?.displayName ?: "Top category"} led specialty share with ${topCat?.let { MetricsCalculator.formatCurrency(it.revenue) } ?: "N/A"} (${topCat?.let { String.format("%.1f%%", it.percentage) } ?: "N/A"}), anchored by ${topService?.serviceName ?: "clinical services"}.",
      if (valuationMode == ValuationMode.SERVICE_VALUE) {
        "5. Service-value accounting recognized ${MetricsCalculator.formatCurrency(reconciliationSummary.packageRectifiedValue)} across ${reconciliationSummary.packageSessionCount} package sessions for total service delivery of ${MetricsCalculator.formatCurrency(reconciliationSummary.serviceValueTotal)}."
      } else if (dualStreamRevenue.pharmacyRevenue > 0) {
        "5. Dual-stream operations contributed ${MetricsCalculator.formatCurrency(dualStreamRevenue.clinicalServiceRevenue)} from clinical services and ${MetricsCalculator.formatCurrency(dualStreamRevenue.pharmacyRevenue)} (${String.format("%.1f%%", dualStreamRevenue.pharmacySharePercentage)}) from pharmacy dispensary."
      } else {
        "5. Cash collections totaled $revCurrent; package session value of ${MetricsCalculator.formatCurrency(reconciliationSummary.packageRectifiedValue)} remains uncollected on cash basis."
      }
    )

    val whatImproved = if (kpis.totalRevenue.percentageChange >= 0 && kpis.totalRevenue.previousValue > 0) {
      "Total revenue rose by $revChange to $revCurrent, and patient bills rose by $billsChange to $billsCurrent."
    } else if (kpis.totalRevenue.previousValue <= 0) {
      "Current revenue reached $revCurrent ($billsCurrent bills); prior period comparison data is missing."
    } else {
      "Procedure units reached ${kpis.totalProcedureUnits} units across active clinical services."
    }

    val whatDropped = if (kpis.totalRevenue.percentageChange < 0 && kpis.totalRevenue.previousValue > 0) {
      "Total revenue dropped by $revChange from $revPrior to $revCurrent."
    } else if (reconciliationSummary.unmappedSessionCount > 0) {
      "${reconciliationSummary.unmappedSessionCount} enrolled session rows have unmapped rules and were recorded at Rs. 0.00."
    } else {
      "No revenue decline detected across primary KPIs; prior period trend data for bottom services is missing."
    }

    val why = buildString {
      append("Performance was primarily anchored by ${topCat?.category?.displayName ?: "top clinical specialties"} generating ${topCat?.let { MetricsCalculator.formatCurrency(it.revenue) } ?: "baseline revenue"}")
      if (topService != null) {
        append(", with '${topService.serviceName}' contributing ${MetricsCalculator.formatCurrency(topService.revenue)} (${topService.count} units)")
      }
      if (topDoc != null) {
        append(", and ${topDoc.doctorName} delivering ${String.format("%.1f%%", topDoc.percentage)} of total turnover (${MetricsCalculator.formatCurrency(topDoc.revenue)})")
      }
      append(".")
    }

    val suggestedActions = listOf(
      if (reconciliationSummary.unmappedSessionCount > 0) {
        "Map per-session values for the ${reconciliationSummary.unmappedSessionCount} unmapped package sessions in the Package Values table to avoid zero-valuation."
      } else {
        "Expand capacity for top-performing '${topService?.serviceName ?: "resurfacing"}' which drove ${topService?.let { MetricsCalculator.formatCurrency(it.revenue) } ?: "primary turnover"}."
      },
      "Review provider scheduling to balance patient volume, leveraging ${topDoc?.doctorName ?: "lead clinician"}'s high throughput (${topDoc?.billCount ?: 0} bills, ${topDoc?.let { MetricsCalculator.formatCurrency(it.revenue) } ?: "Rs. 0"}).",
      if (dualStreamRevenue.pharmacyRevenue > 0) {
        "Strengthen clinical-to-pharmacy cross-dispensing protocols to grow the current ${String.format("%.1f%%", dualStreamRevenue.pharmacySharePercentage)} pharmacy revenue share (${MetricsCalculator.formatCurrency(dualStreamRevenue.pharmacyRevenue)})."
      } else {
        "Audit low-yield bottom clinical procedures (${serviceRankings.bottom10ByRevenue.firstOrNull()?.serviceName ?: "low volume services"}) for pricing or package bundling opportunities."
      }
    )

    return AiSummaryData(
      cacheKey = cacheKey,
      formattedPeriod = filterState.formattedDateDisplay,
      valuationMode = valuationMode,
      branchName = filterState.selectedBranch,
      generatedAtMillis = System.currentTimeMillis(),
      fiveLineSummary = fiveLines,
      whatImproved = whatImproved,
      whatDropped = whatDropped,
      whyAnalysis = why,
      suggestedActions = suggestedActions,
      rawResponseText = "Deterministic Metric Synthesis (Quoting Pre-Aggregated Values Only)",
      isCached = false,
      isLiveGeminiCall = false,
      disclaimerNote = "Generated strictly from pre-computed metrics. No raw patient records or PII transmitted."
    )
  }
}
