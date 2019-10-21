#!/usr/bin/env bash
set -eo pipefail

# jpackage
export JAVA_HOME=$HOME/jdk-14
chmod +x ./scripts/install-jdk.sh && ./scripts/install-jdk.sh --target $JAVA_HOME --feature "jpackage" --license "GPL"

# ant
antver="1.10.7"
antdir="apache-ant-${antver}"
antfile="${antdir}-bin.zip"
anturl="https://www.apache.org/dist/ant/binaries/${antfile}"
curl -sL "$anturl" > $antfile
echo $(curl -sL "${anturl}.sha512")"  $antfile" > ${antfile}.sha512
if [[ $TRAVIS_OS_NAME == "windows" ]]; then
  powershell -file "scripts\windows-verify-checksum.ps1" ${antfile}.sha512 sha512
  7z x ${antfile}
else
  shasum -a512 -c ${antfile}.sha512
  unzip $antfile
fi

# hack to always use "codesign --force" on mac, needed to not error when already signed files are in the .app bundle
if [[ $TRAVIS_OS_NAME == "osx" ]]; then
  cat << EOF > ${antdir}/bin/codesign
#!/bin/bash

exec /usr/bin/codesign -f "\$@"
EOF
  chmod +x ${antdir}/bin/codesign
fi

export PATH="$(pwd)/${antdir}/bin:$PATH"

if [[ $TRAVIS_OS_NAME == "osx" ]]; then
#  brew update-reset
  brew upgrade coreutils
fi
