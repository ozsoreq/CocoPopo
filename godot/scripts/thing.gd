class_name Thing
extends Node2D
## Anything that lives in a location: a character or a prop. Holds the shared simulation state;
## the location runs the simulation and calls apply_visual() each frame.

enum { FREE, SIT, LIE, HELD, ON_TOP, BATHE, SLIDE }

var is_char := false
var id := ""            # prop id
var x := 0.0
var y := 0.0
var sc := 1.0           # user scale
var flip := false
var bw := 100.0
var bh := 100.0

var lift := 0.0
var tilt := 0.0
var hop := 0.0
var hop_v := 0.0
var sq := 0.0
var sqv := 0.0
var pop := 1.0
var vx := 0.0
var wiggle := 0.0
var phase := randf() * 6.0
var dyo := 0.0

var state := FREE
var link: Thing = null
var slot := 0
var off_x := 0.0
var held: Thing = null
var falling := false
var vy := 0.0
var fvx := 0.0
var peak_y := 0.0
var bites := 0
var pstate := 0
var contents: Array[String] = []
var launch_t := 0.0
var shimmer := 0.0

func hit(p: Vector2) -> bool:
	if state == LIE:
		var lx := (p.x - x) / sc * -slot
		var ly := (p.y - y) / sc
		return lx >= -20 and lx <= 380 and ly >= -120 and ly <= 100
	var s := sc * draw_scale()
	var pad := 22.0 / s if is_char else 0.0   # small characters are easier to grab
	var lx2 := (p.x - x) / s
	var ly2 := (p.y - y) / s
	return lx2 >= -bw / 2 - pad and lx2 <= bw / 2 + pad and ly2 >= -bh - pad and ly2 <= pad

func draw_scale() -> float:
	return 1.0

func sort_y() -> float:
	if state == SIT and link != null and link.is_char:
		return link.sort_y() - 1
	if state in [SIT, LIE, ON_TOP, BATHE, SLIDE, HELD] and link != null:
		return link.sort_y() + 1
	return y

func back_ease(t: float) -> float:
	t = clampf(t, 0, 1) - 1
	return t * t * (2.7 * t + 1.7) + 1
