class_name TravelPicker
extends CanvasLayer
## "Where to?" — pick a place for a character who stepped through a door or bus stop.

signal chosen(place: String)
signal cancelled

var here := ""
var cards: Array[Control] = []   # one per destination, in Rules.PLACES order (minus here)


func setup(current: String) -> TravelPicker:
	here = current
	return self


func _ready() -> void:
	layer = 30
	var vs := get_viewport().get_visible_rect().size
	var dim := ColorRect.new()
	dim.color = Color(0.12, 0.08, 0.2, 0.55)
	dim.size = vs
	dim.gui_input.connect(func(e: InputEvent):
		if e is InputEventMouseButton and e.pressed: cancelled.emit())
	add_child(dim)
	var title := UI.label("Where to?", 72, Color.WHITE, 18)
	title.reset_size()
	title.position = Vector2((vs.x - title.size.x) / 2, 70)
	add_child(title)
	var ids := []
	for p in Rules.PLACES:
		if p[0] != here:
			ids.append(p)
	var cw := 250.0
	var gap := 26.0
	var per_row := 4
	var top := 230.0
	for i in ids.size():
		var row := int(i / float(per_row))
		var in_row := mini(per_row, ids.size() - row * per_row)
		var x0 := (vs.x - (in_row * cw + (in_row - 1) * gap)) / 2
		var card := _card(ids[i][0], ids[i][1], Color.hex((int(ids[i][2]) << 8) | 0xFF))
		card.position = Vector2(x0 + (i % per_row) * (cw + gap), top + row * (cw + 40))
		add_child(card)
		cards.append(card)
	var close := RoundButton.new().setup("icon_9", Color("ff6f8f"), 48)
	close.position = Vector2(vs.x - 150, 50)
	close.pressed.connect(func(): cancelled.emit())
	add_child(close)


func _card(id: String, label: String, col: Color) -> Control:
	var c := Panel.new()
	c.size = Vector2(250, 270)
	var sb := StyleBoxFlat.new()
	sb.bg_color = Color.WHITE
	sb.set_corner_radius_all(36)
	sb.border_color = col
	sb.set_border_width_all(6)
	sb.shadow_color = Color(0, 0, 0, 0.18)
	sb.shadow_size = 14
	sb.shadow_offset = Vector2(0, 8)
	c.add_theme_stylebox_override("panel", sb)
	var pic := Sketch.of(func(p: Paint): Vignettes.draw(p, id), Vector2(125, 196))
	pic.scale = Vector2(0.78, 0.78)
	c.add_child(pic)
	var l := UI.label(label, 32, col.darkened(0.25))
	l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	l.size = Vector2(250, 40)
	l.position = Vector2(0, 214)
	c.add_child(l)
	c.gui_input.connect(func(e: InputEvent):
		if e is InputEventMouseButton and e.button_index == MOUSE_BUTTON_LEFT and not e.pressed:
			Sfx.play("tick")
			chosen.emit(id))
	return c
