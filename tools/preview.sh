#!/bin/bash
# Renders game screens to PNG on the desktop (no emulator needed). Usage: tools/preview.sh [screens...]
set -e
cd "$(dirname "$0")/.."
OUT=build/preview-classes
rm -rf $OUT && mkdir -p $OUT
SRC=$(ls app/src/main/java/com/cocopopo/app/*.java | grep -v -E 'MainActivity|GameView')
javac -nowarn -d $OUT -sourcepath tools/preview $(find tools/preview -name '*.java') $SRC 2>&1 | grep -v "^Picked up" || true
if [ "${1:-}" = smoke ]; then java -Djava.awt.headless=true -cp $OUT com.cocopopo.preview.Preview smoke 2>&1 | grep -v "^Picked up"; exit; fi
java -Djava.awt.headless=true -cp $OUT com.cocopopo.preview.Preview preview-out "$@" 2>&1 | grep -v "^Picked up"
