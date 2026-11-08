#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <nozzle/nozzle_c.h>

static void throw_exception(JNIEnv *env, NozzleErrorCode code) {
    if (code == NOZZLE_OK) return;

    jclass cls = (*env)->FindClass(env, "nozzle/NozzleException");
    if (!cls) return;

    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>", "(I)V");
    if (ctor) {
        jthrowable ex = (jthrowable)(*env)->NewObject(env, cls, ctor, (jint)code);
        if (ex) (*env)->Throw(env, ex);
    }
    (*env)->DeleteLocalRef(env, cls);
}

// JVM charset codecs provide strict UTF-8; native storage is owned until the
// synchronous core create call returns. Never continue JNI work after failure.
static char *utf8_from_string(JNIEnv *env, jstring text) {
    if ((*env)->PushLocalFrame(env, 4) < 0) return NULL;
    char *result = NULL;
    jclass bridge = (*env)->FindClass(env, "nozzle/NozzleNative");
    if (!bridge) goto done;
    jmethodID encode = (*env)->GetStaticMethodID(env, bridge, "encodeUtf8", "(Ljava/lang/String;)[B");
    if (!encode) goto done;
    jbyteArray bytes = (jbyteArray)(*env)->CallStaticObjectMethod(env, bridge, encode, text);
    if ((*env)->ExceptionCheck(env) || !bytes) goto done;
    jsize size = (*env)->GetArrayLength(env, bytes);
    result = (char *)malloc((size_t)size + 1u);
    if (!result) {
        throw_exception(env, NOZZLE_ERROR_RESOURCE_CREATION_FAILED);
        goto done;
    }
    (*env)->GetByteArrayRegion(env, bytes, 0, size, (jbyte *)result);
    if ((*env)->ExceptionCheck(env)) {
        free(result);
        result = NULL;
    } else {
        result[size] = '\0';
    }
done:
    (*env)->PopLocalFrame(env, NULL);
    return result;
}

static jbyteArray utf8_bytes(JNIEnv *env, const char *text) {
    size_t size = text ? strlen(text) : 0u;
    if (size > INT32_MAX) {
        throw_exception(env, NOZZLE_ERROR_INVALID_ARGUMENT);
        return NULL;
    }
    jbyteArray bytes = (*env)->NewByteArray(env, (jsize)size);
    if (!bytes) return NULL;
    if (size) (*env)->SetByteArrayRegion(env, bytes, 0, (jsize)size, (const jbyte *)text);
    return (*env)->ExceptionCheck(env) ? NULL : bytes;
}

static jbyteArray snapshot_utf8_bytes(JNIEnv *env, const char *text, size_t capacity) {
    size_t size = 0;
    while (size < capacity && text[size] != '\0') ++size;
    if (size > INT32_MAX) {
        throw_exception(env, NOZZLE_ERROR_INVALID_ARGUMENT);
        return NULL;
    }
    jbyteArray bytes = (*env)->NewByteArray(env, (jsize)size);
    if (!bytes) return NULL;
    if (size) (*env)->SetByteArrayRegion(env, bytes, 0, (jsize)size, (const jbyte *)text);
    return (*env)->ExceptionCheck(env) ? NULL : bytes;
}

static jobject sender_info_object(JNIEnv *env, const NozzleSenderInfo *info) {
    if ((*env)->PushLocalFrame(env, 8) < 0) return NULL;
    jobject result = NULL;
    jclass bridge = (*env)->FindClass(env, "nozzle/NozzleNative");
    if (!bridge) goto done;
    jmethodID factory = (*env)->GetStaticMethodID(env, bridge, "senderInfo", "([B[B[BI)Lnozzle/SenderInfo;");
    if (!factory) goto done;
    jbyteArray name = utf8_bytes(env, info->name);
    if (!name) goto done;
    jbyteArray app = utf8_bytes(env, info->application_name);
    if (!app) goto done;
    jbyteArray id = utf8_bytes(env, info->id);
    if (!id) goto done;
    result = (*env)->CallStaticObjectMethod(env, bridge, factory, name, app, id, (jint)info->backend);
    if ((*env)->ExceptionCheck(env)) result = NULL;
done:
    return (*env)->PopLocalFrame(env, result);
}

