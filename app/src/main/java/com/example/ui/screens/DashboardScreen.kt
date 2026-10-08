package com.example.ui.screens

import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuditInspectionData
import com.example.model.CategoryGroup
import com.example.model.CategoryMetric
import com.example.model.DailyTrendPoint
import com.example.model.DashboardKpis
import com.example.model.DoctorRevenueMetric
import com.example.model.DominantUndersoldAnalysis
import com.example.model.DualStreamRevenue
import com.example.model.FilterState
import com.example.model.KpiMetric
import com.example.model.PackageValueSetting
import com.example.model.RawClinicRow
import com.example.model.ReconciliationSummary
import com.example.model.ServiceItemMetric
import com.example.model.ServiceRankings
import com.example.model.UnmappedPackageService
import com.example.model.ValuationMode
import com.example.model.AiSummaryUiState
import com.example.ui.components.AiSummaryCard
import com.example.ui.components.PackageValuesSettingsDialog
import com.example.ui.components.ReconciliationSummaryCard
import com.example.ui.components.SkeletonBox
import com.example.ui.components.UnmappedPackageWarningBanner
import com.example.ui.components.ValuationModeToggleBar
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
import com.example.ui.theme.TealPrimaryDark
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextHeadline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberBg
import java.text.DecimalFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
  kpis: DashboardKpis,
  dailyTrend: List<DailyTrendPoint>,
  serviceRankings: ServiceRankings,
  categorySplit: List<CategoryMetric>,
  doctorRevenue: List<DoctorRevenueMetric>,
  dominantVsUndersold: DominantUndersoldAnalysis,
  filteredRows: List<RawClinicRow>,
  filterState: FilterState,
  isSkeletonLoading: Boolean,
  auditInspectionData: AuditInspectionData?,
  valuationMode: ValuationMode = ValuationMode.COLLECTED,
  packageValuesMap: Map<String, Double> = emptyMap(),
  packageValueSettings: List<PackageValueSetting> = emptyList(),
  reconciliationSummary: ReconciliationSummary? = null,
  unmappedPackageServices: List<UnmappedPackageService> = emptyList(),
  showPackageValuesDialog: Boolean = false,
  dualStreamRevenue: DualStreamRevenue? = null,
  onSetValuationMode: (ValuationMode) -> Unit = {},
  onOpenPackageValuesDialog: () -> Unit = {},
  onClosePackageValuesDialog: () -> Unit = {},
  onSavePackageValue: (serviceName: String, perSessionValue: Double, category: String, notes: String) -> Unit = { _, _, _, _ -> },
  onDeletePackageValue: (serviceName: String) -> Unit = {},
  onNavigateToPharmacy: () -> Unit = {},
  aiSummaryState: AiSummaryUiState = AiSummaryUiState.Loading,
  onRegenerateAiSummary: () -> Unit = {},
  canManageSettings: Boolean = true,
  onInspectMetric: (title: String, formula: String, value: String, targetRows: List<RawClinicRow>) -> Unit,
  onDismissAudit: () -> Unit,
  onToggleSkeleton: () -> Unit,
  modifier: Modifier = Modifier
) {
  var rankingTabMode by remember { mutableStateOf(0) } // 0: By Revenue, 1: By Count
  var showAllRawRowsAudit by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(22.dp)
  ) {

    // 1. EXECUTIVE HEADER & AUDIT TRIGGER
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
              imageVector = Icons.Default.Dashboard,
              contentDescription = null,
              tint = TealPrimary,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Executive Performance",
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
        // Hidden / Discrete Audit View Button
        OutlinedButton(
          onClick = {
            onInspectMetric(
              "All Filtered Service Sales Records",
              "Sum of all matching raw rows under active filter scope (${valuationMode.displayName})",
              kpis.totalRevenue.formattedValue,
              filteredRows
            )
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .testTag("audit_all_rows_btn")
        ) {
          Icon(
            imageVector = Icons.Default.BugReport,
            contentDescription = "Audit Raw Rows",
            tint = TealPrimary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Audit Rows (${filteredRows.size})",
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

    // 2. PACKAGE RECTIFICATION: VALUATION MODE TOGGLE & UNMAPPED WARNING
    ValuationModeToggleBar(
      currentMode = valuationMode,
      onModeSelected = onSetValuationMode,
      onOpenSettings = onOpenPackageValuesDialog,
      unmappedCount = unmappedPackageServices.size,
      canOpenSettings = canManageSettings
    )

    if (unmappedPackageServices.isNotEmpty()) {
      UnmappedPackageWarningBanner(
        unmappedServices = unmappedPackageServices,
        onOpenSettings = onOpenPackageValuesDialog,
        onQuickDefine = {
          onOpenPackageValuesDialog()
        },
        canOpenSettings = canManageSettings
      )
    }

    // 3. EXECUTIVE AI SUMMARY CARD (Gemini Powered • Pre-computed Metrics Only • Cached per date)
    AiSummaryCard(
      uiState = aiSummaryState,
      onRegenerate = onRegenerateAiSummary
    )

    if (isSkeletonLoading) {
      // Skeleton loader placeholders
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SkeletonBox(modifier = Modifier.weight(1f), height = 120.dp, shape = RoundedCornerShape(16.dp))
        SkeletonBox(modifier = Modifier.weight(1f), height = 120.dp, shape = RoundedCornerShape(16.dp))
        SkeletonBox(modifier = Modifier.weight(1f), height = 120.dp, shape = RoundedCornerShape(16.dp))
      }
      SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 240.dp, shape = RoundedCornerShape(16.dp))
      SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 260.dp, shape = RoundedCornerShape(16.dp))
    } else {
      // 3. PRIMARY KPI CARDS (Total Revenue, Patients/Bills, Average Bill Value)
      // Tap any card to open the hidden Debug / Audit View with raw rows!
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Full-width Prominent Total Revenue Card
        KpiCard(
          title = "Total Revenue",
          kpi = kpis.totalRevenue,
          subtitle = if (valuationMode == ValuationMode.SERVICE_VALUE) "Service-Value Total (Rectified)" else "Gross Collected Sales",
          onClick = {
            onInspectMetric(
              "Total Revenue Breakdown (${valuationMode.displayName})",
              if (valuationMode == ValuationMode.SERVICE_VALUE)
                "Sum(row.NetTotal + packageValue) for ${filteredRows.size} matching rows"
              else
                "Sum(row.NetTotal) for ${filteredRows.size} matching rows",
              kpis.totalRevenue.formattedValue,
              filteredRows
            )
          },
          testTag = "kpi_total_revenue",
          modifier = Modifier.fillMaxWidth()
        )

        // 2-Column Split for Secondary KPIs
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          KpiCard(
            title = "Patients / Bills",
            kpi = kpis.patientBillsCount,
            subtitle = "Distinct Invoices Billed",
            onClick = {
              onInspectMetric(
                "Patients / Bills Audit",
                "Count(distinct row.InvoiceNo)",
                kpis.patientBillsCount.formattedValue,
                filteredRows
              )
            },
            testTag = "kpi_patients_bills",
            modifier = Modifier.weight(1f)
          )

          KpiCard(
            title = "Avg Bill Value",
            kpi = kpis.averageBillValue,
            subtitle = if (valuationMode == ValuationMode.SERVICE_VALUE) "Service Value / Patient" else "Collected / Patient",
            onClick = {
              onInspectMetric(
                "Average Bill Value (ABV)",
                "Total Revenue (${kpis.totalRevenue.formattedValue}) / Bills Count (${kpis.patientBillsCount.formattedValue})",
                kpis.averageBillValue.formattedValue,
                filteredRows
              )
            },
            testTag = "kpi_average_bill_value",
            modifier = Modifier.weight(1f)
          )
        }
      }

      // 4. RECONCILIATION LINE CARD (Collected + Package value = Service-value total)
      if (reconciliationSummary != null) {
        ReconciliationSummaryCard(
          reconciliation = reconciliationSummary,
          onAuditPackageRows = { pkgRows ->
            onInspectMetric(
              "Package / Enrolled Sessions Audit",
              "Rs. 0 billed sessions identified as enrolled patients valued at defined per-session rate",
              "Total ${pkgRows.size} sessions (Rectified Value: Rs. ${DecimalFormat("#,##0.00").format(reconciliationSummary.packageRectifiedValue)})",
              pkgRows
            )
          }
        )
      }

      // 5. DUAL REVENUE STREAM CARD (Clinical Service Sales vs Pharmacy Dispensary)
      if (dualStreamRevenue != null) {
        DualStreamRevenueCard(
          dualStream = dualStreamRevenue,
          onNavigateToPharmacy = onNavigateToPharmacy
        )
      }

      // 3. DAILY REVENUE TREND CHART
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
                text = "Revenue Trend (Daily)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextHeadline
              )
              Text(
                text = "Chronological day-by-day Service Sales progression",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
              )
            }
            Surface(color = TealLightContainer, shape = RoundedCornerShape(6.dp)) {
              Text(
                text = "${dailyTrend.size} Days Tracked",
                style = MaterialTheme.typography.labelSmall,
                color = TealOnContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          if (dailyTrend.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No daily revenue data for selected filter scope.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
              )
            }
          } else {
            DailyTrendChart(
              points = dailyTrend,
              onPointClick = { pt ->
                val dayRows = filteredRows.filter { it.reportDate == pt.date }
                onInspectMetric(
                  "Daily Revenue for ${pt.displayDate} (${pt.date})",
                  "Sum(row.NetTotal) for date ${pt.date}",
                  "$${DecimalFormat("#,##0.00").format(pt.revenue)} (${pt.billsCount} bills)",
                  dayRows
                )
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
            )
          }
        }
      }

      // 4. CATEGORY SPLIT (SKIN / OBESITY / HAIR / OTHER) DONUT CHART
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
                text = "Category Split (Skin / Obesity / Hair / Other)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextHeadline
              )
              Text(
                text = "Clinical specialty mix & revenue contribution share",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // Donut Chart Canvas - Centered & Sized for Mobile
            Box(
              modifier = Modifier
                .size(170.dp)
                .padding(4.dp),
              contentAlignment = Alignment.Center
            ) {
              CategoryDonutChart(
                metrics = categorySplit,
                modifier = Modifier.size(160.dp)
              )
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "TOTAL",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = TextSubtle
                )
                Text(
                  text = kpis.totalRevenue.formattedValue,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = TealPrimary
                )
              }
            }

            // Donut Legend & Breakdown - Full width below
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              categorySplit.forEach { catMetric ->
                Surface(
                  color = SurfaceSubtle,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                      val catRows = filteredRows.filter {
                        com.example.util.MetricsCalculator.getCategoryGroup(it) == catMetric.category
                      }
                      onInspectMetric(
                        "${catMetric.category.displayName} Category Audit",
                        "Raw rows categorized into ${catMetric.category.displayName}",
                        "$${DecimalFormat("#,##0.00").format(catMetric.revenue)} (${String.format(Locale.getDefault(), "%.1f", catMetric.percentage)}%)",
                        catRows
                      )
                    }
                    .testTag("category_row_${catMetric.category.name.lowercase()}")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier = Modifier
                          .size(12.dp)
                          .clip(CircleShape)
                          .background(Color(catMetric.category.colorHex))
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = catMetric.category.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextHeadline
                      )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        text = "$${DecimalFormat("#,##0").format(catMetric.revenue)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadline
                      )
                      Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", catMetric.percentage)}% (${catMetric.count} procedures)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 5. TOP 10 AND BOTTOM 10 SERVICES
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
                text = "Service Catalog Rankings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextHeadline
              )
              Text(
                text = "Top and bottom performing medical procedures",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
              )
            }

            // Segmented Toggle: By Revenue vs By Count
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceSubtle)
                .padding(2.dp)
            ) {
              RankingToggleChip("By Revenue", rankingTabMode == 0) { rankingTabMode = 0 }
              RankingToggleChip("By Count", rankingTabMode == 1) { rankingTabMode = 1 }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          val topList = if (rankingTabMode == 0) serviceRankings.top10ByRevenue else serviceRankings.top10ByCount
          val bottomList = if (rankingTabMode == 0) serviceRankings.bottom10ByRevenue else serviceRankings.bottom10ByCount

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // Top Performers Section
            Column(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "TOP PERFORMERS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = TealPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
              )
              topList.take(5).forEachIndexed { index, item ->
                ServiceRankItem(
                  rank = index + 1,
                  item = item,
                  isTop = true,
                  mode = rankingTabMode,
                  onClick = {
                    val sRows = filteredRows.filter { it.serviceName == item.serviceName }
                    onInspectMetric(
                      "Service Audit: ${item.serviceName}",
                      "Raw rows matching '${item.serviceName}'",
                      "$${DecimalFormat("#,##0.00").format(item.revenue)} (${item.count} count)",
                      sRows
                    )
                  }
                )
                Spacer(modifier = Modifier.height(6.dp))
              }
            }

            // Bottom Performers Section
            Column(modifier = Modifier.fillMaxWidth()) {
              Text(
                text = "BOTTOM PERFORMERS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = WarningAmber,
                modifier = Modifier.padding(bottom = 8.dp)
              )
              bottomList.take(5).forEachIndexed { index, item ->
                ServiceRankItem(
                  rank = index + 1,
                  item = item,
                  isTop = false,
                  mode = rankingTabMode,
                  onClick = {
                    val sRows = filteredRows.filter { it.serviceName == item.serviceName }
                    onInspectMetric(
                      "Underperforming Service Audit: ${item.serviceName}",
                      "Raw rows matching '${item.serviceName}'",
                      "$${DecimalFormat("#,##0.00").format(item.revenue)} (${item.count} count)",
                      sRows
                    )
                  }
                )
                Spacer(modifier = Modifier.height(6.dp))
              }
            }
          }
        }
      }

      // 6. REVENUE BY DOCTOR
      Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "Revenue by Attending Doctor",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextHeadline
          )
          Text(
            text = "Practitioner performance, patient bill load, and revenue share",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
          )

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            doctorRevenue.forEach { doc ->
              Surface(
                color = SurfaceSubtle,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    val docRows = filteredRows.filter { it.doctor.contains(doc.doctorName.substringBefore(",")) }
                    onInspectMetric(
                      "Doctor Audit: ${doc.doctorName}",
                      "All raw bills signed by ${doc.doctorName}",
                      "$${DecimalFormat("#,##0.00").format(doc.revenue)} (${doc.billCount} bills)",
                      docRows
                    )
                  }
                  .testTag("doctor_row_${doc.doctorName.hashCode()}")
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                      modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TealLightContainer),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(20.dp)
                      )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                      Text(
                        text = doc.doctorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadline
                      )
                      Text(
                        text = "${doc.billCount} bills billed · Avg: $${DecimalFormat("#,##0").format(doc.averageBill)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                      )
                    }
                  }

                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = "$${DecimalFormat("#,##0.00").format(doc.revenue)}",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = TealPrimary
                    )
                    Text(
                      text = "${String.format(Locale.getDefault(), "%.1f", doc.percentage)}% of group",
                      style = MaterialTheme.typography.labelSmall,
                      color = TextMuted
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 7. DOMINANT VS UNDER-SOLD SERVICE LINES
      Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "Dominant vs Under-Sold Service Lines",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextHeadline
          )
          Text(
            text = "Pareto revenue pillars vs high-value, under-converted growth opportunities",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
          )

          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            // Dominant Pillar Card
            Surface(
              color = RoleAllowedBg,
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, RoleAllowedGreen.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = RoleAllowedGreen, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Dominant Revenue Pillars", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = RoleAllowedGreen)
                }
                Spacer(modifier = Modifier.height(10.dp))
                dominantVsUndersold.dominantServices.forEach { item ->
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp)
                      .clickable {
                        val sRows = filteredRows.filter { it.serviceName == item.serviceName }
                        onInspectMetric("Dominant Line: ${item.serviceName}", "High revenue driver", "$${DecimalFormat("#,##0.00").format(item.revenue)}", sRows)
                      }
                  ) {
                    Text(text = item.serviceName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = TextHeadline)
                    Text(text = "$${DecimalFormat("#,##0").format(item.revenue)} · ${item.count} sessions", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                  }
                  HorizontalDivider(color = RoleAllowedGreen.copy(alpha = 0.15f), thickness = 0.5.dp)
                }
              }
            }

            // Under-Sold Opportunity Card
            Surface(
              color = WarningAmberBg,
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Under-Sold High Value", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = WarningAmber)
                }
                Spacer(modifier = Modifier.height(10.dp))
                dominantVsUndersold.underSoldServices.forEach { item ->
                  Column(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp)
                      .clickable {
                        val sRows = filteredRows.filter { it.serviceName == item.serviceName }
                        onInspectMetric("Under-Sold Opportunity: ${item.serviceName}", "High rate ($${item.averagePrice}) but low volume (${item.count})", "$${DecimalFormat("#,##0.00").format(item.revenue)}", sRows)
                      }
                  ) {
                    Text(text = item.serviceName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = TextHeadline)
                    Text(text = "High Rate: $${DecimalFormat("#,##0").format(item.averagePrice)} (Only ${item.count} done)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                  }
                  HorizontalDivider(color = WarningAmber.copy(alpha = 0.15f), thickness = 0.5.dp)
                }
              }
            }
          }
        }
      }
    }
  }

  // ------------------------------------------------------------------
  // HIDDEN DEBUG / AUDIT VIEW MODAL (Lists raw rows behind any tapped figure)
  // ------------------------------------------------------------------
  if (auditInspectionData != null) {
    AlertDialog(
      onDismissRequest = onDismissAudit,
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.BugReport,
              contentDescription = null,
              tint = TealPrimary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(text = "Raw Rows Audit Trail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(text = auditInspectionData.title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
          }
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Proof Summary Box
          Surface(
            color = TealLightContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Formula: ${auditInspectionData.formula}",
                style = MaterialTheme.typography.bodySmall,
                color = TealOnContainer
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Computed Value: ${auditInspectionData.computedValue}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TealPrimary
              )
              Text(
                text = "Underlying Raw Rows: ${auditInspectionData.rows.size} Records from Stored Service Sales",
                style = MaterialTheme.typography.labelSmall,
                color = TealOnContainer
              )
            }
          }

          // Raw Rows Table
          Text(
            text = "Audited Raw Rows (Direct from Firestore/Storage):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextHeadline
          )

          Surface(
            color = SurfaceSubtle,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              // Table Header
              Row(modifier = Modifier.padding(bottom = 6.dp)) {
                listOf("Invoice No", "Date", "Doctor", "Branch", "Service Name", "Category", "Amount", "Session Type").forEach { h ->
                  Text(text = h, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TextHeadline, modifier = Modifier.width(if (h == "Session Type") 140.dp else 110.dp))
                }
              }
              HorizontalDivider(color = BorderMedium, thickness = 1.dp)

              // Table Body
              auditInspectionData.rows.forEachIndexed { i, row ->
                val isPkg = row.isPackageSession
                val effectiveVal = row.getEffectiveAmount(valuationMode, packageValuesMap)
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                  Text(text = row.invoiceNo, style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.width(110.dp))
                  Text(text = row.reportDate, style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.width(110.dp))
                  Text(text = row.doctor.substringBefore(","), style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.width(110.dp))
                  Text(text = row.branch.take(12), style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.width(110.dp))
                  Text(text = row.serviceName, style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.width(110.dp), maxLines = 1)
                  Text(text = row.category, style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.width(110.dp), maxLines = 1)

                  // Amount column with package rectification indication
                  Column(modifier = Modifier.width(110.dp)) {
                    Text(
                      text = "Rs. ${DecimalFormat("#,##0.00").format(row.amount)}",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.Bold,
                      color = if (isPkg) TextMuted else TealPrimary
                    )
                    if (isPkg && valuationMode == ValuationMode.SERVICE_VALUE) {
                      Text(
                        text = "Val: Rs. ${DecimalFormat("#,##0").format(effectiveVal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoleAllowedGreen
                      )
                    }
                  }

                  // Session Type Badge
                  Box(modifier = Modifier.width(140.dp)) {
                    if (isPkg) {
                      Surface(
                        color = WarningAmberBg,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, WarningAmber.copy(alpha = 0.5f))
                      ) {
                        Text(
                          text = "Package / Enrolled",
                          style = MaterialTheme.typography.labelSmall,
                          color = WarningAmber,
                          fontSize = 10.sp,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    } else {
                      Surface(
                        color = TealLightContainer,
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text(
                          text = "Invoiced Cash",
                          style = MaterialTheme.typography.labelSmall,
                          color = TealPrimary,
                          fontSize = 10.sp,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                    }
                  }
                }
                if (i < auditInspectionData.rows.size - 1) {
                  HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = onDismissAudit,
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
          Text("Close Audit")
        }
      }
    )
  }

  // ------------------------------------------------------------------
  // PACKAGE VALUES SETTINGS DIALOG
  // ------------------------------------------------------------------
  if (showPackageValuesDialog) {
    PackageValuesSettingsDialog(
      packageValues = packageValueSettings,
      unmappedServices = unmappedPackageServices,
      onSaveRule = onSavePackageValue,
      onDeleteRule = onDeletePackageValue,
      onDismiss = onClosePackageValuesDialog
    )
  }
}

