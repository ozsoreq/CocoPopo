class_name Location
extends Node2D
## One playable place: background, characters and props, the life simulation and all interactions.

signal go_map
signal edit_char(c: Character)
signal new_char

const WB := 2560.0
const H := 1080.0
enum { K_NONE, K_SIT, K_LIE, K_GIVE, K_IN, K_BATHE, K_SLIDE, K_HUG, K_SHOULDER, K_BOUNCE, K_PICK, K_USE }
const BADGE := {K_SIT: [24, "4fb3ff"], K_LIE: [25, "8e7bff"], K_GIVE: [26, "ff9a3d"], K_IN: [27, "58b368"],
	K_BATHE: [28, "3cc5df"], K_SLIDE: [29, "ff6f8f"], K_HUG: [22, "ff5c8a"], K_SHOULDER: [30, "ffb02e"], K_BOUNCE: [30, "b67cff"]}

var loc := "home"
var t := 0.0
var world: Node2D
var layer: Node2D
var fx: Node2D
var badge: Node2D
var badge_icon: Sprite2D
var things: Array[Thing] = []
var sel: Thing = null
var view_w := 1920.0
var view_scale := 1.0
var x_min := 40.0
var x_max := WB - 40.0

# input
var down := false
var moved := false
var drag: Thing = null
var drag_off := Vector2.ZERO
var down_pos := Vector2.ZERO
var last_pos := Vector2.ZERO
var drag_v := Vector2.ZERO
var last_move_ms := 0
var finger := Vector2.ZERO

# drop target
var tg: Thing = null
var tg_kind := K_NONE
var tg_slot := 0
var tg_score := 1.0
var feet := Vector2(-1e9, 0)

var shimmer_t := 3.0
var ui: LocationUI

func setup(place: String) -> void:
	loc = place

func _ready() -> void:
	world = Node2D.new()
	add_child(world)
	var bg := Art.sprite("bg_" + loc)
	bg.z_index = -4000
	world.add_child(bg)
	if loc in ["park", "beach", "fair"]:
		_add_clouds()
	layer = Node2D.new()
	world.add_child(layer)
	fx = Node2D.new()
	fx.set_script(load("res://scripts/particles.gd"))
	fx.z_index = 3900
	world.add_child(fx)
	badge = Node2D.new()
	badge.z_index = 4000
	badge.set_script(load("res://scripts/badge.gd"))
	world.add_child(badge)
	ui = LocationUI.new()
	ui.loc = self
	add_child(ui)
	get_viewport().size_changed.connect(_layout)
	_layout()
	load_state()

func _add_clouds() -> void:
	if loc in ["school", "cafe", "hospital", "market"]:
		return
	for i in 4:
		var c := Art.sprite("cloud", Color(1, 1, 1, 0.95))
		c.z_index = -3990
		c.position = Vector2(randf() * WB, 70 + randf() * 160)
		c.set_meta("speed", 8.0 + randf() * 10.0)
		c.add_to_group("cloud")
		world.add_child(c)

func _layout() -> void:
	var vs := get_viewport().get_visible_rect().size
	var w := vs.x * H / vs.y if vs.y > 0 else 1920.0
	w = vs.x
	if w >= 1920:
		view_scale = 1.0
		view_w = minf(w, WB)
		world.scale = Vector2.ONE
		world.position = Vector2((w - WB) / 2.0, 0)
	else:
		view_scale = w / 1920.0
		view_w = 1920.0
		world.scale = Vector2.ONE * view_scale
		world.position = Vector2((w - WB * view_scale) / 2.0, (H - H * view_scale) / 2.0)
	x_min = (WB - view_w) / 2.0 + 50
	x_max = (WB + view_w) / 2.0 - 50

# ================================================================== things
func add_thing(th: Thing) -> Thing:
	things.append(th)
	layer.add_child(th)
	return th

func remove_thing(th: Thing) -> void:
	things.erase(th)
	if sel == th:
		sel = null
	th.queue_free()

func make_char(l: Look, px: float, py: float, cid := "") -> Character:
	var c := Character.new(l)
	c.x = px; c.y = py
	c.sc = Character.SCENE_SCALE
	c.cid = cid
	return add_thing(c) as Character

func make_prop(pid: String, px: float, py: float) -> Prop:
	var p := Prop.new(pid)
	p.x = px; p.y = py
	return add_thing(p) as Prop

## Finds the character with this identity in this place, or null.
func char_by_cid(cid: String) -> Character:
	if cid == "":
		return null
	for th in things:
		if th.is_char and (th as Character).cid == cid:
			return th as Character
	return null

## Brings a unique character here: if it is already here it just waves; if it is in another place it moves here.
func summon(cid: String, look: Look) -> Character:
	var c := char_by_cid(cid)
	if c != null:
		return c
	var moved_look = Save.take_from_other_places(cid, loc)
	if moved_look != null:
		look = Look.make(moved_look)
	return make_char(look, 0, 0, cid)

func spawn_char(l: Look, cid := "") -> void:
	var c := make_char(l, vis_x(0.5) + (randf() - 0.5) * 300, 420, cid)
	c.pop = 0
	sel = c
	start_fall(c, 0, 0)
	if ui.tray.tab == 1:
		ui.tray.set_tab(1)

func vis_x(frac: float) -> float:
	return (WB - view_w) / 2.0 + frac * view_w

# ================================================================== simulation
func _process(delta: float) -> void:
	var dt := minf(delta, 0.05)
	t += dt
	for c in get_tree().get_nodes_in_group("cloud"):
		if c.get_parent() == world:
			c.position.x += float(c.get_meta("speed")) * dt
			if c.position.x > WB + 200:
				c.position.x = -200
	var i := 0
	while i < things.size():
		var th := things[i]
		update_thing(th, dt, th == drag and moved)
		i += 1
	for th in things:
		th.z_index = clampi(int(sort_key(th)), -3000, 3500)
		th.apply_visual(t)
		if th is Prop:
			_update_overlays(th as Prop)
	_shimmer(dt)
	badge.visible = tg != null and drag != null and moved
	if badge.visible:
		var k: Array = BADGE.get(tg_kind, [16, "ffffff"])
		var bx: float = tg.x + tg.bw * tg.sc * 0.5 + 20
		if absf(bx - drag.x) < 120:
			bx = tg.x - tg.bw * tg.sc * 0.5 - 20
		badge.position = Vector2(clampf(bx, x_min, x_max), maxf(80.0, tg.y + tg.dyo - tg.bh * tg.sc - 10))
		badge.set_badge(k[0], Color(k[1]), t)
	for th in things:
		if th is Prop:
			(th as Prop).glow = 1.0 if (th == tg and drag != null and moved) else 0.0
	ui.update_ui(dt)

