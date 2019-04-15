cmake_minimum_required(VERSION 3.6)


add_library(peercast SHARED

        peercast-yt/core/common/cgi.cpp
        peercast-yt/core/common/chandir.cpp
        peercast-yt/core/common/chanhit.cpp
        peercast-yt/core/common/chaninfo.cpp
        peercast-yt/core/common/chanmgr.cpp
        peercast-yt/core/common/channel.cpp
        peercast-yt/core/common/cstream.cpp
        peercast-yt/core/common/dechunker.cpp
        peercast-yt/core/common/env.cpp
        peercast-yt/core/common/flv.cpp
        peercast-yt/core/common/gnuid.cpp
        peercast-yt/core/common/gnutella.cpp
        peercast-yt/core/common/host.cpp
        peercast-yt/core/common/html.cpp
        peercast-yt/core/common/http.cpp
        peercast-yt/core/common/httppush.cpp
        peercast-yt/core/common/icy.cpp
        peercast-yt/core/common/inifile.cpp
        peercast-yt/core/common/jis.cpp
        peercast-yt/core/common/jrpc.cpp
        peercast-yt/core/common/logbuf.cpp
        peercast-yt/core/common/LUrlParser.cpp
        peercast-yt/core/common/mapper.cpp
        peercast-yt/core/common/md5.cpp
        peercast-yt/core/common/mkv.cpp
        peercast-yt/core/common/mms.cpp
        peercast-yt/core/common/mp3.cpp
        peercast-yt/core/common/notif.cpp
        peercast-yt/core/common/nsv.cpp
        peercast-yt/core/common/ogg.cpp
        peercast-yt/core/common/pcp.cpp
        peercast-yt/core/common/peercast.cpp
        peercast-yt/core/common/playlist.cpp
        peercast-yt/core/common/public.cpp
        peercast-yt/core/common/regexp.cpp
        peercast-yt/core/common/rtmp.cpp
        peercast-yt/core/common/rtmpmonit.cpp
        peercast-yt/core/common/servent.cpp
        peercast-yt/core/common/servhs.cpp
        peercast-yt/core/common/servmgr.cpp
        peercast-yt/core/common/socket.cpp
        peercast-yt/core/common/sstream.cpp
        peercast-yt/core/common/stats.cpp
        peercast-yt/core/common/str.cpp
        peercast-yt/core/common/stream.cpp
        peercast-yt/core/common/subprog.cpp
        peercast-yt/core/common/sys.cpp
        peercast-yt/core/common/template.cpp
        peercast-yt/core/common/uptest.cpp
        peercast-yt/core/common/uri.cpp
        peercast-yt/core/common/url.cpp
        peercast-yt/core/common/varwriter.cpp
        peercast-yt/core/common/wmhttp.cpp
        peercast-yt/core/common/xml.cpp
        peercast-yt/core/common/_string.cpp

        peercast-yt/core/unix/usocket.cpp
#        peercast-yt/core/unix/usubprog.cpp
        peercast-yt/core/unix/usys.cpp

        peercast-yt/Onigmo/regcomp.c
        peercast-yt/Onigmo/regenc.c
        peercast-yt/Onigmo/regerror.c
        peercast-yt/Onigmo/regexec.c
        peercast-yt/Onigmo/regext.c
        peercast-yt/Onigmo/reggnu.c
        peercast-yt/Onigmo/regparse.c
        peercast-yt/Onigmo/regposerr.c
        peercast-yt/Onigmo/regposix.c
        peercast-yt/Onigmo/regsyntax.c
        peercast-yt/Onigmo/regtrav.c
        peercast-yt/Onigmo/regversion.c
        peercast-yt/Onigmo/st.c
        peercast-yt/Onigmo/enc/ascii.c
        peercast-yt/Onigmo/enc/big5.c
        peercast-yt/Onigmo/enc/euc_jp.c
        peercast-yt/Onigmo/enc/euc_kr.c
        peercast-yt/Onigmo/enc/euc_tw.c
        peercast-yt/Onigmo/enc/gb18030.c
        peercast-yt/Onigmo/enc/iso_8859_1.c
        peercast-yt/Onigmo/enc/iso_8859_10.c
        peercast-yt/Onigmo/enc/iso_8859_11.c
        peercast-yt/Onigmo/enc/iso_8859_13.c
        peercast-yt/Onigmo/enc/iso_8859_14.c
        peercast-yt/Onigmo/enc/iso_8859_15.c
        peercast-yt/Onigmo/enc/iso_8859_16.c
        peercast-yt/Onigmo/enc/iso_8859_2.c
        peercast-yt/Onigmo/enc/iso_8859_3.c
        peercast-yt/Onigmo/enc/iso_8859_4.c
        peercast-yt/Onigmo/enc/iso_8859_5.c
        peercast-yt/Onigmo/enc/iso_8859_6.c
        peercast-yt/Onigmo/enc/iso_8859_7.c
        peercast-yt/Onigmo/enc/iso_8859_8.c
        peercast-yt/Onigmo/enc/iso_8859_9.c
        peercast-yt/Onigmo/enc/koi8_r.c
        peercast-yt/Onigmo/enc/koi8_u.c
        peercast-yt/Onigmo/enc/mktable.c
        peercast-yt/Onigmo/enc/shift_jis.c
        peercast-yt/Onigmo/enc/unicode.c
        peercast-yt/Onigmo/enc/utf_16be.c
        peercast-yt/Onigmo/enc/utf_16le.c
        peercast-yt/Onigmo/enc/utf_32be.c
        peercast-yt/Onigmo/enc/utf_32le.c
        peercast-yt/Onigmo/enc/utf_8.c
        peercast-yt/Onigmo/enc/windows_1250.c
        peercast-yt/Onigmo/enc/windows_1251.c
        peercast-yt/Onigmo/enc/windows_1252.c
        peercast-yt/Onigmo/enc/windows_1253.c
        peercast-yt/Onigmo/enc/windows_1254.c
        peercast-yt/Onigmo/enc/windows_1257.c
        peercast-yt/Onigmo/enc/windows_31j.c

        asubprog.cpp
        PeerCastService.cpp
        )

include_directories(
        peercast-yt/core/common
        peercast-yt/core

        peercast-yt/Onigmo
        peercast-yt/Onigmo/enc/unicode
)

add_definitions(-D_UNIX -D_GNU_SOURCE)

target_link_libraries(
        peercast
        log
)

