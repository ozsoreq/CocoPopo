class_name Character
extends Thing
## A cutout-rigged character: tinted SVG parts on a bone hierarchy, animated procedurally.

var look: Look

# behaviour state
var walking := false
var tx := 0.0
var ty := 0.0
var waypoints: Array[Vector2] = []
var walk_ph := 0.0
var walk_amt := 0.0
var idle_t := 3.0 + randf() * 6.0
var act := 0          # 0 none, 1 wave, 2 look around, 3 dance
var act_t := 0.0
var chew_t := 0.0
var face_id := 0
var face_t := 0.0
var hug_t := 0.0
var strum_t := 0.0
var slide_t := 0.0
var glance := 0.0
var glance_t := 0.0
var pend: Thing = null
var emote := -1
var emote_t := -1.0
var blink_t := 0.0
var next_blink := 2.0

# pose (read by the rig and the face)
var anim_t := 0.0
var arm := 0.0
var swing := 0.0
var blink := 0.0
var walk := 0.0
var sit := false
var sleep := false
var no_legs := false
var wave := 0.0
var look_amt := 0.0
var dance := 0.0
var hold := false
var hold_type := 0
var hug := 0.0
var strum := 0.0
var face := 0
var gaze_x := 0.0
var gaze_y := 0.0

# rig nodes
var r_hair_back: Node2D
var r_legs: Array[Node2D] = []
var r_leg_spr: Array[Sprite2D] = []
var r_shorts: Array[Sprite2D] = []
var r_shoes: Array[Sprite2D] = []
var r_torso: Node2D
var r_arms: Array[Node2D] = []
var r_head: Node2D
var r_face: Node2D
var r_grip: Node2D
var r_grips: Array[Sprite2D] = []
var r_emote: Sprite2D
var r_body: Node2D   # everything (rotated when lying)

func _init(l: Look = null) -> void:
	is_char = true
	look = l if l != null else Look.preset(0)

func _ready() -> void:
	build()

func size_from_look() -> void:
	bw = 190.0 * max(1.0, look.b()[1])
	bh = look.height() + 10

func build() -> void:
	for c in get_children():
		c.queue_free()
	r_legs.clear(); r_leg_spr.clear(); r_shorts.clear(); r_shoes.clear(); r_arms.clear(); r_grips.clear()
	size_from_look()
	var l := look
	var skin := Look.col(Look.SKIN[l.skin])
	var hair := Look.col(Look.HAIR[l.hair_color])
	var top := Look.col(Look.CLOTH[l.top_color])
	var pants := Look.col(Look.PANTS[l.bottom])
	var shoe := Look.col(Look.SHOES[l.shoe])
	var dress := l.top == 3
	r_body = Node2D.new()
	add_child(r_body)

	r_hair_back = Node2D.new()
	r_body.add_child(r_hair_back)
	r_hair_back.add_child(Art.sprite("hairb_%d" % l.hair_style, hair))

	var bare := dress or l.bstyle != 0
	for s in [-1, 1]:
		var leg := Node2D.new()
		r_body.add_child(leg)
		var spr := Art.sprite("leg_bare" if bare else "leg_pants", skin if bare else pants)
		leg.add_child(spr)
		r_legs.append(leg)
		r_leg_spr.append(spr)
		if l.bstyle == 1 and not dress:
			var sh := Art.sprite("shorts", pants)
			leg.add_child(sh)
			r_shorts.append(sh)
		var shoe_s := Art.sprite("shoe", shoe)
		r_body.add_child(shoe_s)
		r_shoes.append(shoe_s)

	r_torso = Node2D.new()
	r_body.add_child(r_torso)
	if l.bstyle == 2 and not dress:
		r_torso.add_child(Art.sprite("skirt", pants))
	match l.top:
		1:
			r_torso.add_child(Art.sprite("torso_1", top))
			r_torso.add_child(Art.sprite("torso_1s", top.lightened(0.55)))
		4:
			r_torso.add_child(Art.sprite("torso_4", top.lightened(0.5)))
			r_torso.add_child(Art.sprite("torso_4b", pants))
			r_torso.add_child(Art.sprite("buttons_4"))
		5:
			r_torso.add_child(Art.sprite("torso_5"))
		_:
			r_torso.add_child(Art.sprite("torso_%d" % l.top, top))
	var sleeve_col := Color.WHITE if l.top == 5 else (skin if dress else top)
	for s in [-1, 1]:
		var a := Node2D.new()
		a.position = Vector2(s * 62, -80)
		r_torso.add_child(a)
		a.add_child(Art.sprite("arm_bare" if dress else "sleeve", sleeve_col))
		a.add_child(Art.sprite("mitten_r" if s > 0 else "mitten_l", skin))
		r_arms.append(a)

	r_head = Node2D.new()
	r_body.add_child(r_head)
	r_head.add_child(Art.sprite("neck", skin))
	r_head.add_child(Art.sprite("head_%d" % l.head, skin))
	r_face = Node2D.new()
	r_face.set_script(load("res://scripts/face.gd"))
	r_face.ch = self
	r_head.add_child(r_face)
	if Art.has("hairf_%d" % l.hair_style):
		r_head.add_child(Art.sprite("hairf_%d" % l.hair_style, hair))
	if l.acc > 0:
		var tint := Look.col(Look.CLOTH[l.acc_color]) if l.acc in Look.TINTED_ACC else Color.WHITE
		r_head.add_child(Art.sprite("acc_%d" % l.acc, tint))

	# hands drawn over held items
	r_grip = Node2D.new()
	r_grip.z_index = 2
	add_child(r_grip)
	for s in [-1, 1]:
		var g := Art.sprite("mitten_r", skin)
		r_grip.add_child(g)
		r_grips.append(g)
	r_grip.visible = false

	r_emote = Sprite2D.new()
	r_emote.z_index = 5
	add_child(r_emote)
	r_emote.visible = false