func sort_key(th: Thing) -> float:
	if th == drag and moved:
		return 3400
	if drag != null and moved and th.link == drag:
		return 3401
	if not th.is_char and th.id == "rug":
		return -2000 + th.y * 0.1
	if th.state == Thing.HELD and th.link != null:
		return sort_key(th.link) + 1
	return th.sort_y()

func _update_overlays(p: Prop) -> void:
	var occ := occupant(p)
	if p.blanket:
		p.blanket.visible = occ != null and occ.state == Thing.LIE
	if p.front:
		var show := false
		for th in things:
			if th.link == p and ((th.state == Thing.BATHE) or (th.state == Thing.SIT and Rules.covers_sitter(p.id))):
				show = true
		p.front.visible = show
	if p.blanket and p.blanket.visible:
		p.blanket.scale = Vector2(1.0 / Art.k * (-1.0 if p.flip != (occ.slot < 0) else 1.0), 1.0 / Art.k)

func update_thing(o: Thing, dt: float, dragging: bool) -> void:
	o.lift += ((1.0 if dragging else 0.0) - o.lift) * minf(1, dt * 14)
	var tt := clampf(o.vx * 0.04, -14, 14) if dragging else 0.0
	o.tilt += (tt - o.tilt) * minf(1, dt * 12)
	o.vx *= 0.8
	o.hop_v -= 2600 * dt
	o.hop += o.hop_v * dt
	if o.hop < 0:
		if o.hop_v < -500:
			o.sqv = 2.2
		o.hop = 0; o.hop_v = 0
	o.sqv += (-o.sq * 420 - o.sqv * 18) * dt
	o.sq += o.sqv * dt
	if o.pop < 1:
		o.pop = minf(1, o.pop + dt * 3)
	o.wiggle = maxf(0, o.wiggle - dt * 2.5)
	if o.launch_t > 0:
		o.launch_t -= dt
		if o.launch_t <= 0:
			o.sqv = -3; Sfx.play("drop")
	if o.is_char:
		var c := o as Character
		if c.emote_t >= 0:
			c.emote_t += dt
			if c.emote_t > 2: c.emote_t = -1
		c.next_blink -= dt
		if c.next_blink <= 0:
			c.blink_t = 0.14; c.next_blink = 2 + randf() * 3
		if c.blink_t > 0: c.blink_t -= dt
		for f in ["chew_t", "face_t", "hug_t", "strum_t"]:
			if c.get(f) > 0: c.set(f, c.get(f) - dt)
	if dragging:
		o.falling = false
		if o.is_char:
			var c2 := o as Character
			c2.walking = false; c2.pend = null; c2.waypoints.clear()
			update_char(c2, dt, true)
		return

	if o.state != Thing.FREE and (o.link == null or not things.has(o.link) or (o.link.is_char and o.link.state == Thing.HELD)):
		if o.state == Thing.HELD and o.link != null:
			o.link.held = null
		o.state = Thing.FREE; o.link = null
		start_fall(o, 0, 0)
	var l := o.link
	match o.state:
		Thing.SIT:
			if l.is_char:
				var lc := l as Character
				o.x = l.x
				o.y = l.y + l.dyo + (lc.look.neck_y() - 135 * lc.look.b()[3]) * l.sc - (o as Character).look.hip_y() * o.sc
			else:
				var st := Rules.seat(l.id)
				if st.is_empty() or o.slot + 1 >= st.size():
					o.state = Thing.FREE
				else:
					o.x = l.x + st[1 + o.slot] * l.sc * (-1.0 if l.flip else 1.0)
					if l.id == "swing":
						o.x += sin((t + l.phase) * 2) * 10 * l.sc
					o.y = l.y + st[0] * l.sc - (o as Character).look.hip_y() * o.sc - 4 - l.hop - l.lift * 36 * l.sc
		Thing.LIE:
			# head on the pillow, body under the blanket
			o.x = l.x + (-98 * l.sc + 211 * o.sc) * o.slot
			o.y = l.y + Rules.bed(l.id) * l.sc + 34 * o.sc - l.hop
		Thing.BATHE:
			o.x = l.x - 30 * l.sc * (-1.0 if l.flip else 1.0)
			o.y = l.y - 74 * l.sc - (o as Character).look.hip_y() * o.sc - l.hop
			if randf() < dt * 3:
				fx.burst(Vector2(o.x + (randf() - 0.5) * 200 * l.sc, l.y - 130 * l.sc), 1, Color.WHITE, 3, 120)
		Thing.SLIDE:
			var c3 := o as Character
			c3.slide_t += dt / 1.1
			var u := minf(1, c3.slide_t)
			var dir := -1.0 if l.flip else 1.0
			var p0 := Vector2(-70, -290); var cp := Vector2(60, -190); var p1 := Vector2(150, -20)
			var bp := p0 * (1 - u) * (1 - u) + cp * 2 * (1 - u) * u + p1 * u * u
			o.x = l.x + bp.x * l.sc * dir
			o.y = l.y + bp.y * l.sc - c3.look.hip_y() * o.sc * 0.6
			o.flip = l.flip
			if u >= 1:
				o.state = Thing.FREE; o.link = null
				o.x = l.x + 200 * l.sc * dir; o.y = l.y
				o.sqv = -3; c3.set_face(2, 1.5); c3.do_emote(6)
				Sfx.play("drop")
		Thing.ON_TOP:
			o.x = l.x + o.off_x * l.sc
			o.y = l.y + Rules.surface(l.id) * l.sc - l.lift * 36 * l.sc - l.hop
		Thing.HELD:
			var h := l as Character
			var dirh := -1.0 if h.flip else 1.0
			var hs := hold_scale(o) * h.sc
			var hp := h.hand_pos()
			o.x = h.x + hp.x * h.sc * dirh
			o.y = h.y + h.dyo + hp.y * h.sc + o.bh * hs * (0.1 if h.hold_type == 2 else 0.45)
			(o as Prop).ds = hs / o.sc

	if not o.is_char:
		var p := o as Prop
		if p.state != Thing.HELD:
			p.ds = 1.0
		if p.walking:
			var dv := Vector2(p.tx - p.x, p.ty - p.y)
			if dv.length() < 6:
				p.walking = false
			else:
				var step := minf(dv.length(), 420 * dt)
				p.x += dv.normalized().x * step; p.y += dv.normalized().y * step
				if absf(dv.x) > 4:
					p.flip = dv.x < 0 if p.id == "car" else dv.x > 0
				p.hop = absf(sin(t * 18)) * 3

	if o.falling:
		o.vy += 3200 * dt
		o.x += o.fvx * dt
		o.y += o.vy * dt
		o.peak_y = minf(o.peak_y, o.y)
		if o.x < x_min: o.x = x_min; o.fvx = absf(o.fvx) * 0.6
		if o.x > x_max: o.x = x_max; o.fvx = -absf(o.fvx) * 0.6
		var rest_link: Array = [null]
		var rest := rest_for(o, rest_link)
		if o.y >= rest and o.vy > 0:
			o.y = rest
			if not o.is_char and Rules.tossable(o.id) and o.vy > 800:
				o.vy = -o.vy * (0.62 if o.id == "ball" else 0.38)
				o.fvx *= 0.7
				o.peak_y = o.y
				Sfx.play("bounce", -8)
			else:
				if o.is_char and o.vy > 1900:
					(o as Character).set_face(3, 1.2); (o as Character).do_emote(1)
				o.falling = false; o.vy = 0; o.fvx = 0
				o.sqv = -2.6
				if rest_link[0] != null:
					o.state = Thing.ON_TOP; o.link = rest_link[0]; o.off_x = (o.x - o.link.x) / o.link.sc
				Sfx.play("drop")
	if o.is_char:
		update_char(o as Character, dt, false)

