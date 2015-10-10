// ------------------------------------------------
// File : org_peercast_core_PeerCastService.cpp
// Date: 25-Apr-2013
// Author: (c) 2013 T Yoshizawa
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

#include "org_peercast_core_PeerCastService.h"
#include <android/log.h>

#include "AutoPtr.h"

static JavaVM *sJVM;

#define TAG "PeCaNt"

#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL, TAG, __VA_ARGS__)


#define CHECK_PTR(ptr) do { \
		if ((ptr) == NULL) {\
			LOGF("[%s] %s is NULL", __func__, #ptr);\
			abort(); /*__noreturn*/ \
		}\
	} while(0)

class ScopedWLock {
	WLock &mLock;
public:
	ScopedWLock(WLock &lock) :
			mLock(lock) {
		mLock.on();
	}
	~ScopedWLock() {
		mLock.off();
	}
};

static JNIEnv *getJNIEnv() {
	//必ずJAVAアタッチ済スレッドから呼ばれること。
	JNIEnv *env;
	if (sJVM->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
		::__android_log_write(ANDROID_LOG_FATAL, TAG, "GetEnv()!=JNI_OK");
		return 0;
	}
	return env;
}

/**
 * private継承すれば、コピーコンストラクタがコンパイルエラーになる。
 * */
class NoCopyConstructor {
	NoCopyConstructor(const NoCopyConstructor&);
	void operator=(const NoCopyConstructor&);
public:
	NoCopyConstructor() {
	}
};

/**
 * クラス、メソッドIDをキャッシュするための基底クラス。
 * */
class JClassCache: private NoCopyConstructor {
protected:
	JClassCache() :
			clazz(0) {
	}

public:
	jclass clazz;

	virtual ~JClassCache() {
		//Androidでは.soがアンロードされないので呼ばない。
		//env->DeleteGlobalRef(clazz)
	}

	//JNI_OnLoad時に一度だけfindClassすればよい。
	bool initClass(JNIEnv *env, const char *className) {
		jclass cls = env->FindClass(className);
		if (cls)
			initClass(env, cls);
		return !!cls;
	}
	void initClass(JNIEnv *env, jclass clz) {
		if (clazz)
			return; //クラスが再ロードされたときなら何もしない。
		clazz = (jclass) env->NewGlobalRef(clz);
	}

	/*
	 * methodIDをキャッシュする。
	 * Androidではクラスが再ロードされ、MethodIDが変わることがある。
	 */
	virtual void initIDs(JNIEnv *env) = 0;

};

/**
 * android.os.Bundleのクラス、メソッドIDをキャッシュする。
 * */
static struct BundleClassCache: public JClassCache {
	//clazz = android.os.Bundle
	jmethodID init; //<init>()
	jmethodID putString; // (String key, String value)
	jmethodID putInt; // (String key, int value)
	jmethodID putDouble; // (String key, double value)
	jmethodID putLong; //  (String key, long value)
	jmethodID putBoolean; // (String key, boolean value)
	jmethodID putBundle; // (String key, Bundle value)

	void initIDs(JNIEnv *env) {
		init = env->GetMethodID(clazz, "<init>", "()V");
		CHECK_PTR(init);

		putString = env->GetMethodID(clazz, "putString",
				"(Ljava/lang/String;Ljava/lang/String;)V");
		CHECK_PTR(putString);

		putInt = env->GetMethodID(clazz, "putInt", "(Ljava/lang/String;I)V");
		CHECK_PTR(putInt);

		putDouble = env->GetMethodID(clazz, "putDouble",
				"(Ljava/lang/String;D)V");
		CHECK_PTR(putDouble);

		putLong = env->GetMethodID(clazz, "putLong", "(Ljava/lang/String;J)V");
		CHECK_PTR(putLong);

		putBoolean = env->GetMethodID(clazz, "putBoolean",
				"(Ljava/lang/String;Z)V");
		CHECK_PTR(putBoolean);

		putBundle = env->GetMethodID(clazz, "putBundle",
				"(Ljava/lang/String;Landroid/os/Bundle;)V");
		CHECK_PTR(putBundle);
	}
} gBundleCache;

/**
 * android.os.Bundleのインスタンス作成とc++ラッパー
 *
 * jobj()の参照はローカル関数内のみで有効
 **/