func set_look(l: Look) -> void:
	look = l
	build()

func set_face(f: int, secs: float) -> void:
	face_id = f
	face_t = secs

func do_emote(k: int) -> void:
	emote = k
	emote_t = 0.0

# ------------------------------------------------------------------ pose maths (ported from Avatar.java)
func arm_angle(s: int) -> float:
	var a := 14.0 + 150.0 * arm + sin(anim_t * 2.0 + s) * 4.0 * (1.0 - arm)
	a += sin(walk_ph + (PI if s > 0 else 0.0)) * 22.0 * walk
	if dance > 0:
		a += (60.0 + sin(anim_t * 8 + (PI if s > 0 else 0.0)) * 70.0) * dance
	if s > 0 and wave > 0:
		a = a * (1 - wave) + (150.0 + sin(anim_t * 14) * 24.0) * wave
	if hug > 0:
		a = a * (1 - hug) + (-48.0 + sin(anim_t * 6) * 4.0) * hug
	if hold and arm < 0.5:
		if hold_type == 1:
			a = -62.0
		elif hold_type == 2:
			if s > 0: a = 166.0 + sin(anim_t * 3) * 4.0
		elif s > 0:
			a = 40.0 + sin(anim_t * 30) * 12.0 if strum > 0 else 48.0
	if sit and not hold and wave <= 0.1 and dance <= 0.1 and arm < 0.3:
		a = 6.0
	return s * a

func body_bob() -> float:
	if sit:
		return 0.0
	return -absf(sin(walk_ph)) * 7.0 * walk - absf(sin(anim_t * 8)) * 12.0 * dance

## Right-hand centre (or the two-hand grip point) in local units.
func hand_pos() -> Vector2:
	var b: Array = look.b()
	if hold and hold_type == 1 and arm < 0.5:
		return Vector2(0, look.hip_y() + (-146.0 + 98.0) * b[2] + body_bob())
	var a := deg_to_rad(arm_angle(1))
	var hx: float = (62.0 + 80.0 * sin(a)) * b[1]
	var hy := -178.0 + 80.0 * cos(a)
	return Vector2(hx, look.hip_y() + (hy + 98.0) * b[2] + sin(anim_t * 2.4) * 1.1 + body_bob())

