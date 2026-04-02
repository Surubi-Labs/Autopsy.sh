package dev.autopsy

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class ApplicationTest {

    @Test
    fun `application main class exists`() {
        assertNotNull(::main)
    }
}
