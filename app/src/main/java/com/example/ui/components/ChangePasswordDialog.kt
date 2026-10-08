package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundCanvas
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedBg
import com.example.ui.theme.RoleAllowedGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextBody
import com.example.ui.theme.TextHeadline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSubtle
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordDialog(
  userEmail: String,
  onDismiss: () -> Unit,
  onChangePassword: suspend (currentPass: String, newPass: String) -> Result<Unit>
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
  var isSuccess by remember { mutableStateOf(false) }

  val hasMinLength = newPassword.length >= 8
  val hasNumber = newPassword.any { it.isDigit() }
  val hasSymbol = newPassword.any { !it.isLetterOrDigit() }
  val passwordsMatch = newPassword.isNotEmpty() && newPassword == confirmPassword
  val canSubmit = currentPassword.isNotBlank() && hasMinLength && hasNumber && hasSymbol && passwordsMatch && !isSubmitting

  AlertDialog(
    onDismissRequest = { if (!isSubmitting) onDismiss() },
    containerColor = SurfaceCard,
    shape = RoundedCornerShape(16.dp),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = null,
          tint = TealPrimary,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Change Account Password",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = TextHeadline
        )
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Account: $userEmail",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (isSuccess) {
          Surface(
            color = RoleAllowedGreen.copy(alpha = 0.12f),
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
                text = "Password updated successfully!",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = RoleAllowedGreen
              )
            }
          }
        } else {
          if (errorMessage != null) {
            Surface(
              color = ErrorRedBg,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
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

          // Current Password
          OutlinedTextField(
            value = currentPassword,
            onValueChange = {
              currentPassword = it
              errorMessage = null
            },
            label = { Text("Current Password") },
            singleLine = true,
            trailingIcon = {
              IconButton(onClick = { isCurrentVisible = !isCurrentVisible }) {
                Icon(
                  imageVector = if (isCurrentVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = null,
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
              .testTag("dialog_current_pass")
          )

          Spacer(modifier = Modifier.height(10.dp))

          // New Password
          OutlinedTextField(
            value = newPassword,
            onValueChange = {
              newPassword = it
              errorMessage = null
            },
            label = { Text("New Password (min 8 chars, 1 num, 1 sym)") },
            singleLine = true,
            trailingIcon = {
              IconButton(onClick = { isNewVisible = !isNewVisible }) {
                Icon(
                  imageVector = if (isNewVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = null,
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
              .testTag("dialog_new_pass")
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Confirm New Password
          OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
              confirmPassword = it
              errorMessage = null
            },
            label = { Text("Confirm New Password") },
            singleLine = true,
            trailingIcon = {
              IconButton(onClick = { isConfirmVisible = !isConfirmVisible }) {
                Icon(
                  imageVector = if (isConfirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = null,
                  tint = TextSubtle
                )
              }
            },
            visualTransformation = if (isConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = TealPrimary,
              unfocusedBorderColor = BorderSubtle
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("dialog_confirm_pass")
          )

          Spacer(modifier = Modifier.height(12.dp))

          Surface(
            color = BackgroundCanvas,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "8+ characters: ${if (hasMinLength) "✓" else "✗"} · Number: ${if (hasNumber) "✓" else "✗"} · Symbol: ${if (hasSymbol) "✓" else "✗"} · Match: ${if (passwordsMatch) "✓" else "✗"}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = if (canSubmit) RoleAllowedGreen else TextMuted
              )
            }
          }
        }
      }
    },
    confirmButton = {
      if (isSuccess) {
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
        ) {
          Text("Done")
        }
      } else {
        Button(
          onClick = {
            scope.launch {
              isSubmitting = true
              val res = onChangePassword(currentPassword, newPassword)
              if (res.isSuccess) {
                isSuccess = true
                isSubmitting = false
              } else {
                errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to change password."
                isSubmitting = false
              }
            }
          },
          enabled = canSubmit,
          colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
          modifier = Modifier.testTag("dialog_submit_change_pass")
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(
              color = Color.White,
              strokeWidth = 2.dp,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Saving...")
          } else {
            Text("Update Password")
          }
        }
      }
    },
    dismissButton = {
      if (!isSuccess) {
        OutlinedButton(
          onClick = onDismiss,
          enabled = !isSubmitting
        ) {
          Text("Cancel", color = TextBody)
        }
      }
    }
  )
}
