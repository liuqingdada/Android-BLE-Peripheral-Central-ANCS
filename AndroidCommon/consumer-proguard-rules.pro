# Import proguard options from deprecated "android-sdk/tools/proguard/proguard-android.txt"
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontoptimize
-dontpreverify

# common ProGuard rules for all projects
-keepattributes SourceFile,LineNumberTable

# Keep classes for JSON serialize/deserialize
-keep class com.android.common.json.JsonBean
-keepclassmembers class * implements com.android.common.json.JsonBean {
    <fields>;
    # needed by inner class
    <init>();
}

# Understand the @Keep support annotation.
-keep class androidx.annotation.Keep

-keep @androidx.annotation.Keep class * {*;}

-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}

-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}

-keepclasseswithmembers class * {
    @androidx.annotation.Keep <init>(...);
}

# We class names for Parcelable
-keep class * implements android.os.Parcelable

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

###### ignore known warnings ######

# GMS
-dontwarn com.google.android.gms.**
-dontwarn com.mobvoi.android.wearable.WearableListenerServiceGoogleImpl

# guava
-dontwarn com.google.common.util.concurrent.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn sun.misc.Unsafe
-dontnote com.google.common.**

# fastjson
-keepattributes Signature
-dontnote com.alibaba.fastjson.**
-dontwarn com.alibaba.fastjson.**

# Speech SDK jni
-dontnote com.mobvoi.be.speech.recognizer.jni.**
-dontnote com.mobvoi.be.speech.speex.jni.**
-dontnote com.mobvoi.be.speech.hotword.jni.**
-dontnote com.mobvoi.be.speech.vad.jni.**

# rxjava & rxandroid
-dontwarn rx.internal.**
-dontnote rx.internal.**
-dontwarn android.os.**
-dontnote rx.Subscriber
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode producerNode;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueConsumerNodeRef {
    rx.internal.util.atomic.LinkedQueueNode consumerNode;
}

# Android Support Library
-dontnote android.support.**
-dontwarn android.support.**

# OkHttp
-dontwarn okhttp3.**
-dontnote okhttp3.**
-dontwarn okio.**

# MQTT
-dontwarn rg.eclipse.paho.client.mqttv3.**
-dontnote org.eclipse.paho.client.mqttv3.**

# protobuf
-dontwarn com.google.protobuf.**
-dontnote com.google.protobuf.**

# Android Wear Support
-keep class android.support.wearable.** {*;}

# framework.jar
-dontnote android.**
-dontnote javax.**
-dontwarn android.**
-dontwarn com.android.internal.**

# this library
-dontnote com.mobvoi.android.common.internalapi.**

# wear-common
-dontwarn com.mobvoi.wear.util.TelephonyUtil
-dontwarn com.mobvoi.wear.util.WatchWifiHelper

# Google Services
-dontnote com.google.vending.licensing.ILicensingService
-dontnote com.android.vending.licensing.ILicensingService

# Apache http client
-dontwarn org.apache.http.**

# volley
-dontwarn com.android.volley.toolbox.**

# timber
-dontwarn org.jetbrains.annotations.**

# rxpermissions
# RxPermissions using API 23, witch is not support by Android Make, don't warn for it.
-dontwarn com.tbruyelle.rxpermissions.**

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder

# GreenDao rules
# Source: http://greendao-orm.com/documentation/technical-faq
#
-keepclassmembers class * extends de.greenrobot.dao.AbstractDao {
    public static java.lang.String TABLENAME;
}
-keep class **$Properties {
    public *;
}

# Retrofit
-dontwarn retrofit2.**
-dontnote retrofit2.**
-keep class retrofit2.** { *; }
# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keepattributes Signature
-keepattributes Exceptions

# Gson
# R8 will remove the fields which are annotated by SerializedName
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Java things
-dontnote sun.misc.Unsafe