static jobject sender_snapshot_object(JNIEnv *env, const NozzleSenderInfoSnapshot *info) {
    if ((*env)->PushLocalFrame(env, 8) < 0) return NULL;
    jobject result = NULL;
    jclass bridge = (*env)->FindClass(env, "nozzle/NozzleNative");
    if (!bridge) goto done;
    jmethodID factory = (*env)->GetStaticMethodID(env, bridge, "senderInfo", "([B[B[BI)Lnozzle/SenderInfo;");
    if (!factory) goto done;
    jbyteArray name = snapshot_utf8_bytes(env, info->name, sizeof(info->name));
    if (!name) goto done;
    jbyteArray app = snapshot_utf8_bytes(env, info->application_name, sizeof(info->application_name));
    if (!app) goto done;
    jbyteArray id = snapshot_utf8_bytes(env, info->id, sizeof(info->id));
    if (!id) goto done;
    result = (*env)->CallStaticObjectMethod(env, bridge, factory, name, app, id, (jint)info->backend);
    if ((*env)->ExceptionCheck(env)) result = NULL;
done:
    return (*env)->PopLocalFrame(env, result);
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)vm;
    (void)reserved;
    return JNI_VERSION_1_8;
}

// ========== Sender ==========

JNIEXPORT jlong JNICALL Java_nozzle_NozzleNative_senderCreate(
    JNIEnv *env, jclass cls,
    jstring name, jstring applicationName,
    jint ringBufferSize, jboolean allowFormatFallback
) {
    (void)cls;
    char *cname = utf8_from_string(env, name);
    if (!cname) return 0;
    char *capp = utf8_from_string(env, applicationName);
    if (!capp) {
        free(cname);
        return 0;
    }

    NozzleSenderDesc desc = {0};
    desc.name = cname;
    desc.application_name = capp;
    desc.ring_buffer_size = (uint32_t)ringBufferSize;
    desc.fallback_flags_valid = 1;
    desc.fallback_flags = allowFormatFallback ? NOZZLE_FALLBACK_SAFE_DEFAULTS : NOZZLE_FALLBACK_NONE;

    NozzleSender *sender = NULL;
    NozzleErrorCode rc = nozzle_sender_create(&desc, &sender);

    free(cname);
    free(capp);

    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return 0;
    }
    return (jlong)(intptr_t)sender;
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_senderDestroy(
    JNIEnv *env, jclass cls, jlong senderPtr
) {
    (void)cls;
    (void)env;
    nozzle_sender_destroy((NozzleSender *)(intptr_t)senderPtr);
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_senderPublishTexture(
    JNIEnv *env, jclass cls, jlong senderPtr, jlong texturePtr
) {
    (void)cls;
    NozzleErrorCode rc = nozzle_sender_publish_texture(
        (NozzleSender *)(intptr_t)senderPtr,
        (NozzleTexture *)(intptr_t)texturePtr
    );
    if (rc != NOZZLE_OK) throw_exception(env, rc);
}

JNIEXPORT jlong JNICALL Java_nozzle_NozzleNative_senderAcquireWritableFrame(
    JNIEnv *env, jclass cls, jlong senderPtr, jint width, jint height, jint format
) {
    (void)cls;
    NozzleFrame *frame = NULL;
    NozzleErrorCode rc = nozzle_sender_acquire_writable_frame(
        (NozzleSender *)(intptr_t)senderPtr,
        (uint32_t)width, (uint32_t)height,
        (NozzleTextureFormat)format, &frame
    );
    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return 0;
    }
    return (jlong)(intptr_t)frame;
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_senderCommitFrame(
    JNIEnv *env, jclass cls, jlong senderPtr, jlong framePtr
) {
    (void)cls;
    NozzleErrorCode rc = nozzle_sender_commit_frame(
        (NozzleSender *)(intptr_t)senderPtr,
        (NozzleFrame *)(intptr_t)framePtr
    );
    if (rc != NOZZLE_OK) throw_exception(env, rc);
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_senderPublishGLTexture(
    JNIEnv *env, jclass cls, jlong senderPtr,
    jint glTextureName, jint glTarget, jint width, jint height, jint format
) {
    (void)cls;
    NozzleErrorCode rc = nozzle_sender_publish_gl_texture(
        (NozzleSender *)(intptr_t)senderPtr,
        (uint32_t)glTextureName, (uint32_t)glTarget,
        (uint32_t)width, (uint32_t)height,
        (NozzleTextureFormat)format
    );
    if (rc != NOZZLE_OK) throw_exception(env, rc);
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_senderPublishNativeTexture(
    JNIEnv *env, jclass cls, jlong senderPtr, jlong nativeTexture,
    jint width, jint height, jint format
) {
    (void)cls;
    NozzleErrorCode rc = nozzle_sender_publish_native_texture(
        (NozzleSender *)(intptr_t)senderPtr,
        (void *)(intptr_t)nativeTexture,
        (uint32_t)width, (uint32_t)height,
        (NozzleTextureFormat)format
    );
    if (rc != NOZZLE_OK) throw_exception(env, rc);
}

JNIEXPORT jobject JNICALL Java_nozzle_NozzleNative_senderGetInfo(
    JNIEnv *env, jclass cls, jlong senderPtr
) {
    (void)cls;
    NozzleSenderInfoSnapshot info = {0};
    NozzleErrorCode rc = nozzle_sender_get_info_snapshot(
        (NozzleSender *)(intptr_t)senderPtr, &info
    );
    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return NULL;
    }

    return sender_snapshot_object(env, &info);
}

// ========== Receiver ==========

JNIEXPORT jlong JNICALL Java_nozzle_NozzleNative_receiverCreate(
    JNIEnv *env, jclass cls, jstring name, jstring applicationName, jint receiveMode
) {
    (void)cls;
    char *cname = utf8_from_string(env, name);
    if (!cname) return 0;
    char *capp = utf8_from_string(env, applicationName);
    if (!capp) {
        free(cname);
        return 0;
    }

    NozzleReceiverDesc desc = {0};
    desc.name = cname;
    desc.application_name = capp;
    desc.receive_mode = (NozzleReceiveMode)receiveMode;

    NozzleReceiver *receiver = NULL;
    NozzleErrorCode rc = nozzle_receiver_create(&desc, &receiver);

    free(cname);
    free(capp);

    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return 0;
    }
    return (jlong)(intptr_t)receiver;
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_receiverDestroy(
    JNIEnv *env, jclass cls, jlong receiverPtr
) {
    (void)cls;
    (void)env;
    nozzle_receiver_destroy((NozzleReceiver *)(intptr_t)receiverPtr);
}

JNIEXPORT jlong JNICALL Java_nozzle_NozzleNative_receiverAcquireFrame(
    JNIEnv *env, jclass cls, jlong receiverPtr, jlong timeoutMs
) {
    (void)cls;
    NozzleAcquireDesc desc = {0};
    desc.timeout_ms = (uint64_t)timeoutMs;

    NozzleFrame *frame = NULL;
    NozzleErrorCode rc = nozzle_receiver_acquire_frame(
        (NozzleReceiver *)(intptr_t)receiverPtr, &desc, &frame
    );
    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return 0;
    }
    return (jlong)(intptr_t)frame;
}

JNIEXPORT jobject JNICALL Java_nozzle_NozzleNative_receiverGetConnectionSnapshot(
    JNIEnv *env, jclass cls, jlong receiverPtr
) {
    (void)cls;
    NozzleConnectedSenderSnapshot info = {0};
    NozzleErrorCode rc = nozzle_receiver_get_connected_info_snapshot(
        (NozzleReceiver *)(intptr_t)receiverPtr, &info
    );
    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return NULL;
    }

    if ((*env)->PushLocalFrame(env, 8) < 0) return NULL;
    jobject result = NULL;
    jclass bridge = (*env)->FindClass(env, "nozzle/NozzleNative");
    if (!bridge) goto done;
    jmethodID factory = (*env)->GetStaticMethodID(env, bridge, "connectionSnapshot",
        "([B[B[BIIIIDJJZ)Lnozzle/ConnectionSnapshot;");
    if (!factory) goto done;
    jbyteArray name = snapshot_utf8_bytes(env, info.name, sizeof(info.name));
    if (!name) goto done;
    jbyteArray app = snapshot_utf8_bytes(env, info.application_name, sizeof(info.application_name));
    if (!app) goto done;
    jbyteArray id = snapshot_utf8_bytes(env, info.id, sizeof(info.id));
    if (!id) goto done;
    result = (*env)->CallStaticObjectMethod(env, bridge, factory, name, app, id,
        (jint)info.backend, (jint)info.width, (jint)info.height, (jint)info.format,
        (jdouble)info.estimated_fps, (jlong)info.frame_counter, (jlong)info.last_update_time_ns,
        (jboolean)(info.connected != 0));
    if ((*env)->ExceptionCheck(env)) result = NULL;
done:
    return (*env)->PopLocalFrame(env, result);
}

