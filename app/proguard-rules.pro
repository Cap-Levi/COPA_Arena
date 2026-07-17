# R8's optimize+obfuscate passes together break something in the ComponentActivity /
# Compose lifecycle-owner attach chain in release builds — crashes on launch with
# "CompositionLocal LocalLifecycleOwner not present". Neither flag alone was proven
# sufficient in isolation testing on this codebase; both off is the verified-working
# configuration. Shrinking (dead code/resource removal) still runs.
-dontoptimize
-dontobfuscate

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

# R8 (full or compat mode) breaks the ComponentActivity -> ReportFragment -> ViewTree
# lifecycle-owner attachment chain in release builds — crashes on launch with
# "CompositionLocal LocalLifecycleOwner not present" (Lifecycle classes alone weren't
# enough; the actual break is somewhere in how the Activity/Fragment/SavedState glue
# gets attached, since minifyEnabled=false has zero repro and this class of AndroidX
# platform-glue code is invoked reflectively/via FragmentManager, not a call graph R8
# can trace statically). Keeping the whole chain intact costs negligible size.
-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }
-keepclassmembers class androidx.lifecycle.** { *; }
-keep class androidx.activity.** { *; }
-keep interface androidx.activity.** { *; }
-keep class androidx.fragment.** { *; }
-keep interface androidx.fragment.** { *; }
-keep class androidx.savedstate.** { *; }
-keep interface androidx.savedstate.** { *; }

# Data classes
-keep class com.copaarena.app.data.** { *; }
-keep class com.copaarena.app.domain.model.** { *; }
