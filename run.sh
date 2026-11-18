#!/bin/sh
# ============================================================
#  TALA Accounting System - compile and run (macOS / Linux)
#  Needs: JDK 11 or newer, MySQL running,
#         and mysql-connector-j-<version>.jar inside the lib folder.
# ============================================================
cd "$(dirname "$0")" || exit 1

if ! ls lib/*.jar >/dev/null 2>&1; then
    echo "The lib folder has no MySQL connector jar yet."
    echo "Download mysql-connector-j (Platform Independent) from"
    echo "https://dev.mysql.com/downloads/connector/j/ and put the .jar in lib/"
    exit 1
fi

echo "Compiling..."
rm -rf out && mkdir out
javac -encoding UTF-8 -d out -cp "lib/*" $(find src/main/java -name "*.java") || exit 1

echo "Starting TALA..."
java -cp "out:src/main/resources:lib/*" tala.Main   # resources = the logo
