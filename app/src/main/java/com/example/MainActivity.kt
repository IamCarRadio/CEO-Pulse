package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AuthState
import com.example.model.DashboardTab
import com.example.ui.CeoPulseViewModel
import com.example.ui.ForceChangePasswordScreen
import com.example.ui.LoginScreen
import com.example.ui.MainAppLayout
import com.example.ui.theme.BackgroundCanvas
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val viewModel: CeoPulseViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = BackgroundCanvas
        ) {
          CeoPulseApp(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
fun CeoPulseApp(viewModel: CeoPulseViewModel) {
  val authState by viewModel.authState.collectAsStateWithLifecycle()
  val filterState by viewModel.filterState.collectAsStateWithLifecycle()
  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val isSkeletonLoading by viewModel.isSkeletonLoading.collectAsStateWithLifecycle()
  val firebaseConfigured by viewModel.firebaseConfigStatus.collectAsStateWithLifecycle()
  val configMessage by viewModel.configDialogMessage.collectAsStateWithLifecycle()

  // Initial Setup State
  val setupResults by viewModel.setupResults.collectAsStateWithLifecycle()
  val isSetupRunning by viewModel.isSetupRunning.collectAsStateWithLifecycle()

  // Users & Access State
  val usersAndRoles by viewModel.usersAndRoles.collectAsStateWithLifecycle()

  // Dashboard Metrics State (Pure Module)
  val dashboardKpis by viewModel.dashboardKpis.collectAsStateWithLifecycle()
  val dailyTrend by viewModel.dailyTrend.collectAsStateWithLifecycle()
  val serviceRankings by viewModel.serviceRankings.collectAsStateWithLifecycle()
  val categorySplit by viewModel.categorySplit.collectAsStateWithLifecycle()
  val doctorRevenue by viewModel.doctorRevenue.collectAsStateWithLifecycle()
  val dominantVsUndersold by viewModel.dominantVsUndersold.collectAsStateWithLifecycle()
  val filteredRows by viewModel.filteredRows.collectAsStateWithLifecycle()
  val auditInspectionData by viewModel.auditInspectionData.collectAsStateWithLifecycle()

  // Package Rectification State
  val valuationMode by viewModel.valuationMode.collectAsStateWithLifecycle()
  val packageValuesMap by viewModel.packageValuesMap.collectAsStateWithLifecycle()
  val packageValueSettings by viewModel.packageValueSettings.collectAsStateWithLifecycle()
  val reconciliationSummary by viewModel.reconciliationSummary.collectAsStateWithLifecycle()
  val unmappedPackageServices by viewModel.unmappedPackageServices.collectAsStateWithLifecycle()
  val showPackageValuesDialog by viewModel.showPackageValuesDialog.collectAsStateWithLifecycle()

  // AI Summary State (Gemini powered, cached per date)
  val aiSummaryState by viewModel.aiSummaryState.collectAsStateWithLifecycle()

  // Ask Executive Agent State (Owner only, Gemini function calling)
  val askChatMessages by viewModel.askChatMessages.collectAsStateWithLifecycle()
  val isAskAgentThinking by viewModel.isAskAgentThinking.collectAsStateWithLifecycle()
  val starterQuestions = viewModel.starterQuestions

  // Upload Screen State
  val selectedReportType by viewModel.selectedReportType.collectAsStateWithLifecycle()
  val selectedUploadBranch by viewModel.selectedUploadBranch.collectAsStateWithLifecycle()
  val selectedUploadDate by viewModel.selectedUploadDate.collectAsStateWithLifecycle()
  val parsedSpreadsheet by viewModel.parsedSpreadsheet.collectAsStateWithLifecycle()
  val validationReport by viewModel.validationReport.collectAsStateWithLifecycle()
  val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
  val uploadHistory by viewModel.uploadHistory.collectAsStateWithLifecycle()
  val duplicatePrompt by viewModel.duplicatePrompt.collectAsStateWithLifecycle()
  val lastUploadProof by viewModel.lastUploadProof.collectAsStateWithLifecycle()

  // Pharmacy Screen State
  val pharmacyKpis by viewModel.pharmacyKpis.collectAsStateWithLifecycle()
  val pharmacyRankings by viewModel.pharmacyRankings.collectAsStateWithLifecycle()
  val pharmacyReconciliation by viewModel.pharmacyReconciliation.collectAsStateWithLifecycle()
  val pharmacyItems by viewModel.pharmacyItems.collectAsStateWithLifecycle()
  val pharmacySearchQuery by viewModel.pharmacySearchQuery.collectAsStateWithLifecycle()
  val pharmacySortOption by viewModel.pharmacySortOption.collectAsStateWithLifecycle()

  when (val state = authState) {
    // STEP 4: Forced Password Change Gate (blocks the whole app until password is changed)
    is AuthState.MustChangePassword -> {
      ForceChangePasswordScreen(
        user = state.user,
        onChangePassword = { currentPass, newPass ->
          viewModel.changePassword(state.user.email, currentPass, newPass)
        },
        onSignOut = { viewModel.logout() }
      )
    }

    is AuthState.Authenticated -> {
      // Handle back press: if not on Dashboard, navigate back to Dashboard; otherwise exit
      BackHandler(enabled = activeTab != DashboardTab.DASHBOARD) {
        viewModel.setActiveTab(DashboardTab.DASHBOARD)
      }

      MainAppLayout(
        user = state.user,
        activeTab = activeTab,
        filterState = filterState,
        isSkeletonLoading = isSkeletonLoading,
        dashboardKpis = dashboardKpis,
        dailyTrend = dailyTrend,
        serviceRankings = serviceRankings,
        categorySplit = categorySplit,
        doctorRevenue = doctorRevenue,
        dominantVsUndersold = dominantVsUndersold,
        filteredRows = filteredRows,
        auditInspectionData = auditInspectionData,
        valuationMode = valuationMode,
        packageValuesMap = packageValuesMap,
        packageValueSettings = packageValueSettings,
        reconciliationSummary = reconciliationSummary,
        unmappedPackageServices = unmappedPackageServices,
        showPackageValuesDialog = showPackageValuesDialog,
        selectedReportType = selectedReportType,
        selectedUploadBranch = selectedUploadBranch,
        selectedUploadDate = selectedUploadDate,
        parsedSpreadsheet = parsedSpreadsheet,
        validationReport = validationReport,
        isUploading = isUploading,
        uploadHistory = uploadHistory,
        duplicatePrompt = duplicatePrompt,
        lastUploadProof = lastUploadProof,
        onTabSelected = { viewModel.setActiveTab(it) },
        onFilterStateChange = { newFilter ->
          viewModel.setDatePreset(newFilter.datePreset)
          viewModel.setBranch(newFilter.selectedBranch)
          viewModel.setDoctor(newFilter.selectedDoctor)
          viewModel.setCategory(newFilter.selectedCategory)
          if (newFilter.customStartDate.isNotBlank() && newFilter.customEndDate.isNotBlank()) {
            viewModel.setCustomDateRange(newFilter.customStartDate, newFilter.customEndDate)
          }
        },
        onToggleSkeleton = { viewModel.toggleSkeletonLoading() },
        onInspectMetric = { title, formula, value, rows ->
          viewModel.inspectMetric(title, formula, value, rows)
        },
        onDismissAudit = { viewModel.dismissAudit() },
        onSetValuationMode = { viewModel.setValuationMode(it) },
        onOpenPackageValuesDialog = { viewModel.openPackageValuesDialog() },
        onClosePackageValuesDialog = { viewModel.closePackageValuesDialog() },
        onSavePackageValue = { name, value, cat, notes -> viewModel.savePackageValue(name, value, cat, notes) },
        onDeletePackageValue = { viewModel.deletePackageValue(it) },
        aiSummaryState = aiSummaryState,
        onRegenerateAiSummary = { viewModel.regenerateAiSummary() },
        isOwner = viewModel.isOwner(state.user),
        askChatMessages = askChatMessages,
        isAskAgentThinking = isAskAgentThinking,
        starterQuestions = starterQuestions,
        onSendAskMessage = { viewModel.sendAskMessage(it) },
        onClearAskHistory = { viewModel.clearAskHistory() },
        onSelectReportType = { viewModel.setSelectedReportType(it) },
        onSelectUploadBranch = { viewModel.setSelectedUploadBranch(it) },
        onSelectUploadDate = { viewModel.setSelectedUploadDate(it) },
        onFilePicked = { uri, name -> viewModel.parseUri(uri, name) },
        onLoadSample = { viewModel.loadSampleServiceSalesExport() },
        onClearParsedFile = { viewModel.clearCurrentParsedFile() },
        onInitiateSave = { viewModel.initiateSaveUpload() },
        onConfirmReplace = { viewModel.confirmReplaceUpload() },
        onDismissDuplicate = { viewModel.dismissDuplicatePrompt() },
        onDismissProof = { viewModel.dismissLastUploadProof() },
        onDeleteUpload = { viewModel.deleteUpload(it) },
        pharmacyKpis = pharmacyKpis,
        pharmacyRankings = pharmacyRankings,
        pharmacyReconciliation = pharmacyReconciliation,
        pharmacyItems = pharmacyItems,
        pharmacySearchQuery = pharmacySearchQuery,
        pharmacySortOption = pharmacySortOption,
        onPharmacySearchQueryChange = { viewModel.setPharmacySearchQuery(it) },
        onPharmacySortOptionChange = { viewModel.setPharmacySortOption(it) },
        usersAndRoles = usersAndRoles,
        onRefreshUsers = { viewModel.loadUsersAndRoles() },
        onSendPasswordReset = { viewModel.sendPasswordResetEmail(it) },
        onChangePassword = { currentPass, newPass ->
          viewModel.changePassword(state.user.email, currentPass, newPass)
        },
        onLogout = { viewModel.logout() }
      )
    }

    else -> {
      LoginScreen(
        authState = authState,
        firebaseConfigured = firebaseConfigured,
        configMessage = configMessage,
        rememberedEmail = viewModel.getRememberedEmail(),
        isRememberMeInitiallyChecked = viewModel.isRememberMeEnabled(),
        setupResults = setupResults,
        isSetupRunning = isSetupRunning,
        onLogin = { email, password, rememberMe -> viewModel.login(email, password, rememberMe) },
        onRunInitialSetup = { viewModel.runInitialAccountSetup() },
        onDismissSetupResults = { viewModel.dismissSetupResults() },
        onSendPasswordReset = { viewModel.sendPasswordResetEmail(it) },
        onApplyConfig = { rawConfig -> viewModel.applyPastedFirebaseConfig(rawConfig) },
        onClearConfigMessage = { viewModel.clearConfigMessage() },
        onClearError = { viewModel.clearAuthError() }
      )
    }
  }
}
