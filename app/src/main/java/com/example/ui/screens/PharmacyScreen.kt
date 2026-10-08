package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.model.FilterState
import com.example.model.KpiMetric
import com.example.model.PharmacyItem
import com.example.model.PharmacyKpis
import com.example.model.PharmacyProductRanking
import com.example.model.PharmacyRankings
import com.example.model.PharmacyReconciliation
import com.example.model.PharmacySortOption
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
import java.text.DecimalFormat

private val currencyFormat = DecimalFormat("Rs. #,##0.00")
private val countFormat = DecimalFormat("#,##0")
private val pctFormat = DecimalFormat("#,##0.0'%'")

@Composable
fun PharmacyScreen(
  kpis: PharmacyKpis,
  rankings: PharmacyRankings,
  reconciliation: PharmacyReconciliation,
  filteredItems: List<PharmacyItem>,
  searchQuery: String,
  selectedSortOption: PharmacySortOption,
  filterState: FilterState,
  isSkeletonLoading: Boolean,
  onSearchQueryChange: (String) -> Unit,
  onSortOptionChange: (PharmacySortOption) -> Unit,
  onToggleSkeleton: () -> Unit,
  modifier: Modifier = Modifier
) {
  var rankingTabMode by remember { mutableStateOf(0) } // 0: Top Revenue, 1: Top Margin, 2: Bottom Performers
  var auditedItemsDialog by remember { mutableStateOf<List<PharmacyItem>?>(null) }
  var auditedDialogTitle by remember { mutableStateOf("") }
  var showSortDropdown by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(22.dp)
  ) {

    // 1. EXECUTIVE HEADER
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
              .size(44.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(TealLightContainer)
              .border(1.dp, TealAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.LocalPharmacy,
              contentDescription = null,
              tint = TealPrimary,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Pharmacy Margins",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = TextHeadline
            )
            Text(
              text = "${filterState.formattedDateDisplay} · ${filterState.selectedBranch}",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedButton(
          onClick = {
            auditedDialogTitle = "All Audited Pharmacy Margin Items (${filteredItems.size})"
            auditedItemsDialog = filteredItems
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("audit_pharmacy_all_btn")
        ) {
          Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = "Audit Items",
            tint = TealPrimary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Audit Items (${filteredItems.size})",
            style = MaterialTheme.typography.labelSmall,
            color = TealPrimary
          )
        }

        IconButton(
          onClick = onToggleSkeleton,
          modifier = Modifier
            .size(48.dp)
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Refresh Data",
            tint = TextMuted,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }

    if (isSkeletonLoading) {
      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          SkeletonBox(modifier = Modifier.weight(1f), height = 110.dp, shape = RoundedCornerShape(16.dp))
          SkeletonBox(modifier = Modifier.weight(1f), height = 110.dp, shape = RoundedCornerShape(16.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          SkeletonBox(modifier = Modifier.weight(1f), height = 110.dp, shape = RoundedCornerShape(16.dp))
          SkeletonBox(modifier = Modifier.weight(1f), height = 110.dp, shape = RoundedCornerShape(16.dp))
        }
      }
      SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 140.dp, shape = RoundedCornerShape(16.dp))
      SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 300.dp, shape = RoundedCornerShape(16.dp))
    } else {

      // 2. PHARMACY KPI CARDS: 2x2 Responsive Grid for 390px
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          PharmacyKpiCard(
            title = "Pharmacy Revenue",
            kpi = kpis.totalRevenue,
            subtitle = "${countFormat.format(kpis.totalUnitsSold)} Units Sold",
            onClick = {
              auditedDialogTitle = "Pharmacy Revenue Item-Wise Breakdown"
              auditedItemsDialog = filteredItems
            },
            testTag = "pharmacy_kpi_revenue",
            modifier = Modifier.weight(1f)
          )

          PharmacyKpiCard(
            title = "Gross Margin",
            kpi = kpis.grossMargin,
            subtitle = "Net Dispensary Profit",
            onClick = {
              auditedDialogTitle = "Gross Margin Item-Wise Contribution"
              auditedItemsDialog = filteredItems
            },
            testTag = "pharmacy_kpi_margin",
            modifier = Modifier.weight(1f)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          PharmacyKpiCard(
            title = "Total Cost (COGS)",
            kpi = kpis.totalCost,
            subtitle = "Inventory Acquisition",
            onClick = {
              auditedDialogTitle = "Pharmacy Cost of Goods Sold (COGS) Breakdown"
              auditedItemsDialog = filteredItems
            },
            testTag = "pharmacy_kpi_cost",
            modifier = Modifier.weight(1f)
          )

          PharmacyKpiCard(
            title = "Margin %",
            kpi = kpis.marginPercentage,
            subtitle = "Overall Markup Rate",
            onClick = {
              auditedDialogTitle = "Dispensary Margin % Audit"
              auditedItemsDialog = filteredItems
            },
            testTag = "pharmacy_kpi_margin_pct",
            modifier = Modifier.weight(1f)
          )
        }
      }

      // 3. RECONCILIATION CHECK CARD (File Column Totals vs App Totals)
      PharmacyReconciliationCard(
        reconciliation = reconciliation,
        onInspectReconciliation = {
          auditedDialogTitle = "File vs App Column Totals Reconciliation Trail"
          auditedItemsDialog = filteredItems
        }
      )

      // 4. TOP 10 AND BOTTOM PERFORMERS (By Revenue, By Margin, Bottom Performers)
      Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Column {
              Text(
                text = "Product Margin Performance Rankings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextHeadline
              )
              Text(
                text = "Item-wise margin leaders and under-performing pharmaceuticals",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
              )
            }

            // Mode Selector: By Revenue vs By Margin vs Bottom Performers
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceSubtle)
                .padding(3.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Box(modifier = Modifier.weight(1f)) {
                RankingFilterChip("Top Revenue", rankingTabMode == 0) { rankingTabMode = 0 }
              }
              Box(modifier = Modifier.weight(1f)) {
                RankingFilterChip("Top Margin", rankingTabMode == 1) { rankingTabMode = 1 }
              }
              Box(modifier = Modifier.weight(1f)) {
                RankingFilterChip("Bottom", rankingTabMode == 2) { rankingTabMode = 2 }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          val activeList = when (rankingTabMode) {
            0 -> rankings.top10ByRevenue
            1 -> rankings.top10ByMargin
            else -> rankings.bottomPerformers
          }

          if (activeList.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No pharmacy items found for the active filter.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
              )
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              activeList.forEachIndexed { index, product ->
                ProductRankingRow(
                  rank = index + 1,
                  product = product,
                  isBottom = rankingTabMode == 2,
                  onClick = {
                    val matchingItems = filteredItems.filter { it.productName == product.productName }
                    auditedDialogTitle = "Product Audit: ${product.productName}"
                    auditedItemsDialog = matchingItems
                  }
                )
              }
            }
          }
        }
      }

      // 5. PRODUCT CATALOG SEARCH & SORTABLE INVENTORY LIST
      Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Column {
              Text(
                text = "Item-Wise Inventory Catalog (${filteredItems.size} SKUs)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextHeadline
              )
              Text(
                text = "Complete itemized margin breakdown with real-time search & sorting",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
              )
            }

            // Sort Dropdown Button
            Box {
              OutlinedButton(
                onClick = { showSortDropdown = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .defaultMinSize(minHeight = 48.dp)
                  .testTag("sort_pharmacy_btn")
              ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp), tint = TealPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Sort: ${selectedSortOption.displayName}", style = MaterialTheme.typography.labelSmall, color = TealPrimary)
              }

              DropdownMenu(
                expanded = showSortDropdown,
                onDismissRequest = { showSortDropdown = false }
              ) {
                PharmacySortOption.values().forEach { option ->
                  DropdownMenuItem(
                    text = { Text(text = option.displayName) },
                    onClick = {
                      onSortOptionChange(option)
                      showSortDropdown = false
                    }
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Search Field
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search by product name, SKU, or category...") },
            leadingIcon = {
              Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted)
            },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                  Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("pharmacy_search_input")
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Table of Products
          Surface(
            color = SurfaceSubtle,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              // Header
              Row(modifier = Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Code / SKU", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(110.dp))
                Text(text = "Product Name", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(220.dp))
                Text(text = "Category", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(130.dp))
                Text(text = "Qty", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(60.dp))
                Text(text = "Unit Cost", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(90.dp))
                Text(text = "Unit Price", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(90.dp))
                Text(text = "Total Rev", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(110.dp))
                Text(text = "Total Cost", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(110.dp))
                Text(text = "Margin", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(110.dp))
                Text(text = "Margin %", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(80.dp))
              }
              HorizontalDivider(color = BorderMedium, thickness = 1.dp)

              // Item rows
              filteredItems.forEachIndexed { idx, item ->
                Row(
                  modifier = Modifier
                    .padding(vertical = 6.dp)
                    .clickable {
                      auditedDialogTitle = "Transaction Audit: ${item.productName}"
                      auditedItemsDialog = listOf(item)
                    },
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = item.productCode, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.width(110.dp))
                  Text(text = item.productName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = TextHeadline, modifier = Modifier.width(220.dp), maxLines = 1)
                  Text(text = item.category, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.width(130.dp), maxLines = 1)
                  Text(text = item.quantity.toString(), style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.width(60.dp))
                  Text(text = currencyFormat.format(item.unitCost), style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.width(90.dp))
                  Text(text = currencyFormat.format(item.unitPrice), style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.width(90.dp))
                  Text(text = currencyFormat.format(item.totalRevenue), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(110.dp))
                  Text(text = currencyFormat.format(item.totalCost), style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.width(110.dp))
                  Text(text = currencyFormat.format(item.grossMargin), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TealPrimary, modifier = Modifier.width(110.dp))

                  // Margin % Pill
                  val isGoodMargin = item.marginPercentage >= 50.0
                  Surface(
                    color = if (isGoodMargin) RoleAllowedBg else WarningAmberBg,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.width(80.dp)
                  ) {
                    Text(
                      text = "${String.format(java.util.Locale.getDefault(), "%.1f", item.marginPercentage)}%",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (isGoodMargin) RoleAllowedGreen else WarningAmber,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                if (idx < filteredItems.size - 1) {
                  HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                }
              }
            }
          }
        }
      }
    }
  }

  // -------------------------------------------------------------
  // RAW PHARMACY ITEMS AUDIT MODAL
  // -------------------------------------------------------------
  if (auditedItemsDialog != null) {
    val itemsToAudit = auditedItemsDialog ?: emptyList()
    AlertDialog(
      onDismissRequest = { auditedItemsDialog = null },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.LocalPharmacy, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = auditedDialogTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          IconButton(onClick = { auditedItemsDialog = null }) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
          }
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            color = TealLightContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "Records Audited: ${itemsToAudit.size} Items | Total Sales: ${currencyFormat.format(itemsToAudit.sumOf { it.totalRevenue })} | Total Margin: ${currencyFormat.format(itemsToAudit.sumOf { it.grossMargin })}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TealOnContainer
              )
            }
          }

          Surface(
            color = SurfaceSubtle,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
          ) {
            Column(modifier = Modifier.padding(8.dp)) {
              Row(modifier = Modifier.padding(bottom = 6.dp)) {
                listOf("Code", "Product", "Category", "Qty", "Price", "Cost", "Revenue", "Margin").forEach { h ->
                  Text(text = h, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(100.dp))
                }
              }
              HorizontalDivider(color = BorderMedium, thickness = 1.dp)

              itemsToAudit.forEachIndexed { i, itm ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                  Text(text = itm.productCode, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.width(100.dp))
                  Text(text = itm.productName, style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.width(100.dp), maxLines = 1)
                  Text(text = itm.category, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.width(100.dp), maxLines = 1)
                  Text(text = itm.quantity.toString(), style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.width(100.dp))
                  Text(text = currencyFormat.format(itm.unitPrice), style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.width(100.dp))
                  Text(text = currencyFormat.format(itm.unitCost), style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.width(100.dp))
                  Text(text = currencyFormat.format(itm.totalRevenue), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(100.dp))
                  Text(text = currencyFormat.format(itm.grossMargin), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TealPrimary, modifier = Modifier.width(100.dp))
                }
                if (i < itemsToAudit.size - 1) {
                  HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { auditedItemsDialog = null },
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
          Text("Done")
        }
      }
    )
  }
}

