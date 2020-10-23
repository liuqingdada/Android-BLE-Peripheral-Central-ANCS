
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := mobvoi-common

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
	$(call all-Iaidl-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4-latest

include $(BUILD_STATIC_JAVA_LIBRARY)

