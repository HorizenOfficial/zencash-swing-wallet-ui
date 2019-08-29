#!/usr/bin/env bash

# jpackage
export JAVA_HOME=$HOME/jdk-14
chmod 755 ./scripts/install-jdk.sh && ./scripts/install-jdk.sh --target $JAVA_HOME --feature "jpackage" --license "GPL"

# ant
wget --no-check-certificate https://www.apache.org/dist/ant/binaries/apache-ant-1.10.6-bin.zip
if [[ $TRAVIS_OS_NAME == "windows" ]]; then
  powershell -command "Expand-Archive -Force ./apache-ant-1.10.6-bin.zip ./"
else
  tar -xzvf apache-ant-1.10.6-bin.zip
fi
export PATH=`pwd`/apache-ant-1.10.6/bin:$PATH

# fakeroot (needed for jpackage)
if [[ $TRAVIS_OS_NAME == "linux" ]]; then
  sudo apt-get update && sudo apt-get install fakeroot;
elif [[ $TRAVIS_OS_NAME == "osx" ]]; then
  brew upgrade coreutils
fi