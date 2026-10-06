package com.example.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val notes: String
)

object UpdateChecker {

    const val REPO_OWNER = "nurulhudaturag-droid"
    const val REPO_NAME = "Monthly-Khoroch-Android-App"
    const val APK_ASSET_NAME = "MonthlyKhoroch.apk"
    const val MANIFEST_ASSET_NAME = "update.json"

    // Stable GitHub URLs: they always resolve to the assets of the latest published release.
    val manifestUrl: String
        get() = "https://github.com/$REPO_OWNER/$REPO_NAME/releases/latest/download/$MANIFEST_ASSET_NAME"

    fun parseManifest(json: String): UpdateManifest {
        val obj = JSONObject(json)
        val versionCode = obj.getInt("versionCode")
        require(versionCode > 0) { "versionCode must be positive" }
        val apkUrl = obj.getString("apkUrl")
        require(apkUrl.startsWith("https://")) { "apkUrl must be https" }
        return UpdateManifest(
            versionCode = versionCode,
            versionName = obj.getString("versionName"),
            apkUrl = apkUrl,
            sha256 = obj.optString("sha256", "").lowercase(),
            notes = obj.optString("notes", "")
        )
    }

    suspend fun fetchManifest(): UpdateManifest? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = URL(manifestUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.instanceFollowRedirects = true
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            parseManifest(body)
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    suspend fun downloadFile(url: String, dest: File, onProgress: (Float) -> Unit) =
        withContext(Dispatchers.IO) {
            dest.parentFile?.mkdirs()
            var conn: HttpURLConnection? = null
            try {
                conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 120_000
                conn.instanceFollowRedirects = true
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    error("HTTP ${conn.responseCode}")
                }
                val total = conn.contentLength.toLong()
                conn.inputStream.use { input ->
                    FileOutputStream(dest).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) onProgress(downloaded.toFloat() / total)
                        }
                        output.flush()
                        output.fd.sync()
                    }
                }
                onProgress(1f)
            } finally {
                conn?.disconnect()
            }
        }

    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }

    fun sha256(file: File): String = sha256(file.readBytes())
}
