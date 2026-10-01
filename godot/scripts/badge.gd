extends Node2D
## The little action badge shown over a drop target while dragging.

var icon := 16
var col := Color.WHITE
var tt := 0.0
var _tex: Texture2D
var _shown := -1

func set_badge(i: int, c: Color, t: float) -> void:
	icon = i; col = c; tt = t
	if i != _shown:
		_tex = Art.tex("icon_%d" % i)
		_shown = i
	queue_redraw()

func _draw() -> void:
	var s := 1.0 + sin(tt * 8) * 0.08
	draw_circle(Vector2(0, 6), 46 * s, UI.OUTLINE)
	draw_circle(Vector2.ZERO, 50.5 * s, UI.OUTLINE)
	draw_circle(Vector2.ZERO, 46 * s, Color.WHITE)
	if _tex:
		var m: Array = Art.manifest["icon_%d" % icon]
		var k := 0.82 * s / Art.k
		draw_set_transform(Vector2.ZERO, 0, Vector2(k, k))
		draw_texture(_tex, Vector2(m[0], m[1]) * Art.k, col)
		draw_set_transform(Vector2.ZERO, 0, Vector2.ONE)
