# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase

# Coil
-dontwarn coil.**

# Lottie
-keep class com.airbnb.lottie.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Gson
-keep class com.google.gson.TypeAdapter
-keep class com.google.gson.TypeAdapterFactory
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Hilt
-keepnames class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <methods>;
}

# Data classes
-keep class com.copaarena.app.data.** { *; }
-keep class com.copaarena.app.domain.model.** { *; }
