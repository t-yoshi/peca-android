// ------------------------------------------------
// File : PeerCastService.cpp
// Date: 25-Apr-2019
// Author: (c) 2013-2019 T Yoshizawa
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

#include <unistd.h>
#include "unix/usys.h"

#include "peercast.h"
#include "stats.h"

#include "JniHelper.h"
#include "nativehelper/scoped_utf_chars.h"
#include "nativehelper/scoped_local_frame.h"

#define TAG "PeCaNt"

using namespace std;

/**
 * org.peercast.core.PeerCastServiceのクラス、メソッドIDをキャッシュする。
 * */
static class PeerCastServiceClassCache {
    jclass _clazz;//org.peercast.core.PeerCastService
    jmethodID _notifyMessage; //void notifyMessage(int, String)
    jmethodID _notifyChannel; //void notifyChannel(int, String, String)
public:
    void init(JNIEnv *env, jclass clazz_) {
        _clazz = (jclass) env->NewGlobalRef(clazz_);

        _notifyMessage = CHECK_NOT_NULL(
                env->GetMethodID(_clazz, "notifyMessage",
                                 "(ILjava/lang/String;)V")
        );

        _notifyChannel = CHECK_NOT_NULL(
                env->GetMethodID(_clazz, "notifyChannel",
                                 "(ILjava/lang/String;Ljava/lang/String;)V")
        );
    }

    void APICALL notifyMessage(JNIEnv *env, jobject this_,
                               ServMgr::NOTIFY_TYPE tNotify,
                               const char *message) {
        ScopedLocalFrame frame(env);
        env->CallVoidMethod(this_, _notifyMessage, tNotify, newJString(env, message));
    }

    typedef enum {
        NOTIFY_CHANNEL_START = 0,
        NOTIFY_CHANNEL_UPDATE = 1,
        NOTIFY_CHANNEL_STOP = 2
    } NotifyType;

    void notifyChannel(JNIEnv *env, jobject this_, NotifyType notifyType,
                       const string &chId, const string &jsonChannelInfo) {
        ScopedLocalFrame frame(env);
        env->CallVoidMethod(this_, _notifyChannel,
                            notifyType,
                            newJString(env, chId.data()),
                            newJString(env, jsonChannelInfo.data())
        );
    }
} classCache;


class ASys : public USys {
public:
    void exit() override {
        ::killMyself();
    }

    void executeFile(const char *f) override {
        LOGE("%s is Not Implemented", __func__);
    }

    void setThreadName(const char *name) override {
        ::attachPosixThread(name);
    }
};


#include "jrpc.h"

class AndroidPeercastApp : public PeercastApplication {
    jobject _instance; //Instance of PeerCastService
    const string _iniPath;
    const string _resourceDirPath;
public:
    AndroidPeercastApp(jobject jthis, const string filesDirPath) :
            _iniPath(filesDirPath + "/peercast.ini"),
            _resourceDirPath(filesDirPath + "/") {
        JNIEnv *env = ::getJniEnv();
        _instance = env->NewGlobalRef(jthis);
        LOGD("IniFilePath=%s, ResourceDir=%s", _iniPath.data(), _resourceDirPath.data());
    }

    ~AndroidPeercastApp() override {
        JNIEnv *env = ::getJniEnv();
        env->DeleteGlobalRef(_instance);
    }

    const char *APICALL getIniFilename() final {
        return _iniPath.data();
    }

    const char *APICALL getPath() final {
        return _resourceDirPath.data();
    }

    const char *APICALL getClientTypeOS() final {
        return "Linux";
    }

    void APICALL printLog(LogBuffer::TYPE t, const char *str) final {
        static int const priorities[] = {
                0, //T_NONE=0
                ANDROID_LOG_VERBOSE, //	T_TRACE=1
                ANDROID_LOG_DEBUG, //	T_DEBUG=2
                ANDROID_LOG_INFO,  //	T_INFO=3
                ANDROID_LOG_WARN,  //	T_WARN=4
                ANDROID_LOG_ERROR, //	T_ERROR=5
                ANDROID_LOG_FATAL, //	T_FATAL=6
                0, //	T_OFF=7 未使用?
        };

        if (priorities[t] == 0)
            return;

        char tag[24];//tagは23文字まで
        ::snprintf(tag, sizeof(tag), "%s[%s]", TAG, LogBuffer::getTypeStr(t));
        ::__android_log_print(priorities[t], tag, "%s", str);
    }

