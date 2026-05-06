# nozzle.kotlin

> This codebase is currently in its AI-slob prototyping phase: the code runs on momentum, vibes, and plausible intent.
> Proper debugging will be introduced once demand graduates from hypothetical to measurable.

Kotlin/JVM bindings for [nozzle](https://github.com/nozzle-io/nozzle) — cross-platform GPU texture sharing between local processes.

## Disclaimer / Notice

This library is currently a work in progress and contains many incomplete features and unverified implementations.
Although it may appear usable at first glance, it may not function correctly.

## Build Requirements

- JDK 11+
- Kotlin 2.0.0
- C++17 compiler (clang / MSVC / g++)
- macOS 12+, Windows 10+, or Linux

The nozzle C library is built from source via a git submodule. A `Makefile` compiles the JNI shared library before Gradle runs Kotlin compilation and tests.

## Build

```bash
make
./gradlew build
```

### Run Tests

```bash
make
./gradlew test
```

## Usage

### Sender

```kotlin
import nozzle.*

Sender.create(SenderDesc("kotlin-sender", "MyApp", 3)).use { sender ->
    sender.acquireWritableFrame(1920, 1080, TextureFormat.RGBA8_UNORM).use { frame ->
        frame.lockWritablePixels(TextureOrigin.TOP_LEFT).use { pixels ->
            for (y in 0 until pixels.height) {
                val row = pixels.row(y)
                while (row.hasRemaining()) {
                    row.put(0xFF.toByte())
                }
            }
        }
        sender.commitFrame(frame)
    }
}
```

### Receiver

```kotlin
import nozzle.*

Receiver.create(ReceiverDesc("kotlin-sender", "MyViewer")).use { receiver ->
    receiver.acquireFrame(5000).use { frame ->
        val info = frame.info()
        println("${info.width}x${info.height} frame #${info.frameIndex}")
    }
}
```

### Discovery

```kotlin
import nozzle.*

val senders = Nozzle.enumerateSenders()
println("found ${senders.size} senders")
```

### GPU Check

```kotlin
import nozzle.*

if (Nozzle.isGpuAvailable()) {
    println("GPU available")
}
```

## Error Handling

All fallible operations throw `NozzleException` (a `RuntimeException`):

```kotlin
try {
    Sender.create(SenderDesc("", "", 0))
} catch (e: NozzleException) {
    if (e.errorCode == ErrorCode.INVALID_ARGUMENT) {
        // handle bad args
    }
}
```

## Texture Formats

| Format | Bytes/Pixel |
|--------|-------------|
| `R8_UNORM` | 1 |
| `RG8_UNORM` | 2 |
| `RGBA8_UNORM` / `BGRA8_UNORM` | 4 |
| `RGBA8_SRGB` / `BGRA8_SRGB` | 4 |
| `R16_UNORM` | 2 |
| `RG16_UNORM` | 4 |
| `RGBA16_UNORM` | 8 |
| `R16_FLOAT` | 2 |
| `RG16_FLOAT` | 4 |
| `RGBA16_FLOAT` | 8 |
| `R32_FLOAT` | 4 |
| `RG32_FLOAT` | 8 |
| `RGBA32_FLOAT` | 16 |
| `R32_UINT` | 4 |
| `RGBA32_UINT` | 16 |
| `DEPTH32_FLOAT` | 4 |

## Platform Notes

- **macOS**: Links Metal, IOSurface, Foundation, Accelerate, OpenGL frameworks
- **Windows**: Links d3d11, dxgi, opengl32, bcrypt
- **Linux**: Links drm, gbm, EGL, GL

## License

MIT
