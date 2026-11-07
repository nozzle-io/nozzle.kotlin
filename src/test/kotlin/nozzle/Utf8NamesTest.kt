package nozzle

import java.nio.charset.CharacterCodingException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assumptions.assumeFalse
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Utf8NamesTest {
    private val supplementary = "\uD83D\uDE80"
    private val application = "\u65E5\u672C-app-" + supplementary

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
                if ('\u0000' in bad) assertFailsWith<IllegalArgumentException> { operation() }
                else assertFailsWith<CharacterCodingException> { operation() }
            }
        }
        val error = assertFailsWith<NozzleException> {
            Sender.create(SenderDesc("a".repeat(64), application)).use {}
        }
        assertEquals(ErrorCode.INVALID_ARGUMENT, error.errorCode)
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
