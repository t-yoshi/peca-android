#ifndef _JAVA_THREAD_H_
#define _JAVA_THREAD_H_
// ------------------------------------------------
// File : thread.h
// Date: 14-Mar-2013
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



/*�l�C�e�B�u�X���b�h�J�n���ɌĂсAJVM�ɃA�^�b�`���܂��B*/
#define BEGIN_THREAD_PROC do { \
	logThread(__FUNCTION__); \
	setupNativeThread(); \
} while (0)

/**
 * �f�o�b�O�p�Ɋ֐����ƃX���b�hID�����O�Ɏc���B
 * */
void logThread(const char* funcName);

/**
 * �l�C�e�B�u�̃X���b�h��JVM�ɃA�^�b�`���܂��B
 * */
jboolean setupNativeThread();

/**
 * �X���b�h�I����Ƀf�^�b�`�����s����f�X�g���N�^��o�^���܂��B
 * JVM_OnLoad�֐�����ĂԁB
 * */
jboolean registerThreadShutdownFunc(JavaVM *vm);


#endif
