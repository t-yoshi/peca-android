-ignorewarnings

#必要-> デバッグが困難になる
-keepnames class ** { *; }

-keep class org.peercast.core.** { *; }
-keep class org.jsoup.** { *; }


#必要-> CastException: java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType
-keepattributes * #Signature

#native側から呼び出されるため必要
-keepclassmembers class org.peercast.core.PeerCastService {
    private <methods>;
}