func update_rig() -> void:
	var b: Array = look.b()
	var hip := look.hip_y()
	var neck := look.neck_y()
	var breathe := sin(anim_t * 2.4) * 2.2
	var bob := body_bob()
	var spread: float = 25.0 * minf(b[1], 1.15)
	var leg_len := -24.0 - hip
	for i in 2:
		var s := -1 if i == 0 else 1
		var leg := r_legs[i]
		var shoe := r_shoes[i]
		leg.visible = not no_legs
		shoe.visible = not no_legs
		if sit:
			var kick := sin(anim_t * 3 + s) * 3.0
			var sh := maxf(18.0, 30.0 * b[0])
			leg.position = Vector2(s * (spread + 3), hip + 4)
			leg.rotation = 0
			leg.scale = Vector2(1.1, (sh + 22 + kick) / 74.0)
			shoe.position = Vector2(s * (spread + 5), hip + 22 + sh + kick)
		else:
			var lift_l := maxf(0.0, sin(walk_ph + (PI if s > 0 else 0.0))) * 18.0 * walk \
				+ maxf(0.0, sin(anim_t * 8 + (PI if s > 0 else 0.0))) * 10.0 * dance
			leg.position = Vector2(s * spread, hip - lift_l + bob * 0.3)
			leg.rotation = deg_to_rad(s * swing * 14.0)
			leg.scale = Vector2(1, leg_len / 74.0)
			shoe.position = Vector2(s * spread + s * swing * 18.2 + s * 3, -20 - lift_l)
	r_torso.position = Vector2(0, hip + breathe * 0.5 + bob)
	r_torso.scale = Vector2(b[1], b[2])
	for i in 2:
		var s := -1 if i == 0 else 1
		r_arms[i].rotation = deg_to_rad(-arm_angle(s))
	r_head.position = Vector2(look_amt * 7.0, neck + breathe + bob)
	r_head.rotation = deg_to_rad(sin(anim_t * 8) * 7.0 * dance)
	r_head.scale = Vector2.ONE * b[3]
	r_hair_back.position = r_head.position
	r_hair_back.rotation = r_head.rotation
	r_hair_back.scale = r_head.scale
	r_face.queue_redraw()
	# grips over held items
	r_grip.visible = hold and arm < 0.5
	if r_grip.visible:
		var hp := hand_pos()
		if hold_type == 1:
			r_grips[0].position = Vector2(-30, hp.y + 10) - Vector2(0, 82)
			r_grips[1].position = Vector2(30, hp.y + 10) - Vector2(0, 82)
			r_grips[1].visible = true
		else:
			r_grips[0].position = hp - Vector2(0, 82)
			r_grips[1].visible = false

## Called by the location after simulating: places the whole character.
func apply_visual(t: float) -> void:
	anim_t = t + phase
	update_rig()
	var s := sc * back_ease(pop)
	position = Vector2(x, y - lift * 36.0 * sc - hop)
	if state == LIE:
		rotation = deg_to_rad(-90.0 * slot)
		scale = Vector2(s * 0.8 * (-1.0 if slot < 0 else 1.0), s * 0.8)
	else:
		rotation = deg_to_rad(tilt + sin(t * 30) * 6.0 * wiggle)
		scale = Vector2(s * (1.0 - sq * 0.5) * (-1.0 if flip else 1.0) * (1.0 + lift * 0.05), s * (1.0 + sq))
	# emote bubble above the head (kept upright and unflipped)
	if emote_t >= 0:
		r_emote.visible = true
		Art.set_art(r_emote, "emote_%d" % (emote % 7))
		var a := back_ease(emote_t * 4)
		var fo := maxf(0.0, (2.0 - emote_t) / 0.4) if emote_t > 1.6 else 1.0
		var k := a * fo * 0.9 / Art.k
		r_emote.scale = Vector2(k * (-1.0 if flip and state != LIE else 1.0), k)
		r_emote.position = Vector2(0, -(bh if not sit else bh - 60) - 50 - sin(t * 5) * 4)
		r_emote.rotation = 0 if state != LIE else deg_to_rad(90 * slot)
	else:
		r_emote.visible = false
