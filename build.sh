#!/usr/bin/env bash
# Builds and signs Mic2Assistant.apk without Gradle (JDK + Android SDK build-tools).
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_SDK:-$(cygpath -u "$LOCALAPPDATA")/Android/Sdk}"
BT="$SDK/build-tools/37.0.0"
JAR="$SDK/platforms/android-36.1/android.jar"
JDK_BIN="${JDK_BIN:-/c/Program Files/Java/jdk-22/bin}"
export PATH="$JDK_BIN:$PATH"
w() { cygpath -w "$1"; }

rm -rf build && mkdir -p build/classes build/dex

# 1. Manifest -> base APK
"$BT/aapt2.exe" link -o build/base.apk -I "$(w "$JAR")" \
  --manifest AndroidManifest.xml --min-sdk-version 21 --target-sdk-version 25

# 2. Java -> classes -> dex
javac --release 8 -nowarn -Xlint:-options -cp "$(w "$JAR")" -d build/classes \
  src/local/mic2assistant/*.java
cmd //c "$(w "$BT")\\d8.bat --min-api 21 --lib $(w "$JAR") --output $(w build/dex) $(w build/classes/local/mic2assistant)\\*.class"

# 3. Add classes.dex, align, sign
cp build/base.apk build/unsigned.apk
(cd build/dex && jar uMf ../unsigned.apk classes.dex)
"$BT/zipalign.exe" -f 4 build/unsigned.apk build/aligned.apk

KS=debug.keystore
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass mic2assistant -keypass mic2assistant \
    -alias mic2assistant -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Mic2Assistant" >/dev/null 2>&1
fi
cmd //c "$(w "$BT")\\apksigner.bat sign --ks $KS --ks-pass pass:mic2assistant --out $(w Mic2Assistant.apk) $(w build/aligned.apk)"
echo "Built: $(pwd)/Mic2Assistant.apk"
