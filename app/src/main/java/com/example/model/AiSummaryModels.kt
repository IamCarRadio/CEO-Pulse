package com.example.model

/**
 * Data model for the executive AI Summary generated via Gemini.
 * Contains only pre-computed metrics and strict executive deliverables:
 * - 5-line plain-English CEO summary
 * - What improved, what dropped, and why
 * - 3 suggested actions
 * - Caching metadata per date/period
 */
data class AiSummaryData(
  val cacheKey: String,
  val formattedPeriod: String,
  val valuationMode: ValuationMode,
  val branchName: String,
  val generatedAtMillis: Long,
  val fiveLineSummary: List<String>,
  val whatImproved: String,
  val whatDropped: String,
  val whyAnalysis: String,
  val suggestedActions: List<String>,
  val rawResponseText: String,
  val isCached: Boolean = false,
  val isLiveGeminiCall: Boolean = false,
  val disclaimerNote: String = "Generated strictly from pre-computed metrics. No raw patient records or PII transmitted."
)

sealed class AiSummaryUiState {
  object Idle : AiSummaryUiState()
  object Loading : AiSummaryUiState()
  data class Success(val data: AiSummaryData) : AiSummaryUiState()
  data class Error(val message: String, val cachedData: AiSummaryData? = null) : AiSummaryUiState()
}
