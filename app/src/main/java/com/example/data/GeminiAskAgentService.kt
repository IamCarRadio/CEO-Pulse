package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.AskChatMessage
import com.example.model.AskToolExecution
import com.example.model.AskToolNames
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

/**
 * Gemini-powered conversational agent using function calling.
 * Backed by read-only tools and metrics module.
 */
class GeminiAskAgentService(
  private val toolsHandler: AskToolsHandler
) {

  companion object {
    private const val TAG = "GeminiAskAgentService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
  }

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  /**
   * Main entry point to process a user question.
   */
  suspend fun ask(
    userQuestion: String,
    conversationHistory: List<AskChatMessage> = emptyList()
  ): AskChatMessage = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    val isKeyConfigured = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

    if (isKeyConfigured) {
      try {
        val geminiResult = callGeminiWithFunctionCalling(userQuestion, apiKey)
        if (geminiResult != null) {
          return@withContext geminiResult
        }
      } catch (e: Exception) {
        Log.w(TAG, "Gemini function calling call failed, falling back to local tool execution: ${e.message}")
      }
    }

    // Deterministic local tool execution fallback
    return@withContext executeLocalToolAgent(userQuestion)
  }

  private fun callGeminiWithFunctionCalling(
    userQuestion: String,
    apiKey: String
  ): AskChatMessage? {
    val systemInstructionText = """
You are the Read-Only Executive AI Data Agent for CEO Pulse clinic group.
You assist the clinic owner and CEO with real-time operations, revenue, doctor performance, pharmacy margins, and data feeds.

STRICT MANDATORY RULES:
1. READ-ONLY GUARANTEE: You cannot edit, delete, insert, or modify any database or uploaded file.
2. ANSWER DATA QUESTIONS ONLY BY CALLING TOOLS: You must NEVER invent, assume, or hallucinate metrics. Always call the appropriate tool to retrieve verified metrics.
3. ALWAYS DISCLOSE TOOLS & FILTERS: In your response, clearly state which tool you called and which filters were applied (e.g. branch, period, valuation mode).
4. SIMPLE MATH & WORKING: You can do simple math on tool outputs (such as percentage shares, absolute differences, margins, sums, or ratios). When you perform math, you MUST show your working in a dedicated section titled:
📐 Math Working:
[Show formula, input values from tool, and result step-by-step]
5. Keep answers clear, factual, concise, and structured for executive decision-making.
""".trimIndent()

    val toolsArray = buildToolsSchema()
    val candidateModels = listOf("gemini-3.8-flash", "gemini-3.5-flash", "gemini-flash-latest", "gemini-3.1-pro-preview")

    for (modelName in candidateModels) {
      try {
        // Step 1: Initial request to Gemini with tools
        val contents = JSONArray().apply {
          put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", userQuestion)))
          })
        }

        val requestPayload = JSONObject().apply {
          put("contents", contents)
          put("tools", toolsArray)
          put("systemInstruction", JSONObject().apply {
            put("parts", JSONArray().put(JSONObject().put("text", systemInstructionText)))
          })
          put("generationConfig", JSONObject().apply {
            put("temperature", 0.1)
          })
        }

        val url = "$BASE_URL$modelName:generateContent?key=$apiKey"
        val request = Request.Builder()
          .url(url)
          .post(requestPayload.toString().toRequestBody("application/json".toMediaType()))
          .build()

        val response = okHttpClient.newCall(request).execute()
        val respBody = response.body?.string()

        if (!response.isSuccessful || respBody.isNullOrBlank()) {
          Log.w(TAG, "Model $modelName returned HTTP ${response.code}: $respBody")
          continue
        }

        val respJson = JSONObject(respBody)
        val candidates = respJson.optJSONArray("candidates") ?: continue
        if (candidates.length() == 0) continue
        val firstCand = candidates.getJSONObject(0)
        val content = firstCand.optJSONObject("content") ?: continue
        val parts = content.optJSONArray("parts") ?: continue

        // Check if Gemini invoked a function call
        var functionCallObj: JSONObject? = null
        var directText = ""

        for (i in 0 until parts.length()) {
          val p = parts.getJSONObject(i)
          if (p.has("functionCall")) {
            functionCallObj = p.getJSONObject("functionCall")
            break
          } else if (p.has("text")) {
            directText += p.getString("text")
          }
        }

        if (functionCallObj != null) {
          // Gemini asked to execute a tool!
          val toolName = functionCallObj.getString("name")
          val argsObj = functionCallObj.optJSONObject("args") ?: JSONObject()
          val argsMap = mutableMapOf<String, String>()
          val keys = argsObj.keys()
          while (keys.hasNext()) {
            val k = keys.next()
            argsMap[k] = argsObj.optString(k, "")
          }

          // Execute the tool locally
          val toolExecution = toolsHandler.executeTool(toolName, argsMap)

          // Step 2: Send functionResponse back to Gemini for final executive synthesis
          val secondTurnContents = JSONArray().apply {
            // User turn
            put(JSONObject().apply {
              put("role", "user")
              put("parts", JSONArray().put(JSONObject().put("text", userQuestion)))
            })
            // Model turn (tool call)
            put(JSONObject().apply {
              put("role", "model")
              put("parts", JSONArray().put(JSONObject().apply {
                put("functionCall", functionCallObj)
              }))
            })
            // Tool turn (function response)
            put(JSONObject().apply {
              put("role", "user")
              put("parts", JSONArray().put(JSONObject().apply {
                put("functionResponse", JSONObject().apply {
                  put("name", toolName)
                  put("response", JSONObject().apply {
                    put("name", toolName)
                    put("content", JSONObject(toolExecution.rawOutputJson))
                  })
                })
              }))
            })
          }

          val secondRequestPayload = JSONObject().apply {
            put("contents", secondTurnContents)
            put("systemInstruction", JSONObject().apply {
              put("parts", JSONArray().put(JSONObject().put("text", systemInstructionText)))
            })
            put("generationConfig", JSONObject().apply {
              put("temperature", 0.1)
            })
          }

          val secondReq = Request.Builder()
            .url(url)
            .post(secondRequestPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

          val secondResp = okHttpClient.newCall(secondReq).execute()
          val secondBody = secondResp.body?.string()

          if (secondResp.isSuccessful && !secondBody.isNullOrBlank()) {
            val secondJson = JSONObject(secondBody)
            val secondCands = secondJson.optJSONArray("candidates")
            if (secondCands != null && secondCands.length() > 0) {
              val sContent = secondCands.getJSONObject(0).optJSONObject("content")
              val sParts = sContent?.optJSONArray("parts")
              val synthesizedText = buildString {
                if (sParts != null) {
                  for (j in 0 until sParts.length()) {
                    append(sParts.getJSONObject(j).optString("text", ""))
                  }
                }
              }.trim()

              if (synthesizedText.isNotBlank()) {
                val extractedMath = extractMathWorking(synthesizedText)
                return AskChatMessage(
                  isUser = false,
                  text = synthesizedText,
                  toolCalls = listOf(toolExecution),
                  mathWorking = extractedMath
                )
              }
            }
          }

          // If second turn failed, return tool summary directly
          return AskChatMessage(
            isUser = false,
            text = "${toolExecution.summaryOutput}\n\n*Filters used: ${toolExecution.filtersUsedDisplay}*",
            toolCalls = listOf(toolExecution)
          )
        } else if (directText.isNotBlank()) {
          // Direct response without tool call (e.g., general query)
          return AskChatMessage(
            isUser = false,
            text = directText,
            toolCalls = emptyList()
          )
        }
      } catch (e: Exception) {
        Log.w(TAG, "Attempt with $modelName failed: ${e.message}")
      }
    }
    return null
  }

  /**
   * Builds the 6 function declarations for Gemini API.
   */
  private fun buildToolsSchema(): JSONArray {
    val declarations = JSONArray()

    // 1. getRevenue
    declarations.put(JSONObject().apply {
      put("name", AskToolNames.GET_REVENUE)
      put("description", "Get clinic group or branch revenue, patient bills count, average bill value (ABV), and procedure units.")
      put("parameters", JSONObject().apply {
        put("type", "OBJECT")
        put("properties", JSONObject().apply {
          put("branch", JSONObject().apply {
            put("type", "STRING")
            put("description", "Clinic branch name, e.g. 'Downtown Executive Clinic', 'Westside Wellness Pavilion', 'North Heights Specialty Center', or 'All Branches'")
          })
          put("period", JSONObject().apply {
            put("type", "STRING")
            put("description", "Date preset or date, e.g. 'today', 'yesterday', '7d', '30d', 'this_month', or '2026-10-04'")
          })
          put("valuationMode", JSONObject().apply {
            put("type", "STRING")
            put("description", "Valuation mode: 'collected' (cash collected) or 'service_value' (rectified package sessions)")
          })
        })
      })
    })

    // 2. getTopServices
    declarations.put(JSONObject().apply {
      put("name", AskToolNames.GET_TOP_SERVICES)
      put("description", "Get the top-performing clinical services and procedures ranked by revenue or session volume.")
      put("parameters", JSONObject().apply {
        put("type", "OBJECT")
        put("properties", JSONObject().apply {
          put("limit", JSONObject().apply {
            put("type", "INTEGER")
            put("description", "Number of top services to return, default 5")
          })
          put("sortBy", JSONObject().apply {
            put("type", "STRING")
            put("description", "'revenue' or 'count'")
          })
          put("branch", JSONObject().apply {
            put("type", "STRING")
            put("description", "Branch name or 'All Branches'")
          })
          put("period", JSONObject().apply {
            put("type", "STRING")
            put("description", "Date preset, e.g. 'today', 'yesterday', '7d', 'this_month'")
          })
        })
      })
    })

    // 3. getDoctorSplit
    declarations.put(JSONObject().apply {
      put("name", AskToolNames.GET_DOCTOR_SPLIT)
      put("description", "Get the breakdown of clinic revenue and patient bill counts by attending doctor.")
      put("parameters", JSONObject().apply {
        put("type", "OBJECT")
        put("properties", JSONObject().apply {
          put("branch", JSONObject().apply {
            put("type", "STRING")
            put("description", "Branch name or 'All Branches'")
          })
          put("period", JSONObject().apply {
            put("type", "STRING")
            put("description", "Date preset, e.g. 'today', 'this_month'")
          })
        })
      })
    })

    // 4. getPharmacyMargin
    declarations.put(JSONObject().apply {
      put("name", AskToolNames.GET_PHARMACY_MARGIN)
      put("description", "Get pharmacy dispensary revenue, drug cost of goods, gross margin in currency and percentage, and top margin products.")
      put("parameters", JSONObject().apply {
        put("type", "OBJECT")
        put("properties", JSONObject().apply {
          put("branch", JSONObject().apply {
            put("type", "STRING")
            put("description", "Branch name or 'All Branches'")
          })
          put("period", JSONObject().apply {
            put("type", "STRING")
            put("description", "Date preset, e.g. 'today', 'this_month'")
          })
          put("searchQuery", JSONObject().apply {
            put("type", "STRING")
            put("description", "Optional search term for product or category")
          })
        })
      })
    })

    // 5. comparePeriods
    declarations.put(JSONObject().apply {
      put("name", AskToolNames.COMPARE_PERIODS)
      put("description", "Compare revenue, bill volume, and average bill value between two periods (e.g. today vs yesterday, this_month vs prior_month).")
      put("parameters", JSONObject().apply {
        put("type", "OBJECT")
        put("properties", JSONObject().apply {
          put("period1", JSONObject().apply {
            put("type", "STRING")
            put("description", "First period, e.g. 'today', 'this_month'")
          })
          put("period2", JSONObject().apply {
            put("type", "STRING")
            put("description", "Second comparison period, e.g. 'yesterday', 'prior_month'")
          })
          put("branch", JSONObject().apply {
            put("type", "STRING")
            put("description", "Branch name or 'All Branches'")
          })
        })
      })
    })

    // 6. listUploads
    declarations.put(JSONObject().apply {
      put("name", AskToolNames.LIST_UPLOADS)
      put("description", "List recent spreadsheet data uploads (service sales, pharmacy margins, consultations).")
      put("parameters", JSONObject().apply {
        put("type", "OBJECT")
        put("properties", JSONObject().apply {
          put("reportType", JSONObject().apply {
            put("type", "STRING")
            put("description", "'all', 'service_sales', 'pharmacy_margin', or 'consultations'")
          })
          put("branch", JSONObject().apply {
            put("type", "STRING")
            put("description", "Branch name or 'All Branches'")
          })
        })
      })
    })

    val tools = JSONArray()
    tools.put(JSONObject().apply {
      put("functionDeclarations", declarations)
    })
    return tools
  }

  /**
   * Deterministic local tool execution engine when offline or when API key is missing.
   * Matches question intent, executes the real tool, computes any math working,
   * and returns structured output.
   */
  private fun executeLocalToolAgent(query: String): AskChatMessage {
    val q = query.lowercase().trim()

    val toolExecution: AskToolExecution
    val responseText: String
    var mathWorking: String? = null

    when {
      // 1. Compare periods
      q.contains("compare") || q.contains("vs") || q.contains("versus") || (q.contains("difference") && (q.contains("yesterday") || q.contains("prior"))) -> {
        val branch = detectBranch(q)
        val p1 = if (q.contains("this month")) "this_month" else "today"
        val p2 = if (q.contains("this month")) "prior_month" else "yesterday"
        val args = mapOf("period1" to p1, "period2" to p2, "branch" to branch)
        toolExecution = toolsHandler.executeTool(AskToolNames.COMPARE_PERIODS, args)

        val json = JSONObject(toolExecution.rawOutputJson)
        val comp = json.getJSONObject("comparison")
        val diffRev = comp.getDouble("revenueDifference")
        val pctRev = comp.getString("revenueGrowthPercentage")
        val diffBills = comp.getInt("billsDifference")

        mathWorking = """
Revenue Difference = Period 1 Revenue - Period 2 Revenue = ${MetricsCalculator.formatCurrency(diffRev)}
Growth Percentage = (${MetricsCalculator.formatCurrency(diffRev)} / Period 2 Revenue) * 100 = $pctRev
Bill Volume Shift = $diffBills bills
""".trimIndent()

        responseText = """
**Executive Period Comparison**
- **Period 1:** ${json.getJSONObject("period1").getString("name")}
- **Period 2:** ${json.getJSONObject("period2").getString("name")}
- **Revenue Delta:** ${MetricsCalculator.formatCurrency(diffRev)} ($pctRev)
- **Patient Bills Delta:** $diffBills bills

${toolExecution.summaryOutput}

*Tool invoked:* `comparePeriods` with filters: ${toolExecution.filtersUsedDisplay}
""".trimIndent()
      }

      // 2. Doctor split
      q.contains("doctor") || q.contains("dr.") || q.contains("physician") -> {
        val branch = detectBranch(q)
        val period = detectPeriod(q)
        val args = mapOf("branch" to branch, "period" to period)
        toolExecution = toolsHandler.executeTool(AskToolNames.GET_DOCTOR_SPLIT, args)

        val json = JSONObject(toolExecution.rawOutputJson)
        val docs = json.getJSONArray("doctors")

        mathWorking = if (docs.length() >= 2) {
          val doc1 = docs.getJSONObject(0)
          val doc2 = docs.getJSONObject(1)
          val rev1 = doc1.getDouble("revenue")
          val rev2 = doc2.getDouble("revenue")
          val spread = rev1 - rev2
          "Leading Doctor Spread: ${doc1.getString("doctorName")} (${MetricsCalculator.formatCurrency(rev1)}) - ${doc2.getString("doctorName")} (${MetricsCalculator.formatCurrency(rev2)}) = ${MetricsCalculator.formatCurrency(spread)}"
        } else null

        responseText = """
**Doctor Revenue Contribution**
${toolExecution.summaryOutput}

*Tool invoked:* `getDoctorSplit` with filters: ${toolExecution.filtersUsedDisplay}
""".trimIndent()
      }

      // 3. Pharmacy margin
      q.contains("pharmacy") || q.contains("rx") || q.contains("margin") || q.contains("drug") || q.contains("medication") -> {
        val branch = detectBranch(q)
        val period = detectPeriod(q)
        val args = mapOf("branch" to branch, "period" to period)
        toolExecution = toolsHandler.executeTool(AskToolNames.GET_PHARMACY_MARGIN, args)

        val json = JSONObject(toolExecution.rawOutputJson)
        val rev = json.getDouble("totalRevenue")
        val cost = json.getDouble("totalCost")
        val margin = json.getDouble("grossMargin")
        val marginPct = json.getString("grossMarginPercentage")

        mathWorking = """
Gross Margin = Total Revenue (${MetricsCalculator.formatCurrency(rev)}) - Total Cost (${MetricsCalculator.formatCurrency(cost)}) = ${MetricsCalculator.formatCurrency(margin)}
Margin Percentage = (${MetricsCalculator.formatCurrency(margin)} / ${MetricsCalculator.formatCurrency(rev)}) * 100 = $marginPct
""".trimIndent()

        responseText = """
**Pharmacy & Dispensary Performance**
- **Dispensary Revenue:** ${MetricsCalculator.formatCurrency(rev)}
- **Cost of Goods:** ${MetricsCalculator.formatCurrency(cost)}
- **Gross Profit Margin:** ${MetricsCalculator.formatCurrency(margin)} ($marginPct)
- **Total Units Sold:** ${json.getInt("totalUnitsSold")} units

${toolExecution.summaryOutput}

*Tool invoked:* `getPharmacyMargin` with filters: ${toolExecution.filtersUsedDisplay}
""".trimIndent()
      }

      // 4. Top services / procedures
      q.contains("top service") || q.contains("services") || q.contains("procedure") || q.contains("treatment") || q.contains("ranking") -> {
        val branch = detectBranch(q)
        val period = detectPeriod(q)
        val sortBy = if (q.contains("count") || q.contains("volume")) "count" else "revenue"
        val args = mapOf("branch" to branch, "period" to period, "sortBy" to sortBy, "limit" to "5")
        toolExecution = toolsHandler.executeTool(AskToolNames.GET_TOP_SERVICES, args)

        responseText = """
**Top Clinical Services ($sortBy)**
${toolExecution.summaryOutput}

*Tool invoked:* `getTopServices` with filters: ${toolExecution.filtersUsedDisplay}
""".trimIndent()
      }

      // 5. Uploads / files
      q.contains("upload") || q.contains("file") || q.contains("spreadsheet") || q.contains("history") || q.contains("feed") -> {
        val branch = detectBranch(q)
        val args = mapOf("branch" to branch, "reportType" to "all")
        toolExecution = toolsHandler.executeTool(AskToolNames.LIST_UPLOADS, args)

        responseText = """
**Billing & EHR Upload Records**
${toolExecution.summaryOutput}

*Tool invoked:* `listUploads` with filters: ${toolExecution.filtersUsedDisplay}
""".trimIndent()
      }

      // 6. Default: Revenue & KPI overview
      else -> {
        val branch = detectBranch(q)
        val period = detectPeriod(q)
        val valuation = if (q.contains("service value") || q.contains("package")) "service_value" else "collected"
        val args = mapOf("branch" to branch, "period" to period, "valuationMode" to valuation)
        toolExecution = toolsHandler.executeTool(AskToolNames.GET_REVENUE, args)

        val json = JSONObject(toolExecution.rawOutputJson)
        val totalRev = json.getDouble("totalCombinedRevenue")
        val clinRev = json.getDouble("clinicalRevenue")
        val pharmRev = json.getDouble("pharmacyRevenue")
        val bills = json.getInt("patientBillsCount")
        val abv = json.getDouble("averageBillValue")

        if (totalRev > 0) {
          mathWorking = """
Total Combined Revenue = Clinical (${MetricsCalculator.formatCurrency(clinRev)}) + Pharmacy (${MetricsCalculator.formatCurrency(pharmRev)}) = ${MetricsCalculator.formatCurrency(totalRev)}
Average Bill Value (ABV) = Total Revenue (${MetricsCalculator.formatCurrency(totalRev)}) / Bills ($bills) = ${MetricsCalculator.formatCurrency(abv)}
""".trimIndent()
        }

        responseText = """
**Executive Revenue Overview**
- **Total Combined Revenue:** ${MetricsCalculator.formatCurrency(totalRev)}
- **Clinical Services:** ${MetricsCalculator.formatCurrency(clinRev)} (${json.getString("clinicalSharePercent")})
- **Pharmacy Dispensary:** ${MetricsCalculator.formatCurrency(pharmRev)} (${json.getString("pharmacySharePercent")})
- **Patient Bills:** $bills bills (ABV: ${MetricsCalculator.formatCurrency(abv)})
- **Valuation Accounting:** ${json.getString("valuationMode")}

*Tool invoked:* `getRevenue` with filters: ${toolExecution.filtersUsedDisplay}
""".trimIndent()
      }
    }

    return AskChatMessage(
      isUser = false,
      text = responseText,
      toolCalls = listOf(toolExecution),
      mathWorking = mathWorking
    )
  }

  private fun detectBranch(query: String): String {
    return when {
      query.contains("downtown", ignoreCase = true) -> "Downtown Executive Clinic"
      query.contains("westside", ignoreCase = true) -> "Westside Wellness Pavilion"
      query.contains("north", ignoreCase = true) -> "North Heights Specialty Center"
      else -> "All Branches"
    }
  }

  private fun detectPeriod(query: String): String {
    return when {
      query.contains("yesterday", ignoreCase = true) -> "yesterday"
      query.contains("7 day", ignoreCase = true) || query.contains("week", ignoreCase = true) -> "7d"
      query.contains("30 day", ignoreCase = true) -> "30d"
      query.contains("this month", ignoreCase = true) || query.contains("month", ignoreCase = true) -> "this_month"
      else -> "today"
    }
  }

  private fun extractMathWorking(text: String): String? {
    if (!text.contains("Math Working", ignoreCase = true)) return null
    val marker = text.lines().find { it.contains("Math Working", ignoreCase = true) } ?: return null
    val startIndex = text.indexOf(marker) + marker.length
    val snippet = text.substring(startIndex).trim()
    return snippet.lines().take(6).joinToString("\n").trim()
  }
}
