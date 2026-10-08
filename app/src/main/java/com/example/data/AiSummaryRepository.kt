package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.AiSummaryData
import com.example.model.ValuationMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository responsible for caching AI summaries per date/period key so
 * the Gemini API is not called on every open.
 */
class AiSummaryRepository(private val context: Context) {

  private val prefs = context.getSharedPreferences("ceo_pulse_ai_summary_cache", Context.MODE_PRIVATE)

  /**
   * Generates a deterministic cache key for a given period, valuation mode, and branch.
   */
  fun buildCacheKey(periodDisplay: String, valuationMode: ValuationMode, branch: String): String {
    val cleanPeriod = periodDisplay.replace(" ", "_").replace("(", "").replace(")", "").replace("-", "_")
    val cleanBranch = branch.replace(" ", "_")
    return "ai_sum_${cleanPeriod}_${valuationMode.name}_${cleanBranch}"
  }

  /**
   * Retrieves the cached AI summary if available for the given key.
   */
  fun getCachedSummary(cacheKey: String): AiSummaryData? {
    val jsonStr = prefs.getString(cacheKey, null) ?: return null
    return try {
      val json = JSONObject(jsonStr)
      val fiveLineList = mutableListOf<String>()
      val fiveLineArr = json.optJSONArray("fiveLineSummary") ?: JSONArray()
      for (i in 0 until fiveLineArr.length()) {
        fiveLineList.add(fiveLineArr.getString(i))
      }

      val actionsList = mutableListOf<String>()
      val actionsArr = json.optJSONArray("suggestedActions") ?: JSONArray()
      for (i in 0 until actionsArr.length()) {
        actionsList.add(actionsArr.getString(i))
      }

      val modeStr = json.optString("valuationMode", ValuationMode.COLLECTED.name)
      val valuationMode = try {
        ValuationMode.valueOf(modeStr)
      } catch (_: Exception) {
        ValuationMode.COLLECTED
      }

      AiSummaryData(
        cacheKey = cacheKey,
        formattedPeriod = json.optString("formattedPeriod", ""),
        valuationMode = valuationMode,
        branchName = json.optString("branchName", "All Branches"),
        generatedAtMillis = json.optLong("generatedAtMillis", System.currentTimeMillis()),
        fiveLineSummary = fiveLineList,
        whatImproved = json.optString("whatImproved", ""),
        whatDropped = json.optString("whatDropped", ""),
        whyAnalysis = json.optString("whyAnalysis", ""),
        suggestedActions = actionsList,
        rawResponseText = json.optString("rawResponseText", ""),
        isCached = true,
        isLiveGeminiCall = json.optBoolean("isLiveGeminiCall", false),
        disclaimerNote = json.optString(
          "disclaimerNote",
          "Generated strictly from pre-computed metrics. No raw patient records or PII transmitted."
        )
      )
    } catch (e: Exception) {
      Log.e("AiSummaryRepository", "Error parsing cached AI summary for $cacheKey", e)
      null
    }
  }

  /**
   * Persists the AI summary to SharedPreferences under the cache key.
   */
  fun saveSummary(summary: AiSummaryData) {
    try {
      val json = JSONObject().apply {
        put("cacheKey", summary.cacheKey)
        put("formattedPeriod", summary.formattedPeriod)
        put("valuationMode", summary.valuationMode.name)
        put("branchName", summary.branchName)
        put("generatedAtMillis", summary.generatedAtMillis)

        val fiveLineArr = JSONArray()
        summary.fiveLineSummary.forEach { fiveLineArr.put(it) }
        put("fiveLineSummary", fiveLineArr)

        put("whatImproved", summary.whatImproved)
        put("whatDropped", summary.whatDropped)
        put("whyAnalysis", summary.whyAnalysis)

        val actionsArr = JSONArray()
        summary.suggestedActions.forEach { actionsArr.put(it) }
        put("suggestedActions", actionsArr)

        put("rawResponseText", summary.rawResponseText)
        put("isLiveGeminiCall", summary.isLiveGeminiCall)
        put("disclaimerNote", summary.disclaimerNote)
      }

      prefs.edit().putString(summary.cacheKey, json.toString()).apply()
    } catch (e: Exception) {
      Log.e("AiSummaryRepository", "Error saving AI summary for ${summary.cacheKey}", e)
    }
  }

  /**
   * Clears the cached summary for a specific cache key (e.g. on user-triggered regeneration).
   */
  fun clearCacheForDate(cacheKey: String) {
    prefs.edit().remove(cacheKey).apply()
  }

  /**
   * Clears all cached summaries.
   */
  fun clearAllCache() {
    prefs.edit().clear().apply()
  }
}
