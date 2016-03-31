

#APP_ABI := all 
APP_ABI := armeabi armeabi-v7a x86 mips
#APP_ABI := arm64-v8a x86_64 mips64

#
# どうも64bit-arm版で終了時に意味のわからないクラッシュがある
#

APP_PLATFORM := android-15


APP_OPTIM := release
#APP_OPTIM := debug

NDK_TOOLCHAIN_VERSION := 4.9
#NDK_TOOLCHAIN_VERSION := clang

