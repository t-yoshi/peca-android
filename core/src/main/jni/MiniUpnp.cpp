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
#include "nativehelper/scoped_local_frame.h"
#include "nativehelper/scoped_local_ref.h"
#include "nativehelper/scoped_primitive_array.h"
#include "nativehelper/scoped_utf_chars.h"
#include "json.hpp"
#include <miniupnpc.h>
#include <upnpcommands.h>
#include <upnperrors.h>
#include <upnpdev.h>
#include <cstring>
#include <cstdio>
#include <cstdlib>
#include <memory>
#include <exception>
#include <regex>

using namespace std;

#define TAG "MiniUpnpNt"

#define IP_PROTOCOL_TCP    "TCP"
#define IP_PROTOCOL_UDP    "UDP"

struct BaseError : exception {
    const char *what() const noexcept override { return _what.c_str(); }

    virtual void throwJniException(JNIEnv *) const noexcept = 0;

protected:
    BaseError() : _what("unknown error") {}

    string _what;
};

struct NullError : BaseError {
    void throwJniException(JNIEnv *env) const noexcept override {
        jniThrowNullPointerException(env);
    }
};

struct UpnpError : BaseError {
    explicit UpnpError(const char *what) __attribute__((nonnull)) {
        _what = strprintf("UPnP Error: %s", what);
    }

    explicit UpnpError(int err) : _err(err) {
        const char *s = ::strupnperror(err);
        _what = strprintf("UPnP Error: %s (%d)", s != nullptr ? s : "unknown error", err);
    }

    void throwJniException(JNIEnv *env) const noexcept override {
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        ScopedLocalRef<jclass> eClazz(env,
                                      CHECK_NOT_NULL(
                                              env->FindClass("java/io/IOException")
                                      )
        );
        env->ThrowNew(eClazz.get(), _what.c_str());
    }

    int err() const noexcept { return _err; }

private:
    int _err = 0;
};

static bool upnpStrToBool(const char *s) noexcept {
    static const regex reUpnpBool(R"(1|true|yes)", regex_constants::icase);
    return regex_match(s, reUpnpBool);
}

static long upnpStrToNumber(const char *s, long minValue = LONG_MIN, long maxValue = LONG_MAX,
                            long defaultValue = 0) noexcept {
    char *e;
    errno = 0;
    long l = ::strtol(s, &e, 10);
    if (l < minValue || maxValue < l || errno) {
        LOGE("Invalid number: %ld ->%s", l, e);
        return defaultValue;
    }
    return l;
}


class MiniUpnp {
private:
    struct UPNPUrls_Deleter {
        void operator()(UPNPUrls *p) {
            LOGD("UPNPUrls_Deleter %s %p", __func__, p);
            ::FreeUPNPUrls(p);
            delete p;
        }
    };

    unique_ptr<UPNPUrls, UPNPUrls_Deleter> urls;
    unique_ptr<IGDdatas> datas;
    char lanAddr[64];

    DISALLOW_COPY_AND_ASSIGN(MiniUpnp);

public:
    MiniUpnp() : datas(new IGDdatas{}), lanAddr() {
    }

    void discover() {
        int timeout = 3000;
        int ipv6 = 0; // 0 = IPv4, 1 = IPv6
        unsigned char ttl = 2; // defaulting to 2
        int err = 0;

        unique_ptr<UPNPDev, decltype(&freeUPNPDevlist)> devList(
                ::upnpDiscover(timeout, nullptr, nullptr, 0, ipv6, ttl, &err),
                ::freeUPNPDevlist
        );

        if (!devList) {
            if (err == 0)
                throw UpnpError("discover timeout");
            throw UpnpError(err);
        }

        UPNPDev *dev = devList.get();
        while (dev != nullptr) {
            if (::strstr(dev->st, "InternetGatewayDevice"))
                break;
            dev = dev->pNext;
        }
        if (dev == nullptr)
            dev = devList.get(); // defaulting to first device

        urls.reset(new UPNPUrls{});
        int status = ::UPNP_GetValidIGD(dev, urls.get(), datas.get(), lanAddr, sizeof lanAddr);
        //possible "status" values,
        // 0 = NO IGD found,
        // 1 = A valid connected IGD has been found,
        // 2 = A valid IGD has been found but it reported as not connected,
        // 3 = an UPnP device has been found but was not recognized as an IGD
        LOGI("UPnP device: [desc: %s] [st: %s]", dev->descURL, dev->st);

        if (status == 0)
            throw UpnpError("discover NO IGD found");
    }


