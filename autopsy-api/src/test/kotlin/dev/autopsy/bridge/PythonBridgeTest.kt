package dev.autopsy.bridge

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PythonBridgeTest {

    @Test
    fun `extract with invalid path throws BridgeException`() {
        val bridge = PythonBridge(
            pythonCommand = "python3",
            engineDir = "../autopsy-engine",
        )
        assertFailsWith<BridgeException> {
            bridge.extract("/nonexistent/path")
        }
    }

    @Test
    fun `bridge exception contains stderr message`() {
        val bridge = PythonBridge(
            pythonCommand = "python3",
            engineDir = "../autopsy-engine",
        )
        val exception = assertFailsWith<BridgeException> {
            bridge.extract("/nonexistent/path")
        }
        assertNotNull(exception.message)
        assertTrue(exception.message!!.isNotBlank())
    }

    @Test
    fun `bad python command throws BridgeException`() {
        val bridge = PythonBridge(
            pythonCommand = "nonexistent-python-binary",
            engineDir = "../autopsy-engine",
        )
        assertFailsWith<BridgeException> {
            bridge.extract("/some/path")
        }
    }
}
