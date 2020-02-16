#!/usr/bin/env bash

set -eo pipefail

export KEY_CHAIN=mac-build.keychain
security create-keychain -p travis "${KEY_CHAIN}"
security import ./mac.p12 -k "${KEY_CHAIN}" -P "${MAC_CERT_PASSWORD}" -T /usr/bin/codesign

security list-keychain -s "${KEY_CHAIN}"
security unlock-keychain -p travis "${KEY_CHAIN}"
security set-keychain-settings -t 3600 -u "${KEY_CHAIN}"

security default-keychain -s "${KEY_CHAIN}"

# to see if this process succeeded
security find-identity -v -p codesigning

# set key partition list to avoid UI permission popup that causes hanging at CI
security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k travis "${KEY_CHAIN}"