    void addPort(int port, const char *desc, int duration) {
        LOGV("%s: %d", __func__, port);
        if (!urls)
            throw NullError();

        const string sPort = to_string(port);
        const string sDuration = to_string(duration);

        int err = ::UPNP_AddPortMapping(urls->controlURL, datas->first.servicetype,
                                        sPort.c_str(), sPort.c_str(), lanAddr, desc,
                                        IP_PROTOCOL_TCP,
                                        nullptr, sDuration.c_str());
        // OnlyPermanentLeasesSupported
        if (err == 725 && duration != 0) {
            err = ::UPNP_AddPortMapping(urls->controlURL, datas->first.servicetype,
                                        sPort.c_str(), sPort.c_str(), lanAddr, desc,
                                        IP_PROTOCOL_TCP,
                                        nullptr, "0");
        }

        if (err != 0)
            throw UpnpError(err);
    }

    void removePort(int port) {
        LOGV("%s: %d", __func__, port);
        if (!urls)
            throw NullError();

        int err = ::UPNP_DeletePortMapping(urls->controlURL, datas->first.servicetype,
                                           to_string(port).c_str(),
                                           IP_PROTOCOL_TCP, nullptr);
        if (err != 0)
            throw UpnpError(err);
    }

    nlohmann::ordered_json getStatuses() {
        LOGV("%s", __func__);
        if (!urls)
            throw NullError();
        nlohmann::ordered_json j;

        j["upnp_ip_address"] = getIpAddress();

        char buf[64];
        int err = ::UPNP_GetExternalIPAddress(urls->controlURL, datas->first.servicetype, buf);
        if (err == 0) {
            j["upnp_external_ip_address"] = buf;
        }

        unsigned brUp, brDown;
        err = ::UPNP_GetLinkLayerMaxBitRates(urls->controlURL, datas->first.servicetype, &brDown,
                                             &brUp);
        if (err == 0) {
            j["upnp_downstream_max_bitrate"] = strprintf("%dkbps", brUp / 1024);
            j["upnp_upstream_max_bitrate"] = strprintf("%dkbps", brDown / 1024);
        }

        return j;
    }

    nlohmann::json getPortMap(int index) {
        if (!urls)
            throw NullError();

        const string sIndex = to_string(index);

        char extPort[6]{};
        char intClient[16]{};
        char intPort[6]{};
        char protocol[4]{};
        char desc[80]{};
        char enabled[4]{};
        char rHost[64]{};
        char duration[16]{};

        int err = ::UPNP_GetGenericPortMappingEntry(urls->controlURL,
                                                    datas->first.servicetype,
                                                    sIndex.c_str(),
                                                    extPort, intClient, intPort,
                                                    protocol, desc, enabled,
                                                    rHost, duration);
/*
            class MiniUpnpPortMap(
                override val externalPort: Int,
                override val internalClient: String,
                override val internalPort: Int,
                override val protocol: String,
                override val description: String,
                override val enabled: Boolean,
                override val remoteHost: String,
                override val leaseDuration: Int,
            )
*/
        if (err != 0)
            throw UpnpError(err);

        return {
                {"externalPort",   ::upnpStrToNumber(extPort, 0, 0xffff)},
                {"internalClient", intClient},
                {"internalPort",   ::upnpStrToNumber(intPort, 0, 0xffff)},
                {"protocol",       protocol},
                {"description",    desc},
                {"enabled",        ::upnpStrToBool(enabled)},
                {"remoteHost",     rHost},
                {"leaseDuration",  ::upnpStrToNumber(duration, 0)},
        };
    }

    const char *getIpAddress() const noexcept { return lanAddr; }

};