class JBundle: private NoCopyConstructor {
	jobject mBundle;
protected:
	JNIEnv *mEnv;

public:
	JBundle(JNIEnv *e) :
			mEnv(e) {
		mBundle = mEnv->NewObject(gBundleCache.clazz, gBundleCache.init);
	}
	virtual ~JBundle() {
		mEnv->DeleteLocalRef(mBundle);
	}

	void putString(const char *key, const char *value) {
		jstring jsKey = mEnv->NewStringUTF(key);
		jstring jsVal = mEnv->NewStringUTF(value);

		mEnv->CallVoidMethod(mBundle, gBundleCache.putString, jsKey, jsVal);

		mEnv->DeleteLocalRef(jsKey);
		mEnv->DeleteLocalRef(jsVal);
	}
	void putStringF(const char *key, const char *fmt, ...) {
		char buf[1024];
		va_list list;
		va_start(list, fmt);
		::vsnprintf(buf, sizeof(buf), fmt, list);
		va_end(list);
		putString(key, buf);
	}

#define DEFINE_PUT_METHOD(Method, Type)		\
	void Method(const char *key, Type value){	\
		jstring jsKey = mEnv->NewStringUTF(key);	\
		mEnv->CallVoidMethod(mBundle, gBundleCache.Method, jsKey, value);	\
		mEnv->DeleteLocalRef(jsKey);	\
	}

	DEFINE_PUT_METHOD(putInt, jint) //
	DEFINE_PUT_METHOD(putDouble, jdouble) //
	DEFINE_PUT_METHOD(putLong, jlong) //
	DEFINE_PUT_METHOD(putBoolean, jboolean) //
	DEFINE_PUT_METHOD(putBundle, jobject) //

#undef DEFINE_PUT_METHOD

	/**
	 * Bundleオブジェクトを返す。
	 * NULLの場合は、JAVA例外(OutOfMemoryError)の可能性。
	 * */
	jobject jobj() const {
		return mBundle;
	}

	/**
	 * 新しいLocal参照のBundleオブジェクトを返す。
	 * ネイティブ関数の戻り値用。
	 */
	jobject newRef() const {
		return mEnv->NewLocalRef(mBundle);
	}
};

/**
 * org.peercast.core.PeerCastServiceのクラス、メソッドIDをキャッシュする。
 * */
static struct PeerCastServiceClassCache: public JClassCache {
	// clazz = org.peercast.core.PeerCastService
	jmethodID notifyMessage; //void notifyMessage(int, String)
	jmethodID notifyChannel; //void notifyChannel(int, Bundle)

	void initIDs(JNIEnv *env) {
		notifyMessage = env->GetMethodID(clazz, "notifyMessage",
				"(ILjava/lang/String;)V");
		CHECK_PTR(notifyMessage);

		notifyChannel = env->GetMethodID(clazz, "notifyChannel",
				"(ILandroid/os/Bundle;)V");
		CHECK_PTR(notifyChannel);
	}
} gPeerCastServiceCache;

class ASys: public USys {
public:
	void exit() {
		LOGF("%s is Not Implemented", __func__);
	}
	void executeFile(const char *f) {
		LOGF("%s is Not Implemented", __func__);
	}
};

class AndroidPeercastInst: public PeercastInstance {
public:
	virtual Sys* APICALL createSys() {
		/**
		 * staticで持っておかないと、quit()のあと生きてるスレッドが
		 * sys->endThread()を呼んでクラッシュする。
		 **/
		static AutoPtr<Sys> apSys(new ASys());
		return apSys.get();
	}
};

/**
 * ChanInfoの情報をBundleにput。
 *
 * ラッパー: ChannelInfo.java
 * */
class JBundleChannelInfoData: public JBundle {
public:
	JBundleChannelInfoData(JNIEnv *env) :
			JBundle(env) {
	}

	void setData(ChanInfo* info) {
		char strId[64];
		info->id.toStr(strId);
		putString("id", strId);
		putInt("contentType", info->contentType);
		putString("track.artist", info->track.artist);
		putString("track.title", info->track.title);
		putString("name", info->name);
		putString("desc", info->desc);
		putString("genre", info->genre);
		putString("comment", info->comment);
		putString("url", info->url);
		putInt("bitrate", info->bitrate);
	}
};

