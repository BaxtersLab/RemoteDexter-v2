# Block G: minimal keep rules to avoid stripping JNI/native entrypoints.
-keepclasseswithmembernames class * {
	native <methods>;
}

# Keep frame bridge and session integration classes used by backend/runtime reflection paths.
-keep class com.rd.mobile.render.RustDeskFrameBridge { *; }
-keep class com.rd.mobile.session.AndroidSessionManager { *; }
