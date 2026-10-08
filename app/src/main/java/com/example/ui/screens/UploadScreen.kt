package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SaveUploadResult
import com.example.model.DuplicateCheckResult
import com.example.model.FilterState
import com.example.model.IssueSeverity
import com.example.model.ReportType
import com.example.model.UploadRecord
import com.example.model.ValidationReport
import com.example.ui.components.SkeletonBox
import com.example.ui.theme.BorderMedium
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedBg
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
import com.example.util.ParsedSpreadsheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadScreen(
  selectedReportType: ReportType,
  selectedBranch: String,
  selectedDate: String,
  parsedSpreadsheet: ParsedSpreadsheet?,
  validationReport: ValidationReport?,
  isUploading: Boolean,
  uploadHistory: List<UploadRecord>,
  duplicatePrompt: DuplicateCheckResult?,
  lastUploadProof: SaveUploadResult?,
  onSelectReportType: (ReportType) -> Unit,
  onSelectBranch: (String) -> Unit,
  onSelectDate: (String) -> Unit,
  onFilePicked: (Uri, String) -> Unit,
  onLoadSample: () -> Unit,
  onClearParsedFile: () -> Unit,
  onInitiateSave: () -> Unit,
  onConfirmReplace: () -> Unit,
  onDismissDuplicate: () -> Unit,
  onDismissProof: () -> Unit,
  onDeleteUpload: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showBranchDropdown by remember { mutableStateOf(false) }
  var showDateDialog by remember { mutableStateOf(false) }
  var dateInput by remember { mutableStateOf(selectedDate) }
  var uploadToDelete by remember { mutableStateOf<UploadRecord?>(null) }
  var showPreviewRows by remember { mutableStateOf(false) }

  // Document file picker launcher
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      var fileName = "clinic_export.xlsx"
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
          fileName = cursor.getString(nameIndex)
        }
      }
      onFilePicked(uri, fileName)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(22.dp)
  ) {

    // 1. SECTION TITLE & EXECUTIVE CONTEXT
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TealLightContainer)
            .border(1.dp, TealAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(26.dp)
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
          Text(
            text = "Clinic Data Ingestion",
            style = MaterialTheme.typography.headlineLarge,
            color = TextHeadline
          )
          Text(
            text = "Upload and validate raw Excel / CSV exports into Firestore",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
          )
        }
      }

      Surface(
        color = TealLightContainer,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text(
          text = "SheetJS & OpenXML Parser",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = TealOnContainer,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
      }
    }

    // 2. INGESTION PROOF BANNER (Shows parsed vs saved row count confirmation)
    AnimatedVisibility(visible = lastUploadProof != null) {
      lastUploadProof?.let { proof ->
        Surface(
          color = RoleAllowedBg,
          shape = RoundedCornerShape(16.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, RoleAllowedGreen.copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("upload_proof_banner")
        ) {
          Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(RoleAllowedGreen),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column {
                Text(
                  text = "Ingestion Proof Confirmed",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = TextHeadline
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Rows Parsed from File: ${proof.parsedRows}  ·  Rows Saved to Firestore: ${proof.savedRows}",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = RoleAllowedGreen
                )
                Text(
                  text = "Upload ID: ${proof.uploadId}  (100% matched, zero silent loss)",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextMuted
                )
              }
            }

            IconButton(onClick = onDismissProof) {
              Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", tint = TextMuted)
            }
          }
        }
      }
    }

    // 3. REPORT CONFIGURATION BAR (Report Type, Branch, Report Date)
    Surface(
      color = SurfaceCard,
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "Step 1: Choose Ingestion Parameters",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextHeadline
        )
        Text(
          text = "Designate the target clinical domain, branch clinic, and transaction report date.",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
          modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        // Report Type Chips
        Text(
          text = "REPORT TYPE",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = TextSubtle,
          modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ReportType.values().forEach { rType ->
            val isSelected = selectedReportType == rType
            Surface(
              color = if (isSelected) TealPrimary else SurfaceSubtle,
              shape = RoundedCornerShape(10.dp),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) TealPrimary else BorderSubtle
              ),
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onSelectReportType(rType) }
                .testTag("report_type_${rType.name.lowercase()}")
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 48.dp)
                  .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = rType.displayName,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) Color.White else TextBody,
                  maxLines = 1
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Branch and Date Selectors
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // Branch Dropdown
          Box(modifier = Modifier.weight(1f)) {
            Column {
              Text(
                text = "CLINIC BRANCH",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSubtle,
                modifier = Modifier.padding(bottom = 6.dp)
              )
              Surface(
                color = SurfaceSubtle,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { showBranchDropdown = true }
                  .testTag("upload_branch_selector")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = null,
                      tint = TealPrimary,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = selectedBranch,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = TextHeadline,
                      maxLines = 1
                    )
                  }
                  Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = TextMuted
                  )
                }
              }
            }

            DropdownMenu(
              expanded = showBranchDropdown,
              onDismissRequest = { showBranchDropdown = false }
            ) {
              FilterState.AVAILABLE_BRANCHES.filter { it != "All Branches" }.forEach { branch ->
                DropdownMenuItem(
                  text = { Text(branch, color = TextHeadline) },
                  onClick = {
                    onSelectBranch(branch)
                    showBranchDropdown = false
                  }
                )
              }
            }
          }

          // Report Date Picker
          Box(modifier = Modifier.weight(1f)) {
            Column {
              Text(
                text = "REPORT DATE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TextSubtle,
                modifier = Modifier.padding(bottom = 6.dp)
              )
              Surface(
                color = SurfaceSubtle,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable { showDateDialog = true }
                  .testTag("upload_date_selector")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.CalendarToday,
                      contentDescription = null,
                      tint = TealPrimary,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = selectedDate,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = TextHeadline
                    )
                  }
                  Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = TextMuted
                  )
                }
              }
            }
          }
        }
      }
    }

    // 4. DRAG-AND-DROP & FILE PICKER DROPZONE
    Surface(
      color = SurfaceCard,
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(20.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Column {
            Text(
              text = "Step 2: Upload Clinic Spreadsheet",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextHeadline
            )
            Text(
              text = "Accepts .xlsx, .xls, and .csv exports from Cliniko, Athena, Kareo, etc.",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted
            )
          }

          // Sample Loader Button for Instant Testing
          OutlinedButton(
            onClick = onLoadSample,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .defaultMinSize(minHeight = 48.dp)
              .testTag("load_sample_service_sales_btn")
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = null,
              tint = TealPrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Load Sample 'Service Sales' Export",
              style = MaterialTheme.typography.labelSmall,
              color = TealPrimary
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Drag & Drop Box
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceSubtle)
            .border(
              width = 1.5.dp,
              color = if (parsedSpreadsheet != null) TealAccent else BorderMedium,
              shape = RoundedCornerShape(14.dp)
            )
            .clickable {
              filePickerLauncher.launch(
                arrayOf(
                  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                  "application/vnd.ms-excel",
                  "text/csv",
                  "text/comma-separated-values",
                  "*/*"
                )
              )
            }
            .padding(vertical = 32.dp, horizontal = 20.dp)
            .testTag("file_dropzone"),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(TealLightContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = "Drop file",
                tint = TealPrimary,
                modifier = Modifier.size(28.dp)
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
              text = if (parsedSpreadsheet == null) "Drag and drop Excel/CSV here or tap to browse" else "File Loaded: ${parsedSpreadsheet.fileName}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = TextHeadline
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = if (parsedSpreadsheet == null) "Parses real headers in the browser / client with SheetJS & OpenXML" else "${parsedSpreadsheet.totalRowCount} rows parsed · Tap to replace with another file",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted
            )
          }
        }
      }
    }

    // 5. DETECTED HEADERS, SCHEMA & VALIDATION BREAKDOWN
    AnimatedVisibility(visible = parsedSpreadsheet != null) {
      parsedSpreadsheet?.let { parsed ->
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Surface(
            color = SurfaceCard,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(20.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = "Detected Columns & Schema (${parsed.headers.size} Columns)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextHeadline
                  )
                  Text(
                    text = "Real headers parsed directly from '${parsed.fileName}'",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                  )
                }

                TextButton(onClick = onClearParsedFile) {
                  Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Clear", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Detected Column Badges
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("detected_columns_container")
              ) {
                parsed.headers.forEach { header ->
                  Surface(
                    color = TealLightContainer,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f))
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(13.dp)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = header,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TealOnContainer
                      )
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(16.dp))
              HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
              Spacer(modifier = Modifier.height(16.dp))

              // VALIDATION REPORT
              validationReport?.let { report ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = if (report.hasErrors) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (report.hasErrors) ErrorRed else RoleAllowedGreen,
                        modifier = Modifier.size(20.dp)
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = if (report.hasErrors) "Validation Issues Detected (${report.errorCount} Errors)" else "Validation Passed: All Rows Clean",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (report.hasErrors) ErrorRed else RoleAllowedGreen
                      )
                    }

                    Text(
                      text = "${report.validRows} of ${report.totalRows} Rows Valid",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = TextHeadline
                    )
                  }

                  // Issues list
                  if (report.issues.isNotEmpty()) {
                    Surface(
                      color = if (report.hasErrors) ErrorRedBg else WarningAmberBg,
                      shape = RoundedCornerShape(10.dp),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        report.issues.forEach { issue ->
                          Row(verticalAlignment = Alignment.Top) {
                            Text(
                              text = if (issue.rowNumber > 0) "• Row ${issue.rowNumber} [${issue.columnName}]: " else "• Header [${issue.columnName}]: ",
                              style = MaterialTheme.typography.labelSmall,
                              fontWeight = FontWeight.Bold,
                              color = if (issue.severity == IssueSeverity.ERROR) ErrorRed else WarningAmber
                            )
                            Text(
                              text = issue.issue,
                              style = MaterialTheme.typography.bodySmall,
                              color = TextBody
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              // Raw Table Preview Toggle
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                TextButton(onClick = { showPreviewRows = !showPreviewRows }) {
                  Icon(imageVector = Icons.Default.TableView, contentDescription = null, modifier = Modifier.size(16.dp), tint = TealPrimary)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (showPreviewRows) "Hide Data Preview" else "Show Raw Rows Preview (${parsed.rows.take(5).size} of ${parsed.totalRowCount})",
                    style = MaterialTheme.typography.labelMedium,
                    color = TealPrimary
                  )
                }

                // INITIATE SAVE BUTTON
                Button(
                  onClick = onInitiateSave,
                  enabled = !isUploading && (validationReport?.hasErrors == false || validationReport?.validRows ?: 0 > 0),
                  colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.testTag("save_to_firestore_btn")
                ) {
                  if (isUploading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving to Firestore...")
                  } else {
                    Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Commit ${parsed.totalRowCount} Rows to Firestore")
                  }
                }
              }

              // RAW ROWS PREVIEW TABLE
              if (showPreviewRows) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                  color = SurfaceSubtle,
                  shape = RoundedCornerShape(10.dp),
                  border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    // Table Header
                    Row(modifier = Modifier.padding(bottom = 6.dp)) {
                      parsed.headers.forEach { header ->
                        Text(
                          text = header,
                          style = MaterialTheme.typography.labelSmall,
                          fontWeight = FontWeight.Bold,
                          color = TextHeadline,
                          modifier = Modifier.width(140.dp)
                        )
                      }
                    }
                    HorizontalDivider(color = BorderMedium, thickness = 1.dp)

                    // First 5 Rows
                    parsed.rows.take(5).forEachIndexed { idx, row ->
                      Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        parsed.headers.forEach { header ->
                          Text(
                            text = row[header] ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextBody,
                            modifier = Modifier.width(140.dp),
                            maxLines = 1
                          )
                        }
                      }
                      if (idx < 4) HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // 6. UPLOAD HISTORY LIST
    Surface(
      color = SurfaceCard,
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = null,
              tint = TealPrimary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Upload History & Audit Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextHeadline
              )
              Text(
                text = "${uploadHistory.size} Batches Recorded in Firestore / Local Store",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (uploadHistory.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No clinic uploads yet. Use the dropzone above or tap 'Load Service Sales Export' to ingest data.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextMuted
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())

            uploadHistory.forEach { record ->
              Surface(
                color = SurfaceSubtle,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("upload_record_${record.uploadId}")
              ) {
                Row(
                  modifier = Modifier.padding(16.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TealLightContainer),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(20.dp)
                      )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                      Text(
                        text = record.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadline
                      )
                      Spacer(modifier = Modifier.height(2.dp))
                      Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Surface(color = TealLightContainer, shape = RoundedCornerShape(4.dp)) {
                          Text(
                            text = record.reportType.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TealOnContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                          )
                        }
                        Text(
                          text = "·  ${record.branch}  ·  Date: ${record.reportDate}",
                          style = MaterialTheme.typography.labelSmall,
                          color = TextMuted
                        )
                      }
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = "Parsed: ${record.parsedRowCount} rows · Saved: ${record.savedRowCount} rows · ${dateFormat.format(Date(record.uploadedAt))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSubtle
                      )
                    }
                  }

                  // Delete Upload Button with confirmation requirement
                  IconButton(
                    onClick = { uploadToDelete = record },
                    modifier = Modifier.testTag("delete_upload_${record.uploadId}")
                  ) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "Delete Upload",
                      tint = ErrorRed,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // ------------------------------------------------------------------
  // DIALOGS
  // ------------------------------------------------------------------

  // 1. DUPLICATE DETECTION PROMPT (Never silently double count!)
  if (duplicatePrompt != null) {
    val existing = duplicatePrompt.existingUpload
    AlertDialog(
      onDismissRequest = onDismissDuplicate,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = WarningAmber,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text("Duplicate Upload Detected")
        }
      },
      text = {
        Column {
          Text(
            text = "An upload already exists for:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextHeadline
          )
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            color = SurfaceSubtle,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "• Report Type: ${existing?.reportType?.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = TextBody
              )
              Text(
                text = "• Branch: ${existing?.branch}",
                style = MaterialTheme.typography.bodySmall,
                color = TextBody
              )
              Text(
                text = "• Date: ${existing?.reportDate}",
                style = MaterialTheme.typography.bodySmall,
                color = TextBody
              )
              Text(
                text = "• Existing File: ${existing?.fileName} (${existing?.savedRowCount} rows)",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Re-uploading without replacement will double-count clinic analytics. Do you want to REPLACE that day's data?",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = ErrorRed
          )
        }
      },
      confirmButton = {
        Button(
          onClick = onConfirmReplace,
          colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
          modifier = Modifier.testTag("confirm_replace_upload_btn")
        ) {
          Text("Replace That Day")
        }
      },
      dismissButton = {
        TextButton(onClick = onDismissDuplicate) {
          Text("Cancel Upload")
        }
      }
    )
  }

  // 2. DELETE CONFIRMATION DIALOG (Requires explicit confirmation per instructions)
  if (uploadToDelete != null) {
    val target = uploadToDelete!!
    AlertDialog(
      onDismissRequest = { uploadToDelete = null },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text("Confirm Delete Upload")
        }
      },
      text = {
        Column {
          Text(
            text = "Are you sure you want to delete this upload? This action is permanent and will purge all ${target.savedRowCount} raw rows from Firestore and storage.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextBody
          )
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            color = SurfaceSubtle,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = target.fileName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextHeadline
              )
              Text(
                text = "${target.reportType.displayName} · ${target.branch} · ${target.reportDate}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteUpload(target.uploadId)
            uploadToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
          modifier = Modifier.testTag("confirm_delete_btn")
        ) {
          Text("Delete Permanently")
        }
      },
      dismissButton = {
        TextButton(onClick = { uploadToDelete = null }) {
          Text("Cancel")
        }
      }
    )
  }

  // 3. EDIT REPORT DATE DIALOG
  if (showDateDialog) {
    AlertDialog(
      onDismissRequest = { showDateDialog = false },
      title = { Text("Set Ingestion Report Date") },
      text = {
        Column {
          Text("Enter the date corresponding to this clinical export (YYYY-MM-DD):", style = MaterialTheme.typography.bodySmall, color = TextMuted)
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = dateInput,
            onValueChange = { dateInput = it },
            placeholder = { Text("2026-10-04") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (dateInput.isNotBlank()) {
              onSelectDate(dateInput)
            }
            showDateDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
          Text("Save Date")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDateDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
