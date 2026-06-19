package com.example.utils

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupArchiveHelper {

    /**
     * Call this with an OutputStream obtained from contentResolver.openOutputStream(uri)
     * where uri is the result from the Storage Access Framework document creation.
     */
    fun createBackupZip(
        context: Context,
        outputStream: java.io.OutputStream,
        dbHistoryJson: String,
        preferencesJson: String
    ) {
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
            // Add DB history
            zos.putNextEntry(ZipEntry("history.json"))
            OutputStreamWriter(zos).apply {
                write(dbHistoryJson)
                flush()
            }
            zos.closeEntry()

            // Add Preferences
            zos.putNextEntry(ZipEntry("preferences.json"))
            OutputStreamWriter(zos).apply {
                write(preferencesJson)
                flush()
            }
            zos.closeEntry()
        }
    }
}
