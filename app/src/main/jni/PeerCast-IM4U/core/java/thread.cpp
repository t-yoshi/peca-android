// ------------------------------------------------
// File : thread.cpp
// Date: 14-Mar-2013
// Author: T. Yoshizawa
// Desc:
//　作成したネイティブスレッドをJVMに紐付ける。
//
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

#include "thread.h"

#ifdef _UNIX
#include <sys/types.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include <pthread.h>

#ifndef ANDROID
//ANDROIDではgettid()が使える.
#ifdef __LINUX__
//FIXME: 試してない
static int gettid() {
	return syscall(SYS_gettid);
}
#else
static int gettid() {
	return pthread_self();
}
#endif //__LINUX__
#endif //!ANDROID
#ifdef NDEBUG
#define LOG_THREAD(...) do {} while(0)
#else

//#define LOG_THREAD LOG
#include <android/log.h>
#define LOG_THREAD(...) __android_log_print(ANDROID_LOG_DEBUG, "PeCaTh", __VA_ARGS__)
#endif

static JavaVM *sVM;

ScopedThreadAttacher::ScopedThreadAttacher(const char* funcName) : mEnv(NULL) {
	LOG_THREAD("%s: TID(%d) thread start...", funcName, gettid());
	const char *errmsg;

	jint ret = sVM->GetEnv((void**) &mEnv, JNI_VERSION_1_6);
	if (ret != JNI_EDETACHED) {
		errmsg = "VM->GetEnv()!=JNI_EDETACHED";
		goto FAIL;
	}
	ret = sVM->AttachCurrentThread(&mEnv, NULL);
	if (ret != JNI_OK) {
		errmsg = "VM->AttachCurrentThread()";
		goto FAIL;
	}
	LOG_THREAD("TID(%d) [OK] VM->AttachCurrentThread()", gettid());
	return;

FAIL:
	LOG_THREAD("TID(%d) [FAIL] %s", gettid(), errmsg);
	
}

ScopedThreadAttacher::~ScopedThreadAttacher() {
	if (mEnv) {
		sVM->DetachCurrentThread();
		LOG_THREAD("TID(%d) thread finish. detached... ", gettid());
	}
}


jboolean registerThreadShutdownFunc(JavaVM *vm) {
	sVM = vm;
	return JNI_TRUE;
}

#else
#error "not implemented"

#endif //_UNIX