// -------------------------------------------------------------
// SUB-COMPONENTS
// -------------------------------------------------------------

@Composable
private fun PharmacyKpiCard(
  title: String,
  kpi: KpiMetric,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = SurfaceCard,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = TextMuted
      )

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = kpi.formattedValue,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = TextHeadline
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted
        )

        if (!kpi.isNeutral) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (kpi.isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
              contentDescription = null,
              tint = if (kpi.isPositive) RoleAllowedGreen else ErrorRed,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "${if (kpi.isPositive) "+" else ""}${String.format(java.util.Locale.getDefault(), "%.1f", kpi.percentageChange)}%",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (kpi.isPositive) RoleAllowedGreen else ErrorRed
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PharmacyReconciliationCard(
  reconciliation: PharmacyReconciliation,
  onInspectReconciliation: () -> Unit,
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
            imageVector = if (reconciliation.isFullyReconciled) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (reconciliation.isFullyReconciled) RoleAllowedGreen else WarningAmber,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "File Column Totals Reconciliation Check",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextHeadline
          )
        }

        Surface(
          color = if (reconciliation.isFullyReconciled) RoleAllowedBg else WarningAmberBg,
          shape = RoundedCornerShape(6.dp),
          border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (reconciliation.isFullyReconciled) RoleAllowedGreen.copy(alpha = 0.3f) else WarningAmber.copy(alpha = 0.3f)
          )
        ) {
          Text(
            text = if (reconciliation.isFullyReconciled) "100% Reconciled (Zero Variance)" else "Variance Flagged",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (reconciliation.isFullyReconciled) RoleAllowedGreen else WarningAmber,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Column comparisons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        ReconciliationColumnMetric(
          title = "Sales / Revenue",
          fileTotal = reconciliation.fileTotalRevenue,
          appTotal = reconciliation.appTotalRevenue,
          isMatched = reconciliation.isRevenueMatched,
          modifier = Modifier.weight(1f)
        )

        ReconciliationColumnMetric(
          title = "Total Cost (COGS)",
          fileTotal = reconciliation.fileTotalCost,
          appTotal = reconciliation.appTotalCost,
          isMatched = reconciliation.isCostMatched,
          modifier = Modifier.weight(1f)
        )

        ReconciliationColumnMetric(
          title = "Gross Margin",
          fileTotal = reconciliation.fileTotalMargin,
          appTotal = reconciliation.appTotalMargin,
          isMatched = reconciliation.isMarginMatched,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Independently validates that column totals in the imported spreadsheet exactly match the internal aggregations.",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
          modifier = Modifier.weight(1f)
        )

        OutlinedButton(
          onClick = onInspectReconciliation,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("inspect_pharmacy_reconciliation_btn")
        ) {
          Text(text = "View Audit Trail", style = MaterialTheme.typography.labelSmall, color = TealPrimary)
        }
      }
    }
  }
}

