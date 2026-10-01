## Character body parts for the cutout rig. Parts are drawn white (or near-white) and tinted in the game
## with modulate, so one SVG serves every skin, hair and clothing colour. The game adds soft gradient
## shading (char_shade.gdshader), so parts use flat fills.
## Coordinates are in the classic avatar space: hips at y = -98, neck at y = -196, head above it.
extends RefCounted

const Gfx := preload("res://tools/art/gfx.gd")
const VPath := preload("res://tools/art/vpath.gd")

const W := 0xFFFFFFFF
const OL := 4.5  ## outline width
const N_TOP := 6
const N_HEAD := 3
const N_HAIR := 13
const N_ACC := 11


# ---------------------------------------------------------------- body
## Rounded, slightly pear-shaped torso (hips at y = -98 in torso space).
static func torso_shape(g: Gfx, col: int) -> void:
	g.path(VPath.new().move_to(-34, -197).line_to(34, -197)
		.cubic_to(52, -197, 58, -186, 57, -168)
		.cubic_to(58, -140, 64, -112, 62, -96)
		.cubic_to(61, -86, 52, -82, 40, -82)
		.line_to(-40, -82)
		.cubic_to(-52, -82, -61, -86, -62, -96)
		.cubic_to(-64, -112, -58, -140, -57, -168)
		.cubic_to(-58, -186, -52, -197, -34, -197).close(), col)


## A tapered limb from y0 down to y1 (top half-width w0, bottom half-width w1), rounded at both ends.
static func limb(g: Gfx, w0: float, w1: float, y0: float, y1: float, col: int) -> void:
	g.path(VPath.new().move_to(-w0, y0 + w0 * 0.6).quad_to(-w0, y0, 0, y0).quad_to(w0, y0, w0, y0 + w0 * 0.6)
		.line_to(w1, y1 - w1 * 0.8).quad_to(w1, y1, 0, y1).quad_to(-w1, y1, -w1, y1 - w1 * 0.8).close(), col)


## Tops: 0 tee, 1 stripes (tee + stripes overlay), 2 hoodie, 3 dress, 4 overalls (tee + bib overlay), 5 doctor coat.
static func torso(g: Gfx, top: int) -> void:
	match top:
		3:  # dress
			g.path(VPath.new().move_to(-52, -190).quad_to(0, -208, 52, -190).line_to(60, -150).line_to(84, -66)
				.quad_to(0, -52, -84, -66).line_to(-60, -150).close(), W)
			g.rr(-85, -76, 170, 16, 8, Gfx.lt(W, 0.4))
			g.rr(-57, -156, 114, 12, 6, Gfx.dk(W, 0.14))
			g.ov(0, -192, 24, 14, W)
			return
		5:  # doctor coat
			g.rr(-58, -196, 116, 112, 38, 0xFF6DD3C8)
			g.rr(-58, -196, 116, 124, 36, W)
			g.poly(0xFF6DD3C8, [-14, -196, 14, -196, 0, -140])
			g.ln(0, -140, 0, -76, 4, 0xFFD5DDE8)
			g.poly(W, [-16, -198, 16, -198, 0, -172])
			g.ci(30, -150, 9, 0xFFFF5C73)
			g.ln(30, -158, 30, -142, 4, W)
			g.ln(22, -150, 38, -150, 4, W)
			return
	if top == 2:
		g.ov(0, -190, 52, 26, Gfx.dk(W, 0.16))  # hood behind
	torso_shape(g, W)
	if top == 2:  # pocket + drawstrings
		g.ol(0)
		g.path(VPath.new().move_to(-32, -126).quad_to(0, -134, 32, -126).line_to(28, -100).quad_to(0, -96, -28, -100).close(), Gfx.dk(W, 0.1))
		g.ln(-13, -176, -15, -150, 4, Gfx.dk(W, 0.25))
		g.ln(13, -176, 15, -150, 4, Gfx.dk(W, 0.25))
		g.ol(OL)
	g.ol(0)
	g.ov(0, -194, 24, 13, W)  # neck opening (covered by the neck)
	g.arc(0, -194, 24, 13, 0, 180, 4, Gfx.dk(W, 0.18))


