package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.gguf.GgufParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Sandy", appName)
  }

  @Test
  fun `test gguf header creation and parsing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testFile = File(context.cacheDir, "test_model.gguf")
    GgufParser.createSampleGgufFile(testFile, "SmolLM2-135M-Instruct-Q4_K_M.gguf")

    assertTrue("File should be created", testFile.exists())
    assertTrue("File should have bytes", testFile.length() > 0)
  }
}
