#!/usr/bin/env bash
# Build the signed release APK on the remote build host and copy it back.
# Usage: scripts/build-remote.sh [--install]
set -euo pipefail

HOST="${BUILD_HOST:-202.201.1.132}"
REMOTE_DIR="${REMOTE_DIR:-dev/clash-traffic-monitor}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo ">> Syncing sources to $HOST:$REMOTE_DIR"
ssh "$HOST" "mkdir -p $REMOTE_DIR"
rsync -az --delete \
  --exclude .git --exclude build --exclude .gradle --exclude dist \
  --exclude local.properties --exclude '*.jks' \
  ./ "$HOST:$REMOTE_DIR/"

echo ">> Building on $HOST"
ssh "$HOST" bash -s -- "$REMOTE_DIR" <<'REMOTE'
set -euo pipefail
cd "$1"
SDK="$HOME/Android/Sdk"
echo "sdk.dir=$SDK" > local.properties

# One-time release keystore, kept outside the repo.
KS_DIR="$HOME/keystores"
PROPS="$KS_DIR/clash-traffic-monitor.properties"
if [ ! -f "$PROPS" ]; then
  mkdir -p "$KS_DIR"; chmod 700 "$KS_DIR"
  PASS="$(head -c 24 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 24)"
  keytool -genkeypair -noprompt -keystore "$KS_DIR/clash-traffic-monitor.jks" \
    -alias clashmonitor -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$PASS" -keypass "$PASS" \
    -dname "CN=Clash Traffic Monitor, O=zenkexi"
  cat > "$PROPS" <<P
storeFile=$KS_DIR/clash-traffic-monitor.jks
storePassword=$PASS
keyAlias=clashmonitor
keyPassword=$PASS
P
  chmod 600 "$PROPS" "$KS_DIR/clash-traffic-monitor.jks"
  echo ">> Generated keystore $KS_DIR/clash-traffic-monitor.jks"
fi

# Bootstrap the Gradle wrapper jar from the cached distribution if missing.
if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  G="$(ls -d "$HOME"/.gradle/wrapper/dists/gradle-8.10.2-bin/*/gradle-8.10.2 | head -1)/bin/gradle"
  "$G" --no-daemon -q wrapper --gradle-version 8.10.2
fi

./gradlew --no-daemon testReleaseUnitTest assembleRelease

APK=app/build/outputs/apk/release/app-release.apk
BT="$SDK/build-tools/35.0.1"
"$BT/apksigner" verify --print-certs "$APK" | head -3
"$BT/aapt" dump badging "$APK" | grep -E "^package|sdkVersion|targetSdk"
REMOTE

mkdir -p dist gradle/wrapper
scp -q "$HOST:$REMOTE_DIR/app/build/outputs/apk/release/app-release.apk" dist/clash-traffic-monitor.apk
for f in gradlew gradle/wrapper/gradle-wrapper.jar; do
  [ -f "$f" ] || scp -q "$HOST:$REMOTE_DIR/$f" "$f"
done
chmod +x gradlew 2>/dev/null || true
echo ">> APK: $ROOT/dist/clash-traffic-monitor.apk"

if [ "${1:-}" = "--install" ]; then
  adb install -r dist/clash-traffic-monitor.apk
fi
