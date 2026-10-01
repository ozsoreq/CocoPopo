#!/bin/bash
# Builds CocoPopo.apk without Gradle, using the classic Android SDK command-line tools
# (aapt, dx, zipalign, apksigner + an android.jar). On Debian/Ubuntu:
#   sudo apt-get install aapt apksigner dalvik-exchange zipalign android-sdk-platform-23
set -euo pipefail
cd "$(dirname "$0")"

SDK=${ANDROID_SDK:-/usr/lib/android-sdk}
ANDROID_JAR=${ANDROID_JAR:-$SDK/platforms/android-23/android.jar}
B=build/apk
rm -rf $B && mkdir -p $B/classes release

echo "[1/6] compiling"
javac -nowarn -source 8 -target 8 -Xlint:-options -bootclasspath "$ANDROID_JAR" -d $B/classes \
  $(find app/src/main/java -name '*.java') 2>&1 | grep -v "^Picked up" || true

echo "[2/6] dexing"
dalvik-exchange --dex --output=$B/classes.dex $B/classes 2>&1 | grep -v "^Picked up" || true

echo "[3/6] packaging resources"
aapt package -f -M app/src/main/AndroidManifest.xml -S app/src/main/res -I "$ANDROID_JAR" -F $B/unsigned.apk
(cd $B && aapt add unsigned.apk classes.dex >/dev/null)

echo "[4/6] aligning"
zipalign -f 4 $B/unsigned.apk $B/aligned.apk

echo "[5/6] signing"
KS=${KEYSTORE:-build/cocopopo.keystore}
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass cocopopo -keypass cocopopo -alias cocopopo \
    -keyalg RSA -keysize 2048 -validity 36500 -dname "CN=CocoPopo" 2>&1 | grep -v "^Picked up" || true
fi
apksigner sign --ks "$KS" --ks-pass pass:cocopopo --key-pass pass:cocopopo --out release/CocoPopo.apk $B/aligned.apk 2>&1 | grep -v "^Picked up" || true

echo "[6/6] verifying"
apksigner verify release/CocoPopo.apk 2>&1 | grep -v "^Picked up"
ls -la release/CocoPopo.apk