class AndroidPeercastApp: public PeercastApplication {
	jobject mInstance; //Instance of PeerCastService
	String mIniFilePath;
	String mResourceDir;
public:
	AndroidPeercastApp(jobject jthis, jstring jsIniPath, jstring jsResPath) {
		JNIEnv *env = ::getJNIEnv();

		mInstance = env->NewGlobalRef(jthis);

		const char *ini = env->GetStringUTFChars(jsIniPath, NULL);
		const char *res = env->GetStringUTFChars(jsResPath, NULL);

		mIniFilePath.set(ini);
		mResourceDir.set(res);
		mResourceDir.append('/');

		env->ReleaseStringUTFChars(jsIniPath, ini);
		env->ReleaseStringUTFChars(jsResPath, res);

		LOGD("IniFilePath=%s, ResourceDir=%s", mIniFilePath.cstr(), mResourceDir.cstr());
	}

	virtual ~AndroidPeercastApp() {
		JNIEnv *env = ::getJNIEnv();
		env->DeleteGlobalRef(mInstance);
	}

	virtual const char * APICALL getIniFilename() {
		return mIniFilePath;
	}

	virtual const char * APICALL getPath() {
		return mResourceDir;
	}

	virtual const char *APICALL getClientTypeOS() {
		return PCX_OS_LINUX;
	}

	virtual void APICALL printLog(LogBuffer::TYPE t, const char *str) {
		int prio[] = { ANDROID_LOG_UNKNOWN, //	T_NONE
				ANDROID_LOG_DEBUG, //	T_DEBUG
				ANDROID_LOG_ERROR, //	T_ERROR,
				ANDROID_LOG_INFO, //	T_NETWORK,
				ANDROID_LOG_INFO, //	T_CHANNEL,
				};
		char tag[32];
		::snprintf(tag, sizeof(tag), "%s[%s]", TAG, LogBuffer::getTypeStr(t));
		::__android_log_write(prio[t], tag, str);
	}

	/**
	 * notifyMessage(int, String)
	 *
	 * Nativeからjavaメソッドを呼ぶ。
	 *
	 * ただ、finding channel.. と、PeCaソフト更新情報のみなら用途なし？
	 * */
	void APICALL notifyMessage(ServMgr::NOTIFY_TYPE tNotify,
			const char *message) {
		JNIEnv *env = ::getJNIEnv();

		jstring jMsg = env->NewStringUTF(message);
		CHECK_PTR(jMsg);

		env->CallVoidMethod(mInstance, gPeerCastServiceCache.notifyMessage,
				tNotify, jMsg);
		env->DeleteLocalRef(jMsg);
	}
	/*
	 *  channelStart(ChanInfo *)
	 *  channelUpdate(ChanInfo *)
	 *  channelStop(ChanInfo *)
	 *
	 *    -> (Java) notifyChannel(int, Bundle)
	 */
	void APICALL channelStart(ChanInfo *info) {
		notifyChannel(org_peercast_core_PeerCastService_NOTIFY_CHANNEL_START,
				info);
	}

	void APICALL channelUpdate(ChanInfo *info) {
		notifyChannel(org_peercast_core_PeerCastService_NOTIFY_CHANNEL_UPDATE,
				info);
	}

	void APICALL channelStop(ChanInfo *info) {
		notifyChannel(org_peercast_core_PeerCastService_NOTIFY_CHANNEL_STOP,
				info);
	}

private:

	void notifyChannel(jint notifyType, ChanInfo *info) {
		JNIEnv *env = ::getJNIEnv();

		JBundleChannelInfoData bChInfo(env);
		CHECK_PTR(bChInfo.jobj());

		bChInfo.setData(info);
		env->CallVoidMethod(mInstance, gPeerCastServiceCache.notifyChannel,
				notifyType, bChInfo.jobj());
	}
};

// ----------------------------------
void setSettingsUI() {
}
// ----------------------------------
void showConnections() {
}
// ----------------------------------
void PRINTLOG(LogBuffer::TYPE type, const char *fmt, va_list ap) {
}

JNIEXPORT jint JNICALL
Java_org_peercast_core_PeerCastService_nativeStart(JNIEnv *env, jobject jthis,
		jstring jsIniFilePath, jstring jsResDir) {

	if (peercastApp) {
		jclass ex = env->FindClass("java/lang/IllegalStateException");
		CHECK_PTR(ex);
		env->ThrowNew(ex, "PeerCast already running!");
		return 0;
	}

	peercastApp = new AndroidPeercastApp(jthis, jsIniFilePath, jsResDir);
	peercastInst = new AndroidPeercastInst();

	peercastInst->init();

	//peercastApp->getPathを上書きしない。
	servMgr->getModulePath = false;

	//ポートを指定して起動する場合
	//servMgr->serverHost.port = port;
	//servMgr->restartServer=true;

	return servMgr->serverHost.port;
}

