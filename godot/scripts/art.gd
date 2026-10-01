extends Node
## Loads the vector art (SVG, exported from the original code-drawn art) and rasterises it
## at the device's resolution so everything stays crisp on any screen.

var manifest := {}
var k := 1.0          # raster scale: texture pixels per design unit
var _cache := {}

func _ready() -> void:
	var txt := FileAccess.get_file_as_string("res://art/manifest.json")
	manifest = JSON.parse_string(txt)
	var screen_h := float(DisplayServer.screen_get_size().y)
	if screen_h <= 0:
		screen_h = 1080.0
	k = clampf(screen_h / 1080.0 * 1.25, 1.0, 2.5)

func has(name: String) -> bool:
	return manifest.has(name)

func tex(name: String, scale_mul := 1.0) -> Texture2D:
	var key := "%s@%s" % [name, scale_mul]
	if _cache.has(key):
		return _cache[key]
	var img := Image.new()
	var src := FileAccess.get_file_as_string("res://art/%s.svg" % name)
	if src.is_empty() or img.load_svg_from_string(src, k * scale_mul) != OK:
		push_warning("missing art: " + name)
		return null
	var t := ImageTexture.create_from_image(img)
	_cache[key] = t
	return t

## A sprite whose origin is the art's pivot (the SVG coordinate origin).
func sprite(name: String, tint := Color.WHITE, scale_mul := 1.0) -> Sprite2D:
	var s := Sprite2D.new()
	set_art(s, name, scale_mul)
	s.modulate = tint
	return s

func set_art(s: Sprite2D, name: String, scale_mul := 1.0) -> void:
	if not manifest.has(name):
		s.texture = null
		return
	var m: Array = manifest[name]
	var kk := k * scale_mul
	s.texture = tex(name, scale_mul)
	s.centered = false
	s.offset = Vector2(m[0], m[1]) * kk
	s.scale = Vector2.ONE / kk

func size_of(name: String) -> Vector2:
	if not manifest.has(name):
		return Vector2.ZERO
	var m: Array = manifest[name]
	return Vector2(m[2], m[3])

func rect_of(name: String) -> Rect2:
	var m: Array = manifest[name]
	return Rect2(m[0], m[1], m[2], m[3])
