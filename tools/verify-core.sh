#!/usr/bin/env bash
#
# Headless verification for the Minecraft-independent core of Nebrel Client.
#
# Compiles the settings, config, module-registry, theme, animation and HUD
# geometry layers with a plain JDK and runs tools/coretest/CoreSelfTest.java
# against them. No Minecraft, no Fabric, no Loom, so this works on machines that
# cannot reach maven.fabricmc.net.
#
# Requires: JDK 21+, curl (only on the first run, to fetch Gson).
#
# Usage:  ./tools/verify-core.sh

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="${ROOT}/build/core-verify"
GSON_VERSION="2.10.1"
GSON_JAR="${WORK}/gson-${GSON_VERSION}.jar"
GSON_URL="https://repo1.maven.org/maven2/com/google/code/gson/gson/${GSON_VERSION}/gson-${GSON_VERSION}.jar"

mkdir -p "${WORK}/classes"

if [[ ! -f "${GSON_JAR}" ]]; then
  echo "Fetching Gson ${GSON_VERSION} ..."
  curl -fsSL -o "${GSON_JAR}" "${GSON_URL}"
fi

# The core is exactly the set of packages that import no Minecraft class.
SOURCES="${WORK}/sources.txt"
{
  find "${ROOT}/src/main/java/de/nebrel/client/setting" -name '*.java'
  find "${ROOT}/src/main/java/de/nebrel/client/config" -name '*.java'
  find "${ROOT}/src/main/java/de/nebrel/client/gui/theme" -name '*.java'
  find "${ROOT}/src/main/java/de/nebrel/client/render/animation" -name '*.java'
  find "${ROOT}/src/main/java/de/nebrel/client/util" -name '*.java'
  echo "${ROOT}/src/main/java/de/nebrel/client/module/Module.java"
  echo "${ROOT}/src/main/java/de/nebrel/client/module/ModuleCategory.java"
  echo "${ROOT}/src/main/java/de/nebrel/client/module/ModuleManager.java"
  echo "${ROOT}/src/main/java/de/nebrel/client/hud/HudAnchor.java"
  echo "${ROOT}/tools/coretest/CoreSelfTest.java"
} > "${SOURCES}"

echo "Compiling $(wc -l < "${SOURCES}" | tr -d ' ') source files ..."
javac -Xlint:all -Werror -d "${WORK}/classes" -cp "${GSON_JAR}" "@${SOURCES}"

echo "Running core self test ..."
java -cp "${WORK}/classes:${GSON_JAR}" CoreSelfTest