// ========== Frame ==========

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_frameRelease(
    JNIEnv *env, jclass cls, jlong framePtr
) {
    (void)cls;
    (void)env;
    nozzle_frame_release((NozzleFrame *)(intptr_t)framePtr);
}

JNIEXPORT jobject JNICALL Java_nozzle_NozzleNative_frameGetInfo(
    JNIEnv *env, jclass cls, jlong framePtr
) {
    (void)cls;
    NozzleFrameInfo info = {0};
    NozzleErrorCode rc = nozzle_frame_get_info(
        (NozzleFrame *)(intptr_t)framePtr, &info
    );
    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return NULL;
    }

    jclass infoCls = (*env)->FindClass(env, "nozzle/FrameInfo");
    jclass enumCls = (*env)->FindClass(env, "nozzle/TextureFormat");
    jmethodID fromValue = (*env)->GetStaticMethodID(env, enumCls, "fromValue", "(I)Lnozzle/TextureFormat;");
    jobject jformat = (*env)->CallStaticObjectMethod(env, enumCls, fromValue, (jint)info.format);

    jmethodID ctor = (*env)->GetMethodID(env, infoCls, "<init>",
        "(JJIIILnozzle/TextureFormat;I)V");

    jobject result = (*env)->NewObject(env, infoCls, ctor,
        (jlong)info.frame_index, (jlong)info.timestamp_ns,
        (jint)info.width, (jint)info.height,
        jformat, (jint)info.dropped_frame_count);

    (*env)->DeleteLocalRef(env, infoCls);
    (*env)->DeleteLocalRef(env, enumCls);
    (*env)->DeleteLocalRef(env, jformat);

    return result;
}