#define DELETE_GLOBAL(sym) do {\
	delete sym;\
	sym = 0;\
	LOGD("delete global '%s' OK.", #sym);\
}while(0)

JNIEXPORT void JNICALL
Java_org_peercast_core_PeerCastService_nativeQuit(JNIEnv *env, jobject jthis) {

	if (peercastInst) {
		peercastInst->saveSettings();
		peercastInst->quit();
		LOGD("peercastInst->quit() OK.");
		::sleep(3); //sleepしているスレッドがあるので待つ
	}

	DELETE_GLOBAL(peercastInst);
	DELETE_GLOBAL(peercastApp);
	DELETE_GLOBAL(servMgr);
	DELETE_GLOBAL(chanMgr);
}

class JBundleServentData: public JBundle {
	void putVersionData(ChanHit *chHit) {
		if (chHit->version_ex_number) {
			// 拡張バージョン
			putStringF("version", "%c%c%04d", chHit->version_ex_prefix[0],
					chHit->version_ex_prefix[1], chHit->version_ex_number);
		} else if (chHit->version_vp) {
			putStringF("version", "VP%04d", chHit->version_vp);
		} else {
			putStringF("version", "%04d", chHit->version);
		}
	}

public:
	JBundleServentData(JNIEnv *env) :
			JBundle(env) {
	}


	void setData(Servent *servent) {
		jint totalRelays = 0;
		jint totalListeners = 0;
		jboolean infoFlg = 0;

		ScopedWLock _lock(chanMgr->hitlistlock);
		ChanHitList *chHitList = chanMgr->findHitListByID(servent->chanID);
		// チャンネルのホスト情報があるか
		if (chHitList) {
			// チャンネルのホスト情報がある場合
			ChanHit *chHit = chHitList->hit;
			//　チャンネルのホスト情報を全走査して
			while (chHit) {
				// IDが同じものであれば
				if (servent->servent_id == chHit->servent_id) {
					// トータルリレーとトータルリスナーを加算
					totalRelays += chHit->numRelays;
					totalListeners += chHit->numListeners;
					// 直下であれば
					if (chHit->numHops == 1) {
					    putInt("numHops", chHit->numHops);
		                putBoolean("relay", chHit->relay);
		                putBoolean("firewalled", chHit->firewalled);
		                putInt("numRelays", chHit->numRelays);
		                putVersionData(chHit);
		                char ip[32];
		                chHit->host.IPtoStr(ip);
		                putString("host", ip);
		                putInt("port", chHit->host.port);
		                putBoolean("infoFlg", 1);
						break;
					}
				}
				chHit = chHit->next;
			}
		}

		putInt("servent_id", servent->servent_id);
		putInt("totalListeners", totalListeners);
		putInt("totalRelays", totalRelays);
	}
};




class JBundleChannelData: public JBundle {
	void putServentDatas(Channel *ch) {
		ScopedWLock _lock(servMgr->lock);

		AutoPtr<JBundleServentData> prevServent(NULL);
		int numSvt = 0;
		for (Servent *svt = servMgr->servents; svt; svt = svt->next) {
			if (svt->isConnected() && ch->channel_id == svt->channel_id
					&& svt->type == Servent::T_RELAY) {

				JBundleServentData *bServent = new JBundleServentData(mEnv);
				CHECK_PTR(bServent->jobj());

				bServent->setData(svt);

				if (numSvt == 0) {
					//リニアリストの先頭
					putBundle("servent", bServent->jobj());
				} else {
					//前の要素のnextにプット。
					prevServent->putBundle("next", bServent->jobj());
				}
				//auto_ptrに入れる。前の要素があればdeleteされる。
				prevServent.reset(bServent);
				numSvt++;
			}
		}
		if (numSvt == 0) {
			putBundle("servent", NULL);
		}
	}

public:
	JBundleChannelData(JNIEnv *env) :
			JBundle(env) {
	}
	void setData(Channel *ch) {
		char id[64];
		ch->getID().toStr(id);
		putString("id", id);

		putInt("channel_id", ch->channel_id);
		putInt("totalListeners", ch->totalListeners());
		putInt("totalRelays", ch->totalRelays());
		putInt("status", ch->status);
		putInt("localListeners", ch->localListeners());
		putInt("localRelays", ch->localRelays());
		putBoolean("stayConnected", ch->stayConnected);
		putBoolean("tracker", ch->sourceHost.tracker);
		putInt("lastSkipTime", ch->lastSkipTime);
		putInt("skipCount", ch->skipCount);

		JBundleChannelInfoData bChInfo(mEnv);
		if (bChInfo.jobj())
			bChInfo.setData(&ch->info);
		putBundle("info", bChInfo.jobj());

		putServentDatas(ch);

		//JBundleServentData bChDisp(env);
		//bChDisp.setChanHitData(&ch->chDisp);
		//putBundle("chDisp", bChDisp.jobj());
	}
};



