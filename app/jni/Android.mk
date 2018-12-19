#https://developer.android.com/ndk/guides/android_mk?hl=zh-cn
LOCAL_PATH := $(call my-dir)    #设置当前的编译目录（Android.mk所在的目录）
include $(CLEAR_VARS)   #清除LOCAL_XX变量（LOCAL_PATH除外）
LOCAL_CPP_EXTENSION := .cc  #可以使用此可选变量为 C++ 源文件指明 .cpp 以外的文件扩展名。

#编译模块需要的源文件
LOCAL_SRC_FILES := \
source/compare_common.cc \
source/compare_gcc.cc \
source/compare_neon64.cc \
source/compare_win.cc \
source/compare.cc \
source/convert_argb.cc \
source/convert_from_argb.cc \
source/convert_from.cc \
source/convert_jpeg.cc \
source/convert_to_argb.cc \
source/convert_to_i420.cc \
source/convert.cc \
source/cpu_id.cc \
source/mjpeg_decoder.cc \
source/mjpeg_validate.cc \
source/planar_functions.cc \
source/rotate_any.cc \
source/rotate_argb.cc \
source/rotate_common.cc \
source/rotate_gcc.cc \
source/rotate_mips.cc \
source/rotate_neon64.cc \
source/rotate_win.cc \
source/rotate.cc \
source/row_any.cc \
source/row_common.cc \
source/row_gcc.cc \
source/row_mips.cc \
source/row_neon64.cc \
source/row_win.cc \
source/scale_any.cc \
source/scale_argb.cc \
source/scale_common.cc \
source/scale_gcc.cc \
source/scale_mips.cc \
source/scale_neon64.cc \
source/scale_win.cc \
source/scale.cc \
source/video_common.cc

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_CFLAGS += -DLIBYUV_NEON
    LOCAL_SRC_FILES += \
        source/compare_neon.cc.neon    \
        source/rotate_neon.cc.neon     \
        source/row_neon.cc.neon        \
        source/scale_neon.cc.neon
endif

LOCAL_SRC_FILES += \
yuvutils.c

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include    #此变量与 LOCAL_EXPORT_CFLAGS 相同，但适用于 C include 路径。
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include   #可以使用此可选变量指定相对于 NDK root 目录的路径列表，以便在编译所有源文件（C、C++ 和 Assembly）时添加到 include 搜索路径。

LOCAL_MODULE 	:= libyuv   #指定当前编译模块的名称
LOCAL_MODULE_TAGS := optional
LOCAL_LDLIBS 	:= -ljnigraphics -llog  #此变量包含在构建共享库或可执行文件时要使用的其他链接器标志列表.

include $(BUILD_SHARED_LIBRARY) #指定编译出的库类型，BUILD_SHARED_LIBRARY：动态库；BUILD_STATIC_LIBRARY：静态库， BUILD_EXECUTEABLE指：可执行文件