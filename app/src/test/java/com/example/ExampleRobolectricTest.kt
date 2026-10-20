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
import kotlinx.coroutines.flow.first
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

  @Test
  fun `test sandy interaction room database logging and query`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
        context,
        com.example.data.local.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.interactionDao()
    val testInteraction = com.example.data.local.SandyInteractionEntity(
        id = "test-123",
        command = "Sandy, call 9876543210",
        actionType = "CALL",
        actionSummary = "Call 9876543210",
        isVoice = true
    )

    dao.insertInteraction(testInteraction)
    val list = dao.getAllInteractionsFlow().first()
    assertEquals(1, list.size)
    assertEquals("Sandy, call 9876543210", list[0].command)
    assertEquals("CALL", list[0].actionType)
    assertTrue(list[0].isVoice)

    dao.deleteInteractionById("test-123")
    val listAfterDelete = dao.getAllInteractionsFlow().first()
    assertEquals(0, listAfterDelete.size)

    db.close()
  }
}
