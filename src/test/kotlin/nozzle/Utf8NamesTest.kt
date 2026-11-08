package nozzle

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeFalse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Utf8NamesTest {
    private val supplementary = "\uD83D\uDE80"
    private val application = "\u65E5\u672C-app-" + supplementary

    private fun metadataName(label: String): String {
        val name = "k193-$label-${ProcessHandle.current().pid()}-${System.nanoTime()}-\u65E5$supplementary"
        assertTrue(name.toByteArray(Charsets.UTF_8).size <= 63)
        return name
    }

    private fun requireMetadataRuntime() {
        val os = System.getProperty("os.name")
        assumeFalse(os.startsWith("Linux"),
            "SKIP: Linux default backend unavailable; no native metadata lifecycle claim")
        assertTrue(os.startsWith("Mac") || os.startsWith("Windows"), "Unsupported metadata platform: $os")
    }

    private fun expectedBackend(): BackendType =
        if (System.getProperty("os.name").startsWith("Windows")) BackendType.D3D11 else BackendType.METAL

    @Test
    fun nativeCloseRetainsIdentityButClearsConnection() {
        requireMetadataRuntime()
        val name = metadataName("close")
        Sender.create(SenderDesc(name, application)).use { sender ->
            Receiver.create(ReceiverDesc(name, application)).use { receiver ->
                val own = sender.info()
                val before = receiver.connectedInfo()
                assertEquals(expectedBackend(), own.backend)
                assertEquals(name, before.name)
                assertEquals(application, before.applicationName)
                assertEquals(own.id, before.id)
                assertEquals(own.backend, before.backend)
                assertTrue(receiver.isConnected)
                sender.close()
                val error = assertFailsWith<NozzleException> {
                    receiver.acquireFrame(0).use { }
                }
                assertEquals(NozzleException::class.java, error.javaClass)
                assertEquals(ErrorCode.SENDER_CLOSED, error.errorCode)
                val after = receiver.connectedInfo()
                assertEquals(before.name, after.name)
                assertEquals(before.applicationName, after.applicationName)
                assertEquals(before.id, after.id)
                assertEquals(before.backend, after.backend)
                val connected = receiver.isConnected
                println("METADATA_NATIVE acquire=senderClosed retained_id=${after.id} connected=$connected")
                assertFalse(connected, "After ${error.errorCode} with retained identity ${after.id}, isConnected must be false")
            }
        }
    }

    @Test
    fun nativeMetadataOwnsUtf8AfterLaterGettersAndBothCloses() {
        requireMetadataRuntime()
        val name = metadataName("owned")
        Sender.create(SenderDesc(name, application)).use { sender ->
            Receiver.create(ReceiverDesc(name, application)).use { receiver ->
                val own = sender.info()
                val info = receiver.connectedInfo()
                assertEquals(own.id, sender.info().id)
                assertEquals(info.id, receiver.connectedInfo().id)
                receiver.close()
                sender.close()
                assertEquals(name, own.name)
                assertEquals(application, own.applicationName)
                assertTrue(own.id.isNotEmpty())
                assertEquals(expectedBackend(), own.backend)
                assertEquals(own.name, info.name)
                assertEquals(own.applicationName, info.applicationName)
                assertEquals(own.id, info.id)
                assertEquals(own.backend, info.backend)
                println("METADATA_NATIVE owned_UTF8_after_close id=${own.id}")
            }
        }
    }

    @Test
    fun malformedNamesAndApplicationsAreRejectedThroughJni() {
        for (bad in listOf("a\u0000b", "a\uD800", "a\uDC00", "\uD800x")) {
            val operations = listOf<() -> Unit>(
                { Sender.create(SenderDesc(bad, application)).use {} },
                { Sender.create(SenderDesc("valid", bad)).use {} },
                { Receiver.create(ReceiverDesc(bad, application)).use {} },
                { Receiver.create(ReceiverDesc("valid", bad)).use {} }
            )
            for (operation in operations) {
                val error = assertFailsWith<NozzleException> { operation() }
                assertEquals(NozzleException::class.java, error.javaClass)
                assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode)
                assertEquals(if ('\u0000' in bad) "embedded NUL in name or application name"
                    else "unpaired surrogate in name or application name", error.message)
            }
        }
        val error = assertFailsWith<NozzleException> {
            Sender.create(SenderDesc("a".repeat(64), application)).use {}
        }
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode)
    }

    @Test
    fun malformedNativeBytesUseTheRuntimeErrorContract() {
        // Codec-level negative control; this does not simulate corrupted core IPC.
        val decode = NozzleNative::class.java.getDeclaredMethod("decodeUtf8", ByteArray::class.java)
        decode.isAccessible = true
        for (bad in listOf(byteArrayOf(0xff.toByte()), byteArrayOf(0xed.toByte(), 0xa0.toByte(), 0x80.toByte()))) {
            val error = assertFailsWith<NozzleException> {
                try {
                    decode.invoke(null, bad)
                } catch (error: java.lang.reflect.InvocationTargetException) {
                    throw error.targetException
                }
            }
            assertEquals(NozzleException::class.java, error.javaClass)
            assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode)
            assertEquals("invalid UTF-8 returned by native code", error.message)
        }
    }

    @Test
    fun nativeNonBmpNamesRoundTripAt63Bytes() {
        assumeFalse(System.getProperty("os.name").startsWith("Linux"),
            "SKIP: Linux default backend unavailable; malformed JNI controls run separately")
        val prefix = "k252-" + ProcessHandle.current().pid() + "-"
        val name = prefix + "a".repeat(56 - prefix.length) + "\u65E5" + supplementary
        assertEquals(63, name.toByteArray(Charsets.UTF_8).size)
        Sender.create(SenderDesc(name, application)).use { sender ->
            val own = sender.info()
            assertEquals(name, own.name)
            assertEquals(application, own.applicationName)
            val found = Nozzle.enumerateSenders().single { it.id == own.id }
            assertEquals(name, found.name)
            assertEquals(application, found.applicationName)
            Receiver.create(ReceiverDesc(name, application)).use { receiver ->
                val connected = receiver.connectedInfo()
                assertEquals(name, connected.name)
                assertEquals(application, connected.applicationName)
                assertEquals(own.id, connected.id)
            }
            val error = assertFailsWith<NozzleException> {
                Sender.create(SenderDesc(name + "a", application)).use {}
            }
            assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode)
        }
    }
}
