#!/bin/bash
set -e

base_dir=$(pwd)
asset_zip="$base_dir/../assets/peca-yt.zip"
revision_txt="$base_dir/../assets/revision.txt"

cd peercast-yt-android/ui

[ -f "$revision_txt" ] && revision_old=$(cat $revision_txt)
revision_new=$(git log -n 1 --format=%H)

#echo "$revision_old"
#echo "$revision_new"

if [ -f "$asset_zip" ] && [ "$revision_old" = "$revision_new" ]; then
  echo "peca-yt's revision is not changed."
  exit
fi

echo -n $revision_new > "$revision_txt"

[ -f "$asset_zip" ] && rm "$asset_zip"

RUBYOPT='--disable-gems' ./generate-html
zip -J $asset_zip -r html/

zip -J $asset_zip -r assets/