func rest_for(o: Thing, out_link: Array) -> float:
	var rest := Rules.floor_below(loc, o.peak_y)
	if o.is_char or not Rules.tossable(o.id):
		return rest
	for s in things:
		if s == o or s.is_char:
			continue
		var top := Rules.surface(s.id)
		if top == 0:
			continue
		var top_y := s.y + top * s.sc
		if absf(o.x - s.x) < s.bw * s.sc * 0.42 and top_y >= o.peak_y - 8 and top_y < rest:
			rest = top_y
			out_link[0] = s
	return rest

func start_fall(o: Thing, fvx: float, fvy: float) -> void:
	if o.is_char or not (Rules.wall(o.id) or Rules.floats(o.id)):
		o.falling = true
		o.fvx = fvx; o.vy = fvy
		o.peak_y = o.y

func hold_scale(item: Thing) -> float:
	return minf(1, 120.0 / maxf(item.bw, item.bh)) * item.sc * (1 - item.bites * 0.22)

func update_char(c: Character, dt: float, dragging: bool) -> void:
	c.anim_t = t + c.phase
	var em := 1.0 if (c.emote_t >= 0 and c.emote_t < 0.8) else 0.0
	c.arm = maxf(maxf(c.lift, em), 1.0 if (c.falling or c.state == Thing.SLIDE) else 0.0)
	if c.walking and absf(c.ty - c.y) > absf(c.tx - c.x) * 2:
		c.arm = maxf(c.arm, 0.7)
	c.swing = maxf(c.lift, 0.6 if c.falling else 0.0) * sin(t * 16 + c.phase) * 0.8
	c.blink = sin(PI * (1 - c.blink_t / 0.14)) if c.blink_t > 0 else 0.0
	c.sit = c.state in [Thing.SIT, Thing.BATHE, Thing.SLIDE]
	c.sleep = c.state == Thing.LIE
	c.no_legs = c.state == Thing.BATHE
	c.hold = c.held != null
	c.hold_type = Rules.hold_type(c.held.id) if c.held != null else 0
	c.strum += ((1.0 if c.strum_t > 0 else 0.0) - c.strum) * minf(1, dt * 10)
	c.hug += ((1.0 if c.hug_t > 0 else 0.0) - c.hug) * minf(1, dt * 8)
	if c.act_t > 0:
		c.act_t -= dt
		if c.act_t <= 0: c.act = 0
	var k := minf(1, dt * 8)
	c.wave += ((1.0 if c.act == 1 else 0.0) - c.wave) * k
	c.look_amt += ((sin(t * 2.4 + c.phase) if c.act == 2 else 0.0) - c.look_amt) * k
	c.dance += ((1.0 if c.act == 3 else 0.0) - c.dance) * k

	var face := 0
	if c.face_t > 0: face = c.face_id
	elif dragging: face = 2 if fmod(t + c.phase, 1.4) < 0.7 else 3
	elif c.falling: face = 8
	elif c.hug_t > 0: face = 7
	elif c.chew_t > 0: face = 4
	elif c.state == Thing.BATHE: face = 1
	elif c.emote_t >= 0: face = [7, 3, 1, 3, 0, 6, 2][c.emote % 7]
	elif c.strum_t > 0: face = 2

	var gx := 0.0
	var gy := 0.0
	var tv := watching(c)
	if c.held != null:
		gx = (-1.0 if c.flip else 1.0) * 0.7; gy = 0.6
	elif tv != null:
		gx = -1.0 if tv.x < c.x else 1.0; gy = -0.2
		if tv.pstate == 2 and face == 0 and int(t + c.phase) % 5 == 0: face = 2
		if c.state == Thing.SIT and face == 0: face = 1
	elif c.walking:
		gx = -0.8 if c.tx < c.x else 0.8
	else:
		c.glance_t -= dt
		if c.glance_t <= 0:
			c.glance = (randf() - 0.5) * 1.6; c.glance_t = 1 + randf() * 3
		gx = c.glance
	c.gaze_x += (gx - c.gaze_x) * minf(1, dt * 6)
	c.gaze_y += (gy - c.gaze_y) * minf(1, dt * 6)
	if tv != null and c.act == 0:
		c.look_amt += ((-0.45 if tv.x < c.x else 0.45) - c.look_amt) * k
	c.face = face

	if c.walking and c.state == Thing.FREE and not dragging:
		var dv := Vector2(c.tx - c.x, c.ty - c.y)
		var d := dv.length()
		if d < 6:
			c.x = c.tx; c.y = c.ty
			if not c.waypoints.is_empty():
				var w: Vector2 = c.waypoints.pop_front()
				c.tx = w.x; c.ty = w.y
			else:
				c.walking = false
				if c.pend != null:
					var tgt := c.pend
					c.pend = null
					arrive(c, tgt)
		else:
			var step := minf(d, (150 + 120 * c.sc) * dt)
			c.x += dv.x / d * step; c.y += dv.y / d * step
			if absf(dv.x) > 4: c.flip = dv.x < 0
			c.walk_ph += dt * 11
	else:
		c.walking = false
	c.walk_amt += ((1.0 if c.walking else 0.0) - c.walk_amt) * minf(1, dt * 10)
	c.walk = c.walk_amt
	c.dyo = -c.lift * 36 * c.sc - c.hop

	if not dragging and not c.falling and c != drag:
		c.idle_t -= dt
		if c.idle_t <= 0:
			c.idle_t = 4 + randf() * 7
			if not c.walking:
				pick_idle(c)
		if c.state == Thing.LIE and c.emote_t < 0 and randf() < dt * 0.3:
			c.do_emote(5)

