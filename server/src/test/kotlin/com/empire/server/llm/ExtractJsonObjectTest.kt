package com.empire.server.llm

import kotlin.test.Test
import kotlin.test.assertEquals

class ExtractJsonObjectTest {
    @Test
    fun `returns the object unchanged when there is no fence or prose`() {
        assertEquals("""{"a":1}""", extractJsonObject("""{"a":1}"""))
    }

    @Test
    fun `strips a json code fence`() {
        val text = "```json\n{\"a\":1}\n```"

        assertEquals("""{"a":1}""", extractJsonObject(text))
    }

    @Test
    fun `strips surrounding prose outside the braces`() {
        val text = "Sure, here you go:\n{\"a\":1}\nHope that helps!"

        assertEquals("""{"a":1}""", extractJsonObject(text))
    }
}