/**
 *  nativeGetChannels()
 *
 * 現在アクティブなチャンネルの情報をリンクリスト形式のBundleで返す。
 *   ラッパー: Channel.java ::fromNativeResult()
 *
 * [Key, 対応するPeCa側のコード]
 * {
 *   id:  getID().toStr(), //String
 *   channel_id: ch->channel_id, //int
 * 	 totalListeners: ch->totalListeners(), //int
 * 	 totalRelays: ch->totalRelays(), //int
 * 	 status: ch->status, //int
 * 	 localListeners: ch->localListeners(), //int
 * 	 localRelays: ch->localRelays(), //int
 * 	 stayConnected: ch->stayConnected, //boolean
 * 	 tracker: ch->sourceHost.tracker, //boolean
 * 	 lastSkipTime: ch->lastSkipTime, //int
 * 	 skipCount: ch->skipCount, //int
 * 	 info: { //Bundle
 * 	    //チャンネル情報
 *	    id: info->id.toStr(), //String
 *      track.artist: info->track.artist, //String
 *      track.title: info->track.title, //String
 *      name: info->name, //String
 *      desc: info->desc, //String
 *      genre: info->genre, //String
 *      comment: info->comment, //String
 *      url: info->url, //String
 *      bitrate: info->bitrate, //int
 * 	 },
 *   servent: { //Bundle
 *      // 直下のServent情報のリスト
 *      servetn_id: chHit->servent_id,//int
 *      relay: chHit->relay, //boolean
 *      firewalled: chHit->firewalled, //boolean
 *      numRelays: chHit->numRelays, //int
 *      version: "例.IM0045", //String
 *      host: chHit->host.IPtoStr(ip), //String
 *      port: chHit->host.port, //int
 *      totalListeners: このServent以下の合計リスナ/リレー数, //int
 *		totalRelays:, //int
 *
 *      next: 次のServentへ。なければnull, //Bundle
 *   };
 *
 *   next: 次のチャンネルへ。なければnull, //Bundle
 * };
 *
 */

JNIEXPORT jobject JNICALL Java_org_peercast_core_PeerCastService_nativeGetChannels(
		JNIEnv *env, jobject jthis) {
	if (!peercastApp)
		return 0;

	AutoPtr<JBundleChannelData> prevChData(NULL);
	jobject firstChData = NULL; //戻り値

	ScopedWLock _lock(chanMgr->lock);

	int i = 0;
	for (Channel *ch = chanMgr->channel; ch ; ch = ch->next) {
		if (!ch->isActive())
			continue;

		JBundleChannelData *chData = new JBundleChannelData(env);
		CHECK_PTR(chData->jobj());

		chData->setData(ch);

		if (i == 0) {
			//戻り値
			firstChData = chData->newRef();
		} else {
			//前の要素のnextにプット。
			prevChData->putBundle("next", chData->jobj());
		}
		prevChData.reset(chData);
		i++;
	}

	return firstChData;
}

/**
 * nativeGetStats()
 *
 * stats@stats.hの情報をBundleで返す。
 * {
 *  in_bytes: ダウンロード bytes / sec //int
 *  out_bytes: アップロード bytes / sec //int
 *  in_total_bytes: 起動時からの合計ダウンロード //long
 *  out_total_bytes: 起動時からの合計アップロード //long
 * }
 */

JNIEXPORT jobject JNICALL Java_org_peercast_core_PeerCastService_nativeGetStats(
		JNIEnv *env, jobject jthis) {

	JBundle bStats(env);

	jint down_per_sec = stats.getPerSecond(Stats::BYTESIN)
			- stats.getPerSecond(Stats::LOCALBYTESIN);
	jint up_per_sec = stats.getPerSecond(Stats::BYTESOUT)
			- stats.getPerSecond(Stats::LOCALBYTESOUT);
	jlong totalDown = stats.getCurrent(Stats::BYTESIN)
			- stats.getCurrent(Stats::LOCALBYTESIN);
	jlong totalUp = stats.getCurrent(Stats::BYTESOUT)
			- stats.getCurrent(Stats::LOCALBYTESOUT);

	bStats.putInt("in_bytes", down_per_sec);
	bStats.putInt("out_bytes", up_per_sec);
	bStats.putLong("in_total_bytes", totalDown);
	bStats.putLong("out_total_bytes", totalUp);

	return bStats.newRef();
}

