package com.empire.server.packaging

import com.empire.server.util.sha256
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BundlePackagerTest {
    private fun newDir(): File = Files.createTempDirectory("empire-test").toFile()

    @Test
    fun `includes only source files that actually exist, ignoring missing ones`() {
        val bundleDir = newDir()
        val existing = File(newDir(), "exists.md").apply { writeText("content") }
        val missing = File(newDir(), "does-not-exist.md")

        val info = BundlePackager.createBundle(bundleDir, "Product", "1.0.0", listOf(existing, missing))

        assertTrue(info.bundleExists)
        assertEquals(listOf("exists.md"), info.copiedFiles)
    }

    @Test
    fun `sanitizes the product name into a filesystem-safe zip filename`() {
        val bundleDir = newDir()
        val source = File(newDir(), "a.md").apply { writeText("x") }

        BundlePackager.createBundle(bundleDir, "Senior Dog Nutrition!", "1.0.0", listOf(source))

        assertTrue(File(bundleDir, "senior-dog-nutrition-v1.0.0.zip").exists())
    }

    @Test
    fun `falls back to 'product' when the product name has no usable characters`() {
        val bundleDir = newDir()
        val source = File(newDir(), "a.md").apply { writeText("x") }

        BundlePackager.createBundle(bundleDir, "!!!", "1.0.0", listOf(source))

        assertTrue(File(bundleDir, "product-v1.0.0.zip").exists())
    }

    @Test
    fun `reports the actual checksum of the zip file it wrote`() {
        val bundleDir = newDir()
        val source = File(newDir(), "a.md").apply { writeText("x") }

        val info = BundlePackager.createBundle(bundleDir, "Product", "1.0.0", listOf(source))

        val zipFile = File(bundleDir, "product-v1.0.0.zip")
        assertEquals(sha256(zipFile), info.checksumSha256)
    }
}
