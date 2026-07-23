package com.empire.server.packaging

import com.empire.dashboard.data.BundleInfo
import com.empire.server.util.sha256
import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BundlePackager {
    fun createBundle(bundleDir: File, productName: String, version: String, sourceFiles: List<File>): BundleInfo {
        bundleDir.mkdirs()
        val safeName = productName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "product" }
        val zipFile = File(bundleDir, "$safeName-v$version.zip")
        val existingFiles = sourceFiles.filter { it.exists() }

        ZipOutputStream(zipFile.outputStream()).use { zos ->
            existingFiles.forEach { file ->
                zos.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }

        return BundleInfo(
            productName = productName,
            version = version,
            generatedAt = Instant.now().toString(),
            checksumSha256 = sha256(zipFile),
            bundleExists = true,
            copiedFiles = existingFiles.map { it.name }
        )
    }
}
