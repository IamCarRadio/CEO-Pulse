package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiSummaryData
import com.example.model.AiSummaryUiState
import com.example.model.ValuationMode
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.RoleAllowedGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealLightContainer
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextHeadline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiSummaryCard(
  uiState: AiSummaryUiState,
  onRegenerate: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var isExpanded by remember { mutableStateOf(true) }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color.White,
    border = androidx.compose.foundation.BorderStroke(1.5.dp, TealAccent.copy(alpha = 0.35f)),
    shadowElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("ai_summary_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Header Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(
                Brush.linearGradient(
                  colors = listOf(TealPrimary, Color(0xFF6366F1))
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = "Gemini AI",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Executive AI Briefing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextHeadline
              )
              Spacer(modifier = Modifier.width(8.dp))
              // Privacy Badge
              Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFCBD5E1))
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Private",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(10.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = "Aggregated Metrics Only",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = Color(0xFF475569)
                  )
                }
              }
            }

            Text(
              text = "Gemini synthesized analysis • Zero patient PII transmitted",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
              fontSize = 11.sp
            )
          }
        }

        // Action Controls
        Row(verticalAlignment = Alignment.CenterVertically) {
          when (uiState) {
            is AiSummaryUiState.Success -> {
              IconButton(
                onClick = {
                  val textToCopy = buildClipboardText(uiState.data)
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("CEO Pulse AI Briefing", textToCopy)
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "AI briefing copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                  .size(32.dp)
                  .testTag("copy_ai_summary_btn")
              ) {
                Icon(
                  imageVector = Icons.Filled.ContentCopy,
                  contentDescription = "Copy Summary",
                  tint = Color(0xFF64748B),
                  modifier = Modifier.size(16.dp)
                )
              }

              Spacer(modifier = Modifier.width(4.dp))
            }
            else -> {}
          }

          IconButton(
            onClick = onRegenerate,
            enabled = uiState !is AiSummaryUiState.Loading,
            modifier = Modifier
              .size(32.dp)
              .testTag("regenerate_ai_summary_btn")
          ) {
            Icon(
              imageVector = Icons.Filled.Refresh,
              contentDescription = "Regenerate Summary",
              tint = TealPrimary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Status Bar (Caching info / live indicator)
      when (uiState) {
        is AiSummaryUiState.Success -> {
          val data = uiState.data
          val dateFmt = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
          val generatedTime = dateFmt.format(Date(data.generatedAtMillis))

          FlowRow(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Cache status badge
            Surface(
              color = if (data.isCached) Color(0xFFEFF6FF) else Color(0xFFF0FDF4),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (data.isCached) Color(0xFFBFDBFE) else Color(0xFFBBF7D0)
              )
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = if (data.isCached) Icons.Filled.Info else Icons.Filled.CheckCircle,
                  contentDescription = null,
                  tint = if (data.isCached) InfoBlue else RoleAllowedGreen,
                  modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (data.isCached) "Cached for ${data.formattedPeriod} ($generatedTime)" else "Live Generated ($generatedTime)",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = if (data.isCached) Color(0xFF1E40AF) else Color(0xFF166534)
                )
              }
            }

            // Valuation Mode badge
            Surface(
              color = if (data.valuationMode == ValuationMode.SERVICE_VALUE) TealLightContainer else Color(0xFFF8FAFC),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
              Text(
                text = if (data.valuationMode == ValuationMode.SERVICE_VALUE) "Service-Value Mode (Package Valued)" else "Collected Revenue Mode",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = TextBody,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
        else -> {}
      }

      // Content Body based on UI State
      when (uiState) {
        is AiSummaryUiState.Loading -> {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp)
              .testTag("ai_summary_loading"),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              CircularProgressIndicator(
                color = TealPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
              )
              Text(
                text = "Synthesizing executive briefing via Gemini...",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextHeadline
              )
              Text(
                text = "Processing pre-computed metrics only • Adhering to strict audit rules",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 11.sp
              )
            }
          }
        }

        is AiSummaryUiState.Error -> {
          Surface(
            color = Color(0xFFFEF2F2),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = "Error",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Unable to complete AI analysis",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF991B1B)
                )
                Text(
                  text = uiState.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFB91C1C)
                )
              }
              TextButton(onClick = onRegenerate) {
                Text("Retry", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        is AiSummaryUiState.Success -> {
          val data = uiState.data

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // -------------------------------------------------------------
            // SECTION 1: 5-LINE PLAIN-ENGLISH CEO SUMMARY
            // -------------------------------------------------------------
            Surface(
              color = Color(0xFFF8FAFC),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_summary_five_line_section")
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(bottom = 8.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(TealPrimary)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "5-LINE CEO EXECUTIVE SUMMARY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TealPrimary,
                    letterSpacing = 0.5.sp
                  )
                }

                data.fiveLineSummary.forEachIndexed { index, line ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    Text(
                      text = "${index + 1}.",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      color = TextBody,
                      modifier = Modifier.width(20.dp)
                    )
                    Text(
                      text = line,
                      style = MaterialTheme.typography.bodyMedium,
                      color = TextHeadline,
                      lineHeight = 20.sp
                    )
                  }
                }
              }
            }

            // -------------------------------------------------------------
            // SECTION 2: WHAT IMPROVED, WHAT DROPPED, AND WHY
            // -------------------------------------------------------------
            Surface(
              color = Color.White,
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_summary_improved_dropped_section")
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Text(
                  text = "PERFORMANCE MOVEMENTS & ROOT CAUSES",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = TextBody,
                  letterSpacing = 0.5.sp
                )

                // What Improved
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0FDF4), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                  verticalAlignment = Alignment.Top
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = "Improved",
                    tint = RoleAllowedGreen,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = "What Improved",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF166534)
                    )
                    Text(
                      text = data.whatImproved,
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFF14532D),
                      lineHeight = 18.sp
                    )
                  }
                }

                // What Dropped
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFBEB), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                  verticalAlignment = Alignment.Top
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = "Dropped",
                    tint = WarningAmber,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = "What Dropped",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF92400E)
                    )
                    Text(
                      text = data.whatDropped,
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFF78350F),
                      lineHeight = 18.sp
                    )
                  }
                }

                // Why Analysis
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFF6FF), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFDBEAFE), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                  verticalAlignment = Alignment.Top
                ) {
                  Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Why",
                    tint = InfoBlue,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = "Why (Drivers & Contributing Factors)",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF1E40AF)
                    )
                    Text(
                      text = data.whyAnalysis,
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFF1E3A8A),
                      lineHeight = 18.sp
                    )
                  }
                }
              }
            }

            // -------------------------------------------------------------
            // SECTION 3: 3 SUGGESTED ACTIONS
            // -------------------------------------------------------------
            Surface(
              color = Color(0xFFFAF5FF),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3E8FF)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_summary_actions_section")
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(bottom = 8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF9333EA),
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "3 SUGGESTED STRATEGIC ACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7E22CE),
                    letterSpacing = 0.5.sp
                  )
                }

                data.suggestedActions.forEachIndexed { idx, action ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    Box(
                      modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE9D5FF)),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = "${idx + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B21A8)
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                      text = action,
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color(0xFF3B0764),
                      lineHeight = 20.sp
                    )
                  }
                }
              }
            }

            // Audit Guard Disclosure Footer
            Text(
              text = "Strict Compliance Note: Gemini receives only pre-computed aggregates. Numbers are strictly quoted without extrapolation.",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 10.sp,
              color = TextMuted,
              modifier = Modifier.padding(horizontal = 4.dp)
            )
          }
        }

        is AiSummaryUiState.Idle -> {
          OutlinedButton(
            onClick = onRegenerate,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = TealPrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Executive AI Briefing with Gemini")
          }
        }
      }
    }
  }
}

private fun buildClipboardText(data: AiSummaryData): String {
  return buildString {
    appendLine("=== CEO PULSE: EXECUTIVE AI BRIEFING ===")
    appendLine("Period: ${data.formattedPeriod} | Valuation: ${data.valuationMode.name}")
    appendLine()
    appendLine("5-LINE CEO SUMMARY:")
    data.fiveLineSummary.forEachIndexed { i, line ->
      appendLine("${i + 1}. $line")
    }
    appendLine()
    appendLine("MOVEMENTS & CAUSES:")
    appendLine("• Improved: ${data.whatImproved}")
    appendLine("• Dropped: ${data.whatDropped}")
    appendLine("• Why: ${data.whyAnalysis}")
    appendLine()
    appendLine("3 SUGGESTED ACTIONS:")
    data.suggestedActions.forEachIndexed { i, act ->
      appendLine("${i + 1}. $act")
    }
    appendLine()
    appendLine("Strict Guard: Generated strictly from pre-computed metrics. No raw patient records or PII transmitted.")
  }
}
