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

#include <android/log.h>
#include <jni.h>

static JavaVM *sJVM;

#define TAG "PeCaNt"

#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL, TAG, __VA_ARGS__)

static void _check_ptr(void *ptr, const char *funcName, const char *name) {
    if (ptr == nullptr) {
        LOGF("[%s] %s is nullptr", funcName, name);
        abort(); /*__noreturn*/
    }
}

#define CHECK_PTR(ptr) _check_ptr(ptr, __func__, #ptr)


static JNIEnv *getJNIEnv(const char *funcName) {
    //必ずJAVAアタッチ済スレッドから呼ばれること。
    JNIEnv *env;
    if (sJVM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        LOGF("%s: (%s) GetEnv()!=JNI_OK", TAG, funcName);
        return nullptr;
    }
    return env;
}

//スコープを外れたら自動的にローカル参照を解放する
class ScopedLocalFrame {
    JNIEnv *mEnv;
public:
    explicit ScopedLocalFrame(JNIEnv *env, unsigned capacity) : mEnv(env) {
        mEnv->PushLocalFrame(capacity);
    }

    ~ScopedLocalFrame() {
        mEnv->PopLocalFrame(nullptr);
    }
};

static struct StringClassCache {
    void init(JNIEnv *env) {
        clazz = (jclass) env->NewGlobalRef(env->FindClass("java/lang/String"));
        CHECK_PTR(clazz);
        midStringInit = env->GetMethodID(clazz, "<init>", "([BLjava/lang/String;)V");
        CHECK_PTR(midStringInit);
    }

    jstring SafeNewString(JNIEnv *env, const char *s, const char *encoding = "utf8") {
        if (s == nullptr)
            return nullptr;

        const jsize len = ::strlen(s);
        jbyteArray ba = env->NewByteArray(len);
        if (!ba)
            return nullptr;
        env->SetByteArrayRegion(ba, 0, len, (jbyte *) s);

        return (jstring) env->NewObject(
                clazz, midStringInit,
                ba, env->NewStringUTF(encoding));
    }

private:
    jclass clazz;//java.lang.String
    jmethodID midStringInit; //String(byte[], String)
} sStringClassCache;

/**
 * org.peercast.core.PeerCastServiceのクラス、メソッドIDをキャッシュする。
 * */
static class PeerCastServiceClassCache {
    jclass clazz;//org.peercast.core.PeerCastService
    jmethodID mid_notifyMessage; //void notifyMessage(int, String)
    jmethodID mid_notifyChannel; //void notifyChannel(int, String, String)
public:
    void init(JNIEnv *env, jclass clazz_) {
        clazz = (jclass) env->NewGlobalRef(clazz_);

        mid_notifyMessage = env->GetMethodID(clazz, "notifyMessage",
                                         "(ILjava/lang/String;)V");
        CHECK_PTR(mid_notifyMessage);

        mid_notifyChannel = env->GetMethodID(clazz, "notifyChannel",
                                             "(ILjava/lang/String;Ljava/lang/String;)V");
        CHECK_PTR(mid_notifyChannel);
    }

    void APICALL notifyMessage(JNIEnv *env, jobject this_,
                               ServMgr::NOTIFY_TYPE tNotify,
                               const char *message) {
        ScopedLocalFrame __frame(env, 16);
        jstring jMsg = sStringClassCache.SafeNewString(env, message);
        CHECK_PTR(jMsg);

        env->CallVoidMethod(this_, mid_notifyMessage, tNotify, jMsg);
    }

    typedef enum {
        NOTIFY_CHANNEL_START = 0,
        NOTIFY_CHANNEL_UPDATE = 1,
        NOTIFY_CHANNEL_STOP = 2
    } NotifyType;

    void notifyChannel(JNIEnv *env, jobject this_, NotifyType notifyType,
                       const std::string &chId, const std::string &jsonChannelInfo) {
        ScopedLocalFrame __frame(env, 16);
        env->CallVoidMethod(this_, mid_notifyChannel,
                            notifyType,
                            sStringClassCache.SafeNewString(env, chId.data()),
                            sStringClassCache.SafeNewString(env, jsonChannelInfo.data())

        );
    }
} sPeerCastServiceCache;

#if THREAD_DEBUG
#define LOG_THREAD(...) __android_log_print(ANDROID_LOG_DEBUG, "PeCaTh", __VA_ARGS__)
#else
#define LOG_THREAD(...)
#endif

//ネイティブスレッドをvmに関連付ける
class ScopedThreadAttacher {
    JNIEnv *mEnv;
public:
    explicit ScopedThreadAttacher() : mEnv(nullptr) {
        LOG_THREAD("Thread start. tid=%d", gettid());

        jint ret = sJVM->GetEnv((void **) &mEnv, JNI_VERSION_1_6);
        if (ret != JNI_EDETACHED) {
            LOG_THREAD("Error: VM->GetEnv()!=JNI_EDETACHED, tid=%d", gettid())
            return;
        }
        ret = sJVM->AttachCurrentThread(&mEnv, nullptr);
        if (ret != JNI_OK) {
            LOG_THREAD("Error: VM->AttachCurrentThread(), tid=%d", gettid())
            return;
        }
        LOG_THREAD("OK: VM->AttachCurrentThread(), tid=%d", gettid());
    }

    ~ScopedThreadAttacher() {
        if (mEnv != nullptr) {
            sJVM->DetachCurrentThread();
            LOG_THREAD("Detached thread. tid=%d", gettid());
        }
    }
};


class ASys : public USys {
public:
    void exit() override {
        LOGF("%s is Not Implemented", __func__);
    }

