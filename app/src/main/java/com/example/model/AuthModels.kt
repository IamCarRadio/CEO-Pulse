package com.example.model

data class UserProfile(
  val email: String,
  val uid: String,
  val role: String, // "super_admin" or "ceo"
  val displayName: String = "",
  val mustChangePassword: Boolean = false
) {
  val isSuperAdmin: Boolean
    get() = role.equals("super_admin", ignoreCase = true)

  val isCeo: Boolean
    get() = role.equals("ceo", ignoreCase = true)

  val isViewOnly: Boolean
    get() = isCeo

  val roleDisplayName: String
    get() = when {
      isSuperAdmin -> "Super Admin"
      isCeo -> "Chief Executive Officer (View-Only)"
      else -> role
    }
}

data class RoleDoc(
  val email: String,
  val role: String, // "super_admin" or "ceo"
  val mustChangePassword: Boolean = true,
  val createdAtMillis: Long = System.currentTimeMillis()
)

enum class SetupStatus {
  CREATED,
  ALREADY_EXISTS,
  FAILED
}

data class SetupAccountStatus(
  val email: String,
  val role: String,
  val status: SetupStatus,
  val message: String
)

data class FirebaseProjectConfig(
  val apiKey: String = "",
  val projectId: String = "",
  val applicationId: String = "",
  val storageBucket: String = "",
  val authDomain: String = ""
) {
  val isConfigured: Boolean
    get() = apiKey.isNotBlank() && projectId.isNotBlank() && applicationId.isNotBlank()
}

sealed class AuthState {
  object Unauthenticated : AuthState()
  object Loading : AuthState()
  data class Authenticated(val user: UserProfile) : AuthState()
  data class MustChangePassword(val user: UserProfile) : AuthState()
  data class Error(val message: String) : AuthState()
  data class AccessDenied(val email: String, val details: String) : AuthState()
}

sealed class AuthResult {
  data class Success(val user: UserProfile) : AuthResult()
  data class MustChangePassword(val user: UserProfile) : AuthResult()
  data class AccessDenied(val email: String, val message: String) : AuthResult()
  data class Failure(val error: String) : AuthResult()
}