// -------------------------------------------------------------
// SUB-COMPONENTS
// -------------------------------------------------------------

@Composable
private fun KpiCard(
  title: String,
  kpi: KpiMetric,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
    color = SurfaceCard,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    shadowElevation = 1.dp
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Medium,
          color = TextMuted
        )
        Icon(
          imageVector = Icons.Default.Visibility,
          contentDescription = "Audit this metric",
          tint = TextSubtle,
          modifier = Modifier.size(14.dp)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Large readable number per guidelines
      Text(
        text = kpi.formattedValue,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        color = TealPrimary,
        letterSpacing = (-0.5).sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        val deltaColor = if (kpi.isNeutral) TextMuted else if (kpi.isPositive) RoleAllowedGreen else ErrorRed
        val deltaIcon = if (kpi.isNeutral) Icons.AutoMirrored.Filled.TrendingFlat else if (kpi.isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown

        Surface(
          color = if (kpi.isNeutral) SurfaceSubtle else if (kpi.isPositive) RoleAllowedBg else ErrorRedBg,
          shape = RoundedCornerShape(6.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = deltaIcon,
              contentDescription = null,
              tint = deltaColor,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "${if (kpi.percentageChange >= 0) "+" else ""}${String.format(Locale.getDefault(), "%.1f", kpi.percentageChange)}%",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = deltaColor
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "vs prev period",
          style = MaterialTheme.typography.labelSmall,
          color = TextSubtle
        )
      }
    }
  }
}

