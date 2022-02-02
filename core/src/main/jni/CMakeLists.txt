cmake_minimum_required(VERSION 3.10)
project(PeerCastForAndroid)

if (CMAKE_CURRENT_BINARY_DIR MATCHES "[dD]ebug")
    add_compile_options("-DADEBUG")
endif()


set(YT_SRC_DIR ${CMAKE_SOURCE_DIR}/peercast-yt-android)

add_library(peercast SHARED
        ${YT_SRC_DIR}/core/common/LUrlParser.cpp
        ${YT_SRC_DIR}/core/common/_string.cpp
        ${YT_SRC_DIR}/core/common/amf0.cpp
        ${YT_SRC_DIR}/core/common/assets.cpp
        ${YT_SRC_DIR}/core/common/cgi.cpp
        ${YT_SRC_DIR}/core/common/chandir.cpp
        ${YT_SRC_DIR}/core/common/chanhit.cpp
        ${YT_SRC_DIR}/core/common/chaninfo.cpp
        ${YT_SRC_DIR}/core/common/chanmgr.cpp
        ${YT_SRC_DIR}/core/common/channel.cpp
        ${YT_SRC_DIR}/core/common/chanpacket.cpp
        ${YT_SRC_DIR}/core/common/common.cpp
        ${YT_SRC_DIR}/core/common/cookie.cpp
        ${YT_SRC_DIR}/core/common/cstream.cpp
        ${YT_SRC_DIR}/core/common/dechunker.cpp
        ${YT_SRC_DIR}/core/common/env.cpp
        ${YT_SRC_DIR}/core/common/flag.cpp
        ${YT_SRC_DIR}/core/common/flv.cpp
        ${YT_SRC_DIR}/core/common/gnuid.cpp
        ${YT_SRC_DIR}/core/common/host.cpp
        ${YT_SRC_DIR}/core/common/hostgraph.cpp
        ${YT_SRC_DIR}/core/common/html.cpp
        ${YT_SRC_DIR}/core/common/http.cpp
        ${YT_SRC_DIR}/core/common/httppush.cpp
        ${YT_SRC_DIR}/core/common/icy.cpp
        ${YT_SRC_DIR}/core/common/ini.cpp
        ${YT_SRC_DIR}/core/common/inifile.cpp
        ${YT_SRC_DIR}/core/common/jis.cpp
        ${YT_SRC_DIR}/core/common/jrpc.cpp
        ${YT_SRC_DIR}/core/common/logbuf.cpp
        ${YT_SRC_DIR}/core/common/mapper.cpp
        ${YT_SRC_DIR}/core/common/md5.cpp
        ${YT_SRC_DIR}/core/common/mkv.cpp
        ${YT_SRC_DIR}/core/common/mms.cpp
        ${YT_SRC_DIR}/core/common/mp3.cpp
        ${YT_SRC_DIR}/core/common/mp4.cpp
        ${YT_SRC_DIR}/core/common/notif.cpp
        ${YT_SRC_DIR}/core/common/nsv.cpp
        ${YT_SRC_DIR}/core/common/ogg.cpp
        ${YT_SRC_DIR}/core/common/pcp.cpp
        ${YT_SRC_DIR}/core/common/peercast.cpp
        ${YT_SRC_DIR}/core/common/playlist.cpp
        ${YT_SRC_DIR}/core/common/portcheck.cpp
        ${YT_SRC_DIR}/core/common/public.cpp
        ${YT_SRC_DIR}/core/common/regexp.cpp
        ${YT_SRC_DIR}/core/common/rtmp.cpp
        ${YT_SRC_DIR}/core/common/rtmpmonit.cpp
        ${YT_SRC_DIR}/core/common/servent.cpp
        ${YT_SRC_DIR}/core/common/servfilter.cpp
        ${YT_SRC_DIR}/core/common/servhs.cpp
        ${YT_SRC_DIR}/core/common/servmgr.cpp
        ${YT_SRC_DIR}/core/common/socket.cpp
        ${YT_SRC_DIR}/core/common/sslclientsocket.cpp
        ${YT_SRC_DIR}/core/common/sstream.cpp
        ${YT_SRC_DIR}/core/common/stats.cpp
        ${YT_SRC_DIR}/core/common/str.cpp
        ${YT_SRC_DIR}/core/common/stream.cpp
        ${YT_SRC_DIR}/core/common/subprog.cpp
        ${YT_SRC_DIR}/core/common/sys.cpp
        ${YT_SRC_DIR}/core/common/template.cpp
        ${YT_SRC_DIR}/core/common/uptest.cpp
        ${YT_SRC_DIR}/core/common/uri.cpp
        ${YT_SRC_DIR}/core/common/url.cpp
        ${YT_SRC_DIR}/core/common/varwriter.cpp
        ${YT_SRC_DIR}/core/common/wmhttp.cpp
        ${YT_SRC_DIR}/core/common/xml.cpp

        ${YT_SRC_DIR}/core/unix/usocket.cpp
#        ${YT_SRC_DIR}/core/unix/usubprog.cpp
        ${YT_SRC_DIR}/core/unix/usys.cpp
        ${YT_SRC_DIR}/core/unix/strerror.cpp

        asubprog.cpp
        PeerCastService.cpp
        JniHelper.cpp
)

target_include_directories(peercast PRIVATE
        ${YT_SRC_DIR}/core/common
        ${YT_SRC_DIR}/core
)

target_compile_definitions(peercast PRIVATE _UNIX _GNU_SOURCE)

find_package(openssl REQUIRED CONFIG)

target_link_libraries(
        peercast
        log
        openssl::ssl
        openssl::crypto
)


set(MINIUPNPC_SRC_DIR ${CMAKE_SOURCE_DIR}/miniupnp/miniupnpc/src)

add_library(miniupnp SHARED
        ${MINIUPNPC_SRC_DIR}/addr_is_reserved.c
        ${MINIUPNPC_SRC_DIR}/connecthostport.c
        ${MINIUPNPC_SRC_DIR}/igd_desc_parse.c
        ${MINIUPNPC_SRC_DIR}/minisoap.c
        ${MINIUPNPC_SRC_DIR}/minissdpc.c
        ${MINIUPNPC_SRC_DIR}/miniupnpc.c
        ${MINIUPNPC_SRC_DIR}/miniwget.c
        ${MINIUPNPC_SRC_DIR}/minixml.c
        ${MINIUPNPC_SRC_DIR}/minixmlvalid.c
        ${MINIUPNPC_SRC_DIR}/portlistingparse.c
        ${MINIUPNPC_SRC_DIR}/receivedata.c
        ${MINIUPNPC_SRC_DIR}/upnpcommands.c
        ${MINIUPNPC_SRC_DIR}/upnpdev.c
        ${MINIUPNPC_SRC_DIR}/upnperrors.c
        ${MINIUPNPC_SRC_DIR}/upnpreplyparse.c

        JniHelper.cpp
        MiniUpnp.cpp
        )

target_link_libraries(miniupnp log)

set_target_properties(miniupnp PROPERTIES LINKER_LANGUAGE CXX)

target_include_directories(miniupnp PRIVATE
        ${MINIUPNPC_SRC_DIR}/../include
        ${CMAKE_SOURCE_DIR}
        )