#!/bin/bash
# Exports the code-drawn art (props, backgrounds, character parts, icons) as SVGs for the Godot project.
set -e
cd "$(dirname "$0")/.."
OUT=build/preview-classes
rm -rf $OUT && mkdir -p $OUT
SRC=$(ls app/src/main/java/com/cocopopo/app/*.java | grep -v -E 'MainActivity|GameView')
javac -nowarn -d $OUT $(find tools/preview -name '*.java') $SRC 2>&1 | grep -v "^Picked up" || true
java -Djava.awt.headless=true -cp $OUT com.cocopopo.app.ArtExport godot/art 2>&1 | grep -v "^Picked up"