@Composable
private fun RankingToggleChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
  Surface(
    color = if (isSelected) SurfaceCard else Color.Transparent,
    shape = RoundedCornerShape(6.dp),
    modifier = Modifier
      .defaultMinSize(minHeight = 48.dp)
      .clip(RoundedCornerShape(6.dp))
      .clickable(onClick = onClick)
  ) {
    Box(
      modifier = Modifier
        .defaultMinSize(minHeight = 48.dp)
        .padding(horizontal = 12.dp, vertical = 6.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) TealPrimary else TextMuted
      )
    }
  }
}

@Composable
private fun ServiceRankItem(
  rank: Int,
  item: ServiceItemMetric,
  isTop: Boolean,
  mode: Int,
  onClick: () -> Unit
) {
  Surface(
    color = SurfaceSubtle,
    shape = RoundedCornerShape(8.dp),
    modifier = Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 48.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
  ) {
    Row(
      modifier = Modifier
        .defaultMinSize(minHeight = 48.dp)
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Text(
          text = "$rank",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (isTop) TealPrimary else WarningAmber,
          modifier = Modifier.width(18.dp)
        )
        Column {
          Text(
            text = item.serviceName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextHeadline,
            maxLines = 1
          )
          Text(
            text = item.category,
            style = MaterialTheme.typography.labelSmall,
            color = TextSubtle
          )
        }
      }

      Text(
        text = if (mode == 0) "$${DecimalFormat("#,##0").format(item.revenue)}" else "${item.count} sessions",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = TextHeadline
      )
    }
  }
}

