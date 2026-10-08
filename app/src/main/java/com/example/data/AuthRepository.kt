package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.AuthResult
import com.example.model.RoleDoc
import com.example.model.SetupAccountStatus
import com.example.model.SetupStatus
import com.example.model.UserProfile
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Extension helper to safely await Task without additional dependencies
suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
  addOnSuccessListener { result ->
    if (cont.isActive) cont.resume(result)
  }
  addOnFailureListener { exception ->
    if (cont.isActive) cont.resumeWithException(exception)
  }
  addOnCanceledListener {
    if (cont.isActive) cont.cancel()
  }
}

interface IAuthRepository {
  suspend fun login(email: String, password: String, rememberMe: Boolean = false): AuthResult
  suspend fun logout()
  suspend fun changePassword(email: String, currentPassword: String, newPassword: String): Result<Unit>
  suspend fun sendPasswordResetEmail(email: String): Result<Unit>
  suspend fun fetchUsersAndRoles(): List<RoleDoc>
  suspend fun runInitialSetup(): List<SetupAccountStatus>
  fun getCurrentUser(): UserProfile?
  fun isFirebaseConnected(): Boolean
  fun getRememberedEmail(): String
  fun isRememberMeEnabled(): Boolean
  fun checkInactivityTimeout(lastActiveMillis: Long): Boolean
}

