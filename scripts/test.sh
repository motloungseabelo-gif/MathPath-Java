#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
bash scripts/build.sh
find src/test/java -name '*.java' | LC_ALL=C sort > build/test-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -cp build/classes -d build/test-classes @build/test-sources.txt
java -Djava.awt.headless=true -cp build/classes:build/test-classes com.mathpath.TestSuite
java -Djava.awt.headless=true -cp build/classes:build/test-classes com.mathpath.UiTest
if [[ "${1:-}" == "--desktop" ]]; then
  java -cp dist/mathpath.jar:build/test-classes com.mathpath.DesktopSmokeTest
fi