func watching(c: Character) -> Thing:
	if c.state != Thing.FREE and c.state != Thing.SIT:
		return null
	var best: Thing = null
	var bd := 900.0
	for q in things:
		if q.is_char or q.id != "tv" or q.pstate == 1:
			continue
		var d := absf(q.x - c.x) + absf(q.y - c.y) * 2
		if d < bd and absf(q.x - c.x) > 60:
			bd = d; best = q
	return best

func pick_idle(c: Character) -> void:
	var r := randi() % 20
	match c.state:
		Thing.LIE:
			if r < 2: stand_up(c, 1)
			return
		Thing.BATHE:
			if r < 6:
				stand_up(c, 1)
				fx.burst(Vector2(c.x, c.y - 200 * c.sc), 10, Color("ffe066"), 2, 400)
				Sfx.play("spark")
			else:
				c.do_emote(6)
			return
		Thing.SIT:
			if c.link != null and c.link.is_char:
				if r < 6: c.do_emote(6)
				return
			if r < 3: stand_up(c, 0); return
			if c.held != null and Rules.food(c.held.id) and r < 9: eat(c); return
			if r < 10: c.act = 2; c.act_t = 2.5
			elif r < 15: c.act = 1; c.act_t = 1.8
			else: c.do_emote(randi() % 7)
			return
		Thing.FREE:
			pass
		_:
			return
	if c.held != null and Rules.food(c.held.id) and r < 8: eat(c); return
	if c.held != null and not Rules.food(c.held.id) and r < 3: put_down(c); return
	if r < 7 and auto_use(c): return
	if r < 12:
		var f := Rules.floors(loc)
		var bi := Rules.band(loc, c.y)
		var dist := (120 + randf() * 260) * c.sc * (1.0 if randf() < 0.5 else -1.0)
		c.tx = clampf(c.x + dist, x_min + 20, x_max - 20)
		c.ty = clampf(c.y + (randf() - 0.5) * 80, f[bi] + 10, f[bi + 1] - 6)
		c.waypoints.clear()
		c.walking = true
	elif r < 14: c.act = 1; c.act_t = 1.8
	elif r < 16: c.act = 2; c.act_t = 2.6
	elif r < 18: c.act = 3; c.act_t = 2.6
	else:
		c.hop_v = 560; c.do_emote(randi() % 7)

func auto_use(c: Character) -> bool:
	var bi := Rules.band(loc, c.y)
	var best: Thing = null
	var best_score := 0.0
	for q in things:
		if q == c or q.is_char or q.state == Thing.HELD:
			continue
		if absf(q.x - c.x) > 750 or Rules.band(loc, q.y) != bi or q.x < x_min or q.x > x_max:
			continue
		var score := 0.0
		if not Rules.seat(q.id).is_empty() and free_slot(q, c) >= 0: score = 2
		elif c.held == null and Rules.holdable(q.id) and not Rules.floats(q.id): score = 3.0 if Rules.food(q.id) else 1.5
		elif Rules.is_tub(q.id) and occupant(q) == null: score = 1
		elif Rules.is_slide(q.id): score = 1.2
		if score <= 0:
			continue
		score *= 0.5 + randf()
		if score > best_score:
			best_score = score; best = q
	if best == null:
		return false
	return walk_use(c, best)

func stand_up(c: Character, face: int) -> void:
	var l := c.link
	c.state = Thing.FREE; c.link = null
	if l != null:
		c.x = clampf(l.x + (1.0 if randf() < 0.5 else -1.0) * l.bw * l.sc * 0.4, x_min, x_max)
		c.y = Rules.floor_below(loc, l.y - 4)
		if l.is_char:
			c.y = l.y + 10
	c.hop_v = 420
	if face != 0:
		c.set_face(face, 1.2)

func put_down(c: Character) -> void:
	var it := c.held
	if it == null:
		return
	c.held = null; it.state = Thing.FREE; it.link = null
	it.x = c.x + (-1.0 if c.flip else 1.0) * 90 * c.sc
	start_fall(it, 0, 0)

func _shimmer(dt: float) -> void:
	shimmer_t -= dt
	if shimmer_t > 0:
		return
	shimmer_t = 6 + randf() * 4
	var cands: Array[Thing] = []
	for q in things:
		if not q.is_char and q.state == Thing.FREE and q.x > x_min and q.x < x_max and (Rules.states(q.id) > 0 or Rules.capacity(q.id) > 0 \
				or not Rules.seat(q.id).is_empty() or Rules.bed(q.id) != 0 or Rules.is_tub(q.id) or Rules.is_slide(q.id) or q.id in ["tree", "bush", "palm"]):
			cands.append(q)
	if not cands.is_empty():
		var q: Thing = cands[randi() % cands.size()]
		fx.burst(Vector2(q.x, q.y - q.bh * q.sc * 0.55), 7, Color("fff3a0"), 2, 220)

# ================================================================== interactions
func _consider(obj: Thing, kind: int, slot: int, c: Vector2, rx: float, ry: float, pts: Array) -> void:
	var best := 1e9
	for p in pts:
		var d := pow((p.x - c.x) / rx, 2) + pow((p.y - c.y) / ry, 2)
		best = minf(best, d)
	if best <= 1 and best < tg_score:
		tg_score = best; tg = obj; tg_kind = kind; tg_slot = slot

