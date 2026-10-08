package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoleDoc
import com.example.model.UserProfile
import com.example.ui.theme.BackgroundCanvas
import com.example.ui.theme.BorderSubtle
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
import kotlinx.coroutines.launch

@Composable
fun UsersAndAccessScreen(
  currentUser: UserProfile,
  usersAndRoles: List<RoleDoc>,
  onRefreshUsers: () -> Unit,
  onSendPasswordReset: suspend (String) -> Result<Unit>,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val sendingState = remember { mutableStateMapOf<String, Boolean>() }
  val successState = remember { mutableStateMapOf<String, String>() }

  LaunchedEffect(Unit) {
    onRefreshUsers()
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundCanvas)
      .verticalScroll(rememberScrollState())
      .padding(24.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(TealLightContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AdminPanelSettings,
              contentDescription = null,
              tint = TealPrimary,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Users & Access Control",
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              color = TextHeadline
            )
            Text(
              text = "Role-Based Access Control (RBAC) & Account Permissions",
              style = MaterialTheme.typography.bodyMedium,
              color = TextMuted
            )
          }
        }
      }

      IconButton(
        onClick = onRefreshUsers,
        modifier = Modifier
          .defaultMinSize(minHeight = 48.dp, minWidth = 48.dp)
          .testTag("refresh_users_btn")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Refresh Users",
          tint = TealPrimary
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Zero-Knowledge Password Guarantee Banner
    Surface(
      color = SurfaceCard,
      shape = RoundedCornerShape(14.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, TealAccent.copy(alpha = 0.3f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.Top
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(TealLightContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Zero-Knowledge Password Architecture",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextHeadline
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Super admins can NEVER view, decrypt, or set user passwords directly. All credentials are fully managed by Firebase Authentication. To support any user with account access, trigger the self-service 'Send Password Reset Email' action below.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "AUTHORIZED ACCOUNTS (FIRESTORE 'roles' COLLECTION)",
      style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
      fontWeight = FontWeight.Bold,
      color = TextSubtle
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Users List
    val displayList = if (usersAndRoles.isNotEmpty()) {
      usersAndRoles
    } else {
      listOf(
        RoleDoc("uzairkhanp78@gmail.com", "super_admin", mustChangePassword = false),
        RoleDoc("shehzadruman@gmail.com", "ceo", mustChangePassword = false)
      )
    }

    displayList.forEach { userDoc ->
      val isSuperAdmin = userDoc.role.equals("super_admin", ignoreCase = true)
      val emailKey = userDoc.email.lowercase()
      val isSending = sendingState[emailKey] == true
      val successMsg = successState[emailKey]

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 6.dp),
        color = SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
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
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(if (isSuperAdmin) TealLightContainer else SurfaceSubtle),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (isSuperAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                  contentDescription = null,
                  tint = if (isSuperAdmin) TealPrimary else TextMuted,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column {
                Text(
                  text = userDoc.email,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = TextHeadline
                )
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Surface(
                    color = if (isSuperAdmin) TealLightContainer else SurfaceSubtle,
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = if (isSuperAdmin) "super_admin (Full Access)" else "ceo (View-Only)",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (isSuperAdmin) TealOnContainer else TextHeadline,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }

                  if (userDoc.mustChangePassword) {
                    Surface(
                      color = WarningAmberBg,
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = "Must Change Password",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WarningAmber,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }
                  } else {
                    Surface(
                      color = RoleAllowedBg,
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = RoleAllowedGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }
                  }
                }
              }
            }

            // Action: Send Password Reset Email
            OutlinedButton(
              onClick = {
                scope.launch {
                  sendingState[emailKey] = true
                  val res = onSendPasswordReset(userDoc.email)
                  sendingState[emailKey] = false
                  if (res.isSuccess) {
                    successState[emailKey] = "Reset email sent to ${userDoc.email}"
                  }
                }
              },
              enabled = !isSending,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag("send_reset_${emailKey.replace("@", "_").replace(".", "_")}")
            ) {
              if (isSending) {
                CircularProgressIndicator(
                  strokeWidth = 2.dp,
                  modifier = Modifier.size(16.dp),
                  color = TealPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sending...")
              } else {
                Icon(
                  imageVector = Icons.Default.Mail,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                  tint = TealPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Send Password Reset Email",
                  style = MaterialTheme.typography.labelMedium,
                  color = TealPrimary
                )
              }
            }
          }

          if (successMsg != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              color = RoleAllowedBg,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = RoleAllowedGreen,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = successMsg,
                  style = MaterialTheme.typography.bodySmall,
                  color = RoleAllowedGreen
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Permissions Matrix Card
    Text(
      text = "ROLE PERMISSION MATRIX",
      style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
      fontWeight = FontWeight.Bold,
      color = TextSubtle
    )

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
      color = SurfaceCard,
      shape = RoundedCornerShape(14.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        PermissionRow(
          feature = "Executive Dashboards & Charts",
          superAdmin = "Full Access",
          ceo = "View-Only"
        )
        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))
        PermissionRow(
          feature = "Specialties & Service Performance",
          superAdmin = "Full Access",
          ceo = "View-Only"
        )
        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))
        PermissionRow(
          feature = "Pharmacy Dispensary & Margins",
          superAdmin = "Full Access",
          ceo = "View-Only"
        )
        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))
        PermissionRow(
          feature = "Executive AI Brief & Ask Agent",
          superAdmin = "Full Access",
          ceo = "Full Access"
        )
        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))
        PermissionRow(
          feature = "Upload EHR / Billing Spreadsheets",
          superAdmin = "Enabled",
          ceo = "BLOCKED (UI & Firestore Rules)"
        )
        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))
        PermissionRow(
          feature = "Delete Upload Records",
          superAdmin = "Enabled",
          ceo = "BLOCKED (UI & Firestore Rules)"
        )
        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))
        PermissionRow(
          feature = "Package Valuation Settings",
          superAdmin = "Enabled",
          ceo = "BLOCKED (UI & Firestore Rules)"
        )
        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 10.dp))
        PermissionRow(
          feature = "Users & Access Administration",
          superAdmin = "Enabled",
          ceo = "BLOCKED (Hidden & Prohibited)"
        )
      }
    }

    Spacer(modifier = Modifier.height(28.dp))
  }
}

@Composable
private fun PermissionRow(
  feature: String,
  superAdmin: String,
  ceo: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = feature,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Medium,
      color = TextHeadline,
      modifier = Modifier.weight(1.2f)
    )
    Text(
      text = superAdmin,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = TealPrimary,
      modifier = Modifier.weight(0.9f)
    )
    Text(
      text = ceo,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = if (ceo.contains("BLOCKED")) WarningAmber else TextMuted,
      modifier = Modifier.weight(1.1f)
    )
  }
}