static func stripes(g: Gfx) -> void:
	for i in 4:
		g.rr(-50, -172 + i * 24, 100, 10, 5, W)


static func overalls_bib(g: Gfx) -> void:
	g.rr(-42, -156, 84, 74, 24, W)
	g.rr(-42, -194, 14, 52, 7, W)
	g.rr(28, -194, 14, 52, 7, W)
	g.ol(0)
	g.rr(-15, -130, 30, 24, 9, Gfx.dk(W, 0.12))


static func overalls_buttons(g: Gfx) -> void:
	g.ci(-35, -150, 5.5, 0xFFFFD43B)
	g.ci(35, -150, 5.5, 0xFFFFD43B)


static func skirt(g: Gfx) -> void:
	g.path(VPath.new().move_to(-58, -110).line_to(58, -110).line_to(80, -54).quad_to(0, -44, -80, -54).close(), W)
	g.rr(-81, -62, 162, 12, 6, Gfx.lt(W, 0.35))


## Arms: origin = shoulder.
static func sleeve(g: Gfx) -> void:
	limb(g, 16, 13, -8, 76, W)
	g.ol(0)
	g.rr(-13, 62, 26, 7, 3.5, Gfx.dk(W, 0.08))  # cuff


static func arm_bare(g: Gfx) -> void:
	limb(g, 14, 12, -8, 76, W)


## side: 1 right hand, -1 left hand (thumb side).
static func mitten(g: Gfx, side: int) -> void:
	g.ov(-side * 12, 72, 8, 10, W)
	g.ov(0, 82, 17, 19, W)


## Legs: origin = hip joint.
static func leg_pants(g: Gfx) -> void:
	limb(g, 20, 16, 0, 78, W)
	g.ol(0)
	g.rr(-16, 64, 32, 7, 3.5, Gfx.dk(W, 0.08))  # hem


static func leg_bare(g: Gfx) -> void:
	limb(g, 17, 14, 0, 78, W)


static func shorts(g: Gfx) -> void:
	limb(g, 21, 19, 0, 34, W)


## Rounded sneaker; origin = shoe centre.
static func shoe(g: Gfx) -> void:
	g.ov(0, 6, 31, 12, Gfx.dk(W, 0.28))  # sole
	g.path(VPath.new().move_to(-27, 6).cubic_to(-29, -16, 27, -18, 29, 4).quad_to(29, 8, 24, 8).line_to(-23, 8)
		.quad_to(-28, 8, -27, 6).close(), W)
	g.ol(0)
	g.ov(-8, -6, 9, 4, 0x55FFFFFF)  # shine
	g.ln(-2, -8, 8, -9, 2.5, Gfx.dk(W, 0.2))  # lace


# ---------------------------------------------------------------- head (neck at y = -196)
static func neck(g: Gfx) -> void:
	g.rr(-16, -210, 32, 30, 12, Gfx.dk(W, 0.13))


## Head shapes: 0 oval, 1 rounded square, 2 wide.
static func head(g: Gfx, shape: int) -> void:
	var skin_d := Gfx.dk(W, 0.13)
	var ear_x := 97.0 if shape == 2 else 88.0
	g.ci(-ear_x, -256, 17, W)
	g.ci(ear_x, -256, 17, W)
	g.ci(-ear_x, -256, 8, skin_d)
	g.ci(ear_x, -256, 8, skin_d)
	if shape == 1:
		g.rr(-90, -346, 180, 164, 70, W)
	elif shape == 2:
		g.ov(0, -258, 99, 78, W)
	else:
		g.ov(0, -264, 90, 84, W)


