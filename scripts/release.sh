#!/bin/sh
# Puff 发版流程：./scripts/release.sh <version> 例如 ./scripts/release.sh 1.0.1
# 前置：gitea/gitea CLI 无关；gh 已登录 felix021；PUFF_CONFIG_DIR 或缺省 ~/.config/puff-browser 可用
set -e
V="$1"
[ -z "$V" ] && { echo "用法: $0 <version>（如 1.0.1）"; exit 1; }

cd "$(dirname "$0")/.."

# 1) 版本号：versionCode = major*10000+minor*100+patch
MAJOR=${V%%.*}; REST=${V#*.}; MINOR=${REST%%.*}; PATCH=${REST#*.}
CODE=$((MAJOR*10000 + MINOR*100 + PATCH))
sed -i "s/versionCode = [0-9]*/versionCode = $CODE/; s/versionName = \"[^\"]*\"/versionName = \"$V\"/" app/build.gradle.kts
grep -n "version" app/build.gradle.kts | head -2

# 2) 构建 + 校验（铁律：clean + no-daemon）
./gradlew clean assembleRelease --no-daemon
AAPT=${AAPT:-$(ls /opt/android-sdk/build-tools/*/aapt | tail -1)}
$AAPT dump badging app/build/outputs/apk/release/app-release.apk | head -1

# 3) 真机 smoke（装→启动→起始页→卸载；不影响设备上的 dev 包）
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.felix021.puff/com.felix021.puff.MainActivity && sleep 3
adb shell uiautomator dump /sdcard/ui.xml >/dev/null && adb shell cat /sdcard/ui.xml | grep -q "搜索或输入网址" && echo "smoke OK"
adb shell pm uninstall com.felix021.puff

# 4) 提交版本号 + tag + 推送
git add app/build.gradle.kts
git commit -m "发版 v$V"
git tag "v$V"
git push origin main "v$V"

# 5) GitHub Release
cp app/build/outputs/apk/release/app-release.apk "puff-nav-v$V.apk"
gh release create "v$V" "puff-nav-v$V.apk" --title "v$V" --notes "Puff Nav v$V" --generate-notes
rm "puff-nav-v$V.apk"
echo "v$V 发布完成"
