cmake_minimum_required(VERSION 3.6)

set(YT_SRC_DIR ${CMAKE_SOURCE_DIR}/peercast-yt-android)

add_library(peercast SHARED

        ${YT_SRC_DIR}/core/common/cgi.cpp
        ${YT_SRC_DIR}/core/common/chandir.cpp
        ${YT_SRC_DIR}/core/common/chanhit.cpp
        ${YT_SRC_DIR}/core/common/chaninfo.cpp
        ${YT_SRC_DIR}/core/common/chanmgr.cpp
        ${YT_SRC_DIR}/core/common/channel.cpp
        ${YT_SRC_DIR}/core/common/common.cpp
        ${YT_SRC_DIR}/core/common/cstream.cpp
        ${YT_SRC_DIR}/core/common/dechunker.cpp
        ${YT_SRC_DIR}/core/common/env.cpp
        ${YT_SRC_DIR}/core/common/flv.cpp
        ${YT_SRC_DIR}/core/common/flag.cpp
        ${YT_SRC_DIR}/core/common/gnuid.cpp
        ${YT_SRC_DIR}/core/common/host.cpp
        ${YT_SRC_DIR}/core/common/html.cpp
        ${YT_SRC_DIR}/core/common/http.cpp
        ${YT_SRC_DIR}/core/common/httppush.cpp
        ${YT_SRC_DIR}/core/common/hostgraph.cpp
        ${YT_SRC_DIR}/core/common/icy.cpp
        ${YT_SRC_DIR}/core/common/ini.cpp
        ${YT_SRC_DIR}/core/common/inifile.cpp
        ${YT_SRC_DIR}/core/common/jis.cpp
        ${YT_SRC_DIR}/core/common/jrpc.cpp
        ${YT_SRC_DIR}/core/common/logbuf.cpp
        ${YT_SRC_DIR}/core/common/LUrlParser.cpp
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
        ${YT_SRC_DIR}/core/common/public.cpp
        ${YT_SRC_DIR}/core/common/regexp.cpp
        ${YT_SRC_DIR}/core/common/rtmp.cpp
        ${YT_SRC_DIR}/core/common/rtmpmonit.cpp
        ${YT_SRC_DIR}/core/common/servent.cpp
        ${YT_SRC_DIR}/core/common/servhs.cpp
        ${YT_SRC_DIR}/core/common/servmgr.cpp
        ${YT_SRC_DIR}/core/common/socket.cpp
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
        ${YT_SRC_DIR}/core/common/_string.cpp
        ${YT_SRC_DIR}/core/common/assets.cpp
        ${YT_SRC_DIR}/core/common/servfilter.cpp
        ${YT_SRC_DIR}/core/common/portcheck.cpp
        ${YT_SRC_DIR}/core/common/cookie.cpp
        ${YT_SRC_DIR}/core/common/chanpacket.cpp

        ${YT_SRC_DIR}/core/unix/usocket.cpp
#        ${YT_SRC_DIR}/core/unix/usubprog.cpp
        ${YT_SRC_DIR}/core/unix/usys.cpp

        asubprog.cpp
        PeerCastService.cpp
        )

include_directories(
        ${YT_SRC_DIR}/core/common
        ${YT_SRC_DIR}/core
)

set(YT_REVISION "unknown")
find_package(Git)
if(GIT_FOUND)
    execute_process(
            COMMAND ${GIT_EXECUTABLE} rev-parse --short HEAD
            WORKING_DIRECTORY ${YT_SRC_DIR}
            OUTPUT_VARIABLE YT_REVISION
            ERROR_QUIET
            OUTPUT_STRIP_TRAILING_WHITESPACE
    )
endif()

add_definitions(-D_UNIX -D_GNU_SOURCE -DYT_REVISION="${YT_REVISION}")

target_link_libraries(
        peercast
        log
)