## Hair behind the head. Styles: 0 bald, 1 short, 2 bob, 3 long, 4 ponytail, 5 afro, 6 spiky, 7 buns,
## 8 pigtails, 9 mohawk, 10 top bun, 11 braids, 12 curly.
static func hair_back(g: Gfx, style: int) -> void:
	var hair := W
	var hd := Gfx.dk(hair, 0.1)
	match style:
		1, 6, 12:
			g.ov(0, -268, 94, 86, hair)
		2:
			g.rr(-99, -318, 198, 138, 52, hair)
		3:
			g.rr(-100, -318, 200, 220, 56, hair)
		4:
			g.ov(0, -268, 94, 86, hair)
			g.save(); g.translate(100, -236); g.rotate(-25)
			g.ov(6, 36, 26, 58, hair)
			g.restore()
			g.ci(96, -262, 14, 0xFFFF5C73)
		5:
			g.ci(-72, -300, 52, hair); g.ci(72, -300, 52, hair)
			g.ci(0, -332, 62, hair)
			g.ci(-100, -250, 44, hair); g.ci(100, -250, 44, hair)
			g.ci(-98, -205, 30, hair); g.ci(98, -205, 30, hair)
		7:
			g.ov(0, -268, 94, 86, hair)
			g.ci(-64, -352, 36, hair); g.ci(64, -352, 36, hair)
			g.ci(-64, -352, 15, hd); g.ci(64, -352, 15, hd)
		8:
			g.ov(0, -268, 94, 86, hair)
			for s in [-1, 1]:
				g.save(); g.translate(s * 104, -228); g.rotate(s * 14)
				g.ov(0, 44, 28, 56, hair)
				g.restore()
				g.rr(s * 104 - 16, -250, 32, 16, 8, 0xFFFF5C73)
		10:
			g.ov(0, -268, 94, 86, hair)
			g.ci(0, -372, 42, hair)
			g.rr(-30, -342, 60, 14, 7, 0xFFFF8FD0)
		11:
			g.ov(0, -268, 94, 86, hair)
			for s in [-1, 1]:
				for i in 5:
					g.ov(s * (94 - i * 2), -224 + i * 26, 18, 16, hair if i % 2 == 0 else Gfx.dk(hair, 0.08))
				g.rr(s * 86 - 14, -104, 28, 12, 6, 0xFF4FB3FF)


## Hair in front of the face (fringe, spikes, curls).
static func hair_front(g: Gfx, style: int) -> void:
	var hair := W
	match style:
		0:
			return
		5:  # afro hairline
			g.ov(0, -330, 80, 34, hair)
			g.ci(-48, -318, 30, hair); g.ci(48, -318, 30, hair); g.ci(0, -312, 30, hair)
		6:  # spiky
			fringe(g, hair, 1)
			for i in range(-2, 3):
				var x := i * 34
				g.poly(hair, [x - 22, -330 + absi(i) * 12, x, -392 + absi(i) * 14, x + 22, -330 + absi(i) * 12])
		9:  # mohawk
			g.rr(-18, -404, 36, 100, 18, hair)
			for i in 3:
				g.poly(hair, [-18, -380 + i * 26, -34, -392 + i * 26, -18, -366 + i * 26])
		12:  # curly top
			fringe(g, hair, 1)
			for i in 7:
				var a := deg_to_rad(-160 + i * 23.3)
				g.ci(cos(a) * 82, -282 + sin(a) * 74, 30, hair)
		1, 10:
			fringe(g, hair, 0)
		4:
			fringe(g, hair, 2)
		3, 11:
			fringe(g, hair, 3)
		_:
			fringe(g, hair, 1)


## Hair cap over the forehead. Variants: 0 short, 1 scalloped, 2 side swept, 3 centre parted.
static func fringe(g: Gfx, hair: int, v: int) -> void:
	var p: VPath = VPath.new().move_to(-94, -262).cubic_to(-108, -382, 108, -382, 94, -262)
	match v:
		0:
			p.quad_to(70, -296, 44, -308).quad_to(0, -296, -44, -310).quad_to(-72, -296, -92, -262)
		1:
			p.quad_to(76, -280, 62, -302).quad_to(46, -274, 26, -304).quad_to(8, -272, -14, -304)
			p.quad_to(-32, -274, -52, -302).quad_to(-70, -280, -92, -262)
		2:
			p.quad_to(86, -284, 70, -300).quad_to(20, -270, -40, -302).quad_to(-70, -296, -92, -262)
		_:
			p.quad_to(88, -272, 56, -304).quad_to(30, -296, 0, -320).quad_to(-30, -296, -56, -304).quad_to(-88, -272, -92, -262)
	g.path(p.close(), hair)


