#!/bin/bash
set -e

base_dir=$(pwd)
asset_zip="$base_dir/../assets/peca-yt.zip"

if [ -f "$asset_zip" ]; then
  rm "$asset_zip"
fi

if ! [ -d peercast-yt-android/ui ]; then
  #jitpackでのビルドに支障が出るので
  echo "peercast-yt-android/ui not found."
  exit
fi
cd peercast-yt-android/ui

RUBYOPT='--disable-gems' ./generate-html
zip -J $asset_zip -r html/

zip -J $asset_zip -r assets/

