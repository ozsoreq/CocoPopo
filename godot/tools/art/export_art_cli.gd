extends SceneTree
## Regenerates the art in res://art from the command line:
##   godot --headless --path godot --script res://tools/art/export_art_cli.gd
## Optional: -- --out=DIR to write somewhere else (e.g. to compare with the committed art).

const ArtExport := preload("res://tools/art/art_export.gd")


func _init() -> void:
	var ex := ArtExport.new()
	for a in OS.get_cmdline_user_args():
		if a.begins_with("--out="):
			ex.dir = a.substr(6)
	quit(0 if ex.run() > 0 else 1)