## Accessories: 1 round glasses, 2 sunglasses, 3 cap, 4 beanie, 5 crown, 6 bow, 7 headphones,
## 8 party hat, 9 cat ears, 10 flower crown.
static func accessory(g: Gfx, kind: int) -> void:
	var ac := W
	match kind:
		1:
			g.cis(-35, -262, 22, 0xFF3A3346, 5)
			g.cis(35, -262, 22, 0xFF3A3346, 5)
			g.ln(-13, -264, 13, -264, 5, 0xFF3A3346)
		2:
			g.rr(-62, -278, 56, 32, 14, 0xFF2B2735)
			g.rr(6, -278, 56, 32, 14, 0xFF2B2735)
			g.ln(-8, -268, 8, -268, 5, 0xFF2B2735)
			g.ln(-50, -270, -36, -274, 4, Gfx.al(W, 140))
		3:
			g.path(VPath.new().move_to(-90, -290).cubic_to(-96, -380, 96, -380, 90, -290).close(), ac)
			g.rr(-96, -302, 192, 18, 9, Gfx.dk(ac, 0.18))
			g.rr(20, -300, 100, 14, 7, Gfx.dk(ac, 0.18))
			g.ci(0, -362, 8, Gfx.lt(ac, 0.3))
		4:
			g.path(VPath.new().move_to(-92, -292).cubic_to(-100, -392, 100, -392, 92, -292).close(), ac)
			g.rr(-98, -306, 196, 30, 14, Gfx.dk(ac, 0.15))
			for i in range(-4, 5):
				g.ln(i * 20, -302, i * 20, -282, 3, Gfx.dk(ac, 0.3))
			g.ci(0, -392, 17, Gfx.lt(ac, 0.5))
		5:
			g.poly(0xFFFFC93C, [-58, -330, -58, -388, -29, -358, 0, -400, 29, -358, 58, -388, 58, -330])
			g.rr(-60, -340, 120, 16, 6, 0xFFE8A91F)
			g.ci(0, -378, 7, 0xFFFF5C73)
			g.ci(-38, -368, 5, 0xFF4FB3FF)
			g.ci(38, -368, 5, 0xFF7ED957)
		6:
			g.save(); g.translate(-54, -340); g.rotate(-18)
			g.poly(ac, [0, 0, -40, -24, -40, 24])
			g.poly(ac, [0, 0, 40, -24, 40, 24])
			g.ci(0, 0, 12, Gfx.dk(ac, 0.2))
			g.restore()
		7:
			g.arc(0, -268, 100, 96, 190, 160, 11, 0xFF3A3346)
			g.rr(-112, -284, 30, 56, 14, ac)
			g.rr(82, -284, 30, 56, 14, ac)
		8:
			g.save(); g.rotate(12, 0, -340)
			g.poly(ac, [-42, -336, 42, -336, 0, -440])
			for i in 3:
				g.ci(-14 + i * 14, -360 - i * 24, 6, W)
			g.ci(0, -444, 14, 0xFFFFD43B)
			g.restore()
		9:
			for s in [-1, 1]:
				g.poly(ac, [s * 30, -332, s * 84, -330, s * 70, -398])
				g.poly(0xFFFF9EC4, [s * 44, -338, s * 74, -338, s * 66, -376])
			g.arc(0, -268, 94, 90, 205, 130, 9, ac)
		10:
			for i in 7:
				var a := deg_to_rad(-165 + i * 25)
				var pc: int = [0xFFFF8FC0, 0xFFFFD43B, 0xFFB67CFF][i % 3]
				g.ci(cos(a) * 88, -276 + sin(a) * 76, 15, pc)
				g.ci(cos(a) * 88, -276 + sin(a) * 76, 6, W)
