class_name RoundButton
extends Control
## A chunky round button with an outlined circle and a white glyph (Toca-style), with press squash.

signal pressed

var color := Color("4fb3ff")
var icon := ""
var radius := 52.0
var _down := false
var _scale := 1.0
var _icon_tex: Texture2D
var mirror := false
var active := false

func setup(icon_name: String, col: Color, r := 52.0) -> RoundButton:
	icon = icon_name
	color = col
	radius = r
	custom_minimum_size = Vector2(r * 2, r * 2)
	size = Vector2(r * 2, r * 2)
	pivot_offset = size / 2
	_icon_tex = Art.tex(icon_name)
	mouse_filter = Control.MOUSE_FILTER_STOP
	return self

func _process(delta: float) -> void:
	var target := 0.88 if _down else 1.0
	_scale = lerpf(_scale, target, minf(1.0, delta * 20))
	scale = Vector2(_scale * (-1.0 if mirror else 1.0), _scale)
	queue_redraw()

func _gui_input(e: InputEvent) -> void:
	if e is InputEventMouseButton and e.button_index == MOUSE_BUTTON_LEFT:
		if e.pressed:
			_down = true
			accept_event()
		elif _down:
			_down = false
			accept_event()
			if Rect2(Vector2.ZERO, size).has_point(e.position):
				Sfx.play("tick")
				pressed.emit()

func _draw() -> void:
	var c := size / 2
	draw_circle(c + Vector2(0, 8), radius, UI.OUTLINE)
	draw_circle(c, radius + 4.5, UI.OUTLINE)
	draw_circle(c, radius, color)
	draw_arc(c, radius * 0.78, deg_to_rad(200), deg_to_rad(270), 12, Color(1, 1, 1, 0.45), radius * 0.09, true)
	if active:
		draw_arc(c, radius + 12, 0, TAU, 48, Color.WHITE, 7, true)
	if _icon_tex:
		var m: Array = Art.manifest[icon]
		var s := radius / 50.0 / Art.k
		draw_set_transform(c, 0, Vector2(s, s))
		draw_texture(_icon_tex, Vector2(m[0], m[1]) * Art.k)
		draw_set_transform(Vector2.ZERO, 0, Vector2.ONE)
