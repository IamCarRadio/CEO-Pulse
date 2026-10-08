package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PackageValueSetting
import com.example.model.RawClinicRow
import com.example.model.ReconciliationSummary
import com.example.model.UnmappedPackageService
import com.example.model.ValuationMode
import com.example.ui.theme.BorderMedium
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedBg
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueBg
import com.example.ui.theme.RoleAllowedBg
import com.example.ui.theme.RoleAllowedGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealLightContainer
import com.example.ui.theme.TealOnContainer
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextHeadline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import java.text.DecimalFormat

private val numberFormat = DecimalFormat("#,##0.00")
private val countFormat = DecimalFormat("#,##0")
private fun formatCurrency(amount: Double): String = "Rs. " + numberFormat.format(amount)

/**
 * 1. Valuation Mode Segmented Toggle Bar with settings shortcut
 */
@Composable
fun ValuationModeToggleBar(
  currentMode: ValuationMode,
  onModeSelected: (ValuationMode) -> Unit,
  onOpenSettings: () -> Unit,
  unmappedCount: Int,
  canOpenSettings: Boolean = true,
  modifier: Modifier = Modifier
) {
  Surface(
    color = SurfaceCard,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Revenue Valuation Mode",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextHeadline
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            color = if (currentMode == ValuationMode.SERVICE_VALUE) TealLightContainer else SurfaceSubtle,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = if (currentMode == ValuationMode.SERVICE_VALUE) "Enrolled Sessions Valued" else "Cash Invoiced Only",
              style = MaterialTheme.typography.labelSmall,
              color = if (currentMode == ValuationMode.SERVICE_VALUE) TealPrimary else TextMuted,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
        Text(
          text = currentMode.description,
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
          modifier = Modifier.padding(top = 2.dp)
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        // Segmented Switch
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceSubtle)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .padding(3.dp)
        ) {
          ValuationToggleChip(
            label = "Collected",
            isSelected = currentMode == ValuationMode.COLLECTED,
            testTag = "toggle_mode_collected",
            onClick = { onModeSelected(ValuationMode.COLLECTED) }
          )
          ValuationToggleChip(
            label = "Service-Value",
            isSelected = currentMode == ValuationMode.SERVICE_VALUE,
            testTag = "toggle_mode_service_value",
            onClick = { onModeSelected(ValuationMode.SERVICE_VALUE) }
          )
        }

        if (canOpenSettings) {
          Spacer(modifier = Modifier.width(8.dp))

          // Settings Button to configure per-session package values
          OutlinedButton(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("open_package_values_settings_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Package Values",
              tint = TealPrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Package Values",
              style = MaterialTheme.typography.labelSmall,
              color = TealPrimary
            )
            if (unmappedCount > 0) {
              Spacer(modifier = Modifier.width(4.dp))
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .background(WarningAmber),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = unmappedCount.toString(),
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                  fontSize = 10.sp
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ValuationToggleChip(
  label: String,
  isSelected: Boolean,
  testTag: String,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) TealPrimary else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 8.dp)
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
      color = if (isSelected) Color.White else TextMuted
    )
  }
}

/**
 * 2. Reconciliation Summary Line Card:
 * Collected + Package Value = Service-Value Total
 */
