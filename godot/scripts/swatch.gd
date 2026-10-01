class_name Swatch
extends Node2D
## A round colour choice.

var col := Color.WHITE
var on := false
var pressed_amt := 0.0
var box := Vector2(80, 80)

func _process(_dt: float) -> void:
	var s := 0.9 if pressed_amt > 0 else 1.0
	scale = Vector2(s, s)
	queue_redraw()

func _draw() -> void:
	var c := box / 2
	draw_circle(c, 42.5, UI.OUTLINE)
	draw_circle(c, 38, col)
	if on:
		draw_arc(c, 48, 0, TAU, 48, Color("6c7bff"), 7, true)