    /**
     * notifyMessage(int, String)
     * */
    void APICALL notifyMessage(ServMgr::NOTIFY_TYPE tNotify, const char *message) override {
        classCache.notifyMessage(
                ::getJniEnv(), _instance, tNotify, message
        );
    }

    /*
    *  channelStart(ChanInfo *)
    *  channelUpdate(ChanInfo *)
    *  channelStop(ChanInfo *)
    *    -> (Java) notifyChannel(int, String)
    */
    void APICALL channelStart(ChanInfo *info) final {
        notifyChannel(PeerCastServiceClassCache::NOTIFY_CHANNEL_START, info);
    }

    void APICALL channelUpdate(ChanInfo *info) final {
        notifyChannel(PeerCastServiceClassCache::NOTIFY_CHANNEL_UPDATE, info);
    }

    void APICALL channelStop(ChanInfo *info) final {
        notifyChannel(PeerCastServiceClassCache::NOTIFY_CHANNEL_STOP, info);
    }

private:
    void notifyChannel(PeerCastServiceClassCache::NotifyType notifyType, ChanInfo *info) {
        JNIEnv *env = ::getJniEnv();
        JrpcApi api;
        classCache.notifyChannel(env,
                                 _instance,
                                 notifyType,
                                 info->id.str(),
                                 api.to_json(*info).dump());
    }
};

class AndroidPeercastInst : public PeercastInstance {
public:
    Sys *APICALL createSys() final {
        return new ASys;
    }

    ~AndroidPeercastInst() {
        delete servMgr;
        servMgr = nullptr;
        delete chanMgr;
        chanMgr = nullptr;
        delete sys;
        sys = nullptr;
    }
};


extern "C" JNIEXPORT void JNICALL
Java_org_peercast_core_PeerCastService_nativeStart(JNIEnv *env, jobject jthis,
                                                   jstring filesDirPath) {

    if (peercastApp) {
        LOGE("PeerCast has already been running!");
        return;
    }

    peercastApp = new AndroidPeercastApp(jthis,
                                         ScopedUtfChars(env, filesDirPath).c_str()
    );
    peercastInst = new AndroidPeercastInst();

    peercastInst->init();
}

extern "C" JNIEXPORT void JNICALL
Java_org_peercast_core_PeerCastService_nativeQuit(JNIEnv *env, jobject jthis) {

    if (peercastInst != nullptr) {
        peercastInst->saveSettings();
        peercastInst->quit();
        LOGD("peercastInst->quit() OK.");
        for (int i = 0; getRemainingAttachedThreads() > 0 && i < 3000; i++) {
            //sleepしているスレッドがあれば待つ
            ::usleep(1000);
        }
    }

    delete peercastInst;
    peercastInst = nullptr;
    delete peercastApp;
    peercastApp = nullptr;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_peercast_core_PeerCastService_nativeSetPort(JNIEnv *env, jobject thiz, jint port) {
    if (servMgr && peercastInst && servMgr->serverHost.port != port) {
        if (port >= 1025 && port <= 65532) {
            LOGI("Port's changing: %d -> %d", servMgr->serverHost.port, port);
            servMgr->serverHost.port = (u_short) port;
            servMgr->restartServer = true;
            peercastInst->saveSettings();
            peercast::notifyMessage(ServMgr::NT_PEERCAST, "設定を保存しました。");
            //peercastApp->updateSettings();
        } else {
            LOGE("Invalid port: %d", port);
        }
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_org_peercast_core_PeerCastService_nativeGetPort(JNIEnv *env, jobject thiz) {
    return servMgr ? servMgr->serverHost.port : 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_peercast_core_PeerCastService_nativeClearCache(JNIEnv *env, jobject thiz, jint cmd) {
#define CLEAR_HOST_CACHE    1
#define CLEAR_HIT_LISTS_CACHE   2
#define CLEAR_CHANNELS_CACHE    4

    LOGI("nativeClearCache: cmd=%d", cmd);

    if (servMgr && cmd & CLEAR_HOST_CACHE)
        servMgr->clearHostCache(ServHost::T_SERVENT);
    if (chanMgr && cmd & CLEAR_HIT_LISTS_CACHE)
        chanMgr->clearHitLists();
    if (chanMgr && cmd & CLEAR_CHANNELS_CACHE)
        chanMgr->closeIdles();
}

extern "C" JNIEXPORT void JNICALL Java_org_peercast_core_PeerCastService_nativeClassInit(
        JNIEnv *env, jclass jclz) {
    classCache.init(env, jclz);
}

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    return initJniHelper(vm) == JNI_OK ? JNI_VERSION_1_6 : -1;
}

