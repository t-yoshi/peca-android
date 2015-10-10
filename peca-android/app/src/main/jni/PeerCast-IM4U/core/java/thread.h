#ifndef _JAVA_THREAD_H_
#define _JAVA_THREAD_H_
// ------------------------------------------------
// File : thread.h
// Date: 14-Jul-2015
// Author: T. Yoshizawa
// Desc:
//　作成されたネイティブスレッドをJVMに紐づけします。
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

#include <jni.h>
#include "sys.h"


class ScopedThreadAttacher {
	JNIEnv *mEnv;
public:
	ScopedThreadAttacher(const char* funcName);
	~ScopedThreadAttacher();
};


/*
このスレッドをスコープ内においてJVMにアタッチします。

(スレッド関数の冒頭に置く)
*/
#define BEGIN_THREAD_PROC	ScopedThreadAttacher __thread__attacher(__FUNCTION__);



/**
 * スレッド終了後にデタッチを実行するデストラクタを登録します。
 * JVM_OnLoad関数から呼ぶ。
 * */
jboolean registerThreadShutdownFunc(JavaVM *vm);


#endif
