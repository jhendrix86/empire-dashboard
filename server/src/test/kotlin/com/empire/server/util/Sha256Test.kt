package com.empire.server.util

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class Sha256Test {
    @Test
    fun `matches the known SHA-256 digest of a fixed input`() {
        val file = Files.createTempFile("empire-test", ".txt").toFile().apply { writeText("hello") }

        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", sha256(file))
    }

    @Test
    fun `identical content hashes identically and different content hashes differently`() {
        val fileA = Files.createTempFile("empire-test", ".txt").toFile().apply { writeText("same content") }
        val fileB = Files.createTempFile("empire-test", ".txt").toFile().apply { writeText("same content") }
        val fileC = Files.createTempFile("empire-test", ".txt").toFile().apply { writeText("different content") }

        assertEquals(sha256(fileA), sha256(fileB))
        assertNotEquals(sha256(fileA), sha256(fileC))
    }
}
