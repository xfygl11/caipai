# WebView and JS interface
-keepclassmembers class webapp.newcloud.lottery.movie.WebAppActivity$JavaScriptInterface {
    *;
}
-keepclassmembers class webapp.newcloud.lottery.movie.player.ExoPlayerActivity$PlayerBridge {
    *;
}

# Keep NativeHttp implementation
-keep class webapp.newcloud.lottery.movie.NativeHttp { *; }

# ProGuard optimization hints
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
