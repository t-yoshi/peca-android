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

#pragma once

#include <jni.h>
#include <android/log.h>
#include <string>

#include "nativehelper/nativehelper_utils.h"

#ifdef ADEBUG //define at CMakeLists
#warning "This is debug build"
#define LOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#else
#define LOGV(...)   ((void)0)
#define LOGD(...)   ((void)0)
#endif

#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

template<class T>
inline T *_check_not_null(T *p, const char *tag, const char *varName, const char *funcName) {
    if (p == nullptr)
        ::__android_log_assert(nullptr, tag, "%s is null (%s)", varName, funcName); \
  return p;
}

#define CHECK_NOT_NULL(ptr) _check_not_null(ptr, TAG, #ptr, __func__)
#define A_ASSERT(cond) do { \
  if (!(cond)) \
    ::__android_log_assert("Assertion failed: " #cond, TAG, nullptr); \
} while(0)


JNIEnv *getJniEnv();

//ネイティブスレッドをvmに関連付ける
void attachPosixThread(const char *name = nullptr);

//現在、関連付けられている数
int getRemainingAttachedThreads();

//自分をklllする
void killMyself();

jstring newJString(JNIEnv *env, const char *s, const char *encoding = "utf8");

std::string strprintf(const char *format, ...)  __attribute__((__format__(printf, 1, 2)));

jint initJniHelper(JavaVM *vm);

