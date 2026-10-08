package com.example.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DatePreset
import com.example.model.FilterState
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

@Composable
fun GlobalFilterBar(
  filterState: FilterState,
  onDatePresetSelected: (DatePreset) -> Unit,
  onCustomDateRangeSet: (String, String) -> Unit,
  onBranchSelected: (String) -> Unit,
  onDoctorSelected: (String) -> Unit,
  onCategorySelected: (String) -> Unit,
  onResetFilters: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showDateDropdown by remember { mutableStateOf(false) }
  var showBranchDropdown by remember { mutableStateOf(false) }
  var showDoctorDropdown by remember { mutableStateOf(false) }
  var showCategoryDropdown by remember { mutableStateOf(false) }
  var showCustomDateDialog by remember { mutableStateOf(false) }

  var customStartInput by remember { mutableStateOf(filterState.customStartDate) }
  var customEndInput by remember { mutableStateOf(filterState.customEndDate) }

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = SurfaceCard,
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    shadowElevation = 1.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Filter Prefix Icon & Active Count
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(end = 4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = "Filters",
            tint = TealPrimary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Filter Bar",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextHeadline
          )
          if (filterState.activeFilterCount > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
              modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(TealPrimary),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${filterState.activeFilterCount}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // 1. DATE PRESET SELECTOR
        Box {
          FilterDropdownChip(
            icon = Icons.Default.CalendarToday,
            label = filterState.datePreset.label,
            isActive = filterState.datePreset != DatePreset.THIS_MONTH,
            onClick = { showDateDropdown = true },
            testTag = "filter_date_chip"
          )

          DropdownMenu(
            expanded = showDateDropdown,
            onDismissRequest = { showDateDropdown = false }
          ) {
            DatePreset.values().forEach { preset ->
              DropdownMenuItem(
                text = {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = preset.label,
                      fontWeight = if (filterState.datePreset == preset) FontWeight.Bold else FontWeight.Normal,
                      color = if (filterState.datePreset == preset) TealPrimary else TextHeadline
                    )
                  }
                },
                onClick = {
                  showDateDropdown = false
                  if (preset == DatePreset.CUSTOM) {
                    showCustomDateDialog = true
                  } else {
                    onDatePresetSelected(preset)
                  }
                }
              )
            }
          }
        }

        // 2. BRANCH SELECTOR
        Box {
          FilterDropdownChip(
            icon = Icons.Default.LocationOn,
            label = filterState.selectedBranch,
            isActive = filterState.selectedBranch != "All Branches",
            onClick = { showBranchDropdown = true },
            testTag = "filter_branch_chip"
          )

          DropdownMenu(
            expanded = showBranchDropdown,
            onDismissRequest = { showBranchDropdown = false }
          ) {
            FilterState.AVAILABLE_BRANCHES.forEach { branch ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = branch,
                    fontWeight = if (filterState.selectedBranch == branch) FontWeight.Bold else FontWeight.Normal,
                    color = if (filterState.selectedBranch == branch) TealPrimary else TextHeadline
                  )
                },
                onClick = {
                  onBranchSelected(branch)
                  showBranchDropdown = false
                }
              )
            }
          }
        }

        // 3. DOCTOR SELECTOR
        Box {
          FilterDropdownChip(
            icon = Icons.Default.Person,
            label = if (filterState.selectedDoctor == "All Doctors") "All Doctors" else filterState.selectedDoctor.substringBefore(","),
            isActive = filterState.selectedDoctor != "All Doctors",
            onClick = { showDoctorDropdown = true },
            testTag = "filter_doctor_chip"
          )

          DropdownMenu(
            expanded = showDoctorDropdown,
            onDismissRequest = { showDoctorDropdown = false }
          ) {
            FilterState.AVAILABLE_DOCTORS.forEach { doctor ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = doctor,
                    fontWeight = if (filterState.selectedDoctor == doctor) FontWeight.Bold else FontWeight.Normal,
                    color = if (filterState.selectedDoctor == doctor) TealPrimary else TextHeadline
                  )
                },
                onClick = {
                  onDoctorSelected(doctor)
                  showDoctorDropdown = false
                }
              )
            }
          }
        }

        // 4. CATEGORY SELECTOR
        Box {
          FilterDropdownChip(
            icon = Icons.Default.Category,
            label = filterState.selectedCategory,
            isActive = filterState.selectedCategory != "All Categories",
            onClick = { showCategoryDropdown = true },
            testTag = "filter_category_chip"
          )

          DropdownMenu(
            expanded = showCategoryDropdown,
            onDismissRequest = { showCategoryDropdown = false }
          ) {
            FilterState.AVAILABLE_CATEGORIES.forEach { category ->
              DropdownMenuItem(
                text = {
                  Text(
                    text = category,
                    fontWeight = if (filterState.selectedCategory == category) FontWeight.Bold else FontWeight.Normal,
                    color = if (filterState.selectedCategory == category) TealPrimary else TextHeadline
                  )
                },
                onClick = {
                  onCategorySelected(category)
                  showCategoryDropdown = false
                }
              )
            }
          }
        }

        // Reset Button if active
        if (filterState.activeFilterCount > 0) {
          TextButton(
            onClick = onResetFilters,
            modifier = Modifier.testTag("reset_filters_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Clear",
              tint = TealPrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Reset",
              style = MaterialTheme.typography.labelSmall,
              color = TealPrimary
            )
          }
        }
      }
    }
  }

  // DIALOG FOR CUSTOM DATE RANGE
  if (showCustomDateDialog) {
    AlertDialog(
      onDismissRequest = { showCustomDateDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Custom Date Range")
        }
      },
      text = {
        Column {
          Text(
            text = "Specify start and end dates (YYYY-MM-DD):",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = customStartInput,
            onValueChange = { customStartInput = it },
            label = { Text("Start Date") },
            placeholder = { Text("2026-10-01") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = customEndInput,
            onValueChange = { customEndInput = it },
            label = { Text("End Date") },
            placeholder = { Text("2026-10-03") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (customStartInput.isNotBlank() && customEndInput.isNotBlank()) {
              onCustomDateRangeSet(customStartInput, customEndInput)
            }
            showCustomDateDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCustomDateDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun FilterDropdownChip(
  icon: ImageVector,
  label: String,
  isActive: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  val background = if (isActive) TealLightContainer else SurfaceSubtle
  val border = if (isActive) TealAccent else BorderSubtle
  val textColor = if (isActive) TealOnContainer else TextBody

  Surface(
    color = background,
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, border),
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isActive) TealPrimary else TextMuted,
        modifier = Modifier.size(15.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
        color = textColor
      )
      Spacer(modifier = Modifier.width(4.dp))
      Icon(
        imageVector = Icons.Default.ArrowDropDown,
        contentDescription = null,
        tint = if (isActive) TealPrimary else TextMuted,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
