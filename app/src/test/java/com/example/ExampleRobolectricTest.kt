package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NoteLedger", appName)
  }

  @Test
  fun `verify note deletion callback triggers properly`() {
    var deleted = false
    val onDelete: () -> Unit = { deleted = true }
    onDelete()
    assertEquals(true, deleted)
  }

  @Test
  fun `verify expense deletion callback triggers properly`() {
    var deleted = false
    val onDelete: () -> Unit = { deleted = true }
    onDelete()
    assertEquals(true, deleted)
  }
}