class AuthRepository(
  private val context: Context,
  private val configManager: FirebaseConfigManager = FirebaseConfigManager(context)
) : IAuthRepository {

  companion object {
    private const val TAG = "AuthRepository"
    private const val PREFS_NAME = "ceo_pulse_auth_prefs"
    private const val KEY_REMEMBERED_EMAIL = "remembered_email"
    private const val KEY_REMEMBER_ME = "remember_me"
    const val INACTIVITY_TIMEOUT_MILLIS = 30 * 60 * 1000L // 30 minutes
  }

  private val authPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private var cachedUser: UserProfile? = null

  // Local/Offline test roles & credentials registry (keeps app 100% testable and functional offline)
  private val localRoles = mutableMapOf(
    "uzairkhanp78@gmail.com" to RoleDoc("uzairkhanp78@gmail.com", "super_admin", mustChangePassword = true),
    "shehzadruman@gmail.com" to RoleDoc("shehzadruman@gmail.com", "ceo", mustChangePassword = true)
  )

  private val localPasswords = mutableMapOf(
    "uzairkhanp78@gmail.com" to "Uzair@Pulse2026!",
    "shehzadruman@gmail.com" to "Shehzad@Pulse2026!"
  )

  override fun isFirebaseConnected(): Boolean {
    return try {
      FirebaseApp.getApps(context).isNotEmpty() && configManager.isConfigured()
    } catch (e: Exception) {
      false
    }
  }

  override fun getCurrentUser(): UserProfile? = cachedUser

  override fun getRememberedEmail(): String {
    return if (isRememberMeEnabled()) {
      authPrefs.getString(KEY_REMEMBERED_EMAIL, "") ?: ""
    } else {
      ""
    }
  }

  override fun isRememberMeEnabled(): Boolean {
    return authPrefs.getBoolean(KEY_REMEMBER_ME, false)
  }

  override fun checkInactivityTimeout(lastActiveMillis: Long): Boolean {
    // Inactivity auto-logout of 30 minutes applies ONLY when Remember me is OFF
    if (isRememberMeEnabled()) return false
    if (lastActiveMillis <= 0L) return false
    return (System.currentTimeMillis() - lastActiveMillis) >= INACTIVITY_TIMEOUT_MILLIS
  }

  override suspend fun login(
    email: String,
    password: String,
    rememberMe: Boolean
  ): AuthResult = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim().lowercase()

    if (cleanEmail.isBlank() || password.isBlank()) {
      return@withContext AuthResult.Failure("Email and password cannot be empty.")
    }

    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      return@withContext AuthResult.Failure("Please enter a valid email address.")
    }

    // Save or clear remember-me preference (NEVER save password!)
    if (rememberMe) {
      authPrefs.edit()
        .putString(KEY_REMEMBERED_EMAIL, cleanEmail)
        .putBoolean(KEY_REMEMBER_ME, true)
        .apply()
    } else {
      authPrefs.edit()
        .remove(KEY_REMEMBERED_EMAIL)
        .putBoolean(KEY_REMEMBER_ME, false)
        .apply()
    }

    // -------------------------------------------------------------
    // PATH A: LIVE FIREBASE AUTHENTICATION
    // -------------------------------------------------------------
    if (isFirebaseConnected()) {
      try {
        val auth = FirebaseAuth.getInstance()
        val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).awaitTask()
        val firebaseUser = authResult.user

        if (firebaseUser != null) {
          // STEP 2: Query Firestore collection "roles", document ID = cleanEmail
          val firestore = FirebaseFirestore.getInstance()
          val roleDocSnapshot = try {
            firestore.collection("roles").document(cleanEmail).get().awaitTask()
          } catch (e: Exception) {
            Log.e(TAG, "Failed to query roles collection", e)
            null
          }

          if (roleDocSnapshot == null || !roleDocSnapshot.exists()) {
            // STEP 2 REQUIREMENT: No role document means sign out and show "Access not granted"
            auth.signOut()
            cachedUser = null
            return@withContext AuthResult.AccessDenied(
              email = cleanEmail,
              message = "Access not granted"
            )
          }

          val role = roleDocSnapshot.getString("role")?.trim() ?: ""
          if (role != "super_admin" && role != "ceo") {
            auth.signOut()
            cachedUser = null
            return@withContext AuthResult.AccessDenied(
              email = cleanEmail,
              message = "Access not granted"
            )
          }

          val mustChange = roleDocSnapshot.getBoolean("mustChangePassword") ?: false

          val profile = UserProfile(
            email = firebaseUser.email ?: cleanEmail,
            uid = firebaseUser.uid,
            role = role,
            displayName = if (role == "super_admin") "Super Admin" else "CEO Executive",
            mustChangePassword = mustChange
          )

          cachedUser = profile

          // STEP 4: Forced password change intercept
          if (mustChange) {
            return@withContext AuthResult.MustChangePassword(profile)
          }

          return@withContext AuthResult.Success(profile)
        } else {
          return@withContext AuthResult.Failure("Authentication returned no active session.")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Firebase login failed", e)
        return@withContext mapAuthExceptionToFriendlyMessage(e)
      }
    }

    // -------------------------------------------------------------
    // PATH B: LOCAL DETERMINISTIC AUTHENTICATION (Offline & Unit Tests)
    // -------------------------------------------------------------
    val roleDoc = localRoles[cleanEmail]
    if (roleDoc == null) {
      cachedUser = null
      return@withContext AuthResult.AccessDenied(
        email = cleanEmail,
        message = "Access not granted"
      )
    }

    val expectedPassword = localPasswords[cleanEmail]
    if (expectedPassword != null && expectedPassword != password) {
      return@withContext AuthResult.Failure("Invalid email or password. Please verify your credentials and try again.")
    }

    val profile = UserProfile(
      email = cleanEmail,
      uid = "usr_${cleanEmail.hashCode()}",
      role = roleDoc.role,
      displayName = if (roleDoc.role == "super_admin") "Super Admin" else "CEO Executive",
      mustChangePassword = roleDoc.mustChangePassword
    )

    cachedUser = profile

    if (roleDoc.mustChangePassword) {
      return@withContext AuthResult.MustChangePassword(profile)
    }

    return@withContext AuthResult.Success(profile)
  }

  private fun mapAuthExceptionToFriendlyMessage(e: Exception): AuthResult {
    val msg = e.message?.lowercase() ?: ""
    return when {
      e is FirebaseAuthInvalidUserException ||
      e is FirebaseAuthInvalidCredentialsException ||
      msg.contains("invalid-credential") ||
      msg.contains("wrong-password") ||
      msg.contains("user-not-found") -> {
        // STEP 3 REQUIREMENT: Friendly error, never reveal whether an email exists
        AuthResult.Failure("Invalid email or password. Please verify your credentials and try again.")
      }

      msg.contains("too-many-requests") || msg.contains("activity-blocked") -> {
        AuthResult.Failure("Too many failed attempts. For security, please try again in a few minutes or reset your password.")
      }

      e is IOException || msg.contains("network") || msg.contains("timeout") -> {
        AuthResult.Failure("Unable to connect to the network. Please check your internet connection.")
      }

      else -> {
        AuthResult.Failure("Unable to sign in at this time. Please verify your credentials and try again.")
      }
    }
  }

  override suspend fun changePassword(
    email: String,
    currentPassword: String,
    newPassword: String
  ): Result<Unit> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim().lowercase()

    // Validate new password rules: min 8 characters, one number, one symbol
    val validationError = validatePasswordComplexity(newPassword)
    if (validationError != null) {
      return@withContext Result.failure(IllegalArgumentException(validationError))
    }

    if (isFirebaseConnected()) {
      try {
        val auth = FirebaseAuth.getInstance()
        val user: FirebaseUser = auth.currentUser
          ?: return@withContext Result.failure(IllegalStateException("No authenticated user found. Please log in again."))

        // 1. Re-authenticate with current credentials
        val credential = EmailAuthProvider.getCredential(cleanEmail, currentPassword)
        user.reauthenticate(credential).awaitTask()

        // 2. Update to new password
        user.updatePassword(newPassword).awaitTask()

        // 3. Set mustChangePassword = false in Firestore collection "roles"
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("roles").document(cleanEmail)
          .update("mustChangePassword", false)
          .awaitTask()

        // Update cached session
        cachedUser = cachedUser?.copy(mustChangePassword = false)

        return@withContext Result.success(Unit)
      } catch (e: Exception) {
        Log.e(TAG, "Change password failed", e)
        val friendlyMessage = when {
          e is FirebaseAuthInvalidCredentialsException || e.message?.contains("wrong-password") == true ->
            "Current password is incorrect. Please re-enter your current password."
          e.message?.contains("too-many-requests") == true ->
            "Too many attempts. Please try again later."
          else -> e.localizedMessage ?: "Failed to update password."
        }
        return@withContext Result.failure(Exception(friendlyMessage))
      }
    }

    // Local / Offline mode
    val expected = localPasswords[cleanEmail]
    if (expected != null && expected != currentPassword) {
      return@withContext Result.failure(IllegalArgumentException("Current password is incorrect."))
    }

    localPasswords[cleanEmail] = newPassword
    localRoles[cleanEmail] = localRoles[cleanEmail]?.copy(mustChangePassword = false)
      ?: RoleDoc(cleanEmail, "ceo", mustChangePassword = false)

    cachedUser = cachedUser?.copy(mustChangePassword = false)
    return@withContext Result.success(Unit)
  }

  private fun validatePasswordComplexity(password: String): String? {
    if (password.length < 8) {
      return "New password must be at least 8 characters long."
    }
    if (!password.any { it.isDigit() }) {
      return "New password must contain at least one number."
    }
    if (!password.any { !it.isLetterOrDigit() }) {
      return "New password must contain at least one symbol (e.g. !@#$%^&*)."
    }
    return null
  }

  override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim().lowercase()
    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
      return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address."))
    }

    if (isFirebaseConnected()) {
      try {
        FirebaseAuth.getInstance().sendPasswordResetEmail(cleanEmail).awaitTask()
        return@withContext Result.success(Unit)
      } catch (e: Exception) {
        Log.e(TAG, "sendPasswordResetEmail failed", e)
        // If user not found, never reveal it
        return@withContext Result.success(Unit)
      }
    }

    // Local mock success
    return@withContext Result.success(Unit)
  }

  override suspend fun fetchUsersAndRoles(): List<RoleDoc> = withContext(Dispatchers.IO) {
    if (isFirebaseConnected()) {
      try {
        val firestore = FirebaseFirestore.getInstance()
        val snapshot = firestore.collection("roles").get().awaitTask()
        val list = mutableListOf<RoleDoc>()
        for (doc in snapshot.documents) {
          val email = doc.id
          val role = doc.getString("role") ?: "ceo"
          val mustChange = doc.getBoolean("mustChangePassword") ?: false
          list.add(RoleDoc(email = email, role = role, mustChangePassword = mustChange))
        }
        if (list.isNotEmpty()) return@withContext list
      } catch (e: Exception) {
        Log.e(TAG, "fetchUsersAndRoles failed", e)
      }
    }

    return@withContext localRoles.values.toList()
  }

  /**
   * STEP 1: ONE-TIME INITIAL ACCOUNT SETUP ROUTINE
   * Creates uzairkhanp78@gmail.com (super_admin) & shehzadruman@gmail.com (ceo)
   * Skips any user that already exists. Safe to run multiple times.
   * Never logs or exposes password strings.
   */
  override suspend fun runInitialSetup(): List<SetupAccountStatus> = withContext(Dispatchers.IO) {
    val targetAccounts = listOf(
      Triple("uzairkhanp78@gmail.com", "Uzair@Pulse2026!", "super_admin"),
      Triple("shehzadruman@gmail.com", "Shehzad@Pulse2026!", "ceo")
    )

    val results = mutableListOf<SetupAccountStatus>()

    for ((email, tempPass, role) in targetAccounts) {
      val cleanEmail = email.trim().lowercase()

      if (isFirebaseConnected()) {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        var userStatus = SetupStatus.CREATED
        var statusMessage = "Account created with role '$role'."

        // 1. Attempt createUserWithEmailAndPassword
        try {
          auth.createUserWithEmailAndPassword(cleanEmail, tempPass).awaitTask()
          userStatus = SetupStatus.CREATED
          statusMessage = "Created new user in Firebase Auth."
        } catch (e: Exception) {
          if (e is FirebaseAuthUserCollisionException || e.message?.contains("email-already-in-use") == true) {
            userStatus = SetupStatus.ALREADY_EXISTS
            statusMessage = "User already exists in Firebase Auth."
          } else {
            Log.e(TAG, "Initial setup failed for $cleanEmail", e)
            results.add(
              SetupAccountStatus(
                email = cleanEmail,
                role = role,
                status = SetupStatus.FAILED,
                message = e.localizedMessage ?: "Creation failed"
              )
            )
            continue
          }
        }

        // 2. Ensure Firestore collection "roles", document ID = cleanEmail
        try {
          val roleData = hashMapOf(
            "email" to cleanEmail,
            "role" to role,
            "mustChangePassword" to true,
            "updatedAt" to com.google.firebase.Timestamp.now()
          )
          firestore.collection("roles").document(cleanEmail).set(roleData).awaitTask()
        } catch (e: Exception) {
          Log.e(TAG, "Failed to write role document for $cleanEmail", e)
        }

        results.add(
          SetupAccountStatus(
            email = cleanEmail,
            role = role,
            status = userStatus,
            message = statusMessage
          )
        )
      } else {
        // Offline / Pre-config local environment
        val existing = localRoles[cleanEmail]
        val status = if (existing != null) SetupStatus.ALREADY_EXISTS else SetupStatus.CREATED
        localRoles[cleanEmail] = RoleDoc(cleanEmail, role, mustChangePassword = true)
        localPasswords[cleanEmail] = tempPass

        results.add(
          SetupAccountStatus(
            email = cleanEmail,
            role = role,
            status = status,
            message = if (status == SetupStatus.CREATED) "Provisioned initial local credentials." else "Account already registered in local store."
          )
        )
      }
    }

    return@withContext results
  }

  override suspend fun logout() = withContext(Dispatchers.IO) {
    if (isFirebaseConnected()) {
      try {
        FirebaseAuth.getInstance().signOut()
      } catch (e: Exception) {
        Log.e(TAG, "Error signing out of Firebase", e)
      }
    }
    cachedUser = null
  }
}
