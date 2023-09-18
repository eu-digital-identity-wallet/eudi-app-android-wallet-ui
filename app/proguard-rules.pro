-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# Retrofit
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Couroutines
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# API Models
-keep class ceu.europa.ec.networklogic.model.** { *; }

# Enum
-keep enum * { *; }

# UI Config
-keep interface eu.europa.ec.uilogic.utilities.serializer.UiSerializableParser
-keep interface eu.europa.ec.uilogic.utilities.serializer.UiSerializable
-keepclassmembers class * implements eu.europa.ec.uilogic.utilities.serializer.UiSerializableParser { *; }
-keepclassmembers class * implements eu.europa.ec.uilogic.utilities.serializer.UiSerializable { *; }