JNIEXPORT jobject JNICALL Java_nozzle_NozzleNative_frameGetResolvedFormat(
    JNIEnv *env, jclass cls, jlong framePtr
) {
    (void)cls;
    NozzleResolvedTextureFormat resolved = {0};
    NozzleErrorCode rc = nozzle_frame_get_resolved_format(
        (NozzleFrame *)(intptr_t)framePtr, &resolved
    );
    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return NULL;
    }

    jclass storageCls = (*env)->FindClass(env, "nozzle/TextureFormat");
    jclass sourceCls = (*env)->FindClass(env, "nozzle/FormatSource");
    jclass backendCls = (*env)->FindClass(env, "nozzle/BackendType");
    jclass kindCls = (*env)->FindClass(env, "nozzle/NativeFormatKind");

    jmethodID fmtFrom = (*env)->GetStaticMethodID(env, storageCls, "fromValue", "(I)Lnozzle/TextureFormat;");
    jmethodID srcFrom = (*env)->GetStaticMethodID(env, sourceCls, "fromValue", "(I)Lnozzle/FormatSource;");
    jmethodID bkdFrom = (*env)->GetStaticMethodID(env, backendCls, "fromValue", "(I)Lnozzle/BackendType;");
    jmethodID kndFrom = (*env)->GetStaticMethodID(env, kindCls, "fromValue", "(I)Lnozzle/NativeFormatKind;");

    jobject jstorage = (*env)->CallStaticObjectMethod(env, storageCls, fmtFrom, (jint)resolved.storage_format);
    jobject jsemantic = (*env)->CallStaticObjectMethod(env, storageCls, fmtFrom, (jint)resolved.semantic_format);
    jobject jsource = (*env)->CallStaticObjectMethod(env, sourceCls, srcFrom, (jint)resolved.format_source);
    jobject jbackend = (*env)->CallStaticObjectMethod(env, backendCls, bkdFrom, (jint)resolved.native_backend);
    jobject jkind = (*env)->CallStaticObjectMethod(env, kindCls, kndFrom, (jint)resolved.native_kind);

    jclass resultCls = (*env)->FindClass(env, "nozzle/ResolvedTextureFormat");
    jmethodID ctor = (*env)->GetMethodID(env, resultCls, "<init>",
        "(Lnozzle/TextureFormat;Lnozzle/TextureFormat;Lnozzle/FormatSource;"
        "Lnozzle/BackendType;Lnozzle/NativeFormatKind;"
        "IIIIIII)V");

    jobject result = (*env)->NewObject(env, resultCls, ctor,
        jstorage, jsemantic, jsource, jbackend, jkind,
        (jint)resolved.native_value, (jint)resolved.channel_order,
        (jint)resolved.component_type, (jint)resolved.component_bits,
        (jint)resolved.channel_count, (jint)resolved.bytes_per_pixel);

    (*env)->DeleteLocalRef(env, storageCls);
    (*env)->DeleteLocalRef(env, sourceCls);
    (*env)->DeleteLocalRef(env, backendCls);
    (*env)->DeleteLocalRef(env, kindCls);
    (*env)->DeleteLocalRef(env, jstorage);
    (*env)->DeleteLocalRef(env, jsemantic);
    (*env)->DeleteLocalRef(env, jsource);
    (*env)->DeleteLocalRef(env, jbackend);
    (*env)->DeleteLocalRef(env, jkind);
    (*env)->DeleteLocalRef(env, resultCls);

    return result;
}

