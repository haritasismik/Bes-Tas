# ==================================================
# Beş Taş - ProGuard / R8 Kuralları
# ==================================================

# --- Genel Kurallar ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Uygulama Modelleri (Firebase serialization için) ---
-keep class com.haritasismik.bestas.game.models.** { *; }
-keep class com.haritasismik.bestas.data.firebase.RoomState { *; }
-keep class com.haritasismik.bestas.data.firebase.GameMove { *; }
-keep class com.haritasismik.bestas.data.firebase.LeaderboardEntry { *; }
-keep class com.haritasismik.bestas.data.firebase.PlayerStats { *; }

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- Jetpack Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# --- Kotlin Serialization ---
-keepattributes RuntimeVisibleAnnotations

# --- Enum koruma ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Data class koruma (Firebase için gerekli) ---
-keepclassmembers class * {
    public <init>(...);
}

# --- Hata raporlama (Crashlytics uyumluluğu) ---
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