static struct {
    MiniUpnp *newMiniUpnp(JNIEnv *env, jobject obj) noexcept {
        A_ASSERT(env->GetLongField(obj, _nativeInstance) == 0L);
        auto m = new MiniUpnp;
        env->SetLongField(obj, _nativeInstance, (jlong) m);
        return m;
    }

    MiniUpnp *getMiniUpnp(JNIEnv *env, jobject obj) {
        jlong p = env->GetLongField(obj, _nativeInstance);
        if (p == 0L)
            throw NullError();
        return reinterpret_cast<MiniUpnp *>(p);
    }

    void deleteMiniUpnp(JNIEnv *env, jobject obj) noexcept {
        delete reinterpret_cast<MiniUpnp *>(env->GetLongField(obj, _nativeInstance));
    }

    void initClassCache(JNIEnv *env, jclass clz) noexcept {
        _nativeInstance = CHECK_NOT_NULL(
                env->GetFieldID(clz, "nativeInstance", "J")
        );
    }

private:
    jfieldID _nativeInstance;
} classCache;


extern "C"
JNIEXPORT void JNICALL
Java_org_peercast_core_upnp_MiniUpnp_initInstance(JNIEnv *env, jobject thiz) {
    LOGV("%s", __func__);
    try {
        classCache.newMiniUpnp(env, thiz)->discover();
    } catch (const BaseError &e) {
        e.throwJniException(env);
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_peercast_core_upnp_MiniUpnp_getIpAddress(JNIEnv *env, jobject thiz) {
    LOGV("%s", __func__);
    try {
        return newJString(env, classCache.getMiniUpnp(env, thiz)->getIpAddress());
    } catch (const BaseError &e) {
        e.throwJniException(env);
        return nullptr;
    }
}


extern "C"
JNIEXPORT void JNICALL
Java_org_peercast_core_upnp_MiniUpnp_addPort(JNIEnv *env, jobject thiz,
                                             jint port, jstring desc, jint duration) {
    LOGV("%s", __func__);
    try {
        classCache.getMiniUpnp(env, thiz)->addPort(
                port,
                ScopedUtfChars(env, desc).c_str(),
                duration);
    } catch (const BaseError &e) {
        e.throwJniException(env);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_org_peercast_core_upnp_MiniUpnp_removePort(JNIEnv *env, jobject thiz, jint port) {
    LOGV("%s", __func__);
    try {
        classCache.getMiniUpnp(env, thiz)->removePort(port);
    } catch (const BaseError &e) {
        e.throwJniException(env);
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_peercast_core_upnp_MiniUpnp_getPortMapsJson(JNIEnv *env, jobject thiz) {
    LOGV("%s", __func__);
    try {
        auto entries = nlohmann::json::array();
        for (int i = 0; i < 128; i++) {
            try {
                entries += classCache.getMiniUpnp(env, thiz)->getPortMap(i);
            } catch (const UpnpError &e) {
                //SpecifiedArrayIndexInvalid
                if (e.err() == 713 || !entries.empty())
                    break;
                throw;
            }
        }
        return newJString(env, entries.dump().data());
    } catch (const BaseError &e) {
        e.throwJniException(env);
        return nullptr;
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_peercast_core_upnp_MiniUpnp_getStatusesJson(JNIEnv *env, jobject thiz) {
    LOGV("%s", __func__);
    try {
        auto j = classCache.getMiniUpnp(env, thiz)->getStatuses();
        return newJString(env, j.dump().c_str());
    } catch (const BaseError &e) {
        e.throwJniException(env);
        return nullptr;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_org_peercast_core_upnp_MiniUpnp_finalize(JNIEnv *env, jobject thiz) {
    LOGV("%s", __func__);
    classCache.deleteMiniUpnp(env, thiz);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_peercast_core_upnp_MiniUpnp_initClass(JNIEnv *env, jclass clazz) {
    LOGV("%s", __func__);
    classCache.initClassCache(env, clazz);
}

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    return initJniHelper(vm) == JNI_OK ? JNI_VERSION_1_6 : -1;
}
