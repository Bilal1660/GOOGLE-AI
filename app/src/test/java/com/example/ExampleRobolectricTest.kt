package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Expense Manager", appName)
  }

  @Test
  fun `verify camera permission declared in manifest`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val packageInfo = context.packageManager.getPackageInfo(
      context.packageName,
      android.content.pm.PackageManager.GET_PERMISSIONS
    )
    val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
    org.junit.Assert.assertTrue(
      "Camera permission must be declared in AndroidManifest.xml",
      requestedPermissions.contains(android.Manifest.permission.CAMERA)
    )
  }
}