// -------------------------------------------------------------
// CUSTOM JETPACK COMPOSE CANVAS CHARTS
// -------------------------------------------------------------

@Composable
private fun DailyTrendChart(
  points: List<DailyTrendPoint>,
  onPointClick: (DailyTrendPoint) -> Unit,
  modifier: Modifier = Modifier
) {
  if (points.isEmpty()) return

  val maxRevenue = points.maxOfOrNull { it.revenue }?.coerceAtLeast(100.0) ?: 100.0
  val primaryColor = TealPrimary
  val accentColor = TealAccent

  Canvas(modifier = modifier) {
    val width = size.width
    val height = size.height - 30.dp.toPx()
    val pointSpacing = width / (points.size + 1)

    // Draw horizontal grid lines
    val gridLines = 3
    for (i in 0..gridLines) {
      val y = height * (i.toFloat() / gridLines)
      drawLine(
        color = Color(0xFFE2E8F0),
        start = Offset(0f, y),
        end = Offset(width, y),
        strokeWidth = 1.dp.toPx()
      )
    }

    // Path for curve and gradient fill
    val path = Path()
    val fillPath = Path()

    points.forEachIndexed { i, pt ->
      val x = pointSpacing * (i + 1)
      val y = height - (pt.revenue.toFloat() / maxRevenue.toFloat() * height)

      if (i == 0) {
        path.moveTo(x, y)
        fillPath.moveTo(x, height)
        fillPath.lineTo(x, y)
      } else {
        path.lineTo(x, y)
        fillPath.lineTo(x, y)
      }
    }

    // Close fillPath
    val lastX = pointSpacing * points.size
    fillPath.lineTo(lastX, height)
    fillPath.close()

    // Draw area gradient fill
    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
        startY = 0f,
        endY = height
      )
    )

    // Draw main stroke line
    drawPath(
      path = path,
      color = primaryColor,
      style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )

    // Draw individual points
    points.forEachIndexed { i, pt ->
      val x = pointSpacing * (i + 1)
      val y = height - (pt.revenue.toFloat() / maxRevenue.toFloat() * height)

      drawCircle(
        color = Color.White,
        radius = 5.dp.toPx(),
        center = Offset(x, y)
      )
      drawCircle(
        color = primaryColor,
        radius = 3.5.dp.toPx(),
        center = Offset(x, y)
      )
    }
  }
}

