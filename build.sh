#!/bin/sh
# Build a release APK and sign it with the local SbandCode key.
set -eu
here=$(cd "$(dirname "$0")" && pwd)
cd "$here"

gradle=${GRADLE:-gradle}
if ! command -v "$gradle" >/dev/null 2>&1; then
    echo "Install Gradle 8.11.1 or set GRADLE to its executable path." >&2
    exit 1
fi
if [ ! -r "${SIGNER:-}" ]; then
    echo "Set SIGNER to the path of uber-apk-signer.jar." >&2
    exit 1
fi
if [ ! -r keys/sbandcode.jks ] || [ ! -r keys/sbandcode.pass ]; then
    echo "Provide keys/sbandcode.jks (alias sbandcode) and keys/sbandcode.pass." >&2
    exit 1
fi

"$gradle" -q :app:assembleRelease
version=$(sed -n 's/^[[:space:]]*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts)
mkdir -p out
apk="out/sbandcode-$version.apk"
cp app/build/outputs/apk/release/app-release-unsigned.apk "$apk"
pass=$(cat keys/sbandcode.pass)
java -jar "$SIGNER" -a "$apk" --allowResign --overwrite \
    --ks keys/sbandcode.jks --ksAlias sbandcode --ksPass "$pass" --ksKeyPass "$pass" > /dev/null
echo "$here/$apk"