static jobject create_mapped_pixels(JNIEnv *env, NozzleMappedPixels *mapped,
                                     long framePtr, int writable) {
    int64_t total_size = (int64_t)mapped->height * (int64_t)mapped->row_stride_bytes;
    jobject bb = (*env)->NewDirectByteBuffer(env, mapped->data, (jlong)total_size);
    if (!bb) return NULL;

    jclass fmtCls = (*env)->FindClass(env, "nozzle/TextureFormat");
    jclass origCls = (*env)->FindClass(env, "nozzle/TextureOrigin");
    jmethodID fmtFrom = (*env)->GetStaticMethodID(env, fmtCls, "fromValue", "(I)Lnozzle/TextureFormat;");
    jmethodID origFrom = (*env)->GetStaticMethodID(env, origCls, "fromValue", "(I)Lnozzle/TextureOrigin;");
    jobject jformat = (*env)->CallStaticObjectMethod(env, fmtCls, fmtFrom, (jint)mapped->format);
    jobject jorigin = (*env)->CallStaticObjectMethod(env, origCls, origFrom, (jint)mapped->origin);

    jclass mpCls = (*env)->FindClass(env, "nozzle/MappedPixels");
    jmethodID ctor = (*env)->GetMethodID(env, mpCls, "<init>",
        "(Ljava/nio/ByteBuffer;IIIILnozzle/TextureOrigin;JZ)V");

    jobject result = (*env)->NewObject(env, mpCls, ctor,
        bb, (jint)mapped->row_stride_bytes,
        (jint)mapped->width, (jint)mapped->height,
        jformat, jorigin, (jlong)framePtr, (jboolean)writable);

    (*env)->DeleteLocalRef(env, bb);
    (*env)->DeleteLocalRef(env, fmtCls);
    (*env)->DeleteLocalRef(env, origCls);
    (*env)->DeleteLocalRef(env, jformat);
    (*env)->DeleteLocalRef(env, jorigin);
    (*env)->DeleteLocalRef(env, mpCls);

    return result;
}