@Composable
private fun CategoryDonutChart(
  metrics: List<CategoryMetric>,
  modifier: Modifier = Modifier
) {
  val total = metrics.sumOf { it.revenue }.coerceAtLeast(1.0)

  Canvas(modifier = modifier) {
    val strokeWidth = 24.dp.toPx()
    val radius = (size.minDimension - strokeWidth) / 2
    val center = Offset(size.width / 2, size.height / 2)

    var startAngle = -90f

    metrics.forEach { metric ->
      val sweepAngle = ((metric.revenue / total) * 360f).toFloat()
      if (sweepAngle > 0f) {
        drawArc(
          color = Color(metric.category.colorHex),
          startAngle = startAngle,
          sweepAngle = sweepAngle - 2f, // subtle gap
          useCenter = false,
          topLeft = Offset(center.x - radius, center.y - radius),
          size = Size(radius * 2, radius * 2),
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += sweepAngle
      }
    }
  }
}

@Composable
private fun DualStreamRevenueCard(
  dualStream: DualStreamRevenue?,
  onNavigateToPharmacy: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (dualStream == null) return

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
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Assessment,
              contentDescription = null,
              tint = TealPrimary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Clinic Group Revenue Streams",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextHeadline
            )
          }
          Text(
            text = "Clinical Services vs Pharmacy Dispensary multi-stream turnover",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp)
          )
        }

        OutlinedButton(
          onClick = onNavigateToPharmacy,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("nav_to_pharmacy_btn")
        ) {
          Icon(imageVector = Icons.Filled.MedicalServices, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Pharmacy Stream", style = MaterialTheme.typography.labelSmall, color = TealPrimary)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Total Combined Group Card
      Surface(
        color = TealLightContainer,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "TOTAL CLINIC GROUP REVENUE (ALL STREAMS)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = TealOnContainer
            )
            Text(
              text = "Rs. ${DecimalFormat("#,##0.00").format(dualStream.totalCombinedRevenue)}",
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color = TealPrimary
            )
          }

          Surface(color = Color.White, shape = RoundedCornerShape(6.dp)) {
            Text(
              text = "Clinical + Dispensary",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = TealPrimary,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Proportional Bar
      val clinicalWeight = (dualStream.clinicalSharePercentage.coerceIn(5.0, 95.0) / 100f).toFloat()
      val pharmacyWeight = (1f - clinicalWeight).coerceAtLeast(0.05f)

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(10.dp)
          .clip(CircleShape)
      ) {
        Box(
          modifier = Modifier
            .weight(clinicalWeight)
            .height(10.dp)
            .background(TealPrimary)
        )
        Box(
          modifier = Modifier
            .weight(pharmacyWeight)
            .height(10.dp)
            .background(Color(0xFF0284C7)) // Sky / Cyan Pharmacy Stream
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Details Breakdown Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Clinical Stream Box
        Surface(
          color = SurfaceSubtle,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(TealPrimary))
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = "Clinical Service Sales", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = TextHeadline)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Rs. ${DecimalFormat("#,##0.00").format(dualStream.clinicalServiceRevenue)}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextHeadline
            )
            Text(
              text = "${String.format(java.util.Locale.getDefault(), "%.1f", dualStream.clinicalSharePercentage)}% of turnover · ${dualStream.clinicalBillCount} patient encounters",
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted
            )
          }
        }

        // Pharmacy Stream Box
        Surface(
          color = SurfaceSubtle,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF0284C7)))
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = "Pharmacy Dispensary", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = TextHeadline)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Rs. ${DecimalFormat("#,##0.00").format(dualStream.pharmacyRevenue)}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF0284C7)
            )
            Text(
              text = "${String.format(java.util.Locale.getDefault(), "%.1f", dualStream.pharmacySharePercentage)}% of turnover · ${dualStream.pharmacyUnitsSold} units sold",
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted
            )
          }
        }
      }
    }
  }
}