    void executeFile(const char *f) override {
        LOGF("%s is Not Implemented", __func__);
    }

    bool startThread(ThreadInfo *info) final {
        info->m_active.store(true);

        try {
            info->handle = std::thread([info]() {
                ScopedThreadAttacher __attacher;
                try {
                    sys->setThreadName("new thread");
                    info->func(info);
                } catch (std::exception &e) {
                    // just log it and continue..
                    LOG_ERROR("Unexpected exception: %s", e.what());
                }
            });
            info->handle.detach();
            return true;
        } catch (std::system_error &e) {
            LOG_ERROR("Error creating thread");
            return false;
        }
    }

};

class AndroidPeercastInst : public PeercastInstance {
public:
    Sys *APICALL createSys() final {
        return new ASys;
    }
};

#include "jrpc.h"

class AndroidPeercastApp : public PeercastApplication {
    jobject serviceInstance; //Instance of PeerCastService
    std::string iniPath;
    std::string resourceDirPath;
public:
    AndroidPeercastApp(jobject jthis, jstring jFilesDirPath) {
        JNIEnv *env = ::getJNIEnv(__func__);

        serviceInstance = env->NewGlobalRef(jthis);

        const char *filesDirPath = env->GetStringUTFChars(jFilesDirPath, nullptr);

        iniPath = filesDirPath;
        iniPath += "/peercast.ini";
        resourceDirPath = filesDirPath;
        resourceDirPath += "/";

        env->ReleaseStringUTFChars(jFilesDirPath, filesDirPath);

        LOGD("IniFilePath=%s, ResourceDir=%s", iniPath.data(), resourceDirPath.data());
    }

    ~AndroidPeercastApp() override {
        JNIEnv *env = ::getJNIEnv(__func__);
        env->DeleteGlobalRef(serviceInstance);
    }

    const char *APICALL getIniFilename() final {
        return iniPath.data();
    }

    const char *APICALL getPath() final {
        return resourceDirPath.data();
    }

    const char *APICALL getClientTypeOS() final {
        return "Linux";
    }

    void APICALL printLog(LogBuffer::TYPE t, const char *str) final {
        thread_local static int priorities[] = {
                0, //行の続き T_NONE=0
                ANDROID_LOG_VERBOSE, //	T_TRACE=1
                ANDROID_LOG_DEBUG, //	T_DEBUG=2
                ANDROID_LOG_INFO,  //	T_INFO=3
                ANDROID_LOG_WARN,  //	T_WARN=4
                ANDROID_LOG_ERROR, //	T_ERROR=5
                ANDROID_LOG_FATAL, //	T_FATAL=6
                0, //	T_OFF=7 未使用?
        };
        bool isNone = t == LogBuffer::TYPE::T_NONE;
        if (!isNone)
            priorities[LogBuffer::TYPE::T_NONE] = priorities[t]; //スレッド変数に保存しておく

        if (priorities[t] == 0)
            return;

        char tag[24];//tagは23文字まで
        ::snprintf(tag, sizeof(tag), "%s[%s]", TAG, LogBuffer::getTypeStr(t));
        ::__android_log_print(priorities[t], tag, "%s%s", isNone ? "  " : "", str);
    }

    /**
     * notifyMessage(int, String)
     * */
    void APICALL notifyMessage(ServMgr::NOTIFY_TYPE tNotify, const char *message) {
        sPeerCastServiceCache.notifyMessage(
                ::getJNIEnv(__func__), serviceInstance, tNotify, message
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
        JNIEnv *env = ::getJNIEnv(__func__);
        JrpcApi api;
        sPeerCastServiceCache.notifyChannel(env,
                                            serviceInstance,
                                            notifyType,
                                            info->id.str(),
                                            api.to_json(*info).dump());
    }
};


extern "C" JNIEXPORT void JNICALL
Java_org_peercast_core_PeerCastService_nativeStart(JNIEnv *env, jobject jthis,
                                                   jstring filesDirPath) {

    if (peercastApp) {
        LOGE("PeerCast has already been running!");
        return;
    }

    peercastApp = new AndroidPeercastApp(jthis, filesDirPath);
    peercastInst = new AndroidPeercastInst();

    peercastInst->init();
}

extern "C" JNIEXPORT void JNICALL
Java_org_peercast_core_PeerCastService_nativeQuit(JNIEnv *env, jobject jthis) {

    if (peercastInst != nullptr) {
        peercastInst->saveSettings();
        peercastInst->quit();
        LOGD("peercastInst->quit() OK.");
        ::sleep(3); //sleepしているスレッドがあるので待つ
    }

    delete servMgr;
    servMgr = nullptr;
    delete chanMgr;
    chanMgr = nullptr;

    delete peercastInst;
    peercastInst = nullptr;
    delete peercastApp;
    peercastApp = nullptr;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_peercast_core_PeerCastService_setPort(JNIEnv *env, jobject thiz, jint port) {
    if (servMgr && peercastInst && servMgr->serverHost.port != port) {
        if (port >= 1025 && port <= 65532){
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
Java_org_peercast_core_PeerCastService_getPort(JNIEnv *env, jobject thiz) {
    return servMgr ? servMgr->serverHost.port : 0;
}

extern "C" JNIEXPORT void JNICALL Java_org_peercast_core_PeerCastService_nativeClassInit(
        JNIEnv *env, jclass jclz) {
    sPeerCastServiceCache.init(env, jclz);
}

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    LOGI("PeerCast-YT(A): revision=%s", YT_REVISION);
    LOGI("jni/PeerCastService.cpp: %s %s", __DATE__, __TIME__);

    sJVM = vm;

    sStringClassCache.init(env);

    return JNI_VERSION_1_6;
}

