#!/bin/bash

set -euo pipefail

zend_version_old="3.1.0"
zend_version_new="3.2.0"

swing_version_old="1.0.6"
swing_version_new="1.0.7"

# bump zend version
sed -i "s/${zend_version_old//./\\.}/${zend_version_new//./\\.}/g" .travis.yml

# bump swing version
grep -lr --exclude-dir=.git --exclude-dir=docs --exclude-dir=scripts "${swing_version_old//./\\.}" | xargs sed -i "s/${swing_version_old//./\\.}/${swing_version_new//./\\.}/g"

# create release notes
new_notes="docs/Release_${swing_version_new}.md"
cp "docs/Release_${swing_version_old}.md" "${new_notes}"
sed -i "s/${zend_version_old//./\\.}/${zend_version_new//./\\.}/g" "${new_notes}"
sed -i "s/${swing_version_old//./\\.}/${swing_version_new//./\\.}/g" "${new_notes}"
sed -i 's/is\ not\ `.*`/is\ not\ `TODO`/g' "${new_notes}"
sed -i 's/^.*\ \ HorizenDesktopGUIWallet/TODO\ \ HorizenDesktopGUIWallet/g' "${new_notes}"
