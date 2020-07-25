#!/usr/bin/env bash

set -eo pipefail

cd "${TRAVIS_BUILD_DIR}"
# Build jars
ant -buildfile ./src/build/build.xml
#TODO sign JARS in ant

# Download zend and zend-cli
if [[ "${TRAVIS_OS_NAME}" == "osx" ]]; then
  export PLATFORM_NAME=Mac
elif [[ "${TRAVIS_OS_NAME}" == "windows" ]]; then
  export PLATFORM_NAME=Win
fi

if [[ "${TRAVIS_OS_NAME}" == "osx" || "${TRAVIS_OS_NAME}" == "windows" ]]; then
  export FOLDER_NAME="Zen_${PLATFORM_NAME}_binaries_v${ZEND_VERSION}"
  export ZEND_FILE_NAME="${FOLDER_NAME}.zip"
  export FILE_EXT=""

  curl -sL "https://github.com/HorizenOfficial/zen/releases/download/v${ZEND_VERSION}/${ZEND_FILE_NAME}" > "${ZEND_FILE_NAME}"
  curl -sL "https://github.com/HorizenOfficial/zen/releases/download/v${ZEND_VERSION}/${ZEND_FILE_NAME}.sha256" > "${ZEND_FILE_NAME}.sha256"

  echo "DOWLNOADED"

  if [[ "${TRAVIS_OS_NAME}" == "windows" ]]; then
    export FILE_EXT=".exe"

    # Check sha256
    powershell -file "scripts\windows-verify-checksum.ps1" "${ZEND_FILE_NAME}.sha256" sha256

    # Extract ZEND binaries
    7z x "${ZEND_FILE_NAME}"

    # Verify extracted binaries
    pushd "${FOLDER_NAME}"
    powershell -file "..\scripts\windows-verify-checksum.ps1" checksums.sha256 sha256
    popd
  else
    # Check sha256
    shasum -a256 -c "${ZEND_FILE_NAME}.sha256"

    # Extract ZEND binaries
    unzip "${ZEND_FILE_NAME}"

    # Verify extracted binaries
    pushd "${FOLDER_NAME}"
    shasum -a256 -c checksums.sha256
    popd
  fi

  mv "./${FOLDER_NAME}/zend${FILE_EXT}" "${JARS_PATH}/zend${FILE_EXT}"
  chmod a+x "./${JARS_PATH}/zend${FILE_EXT}"
  mv "./${FOLDER_NAME}/zen-cli${FILE_EXT}" "${JARS_PATH}/zen-cli${FILE_EXT}"
  chmod a+x "./${JARS_PATH}/zen-cli${FILE_EXT}"
  mv "./${FOLDER_NAME}/zen-tx${FILE_EXT}" "${JARS_PATH}/zen-tx${FILE_EXT}"
  chmod a+x "./${JARS_PATH}/zen-tx${FILE_EXT}"
  rm -f "${JARS_PATH}/ZENCashSwingWalletUI-src.jar"
fi

if [[ "${TRAVIS_OS_NAME}" != "linux" ]]; then
  # TODO jpackage has no code signing support on windows yet, so the launcher is unsigned. https://bugs.openjdk.java.net/browse/JDK-8230668
  echo "Running jpackage command: ${JAVA_HOME}/bin/jpackage --type ${PACKAGE_TYPE} -d ${BUILD_PATH} -i ${JARS_PATH} --app-version ${VERSION} --main-class ${MAIN_CLASS} --main-jar ${MAIN_JAR} -n ${NAME} ${COMMON_PARAMS} ${MAC_PARAMS} ${WINDOWS_PARAMS}"
  bash -c "${JAVA_HOME}/bin/jpackage --type ${PACKAGE_TYPE} -d ${BUILD_PATH} -i ${JARS_PATH} --app-version ${VERSION} --main-class ${MAIN_CLASS} --main-jar ${MAIN_JAR} -n ${NAME} ${COMMON_PARAMS} ${MAC_PARAMS} ${WINDOWS_PARAMS}"
fi

if [[ "${TRAVIS_OS_NAME}" == "windows" ]]; then
  # Sign installer
  if [[ "${SIGN}" == "true" ]]; then
    powershell -file "scripts/windows-sign.ps1"
  fi

  # Create checksum
  pushd "${BUILD_PATH}"
  powershell -File "..\..\scripts\windows-get-checksum.ps1" "${APPLICATION_NAME}" sha256 > "${APPLICATION_NAME}.sha256"
  echo "" >> "${APPLICATION_NAME}.sha256"
  popd
elif [[ "${TRAVIS_OS_NAME}" == "osx" ]]; then
  # Sign DMG
  if [[ "${SIGN}" == "true" ]]; then
    /usr/bin/codesign --deep --force --verbose --sign "Developer ID Application" "${APPLICATION_PATH}"
  fi

  # Create checksum
  pushd "${BUILD_PATH}"
  shasum -a256 "${APPLICATION_NAME}" > "${APPLICATION_NAME}.sha256"
  popd
fi

# Move files to "releases" folder in HOME
if [[ "${TRAVIS_OS_NAME}" == "osx" || "${TRAVIS_OS_NAME}" == "windows" ]]; then
  rm -rf "${RELEASES_PATH:?}/*"
  mv "./${APPLICATION_PATH}" "${RELEASES_PATH}/${APPLICATION_NAME}"
  mv "./${APPLICATION_PATH}.sha256" "${RELEASES_PATH}/${APPLICATION_NAME}.sha256"
fi
if [[ "${TRAVIS_OS_NAME}" == "linux" ]]; then
  rm -rf "${RELEASES_PATH:?}/*"
  mv ./build/ubuntu-package/*.deb "${RELEASES_PATH}/"
  pushd "${RELEASES_PATH}/"
  for file in *.deb; do sha256sum "${file}" > "${file}.sha256"; done
  popd
fi
