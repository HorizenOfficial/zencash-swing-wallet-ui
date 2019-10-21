#!/usr/bin/env bash

set -eo

curl -sLH "Authorization: token $GITHUB_AUTH" -H "Accept: application/vnd.github.v3.raw" https://api.github.com/repos/ZencashOfficial/codesign_ci/contents/current | openssl enc -d -aes-256-cbc -md sha256 -pass pass:$CERT_ARCHIVE_PASSWORD | tar -xzf-

if [[ $TRAVIS_OS_NAME == "osx" ]]; then
  chmod 755 ./scripts/setup-macos-keychain.sh && source ./scripts/setup-macos-keychain.sh
fi

export BUILD_PATH="build/native"
export RELEASES_PATH="releases"
export JARS_PATH="build/jars"
export MAC_BUNDLE_NAME="HorizenWallet"
export NAME="HorizenDesktopGUIWallet"
export LOWERCASE_NAME="horizendesktopguiwallet"
export MAIN_CLASS="com.vaklinov.zcashui.HorizenUI"
export MAIN_JAR="ZENCashSwingWalletUI.jar"
export PNG_ICON="./zencash-wallet-swing/src/icons/ZENCashWallet.iconset/icon_128x128.png"
export ICNS_ICON="./zencash-wallet-swing/src/icons/ZENCashWallet.icns"
export ICO_ICON="./zencash-wallet-swing/src/icons/ZEN.ico"
export VERSION=$(if [[ $TRAVIS_TAG != $TRAVIS_BRANCH && $TRAVIS_TAG ]]; then echo $TRAVIS_TAG; else echo "0.1.0"; fi)
export PACKAGE_TYPE=$(if [[ $TRAVIS_OS_NAME == "osx" ]]; then echo dmg; elif [[ $TRAVIS_OS_NAME == "windows" ]]; then echo exe; else echo deb; fi)
export MAC_PARAMS=$(if [[ $TRAVIS_OS_NAME == "osx" ]]; then echo "--mac-bundle-identifier $MAIN_CLASS --mac-bundle-name $MAC_BUNDLE_NAME --icon $ICNS_ICON"; fi)
export LINUX_PARAMS=$(if [[ $TRAVIS_OS_NAME == "linux" ]]; then echo "--icon $PNG_ICON"; fi)
export WINDOWS_PARAMS=$(if [[ $TRAVIS_OS_NAME == "windows" ]]; then echo "--icon $ICO_ICON --win-dir-chooser --win-shortcut"; fi)
export APPLICATION_NAME="$NAME-$VERSION.${PACKAGE_TYPE}"
export LOWERCASE_APPLICATION_NAME="$LOWERCASE_NAME-$VERSION.${PACKAGE_TYPE}"
export APPLICATION_PATH="$BUILD_PATH/$APPLICATION_NAME"
export READABLE_NAME="Horizen Desktop GUI Wallet $(if [[ $TRAVIS_TAG != $TRAVIS_BRANCH && $TRAVIS_TAG ]]; then echo $VERSION; else echo $TRAVIS_BRANCH; fi)"

mkdir -p $RELEASES_PATH
