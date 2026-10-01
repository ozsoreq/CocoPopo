class_name Sketch
extends Node2D
## A node that paints itself with a Paint callback. Drawing is cached by Godot; animate it by moving,
## rotating or tinting the node (call queue_redraw() only if the picture itself changes).

var fn: Callable


static func of(paint_fn: Callable, pos := Vector2.ZERO) -> Sketch:
	var s := Sketch.new()
	s.fn = paint_fn
	s.position = pos
	return s


func _draw() -> void:
	if fn.is_valid():
		fn.call(Paint.new(self))