## Finds what the dragged thing would interact with if released now (forgiving: finger, body or feet).
func resolve(d: Thing, f: Vector2) -> void:
	tg = null; tg_kind = K_NONE; tg_score = 1.0001
	if d == null:
		return
	if d.is_char:
		var dc := d as Character
		var body := Vector2(d.x, d.y - (d.bh - 60) * d.sc * 0.5)
		var pts := [f, body, Vector2(d.x, d.y)]
		for q in things:
			if q == d:
				continue
			var qs := maxf(q.sc, 0.85)
			if q.is_char:
				if q.state in [Thing.HELD, Thing.LIE, Thing.BATHE, Thing.SLIDE] or q.link == d:
					continue
				var qc := q as Character
				var head_y := q.y + q.dyo + (qc.look.neck_y() - 90) * q.sc
				if not has_rider(q):
					_consider(q, K_SHOULDER, 0, Vector2(q.x, head_y), 95 * qs, 80 * qs, [f, body + Vector2(0, -60 * d.sc)])
				if q.state == Thing.FREE:
					_consider(q, K_HUG, 0, Vector2(q.x, q.y - q.bh * q.sc * 0.4), 120 * qs, 130 * qs, [f, body])
				continue
			var st := Rules.seat(q.id)
			if not st.is_empty():
				for j in range(st.size() - 1):
					if seat_taken(q, j):
						continue
					var sx: float = q.x + st[1 + j] * q.sc * (-1.0 if q.flip else 1.0)
					_consider(q, K_SIT, j, Vector2(sx, q.y + (st[0] - 40) * q.sc), 120 * qs, 130 * qs, pts)
			var m := Rules.bed(q.id)
			if m != 0 and occupant(q) == null:
				_consider(q, K_LIE, 0, Vector2(q.x, q.y + (m - 30) * q.sc), q.bw * q.sc * 0.6, 150 * qs, pts)
			if Rules.is_tub(q.id) and occupant(q) == null:
				_consider(q, K_BATHE, 0, Vector2(q.x, q.y - 110 * q.sc), q.bw * q.sc * 0.55, 140 * qs, pts)
			if Rules.is_slide(q.id):
				_consider(q, K_SLIDE, 0, Vector2(q.x - 100 * q.sc * (-1.0 if q.flip else 1.0), q.y - 300 * q.sc), 140 * qs, 140 * qs, pts)
			if Rules.bouncy(q.id) and q.state == Thing.FREE:
				_consider(q, K_BOUNCE, 0, Vector2(q.x, q.y - 80 * q.sc), 110 * qs, 110 * qs, [f, Vector2(d.x, d.y)])
		var _unused := dc
	else:
		var body2 := Vector2(d.x, d.y - d.bh * d.sc * 0.5)
		for q in things:
			if q == d:
				continue
			var qs2 := maxf(q.sc, 0.85)
			if q.is_char:
				if q.held == null and Rules.holdable(d.id) and not q.state in [Thing.LIE, Thing.HELD, Thing.SLIDE]:
					_consider(q, K_GIVE, 0, Vector2(q.x, q.y + q.dyo - q.bh * q.sc * 0.45), 130 * qs2, q.bh * q.sc * 0.5, [f, body2])
				continue
			var cap := Rules.capacity(q.id)
			if cap > 0 and q.contents.size() < cap and Rules.holdable(d.id) and Rules.capacity(d.id) == 0 and not (q.id == "gift" and q.pstate == 0):
				var zy := q.y - 120 * q.sc if Rules.is_tub(q.id) else q.y - q.bh * q.sc * 0.55
				_consider(q, K_IN, 0, Vector2(q.x, zy), q.bw * q.sc * 0.5, maxf(90, q.bh * q.sc * 0.5), [f, body2])

func has_rider(c: Thing) -> bool:
	for q in things:
		if q.is_char and q.link == c and q.state == Thing.SIT:
			return true
	return false

func occupant(p: Thing) -> Thing:
	for q in things:
		if q.is_char and q.link == p and (q.state == Thing.LIE or q.state == Thing.BATHE):
			return q
	return null

func seat_taken(s: Thing, slot: int) -> bool:
	for q in things:
		if q.is_char and q.link == s and q.state == Thing.SIT and q.slot == slot:
			return true
	return false

func free_slot(s: Thing, who: Thing) -> int:
	var st := Rules.seat(s.id)
	if st.is_empty():
		return -1
	var best := -1
	var bd := 1e9
	for j in range(st.size() - 1):
		if seat_taken(s, j):
			continue
		var d := absf(s.x + st[1 + j] * s.sc * (-1.0 if s.flip else 1.0) - who.x)
		if d < bd:
			bd = d; best = j
	return best

func pick_up(o: Thing) -> void:
	Sfx.play("pop")
	haptic()
	o.falling = false
	if o.is_char:
		var c := o as Character
		c.walking = false; c.act = 0; c.pend = null; c.waypoints.clear()
	if o.state == Thing.HELD and o.link != null:
		o.link.held = null
	o.state = Thing.FREE; o.link = null

func drop(o: Thing) -> void:
	resolve(o, finger)
	var target := tg
	var kind := tg_kind
	var slot := tg_slot
	tg = null; tg_kind = K_NONE
	if target != null and perform(o, target, kind, slot):
		return
	if Time.get_ticks_msec() - last_move_ms > 80:
		drag_v = Vector2.ZERO
	if o.is_char:
		start_fall(o, 0, 0)
	else:
		var toss := Rules.tossable(o.id)
		start_fall(o, clampf(drag_v.x, -2600, 2600) * 0.8 if toss else 0.0, clampf(drag_v.y, -2600, 1200) * 0.8 if toss else 0.0)
		if not o.falling:
			o.sqv = -2.4; Sfx.play("drop")
		elif absf(drag_v.x) + absf(drag_v.y) > 1500:
			Sfx.play("whoosh")

