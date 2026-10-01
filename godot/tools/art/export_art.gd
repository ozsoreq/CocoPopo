@tool
extends EditorScript
## Regenerates the art in res://art. In the Godot editor: open this script and use File > Run (Ctrl+Shift+X).

const ArtExport := preload("res://tools/art/art_export.gd")


func _run() -> void:
	ArtExport.new().run()
	EditorInterface.get_resource_filesystem().scan()
