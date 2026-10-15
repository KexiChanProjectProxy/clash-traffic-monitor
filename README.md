# Clash Traffic Monitor

Landscape Android dashboard (Android 10+) for a Clash / mihomo external controller.
Shows live download and upload speed, active connections, and new connections per second,
each with a 60-second sparkline. Material 3, follows system dark mode (dynamic color on
Android 12+), keeps the screen on while open.

## Build

Builds on the remote host `202.201.1.132` (Java 17 + Android SDK there):

    scripts/build-remote.sh            # -> dist/clash-traffic-monitor.apk
    scripts/build-remote.sh --install  # also adb install to a connected device

The release keystore lives on the build host at `~/keystores/clash-traffic-monitor.jks`
with its passwords in `~/keystores/clash-traffic-monitor.properties`. Back these up:
updates must be signed with the same key.

## Test without a real Clash

    python3 tools/fake_clash_api.py --port 9090 [--secret abc]

Point the app at `<this machine's IP>:9090`.

Dashboard screenshots render on the JVM via Paparazzi:
`./gradlew testDebugUnitTest --tests '*Screenshot*'`, output in
`app/build/reports/paparazzi/`.
