package com.example

import com.example.update.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UpdateManifestRobolectricTest {

    private val validJson = """
        {
          "versionCode": 2,
          "versionName": "1.1",
          "apkUrl": "https://github.com/nurulhudaturag-droid/Monthly-Khoroch-Android-App/releases/latest/download/MonthlyKhoroch.apk",
          "sha256": "ABC123DEF",
          "notes": "নতুন ফিচার"
        }
    """.trimIndent()

    @Test
    fun `parses valid update manifest`() {
        val manifest = UpdateChecker.parseManifest(validJson)
        assertEquals(2, manifest.versionCode)
        assertEquals("1.1", manifest.versionName)
        assertTrue(manifest.apkUrl.startsWith("https://"))
        assertEquals("abc123def", manifest.sha256)
        assertEquals("নতুন ফিচার", manifest.notes)
    }

    @Test
    fun `parses manifest without optional fields`() {
        val manifest = UpdateChecker.parseManifest(
            """{"versionCode":3,"versionName":"1.2","apkUrl":"https://example.com/a.apk"}"""
        )
        assertEquals(3, manifest.versionCode)
        assertEquals("", manifest.sha256)
        assertEquals("", manifest.notes)
    }

    @Test(expected = Exception::class)
    fun `rejects invalid json`() {
        UpdateChecker.parseManifest("{ invalid json content }")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non https apk url`() {
        UpdateChecker.parseManifest(
            """{"versionCode":2,"versionName":"1.1","apkUrl":"http://example.com/a.apk"}"""
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non positive versionCode`() {
        UpdateChecker.parseManifest(
            """{"versionCode":0,"versionName":"1.1","apkUrl":"https://example.com/a.apk"}"""
        )
    }

    @Test
    fun `sha256 matches known vectors`() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            UpdateChecker.sha256(ByteArray(0))
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            UpdateChecker.sha256("abc".toByteArray())
        )
    }
}