func perform(o: Thing, q: Thing, kind: int, slot: int) -> bool:
	o.falling = false
	if o.is_char:
		(o as Character).walking = false
		(o as Character).waypoints.clear()
	match kind:
		K_SIT:
			if seat_taken(q, slot): return false
			o.state = Thing.SIT; o.link = q; o.slot = slot; o.flip = false
			o.sqv = -2.4; (o as Character).set_face(1, 1.2)
			Sfx.play("drop" if not Rules.vehicle(q.id) else "vroom")
			return true
		K_LIE:
			if occupant(q) != null: return false
			o.state = Thing.LIE; o.link = q; o.slot = -1 if q.flip else 1
			(o as Character).do_emote(5)
			Sfx.play("yawn")
			return true
		K_BATHE:
			if occupant(q) != null: return false
			o.state = Thing.BATHE; o.link = q; o.flip = false
			(o as Character).set_face(2, 1.5)
			fx.burst(Vector2(q.x, q.y - 130 * q.sc), 14, Color.WHITE, 3, 260)
			Sfx.play("splash")
			return true
		K_SLIDE:
			o.state = Thing.SLIDE; o.link = q; (o as Character).slide_t = 0
			(o as Character).set_face(2, 1.6)
			Sfx.play("wheee")
			return true
		K_HUG:
			var side := 1.0 if o.x >= q.x else -1.0
			o.x = clampf(q.x + side * 118 * maxf(o.sc, q.sc), x_min, x_max)
			o.y = q.y
			o.flip = side > 0; q.flip = side < 0
			(o as Character).hug_t = 1.8; (q as Character).hug_t = 1.8
			(o as Character).do_emote(0); (q as Character).do_emote(0)
			(q as Character).walking = false; (q as Character).act = 0
			fx.burst(Vector2((o.x + q.x) / 2, o.y - 260 * o.sc), 6, Color("ff6f8f"), 2, 260)
			Sfx.play("spark")
			return true
		K_SHOULDER:
			o.state = Thing.SIT; o.link = q; o.slot = 0; o.flip = q.flip
			(o as Character).set_face(2, 1.5); (q as Character).set_face(3, 1)
			Sfx.play("tada")
			return true
		K_BOUNCE:
			o.x = q.x; o.y = q.y - 100 * q.sc
			o.falling = true; o.fvx = (randf() - 0.5) * 300; o.vy = -2100; o.peak_y = o.y
			q.sqv = -4
			(o as Character).set_face(2, 1.5)
			Sfx.play("bounce")
			return true
		K_GIVE, K_PICK:
			var ch: Character = (q if kind == K_GIVE else o) as Character
			var it: Thing = o if kind == K_GIVE else q
			if ch.held != null or not Rules.holdable(it.id): return false
			if it.state == Thing.HELD and it.link != null:
				it.link.held = null
			ch.held = it; it.state = Thing.HELD; it.link = ch; it.falling = false
			ch.hop_v = 380
			Sfx.play("spark")
			if Rules.food(it.id):
				eat(ch)
			else:
				ch.do_emote(1); ch.set_face(1, 1.2)
			return true
		K_IN:
			if q.contents.size() >= Rules.capacity(q.id): return false
			q.contents.append(o.id)
			remove_thing(o)
			if sel == o: sel = q
			q.wiggle = 0.8; q.sqv = -2
			if q.id == "fridge": q.pstate = 1
			(q as Prop).refresh_art()
			fx.burst(Vector2(q.x, q.y - q.bh * q.sc * 0.6), 5, Color("ffe066"), 2, 250)
			Sfx.play("pop")
			return true
		K_USE:
			tap_thing(q)
			return true
	return false

func use_kind(c: Character, q: Thing) -> int:
	if q.is_char: return K_NONE
	if not Rules.seat(q.id).is_empty() and free_slot(q, c) >= 0: return K_SIT
	if Rules.bed(q.id) != 0 and occupant(q) == null: return K_LIE
	if Rules.is_tub(q.id) and occupant(q) == null: return K_BATHE
	if Rules.is_slide(q.id): return K_SLIDE
	if Rules.bouncy(q.id) and q.state == Thing.FREE: return K_BOUNCE
	if Rules.holdable(q.id) and c.held == null and q.state != Thing.HELD: return K_PICK
	if Rules.states(q.id) > 0 or Rules.capacity(q.id) > 0 or q.id in ["tree", "palm", "bush", "rocket", "camera", "guitar"]: return K_USE
	return K_NONE

func walk_use(c: Character, q: Thing) -> bool:
	var kind := use_kind(c, q)
	if kind == K_NONE or c.state == Thing.HELD:
		return false
	if c.state != Thing.FREE:
		stand_up(c, 0)
	var dir := -1.0 if c.x < q.x else 1.0
	var ux := q.x + dir * minf(q.bw * q.sc * 0.5 + 40, 220)
	if kind == K_SIT:
		var st := Rules.seat(q.id)
		ux = q.x + st[1 + free_slot(q, c)] * q.sc * (-1.0 if q.flip else 1.0)
	var base_y := q.link.y if (q.state == Thing.ON_TOP and q.link != null) else q.y
	var uy := Rules.floor_below(loc, base_y - 4) + 6
	plan_walk(c, clampf(ux, x_min, x_max), uy)
	c.pend = q
	c.act = 0; c.idle_t = 8
	return true

## Route to (px, py); uses the ladder when the destination is on another floor.
func plan_walk(c: Character, px: float, py: float) -> void:
	var f := Rules.floors(loc)
	var from := Rules.band(loc, c.y)
	var to := Rules.band(loc, py)
	c.waypoints.clear()
	if from != to and Rules.ladder(loc) >= 0:
		var lx := Rules.ladder(loc) * WB
		c.tx = lx; c.ty = f[from] + 30
		c.waypoints.append(Vector2(lx, f[to] + 30))
		c.waypoints.append(Vector2(px, py))
	else:
		c.tx = px; c.ty = py
	c.walking = true

func arrive(c: Character, q: Thing) -> void:
	if not things.has(q):
		return
	var kind := use_kind(c, q)
	perform(c, q, kind, free_slot(q, c) if kind == K_SIT else 0)

func spawn_from(pid: String, px: float, py: float, s: float) -> void:
	var g := make_prop(pid, px, py)
	g.sc = clampf(s, 0.8, 1.2); g.pop = 0
	start_fall(g, (randf() - 0.5) * 600, -900)

func react(px: float, radius: float, face: int, secs: float) -> void:
	for q in things:
		if q.is_char and absf(q.x - px) < radius and q.state != Thing.LIE:
			(q as Character).set_face(face, secs)

func strum(player: Character) -> void:
	if player != null:
		player.strum_t = 2.6; player.do_emote(2)
		for q in things:
			if q.is_char and q != player and q.state == Thing.FREE and absf(q.x - player.x) < 600:
				(q as Character).act = 3; (q as Character).act_t = 2.6
	Sfx.play("strum")

