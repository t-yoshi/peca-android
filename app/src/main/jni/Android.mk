LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := peercast

_CORE=PeerCast-IM4U/core

#ƒrƒ‹ƒh•û–@:
#ndk-build V=1 NDK_DEBUG=1 TARGET_PLATFORM=android-15
#



_CORESOURCE = $(_CORE)/unix/usys.cpp \
	 $(_CORE)/unix/usocket.cpp \
	 $(_CORE)/common/socket.cpp \
	 $(_CORE)/common/servent.cpp \
	 $(_CORE)/common/servhs.cpp \
	 $(_CORE)/common/servmgr.cpp \
	 $(_CORE)/common/xml.cpp \
	 $(_CORE)/common/stream.cpp \
	 $(_CORE)/common/sys.cpp \
	 $(_CORE)/common/gnutella.cpp \
	 $(_CORE)/common/html.cpp \
	 $(_CORE)/common/channel.cpp \
	 $(_CORE)/common/http.cpp \
	 $(_CORE)/common/inifile.cpp \
	 $(_CORE)/common/peercast.cpp \
	 $(_CORE)/common/stats.cpp \
	 $(_CORE)/common/mms.cpp \
	 $(_CORE)/common/mp3.cpp \
	 $(_CORE)/common/nsv.cpp \
	 $(_CORE)/common/flv.cpp \
	 $(_CORE)/common/ogg.cpp \
	 $(_CORE)/common/url.cpp \
	 $(_CORE)/common/icy.cpp \
	 $(_CORE)/common/pcp.cpp \
	 $(_CORE)/java/thread.cpp \
	 $(_CORE)/common/jis.cpp 
	 

	 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(_CORE) \
					$(LOCAL_PATH)/$(_CORE)/common \
					$(LOCAL_PATH)/$(_CORE)/java
LOCAL_CPPFLAGS := -pthread -DENABLE_BINRELOC -D_UNIX -D_JAVA -D_REENTRANT					
LOCAL_CPP_FEATURES += exceptions



#ifeq ($(APP_OPTIM),debug)
#	LOCAL_CPPFLAGS += -O0
#else
#	LOCAL_CPPFLAGS += -Os
#endif


LOCAL_SRC_FILES := $(_CORESOURCE) \
	 org_peercast_core_PeerCastService.cpp


LOCAL_LDLIBS := -llog 


include $(BUILD_SHARED_LIBRARY)



