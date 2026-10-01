extends Node
## Headless/xvfb test driver: `-- --test=<mode>` runs a scripted session, prints results, saves screenshots.

var main: Node
var out := "/tmp"

func run(m: Node, mode: String) -> void:
	main = m
	for a in OS.get_cmdline_user_args():
		if a.begins_with("--out="):
			out = a.substr(6)
	await frames(10)
	match mode:
		"shots": await shots()
		"smoke": await smoke()
		"editor": await editor_test()
	get_tree().quit()

func frames(n: int) -> void:
	for i in n:
		await get_tree().process_frame

func shot(name: String) -> void:
	await RenderingServer.frame_post_draw
	get_viewport().get_texture().get_image().save_png("%s/%s.png" % [out, name])
	print("shot ", name)

func loc() -> Location:
	return main.screen as Location

func shots() -> void:
	await frames(20)
	await shot("map")
	for p in ["home", "park", "beach", "school"]:
		Save.data.erase(p)
		main.go_place(p)
		await frames(60)
		await shot("place_" + p)
		main.go_map()
		await frames(30)
	main.go_place("home")
	await frames(40)
	loc().ui._toggle_tray(2)
	await frames(30)
	await shot("tray_items")
	loc().ui._toggle_tray(1)
	await frames(30)
	await shot("tray_chars")

func tap(p: Vector2) -> void:
	var l := loc()
	l.on_down(p); await frames(1); l.on_up(p); await frames(1)

func drag(a: Vector2, b: Vector2) -> void:
	var l := loc()
	l.on_down(a)
	await frames(1)
	for i in range(1, 13):
		l.on_move(a.lerp(b, i / 12.0))
		await frames(1)
	await get_tree().create_timer(0.12).timeout
	l.on_up(b)
	await frames(2)

func bodyp(c: Thing) -> Vector2:
	return Vector2(c.x, c.y + c.dyo - c.bh * c.sc * 0.45)

func check(name: String, ok: bool) -> void:
	print(("PASS " if ok else "FAIL ") + name)

func smoke() -> void:
	main.go_place("home")
	await frames(40)
	var l := loc()
	for th in l.things.duplicate():
		l.remove_thing(th)
	await frames(2)
	var cx := l.WB / 2
	var sofa := l.make_prop("sofa", cx - 500, 965)
	var bed := l.make_prop("bed", cx - 500, 530)
	var tub := l.make_prop("tub", cx + 400, 545)
	var table := l.make_prop("table", cx + 500, 965)
	var apple := l.make_prop("apple", cx + 100, 965)
	var cake := l.make_prop("cake", cx - 760, 965)
	var kid := l.make_char(Look.preset(1), cx - 200, 970)
	var mom := l.make_char(Look.preset(6), cx + 250, 970)
	var fridge := l.make_prop("fridge", cx + 800, 965)
	for th in l.things:
		th.pop = 1
		if th.is_char: (th as Character).idle_t = 9999
	await frames(5)
	await drag(bodyp(kid), Vector2(sofa.x - 86, sofa.y - 150))
	await frames(10)
	check("sit on sofa", kid.state == Thing.SIT and kid.link == sofa)
	await shot("t_sit")
	await drag(bodyp(kid), Vector2(bed.x + 20, bed.y - 180))
	await frames(10)
	check("sleep in bed", kid.state == Thing.LIE and kid.link == bed)
	await shot("t_sleep")
	await drag(Vector2(kid.x - 150, kid.y - 20), Vector2(cx - 200, 820))
	await frames(40)
	check("out of bed and fell to floor", kid.state == Thing.FREE and kid.y > 900)
	await drag(Vector2(apple.x, apple.y - 40), bodyp(kid))
	await frames(10)
	check("hold apple (auto bite)", kid.held == apple and apple.bites == 1)
	for i in 2:
		await tap(bodyp(kid))
		await frames(30)
	check("ate apple", kid.held == null and not is_instance_valid(apple))
	await drag(Vector2(cake.x, cake.y - 40), Vector2(table.x + 10, table.y - 400))
	await frames(60)
	check("cake on table", cake.state == Thing.ON_TOP and cake.link == table)
	await drag(bodyp(mom), bodyp(tub))
	await frames(10)
	check("bathe", mom.state == Thing.BATHE)
	await shot("t_bath")
	var juice := l.make_prop("juice", cx + 650, 1000)
	juice.pop = 1
	await frames(3)
	await drag(Vector2(juice.x, juice.y - 40), bodyp(fridge))
	await frames(5)
	check("juice into fridge", fridge.contents.has("juice"))
	await drag(bodyp(mom), bodyp(kid) + Vector2(30, 0))
	await frames(5)
	check("hug", mom.hug_t > 0 and kid.hug_t > 0)
	await shot("t_hug")
	await frames(80)
	await drag(bodyp(kid), Vector2(mom.x, mom.y + (mom.look.neck_y() - 90) * mom.sc))
	await frames(10)
	check("shoulder ride", kid.state == Thing.SIT and kid.link == mom)
	await shot("t_shoulder")
	await drag(bodyp(kid), Vector2(cx - 150, 400))
	await frames(40)
	await tap(bodyp(kid))
	await tap(Vector2(sofa.x, sofa.y - 60))
	for i in 600:
		if kid.state == Thing.SIT:
			break
		await frames(1)
	check("walk downstairs via ladder and sit", kid.state == Thing.SIT and kid.link == sofa)
	l.save_state()
	main.go_map()
	await frames(40)
	main.go_place("home")
	await frames(40)
	var sits := 0
	for th in loc().things:
		if th.is_char and th.state == Thing.SIT:
			sits += 1
	check("saved and reloaded (seated kid)", sits >= 1)
	var empty := []
	var others := ["school", "hospital", "market", "cafe", "park", "beach", "fair"]
	for p in others:
		Save.data.erase(p)
	for p in others:
		main.go_map()
		await frames(30)
		main.go_place(p)
		await frames(200)
		var n := 0
		for th in loc().things:
			if th.is_char:
				n += 1
		if n == 0:
			empty.append(p)
	check("all places ran", true)
	check("every place starts with someone " + str(empty), empty.is_empty())
	main.go_map()
	await frames(30)
	var m := main.screen as WorldMap
	m.spin_by(1)
	await frames(60)
	check("world spins", m.focus == 1)
	# characters are unique: tapping a preset card twice gives one character, and it can move between places
	main.go_place("park")
	await frames(40)
	var pl := loc()
	for th in pl.things.duplicate():
		if th.is_char: pl.remove_thing(th)
	pl.ui._toggle_tray(1)
	await frames(20)
	var idx := -1
	for i in pl.ui.tray.cards.size():
		if pl.ui.tray.cards[i][0] == "char" and pl.ui.tray.cards[i][1] == 4:
			idx = i
	pl.ui.tray._tap_card(idx)
	await frames(30)
	pl.ui.tray._tap_card(idx)
	await frames(30)
	var nanas := 0
	for th in pl.things:
		if th.is_char and (th as Character).cid == "p4": nanas += 1
	check("same character can't be spawned twice", nanas == 1)
	pl.save_state()
	main.go_place("beach")
	await frames(40)
	var bl := loc()
	bl.ui._toggle_tray(1)
	await frames(20)
	bl.ui.tray._tap_card(idx)
	await frames(30)
	var here := bl.char_by_cid("p4") != null
	var left := true
	for r in Save.data.get("park", []):
		if r.get("cid", "") == "p4": left = false
	check("character moves between places (no twin left behind)", here and left)
	await shot("t_halfsize")

