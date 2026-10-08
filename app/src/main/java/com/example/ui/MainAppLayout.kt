package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuditInspectionData
import com.example.model.CategoryMetric
import com.example.model.DailyTrendPoint
import com.example.model.DashboardKpis
import com.example.model.DashboardTab
import com.example.model.DoctorRevenueMetric
import com.example.model.DominantUndersoldAnalysis
import com.example.model.DuplicateCheckResult
import com.example.model.FilterState
import com.example.model.PackageValueSetting
import com.example.model.RawClinicRow
import com.example.model.ReconciliationSummary
import com.example.model.ReportType
import com.example.model.ServiceRankings
import com.example.model.UnmappedPackageService
import com.example.model.UploadRecord
import com.example.model.UserProfile
import com.example.model.ValidationReport
import com.example.model.ValuationMode
import com.example.model.AiSummaryUiState
import com.example.model.AskChatMessage
import com.example.data.SaveUploadResult
import com.example.model.PharmacyKpis
import com.example.model.PharmacyRankings
import com.example.model.PharmacyReconciliation
import com.example.model.PharmacyItem
import com.example.model.PharmacySortOption
import com.example.model.RoleDoc
import com.example.ui.components.ChangePasswordDialog
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.GlobalFilterBar
import com.example.ui.screens.AskScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PharmacyScreen
import com.example.ui.screens.PlaceholderScreen
import com.example.ui.screens.UploadScreen
import com.example.ui.screens.UsersAndAccessScreen
import com.example.util.ParsedSpreadsheet
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.theme.BackgroundCanvas
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(
  user: UserProfile,
  activeTab: DashboardTab,
  filterState: FilterState,
  isSkeletonLoading: Boolean,
  dashboardKpis: DashboardKpis,
  dailyTrend: List<DailyTrendPoint>,
  serviceRankings: ServiceRankings,
  categorySplit: List<CategoryMetric>,
  doctorRevenue: List<DoctorRevenueMetric>,
  dominantVsUndersold: DominantUndersoldAnalysis,
  filteredRows: List<RawClinicRow>,
  auditInspectionData: AuditInspectionData?,
  valuationMode: ValuationMode = ValuationMode.COLLECTED,
  packageValuesMap: Map<String, Double> = emptyMap(),
  packageValueSettings: List<PackageValueSetting> = emptyList(),
  reconciliationSummary: ReconciliationSummary? = null,
  unmappedPackageServices: List<UnmappedPackageService> = emptyList(),
  showPackageValuesDialog: Boolean = false,
  selectedReportType: ReportType,
  selectedUploadBranch: String,
  selectedUploadDate: String,
  parsedSpreadsheet: ParsedSpreadsheet?,
  validationReport: ValidationReport?,
  isUploading: Boolean,
  uploadHistory: List<UploadRecord>,
  duplicatePrompt: DuplicateCheckResult?,
  lastUploadProof: SaveUploadResult?,
  onTabSelected: (DashboardTab) -> Unit,
  onFilterStateChange: (FilterState) -> Unit,
  onToggleSkeleton: () -> Unit,
  onInspectMetric: (title: String, formula: String, value: String, rows: List<RawClinicRow>) -> Unit,
  onDismissAudit: () -> Unit,
  onSetValuationMode: (ValuationMode) -> Unit = {},
  onOpenPackageValuesDialog: () -> Unit = {},
  onClosePackageValuesDialog: () -> Unit = {},
  onSavePackageValue: (serviceName: String, perSessionValue: Double, category: String, notes: String) -> Unit = { _, _, _, _ -> },
  onDeletePackageValue: (serviceName: String) -> Unit = {},
  aiSummaryState: AiSummaryUiState = AiSummaryUiState.Loading,
  onRegenerateAiSummary: () -> Unit = {},
  isOwner: Boolean = true,
  askChatMessages: List<AskChatMessage> = emptyList(),
  isAskAgentThinking: Boolean = false,
  starterQuestions: List<String> = emptyList(),
  onSendAskMessage: (String) -> Unit = {},
  onClearAskHistory: () -> Unit = {},
  onSelectReportType: (ReportType) -> Unit,
  onSelectUploadBranch: (String) -> Unit,
  onSelectUploadDate: (String) -> Unit,
  onFilePicked: (android.net.Uri, String) -> Unit,
  onLoadSample: () -> Unit,
  onClearParsedFile: () -> Unit,
  onInitiateSave: () -> Unit,
  onConfirmReplace: () -> Unit,
  onDismissDuplicate: () -> Unit,
  onDismissProof: () -> Unit,
  onDeleteUpload: (String) -> Unit,
  pharmacyKpis: PharmacyKpis = PharmacyKpis(),
  pharmacyRankings: PharmacyRankings = PharmacyRankings(),
  pharmacyReconciliation: PharmacyReconciliation = PharmacyReconciliation(),
  pharmacyItems: List<PharmacyItem> = emptyList(),
  pharmacySearchQuery: String = "",
  pharmacySortOption: PharmacySortOption = PharmacySortOption.REVENUE_DESC,
  onPharmacySearchQueryChange: (String) -> Unit = {},
  onPharmacySortOptionChange: (PharmacySortOption) -> Unit = {},
  usersAndRoles: List<RoleDoc> = emptyList(),
  onRefreshUsers: () -> Unit = {},
  onSendPasswordReset: suspend (String) -> Result<Unit> = { Result.success(Unit) },
  onChangePassword: suspend (String, String) -> Result<Unit> = { _, _ -> Result.success(Unit) },
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showMobileFilterSheet by remember { mutableStateOf(false) }
  var showChangePasswordDialog by remember { mutableStateOf(false) }

  val visibleTabs = remember(user.isSuperAdmin) {
    if (user.isSuperAdmin) {
      DashboardTab.values().toList()
    } else {
      listOf(DashboardTab.DASHBOARD, DashboardTab.SERVICES, DashboardTab.PHARMACY, DashboardTab.AI)
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val isDesktop = maxWidth >= 600.dp

    if (isDesktop) {
      // DESKTOP / TABLET EXPANDED LAYOUT: Left Sidebar + Right Main Content (1440px responsive)
      Row(
        modifier = Modifier
          .fillMaxSize()
          .background(BackgroundCanvas)
          .statusBarsPadding()
          .navigationBarsPadding()
      ) {
        // LEFT SIDEBAR (Desktop 1440px)
        DesktopSidebar(
          user = user,
          activeTab = activeTab,
          visibleTabs = visibleTabs,
          onTabSelected = onTabSelected,
          onChangePassword = { showChangePasswordDialog = true },
          onLogout = onLogout,
          modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
        )

        VerticalDivider(color = BorderSubtle, thickness = 1.dp)

        // MAIN CONTENT AREA
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
          // Global Filter Bar on Desktop
          GlobalFilterBar(
            filterState = filterState,
            onDatePresetSelected = { onFilterStateChange(filterState.copy(datePreset = it)) },
            onCustomDateRangeSet = { s, e ->
              onFilterStateChange(
                filterState.copy(
                  datePreset = com.example.model.DatePreset.CUSTOM,
                  customStartDate = s,
                  customEndDate = e
                )
              )
            },
            onBranchSelected = { onFilterStateChange(filterState.copy(selectedBranch = it)) },
            onDoctorSelected = { onFilterStateChange(filterState.copy(selectedDoctor = it)) },
            onCategorySelected = { onFilterStateChange(filterState.copy(selectedCategory = it)) },
            onResetFilters = { onFilterStateChange(FilterState()) }
          )

          // Centered Content container for expansive screens (generous whitespace at 1440px)
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter
          ) {
            Box(modifier = Modifier.widthIn(max = 1200.dp)) {
              when (activeTab) {
                DashboardTab.DASHBOARD -> {
                  DashboardScreen(
                    kpis = dashboardKpis,
                    dailyTrend = dailyTrend,
                    serviceRankings = serviceRankings,
                    categorySplit = categorySplit,
                    doctorRevenue = doctorRevenue,
                    dominantVsUndersold = dominantVsUndersold,
                    filteredRows = filteredRows,
                    filterState = filterState,
                    isSkeletonLoading = isSkeletonLoading,
                    auditInspectionData = auditInspectionData,
                    valuationMode = valuationMode,
                    packageValuesMap = packageValuesMap,
                    packageValueSettings = packageValueSettings,
                    reconciliationSummary = reconciliationSummary,
                    unmappedPackageServices = unmappedPackageServices,
                    showPackageValuesDialog = showPackageValuesDialog,
                    onSetValuationMode = onSetValuationMode,
                    onOpenPackageValuesDialog = onOpenPackageValuesDialog,
                    onClosePackageValuesDialog = onClosePackageValuesDialog,
                    onSavePackageValue = onSavePackageValue,
                    onDeletePackageValue = onDeletePackageValue,
                    onInspectMetric = onInspectMetric,
                    onDismissAudit = onDismissAudit,
                    onToggleSkeleton = onToggleSkeleton,
                    aiSummaryState = aiSummaryState,
                    onRegenerateAiSummary = onRegenerateAiSummary,
                    canManageSettings = user.isSuperAdmin
                  )
                }

                DashboardTab.UPLOAD -> {
                  UploadScreen(
                    selectedReportType = selectedReportType,
                    selectedBranch = selectedUploadBranch,
                    selectedDate = selectedUploadDate,
                    parsedSpreadsheet = parsedSpreadsheet,
                    validationReport = validationReport,
                    isUploading = isUploading,
                    uploadHistory = uploadHistory,
                    duplicatePrompt = duplicatePrompt,
                    lastUploadProof = lastUploadProof,
                    onSelectReportType = onSelectReportType,
                    onSelectBranch = onSelectUploadBranch,
                    onSelectDate = onSelectUploadDate,
                    onFilePicked = onFilePicked,
                    onLoadSample = onLoadSample,
                    onClearParsedFile = onClearParsedFile,
                    onInitiateSave = onInitiateSave,
                    onConfirmReplace = onConfirmReplace,
                    onDismissDuplicate = onDismissDuplicate,
                    onDismissProof = onDismissProof,
                    onDeleteUpload = onDeleteUpload
                  )
                }

                DashboardTab.PHARMACY -> {
                  PharmacyScreen(
                    kpis = pharmacyKpis,
                    rankings = pharmacyRankings,
                    reconciliation = pharmacyReconciliation,
                    filteredItems = pharmacyItems,
                    searchQuery = pharmacySearchQuery,
                    selectedSortOption = pharmacySortOption,
                    filterState = filterState,
                    isSkeletonLoading = isSkeletonLoading,
                    onSearchQueryChange = onPharmacySearchQueryChange,
                    onSortOptionChange = onPharmacySortOptionChange,
                    onToggleSkeleton = onToggleSkeleton
                  )
                }

                DashboardTab.AI -> {
                  AskScreen(
                    user = user,
                    isOwner = isOwner,
                    messages = askChatMessages,
                    isThinking = isAskAgentThinking,
                    starterQuestions = starterQuestions,
                    filterState = filterState,
                    onSendMessage = onSendAskMessage,
                    onClearHistory = onClearAskHistory
                  )
                }

                DashboardTab.USERS -> {
                  UsersAndAccessScreen(
                    currentUser = user,
                    usersAndRoles = usersAndRoles,
                    onRefreshUsers = onRefreshUsers,
                    onSendPasswordReset = onSendPasswordReset
                  )
                }

                else -> {
                  PlaceholderScreen(
                    tab = activeTab,
                    filterState = filterState,
                    isSkeletonLoading = isSkeletonLoading,
                    onToggleSkeleton = onToggleSkeleton
                  )
                }
              }
            }
          }
        }
      }
    } else {
      // MOBILE COMPACT LAYOUT (390px): Top Bar + Collapsible Filter Trigger + Content + Bottom Tab Bar
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundCanvas,
        topBar = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(SurfaceCard)
              .statusBarsPadding()
          ) {
            MobileTopBar(
              user = user,
              onChangePassword = { showChangePasswordDialog = true },
              onLogout = onLogout
            )

            // Sleek Non-Scrolling Mobile Filter Pill (Collapses into FilterBottomSheet)
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { showMobileFilterSheet = true }
                .testTag("mobile_filter_trigger_bar"),
              color = SurfaceSubtle,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .defaultMinSize(minHeight = 48.dp)
                  .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Box(
                    modifier = Modifier
                      .size(32.dp)
                      .clip(CircleShape)
                      .background(TealLightContainer),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Tune,
                      contentDescription = "Filters",
                      tint = TealPrimary,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                  Column {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Text(
                        text = "Active Scope",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextHeadline
                      )
                      if (filterState.activeFilterCount > 0) {
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
                    Text(
                      text = "${filterState.datePreset.label} · ${filterState.selectedBranch}",
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                      color = TextMuted,
                      maxLines = 1
                    )
                  }
                }

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = "Filters",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TealPrimary
                  )
                  Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand Filters",
                    tint = TealPrimary,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        },
        bottomBar = {
          MobileBottomNavBar(
            activeTab = activeTab,
            visibleTabs = visibleTabs,
            onTabSelected = onTabSelected
          )
        }
      ) { innerPadding ->
        when (activeTab) {
          DashboardTab.DASHBOARD -> {
            DashboardScreen(
              kpis = dashboardKpis,
              dailyTrend = dailyTrend,
              serviceRankings = serviceRankings,
              categorySplit = categorySplit,
              doctorRevenue = doctorRevenue,
              dominantVsUndersold = dominantVsUndersold,
              filteredRows = filteredRows,
              filterState = filterState,
              isSkeletonLoading = isSkeletonLoading,
              auditInspectionData = auditInspectionData,
              valuationMode = valuationMode,
              packageValuesMap = packageValuesMap,
              packageValueSettings = packageValueSettings,
              reconciliationSummary = reconciliationSummary,
              unmappedPackageServices = unmappedPackageServices,
              showPackageValuesDialog = showPackageValuesDialog,
              onSetValuationMode = onSetValuationMode,
              onOpenPackageValuesDialog = onOpenPackageValuesDialog,
              onClosePackageValuesDialog = onClosePackageValuesDialog,
              onSavePackageValue = onSavePackageValue,
              onDeletePackageValue = onDeletePackageValue,
              onInspectMetric = onInspectMetric,
              onDismissAudit = onDismissAudit,
              onToggleSkeleton = onToggleSkeleton,
              aiSummaryState = aiSummaryState,
              onRegenerateAiSummary = onRegenerateAiSummary,
              canManageSettings = user.isSuperAdmin,
              modifier = Modifier.padding(innerPadding)
            )
          }

          DashboardTab.PHARMACY -> {
            PharmacyScreen(
              kpis = pharmacyKpis,
              rankings = pharmacyRankings,
              reconciliation = pharmacyReconciliation,
              filteredItems = pharmacyItems,
              searchQuery = pharmacySearchQuery,
              selectedSortOption = pharmacySortOption,
              filterState = filterState,
              isSkeletonLoading = isSkeletonLoading,
              onSearchQueryChange = onPharmacySearchQueryChange,
              onSortOptionChange = onPharmacySortOptionChange,
              onToggleSkeleton = onToggleSkeleton,
              modifier = Modifier.padding(innerPadding)
            )
          }

          DashboardTab.UPLOAD -> {
            UploadScreen(
              selectedReportType = selectedReportType,
              selectedBranch = selectedUploadBranch,
              selectedDate = selectedUploadDate,
              parsedSpreadsheet = parsedSpreadsheet,
              validationReport = validationReport,
              isUploading = isUploading,
              uploadHistory = uploadHistory,
              duplicatePrompt = duplicatePrompt,
              lastUploadProof = lastUploadProof,
              onSelectReportType = onSelectReportType,
              onSelectBranch = onSelectUploadBranch,
              onSelectDate = onSelectUploadDate,
              onFilePicked = onFilePicked,
              onLoadSample = onLoadSample,
              onClearParsedFile = onClearParsedFile,
              onInitiateSave = onInitiateSave,
              onConfirmReplace = onConfirmReplace,
              onDismissDuplicate = onDismissDuplicate,
              onDismissProof = onDismissProof,
              onDeleteUpload = onDeleteUpload,
              modifier = Modifier.padding(innerPadding)
            )
          }

          DashboardTab.AI -> {
            AskScreen(
              user = user,
              isOwner = isOwner,
              messages = askChatMessages,
              isThinking = isAskAgentThinking,
              starterQuestions = starterQuestions,
              filterState = filterState,
              onSendMessage = onSendAskMessage,
              onClearHistory = onClearAskHistory,
              modifier = Modifier.padding(innerPadding)
            )
          }

          DashboardTab.USERS -> {
            UsersAndAccessScreen(
              currentUser = user,
              usersAndRoles = usersAndRoles,
              onRefreshUsers = onRefreshUsers,
              onSendPasswordReset = onSendPasswordReset,
              modifier = Modifier.padding(innerPadding)
            )
          }

          else -> {
            PlaceholderScreen(
              tab = activeTab,
              filterState = filterState,
              isSkeletonLoading = isSkeletonLoading,
              onToggleSkeleton = onToggleSkeleton,
              modifier = Modifier.padding(innerPadding)
            )
          }
        }
      }

      // 390px Mobile Filter Bottom Sheet
      if (showMobileFilterSheet) {
        FilterBottomSheet(
          filterState = filterState,
          onDismiss = { showMobileFilterSheet = false },
          onApplyFilters = { updated ->
            onFilterStateChange(updated)
          },
          onResetFilters = {
            onFilterStateChange(FilterState())
          }
        )
      }

      if (showChangePasswordDialog) {
        ChangePasswordDialog(
          userEmail = user.email,
          onDismiss = { showChangePasswordDialog = false },
          onChangePassword = onChangePassword
        )
      }
    }
  }
}

