cmake_minimum_required(VERSION 3.6)

set(YT_SRC_DIR ${CMAKE_SOURCE_DIR}/peercast-yt-android)

add_library(peercast SHARED

        ${YT_SRC_DIR}/core/common/cgi.cpp
        ${YT_SRC_DIR}/core/common/chandir.cpp
        ${YT_SRC_DIR}/core/common/chanhit.cpp
        ${YT_SRC_DIR}/core/common/chaninfo.cpp
        ${YT_SRC_DIR}/core/common/chanmgr.cpp
        ${YT_SRC_DIR}/core/common/channel.cpp
        ${YT_SRC_DIR}/core/common/cstream.cpp
        ${YT_SRC_DIR}/core/common/dechunker.cpp
        ${YT_SRC_DIR}/core/common/env.cpp
        ${YT_SRC_DIR}/core/common/flv.cpp
        ${YT_SRC_DIR}/core/common/gnuid.cpp
        ${YT_SRC_DIR}/core/common/host.cpp
        ${YT_SRC_DIR}/core/common/html.cpp
        ${YT_SRC_DIR}/core/common/http.cpp
        ${YT_SRC_DIR}/core/common/httppush.cpp
        ${YT_SRC_DIR}/core/common/icy.cpp
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
        ${YT_SRC_DIR}/core/common/notif.cpp
        ${YT_SRC_DIR}/core/common/nsv.cpp
        ${YT_SRC_DIR}/core/common/ogg.cpp
        ${YT_SRC_DIR}/core/common/pcp.cpp
        ${YT_SRC_DIR}/core/common/peercast.cpp
        ${YT_SRC_DIR}/core/common/playlist.cpp
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

        ${YT_SRC_DIR}/core/unix/usocket.cpp
#        ${YT_SRC_DIR}/core/unix/usubprog.cpp
        ${YT_SRC_DIR}/core/unix/usys.cpp

        ${YT_SRC_DIR}/Onigmo/regcomp.c
        ${YT_SRC_DIR}/Onigmo/regenc.c
        ${YT_SRC_DIR}/Onigmo/regerror.c
        ${YT_SRC_DIR}/Onigmo/regexec.c
        ${YT_SRC_DIR}/Onigmo/regext.c
        ${YT_SRC_DIR}/Onigmo/reggnu.c
        ${YT_SRC_DIR}/Onigmo/regparse.c
        ${YT_SRC_DIR}/Onigmo/regposerr.c
        ${YT_SRC_DIR}/Onigmo/regposix.c
        ${YT_SRC_DIR}/Onigmo/regsyntax.c
        ${YT_SRC_DIR}/Onigmo/regtrav.c
        ${YT_SRC_DIR}/Onigmo/regversion.c
        ${YT_SRC_DIR}/Onigmo/st.c
        ${YT_SRC_DIR}/Onigmo/enc/ascii.c
        ${YT_SRC_DIR}/Onigmo/enc/big5.c
        ${YT_SRC_DIR}/Onigmo/enc/euc_jp.c
        ${YT_SRC_DIR}/Onigmo/enc/euc_kr.c
        ${YT_SRC_DIR}/Onigmo/enc/euc_tw.c
        ${YT_SRC_DIR}/Onigmo/enc/gb18030.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_1.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_10.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_11.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_13.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_14.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_15.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_16.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_2.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_3.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_4.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_5.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_6.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_7.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_8.c
        ${YT_SRC_DIR}/Onigmo/enc/iso_8859_9.c
        ${YT_SRC_DIR}/Onigmo/enc/koi8_r.c
        ${YT_SRC_DIR}/Onigmo/enc/koi8_u.c
        ${YT_SRC_DIR}/Onigmo/enc/mktable.c
        ${YT_SRC_DIR}/Onigmo/enc/shift_jis.c
        ${YT_SRC_DIR}/Onigmo/enc/unicode.c
        ${YT_SRC_DIR}/Onigmo/enc/utf_16be.c
        ${YT_SRC_DIR}/Onigmo/enc/utf_16le.c
        ${YT_SRC_DIR}/Onigmo/enc/utf_32be.c
        ${YT_SRC_DIR}/Onigmo/enc/utf_32le.c
        ${YT_SRC_DIR}/Onigmo/enc/utf_8.c
        ${YT_SRC_DIR}/Onigmo/enc/windows_1250.c
        ${YT_SRC_DIR}/Onigmo/enc/windows_1251.c
        ${YT_SRC_DIR}/Onigmo/enc/windows_1252.c
        ${YT_SRC_DIR}/Onigmo/enc/windows_1253.c
        ${YT_SRC_DIR}/Onigmo/enc/windows_1254.c
        ${YT_SRC_DIR}/Onigmo/enc/windows_1257.c
        ${YT_SRC_DIR}/Onigmo/enc/windows_31j.c

        asubprog.cpp
        PeerCastService.cpp
        )

include_directories(
        ${YT_SRC_DIR}/core/common
        ${YT_SRC_DIR}/core

        ${YT_SRC_DIR}/Onigmo
        ${YT_SRC_DIR}/Onigmo/enc/unicode
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

configure_file(${CMAKE_SOURCE_DIR}/Onigmo-config.h ${YT_SRC_DIR}/Onigmo/config.h COPYONLY)

target_link_libraries(
        peercast
        log
)