func editor_test() -> void:
	Save.data.erase("lib")
	main.open_editor(Look.preset(0), "New character", main._add_to_lib)
	await frames(40)
	var ed: DressUp = main.editor
	check("editor opened", ed != null)
	for i in 5:
		ed._set_tab(i)
		await frames(8)
		await shot("editor_tab%d" % i)
	ed._set_tab(1)
	ed._apply(1, 12)
	ed._apply(2, 6)
	ed._set_tab(3)
	ed._apply(5, 1)
	ed._apply(6, 3)
	ed._apply(12, 2)
	await frames(10)
	check("options applied", ed.work.hair_style == 12 and ed.work.hair_color == 6 and ed.work.top == 1 and ed.work.bstyle == 2)
	# tap an option card through real input
	ed._set_tab(0)
	await frames(5)
	var card: Node = null
	for c in ed.content.get_children():
		if c.has_meta("opt") and c.get_meta("opt") == [10, 2]:
			card = c
	var p: Vector2 = ed.panel.position + ed.content.position + card.position + Vector2(60, 60)
	var ev := InputEventMouseButton.new()
	ev.button_index = MOUSE_BUTTON_LEFT
	ev.pressed = true
	ev.position = p - ed.panel.position
	ed._panel_input(ev)
	var ev2 := ev.duplicate()
	ev2.pressed = false
	ed._panel_input(ev2)
	await frames(5)
	check("tap card sets body type (teen)", ed.work.body == 2)
	await shot("editor_teen")
	ed.finished.emit(ed.work, true)
	await frames(40)
	check("saved to My Characters", Save.data.get("lib", []).size() == 1 and main.editor == null)
	# edit a character inside a place (fresh park: its starting character is Mia)
	Save.data.erase("park")
	main.go_place("park")
	await frames(40)
	var l := loc()
	var kid: Character = null
	for th in l.things:
		if th.is_char:
			kid = th
	l.edit_char.emit(kid)
	await frames(40)
	check("editor opens for scene character", main.editor != null)
	main.editor._apply(8, 5)
	await frames(5)
	main.editor.finished.emit(main.editor.work, true)
	await frames(40)
	check("scene character got the crown", kid.look.acc == 5)
	l.ui._toggle_tray(1)
	await frames(30)
	await shot("tray_with_mine")
	var n_before := l.things.size()
	l.new_char.emit()
	await frames(40)
	main.editor.finished.emit(main.editor.work, true)
	await frames(60)
	check("create from tray spawns a character", l.things.size() == n_before + 1 and Save.data["lib"].size() == 2)
	main.go_map()
	await frames(40)
	(main.screen as WorldMap).dress_up.emit()
	await frames(40)
	check("dress-up from the world map", main.editor != null)
	main.editor.finished.emit(null, false)
	await frames(40)
	check("cancel closes editor", main.editor == null and Save.data["lib"].size() == 2)
