#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v javac >/dev/null 2>&1 || { echo "Install JDK 17 or newer and add its bin directory to PATH." >&2; exit 1; }
mkdir -p build/classes build/test-classes dist
find src/main/java -name '*.java' | LC_ALL=C sort > build/main-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/classes @build/main-sources.txt
jar --create --file dist/mathpath.jar --main-class com.mathpath.MathPath -C build/classes .
echo "Built dist/mathpath.jar"
