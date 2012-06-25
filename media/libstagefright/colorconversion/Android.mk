LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:=                     \
        ColorConverter.cpp            \
        SoftwareRenderer.cpp

LOCAL_C_INCLUDES := \
        $(TOP)/frameworks/base/include/media/stagefright/openmax \
        $(TOP)/hardware/msm7k

LOCAL_MODULE:= libstagefright_color_conversion

ifneq ($(BOARD_WITHOUT_PIXEL_FORMAT_YV12),)
    LOCAL_CFLAGS += -DMISSING_EGL_PIXEL_FORMAT_YV12
endif

include $(BUILD_STATIC_LIBRARY)
