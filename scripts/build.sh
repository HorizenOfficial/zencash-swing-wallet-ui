#!/usr/bin/env bash

set -eo

# Build jars
ant -buildfile ./src/build/build.xml

# Download zend and zend-cli
if [[ $TRAVIS_OS_NAME == "osx" ]]; then
  export PLATFORM_NAME=Mac
elif [[ $TRAVIS_OS_NAME == "windows" ]]; then
  export PLATFORM_NAME=Win
fi

if [[ $TRAVIS_OS_NAME == "osx" || $TRAVIS_OS_NAME == "windows" ]]; then
  export FOLDER_NAME="Zen_${PLATFORM_NAME}_binaries_v${ZEND_VERSION}"
  export ZEND_FILE_NAME="${FOLDER_NAME}.zip"
  export FILE_EXT=""

  wget --no-check-certificate "https://github.com/ZencashOfficial/zen/releases/download/v${ZEND_VERSION}/${ZEND_FILE_NAME}"
  wget --no-check-certificate "https://github.com/ZencashOfficial/zen/releases/download/v${ZEND_VERSION}/${ZEND_FILE_NAME}.sha256"

  echo "DOWLNOADED"

  if [[ $TRAVIS_OS_NAME == "windows" ]]; then
    export FILE_EXT=".exe"

    # Check sha256
    powershell -command "& \"scripts\windows-check-sha256.ps1\""

    # Extract ZEND binaries
    powershell -command "Expand-Archive -Force ./${ZEND_FILE_NAME} ./"
  else
    # Check sha256
    echo $(cat $ZEND_FILE_NAME.sha256) | sha256sum --check
    FILE_SUM=$(sha256sum "./$ZEND_FILE_NAME")
    REAL_SUM=$(cat "$ZEND_FILE_NAME.sha256")

    if [[ $FILE_SUM == $REAL_SUM ]]; then
      echo "sha256 matches!"
    else
      echo "sha256 does not match!"
      exit 1
    fi

    # Extract ZEND binaries
    tar -xzvf $ZEND_FILE_NAME
  fi

  mv "./${FOLDER_NAME}/zend${FILE_EXT}" "${JARS_PATH}/zend${FILE_EXT}"
  mv "./${FOLDER_NAME}/zen-cli${FILE_EXT}" "${JARS_PATH}/zen-cli${FILE_EXT}"
  mv "./${FOLDER_NAME}/zen-tx${FILE_EXT}" "${JARS_PATH}/zen-tx${FILE_EXT}"
fi

if [[ $TRAVIS_OS_NAME == "osx" ]]; then
  # Package jars into .app
  $JAVA_HOME/bin/jpackage -o $BUILD_PATH -i $JARS_PATH --app-version $VERSION --main-class $MAIN_CLASS --main-jar $MAIN_JAR -n $NAME $MAC_PARAMS $LINUX_PARAMS $WINDOWS_PARAMS

  # Sign .app
  codesign --deep --force --verbose --sign "Developer ID Application" "$BUILD_PATH/$NAME.app"

  # Package .app into dmg
  $JAVA_HOME/bin/jpackage --package-type $PACKAGE_TYPE -o $BUILD_PATH --app-version $VERSION --identifier $MAIN_CLASS -n $NAME --app-image "$BUILD_PATH/$NAME.app"
else
  $JAVA_HOME/bin/jpackage --package-type $PACKAGE_TYPE -o $BUILD_PATH -i $JARS_PATH --app-version $VERSION --main-class $MAIN_CLASS --main-jar $MAIN_JAR -n $NAME $MAC_PARAMS $LINUX_PARAMS $WINDOWS_PARAMS
fi

export PASS="pass:$WIN_CERT_PASSWORD"

if [[ $TRAVIS_OS_NAME == "windows" ]]; then
  choco install -y windows-sdk-10.0

  # Sign
  powershell -command "& \"scripts\windows-sign.ps1\""

  # Create checksum
  $(echo powershell -command "& \"scripts\windows-get-sha256.ps1\"") > "./${BUILD_PATH}/${APPLICATION_NAME}.sha256"
elif [[ $TRAVIS_OS_NAME == "linux" ]]; then
  # Linux's build has a lowercase name for some reason
  mv "$BUILD_PATH/$LOWERCASE_APPLICATION_NAME" "$APPLICATION_PATH"
elif [[ $TRAVIS_OS_NAME == "osx" ]]; then
  # Sign
  codesign --deep --force --verbose --sign "Developer ID Application" $APPLICATION_PATH

  # Create checksum
  cd $BUILD_PATH && sha256sum $APPLICATION_NAME > "${APPLICATION_NAME}.sha256" && cd ../..
fi

# Move files to "releases" folder in the root
rm -rf "$RELEASES_PATH/$APPLICATION_NAME"
mv "./$APPLICATION_PATH" "$RELEASES_PATH/$APPLICATION_NAME"

if [[ $TRAVIS_OS_NAME == "osx" || $TRAVIS_OS_NAME == "windows" ]]; then
  rm -rf "$RELEASES_PATH/$APPLICATION_NAME.sha256"
  mv "./$APPLICATION_PATH.sha256" "$RELEASES_PATH/$APPLICATION_NAME.sha256"
fi
