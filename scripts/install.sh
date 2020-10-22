#!/usr/bin/env bash

cd "${HOME}"

# jpackage
export JAVA_HOME="${HOME}/jdk-${JDK}"
source "${TRAVIS_BUILD_DIR}/scripts/install-jdk.sh" --feature "${JDK}" -v --target "${JAVA_HOME}"

set -eo pipefail
# ant
antver="1.10.9"
antdir="apache-ant-${antver}"
antfile="${antdir}-bin.zip"
anturl="https://downloads.apache.org/ant/binaries/${antfile}"
curl -sL "${anturl}" > "${HOME}/${antfile}"
curl -sL "${anturl}.sha512" | xargs -I {} echo "{}  ${antfile}" > "${HOME}/${antfile}.sha512"
if [[ "${TRAVIS_OS_NAME}" == "windows" ]]; then
  powershell -file "${TRAVIS_BUILD_DIR}\scripts\windows-verify-checksum.ps1" "${HOME}/${antfile}.sha512" sha512
  7z x "${HOME}/${antfile}"
else
  shasum -a512 -c "${HOME}/${antfile}.sha512"
  unzip "${HOME}/$antfile"
fi

# hack to always use "codesign --force" on mac, needed to not error when already signed files are in the .app bundle
if [[ "${TRAVIS_OS_NAME}" == "osx" ]]; then
  cat << EOF > "${HOME}/${antdir}/bin/codesign"
#!/bin/bash

exec /usr/bin/codesign -f "\$@"
EOF
  chmod +x "${HOME}/${antdir}/bin/codesign"
fi

set +e
set +o pipefail
set +o errexit

export PATH="${HOME}/${antdir}/bin:${PATH}"

cd "${TRAVIS_BUILD_DIR}"