func camera_flash() -> void:
	ui.flash()
	for q in things:
		if q.is_char and q.state != Thing.LIE:
			var c := q as Character
			c.set_face(3 if randf() < 0.5 else 2, 1.5)
			if c.held == null:
				c.act = 1; c.act_t = 1.4
	Sfx.play("toggle")

func eat(c: Character) -> void:
	var f := c.held
	if f == null:
		return
	c.chew_t = 0.9
	f.bites += 1
	var dir := -1.0 if c.flip else 1.0
	fx.burst(Vector2(c.x + 30 * dir * c.sc, c.y + c.dyo - 225 * c.sc), 8, Color("8fd8ff") if Rules.drink(f.id) else Color("e9b36c"), 0, 300)
	Sfx.play("bite")
	if f.bites >= 3:
		remove_thing(f)
		c.held = null
		c.do_emote(0); c.set_face(7, 1.4)

func tap_thing(o: Thing) -> void:
	haptic()
	if o.is_char:
		var c := o as Character
		if c.held != null and Rules.food(c.held.id): eat(c); return
		if c.held != null and c.held.id == "guitar": strum(c); return
		if c.held != null and c.held.id == "camera": camera_flash(); return
		if c.state == Thing.LIE:
			stand_up(c, 1); c.do_emote(6); Sfx.play("tick"); return
		c.hop_v = 560.0 if c.state == Thing.FREE else 0.0
		c.wiggle = 0.0 if c.state == Thing.FREE else 0.6
		c.do_emote(randi() % 7)
		Sfx.play("squeak")
		return
	var p := o as Prop
	if not p.contents.is_empty() and not (p.id == "fridge" and p.pstate == 0):
		var cid: String = p.contents.pop_back()
		var g := make_prop(cid, p.x, p.y - p.bh * p.sc * 0.6)
		g.pop = 0
		start_fall(g, (randf() - 0.5) * 700, -1300)
		p.wiggle = 0.7
		if p.id == "gift": p.pstate = 1
		p.refresh_art()
		Sfx.play("pop")
		return
	match p.id:
		"gift":
			if p.pstate == 0:
				p.pstate = 1; p.wiggle = 1; p.refresh_art()
				var pool := ["teddy", "ball", "duck", "rocket", "car", "cupcake", "donut", "balloon", "trophy", "icecream"]
				spawn_from(pool[randi() % pool.size()], p.x, p.y - 100 * p.sc, p.sc)
				fx.burst(Vector2(p.x, p.y - 100 * p.sc), 14, Color("ffd43b"), 2, 600)
				react(p.x, 600, 3, 1.4)
				Sfx.play("tada")
				return
		"tree":
			p.wiggle = 1
			spawn_from("apple", p.x + (randf() - 0.5) * 140 * p.sc, p.y - 300 * p.sc, p.sc)
			Sfx.play("tick"); return
		"palm":
			p.wiggle = 1
			spawn_from("coconut", p.x + 40 * p.sc, p.y - 320 * p.sc, p.sc)
			Sfx.play("tick"); return
		"bush":
			p.wiggle = 1
			fx.burst(Vector2(p.x, p.y - 80 * p.sc), 6, Color("5bd07a"), 0, 300)
			if randi() % 3 == 0:
				var pool2 := ["ball", "duck", "flower", "teddy", "mushroom"]
				spawn_from(pool2[randi() % pool2.size()], p.x, p.y - 120 * p.sc, p.sc)
				Sfx.play("spark")
			else:
				Sfx.play("tick")
			return
		"balloon":
			if p.state == Thing.FREE:
				fx.burst(Vector2(p.x, p.y - 220 * p.sc), 18, Color("ff5c73"), 2, 700)
				remove_thing(p)
				react(p.x, 500, 3, 1)
				Sfx.play("bounce"); return
		"rocket":
			if p.launch_t <= 0:
				p.launch_t = 3; p.wiggle = 0.5; Sfx.play("whoosh"); return
		"camera":
			camera_flash(); return
		"tub":
			fx.burst(Vector2(p.x, p.y - 130 * p.sc), 16, Color.WHITE, 3, 300)
			Sfx.play("splash"); return
		"guitar":
			strum(null); return
		"duck":
			p.hop_v = 420; Sfx.play("squeak"); return
		"shelf":
			spawn_from("books", p.x, p.y - 200 * p.sc, p.sc)
			Sfx.play("pop"); return
	var n := Rules.states(p.id)
	if n > 0:
		p.pstate = (p.pstate + 1) % n; p.wiggle = 0.7
		p.refresh_art()
		Sfx.play("toggle")
		if p.id == "lamp":
			if p.pstate == 1:
				fx.burst(Vector2(p.x, p.y - 280 * p.sc), 8, Color("ffe066"), 2, 300)
			else:
				react(p.x, 700, 6, 1.6); Sfx.play("yawn")
		if p.id == "tv" and p.pstate == 2:
			react(p.x, 900, 2, 1.2)
		return
	if Rules.tossable(p.id) and p.state == Thing.FREE and not p.falling:
		start_fall(p, (randf() - 0.5) * 900, -1300)
		Sfx.play("bounce"); return
	p.hop_v = 420; p.wiggle = 0.5
	Sfx.play("tick")

## Tapping empty floor sends the selected character (or its vehicle) there.
func tap_floor(p: Vector2) -> void:
	var c := sel as Character if (sel != null and sel.is_char) else null
	if c == null or c.state == Thing.HELD:
		sel = null; return
	var f := Rules.floors(loc)
	var bt := -1
	for i in range(0, f.size(), 2):
		if p.y >= f[i] - 20 and p.y <= f[i + 1]:
			bt = i
	if bt < 0:
		sel = null; return
	var tx := clampf(p.x, x_min, x_max)
	var ty := clampf(p.y, f[bt] + 8, f[bt + 1] - 4)
	if c.state == Thing.SIT and c.link != null and not c.link.is_char and Rules.vehicle(c.link.id):
		var v := c.link as Prop
		if Rules.band(loc, v.y) == bt:
			v.tx = tx; v.ty = ty; v.walking = true
			fx.burst(Vector2(tx, ty), 1, Color.WHITE, 1, 0)
			Sfx.play("vroom"); return
	if c.state != Thing.FREE:
		stand_up(c, 0)
	if Rules.band(loc, c.y) != bt and Rules.ladder(loc) < 0:
		sel = null; return
	plan_walk(c, tx, ty)
	c.pend = null; c.act = 0; c.idle_t = 6 + randf() * 6
	fx.burst(Vector2(tx, ty), 1, Color.WHITE, 1, 0)
	Sfx.play("tick")

