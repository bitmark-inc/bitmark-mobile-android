#include <jni.h>

const char *BITMARK_API_KEY = "bitmark-api-key-to-be-filled";
const char *INTERCOM_API_KEY = "intercom-api-key-to-be-filled";

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bitmark_registry_keymanagement_ApiKeyManager_getBitmarkApiKey(JNIEnv *env,
                                                                       jobject instance) {
    return env->NewStringUTF(BITMARK_API_KEY);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_bitmark_registry_keymanagement_ApiKeyManager_getIntercomApiKey(JNIEnv *env,
                                                                        jobject instance) {

    return env->NewStringUTF(INTERCOM_API_KEY);
}