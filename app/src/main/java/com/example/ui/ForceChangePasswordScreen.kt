package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.ui.theme.BackgroundCanvas
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedBg
import com.example.ui.theme.RoleAllowedBg
import com.example.ui.theme.RoleAllowedGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealLightContainer
import com.example.ui.theme.TealOnContainer
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextHeadline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import kotlinx.coroutines.launch

@Composable
fun ForceChangePasswordScreen(
  user: UserProfile,
  onChangePassword: suspend (currentPass: String, newPass: String) -> Result<Unit>,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  var currentPassword by remember { mutableStateOf("") }
  var newPassword by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }

  var isCurrentVisible by remember { mutableStateOf(false) }
  var isNewVisible by remember { mutableStateOf(false) }
  var isConfirmVisible by remember { mutableStateOf(false) }

  var isSubmitting by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Validation criteria
  val hasMinLength = newPassword.length >= 8
  val hasNumber = newPassword.any { it.isDigit() }
  val hasSymbol = newPassword.any { !it.isLetterOrDigit() }
  val passwordsMatch = newPassword.isNotEmpty() && newPassword == confirmPassword
  val canSubmit = currentPassword.isNotBlank() && hasMinLength && hasNumber && hasSymbol && passwordsMatch && !isSubmitting

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BackgroundCanvas)
      .padding(horizontal = 24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 480.dp)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Spacer(modifier = Modifier.height(24.dp))

      // Shield Emblem
      Box(
        modifier = Modifier
          .size(68.dp)
          .clip(CircleShape)
          .background(TealLightContainer)
          .border(1.dp, TealAccent.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = "Security Shield",
          tint = TealPrimary,
          modifier = Modifier.size(36.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Set Your New Password",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = TextHeadline
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Initial setup requires you to change your temporary password before accessing CEO Pulse.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextMuted,
        modifier = Modifier.padding(horizontal = 12.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )

      Spacer(modifier = Modifier.height(10.dp))

      Surface(
        color = TealLightContainer,
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${user.email} · ${user.roleDisplayName}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = TealOnContainer
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 2.dp
      ) {
        Column(modifier = Modifier.padding(24.dp)) {

          if (errorMessage != null) {
            Surface(
              color = ErrorRedBg,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = null,
                  tint = ErrorRed,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = errorMessage ?: "",
                  style = MaterialTheme.typography.bodySmall,
                  color = ErrorRed
                )
              }
            }
          }

          // 1. Current (Temporary) Password
          Text(
            text = "Current Password",
            style = MaterialTheme.typography.labelMedium,
            color = TextHeadline,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = currentPassword,
            onValueChange = {
              currentPassword = it
              errorMessage = null
            },
            placeholder = { Text("Enter current temporary password") },
            singleLine = true,
            leadingIcon = {
              Icon(Icons.Default.Lock, contentDescription = null, tint = TextSubtle)
            },
            trailingIcon = {
              IconButton(onClick = { isCurrentVisible = !isCurrentVisible }) {
                Icon(
                  imageVector = if (isCurrentVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (isCurrentVisible) "Hide" else "Show",
                  tint = TextSubtle
                )
              }
            },
            visualTransformation = if (isCurrentVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = TealPrimary,
              unfocusedBorderColor = BorderSubtle
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("current_password_input")
          )

          Spacer(modifier = Modifier.height(16.dp))

          // 2. New Password
          Text(
            text = "New Password",
            style = MaterialTheme.typography.labelMedium,
            color = TextHeadline,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = newPassword,
            onValueChange = {
              newPassword = it
              errorMessage = null
            },
            placeholder = { Text("Minimum 8 chars, 1 number, 1 symbol") },
            singleLine = true,
            leadingIcon = {
              Icon(Icons.Default.Lock, contentDescription = null, tint = TextSubtle)
            },
            trailingIcon = {
              IconButton(onClick = { isNewVisible = !isNewVisible }) {
                Icon(
                  imageVector = if (isNewVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (isNewVisible) "Hide" else "Show",
                  tint = TextSubtle
                )
              }
            },
            visualTransformation = if (isNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = TealPrimary,
              unfocusedBorderColor = BorderSubtle
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("new_password_input")
          )

          Spacer(modifier = Modifier.height(16.dp))

          // 3. Confirm New Password
          Text(
            text = "Confirm New Password",
            style = MaterialTheme.typography.labelMedium,
            color = TextHeadline,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
              confirmPassword = it
              errorMessage = null
            },
            placeholder = { Text("Re-enter your new password") },
            singleLine = true,
            leadingIcon = {
              Icon(Icons.Default.Lock, contentDescription = null, tint = TextSubtle)
            },
            trailingIcon = {
              IconButton(onClick = { isConfirmVisible = !isConfirmVisible }) {
                Icon(
                  imageVector = if (isConfirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (isConfirmVisible) "Hide" else "Show",
                  tint = TextSubtle
                )
              }
            },
            visualTransformation = if (isConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
              onDone = {
                if (canSubmit) {
                  scope.launch {
                    isSubmitting = true
                    val result = onChangePassword(currentPassword, newPassword)
                    if (result.isFailure) {
                      errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Password update failed."
                      isSubmitting = false
                    }
                  }
                }
              }
            ),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = TealPrimary,
              unfocusedBorderColor = BorderSubtle
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("confirm_password_input")
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Requirements checklist
          Surface(
            color = BackgroundCanvas,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "PASSWORD REQUIREMENTS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextSubtle,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(6.dp))
              RequirementRow(label = "At least 8 characters", met = hasMinLength)
              RequirementRow(label = "At least one number (0-9)", met = hasNumber)
              RequirementRow(label = "At least one symbol (!@#$%^&*)", met = hasSymbol)
              RequirementRow(label = "Passwords match", met = passwordsMatch)
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          // Submit Button
          Button(
            onClick = {
              scope.launch {
                isSubmitting = true
                val result = onChangePassword(currentPassword, newPassword)
                if (result.isFailure) {
                  errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Password update failed."
                  isSubmitting = false
                }
              }
            },
            enabled = canSubmit,
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("submit_password_change_btn"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = TealPrimary,
              contentColor = Color.White
            )
          ) {
            if (isSubmitting) {
              CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Updating Password...")
            } else {
              Text("Save New Password & Continue", fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Sign Out button
          OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .testTag("force_change_signout_btn"),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cancel & Sign Out", color = TextBody)
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}

@Composable
private fun RequirementRow(label: String, met: Boolean) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(vertical = 3.dp)
  ) {
    Icon(
      imageVector = if (met) Icons.Default.Check else Icons.Default.Close,
      contentDescription = null,
      tint = if (met) RoleAllowedGreen else TextSubtle,
      modifier = Modifier.size(14.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
      color = if (met) TextHeadline else TextMuted
    )
  }
}