@Composable
fun ReconciliationSummaryCard(
  reconciliation: ReconciliationSummary,
  onAuditPackageRows: (List<RawClinicRow>) -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = SurfaceCard,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = RoleAllowedGreen,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Package Rectification & Audit Reconciliation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextHeadline
          )
        }

        Surface(
          color = if (reconciliation.isReconciled) RoleAllowedBg else WarningAmberBg,
          shape = RoundedCornerShape(6.dp),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (reconciliation.isReconciled) RoleAllowedGreen.copy(alpha = 0.3f) else WarningAmber.copy(alpha = 0.3f)
          )
        ) {
          Text(
            text = if (reconciliation.isReconciled) "100% Reconciled" else "Variance Detected",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (reconciliation.isReconciled) RoleAllowedGreen else WarningAmber,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // The core reconciliation line formula display
      Surface(
        color = TealLightContainer.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Collected
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "Collected Revenue", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
              text = formatCurrency(reconciliation.collectedRevenue),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextHeadline
            )
            Text(text = "${reconciliation.paidSessionCount} Paid Sessions", style = MaterialTheme.typography.labelSmall, color = TextMuted)
          }

          Text(
            text = "+",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TealPrimary,
            modifier = Modifier.padding(horizontal = 8.dp)
          )

          // Package Value Rectified
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "Package Value", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
              text = formatCurrency(reconciliation.packageRectifiedValue),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TealPrimary
            )
            Text(text = "${reconciliation.packageSessionCount} Enrolled (Rs. 0)", style = MaterialTheme.typography.labelSmall, color = TealOnContainer)
          }

          Text(
            text = "=",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TealPrimary,
            modifier = Modifier.padding(horizontal = 8.dp)
          )

          // Service-Value Total
          Column(modifier = Modifier.weight(1.1f)) {
            Text(text = "Service-Value Total", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
              text = formatCurrency(reconciliation.serviceValueTotal),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = RoleAllowedGreen
            )
            Text(text = "${reconciliation.totalSessionCount} Total Clinical Encounters", style = MaterialTheme.typography.labelSmall, color = TextMuted)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Footer action and breakdown
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Rs. 0 invoices represent prepaid packages or membership enrollments, accounted at defined session rates.",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
          modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        TextButton(
          onClick = { onAuditPackageRows(reconciliation.packageRows) },
          modifier = Modifier.testTag("audit_package_rows_btn")
        ) {
          Text(
            text = "Audit ${reconciliation.packageSessionCount} Package Rows",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TealPrimary
          )
        }
      }
    }
  }
}

/**
 * 3. Warning banner if Rs. 0 rows exist with no defined package values
 */
