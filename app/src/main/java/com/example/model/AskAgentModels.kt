package com.example.model

/**
 * Data model for the Ask Agent chat conversation.
 */
data class AskChatMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val isUser: Boolean,
  val text: String,
  val timestamp: Long = System.currentTimeMillis(),
  val toolCalls: List<AskToolExecution> = emptyList(),
  val mathWorking: String? = null,
  val isError: Boolean = false
)

/**
 * Represents a tool execution triggered during query answering.
 * Highlights tool name, filters applied, raw outputs, and formatted summary.
 */
data class AskToolExecution(
  val toolName: String,
  val arguments: Map<String, String>,
  val filtersUsedDisplay: String,
  val rawOutputJson: String,
  val summaryOutput: String
)

/**
 * Static tool definitions for Gemini function calling.
 */
object AskToolNames {
  const val GET_REVENUE = "getRevenue"
  const val GET_TOP_SERVICES = "getTopServices"
  const val GET_DOCTOR_SPLIT = "getDoctorSplit"
  const val GET_PHARMACY_MARGIN = "getPharmacyMargin"
  const val COMPARE_PERIODS = "comparePeriods"
  const val LIST_UPLOADS = "listUploads"
}