// -------------------------------------------------------------
// DESKTOP SIDEBAR COMPONENT
// -------------------------------------------------------------
@Composable
private fun DesktopSidebar(
  user: UserProfile,
  activeTab: DashboardTab,
  visibleTabs: List<DashboardTab>,
  onTabSelected: (DashboardTab) -> Unit,
  onChangePassword: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    color = SurfaceCard
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(20.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        // Brand Header
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(TealLightContainer)
              .border(1.dp, TealAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.HealthAndSafety,
              contentDescription = "CEO Pulse",
              tint = TealPrimary,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "CEO PULSE",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = TextHeadline
            )
            Text(
              text = "Clinic Group Analytics",
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Role Gate Verified Badge
        Surface(
          color = RoleAllowedBg,
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, RoleAllowedGreen.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = RoleAllowedGreen,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Role: ${user.roleDisplayName}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = RoleAllowedGreen
            )
          }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
          text = "NAVIGATION",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = TextSubtle,
          modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
        )

        // Sidebar Navigation Links
        visibleTabs.forEach { tab ->
          val isSelected = activeTab == tab
          val icon = getTabIcon(tab)

          Surface(
            color = if (isSelected) TealLightContainer else Color.Transparent,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .clickable { onTabSelected(tab) }
              .testTag("nav_item_${tab.name.lowercase()}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = icon,
                contentDescription = tab.title,
                tint = if (isSelected) TealPrimary else TextMuted,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = tab.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TealPrimary else TextBody
              )
            }
          }
          Spacer(modifier = Modifier.height(4.dp))
        }
      }

      // Bottom User Profile & Sign Out
      Column {
        HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
        Spacer(modifier = Modifier.height(14.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(TealLightContainer),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = user.email.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = TealPrimary,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = user.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextHeadline,
                maxLines = 1
              )
              Text(
                text = user.email,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onChangePassword,
              modifier = Modifier.testTag("desktop_change_password_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Change Password",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
              )
            }
            IconButton(
              onClick = onLogout,
              modifier = Modifier.testTag("desktop_logout_btn")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Sign Out",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// MOBILE TOP BAR & BOTTOM NAV BAR
// -------------------------------------------------------------
@Composable
private fun MobileTopBar(
  user: UserProfile,
  onChangePassword: () -> Unit,
  onLogout: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(TealLightContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.HealthAndSafety,
          contentDescription = null,
          tint = TealPrimary,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "CEO PULSE",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextHeadline
        )
        Text(
          text = user.email,
          style = MaterialTheme.typography.labelSmall,
          color = TextMuted
        )
      }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      IconButton(
        onClick = onChangePassword,
        modifier = Modifier.testTag("mobile_change_password_btn")
      ) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = "Change Password",
          tint = TextMuted,
          modifier = Modifier.size(18.dp)
        )
      }
      IconButton(
        onClick = onLogout,
        modifier = Modifier.testTag("mobile_logout_btn")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = "Logout",
          tint = TextMuted,
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }
}

