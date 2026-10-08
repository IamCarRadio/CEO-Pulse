package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DatePreset
import com.example.model.FilterState
import com.example.ui.theme.BackgroundCanvas
import com.example.ui.theme.BorderMedium
import com.example.ui.theme.BorderSubtle
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
  filterState: FilterState,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  onDismiss: () -> Unit,
  onApplyFilters: (FilterState) -> Unit,
  onResetFilters: () -> Unit,
  modifier: Modifier = Modifier
) {
  var tempPreset by remember(filterState) { mutableStateOf(filterState.datePreset) }
  var tempBranch by remember(filterState) { mutableStateOf(filterState.selectedBranch) }
  var tempDoctor by remember(filterState) { mutableStateOf(filterState.selectedDoctor) }
  var tempCategory by remember(filterState) { mutableStateOf(filterState.selectedCategory) }
  var tempCustomStart by remember(filterState) { mutableStateOf(filterState.customStartDate) }
  var tempCustomEnd by remember(filterState) { mutableStateOf(filterState.customEndDate) }

  val activeCount = (if (tempPreset != DatePreset.THIS_MONTH) 1 else 0) +
    (if (tempBranch != "All Branches") 1 else 0) +
    (if (tempDoctor != "All Doctors") 1 else 0) +
    (if (tempCategory != "All Categories") 1 else 0)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = SurfaceCard,
    contentColor = TextHeadline,
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(vertical = 12.dp)
          .size(width = 44.dp, height = 5.dp)
          .clip(CircleShape)
          .background(BorderMedium)
      )
    },
    modifier = modifier.testTag("filter_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 6.dp)
        .navigationBarsPadding()
    ) {
      // Header Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(TealLightContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Filter Icon",
              tint = TealPrimary,
              modifier = Modifier.size(20.dp)
            )
          }
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = "Filter Analytics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextHeadline
              )
              if (activeCount > 0) {
                Box(
                  modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(TealPrimary),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "$activeCount",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
            Text(
              text = "Scope performance metrics across clinic parameters",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted
            )
          }
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(48.dp)
            .testTag("close_filter_sheet_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close Filters",
            tint = TextMuted,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))
      HorizontalDivider(color = BorderSubtle)
      Spacer(modifier = Modifier.height(16.dp))

      // 1. DATE PRESET SECTION
      FilterSectionTitle(
        icon = Icons.Default.CalendarToday,
        title = "Reporting Period",
        subtitle = "Chronological window for clinic metrics"
      )
      Spacer(modifier = Modifier.height(10.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        DatePreset.values().forEach { preset ->
          FilterSelectChip(
            label = preset.label,
            isSelected = tempPreset == preset,
            onClick = { tempPreset = preset },
            testTag = "sheet_preset_${preset.name.lowercase()}"
          )
        }
      }

      // Custom date inputs if CUSTOM is selected
      AnimatedVisibility(visible = tempPreset == DatePreset.CUSTOM) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(SurfaceSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "Enter Custom Date Range (YYYY-MM-DD):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextHeadline
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedTextField(
              value = tempCustomStart,
              onValueChange = { tempCustomStart = it },
              label = { Text("Start Date") },
              placeholder = { Text("2026-06-01") },
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPrimary,
                unfocusedBorderColor = BorderMedium
              ),
              singleLine = true
            )
            OutlinedTextField(
              value = tempCustomEnd,
              onValueChange = { tempCustomEnd = it },
              label = { Text("End Date") },
              placeholder = { Text("2026-06-30") },
              modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPrimary,
                unfocusedBorderColor = BorderMedium
              ),
              singleLine = true
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 2. CLINIC BRANCH SECTION
      FilterSectionTitle(
        icon = Icons.Default.LocationOn,
        title = "Clinic Branch Location",
        subtitle = "Multi-branch clinic segregation"
      )
      Spacer(modifier = Modifier.height(10.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        FilterState.AVAILABLE_BRANCHES.forEach { branch ->
          FilterSelectChip(
            label = branch,
            isSelected = tempBranch == branch,
            onClick = { tempBranch = branch },
            testTag = "sheet_branch_${branch.lowercase().replace(" ", "_")}"
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 3. DOCTOR SECTION
      FilterSectionTitle(
        icon = Icons.Default.Person,
        title = "Attending Physician / Doctor",
        subtitle = "Doctor-wise revenue attribution"
      )
      Spacer(modifier = Modifier.height(10.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        FilterState.AVAILABLE_DOCTORS.forEach { doctor ->
          FilterSelectChip(
            label = doctor,
            isSelected = tempDoctor == doctor,
            onClick = { tempDoctor = doctor },
            testTag = "sheet_doctor_${doctor.lowercase().replace(" ", "_").replace(".", "")}"
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // 4. CATEGORY SECTION
      FilterSectionTitle(
        icon = Icons.Default.MedicalServices,
        title = "Specialty Category",
        subtitle = "Skin, Obesity, Hair, and other clinical domains"
      )
      Spacer(modifier = Modifier.height(10.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        FilterState.AVAILABLE_CATEGORIES.forEach { category ->
          FilterSelectChip(
            label = category,
            isSelected = tempCategory == category,
            onClick = { tempCategory = category },
            testTag = "sheet_cat_${category.lowercase()}"
          )
        }
      }

      Spacer(modifier = Modifier.height(26.dp))
      HorizontalDivider(color = BorderSubtle)
      Spacer(modifier = Modifier.height(16.dp))

      // 5. ACTION BUTTONS: RESET & APPLY
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = {
            tempPreset = DatePreset.THIS_MONTH
            tempBranch = "All Branches"
            tempDoctor = "All Doctors"
            tempCategory = "All Categories"
            tempCustomStart = ""
            tempCustomEnd = ""
            onResetFilters()
            onDismiss()
          },
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("reset_all_filters_btn"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.RotateLeft,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = TextMuted
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Reset All",
            style = MaterialTheme.typography.labelLarge,
            color = TextHeadline
          )
        }

        Button(
          onClick = {
            val updated = FilterState(
              datePreset = tempPreset,
              customStartDate = tempCustomStart,
              customEndDate = tempCustomEnd,
              selectedBranch = tempBranch,
              selectedDoctor = tempDoctor,
              selectedCategory = tempCategory
            )
            onApplyFilters(updated)
            onDismiss()
          },
          modifier = Modifier
            .weight(1.4f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("apply_filters_btn"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.White
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Apply Filters",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }
    }
  }
}

@Composable
private fun FilterSectionTitle(
  icon: ImageVector,
  title: String,
  subtitle: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = TealPrimary,
      modifier = Modifier.size(18.dp)
    )
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = TextHeadline
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted
      )
    }
  }
}

/**
 * Filter Chip with guaranteed 48dp x 48dp minimum accessible touch target.
 */
@Composable
private fun FilterSelectChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isSelected) TealPrimary else SurfaceSubtle,
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = if (isSelected) TealPrimary else BorderMedium
    ),
    modifier = Modifier
      .defaultMinSize(minHeight = 48.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .defaultMinSize(minHeight = 48.dp)
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      if (isSelected) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(14.dp)
        )
      }
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) Color.White else TextBody
      )
    }
  }
}