/**
 * nativeGetApplicationProperties()
 *
 * PeerCastの動作プロパティーをBundleで返す。
 *
 *  {
 *    port: servMgr->serverHost.port //int
 *  }
 * */

JNIEXPORT jobject JNICALL Java_org_peercast_core_PeerCastService_nativeGetApplicationProperties(
		JNIEnv *env, jobject jthis) {

	JBundle bProp(env);

	if (peercastApp) {
		bProp.putInt("port", servMgr->serverHost.port);
	} else {
		bProp.putInt("port", 0);
	}
	return bProp.newRef();
}

/**
 * nativeChannelCommand()
 *  チャンネルに関する操作を行う。
 *
 *  (BUMP | DISCONNECT | KEEP_YES | KEEP_NO)
 */

JNIEXPORT jboolean JNICALL Java_org_peercast_core_PeerCastService_nativeChannelCommand(
		JNIEnv *env, jobject jthis, jint cmdType, jint channel_id) {

	if(!chanMgr){
	    LOGE("nativeChannelCommand: chanMgr is NULL");
	    return JNI_FALSE;
    }
	Channel *ch = chanMgr->findChannelByChannelID(channel_id);
	if (!ch) {
		LOGE("nativeChannelCommand: channel not found. (channel_id=%d)",
				channel_id);
		return JNI_FALSE;
	}

	switch (cmdType) {
	case org_peercast_core_PeerCastService_MSG_CMD_CHANNEL_BUMP:
		// 再接続
		LOGI("Bump: channel_id=%d", channel_id);
		ch->bump = true;
		return JNI_TRUE;

	case org_peercast_core_PeerCastService_MSG_CMD_CHANNEL_DISCONNECT:
		// 切断
		// bump中は切断しない
		if (!ch->bumped) {
			LOGI("Disconnect: channel_id=%d", channel_id);
			ch->thread.active = false;
			ch->thread.finish = true;
			return JNI_TRUE;
		}
		break;

	case org_peercast_core_PeerCastService_MSG_CMD_CHANNEL_KEEP_YES:
		// キープする
		LOGI("Keep yes: channel_id=%d", channel_id);
		ch->stayConnected = true;
		return JNI_TRUE;

	case org_peercast_core_PeerCastService_MSG_CMD_CHANNEL_KEEP_NO:
		// キープしない
		LOGI("Keep no: channel_id=%d", channel_id);
		ch->stayConnected = false;
		return JNI_TRUE;

	default:
		LOGE("nativeChannelCommand: Invalid cmdType=0x%x", cmdType);
		break;
	}
	return JNI_FALSE;
}

/**
 * nativeDisconnectServent()
 * 指定したServentを切断する。
 * */
JNIEXPORT jboolean JNICALL Java_org_peercast_core_PeerCastService_nativeDisconnectServent
  (JNIEnv *env, jobject jthis, jint servent_id){

	if (!servMgr){
	    LOGE("nativeDisconnectServent: servMgr is NULL");
    	return JNI_FALSE;
    }
	Servent *s = servMgr->findServentByServentID(servent_id);
	if (!s){
		LOGE("nativeDisconnectServent: servent not found. (servent_id=%d)",
						servent_id);
		return JNI_FALSE;
	}

	s->thread.active = false;
	// COUT切断
	if (s->type == Servent::T_COUT)
		s->thread.finish = true;

	return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_org_peercast_core_PeerCastService_nativeClassInit(
		JNIEnv *env, jclass jclz) {
	gPeerCastServiceCache.initClass(env, jclz);
	gPeerCastServiceCache.initIDs(env);
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
	JNIEnv* env;
	if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
		return -1;
	}
	LOGI("libpeercast: Build(%s %s)", __DATE__, __TIME__);

	sJVM = vm;
	::registerThreadShutdownFunc(vm);

	gBundleCache.initClass(env, "android/os/Bundle");
	gBundleCache.initIDs(env);

	return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
JNI_OnUnload(JavaVM *vm, void *reserved) {
	// Androidでは呼ばれないらしい。
}

