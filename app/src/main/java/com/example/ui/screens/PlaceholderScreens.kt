package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.DashboardTab
import com.example.model.FilterState
import com.example.ui.components.SkeletonBox
import com.example.ui.components.skeleton
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaceholderScreen(
  tab: DashboardTab,
  filterState: FilterState,
  isSkeletonLoading: Boolean,
  onToggleSkeleton: () -> Unit,
  modifier: Modifier = Modifier
) {
  val icon = when (tab) {
    DashboardTab.DASHBOARD -> Icons.Default.Dashboard
    DashboardTab.SERVICES -> Icons.Default.MedicalServices
    DashboardTab.PHARMACY -> Icons.Default.LocalPharmacy
    DashboardTab.UPLOAD -> Icons.Default.CloudUpload
    DashboardTab.AI -> Icons.Default.AutoAwesome
    DashboardTab.USERS -> Icons.Default.AdminPanelSettings
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 24.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    // 1. Executive Section Header with Skeleton Toggle Button
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
            imageVector = icon,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(26.dp)
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
          Text(
            text = tab.title,
            style = MaterialTheme.typography.headlineLarge,
            color = TextHeadline
          )
          Text(
            text = tab.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
          )
        }
      }

      OutlinedButton(
        onClick = onToggleSkeleton,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("toggle_skeleton_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = null,
          tint = TealPrimary,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isSkeletonLoading) "Show Loaded" else "Preview Skeleton",
          style = MaterialTheme.typography.labelSmall,
          color = TealPrimary
        )
      }
    }

    // 2. Active Global Scope Card (Reflecting the global filter bar values)
    Surface(
      color = SurfaceCard,
      shape = RoundedCornerShape(16.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Active Executive Scope",
            style = MaterialTheme.typography.labelLarge,
            color = TextHeadline
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            color = TealLightContainer,
            shape = RoundedCornerShape(6.dp)
          ) {
            Text(
              text = filterState.formattedDateDisplay,
              style = MaterialTheme.typography.labelSmall,
              color = TealOnContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ScopeTag(label = "Branch", value = filterState.selectedBranch)
          ScopeTag(label = "Doctor", value = filterState.selectedDoctor.substringBefore(","))
          ScopeTag(label = "Category", value = filterState.selectedCategory)
        }
      }
    }

    // 3. Subtle Cards & Executive KPI Placeholders (Large readable numbers, no real metrics yet)
    if (isSkeletonLoading) {
      // Smooth Skeleton Loaders Grid
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        SkeletonBox(modifier = Modifier.weight(1f), height = 110.dp, shape = RoundedCornerShape(16.dp))
        SkeletonBox(modifier = Modifier.weight(1f), height = 110.dp, shape = RoundedCornerShape(16.dp))
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        SkeletonBox(modifier = Modifier.weight(1f), height = 110.dp, shape = RoundedCornerShape(16.dp))
        SkeletonBox(modifier = Modifier.weight(1f), height = 110.dp, shape = RoundedCornerShape(16.dp))
      }
      SkeletonBox(modifier = Modifier.fillMaxWidth(), height = 180.dp, shape = RoundedCornerShape(16.dp))
    } else {
      // Clean Executive Placeholder Structure
      val stats = when (tab) {
        DashboardTab.DASHBOARD -> listOf(
          StatSlot("Active Clinic Branches", "05", "Multi-Branch Network"),
          StatSlot("Licensed Practitioners", "42", "Credentialed Staff"),
          StatSlot("Operating Theaters", "14", "Real-Time Utilization"),
          StatSlot("Group Health Index", "98.4%", "Audit Benchmark")
        )
        DashboardTab.SERVICES -> listOf(
          StatSlot("Service Categories", "07", "Clinical Specialties"),
          StatSlot("Procedures Defined", "128", "Group Master Catalog"),
          StatSlot("Avg Wait Time", "14m", "Target: <20m"),
          StatSlot("Standard Care Protocols", "100%", "Accredited Status")
        )
        DashboardTab.PHARMACY -> listOf(
          StatSlot("Active Dispensaries", "05", "Branch Pharmacies"),
          StatSlot("Formulary SKUs", "1,840", "Master Inventory"),
          StatSlot("Rx Fill Rate", "99.1%", "Prescription Fulfillment"),
          StatSlot("Cold Chain Integrity", "100%", "Sensor Monitored")
        )
        DashboardTab.UPLOAD -> listOf(
          StatSlot("Data Ingestion Connectors", "03", "EHR / Lab / Billing"),
          StatSlot("Last Sync Feed", "0m ago", "Continuous Streaming"),
          StatSlot("Ingestion Errors", "00", "Clean Records Pipeline"),
          StatSlot("Security Encryption", "256-bit", "HIPAA Compliant")
        )
        DashboardTab.AI -> listOf(
          StatSlot("Predictive Forecasting", "Active", "Operational Engine"),
          StatSlot("Staffing Optimization", "Optimal", "Shift Allocation"),
          StatSlot("Revenue Leak Alerts", "00", "Real-Time Watchdog"),
          StatSlot("Model Confidence", "97.8%", "Multi-Site Baseline")
        )
        DashboardTab.USERS -> listOf(
          StatSlot("Super Administrators", "01", "Full Access Control"),
          StatSlot("Executive Observers", "01", "CEO Read-Only Role"),
          StatSlot("Password Policy", "Enforced", "8+ chars, 1 num, 1 sym"),
          StatSlot("RBAC Integrity", "100%", "Firestore & Storage Rules")
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        ExecutiveSlotCard(stats[0], modifier = Modifier.weight(1f))
        ExecutiveSlotCard(stats[1], modifier = Modifier.weight(1f))
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        ExecutiveSlotCard(stats[2], modifier = Modifier.weight(1f))
        ExecutiveSlotCard(stats[3], modifier = Modifier.weight(1f))
      }

      // Large Structural Placeholder Card
      Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(SurfaceSubtle),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = TealPrimary,
              modifier = Modifier.size(28.dp)
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "${tab.title} Architecture Ready",
            style = MaterialTheme.typography.titleLarge,
            color = TextHeadline
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Structure initialized. Metric cards, analytics feeds, and Firestore integrations will populate this pane according to active global filters.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        }
      }
    }
  }
}

data class StatSlot(val label: String, val value: String, val caption: String)

@Composable
fun ExecutiveSlotCard(
  stat: StatSlot,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    color = SurfaceCard,
    shape = RoundedCornerShape(16.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    shadowElevation = 1.dp
  ) {
    Column(
      modifier = Modifier.padding(20.dp)
    ) {
      Text(
        text = stat.label,
        style = MaterialTheme.typography.labelMedium,
        color = TextMuted
      )
      Spacer(modifier = Modifier.height(8.dp))
      // Large readable numbers per requirement
      Text(
        text = stat.value,
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Bold,
        color = TealPrimary,
        letterSpacing = (-0.5).sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stat.caption,
        style = MaterialTheme.typography.labelSmall,
        color = TextSubtle
      )
    }
  }
}

@Composable
fun ScopeTag(label: String, value: String) {
  Surface(
    color = SurfaceSubtle,
    shape = RoundedCornerShape(8.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "$label: ",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Normal,
        color = TextMuted
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = TextHeadline
      )
    }
  }
}
