#!/bin/bash
# Regenerates godot/art (SVGs + manifest.json) with the GDScript art generator in godot/tools/art.
# Needs Godot 4.7: set GODOT=/path/to/godot or have `godot` on PATH. In the editor you can instead
# open godot/tools/art/export_art.gd and use File > Run.
set -e
cd "$(dirname "$0")/../godot"
"${GODOT:-godot}" --headless --path . --script res://tools/art/export_art_cli.gd "$@"
