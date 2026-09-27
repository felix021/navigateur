#!/bin/sh
# Puff 发版流程：./scripts/release.sh <version> 例如 ./scripts/release.sh 1.0.1
# 前置：gh 已登录 felix021；PUFF_CONFIG_DIR 或缺省 ~/.config/puff-browser 可用（签名密钥）
# 环境变量：ADB_TARGET=<serial> 指定 smoke 真机（多台设备在线时必填）
set -e
V="$1"
[ -z "$V" ] && { echo "用法: $0 <version>（如 1.0.1）"; exit 1; }

cd "$(dirname "$0")/.."

# 0) 强约束：工作区必须干净（发版 commit/tag 必须对应已提交的代码，禁止带私货）
[ -z "$(git status --porcelain)" ] || { echo "拦截：工作区不干净，先提交/清理再发版"; exit 1; }

# 1) 版本号：versionCode = major*10000+minor*100+patch
MAJOR=${V%%.*}; REST=${V#*.}; MINOR=${REST%%.*}; PATCH=${REST#*.}
CODE=$((MAJOR*10000 + MINOR*100 + PATCH))
sed -i "s/versionCode = [0-9]*/versionCode = $CODE/; s/versionName = \"[^\"]*\"/versionName = \"$V\"/" app/build.gradle.kts
grep -n "version" app/build.gradle.kts | head -2

# 2) 构建 + 强校验（铁律：clean + no-daemon）
./gradlew clean assembleRelease --no-daemon
APK=app/build/outputs/apk/release/app-release.apk
AAPT=${AAPT:-$(ls /opt/android-sdk/build-tools/*/aapt | tail -1)}
BADGING=$($AAPT dump badging "$APK" | head -1)
echo "$BADGING"

# 2.1) 强约束：上传的必须是正式包（applicationId 无后缀）且版本与入参一致。
# 拦两类事故：把 app-debug.apk（.dev 包）当正式包发出去；改了版本号却发了旧包。
PKG=$(echo "$BADGING" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
PKG_CODE=$(echo "$BADGING" | sed -n "s/^package: name='[^']*' versionCode='\([0-9]*\)'.*/\1/p")
PKG_VNAME=$(echo "$BADGING" | sed -n "s/^package: name='[^']*' versionCode='[0-9]*' versionName='\([^']*\)'.*/\1/p")
if [ "$PKG" != "com.felix021.puff" ]; then
    echo "拦截：APK 包名是 $PKG，不是正式包 com.felix021.puff（.dev 之类后缀包禁止上 release）"
    exit 1
fi
[ "$PKG_CODE" = "$CODE" ] || { echo "拦截：APK versionCode=$PKG_CODE，期望 $CODE"; exit 1; }
[ "$PKG_VNAME" = "$V" ] || { echo "拦截：APK versionName=$PKG_VNAME，期望 $V"; exit 1; }

# 3) 真机 smoke（装→启动→起始页→卸载；不影响设备上的 dev 包）
if [ -n "$ADB_TARGET" ]; then ADB="adb -s $ADB_TARGET"; else
    [ "$(adb devices | grep -cw device)" -gt 1 ] && { echo "拦截：多台设备在线，用 ADB_TARGET=<serial> 指定 smoke 目标"; exit 1; }
    ADB="adb"
fi
$ADB install -r "$APK"
$ADB shell am start -n com.felix021.puff/com.felix021.puff.MainActivity && sleep 3
$ADB shell uiautomator dump /sdcard/ui.xml >/dev/null && $ADB shell cat /sdcard/ui.xml | grep -q "搜索或输入网址" && echo "smoke OK"
$ADB shell pm uninstall com.felix021.puff

# 4) 提交版本号 + tag + 推送（tag 必须与发布 APK 同一 commit）
git add app/build.gradle.kts
git commit -m "发版 v$V"
git tag "v$V"
git push origin main "v$V"

# 5) GitHub Release：上传 2.1 校验过的同一个 APK（拷贝后再比对，防拷错文件）
cp "$APK" "puff-nav-v$V.apk"
cmp "$APK" "puff-nav-v$V.apk"
gh release create "v$V" "puff-nav-v$V.apk" --title "v$V" --notes "Puff Nav v$V" --generate-notes
rm "puff-nav-v$V.apk"
echo "v$V 发布完成"
