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
	var cake := l.make_prop("cake", cx - 100, 965)
	var kid := l.make_char(Look.preset(1), cx - 200, 970)
	var mom := l.make_char(Look.preset(6), cx + 250, 970)
	var fridge := l.make_prop("fridge", cx + 800, 965)
	for th in l.things:
		th.pop = 1
		if th.is_char: (th as Character).idle_t = 9999
	await frames(5)
	await drag(Vector2(kid.x, kid.y - 150), Vector2(sofa.x - 86, sofa.y - 150))
	await frames(10)
	check("sit on sofa", kid.state == Thing.SIT and kid.link == sofa)
	await shot("t_sit")
	await drag(Vector2(kid.x, kid.y - 150), Vector2(bed.x + 20, bed.y - 180))
	await frames(10)
	check("sleep in bed", kid.state == Thing.LIE and kid.link == bed)
	await shot("t_sleep")
	await drag(Vector2(kid.x - 150, kid.y - 20), Vector2(cx - 200, 820))
	await frames(40)
	check("out of bed and fell to floor", kid.state == Thing.FREE and kid.y > 900)
	await drag(Vector2(apple.x, apple.y - 40), Vector2(kid.x, kid.y - 170))
	await frames(10)
	check("hold apple (auto bite)", kid.held == apple and apple.bites == 1)
	for i in 2:
		await tap(Vector2(kid.x, kid.y - 300))
		await frames(30)
	check("ate apple", kid.held == null and not is_instance_valid(apple))
	await drag(Vector2(cake.x, cake.y - 40), Vector2(table.x + 10, table.y - 400))
	await frames(60)
	check("cake on table", cake.state == Thing.ON_TOP and cake.link == table)
	await drag(Vector2(mom.x, mom.y - 150), Vector2(tub.x, tub.y - 150))
	await frames(10)
	check("bathe", mom.state == Thing.BATHE)
	await shot("t_bath")
	var juice := l.make_prop("juice", cx + 650, 1000)
	juice.pop = 1
	await frames(3)
	await drag(Vector2(juice.x, juice.y - 40), Vector2(fridge.x, fridge.y - 200))
	await frames(5)
	check("juice into fridge", fridge.contents.has("juice"))
	await drag(Vector2(mom.x, mom.y - 150), Vector2(kid.x + 30, kid.y - 170))
	await frames(5)
	check("hug", mom.hug_t > 0 and kid.hug_t > 0)
	await shot("t_hug")
	await frames(80)
	await drag(Vector2(kid.x, kid.y - 150), Vector2(mom.x, mom.y + (mom.look.neck_y() - 90) * mom.sc))
	await frames(10)
	check("shoulder ride", kid.state == Thing.SIT and kid.link == mom)
	await shot("t_shoulder")
	await drag(Vector2(kid.x, kid.y - 150), Vector2(cx - 150, 400))
	await frames(40)
	await tap(Vector2(kid.x, kid.y - 200))
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
	for p in ["school", "hospital", "market", "cafe", "park", "beach", "fair"]:
		main.go_map()
		await frames(30)
		main.go_place(p)
		await frames(200)
	check("all places ran", true)
	main.go_map()
	await frames(30)
	var m := main.screen as WorldMap
	m.spin_by(1)
	await frames(60)
	check("world spins", m.focus == 1)