func haptic() -> void:
	if OS.has_feature("mobile"):
		Input.vibrate_handheld(12)

# ================================================================== input
func world_pos(screen: Vector2) -> Vector2:
	return (screen - world.position) / world.scale.x

func thing_at(p: Vector2) -> Thing:
	var best: Thing = null
	var bz := -100000
	for th in things:
		var z := th.z_index
		if th.is_char and th.state == Thing.SIT and th.link != null and th.link.is_char:
			z = th.link.z_index + 1   # shoulder riders are grabbable even though drawn behind the head
		if th.hit(p) and z > bz:
			bz = z; best = th
	return best

func _unhandled_input(e: InputEvent) -> void:
	if e is InputEventMouseButton and e.button_index == MOUSE_BUTTON_LEFT:
		var p := world_pos(e.position)
		if e.pressed:
			on_down(p)
		else:
			on_up(p)
	elif e is InputEventMouseMotion and down:
		on_move(world_pos(e.position))

func on_down(p: Vector2) -> void:
	down = true; moved = false
	down_pos = p; last_pos = p
	drag_v = Vector2.ZERO; last_move_ms = Time.get_ticks_msec()
	drag = thing_at(p)
	if drag != null:
		drag_off = Vector2(drag.x, drag.y) - p

func on_move(p: Vector2) -> void:
	var now := Time.get_ticks_msec()
	var mdt := maxf(0.004, (now - last_move_ms) / 1000.0)
	last_move_ms = now
	if drag != null:
		if not moved and p.distance_to(down_pos) > 18:
			moved = true
			sel = drag
			pick_up(drag)
		if moved:
			drag_v = drag_v * 0.5 + (p - last_pos) / mdt * 0.5
			drag.vx = (p.x - last_pos.x) * 60
			drag.x = clampf(p.x + drag_off.x, x_min, x_max)
			drag.y = clampf(p.y + drag_off.y, 150, H - 6)
			if drag.held != null:
				drag.dyo = -drag.lift * 36 * drag.sc
			finger = p
			var before := tg
			resolve(drag, p)
			if tg != null and tg != before:
				haptic(); Sfx.play("tick", -10)
	elif p.distance_to(down_pos) > 18:
		moved = true
	last_pos = p

func on_up(p: Vector2) -> void:
	if not down:
		return
	down = false
	if drag != null:
		var o := drag
		drag = null
		var prev := sel
		if not moved and o.state == Thing.HELD and o.link != null and o.link.is_char:
			o = o.link   # tapping what someone holds = tapping them (eat, strum, flash...)
		if not moved:
			if not o.is_char and prev != null and prev.is_char and prev != o and prev.state != Thing.HELD \
					and o.state != Thing.HELD and things.has(prev) and walk_use(prev as Character, o):
				sel = prev
				fx.burst(Vector2(o.x, o.y - o.bh * o.sc * 0.5), 6, Color("ffe066"), 2, 260)
				Sfx.play("tick")
			else:
				sel = o
				tap_thing(o)
		else:
			sel = o
			drop(o)
		tg = null
	elif not moved:
		tap_floor(p)
	moved = false

## Begin dragging something spawned from the tray (finger already on screen).
func begin_external_drag(th: Thing, screen: Vector2) -> void:
	var p := world_pos(screen)
	th.x = p.x; th.y = p.y + th.bh * th.sc * 0.4
	drag = th; moved = true; down = true
	drag_off = Vector2(th.x, th.y) - p
	last_pos = p; last_move_ms = Time.get_ticks_msec()
	sel = th
	Sfx.play("pop")

func external_move(screen: Vector2) -> void:
	on_move(world_pos(screen))

func external_up(screen: Vector2) -> void:
	on_up(world_pos(screen))

# ================================================================== persistence
func save_state() -> void:
	var recs := []
	for th in things:
		var r := {"c": th.is_char, "x": th.x, "y": th.y, "sc": th.sc, "flip": th.flip, "st": th.state,
			"ln": things.find(th.link) if th.link != null else -1, "sl": th.slot, "ps": th.pstate, "bi": th.bites,
			"ct": th.contents, "ox": th.off_x}
		if th.is_char:
			r["look"] = (th as Character).look.to_array()
			r["cid"] = (th as Character).cid
		else:
			r["id"] = th.id
		recs.append(r)
	Save.data[loc] = recs
	Save.write()

func load_state() -> void:
	var recs = Save.data.get(loc)
	if recs == null:
		for d in Rules.DEFAULTS.get(loc, []):
			var parts: PackedStringArray = d.split(":")
			var px := vis_x(float(parts[1]))
			var py := float(parts[2])
			var th: Thing
			if parts[0].begins_with("@"):
				var pi := int(parts[0].substr(1))
				var cid := "p%d" % pi
				if Save.cid_anywhere(cid):
					continue   # this character is visiting another place
				th = make_char(Look.preset(pi), px, py, cid)
				th.sc = float(parts[3]) * Character.SCENE_SCALE
			else:
				th = make_prop(parts[0], px, py)
				th.sc = float(parts[3])
		return
	for r in recs:
		var th: Thing
		if r["c"]:
			th = make_char(Look.make(r["look"]), r["x"], r["y"], r.get("cid", ""))
		else:
			th = make_prop(r["id"], r["x"], r["y"])
		th.sc = r["sc"]; th.flip = r["flip"]; th.slot = r["sl"]; th.pstate = r["ps"]; th.bites = r["bi"]
		th.off_x = r.get("ox", 0.0)
		for cid in r["ct"]:
			th.contents.append(cid)
		th.set_meta("st", r["st"]); th.set_meta("ln", r["ln"])
	for th in things:
		var st: int = th.get_meta("st")
		var ln: int = th.get_meta("ln")
		if st != Thing.FREE and ln >= 0 and ln < things.size():
			th.state = st
			th.link = things[ln]
			if st == Thing.HELD:
				th.link.held = th
			if st == Thing.SLIDE:
				th.state = Thing.FREE
		if th is Prop:
			(th as Prop).call_deferred("refresh_art")

func reset_place() -> void:
	for th in things.duplicate():
		remove_thing(th)
	Save.data.erase(loc)
	Save.write()
	load_state()