JNIEXPORT jobject JNICALL Java_nozzle_NozzleNative_frameLockPixels(
    JNIEnv *env, jclass cls, jlong framePtr, jint origin
) {
    (void)cls;
    NozzleMappedPixels mapped = {0};
    NozzleErrorCode rc = nozzle_frame_lock_pixels_with_origin(
        (NozzleFrame *)(intptr_t)framePtr,
        (NozzleTextureOrigin)origin, &mapped
    );
    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return NULL;
    }
    return create_mapped_pixels(env, &mapped, framePtr, 0);
}

JNIEXPORT jobject JNICALL Java_nozzle_NozzleNative_frameLockWritablePixels(
    JNIEnv *env, jclass cls, jlong framePtr, jint origin
) {
    (void)cls;
    NozzleMappedPixels mapped = {0};
    NozzleErrorCode rc = nozzle_frame_lock_writable_pixels_with_origin(
        (NozzleFrame *)(intptr_t)framePtr,
        (NozzleTextureOrigin)origin, &mapped
    );
    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return NULL;
    }
    return create_mapped_pixels(env, &mapped, framePtr, 1);
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_unlockPixels(
    JNIEnv *env, jclass cls, jlong framePtr
) {
    (void)cls;
    (void)env;
    nozzle_frame_unlock_pixels((NozzleFrame *)(intptr_t)framePtr);
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_unlockWritablePixels(
    JNIEnv *env, jclass cls, jlong framePtr
) {
    (void)cls;
    (void)env;
    nozzle_frame_unlock_writable_pixels((NozzleFrame *)(intptr_t)framePtr);
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_frameCopyToGLTexture(
    JNIEnv *env, jclass cls, jlong framePtr,
    jint glTextureName, jint glTarget, jint width, jint height, jint format
) {
    (void)cls;
    NozzleErrorCode rc = nozzle_frame_copy_to_gl_texture(
        (NozzleFrame *)(intptr_t)framePtr,
        (uint32_t)glTextureName, (uint32_t)glTarget,
        (uint32_t)width, (uint32_t)height,
        (NozzleTextureFormat)format
    );
    if (rc != NOZZLE_OK) throw_exception(env, rc);
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_frameCopyToNativeTexture(
    JNIEnv *env, jclass cls, jlong framePtr, jlong nativeTexture,
    jint width, jint height, jint format
) {
    (void)cls;
    NozzleErrorCode rc = nozzle_frame_copy_to_native_texture(
        (NozzleFrame *)(intptr_t)framePtr,
        (void *)(intptr_t)nativeTexture,
        (uint32_t)width, (uint32_t)height,
        (NozzleTextureFormat)format
    );
    if (rc != NOZZLE_OK) throw_exception(env, rc);
}

// ========== Device ==========

JNIEXPORT jlong JNICALL Java_nozzle_NozzleNative_deviceGetDefault(
    JNIEnv *env, jclass cls
) {
    (void)cls;
    NozzleDevice *device = NULL;
    NozzleErrorCode rc = nozzle_device_get_default(&device);
    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return 0;
    }
    return (jlong)(intptr_t)device;
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_deviceDestroy(
    JNIEnv *env, jclass cls, jlong devicePtr
) {
    (void)cls;
    (void)env;
    nozzle_device_destroy((NozzleDevice *)(intptr_t)devicePtr);
}

// ========== Texture ==========

JNIEXPORT jlong JNICALL Java_nozzle_NozzleNative_textureWrap(
    JNIEnv *env, jclass cls, jlong nativeTexture,
    jint width, jint height, jint format, jint backend
) {
    (void)cls;
    NozzleTextureWrapDesc desc = {0};
    desc.native_texture = (void *)(intptr_t)nativeTexture;
    desc.width = (uint32_t)width;
    desc.height = (uint32_t)height;
    desc.format = (NozzleTextureFormat)format;
    desc.backend = (NozzleBackendType)backend;

    NozzleTexture *texture = NULL;
    NozzleErrorCode rc = nozzle_texture_wrap(&desc, &texture);
    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return 0;
    }
    return (jlong)(intptr_t)texture;
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_textureDestroy(
    JNIEnv *env, jclass cls, jlong texturePtr
) {
    (void)cls;
    (void)env;
    nozzle_texture_destroy((NozzleTexture *)(intptr_t)texturePtr);
}

// ========== Discovery ==========

JNIEXPORT jobjectArray JNICALL Java_nozzle_NozzleNative_enumerateSenders(
    JNIEnv *env, jclass cls
) {
    (void)cls;
    NozzleSenderInfoArray array = {0};
    NozzleErrorCode rc = nozzle_enumerate_senders(&array);
    if (rc != NOZZLE_OK) {
        throw_exception(env, rc);
        return NULL;
    }

    jclass infoCls = (*env)->FindClass(env, "nozzle/SenderInfo");
    jobjectArray result = NULL;
    if (!infoCls) goto done;
    result = (*env)->NewObjectArray(env, (jsize)array.count, infoCls, NULL);
    if (!result) goto done;
    for (uint32_t i = 0; i < array.count; i++) {
        jobject obj = sender_info_object(env, &array.items[i]);
        if (!obj) break;
        (*env)->SetObjectArrayElement(env, result, (jsize)i, obj);
        (*env)->DeleteLocalRef(env, obj);
        if ((*env)->ExceptionCheck(env)) break;
    }
done:
    nozzle_free_sender_info_array(&array);
    if (infoCls) (*env)->DeleteLocalRef(env, infoCls);
    return (*env)->ExceptionCheck(env) ? NULL : result;
}

// ========== Pixel Utilities ==========

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_swizzleChannels(
    JNIEnv *env, jclass cls,
    jobject src, jobject dst, jint width, jint height,
    jint srcRowBytes, jint dstRowBytes, jint format, jbyteArray permuteMap
) {
    (void)cls;
    void *srcPtr = (*env)->GetDirectBufferAddress(env, src);
    void *dstPtr = (*env)->GetDirectBufferAddress(env, dst);
    jbyte *perm = (*env)->GetByteArrayElements(env, permuteMap, NULL);

    NozzleErrorCode rc = nozzle_swizzle_channels(
        srcPtr, dstPtr,
        (uint32_t)width, (uint32_t)height,
        (uint32_t)srcRowBytes, (uint32_t)dstRowBytes,
        (NozzleTextureFormat)format, (const uint8_t *)perm
    );

    (*env)->ReleaseByteArrayElements(env, permuteMap, perm, JNI_ABORT);

    if (rc != NOZZLE_OK) throw_exception(env, rc);
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_widenUint16ToUint32(
    JNIEnv *env, jclass cls,
    jobject src, jobject dst, jint width, jint height,
    jint srcRowBytes, jint dstRowBytes, jint channels
) {
    (void)cls;
    void *srcPtr = (*env)->GetDirectBufferAddress(env, src);
    void *dstPtr = (*env)->GetDirectBufferAddress(env, dst);

    NozzleErrorCode rc = nozzle_widen_uint16_to_uint32(
        srcPtr, dstPtr,
        (uint32_t)width, (uint32_t)height,
        (uint32_t)srcRowBytes, (uint32_t)dstRowBytes,
        (uint32_t)channels
    );
    if (rc != NOZZLE_OK) throw_exception(env, rc);
}

JNIEXPORT void JNICALL Java_nozzle_NozzleNative_convertUint32ToFloat32(
    JNIEnv *env, jclass cls,
    jobject src, jobject dst, jint width, jint height,
    jint srcRowBytes, jint dstRowBytes, jint channels
) {
    (void)cls;
    void *srcPtr = (*env)->GetDirectBufferAddress(env, src);
    void *dstPtr = (*env)->GetDirectBufferAddress(env, dst);

    NozzleErrorCode rc = nozzle_convert_uint32_to_float32(
        srcPtr, dstPtr,
        (uint32_t)width, (uint32_t)height,
        (uint32_t)srcRowBytes, (uint32_t)dstRowBytes,
        (uint32_t)channels
    );
    if (rc != NOZZLE_OK) throw_exception(env, rc);
}
