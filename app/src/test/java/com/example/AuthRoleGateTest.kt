package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AuthRepository
import com.example.data.FirebaseConfigManager
import com.example.model.AuthResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthRoleGateTest {

  private lateinit var context: Context
  private lateinit var authRepository: AuthRepository

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext<Context>()
    val configManager = FirebaseConfigManager(context)
    authRepository = AuthRepository(context, configManager)
  }

  @Test
  fun `rejects blank credentials without information leakage`() = runTest {
    val emptyEmailResult = authRepository.login("", "secret123")
    assertTrue(emptyEmailResult is AuthResult.Failure)
    assertEquals("Email and password cannot be empty.", (emptyEmailResult as AuthResult.Failure).error)

    val emptyPasswordResult = authRepository.login("uzairkhanp78@gmail.com", "")
    assertTrue(emptyPasswordResult is AuthResult.Failure)
    assertEquals("Email and password cannot be empty.", (emptyPasswordResult as AuthResult.Failure).error)
  }

  @Test
  fun `rejects invalid email format`() = runTest {
    val result = authRepository.login("invalid-email-address", "secret123")
    assertTrue(result is AuthResult.Failure)
    assertEquals("Please enter a valid email address.", (result as AuthResult.Failure).error)
  }

  @Test
  fun `role gate rejects unauthorized email not in roles collection`() = runTest {
    val unauthorizedEmail = "intruder@externaldomain.com"
    val result = authRepository.login(unauthorizedEmail, "validPassword123")

    assertTrue("Expected AccessDenied result for unauthorized email", result is AuthResult.AccessDenied)
    val accessDenied = result as AuthResult.AccessDenied
    assertEquals(unauthorizedEmail, accessDenied.email)
    assertEquals("Access not granted", accessDenied.message)
    assertNull("Unauthorized user must not be cached in session", authRepository.getCurrentUser())
  }

  @Test
  fun `super_admin account logs in and triggers forced password change on first login`() = runTest {
    val adminEmail = "uzairkhanp78@gmail.com"
    val result = authRepository.login(adminEmail, "Uzair@Pulse2026!")

    assertTrue("Expected MustChangePassword result for new super_admin", result is AuthResult.MustChangePassword)
    val mustChange = result as AuthResult.MustChangePassword
    assertEquals(adminEmail, mustChange.user.email)
    assertEquals("super_admin", mustChange.user.role)
    assertTrue(mustChange.user.mustChangePassword)
    assertTrue(mustChange.user.isSuperAdmin)
  }

  @Test
  fun `ceo account logs in with read-only role flag`() = runTest {
    val ceoEmail = "shehzadruman@gmail.com"
    val result = authRepository.login(ceoEmail, "Shehzad@Pulse2026!")

    assertTrue("Expected MustChangePassword result for new ceo", result is AuthResult.MustChangePassword)
    val mustChange = result as AuthResult.MustChangePassword
    assertEquals(ceoEmail, mustChange.user.email)
    assertEquals("ceo", mustChange.user.role)
    assertTrue(mustChange.user.isCeo)
    assertTrue(mustChange.user.isViewOnly)
  }

  @Test
  fun `password change validates complexity and clears mustChangePassword`() = runTest {
    val adminEmail = "uzairkhanp78@gmail.com"

    // Weak password rejected
    val weakRes = authRepository.changePassword(adminEmail, "Uzair@Pulse2026!", "weakpass")
    assertTrue(weakRes.isFailure)

    // Strong password accepted (>= 8 chars, 1 number, 1 symbol)
    val strongPass = "NewSecure#2026Key"
    val strongRes = authRepository.changePassword(adminEmail, "Uzair@Pulse2026!", strongPass)
    assertTrue(strongRes.isSuccess)

    // Login with new password succeeds without MustChangePassword
    val nextLogin = authRepository.login(adminEmail, strongPass)
    assertTrue("Should succeed normally now", nextLogin is AuthResult.Success)
    val success = nextLogin as AuthResult.Success
    assertEquals(false, success.user.mustChangePassword)
  }

  @Test
  fun `remember me persists email while never storing password`() = runTest {
    val adminEmail = "uzairkhanp78@gmail.com"
    authRepository.login(adminEmail, "Uzair@Pulse2026!", rememberMe = true)

    assertTrue(authRepository.isRememberMeEnabled())
    assertEquals(adminEmail, authRepository.getRememberedEmail())

    // Login without remember me clears it
    authRepository.login(adminEmail, "Uzair@Pulse2026!", rememberMe = false)
    assertEquals(false, authRepository.isRememberMeEnabled())
    assertEquals("", authRepository.getRememberedEmail())
  }

  @Test
  fun `logout clears session state`() = runTest {
    val adminEmail = "uzairkhanp78@gmail.com"
    authRepository.login(adminEmail, "Uzair@Pulse2026!")
    assertNotNull(authRepository.getCurrentUser())

    authRepository.logout()
    assertNull("Current user must be null after logout", authRepository.getCurrentUser())
  }
}
