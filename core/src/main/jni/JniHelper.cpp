// ------------------------------------------------
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// ------------------------------------------------

#include "JniHelper.h"
#include "nativehelper/scoped_local_ref.h"

#include <atomic>
#include <cstdio>
#include <cstring>
#include <cstdarg>
#include <unistd.h>

using namespace std;

#define TAG "JniHelper"

static JavaVM *javaVM;
static jclass clzString;
static jmethodID midStringInitBS;

jstring NewJString(JNIEnv *env, const char *s, const char *encoding) {
    CHECK_NOT_NULL(clzString);
    CHECK_NOT_NULL(midStringInitBS);

    if (s == nullptr)
        return nullptr;

    const jsize len = ::strlen(s);
    ScopedLocalRef<jbyteArray> jBuf(env, env->NewByteArray(len));
    ScopedLocalRef<jstring> jEncoding(env, env->NewStringUTF(encoding));

    if (jBuf == nullptr || jEncoding == nullptr)
        return nullptr;

    env->SetByteArrayRegion(jBuf.get(), 0, len, reinterpret_cast<const jbyte *>(s));

    return (jstring) env->NewObject(
            clzString, midStringInitBS,
            jBuf.get(), jEncoding.get()
    );
}

JNIEnv *GetJNIEnv() {
    //必ずJAVAアタッチ済スレッドから呼ばれること。
    JNIEnv *env;
    if (javaVM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        ::__android_log_assert(nullptr, TAG, "GetEnv()!=JNI_OK");
    }
    return env;
}

static std::atomic<int> nLiveThreads;

ScopedThreadAttach::ScopedThreadAttach() : mEnv(nullptr) {
    //LOGD("Thread start. tid=%d", gettid());
    CHECK_NOT_NULL(javaVM);
    jint ret = javaVM->GetEnv((void **) &mEnv, JNI_VERSION_1_6);
    if (ret != JNI_EDETACHED) {
        LOGE("Error: VM->GetEnv()!=JNI_EDETACHED, tid=%d", gettid());
        return;
    }
    ret = javaVM->AttachCurrentThread(&mEnv, nullptr);
    if (ret != JNI_OK) {
        LOGE("Error: VM->AttachCurrentThread(), tid=%d", gettid());
        return;
    }
    ++nLiveThreads;
    //LOGD("OK: VM->AttachCurrentThread(), tid=%d", gettid());
}

ScopedThreadAttach::~ScopedThreadAttach() {
    if (mEnv != nullptr) {
        --nLiveThreads;
        javaVM->DetachCurrentThread();
        //LOGD("Detached thread. tid=%d", gettid());
    }
}

int ScopedThreadAttach::numLiveThreads() {
    return nLiveThreads;
}

static void InitJString(JNIEnv *env) {
    ScopedLocalRef<jclass> clz(env, CHECK_NOT_NULL(env->FindClass("java/lang/String")));
    clzString = (jclass) env->NewGlobalRef(clz.get());
    midStringInitBS = CHECK_NOT_NULL(
            env->GetMethodID(clz.get(), "<init>", "([BLjava/lang/String;)V")
    );
}

string strprintf(const char *format, ...) {
    va_list ap;
    int bufSize = 1024;
    auto buf = unique_ptr<char[]>();
    for (;;) {
        buf.reset(new char[bufSize]);
        va_start(ap, format);
        int r = ::vsnprintf(buf.get(), bufSize, format, ap);
        va_end(ap);
        if (r < 0)
            ::__android_log_assert(nullptr, TAG, "printf format error: `%s`", format);
        if (r < bufSize - 1)
            break;
        bufSize = r;
    }
    return buf.get();
}

jint InitJniHelper(JavaVM *vm) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    javaVM = vm;

    InitJString(env);

    return JNI_OK;
}
