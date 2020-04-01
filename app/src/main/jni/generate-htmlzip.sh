#!/bin/bash
set -e

base_dir=$(pwd)
asset_zip="$base_dir/../assets/peca-yt.zip"

if [ -f "$asset_zip" ]; then
  exit 0
fi

cd peercast-yt-android/ui

RUBYOPT='--disable-gems' ./generate-html
zip -J $asset_zip -r html/

cd linux
zip -J $asset_zip -r assets/

