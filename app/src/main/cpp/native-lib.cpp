#include <jni.h>
#include "progress.h"

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_dgoal_NativeBridge_calculateProgress(
        JNIEnv* env,
        jobject obj,
        jint completed,
        jint total) {

    return calculateProgress(completed, total);
}