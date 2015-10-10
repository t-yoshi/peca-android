#!/bin/bash

set -e

ANDROID_SDK=/opt/android/sdk

ANDROID_JAR=$ANDROID_SDK/sdk/platforms/android-15/android.jar


CLASS_PATH=../bin/classes:$ANDROID_JAR
CLASS_NAME=org.peercast.core.PeerCastService


javah  -classpath $CLASS_PATH $CLASS_NAME

#
#
#