@Composable
private fun MobileBottomNavBar(
  activeTab: DashboardTab,
  visibleTabs: List<DashboardTab>,
  onTabSelected: (DashboardTab) -> Unit
) {
  NavigationBar(
    containerColor = SurfaceCard,
    contentColor = TealPrimary,
    tonalElevation = 8.dp,
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .testTag("bottom_tab_navigation")
  ) {
    visibleTabs.forEach { tab ->
      val isSelected = activeTab == tab
      NavigationBarItem(
        selected = isSelected,
        onClick = { onTabSelected(tab) },
        icon = {
          Icon(
            imageVector = getTabIcon(tab),
            contentDescription = tab.title
          )
        },
        label = {
          Text(
            text = tab.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = TealPrimary,
          selectedTextColor = TealPrimary,
          indicatorColor = TealLightContainer,
          unselectedIconColor = TextMuted,
          unselectedTextColor = TextMuted
        ),
        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
      )
    }
  }
}

private fun getTabIcon(tab: DashboardTab): ImageVector {
  return when (tab) {
    DashboardTab.DASHBOARD -> Icons.Default.Dashboard
    DashboardTab.SERVICES -> Icons.Default.MedicalServices
    DashboardTab.PHARMACY -> Icons.Default.LocalPharmacy
    DashboardTab.UPLOAD -> Icons.Default.CloudUpload
    DashboardTab.AI -> Icons.Default.AutoAwesome
    DashboardTab.USERS -> Icons.Default.AdminPanelSettings
  }
}
