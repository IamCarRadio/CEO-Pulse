package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AskToolsHandler
import com.example.data.AuthRepository
import com.example.data.FirebaseConfigManager
import com.example.data.GeminiAskAgentService
import com.example.data.SaveUploadResult
import com.example.data.UploadRepository
import com.example.model.AskChatMessage
import com.example.model.AuthResult
import com.example.model.AuthState
import com.example.model.DashboardTab
import com.example.model.DatePreset
import com.example.model.DoctorRevenueMetric
import com.example.model.DualStreamRevenue
import com.example.model.DuplicateCheckResult
import com.example.model.FilterState
import com.example.model.PharmacyItem
import com.example.model.PharmacyKpis
import com.example.model.PharmacyProductRanking
import com.example.model.PharmacyRankings
import com.example.model.PharmacyReconciliation
import com.example.model.PharmacySortOption
import com.example.model.ReportType
import com.example.model.UploadRecord
import com.example.model.UserProfile
import com.example.model.ValidationReport
import com.example.util.ExcelParser
import com.example.util.ParsedSpreadsheet
import com.example.util.ReportValidator
import com.example.util.SampleExportData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CeoPulseViewModel(application: Application) : AndroidViewModel(application) {

  private val configManager = FirebaseConfigManager(application)
  private val authRepository = AuthRepository(application, configManager)
  private val uploadRepository = UploadRepository(application, configManager)
  private val packageValuesRepository = com.example.data.PackageValuesRepository(application)
  private val aiSummaryRepository = com.example.data.AiSummaryRepository(application)
  private val geminiSummaryService = com.example.data.GeminiSummaryService()

  private val _aiSummaryState = MutableStateFlow<com.example.model.AiSummaryUiState>(com.example.model.AiSummaryUiState.Loading)
  val aiSummaryState: StateFlow<com.example.model.AiSummaryUiState> = _aiSummaryState.asStateFlow()

  private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
  val authState: StateFlow<AuthState> = _authState.asStateFlow()

  private val _setupResults = MutableStateFlow<List<com.example.model.SetupAccountStatus>?>(null)
  val setupResults: StateFlow<List<com.example.model.SetupAccountStatus>?> = _setupResults.asStateFlow()

  private val _isSetupRunning = MutableStateFlow(false)
  val isSetupRunning: StateFlow<Boolean> = _isSetupRunning.asStateFlow()

  private val _usersAndRoles = MutableStateFlow<List<com.example.model.RoleDoc>>(emptyList())
  val usersAndRoles: StateFlow<List<com.example.model.RoleDoc>> = _usersAndRoles.asStateFlow()

  private var lastActivityMillis: Long = System.currentTimeMillis()

  private val _filterState = MutableStateFlow(FilterState())
  val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

  private val _activeTab = MutableStateFlow(DashboardTab.DASHBOARD)
  val activeTab: StateFlow<DashboardTab> = _activeTab.asStateFlow()

  // ------------------------------------------------------------------
  // PACKAGE RECTIFICATION STATE
  // ------------------------------------------------------------------
  private val _valuationMode = MutableStateFlow(com.example.model.ValuationMode.COLLECTED)
  val valuationMode: StateFlow<com.example.model.ValuationMode> = _valuationMode.asStateFlow()

  private val _packageValuesMap = MutableStateFlow(packageValuesRepository.getPackageValuesMap())
  val packageValuesMap: StateFlow<Map<String, Double>> = _packageValuesMap.asStateFlow()

  private val _packageValueSettings = MutableStateFlow(packageValuesRepository.getPackageValueSettingsList())
  val packageValueSettings: StateFlow<List<com.example.model.PackageValueSetting>> = _packageValueSettings.asStateFlow()

  private val _reconciliationSummary = MutableStateFlow(
    com.example.util.MetricsCalculator.calculateReconciliation(emptyList(), emptyMap())
  )
  val reconciliationSummary: StateFlow<com.example.model.ReconciliationSummary> = _reconciliationSummary.asStateFlow()

  private val _unmappedPackageServices = MutableStateFlow<List<com.example.model.UnmappedPackageService>>(emptyList())
  val unmappedPackageServices: StateFlow<List<com.example.model.UnmappedPackageService>> = _unmappedPackageServices.asStateFlow()

  private val _showPackageValuesDialog = MutableStateFlow(false)
  val showPackageValuesDialog: StateFlow<Boolean> = _showPackageValuesDialog.asStateFlow()

  // Smooth skeleton loading state for demonstration and data loading
  private val _isSkeletonLoading = MutableStateFlow(false)
  val isSkeletonLoading: StateFlow<Boolean> = _isSkeletonLoading.asStateFlow()

  private val _firebaseConfigStatus = MutableStateFlow(configManager.isConfigured())
  val firebaseConfigStatus: StateFlow<Boolean> = _firebaseConfigStatus.asStateFlow()

  private val _configDialogMessage = MutableStateFlow<String?>(null)
  val configDialogMessage: StateFlow<String?> = _configDialogMessage.asStateFlow()

  // ------------------------------------------------------------------
  // UPLOAD SCREEN STATE
  // ------------------------------------------------------------------
  private val _selectedReportType = MutableStateFlow(ReportType.SERVICE_SALES)
  val selectedReportType: StateFlow<ReportType> = _selectedReportType.asStateFlow()

  private val _selectedUploadBranch = MutableStateFlow("Downtown Executive Clinic")
  val selectedUploadBranch: StateFlow<String> = _selectedUploadBranch.asStateFlow()

  private val _selectedUploadDate = MutableStateFlow("2026-10-04")
  val selectedUploadDate: StateFlow<String> = _selectedUploadDate.asStateFlow()

  private val _parsedSpreadsheet = MutableStateFlow<ParsedSpreadsheet?>(null)
  val parsedSpreadsheet: StateFlow<ParsedSpreadsheet?> = _parsedSpreadsheet.asStateFlow()

  private val _validationReport = MutableStateFlow<ValidationReport?>(null)
  val validationReport: StateFlow<ValidationReport?> = _validationReport.asStateFlow()

  private val _isUploading = MutableStateFlow(false)
  val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

  private val _uploadHistory = MutableStateFlow<List<UploadRecord>>(emptyList())
  val uploadHistory: StateFlow<List<UploadRecord>> = _uploadHistory.asStateFlow()

  private val _duplicatePrompt = MutableStateFlow<DuplicateCheckResult?>(null)
  val duplicatePrompt: StateFlow<DuplicateCheckResult?> = _duplicatePrompt.asStateFlow()

  private val _lastUploadProof = MutableStateFlow<SaveUploadResult?>(null)
  val lastUploadProof: StateFlow<SaveUploadResult?> = _lastUploadProof.asStateFlow()

  // ------------------------------------------------------------------
  // DASHBOARD STATE (Computed strictly by pure MetricsCalculator module)
  // ------------------------------------------------------------------
  private val _rawRows = MutableStateFlow<List<com.example.model.RawClinicRow>>(emptyList())
  val rawRows: StateFlow<List<com.example.model.RawClinicRow>> = _rawRows.asStateFlow()

  private val _filteredRows = MutableStateFlow<List<com.example.model.RawClinicRow>>(emptyList())
  val filteredRows: StateFlow<List<com.example.model.RawClinicRow>> = _filteredRows.asStateFlow()

  private val _dashboardKpis = MutableStateFlow(
    com.example.util.MetricsCalculator.calculateKpis(emptyList(), emptyList())
  )
  val dashboardKpis: StateFlow<com.example.model.DashboardKpis> = _dashboardKpis.asStateFlow()

  private val _dailyTrend = MutableStateFlow<List<com.example.model.DailyTrendPoint>>(emptyList())
  val dailyTrend: StateFlow<List<com.example.model.DailyTrendPoint>> = _dailyTrend.asStateFlow()

  private val _serviceRankings = MutableStateFlow(
    com.example.model.ServiceRankings(emptyList(), emptyList(), emptyList(), emptyList())
  )
  val serviceRankings: StateFlow<com.example.model.ServiceRankings> = _serviceRankings.asStateFlow()

  private val _categorySplit = MutableStateFlow<List<com.example.model.CategoryMetric>>(emptyList())
  val categorySplit: StateFlow<List<com.example.model.CategoryMetric>> = _categorySplit.asStateFlow()

  private val _doctorRevenue = MutableStateFlow<List<com.example.model.DoctorRevenueMetric>>(emptyList())
  val doctorRevenue: StateFlow<List<com.example.model.DoctorRevenueMetric>> = _doctorRevenue.asStateFlow()

  private val _dominantVsUndersold = MutableStateFlow(
    com.example.model.DominantUndersoldAnalysis(emptyList(), emptyList())
  )
  val dominantVsUndersold: StateFlow<com.example.model.DominantUndersoldAnalysis> = _dominantVsUndersold.asStateFlow()

  // Hidden Debug / Audit View Modal state
  private val _auditInspectionData = MutableStateFlow<com.example.model.AuditInspectionData?>(null)
  val auditInspectionData: StateFlow<com.example.model.AuditInspectionData?> = _auditInspectionData.asStateFlow()

  // ------------------------------------------------------------------
  // PHARMACY ITEM-WISE MARGIN STATE
  // ------------------------------------------------------------------
  private val _pharmacyItems = MutableStateFlow<List<PharmacyItem>>(emptyList())
  val pharmacyItems: StateFlow<List<PharmacyItem>> = _pharmacyItems.asStateFlow()

  private val _filteredPharmacyItems = MutableStateFlow<List<PharmacyItem>>(emptyList())
  val filteredPharmacyItems: StateFlow<List<PharmacyItem>> = _filteredPharmacyItems.asStateFlow()

  private val _pharmacyKpis = MutableStateFlow(
    com.example.util.MetricsCalculator.calculatePharmacyKpis(emptyList(), emptyList())
  )
  val pharmacyKpis: StateFlow<PharmacyKpis> = _pharmacyKpis.asStateFlow()

  private val _pharmacyRankings = MutableStateFlow(
    com.example.model.PharmacyRankings(emptyList(), emptyList(), emptyList())
  )
  val pharmacyRankings: StateFlow<PharmacyRankings> = _pharmacyRankings.asStateFlow()

  private val _pharmacyReconciliation = MutableStateFlow(
    com.example.util.MetricsCalculator.calculatePharmacyReconciliation(emptyList())
  )
  val pharmacyReconciliation: StateFlow<PharmacyReconciliation> = _pharmacyReconciliation.asStateFlow()

  private val _pharmacySearchQuery = MutableStateFlow("")
  val pharmacySearchQuery: StateFlow<String> = _pharmacySearchQuery.asStateFlow()

  private val _pharmacySortOption = MutableStateFlow(PharmacySortOption.REVENUE_DESC)
  val pharmacySortOption: StateFlow<PharmacySortOption> = _pharmacySortOption.asStateFlow()

  private val _dualStreamRevenue = MutableStateFlow(
    com.example.util.MetricsCalculator.calculateDualStreamRevenue(emptyList(), emptyList())
  )
  val dualStreamRevenue: StateFlow<DualStreamRevenue> = _dualStreamRevenue.asStateFlow()

  init {
    refreshUploadHistory()
    loadRawRowsAndRecalculate()

    // 30-Minute Inactivity Auto-Logout Timer (runs when Remember me is OFF)
    viewModelScope.launch {
      while (true) {
        delay(15_000L)
        if (_authState.value is AuthState.Authenticated) {
          if (authRepository.checkInactivityTimeout(lastActivityMillis)) {
            logout()
          }
        }
      }
    }
  }

  fun loadRawRowsAndRecalculate() {
    val all = uploadRepository.getAllRawRows()
    val allPharm = uploadRepository.getAllPharmacyItems()
    _rawRows.value = all
    _pharmacyItems.value = allPharm
    recalculateDashboardMetrics(all, allPharm, _filterState.value)
    recalculatePharmacyMetrics(allPharm, _filterState.value)
  }

  private fun recalculateDashboardMetrics(
    allRows: List<com.example.model.RawClinicRow>,
    allPharm: List<PharmacyItem> = _pharmacyItems.value,
    filter: FilterState
  ) {
    val filtered = com.example.util.MetricsCalculator.filterRows(allRows, filter)
    val prevPeriod = com.example.util.MetricsCalculator.getPreviousPeriodRows(allRows, filter)
    val mode = _valuationMode.value
    val pkgMap = _packageValuesMap.value
    val filteredPharm = com.example.util.MetricsCalculator.filterPharmacyItems(allPharm, filter)

    _filteredRows.value = filtered
    _reconciliationSummary.value = com.example.util.MetricsCalculator.calculateReconciliation(filtered, pkgMap)
    _unmappedPackageServices.value = com.example.util.MetricsCalculator.findUnmappedPackageServices(filtered, pkgMap)
    _dashboardKpis.value = com.example.util.MetricsCalculator.calculateKpis(filtered, prevPeriod, mode, pkgMap)
    _dailyTrend.value = com.example.util.MetricsCalculator.calculateDailyRevenueTrend(filtered, mode, pkgMap)
    _serviceRankings.value = com.example.util.MetricsCalculator.calculateTopAndBottomServices(filtered, mode, pkgMap)
    _categorySplit.value = com.example.util.MetricsCalculator.calculateCategorySplit(filtered, mode, pkgMap)
    _doctorRevenue.value = com.example.util.MetricsCalculator.calculateRevenueByDoctor(filtered, mode, pkgMap)
    _dominantVsUndersold.value = com.example.util.MetricsCalculator.calculateDominantVsUndersold(filtered, mode, pkgMap)

    // Compute Dual Revenue Stream (Clinical + Pharmacy)
    _dualStreamRevenue.value = com.example.util.MetricsCalculator.calculateDualStreamRevenue(
      serviceRows = filtered,
      pharmacyItems = filteredPharm,
      valuationMode = mode,
      packageValues = pkgMap
    )

    // Check cache and load/generate AI summary for this date & valuation mode
    checkAndLoadAiSummary(forceRegenerate = false)
  }

  fun checkAndLoadAiSummary(forceRegenerate: Boolean = false) {
    val filter = _filterState.value
    val mode = _valuationMode.value
    val cacheKey = aiSummaryRepository.buildCacheKey(
      periodDisplay = filter.formattedDateDisplay,
      valuationMode = mode,
      branch = filter.selectedBranch
    )

    if (!forceRegenerate) {
      val cached = aiSummaryRepository.getCachedSummary(cacheKey)
      if (cached != null) {
        _aiSummaryState.value = com.example.model.AiSummaryUiState.Success(cached)
        return
      }
    }

    _aiSummaryState.value = com.example.model.AiSummaryUiState.Loading
    viewModelScope.launch {
      try {
        val summary = geminiSummaryService.generateExecutiveSummary(
          cacheKey = cacheKey,
          filterState = filter,
          valuationMode = mode,
          kpis = _dashboardKpis.value,
          dailyTrend = _dailyTrend.value,
          serviceRankings = _serviceRankings.value,
          categorySplit = _categorySplit.value,
          doctorRevenue = _doctorRevenue.value,
          dualStreamRevenue = _dualStreamRevenue.value,
          reconciliationSummary = _reconciliationSummary.value
        )
        // Cache per date so it is not regenerated on every open
        aiSummaryRepository.saveSummary(summary)
        _aiSummaryState.value = com.example.model.AiSummaryUiState.Success(summary)
      } catch (e: Exception) {
        _aiSummaryState.value = com.example.model.AiSummaryUiState.Error(
          message = e.localizedMessage ?: "Failed to generate AI executive summary"
        )
      }
    }
  }

  fun regenerateAiSummary() {
    val filter = _filterState.value
    val mode = _valuationMode.value
    val cacheKey = aiSummaryRepository.buildCacheKey(
      periodDisplay = filter.formattedDateDisplay,
      valuationMode = mode,
      branch = filter.selectedBranch
    )
    aiSummaryRepository.clearCacheForDate(cacheKey)
    checkAndLoadAiSummary(forceRegenerate = true)
  }

  fun setPharmacySearchQuery(query: String) {
    _pharmacySearchQuery.value = query
    recalculatePharmacyMetrics(_pharmacyItems.value, _filterState.value)
  }

  fun setPharmacySortOption(sortOption: PharmacySortOption) {
    _pharmacySortOption.value = sortOption
    recalculatePharmacyMetrics(_pharmacyItems.value, _filterState.value)
  }

  private fun recalculatePharmacyMetrics(
    allItems: List<PharmacyItem>,
    filter: FilterState
  ) {
    val filtered = com.example.util.MetricsCalculator.filterPharmacyItems(
      items = allItems,
      filter = filter,
      searchQuery = _pharmacySearchQuery.value,
      sortOption = _pharmacySortOption.value
    )
    val prevPeriod = com.example.util.MetricsCalculator.getPreviousPeriodPharmacyItems(allItems, filter)

    _filteredPharmacyItems.value = filtered
    _pharmacyKpis.value = com.example.util.MetricsCalculator.calculatePharmacyKpis(filtered, prevPeriod)
    _pharmacyRankings.value = com.example.util.MetricsCalculator.calculatePharmacyRankings(filtered)
    _pharmacyReconciliation.value = com.example.util.MetricsCalculator.calculatePharmacyReconciliation(filtered)
  }

  fun setValuationMode(mode: com.example.model.ValuationMode) {
    if (_valuationMode.value != mode) {
      _valuationMode.value = mode
      recalculateDashboardMetrics(_rawRows.value, _pharmacyItems.value, _filterState.value)
    }
  }

  fun savePackageValue(serviceName: String, perSessionValue: Double, category: String = "Clinical", notes: String = "") {
    val currentUser = (_authState.value as? AuthState.Authenticated)?.user
    if (currentUser?.isSuperAdmin != true) {
      android.util.Log.w("CeoPulseViewModel", "CEO is view-only: Package settings write blocked by RBAC.")
      return
    }
    packageValuesRepository.savePackageValue(serviceName, perSessionValue, category, notes)
    _packageValuesMap.value = packageValuesRepository.getPackageValuesMap()
    _packageValueSettings.value = packageValuesRepository.getPackageValueSettingsList()
    recalculateDashboardMetrics(_rawRows.value, _pharmacyItems.value, _filterState.value)
  }

  fun deletePackageValue(serviceName: String) {
    val currentUser = (_authState.value as? AuthState.Authenticated)?.user
    if (currentUser?.isSuperAdmin != true) {
      android.util.Log.w("CeoPulseViewModel", "CEO is view-only: Package settings delete blocked by RBAC.")
      return
    }
    packageValuesRepository.deletePackageValue(serviceName)
    _packageValuesMap.value = packageValuesRepository.getPackageValuesMap()
    _packageValueSettings.value = packageValuesRepository.getPackageValueSettingsList()
    recalculateDashboardMetrics(_rawRows.value, _pharmacyItems.value, _filterState.value)
  }

  fun openPackageValuesDialog() {
    val currentUser = (_authState.value as? AuthState.Authenticated)?.user
    if (currentUser?.isSuperAdmin != true) {
      return
    }
    _showPackageValuesDialog.value = true
  }

  fun closePackageValuesDialog() {
    _showPackageValuesDialog.value = false
  }

  fun inspectMetric(title: String, formula: String, value: String, targetRows: List<com.example.model.RawClinicRow>) {
    _auditInspectionData.value = com.example.model.AuditInspectionData(
      title = title,
      formula = formula,
      computedValue = value,
      rows = targetRows
    )
  }

  fun dismissAudit() {
    _auditInspectionData.value = null
  }

  fun login(email: String, password: String, rememberMe: Boolean = false) {
    viewModelScope.launch {
      _authState.value = AuthState.Loading
      recordUserActivity()
      val result = authRepository.login(email, password, rememberMe)
      when (result) {
        is AuthResult.Success -> {
          _authState.value = AuthState.Authenticated(result.user)
          refreshUploadHistory()
          loadUsersAndRoles()
          simulateLoadingData()
        }
        is AuthResult.MustChangePassword -> {
          _authState.value = AuthState.MustChangePassword(result.user)
        }
        is AuthResult.AccessDenied -> {
          _authState.value = AuthState.AccessDenied(result.email, result.message)
        }
        is AuthResult.Failure -> {
          _authState.value = AuthState.Error(result.error)
        }
      }
    }
  }

  fun runInitialAccountSetup() {
    viewModelScope.launch {
      _isSetupRunning.value = true
      val res = authRepository.runInitialSetup()
      _setupResults.value = res
      _isSetupRunning.value = false
    }
  }

  fun dismissSetupResults() {
    _setupResults.value = null
  }

  suspend fun changePassword(email: String, currentPass: String, newPass: String): Result<Unit> {
    recordUserActivity()
    val res = authRepository.changePassword(email, currentPass, newPass)
    if (res.isSuccess) {
      val currentUser = (_authState.value as? AuthState.MustChangePassword)?.user
        ?: (_authState.value as? AuthState.Authenticated)?.user
      if (currentUser != null) {
        val updated = currentUser.copy(mustChangePassword = false)
        _authState.value = AuthState.Authenticated(updated)
      }
    }
    return res
  }

  suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
    recordUserActivity()
    return authRepository.sendPasswordResetEmail(email)
  }

  fun loadUsersAndRoles() {
    viewModelScope.launch {
      _usersAndRoles.value = authRepository.fetchUsersAndRoles()
    }
  }

  fun recordUserActivity() {
    lastActivityMillis = System.currentTimeMillis()
  }

  fun getRememberedEmail(): String = authRepository.getRememberedEmail()

  fun isRememberMeEnabled(): Boolean = authRepository.isRememberMeEnabled()

  fun logout() {
    viewModelScope.launch {
      authRepository.logout()
      _authState.value = AuthState.Unauthenticated
      _activeTab.value = DashboardTab.DASHBOARD
    }
  }

  fun clearAuthError() {
    if (_authState.value is AuthState.Error || _authState.value is AuthState.AccessDenied) {
      _authState.value = AuthState.Unauthenticated
    }
  }

  fun setActiveTab(tab: DashboardTab) {
    if (_activeTab.value != tab) {
      _activeTab.value = tab
      if (tab == DashboardTab.UPLOAD) {
        refreshUploadHistory()
      }
      simulateLoadingData()
    }
  }

  private fun simulateLoadingData() {
    viewModelScope.launch {
      _isSkeletonLoading.value = true
      delay(500)
      _isSkeletonLoading.value = false
    }
  }

  fun toggleSkeletonLoading() {
    _isSkeletonLoading.value = !_isSkeletonLoading.value
  }

  // Filter Bar Actions
  fun setDatePreset(preset: DatePreset) {
    _filterState.update { it.copy(datePreset = preset) }
    recalculateDashboardMetrics(_rawRows.value, _pharmacyItems.value, _filterState.value)
    recalculatePharmacyMetrics(_pharmacyItems.value, _filterState.value)
  }

  fun setCustomDateRange(start: String, end: String) {
    _filterState.update {
      it.copy(
        datePreset = DatePreset.CUSTOM,
        customStartDate = start,
        customEndDate = end
      )
    }
    recalculateDashboardMetrics(_rawRows.value, _pharmacyItems.value, _filterState.value)
    recalculatePharmacyMetrics(_pharmacyItems.value, _filterState.value)
  }

  fun setBranch(branch: String) {
    _filterState.update { it.copy(selectedBranch = branch) }
    recalculateDashboardMetrics(_rawRows.value, _pharmacyItems.value, _filterState.value)
    recalculatePharmacyMetrics(_pharmacyItems.value, _filterState.value)
  }

  fun setDoctor(doctor: String) {
    _filterState.update { it.copy(selectedDoctor = doctor) }
    recalculateDashboardMetrics(_rawRows.value, _pharmacyItems.value, _filterState.value)
    recalculatePharmacyMetrics(_pharmacyItems.value, _filterState.value)
  }

  fun setCategory(category: String) {
    _filterState.update { it.copy(selectedCategory = category) }
    recalculateDashboardMetrics(_rawRows.value, _pharmacyItems.value, _filterState.value)
    recalculatePharmacyMetrics(_pharmacyItems.value, _filterState.value)
  }

  fun resetFilters() {
    _filterState.value = FilterState()
    recalculateDashboardMetrics(_rawRows.value, _pharmacyItems.value, _filterState.value)
    recalculatePharmacyMetrics(_pharmacyItems.value, _filterState.value)
  }

  // Firebase Config Paste Handler
  fun applyPastedFirebaseConfig(rawConfig: String) {
    viewModelScope.launch {
      val (success, message) = configManager.parseAndSavePastedConfig(rawConfig)
      _configDialogMessage.value = message
      _firebaseConfigStatus.value = configManager.isConfigured()
    }
  }

  fun clearConfigMessage() {
    _configDialogMessage.value = null
  }

  // ------------------------------------------------------------------
  // UPLOAD SCREEN ACTIONS
  // ------------------------------------------------------------------
  fun setSelectedReportType(type: ReportType) {
    _selectedReportType.value = type
    // Re-validate if a file is already loaded
    _parsedSpreadsheet.value?.let { parsed ->
      _validationReport.value = ReportValidator.validate(type, parsed.headers, parsed.rows)
    }
  }

  fun setSelectedUploadBranch(branch: String) {
    _selectedUploadBranch.value = branch
  }

  fun setSelectedUploadDate(date: String) {
    _selectedUploadDate.value = date
  }

  fun clearCurrentParsedFile() {
    _parsedSpreadsheet.value = null
    _validationReport.value = null
    _duplicatePrompt.value = null
  }

  fun dismissLastUploadProof() {
    _lastUploadProof.value = null
  }

  fun parseUri(uri: Uri, fileName: String) {
    viewModelScope.launch {
      _isUploading.value = true
      try {
        val parsed = ExcelParser.parseUri(getApplication(), uri, fileName)
        _parsedSpreadsheet.value = parsed
        _validationReport.value = ReportValidator.validate(
          reportType = _selectedReportType.value,
          headers = parsed.headers,
          rows = parsed.rows
        )
      } catch (e: Exception) {
        _validationReport.value = ValidationReport(
          totalRows = 0,
          validRows = 0,
          detectedColumns = emptyList(),
          issues = listOf(
            com.example.model.ValidationIssue(
              rowNumber = 0,
              columnName = "File",
              issue = "Parsing failed: ${e.localizedMessage}",
              severity = com.example.model.IssueSeverity.ERROR
            )
          )
        )
      } finally {
        _isUploading.value = false
      }
    }
  }

  fun loadSampleServiceSalesExport() {
    viewModelScope.launch {
      _isUploading.value = true
      delay(300)
      val branch = _selectedUploadBranch.value
      val date = _selectedUploadDate.value
      val rows = SampleExportData.getServiceSalesSampleRows(date, branch)
      val parsed = ParsedSpreadsheet(
        fileName = "Service_Sales_Export_${date.replace("-", "")}.xlsx",
        headers = SampleExportData.HEADERS,
        rows = rows,
        totalRowCount = rows.size
      )
      _parsedSpreadsheet.value = parsed
      _validationReport.value = ReportValidator.validate(
        reportType = _selectedReportType.value,
        headers = parsed.headers,
        rows = parsed.rows
      )
      _isUploading.value = false
    }
  }

  fun initiateSaveUpload() {
    val currentUser = (_authState.value as? AuthState.Authenticated)?.user
    if (currentUser?.isSuperAdmin != true) {
      android.util.Log.w("CeoPulseViewModel", "CEO is view-only: Upload write blocked by RBAC.")
      return
    }
    val parsed = _parsedSpreadsheet.value ?: return
    viewModelScope.launch {
      _isUploading.value = true
      // Check for duplicate upload (same date, report type, and branch)
      val duplicateCheck = uploadRepository.checkDuplicate(
        reportType = _selectedReportType.value,
        branch = _selectedUploadBranch.value,
        reportDate = _selectedUploadDate.value
      )

      if (duplicateCheck.exists) {
        _duplicatePrompt.value = duplicateCheck
        _isUploading.value = false
      } else {
        executeSave(replaceExistingUploadId = null)
      }
    }
  }

  fun confirmReplaceUpload() {
    val currentUser = (_authState.value as? AuthState.Authenticated)?.user
    if (currentUser?.isSuperAdmin != true) {
      android.util.Log.w("CeoPulseViewModel", "CEO is view-only: Replace upload blocked by RBAC.")
      return
    }
    val existingUploadId = _duplicatePrompt.value?.existingUpload?.uploadId
    _duplicatePrompt.value = null
    viewModelScope.launch {
      _isUploading.value = true
      executeSave(replaceExistingUploadId = existingUploadId)
    }
  }

  fun dismissDuplicatePrompt() {
    _duplicatePrompt.value = null
    _isUploading.value = false
  }

  private suspend fun executeSave(replaceExistingUploadId: String?) {
    val currentUser = (_authState.value as? AuthState.Authenticated)?.user
    if (currentUser?.isSuperAdmin != true) {
      _isUploading.value = false
      return
    }
    val parsed = _parsedSpreadsheet.value ?: return
    val userEmail = currentUser.email

    val result = uploadRepository.saveUpload(
      fileName = parsed.fileName,
      reportType = _selectedReportType.value,
      branch = _selectedUploadBranch.value,
      reportDate = _selectedUploadDate.value,
      headers = parsed.headers,
      rows = parsed.rows,
      userEmail = userEmail,
      replaceExistingUploadId = replaceExistingUploadId
    )

    _lastUploadProof.value = result
    _isUploading.value = false

    if (result.success) {
      _parsedSpreadsheet.value = null
      _validationReport.value = null
      refreshUploadHistory()
      loadRawRowsAndRecalculate()
    }
  }

  fun deleteUpload(uploadId: String) {
    val currentUser = (_authState.value as? AuthState.Authenticated)?.user
    if (currentUser?.isSuperAdmin != true) {
      android.util.Log.w("CeoPulseViewModel", "CEO is view-only: Delete upload blocked by RBAC.")
      return
    }
    viewModelScope.launch {
      uploadRepository.deleteUpload(uploadId)
      refreshUploadHistory()
      loadRawRowsAndRecalculate()
    }
  }

  fun refreshUploadHistory() {
    _uploadHistory.value = uploadRepository.getUploadHistory()
  }

  // ------------------------------------------------------------------
  // ASK EXECUTIVE AGENT (Owner Only, Gemini Function Calling)
  // ------------------------------------------------------------------
  private val askToolsHandler by lazy {
    AskToolsHandler(
      getRawRows = { _rawRows.value },
      getPharmacyItems = { _pharmacyItems.value },
      getPackageValues = { _packageValuesMap.value },
      getUploadHistory = { _uploadHistory.value },
      getGlobalFilter = { _filterState.value },
      getValuationMode = { _valuationMode.value }
    )
  }

  private val geminiAskAgentService by lazy {
    GeminiAskAgentService(askToolsHandler)
  }

  private val _askChatMessages = MutableStateFlow<List<AskChatMessage>>(
    listOf(
      AskChatMessage(
        isUser = false,
        text = "Welcome, Executive Owner. I am your read-only AI data agent backed by the CEO Pulse metrics engine and Gemini function calling.\n\nI answer data questions exclusively by executing live verified tools. I can also perform math on tool outputs with the working shown."
      )
    )
  )
  val askChatMessages: StateFlow<List<AskChatMessage>> = _askChatMessages.asStateFlow()

  private val _isAskAgentThinking = MutableStateFlow(false)
  val isAskAgentThinking: StateFlow<Boolean> = _isAskAgentThinking.asStateFlow()

  val starterQuestions = listOf(
    "What is today's total revenue across all branches?",
    "Who are our top revenue-generating doctors?",
    "What are our top 5 services by revenue?",
    "What is the pharmacy gross margin percentage?",
    "Compare today's performance with yesterday",
    "What spreadsheet files have been uploaded recently?"
  )

  fun isOwner(user: UserProfile?): Boolean {
    if (user == null) return false
    return user.isSuperAdmin || user.isCeo
  }

  fun sendAskMessage(query: String) {
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank() || _isAskAgentThinking.value) return

    val userMsg = AskChatMessage(
      isUser = true,
      text = cleanQuery
    )
    _askChatMessages.update { it + userMsg }
    _isAskAgentThinking.value = true

    viewModelScope.launch {
      try {
        val response = geminiAskAgentService.ask(cleanQuery, _askChatMessages.value)
        _askChatMessages.update { it + response }
      } catch (e: Exception) {
        _askChatMessages.update {
          it + AskChatMessage(
            isUser = false,
            text = "An unexpected error occurred while executing the tool query: ${e.message}",
            isError = true
          )
        }
      } finally {
        _isAskAgentThinking.value = false
      }
    }
  }

  fun clearAskHistory() {
    _askChatMessages.value = listOf(
      AskChatMessage(
        isUser = false,
        text = "Conversation history cleared. I am ready for your next data query."
      )
    )
  }
}
