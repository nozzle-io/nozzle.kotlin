package nozzle

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EnumConformanceTest {
    private val propertyPrefix = "nozzle.enum."

    // Exact identifiers, not family-wide exemptions: new C members require review.
    private val unexposed = mapOf(
        "NozzleTransferMode.NOZZLE_TRANSFER_UNKNOWN" to "Transfer mode is not exposed in Kotlin",
        "NozzleTransferMode.NOZZLE_TRANSFER_ZERO_COPY_SHARED_TEXTURE" to "Transfer mode is not exposed in Kotlin",
        "NozzleTransferMode.NOZZLE_TRANSFER_GPU_COPY" to "Transfer mode is not exposed in Kotlin",
        "NozzleTransferMode.NOZZLE_TRANSFER_CPU_COPY" to "Transfer mode is not exposed in Kotlin",
        "NozzleSyncMode.NOZZLE_SYNC_NONE" to "Sync mode is not exposed in Kotlin",
        "NozzleSyncMode.NOZZLE_SYNC_ACCESS_GUARDED" to "Sync mode is not exposed in Kotlin",
        "NozzleSyncMode.NOZZLE_SYNC_GPU_FENCE_BEST_EFFORT" to "Sync mode is not exposed in Kotlin",
        "NozzleBackendSharingMechanism.NOZZLE_SHARING_NONE" to "Backend sharing query is not exposed in Kotlin",
        "NozzleBackendSharingMechanism.NOZZLE_SHARING_IOSURFACE" to "Backend sharing query is not exposed in Kotlin",
        "NozzleBackendSharingMechanism.NOZZLE_SHARING_D3D11_NT_HANDLE" to "Backend sharing query is not exposed in Kotlin",
        "NozzleBackendSharingMechanism.NOZZLE_SHARING_DMA_BUF" to "Backend sharing query is not exposed in Kotlin",
        "NozzleBackendSharingMechanism.NOZZLE_SHARING_OPENGL_TEXTURE" to "Backend sharing query is not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_NONE" to "Backend capability flags are not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_SENDER" to "Backend capability flags are not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_RECEIVER" to "Backend capability flags are not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_WRITABLE_FRAMES" to "Backend capability flags are not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_NATIVE_TEXTURE_PUBLISH" to "Backend capability flags are not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_DIRECT_EXTERNAL_PUBLISH" to "Backend capability flags are not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_CPU_READ" to "Backend capability flags are not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_CPU_WRITE" to "Backend capability flags are not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_ZERO_COPY_RECEIVE" to "Backend capability flags are not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_ZERO_COPY_PUBLISH" to "Backend capability flags are not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_REQUIRES_MATCHING_BACKEND" to "Backend capability flags are not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_SINGLE_SENDER_PER_PROCESS" to "Backend capability flags are not exposed in Kotlin",
        "NozzleBackendCapabilityFlags.NOZZLE_BACKEND_CAP_MAY_REQUIRE_RUNTIME_PROBE" to "Backend capability flags are not exposed in Kotlin",
        "NozzleFallbackCategory.NOZZLE_FALLBACK_CATEGORY_NONE" to "Fallback category is not exposed in Kotlin",
        "NozzleFallbackCategory.NOZZLE_FALLBACK_CATEGORY_STORAGE_COMPATIBLE" to "Fallback category is not exposed in Kotlin",
        "NozzleFallbackCategory.NOZZLE_FALLBACK_CATEGORY_CHANNEL_EXPANSION" to "Fallback category is not exposed in Kotlin",
        "NozzleFallbackCategory.NOZZLE_FALLBACK_CATEGORY_QUALITY_LOSS" to "Fallback category is not exposed in Kotlin",
        "NozzleChannelSwizzle.NOZZLE_CHANNEL_SWIZZLE_IDENTITY" to "Channel swizzle is not exposed in Kotlin",
        "NozzleChannelSwizzle.NOZZLE_CHANNEL_SWIZZLE_SWAP_RB" to "Channel swizzle is not exposed in Kotlin"
    )

    @Test
    fun productionEnumsMatchCoreManifest() {
        val covered = mutableSetOf<String>()
        checkFamily(covered, "NozzleErrorCode", ErrorCode.entries,
            { if (it == ErrorCode.OK) "NOZZLE_OK" else "NOZZLE_ERROR_${it.name}" },
            { it.value }, ErrorCode::fromValue)
        checkFamily(covered, "NozzleBackendType", BackendType.entries,
            { "NOZZLE_BACKEND_${it.name}" }, { it.value }, BackendType::fromValue)
        checkFamily(covered, "NozzleTextureFormat", TextureFormat.entries,
            { "NOZZLE_FORMAT_${it.name}" }, { it.value }, TextureFormat::fromValue)
        checkFamily(covered, "NozzleReceiveMode", ReceiveMode.entries,
            { "NOZZLE_RECEIVE_${it.name}" }, { it.value }, ReceiveMode::fromValue)
        checkFamily(covered, "NozzleFrameStatus", FrameStatus.entries,
            { "NOZZLE_FRAME_${it.name}" }, { it.value }, FrameStatus::fromValue)
        checkFamily(covered, "NozzleTextureOrigin", TextureOrigin.entries,
            { "NOZZLE_ORIGIN_${it.name}" }, { it.value }, TextureOrigin::fromValue)
        checkFamily(covered, "NozzleFormatSource", FormatSource.entries,
            { "NOZZLE_FORMAT_SOURCE_${it.name}" }, { it.value }, FormatSource::fromValue)
        checkFamily(covered, "NozzleNativeFormatKind", NativeFormatKind.entries,
            { "NOZZLE_NATIVE_KIND_${it.name}" }, { it.value }, NativeFormatKind::fromValue)

        unexposed.forEach { (identifier, reason) ->
            assertFalse(reason.isBlank(), "$identifier needs an unexposed reason")
            assertTrue(covered.add(identifier), "$identifier is both mapped and unexposed")
        }
        val oracleMembers = System.getProperties().stringPropertyNames()
            .filter { it.startsWith(propertyPrefix) }
            .map { it.removePrefix(propertyPrefix) }
            .toSet()
        assertEquals(oracleMembers, covered, "Every C member must be mapped or explicitly unexposed")
    }

    private fun <E : Enum<E>> checkFamily(
        covered: MutableSet<String>, family: String, cases: List<E>, cName: (E) -> String,
        value: (E) -> Int, decode: (Int) -> E
    ) {
        for (item in cases) {
            val identifier = "$family.${cName(item)}"
            assertTrue(covered.add(identifier), "Duplicate mapping: $identifier")
            val property = System.getProperty(propertyPrefix + identifier)
            assertNotNull(property, "Missing core oracle member: $identifier")
            val expected = property.toInt()
            assertEquals(expected, value(item), "$identifier value")
            assertSame(item, decode(expected), "$identifier fromValue")
        }
    }
}