@Composable
private fun ReconciliationColumnMetric(
  title: String,
  fileTotal: Double,
  appTotal: Double,
  isMatched: Boolean,
  modifier: Modifier = Modifier
) {
  Surface(
    color = SurfaceSubtle,
    shape = RoundedCornerShape(10.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextMuted)
        Icon(
          imageVector = if (isMatched) Icons.Default.CheckCircle else Icons.Default.Warning,
          contentDescription = null,
          tint = if (isMatched) RoleAllowedGreen else ErrorRed,
          modifier = Modifier.size(14.dp)
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = currencyFormat.format(appTotal),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextHeadline
      )
      Text(
        text = "File: ${currencyFormat.format(fileTotal)}",
        style = MaterialTheme.typography.labelSmall,
        color = if (isMatched) RoleAllowedGreen else ErrorRed
      )
    }
  }
}

@Composable
private fun ProductRankingRow(
  rank: Int,
  product: PharmacyProductRanking,
  isBottom: Boolean,
  onClick: () -> Unit
) {
  Surface(
    color = SurfaceSubtle,
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (isBottom) WarningAmberBg else TealLightContainer),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isBottom) WarningAmber else TealPrimary
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = product.productName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextHeadline
          )
          Text(
            text = "${product.category} · ${product.unitsSold} units sold @ ${currencyFormat.format(product.averageSellingPrice)}",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = currencyFormat.format(product.revenue),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextHeadline
          )
          Text(
            text = "Margin: ${currencyFormat.format(product.margin)}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TealPrimary
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
          color = if (product.marginPercentage >= 50.0) RoleAllowedBg else if (product.marginPercentage >= 30.0) TealLightContainer else WarningAmberBg,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = "${String.format(java.util.Locale.getDefault(), "%.1f", product.marginPercentage)}%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (product.marginPercentage >= 50.0) RoleAllowedGreen else if (product.marginPercentage >= 30.0) TealPrimary else WarningAmber,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun RankingFilterChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) TealPrimary else Color.Transparent,
    modifier = Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 48.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 48.dp)
        .padding(horizontal = 8.dp, vertical = 6.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) Color.White else TextMuted,
        maxLines = 1
      )
    }
  }
}