@Composable
fun UnmappedPackageWarningBanner(
  unmappedServices: List<UnmappedPackageService>,
  onOpenSettings: () -> Unit,
  onQuickDefine: (UnmappedPackageService) -> Unit,
  canOpenSettings: Boolean = true,
  modifier: Modifier = Modifier
) {
  if (unmappedServices.isEmpty()) return

  Surface(
    color = WarningAmberBg,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = WarningAmber,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${unmappedServices.size} Unmapped Package Service Lines Detected (${unmappedServices.sumOf { it.sessionCount }} sessions)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = WarningAmber
          )
        }

        if (canOpenSettings) {
          OutlinedButton(
            onClick = onOpenSettings,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = "Configure Rules",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = WarningAmber
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "These enrolled patient sessions appeared with Rs. 0 bills and have no defined per-session rate. Define their value so nothing is silently valued at zero!",
        style = MaterialTheme.typography.bodySmall,
        color = TextHeadline
      )

      Spacer(modifier = Modifier.height(12.dp))

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        unmappedServices.take(4).forEach { unmapped ->
          Surface(
            color = SurfaceCard,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = unmapped.serviceName,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = TextHeadline
                )
                Text(
                  text = "${unmapped.sessionCount} enrolled sessions · Last seen: ${unmapped.lastSeenDate}",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextMuted
                )
              }

              Button(
                onClick = { onQuickDefine(unmapped) },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarningAmber)
              ) {
                Text(
                  text = "Set Per-Session Rs.",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * 4. Executive Modal Table for Defining Package Valuation Rules
 */
@Composable
fun PackageValuesSettingsDialog(
  packageValues: List<PackageValueSetting>,
  unmappedServices: List<UnmappedPackageService>,
  onSaveRule: (serviceName: String, perSessionValue: Double, category: String, notes: String) -> Unit,
  onDeleteRule: (serviceName: String) -> Unit,
  onDismiss: () -> Unit
) {
  var serviceNameInput by remember { mutableStateOf("") }
  var categoryInput by remember { mutableStateOf("Skin & Dermatology") }
  var perSessionValueInput by remember { mutableStateOf("") }
  var notesInput by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isAddingNew by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Package Valuation Rules",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextHeadline
            )
            Text(
              text = "Define per-session credit for enrolled Rs. 0 patients",
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted
            )
          }
        }

        IconButton(onClick = onDismiss) {
          Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Explanation
        Text(
          text = "When clinics sell packages (e.g. 5 laser sessions for upfront fee), subsequent appointments are billed at Rs. 0. Define each package's per-session economic value here so the CEO dashboard accurately reflects clinical productivity and service-value revenue.",
          style = MaterialTheme.typography.bodySmall,
          color = TextBody
        )

        // Add / Edit Rule Form
        Surface(
          color = SurfaceSubtle,
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Add / Edit Valuation Rule",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = TealPrimary
            )

            // Quick populate unmapped dropdown / chips if available
            if (unmappedServices.isNotEmpty()) {
              Text(
                text = "Unmapped candidates (tap to fill):",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
              )
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                unmappedServices.forEach { candidate ->
                  Surface(
                    color = WarningAmberBg,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.3f)),
                    modifier = Modifier.clickable {
                      serviceNameInput = candidate.serviceName
                      categoryInput = candidate.category
                    }
                  ) {
                    Text(
                      text = candidate.serviceName,
                      style = MaterialTheme.typography.labelSmall,
                      color = WarningAmber,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }
              }
            }

            OutlinedTextField(
              value = serviceNameInput,
              onValueChange = { serviceNameInput = it; errorMessage = null },
              label = { Text("Procedure / Service Name") },
              placeholder = { Text("e.g. Full-Face Laser Genesis Resurfacing") },
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("rule_service_name_input")
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              OutlinedTextField(
                value = perSessionValueInput,
                onValueChange = { perSessionValueInput = it; errorMessage = null },
                label = { Text("Per-Session Rs.") },
                placeholder = { Text("350.00") },
                singleLine = true,
                modifier = Modifier
                  .weight(1f)
                  .testTag("rule_value_input")
              )

              OutlinedTextField(
                value = categoryInput,
                onValueChange = { categoryInput = it },
                label = { Text("Category") },
                singleLine = true,
                modifier = Modifier.weight(1f)
              )
            }

            OutlinedTextField(
              value = notesInput,
              onValueChange = { notesInput = it },
              label = { Text("Notes / Package Description (Optional)") },
              placeholder = { Text("e.g. 5-Session Prepaid Laser Protocol") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
              Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed
              )
            }

            Button(
              onClick = {
                val valNum = perSessionValueInput.toDoubleOrNull()
                if (serviceNameInput.isBlank()) {
                  errorMessage = "Service name is required."
                } else if (valNum == null || valNum <= 0.0) {
                  errorMessage = "Please enter a valid positive per-session value."
                } else {
                  onSaveRule(serviceNameInput.trim(), valNum, categoryInput.trim(), notesInput.trim())
                  serviceNameInput = ""
                  perSessionValueInput = ""
                  notesInput = ""
                  errorMessage = null
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("save_rule_btn")
            ) {
              Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save / Update Package Rule")
            }
          }
        }

        // Active Rules Table
        Text(
          text = "Active Package Rules (${packageValues.size} defined):",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = TextHeadline
        )

        Surface(
          color = SurfaceCard,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(modifier = Modifier.padding(bottom = 6.dp)) {
              Text(text = "Service Name", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(180.dp))
              Text(text = "Category", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(120.dp))
              Text(text = "Per-Session Value", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(110.dp))
              Text(text = "Notes", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(160.dp))
              Text(text = "Action", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(60.dp))
            }
            HorizontalDivider(color = BorderMedium, thickness = 1.dp)

            // Rows
            packageValues.forEachIndexed { idx, rule ->
              Row(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = rule.serviceName,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  color = TextHeadline,
                  modifier = Modifier.width(180.dp)
                )
                Text(
                  text = rule.category,
                  style = MaterialTheme.typography.bodySmall,
                  color = TextMuted,
                  modifier = Modifier.width(120.dp)
                )
                Text(
                  text = formatCurrency(rule.perSessionValue),
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = TealPrimary,
                  modifier = Modifier.width(110.dp)
                )
                Text(
                  text = rule.notes.ifBlank { "—" },
                  style = MaterialTheme.typography.bodySmall,
                  color = TextMuted,
                  modifier = Modifier.width(160.dp),
                  maxLines = 1
                )
                IconButton(
                  onClick = { onDeleteRule(rule.serviceName) },
                  modifier = Modifier.size(36.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Rule",
                    tint = ErrorRed,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
              if (idx < packageValues.size - 1) {
                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
      ) {
        Text("Done")
      }
    }
  )
}
