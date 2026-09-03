# The Glyph SDK is a vendored, closed-source AAR reached over AIDL. R8 has no way to see the
# reflective/binder entry points, so keep it whole rather than debug obfuscated IPC failures.
-keep class com.nothing.ketchum.** { *; }
-keep class com.nothing.thirdparty.** { *; }

# Toy services are instantiated by Nothing OS from the manifest, never from our code.
-keep class dev.omesh.glyphtoys.toys.** extends android.app.Service { *; }
