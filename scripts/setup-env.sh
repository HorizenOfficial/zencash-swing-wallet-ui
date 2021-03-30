#!/usr/bin/env bash

set -eo pipefail

SEMVER_REGEX="^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(\\-[0-9A-Za-z-]+(\\.[0-9A-Za-z-]+)*)?(\\+[0-9A-Za-z-]+(\\.[0-9A-Za-z-]+)*)?$"

if [ ! -z "${TRAVIS_TAG+x}" ] && [[ "${TRAVIS_TAG}" =~ ${SEMVER_REGEX} ]] && [[ "${TRAVIS_OS_NAME}" != "linux" ]]; then
  echo "Release tag found, packages will be code signed."
  export SIGN=true
  curl -sLH "Authorization: token ${GITHUB_AUTH}" -H "Accept: application/vnd.github.v3.raw" https://api.github.com/repos/HorizenOfficial/codesign_ci/contents/current | openssl enc -d -aes-256-cbc -md sha256 -pass "pass:${CERT_ARCHIVE_PASSWORD}" | tar -xzf-
  if [[ "${TRAVIS_OS_NAME}" == "osx" ]]; then
    source ./scripts/setup-macos-keychain.sh
  fi
else
  export SIGN=false
  unset CERT_ARCHIVE_PASSWORD
  unset MAC_CERT_PASSWORD
  unset WIN_CERT_PASSWORD
fi

export BUILD_PATH="build/native"
export RELEASES_PATH="releases"
export JARS_PATH="build/jars"
export MAC_BUNDLE_NAME="HorizenWallet"
export NAME="HorizenDesktopGUIWallet"
export MAIN_CLASS="com.vaklinov.zcashui.HorizenUI"
export MAIN_JAR="ZENCashSwingWalletUI.jar"
export PNG_ICON="./zencash-wallet-swing/src/icons/ZENCashWallet.iconset/icon_128x128.png"
export ICNS_ICON="./zencash-wallet-swing/src/icons/ZENCashWallet.icns"
export ICO_ICON="./zencash-wallet-swing/src/icons/ZEN.ico"
export VERSION="$(if [ ! -z "${TRAVIS_TAG+x}" ] && [[ "$TRAVIS_TAG" =~ ${SEMVER_REGEX} ]]; then echo "${TRAVIS_TAG}"; else echo "1.0.0"; fi)"
export PACKAGE_TYPE="$(if [[ "${TRAVIS_OS_NAME}" == "osx" ]]; then echo dmg; elif [[ "${TRAVIS_OS_NAME}" == "windows" ]]; then echo exe; fi)"
export MAC_PARAMS="$(if [[ "${TRAVIS_OS_NAME}" == "osx" ]]; then echo -n "--icon ${ICNS_ICON}"; if [[ "${SIGN}" = true ]]; then echo " --mac-sign"; fi; fi)"
export WINDOWS_PARAMS="$(if [[ "${TRAVIS_OS_NAME}" == "windows" ]]; then echo "--icon ${ICO_ICON} --win-dir-chooser --win-shortcut --win-menu --win-upgrade-uuid e51de4ee-2dc7-45d8-b32d-aebd3fe81547"; fi)"
export COMMON_PARAMS="--java-options -Xmx1536m --verbose --license-file ./LICENSE --copyright \"Copyright (c) 2021 Zen Blockchain Foundation\" --description \"Horizen Desktop GUI Wallet ${VERSION}\""
export APPLICATION_NAME="${NAME}-${VERSION}.${PACKAGE_TYPE}"
export APPLICATION_PATH="${BUILD_PATH}/${APPLICATION_NAME}"
export READABLE_NAME="Horizen Desktop GUI Wallet $(if [[ "${TRAVIS_TAG}" != "${TRAVIS_BRANCH}" && "${TRAVIS_TAG}" ]]; then echo "${VERSION}"; else echo "${TRAVIS_BRANCH}"; fi)"

mkdir -p "${RELEASES_PATH}"
