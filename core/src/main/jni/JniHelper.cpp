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
#include <pthread.h>
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

jstring newJString(JNIEnv *env, const char *s, const char *encoding) {
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

JNIEnv *getJniEnv() {
    //必ずJAVAアタッチ済スレッドから呼ばれること。
    JNIEnv *env;
    if (javaVM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        ::__android_log_assert(nullptr, TAG, "Native thread (tid=%d) is not attached yet.",
                               gettid());
    }
    return env;
}

static std::atomic<int> nAttached;

struct ThreadSpecific {
    ThreadSpecific() : _attached(false) {
        JNIEnv *env;
        jint r = javaVM->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
        if (r == JNI_OK) {
            LOGW("Already attached: tid=%d", gettid());
            return;
        }
        if (r != JNI_EDETACHED) {
            LOGE("GetEnv failed: tid=%d", gettid());
            return;
        }
        r = javaVM->AttachCurrentThread(&env, nullptr);
        if (r == JNI_OK) {
            _attached = true;
            ++nAttached;
            LOGV("AttachCurrentThread success: tid=%d", gettid());
        } else {
            LOGE("AttachCurrentThread failed: tid=%d", gettid());
        }
    }

    ~ThreadSpecific() {
        if (_attached) {
            javaVM->DetachCurrentThread();
            int remaining = --nAttached;
            LOGV("DetachCurrentThread: tid=%d (remaining=%d)", gettid(), remaining);
        }
    }

private:
    bool _attached;
    DISALLOW_COPY_AND_ASSIGN(ThreadSpecific);
};

static class {
    pthread_key_t _key;

    static void destroyer(void *p) {
        delete static_cast<ThreadSpecific *>(p);
    }

public:
    bool init() {
        return ::pthread_key_create(&_key, destroyer) == 0;
    }

    void attach() const {
        if (::pthread_getspecific(_key) == nullptr) {
            ::pthread_setspecific(_key, new ThreadSpecific);
        }
    }
} threadAttacher;

void attachPosixThread(const char *name) {
    threadAttacher.attach();
    if (name != nullptr && ::strlen(name) < 16) {
        LOGV("Set thread name: `%s` tid=%d", name, gettid());
        ::pthread_setname_np(
                ::pthread_self(), name
        );
    }
}

int getRemainingAttachedThreads() {
    return nAttached;
}

void killMyself() {
    auto env = getJniEnv();
    auto clz = ScopedLocalRef<jclass>(env,
                                      CHECK_NOT_NULL(
                                              env->FindClass("android/os/Process")
                                      )
    );
    jmethodID mid = CHECK_NOT_NULL(
            env->GetStaticMethodID(clz.get(), "killProcess", "(I)V")
    );
    env->CallStaticVoidMethod(clz.get(), mid, getpid());
}

static void initJString(JNIEnv *env) {
    ScopedLocalRef<jclass> clz(env, CHECK_NOT_NULL(env->FindClass("java/lang/String")));
    clzString = (jclass) env->NewGlobalRef(clz.get());
    midStringInitBS = CHECK_NOT_NULL(
            env->GetMethodID(clz.get(), "<init>", "([BLjava/lang/String;)V")
    );
}

string strprintf(const char *format, ...) {
    va_list ap;
    const int init_buf_size = 1024;
    auto buf = unique_ptr<char[]>(new char[init_buf_size]);

    va_start(ap, format);
    int r = ::vsnprintf(buf.get(), init_buf_size, format, ap);
    va_end(ap);
    if (r < 0)
        ::__android_log_assert(nullptr, TAG, "printf format error: `%s`", format);
    if (r >= init_buf_size - 1) {
        buf.reset(new char[r]);
        va_start(ap, format);
        ::vsnprintf(buf.get(), r, format, ap);
        va_end(ap);
    }
    return buf.get();
}

jint initJniHelper(JavaVM *vm) {
    if (!threadAttacher.init())
        return JNI_ERR;

    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    javaVM = vm;

    initJString(env);

    return JNI_OK;
}
