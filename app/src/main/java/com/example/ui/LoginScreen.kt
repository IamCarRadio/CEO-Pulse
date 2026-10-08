package com.example.ui

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuthState
import com.example.model.SetupAccountStatus
import com.example.model.SetupStatus
import com.example.ui.theme.BackgroundCanvas
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
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
  authState: AuthState,
  firebaseConfigured: Boolean,
  configMessage: String?,
  rememberedEmail: String,
  isRememberMeInitiallyChecked: Boolean,
  setupResults: List<SetupAccountStatus>?,
  isSetupRunning: Boolean,
  onLogin: (String, String, Boolean) -> Unit,
  onRunInitialSetup: () -> Unit,
  onDismissSetupResults: () -> Unit,
  onSendPasswordReset: suspend (String) -> Result<Unit>,
  onApplyConfig: (String) -> Unit,
  onClearConfigMessage: () -> Unit,
  onClearError: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()

  // STEP 3 REQUIREMENT: Remember only last email, and only if Remember me is checked.
  // Never pre-fill passwords. No click-to-fill accounts.
  var email by remember { mutableStateOf(rememberedEmail) }
  var password by remember { mutableStateOf("") }
  var rememberMe by remember { mutableStateOf(isRememberMeInitiallyChecked) }
  var isPasswordVisible by remember { mutableStateOf(false) }

  // Dialog states
  var showForgotPasswordDialog by remember { mutableStateOf(false) }
  var forgotPasswordEmail by remember { mutableStateOf("") }
  var isSendingReset by remember { mutableStateOf(false) }
  var resetConfirmationMessage by remember { mutableStateOf<String?>(null) }

  var showInitialSetupConfirmDialog by remember { mutableStateOf(false) }
  var showConfigDialog by remember { mutableStateOf(false) }
  var pastedConfigText by remember { mutableStateOf("") }

  val isLoading = authState is AuthState.Loading

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundCanvas)
      .padding(horizontal = 24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 440.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      // Executive Brand Emblem
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(TealLightContainer)
          .border(1.dp, TealAccent.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.HealthAndSafety,
          contentDescription = "CEO Pulse Emblem",
          tint = TealPrimary,
          modifier = Modifier.size(40.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "CEO PULSE",
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = TextHeadline,
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Executive Clinic Group Intelligence",
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Role Gate Notice
      Surface(
        color = TealLightContainer,
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = TealOnContainer,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "RBAC Security Gate · Firestore 'roles'",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = TealOnContainer
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Main Sign-In Card
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
      ) {
        Column(
          modifier = Modifier.padding(28.dp)
        ) {
          Text(
            text = "Executive Sign In",
            style = MaterialTheme.typography.titleLarge,
            color = TextHeadline
          )

          Text(
            text = "Enter your clinic credentials to access the intelligence platform.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
          )

          // 1. Email Field
          OutlinedTextField(
            value = email,
            onValueChange = {
              email = it
              onClearError()
            },
            label = { Text("Account Email") },
            placeholder = { Text("e.g. user@clinicgroup.com") },
            singleLine = true,
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Mail,
                contentDescription = "Email Icon",
                tint = TextSubtle
              )
            },
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Email,
              imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = TealPrimary,
              unfocusedBorderColor = BorderSubtle,
              focusedLabelColor = TealPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("email_input")
          )

          Spacer(modifier = Modifier.height(16.dp))

          // 2. Password Field with show/hide toggle
          OutlinedTextField(
            value = password,
            onValueChange = {
              password = it
              onClearError()
            },
            label = { Text("Password") },
            placeholder = { Text("••••••••") },
            singleLine = true,
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Password Icon",
                tint = TextSubtle
              )
            },
            trailingIcon = {
              IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                Icon(
                  imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                  tint = TextSubtle
                )
              }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password,
              imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
              onDone = {
                if (email.isNotBlank() && password.isNotBlank() && !isLoading) {
                  onLogin(email, password, rememberMe)
                }
              }
            ),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = TealPrimary,
              unfocusedBorderColor = BorderSubtle,
              focusedLabelColor = TealPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("password_input")
          )

          Spacer(modifier = Modifier.height(12.dp))

          // STEP 3: "Remember me" checkbox & "Forgot password" link
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.testTag("remember_me_row")
            ) {
              Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                colors = CheckboxDefaults.colors(
                  checkedColor = TealPrimary,
                  checkmarkColor = Color.White
                ),
                modifier = Modifier.testTag("remember_me_checkbox")
              )
              Text(
                text = "Remember me",
                style = MaterialTheme.typography.bodyMedium,
                color = TextHeadline
              )
            }

            TextButton(
              onClick = {
                forgotPasswordEmail = email.trim()
                resetConfirmationMessage = null
                showForgotPasswordDialog = true
              },
              modifier = Modifier.testTag("forgot_password_btn")
            ) {
              Text(
                text = "Forgot password?",
                style = MaterialTheme.typography.labelMedium,
                color = TealPrimary,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // ACCESS DENIED ERROR BANNER (ROLE GATE VIOLATION)
          if (authState is AuthState.AccessDenied) {
            Surface(
              color = ErrorRedBg,
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("access_denied_banner")
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Access Not Granted",
                    style = MaterialTheme.typography.labelLarge,
                    color = ErrorRed
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "No role document found for '${authState.email}' in the Firestore 'roles' collection. Access is restricted to designated administrators.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = TextBody
                )
              }
            }
          }

          // GENERAL / FRIENDLY ERROR BANNER
          if (authState is AuthState.Error) {
            Surface(
              color = ErrorRedBg,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("error_banner")
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = ErrorRed,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = authState.message,
                  style = MaterialTheme.typography.bodyMedium,
                  color = ErrorRed
                )
              }
            }
          }

          // SIGN IN BUTTON
          Button(
            onClick = { onLogin(email, password, rememberMe) },
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .testTag("sign_in_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = TealPrimary,
              contentColor = Color.White
            )
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Authenticating...",
                style = MaterialTheme.typography.labelLarge
              )
            } else {
              Text(
                text = "Sign In",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Footer Utilities: Hidden Initial Setup & Firebase Config
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // STEP 1: Hidden Initial Setup Routine Trigger
        OutlinedButton(
          onClick = { showInitialSetupConfirmDialog = true },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TealPrimary),
          modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .testTag("initial_setup_trigger")
        ) {
          Icon(
            imageVector = Icons.Default.BuildCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Initial Setup",
            style = MaterialTheme.typography.labelMedium
          )
        }

        OutlinedButton(
          onClick = { showConfigDialog = true },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
          modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .testTag("open_firebase_config_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (firebaseConfigured) "Firebase: Active" else "Firebase Config",
            style = MaterialTheme.typography.labelMedium
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))
    }
  }

  // -------------------------------------------------------------
  // DIALOG 1: FORGOT PASSWORD
  // -------------------------------------------------------------
  if (showForgotPasswordDialog) {
    AlertDialog(
      onDismissRequest = {
        if (!isSendingReset) showForgotPasswordDialog = false
      },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Mail,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Reset Password", style = MaterialTheme.typography.titleLarge)
        }
      },
      text = {
        Column {
          if (resetConfirmationMessage != null) {
            Surface(
              color = RoleAllowedBg,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = RoleAllowedGreen,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = resetConfirmationMessage ?: "",
                  style = MaterialTheme.typography.bodyMedium,
                  color = RoleAllowedGreen
                )
              }
            }
          } else {
            Text(
              text = "Enter your email address. If an account exists, a secure password reset link will be sent to your inbox.",
              style = MaterialTheme.typography.bodyMedium,
              color = TextBody
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
              value = forgotPasswordEmail,
              onValueChange = { forgotPasswordEmail = it },
              label = { Text("Account Email") },
              singleLine = true,
              shape = RoundedCornerShape(10.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPrimary,
                unfocusedBorderColor = BorderSubtle
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_email_input")
            )
          }
        }
      },
      confirmButton = {
        if (resetConfirmationMessage != null) {
          Button(
            onClick = { showForgotPasswordDialog = false },
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
          ) {
            Text("Done")
          }
        } else {
          Button(
            onClick = {
              if (forgotPasswordEmail.isNotBlank()) {
                scope.launch {
                  isSendingReset = true
                  onSendPasswordReset(forgotPasswordEmail)
                  isSendingReset = false
                  // STEP 3: Friendly confirmation, never reveal if email exists
                  resetConfirmationMessage = "If an account with this email exists, password reset instructions have been sent to your inbox."
                }
              }
            },
            enabled = forgotPasswordEmail.isNotBlank() && !isSendingReset,
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            modifier = Modifier.testTag("submit_reset_btn")
          ) {
            if (isSendingReset) {
              CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sending...")
            } else {
              Text("Send Reset Link")
            }
          }
        }
      },
      dismissButton = {
        if (resetConfirmationMessage == null) {
          OutlinedButton(
            onClick = { showForgotPasswordDialog = false },
            enabled = !isSendingReset
          ) {
            Text("Cancel", color = TextBody)
          }
        }
      }
    )
  }

  // -------------------------------------------------------------
  // DIALOG 2: STEP 1 INITIAL SETUP CONFIRMATION
  // -------------------------------------------------------------
  if (showInitialSetupConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showInitialSetupConfirmDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.BuildCircle,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text("Run Initial Account Setup", style = MaterialTheme.typography.titleLarge)
        }
      },
      text = {
        Column {
          Text(
            text = "This one-time setup routine will provision the initial administrator accounts in Firebase Auth and Firestore 'roles':",
            style = MaterialTheme.typography.bodyMedium,
            color = TextBody
          )
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            color = BackgroundCanvas,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "1. uzairkhanp78@gmail.com -> role: super_admin (mustChangePassword: true)",
                style = MaterialTheme.typography.bodySmall,
                color = TextHeadline,
                fontWeight = FontWeight.Medium
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "2. shehzadruman@gmail.com -> role: ceo (mustChangePassword: true)",
                style = MaterialTheme.typography.bodySmall,
                color = TextHeadline,
                fontWeight = FontWeight.Medium
              )
            }
          }
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "• Safe to run multiple times (skips already existing accounts).\n• Passwords are never displayed in logs, UI, or console.\n• Both accounts will be forced to change their password on first login.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showInitialSetupConfirmDialog = false
            onRunInitialSetup()
          },
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
          modifier = Modifier.testTag("confirm_run_setup_btn")
        ) {
          Text("Run Setup")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showInitialSetupConfirmDialog = false }) {
          Text("Cancel", color = TextBody)
        }
      }
    )
  }

  // -------------------------------------------------------------
  // DIALOG 3: STEP 1 INITIAL SETUP RESULTS SCREEN
  // -------------------------------------------------------------
  if (setupResults != null || isSetupRunning) {
    AlertDialog(
      onDismissRequest = {
        if (!isSetupRunning) onDismissSetupResults()
      },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text("Initial Setup Results", style = MaterialTheme.typography.titleLarge)
        }
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          if (isSetupRunning) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(vertical = 16.dp)
            ) {
              CircularProgressIndicator(
                color = TealPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text("Provisioning accounts and Firestore roles...", style = MaterialTheme.typography.bodyMedium)
            }
          } else {
            setupResults?.forEach { item ->
              val isSuccess = item.status == SetupStatus.CREATED || item.status == SetupStatus.ALREADY_EXISTS
              val badgeColor = when (item.status) {
                SetupStatus.CREATED -> RoleAllowedGreen
                SetupStatus.ALREADY_EXISTS -> TealPrimary
                SetupStatus.FAILED -> ErrorRed
              }
              val badgeBg = when (item.status) {
                SetupStatus.CREATED -> RoleAllowedBg
                SetupStatus.ALREADY_EXISTS -> TealLightContainer
                SetupStatus.FAILED -> ErrorRedBg
              }
              val badgeText = when (item.status) {
                SetupStatus.CREATED -> "CREATED"
                SetupStatus.ALREADY_EXISTS -> "ALREADY EXISTED"
                SetupStatus.FAILED -> "FAILED"
              }

              Surface(
                color = SurfaceSubtle,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = item.email,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = TextHeadline
                    )
                    Surface(
                      color = badgeBg,
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Role: ${item.role} · mustChangePassword: true",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                  )
                  Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSubtle
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              color = TealLightContainer,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "✓ Zero-Exposure Security: No passwords are shown or logged. Users must set their own passwords upon logging in.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TealOnContainer,
                modifier = Modifier.padding(10.dp)
              )
            }
          }
        }
      },
      confirmButton = {
        if (!isSetupRunning) {
          Button(
            onClick = onDismissSetupResults,
            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
            modifier = Modifier.testTag("close_setup_results_btn")
          ) {
            Text("Done")
          }
        }
      }
    )
  }

  // -------------------------------------------------------------
  // DIALOG 4: PASTE FIREBASE CONFIG
  // -------------------------------------------------------------
  if (showConfigDialog) {
    AlertDialog(
      onDismissRequest = { showConfigDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(text = "Firebase Project Config")
        }
      },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text(
            text = "Paste your Firebase configuration object or JSON below (e.g. apiKey, projectId, appId):",
            style = MaterialTheme.typography.bodyMedium,
            color = TextBody
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = pastedConfigText,
            onValueChange = { pastedConfigText = it },
            placeholder = {
              Text(
                "{\n  \"apiKey\": \"AIzaSy...\",\n  \"projectId\": \"my-clinic-project\",\n  \"appId\": \"1:12345:web:67890\"\n}",
                style = MaterialTheme.typography.bodySmall
              )
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp)
              .testTag("pasted_config_input"),
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodySmall
          )
          if (configMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
              color = TealLightContainer,
              shape = RoundedCornerShape(6.dp)
            ) {
              Text(
                text = configMessage,
                color = TealOnContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (pastedConfigText.isNotBlank()) {
              onApplyConfig(pastedConfigText)
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
          modifier = Modifier.testTag("save_config_btn")
        ) {
          Text("Apply & Connect")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            onClearConfigMessage()
            showConfigDialog = false
          }
        ) {
          Text("Close")
        }
      }
    )
  }
}
