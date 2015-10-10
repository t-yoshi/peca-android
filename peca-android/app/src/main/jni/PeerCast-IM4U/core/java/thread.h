#ifndef _JAVA_THREAD_H_
#define _JAVA_THREAD_H_
// ------------------------------------------------
// File : thread.h
// Date: 14-Jul-2015
// Author: T. Yoshizawa
// Desc:
//�@�쐬���ꂽ�l�C�e�B�u�X���b�h��JVM�ɕR�Â����܂��B
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
���̃X���b�h���X�R�[�v���ɂ�����JVM�ɃA�^�b�`���܂��B

(�X���b�h�֐��̖`���ɒu��)
*/
#define BEGIN_THREAD_PROC	ScopedThreadAttacher __thread__attacher(__FUNCTION__);



/**
 * �X���b�h�I����Ƀf�^�b�`�����s����f�X�g���N�^��o�^���܂��B
 * JVM_OnLoad�֐�����ĂԁB
 * */
jboolean registerThreadShutdownFunc(JavaVM *vm);


#endif
