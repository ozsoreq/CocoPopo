## Vector artwork for the props. Local origin = bottom centre.
extends RefCounted

const Gfx := preload("res://tools/art/gfx.gd")
const VPath := preload("res://tools/art/vpath.gd")

const WOOD := 0xFFD39A62
const WOOD_D := 0xFFB07744
const WHITE := 0xFFFFFFFF
const INK := 0xFF2B2230
const T := 1.2  ## moment in time the animated details (steam, swing, clock hands) are drawn at


## Blanket drawn over a character sleeping in a bed.
static func blanket(g: Gfx, id: String) -> void:
	if id == "hbed":
		g.rr(-60, -150, 228, 66, 22, 0xFF6DD3C8)
		g.rr(-60, -150, 228, 18, 9, 0xFF8FE6DC)
	else:
		g.rr(-60, -160, 238, 76, 26, 0xFF8E9BFF)
		g.rr(-60, -160, 238, 22, 11, 0xFFA9B4FF)
		for i in 5:
			g.ci(-24 + i * 44, -112, 6, 0xFFC8CEFF)


## Front part drawn over a character sitting / bathing inside the prop.
static func front(g: Gfx, id: String) -> void:
	if id == "tub":
		g.rr(-176, -120, 352, 104, 50, 0xFFC3E8FA)
		for i in 7:
			g.ci(-130 + i * 42, -122 - (i % 3) * 7 + sin(i) * 3, 18 - (i % 2) * 5, WHITE)
	elif id == "car":
		g.rr(-100, -62, 200, 40, 16, 0xFFFF6F61)
		g.ci(-56, -22, 22, INK); g.ci(56, -22, 22, INK)
		g.ci(-56, -22, 10, 0xFFDDE4EE); g.ci(56, -22, 10, 0xFFDDE4EE)
		g.ci(94, -44, 6, 0xFFFFE066)


## st = state (lamp on, tv channel, fridge open, ...).
static func draw(g: Gfx, id: String, st := 0) -> void:
	match id:
		"bed":
			g.rr(-176, -70, 24, 70, 8, WOOD_D); g.rr(150, -70, 24, 70, 8, WOOD_D)
			g.rr(-186, -236, 34, 200, 14, WOOD)
			g.rr(-170, -112, 346, 52, 14, WOOD)
			g.rr(-156, -148, 332, 56, 22, 0xFFF4F1FF)
			g.rr(-40, -156, 218, 70, 26, 0xFF8E9BFF)
			g.rr(-40, -156, 218, 20, 10, 0xFFA9B4FF)
			for i in 5:
				g.ci(-10 + i * 42, -112, 6, 0xFFC8CEFF)
			g.rr(-148, -176, 100, 48, 24, WHITE)
			g.rr(-148, -150, 100, 22, 11, 0xFFE6E8F5)
		"sofa":
			g.rr(-170, -190, 340, 126, 44, 0xFFFF8FA3)
			g.rr(-162, -112, 324, 70, 26, 0xFFFFA9B8)
			g.rr(-176, -140, 54, 114, 26, 0xFFE87389)
			g.rr(122, -140, 54, 114, 26, 0xFFE87389)
			g.rr(-110, -150, 106, 56, 22, 0xFFFFC1CB)
			g.rr(4, -150, 106, 56, 22, 0xFFFFC1CB)
			g.rr(-140, -26, 20, 26, 6, WOOD_D); g.rr(120, -26, 20, 26, 6, WOOD_D)
		"armchair":
			g.rr(-96, -190, 192, 130, 46, 0xFF7ED3C0)
			g.rr(-86, -108, 172, 66, 24, 0xFF9BE2D2)
			g.rr(-100, -136, 44, 108, 22, 0xFF55B8A4)
			g.rr(56, -136, 44, 108, 22, 0xFF55B8A4)
			g.rr(-72, -28, 16, 28, 5, WOOD_D); g.rr(56, -28, 16, 28, 5, WOOD_D)
		"table":
			g.rr(-140, -130, 22, 130, 6, WOOD_D); g.rr(118, -130, 22, 130, 6, WOOD_D)
			g.rr(-155, -156, 310, 34, 14, WOOD)
			g.rr(-155, -138, 310, 12, 6, WOOD_D)
			# cloth + vase
			g.rr(-100, -168, 200, 14, 7, 0xFFFFE9A8)
			g.rr(-14, -204, 28, 38, 10, 0xFF6FC3FF)
			g.ci(0, -218, 12, 0xFFFF6F8F); g.ci(-12, -210, 9, 0xFFFFC93C); g.ci(12, -210, 9, 0xFFFF9A3D)
		"chair":
			g.rr(-48, -190, 20, 110, 9, WOOD)
			g.rr(-54, -96, 108, 24, 10, WOOD)
			g.rr(-48, -76, 16, 76, 5, WOOD_D); g.rr(32, -76, 16, 76, 5, WOOD_D)
			g.rr(-44, -170, 14, 14, 5, WOOD_D)
		"shelf":
			g.rr(-112, -350, 224, 350, 14, WOOD)
			g.rr(-98, -336, 196, 322, 8, 0xFFB98350)
			var bc := [0xFFFF6F8F, 0xFF6FC3FF, 0xFFFFC93C, 0xFF7ED957, 0xFFB67CFF, 0xFFFF9A3D]
			for r in 4:
				var y := -336 + (r + 1) * 80
				g.rr(-102, y - 6, 204, 12, 4, WOOD)
				var x := -92.0
				for i in 6:
					var bw := 14 + ((i * 7 + r * 5) % 4) * 4
					var bh := 38 + ((i * 5 + r * 3) % 4) * 8
					if r == 2 and i >= 3:
						g.ci(x + 20, y - 24, 18, WHITE)
						break
					g.rr(x, y - 6 - bh, bw, bh, 3, bc[(i + r) % 6])
					x += bw + 3
		"lamp":
			if st == 1:  # glow
				var o := g.olw
				g.ol(0)
				g.ci(0, -230, 150, Gfx.al(0xFFFFF2A0, 70))
				g.ci(0, -230, 100, Gfx.al(0xFFFFF2A0, 90))
				g.poly(Gfx.al(0xFFFFF6C0, 120), [-40, -250, 40, -250, 110, -20, -110, -20])
				g.ol(o)
			g.ov(0, -6, 38, 10, 0xFF5B5470)
			g.rr(-5, -250, 10, 244, 4, 0xFF6B6480)
			g.poly(0xFFFFF09A if st == 1 else 0xFFFFD86B, [-50, -250, 50, -250, 34, -310, -34, -310])
			g.rr(-50, -258, 100, 12, 6, 0xFFFFB93D)
			g.ov(0, -240, 40, 8, Gfx.al(0xFFFFF2B0, 110))
		"tv":
			g.rr(-120, -76, 240, 76, 12, WOOD)
			g.rr(-100, -62, 90, 22, 6, WOOD_D); g.rr(10, -62, 90, 22, 6, WOOD_D)
			g.rr(-112, -226, 224, 150, 16, 0xFF2B2735)
			_tv_screen(g, st)
			g.rr(-20, -80, 40, 6, 3, 0xFF4B4660)
		"fridge":
			if st == 1:  # open
				g.rr(-88, -350, 176, 350, 20, 0xFFE3F4FF)
				g.rr(-76, -338, 152, 326, 12, 0xFFFFFDF2)
				for r in 3:
					g.rr(-76, -250 + r * 80, 152, 8, 3, 0xFFB7D6EA)
				g.ci(-40, -270, 20, 0xFFFF4D5E); g.ci(0, -268, 18, 0xFF7ED957); g.rr(22, -300, 34, 48, 8, WHITE)
				g.rr(-56, -220, 40, 46, 8, 0xFFFFC93C); g.ov(26, -196, 32, 20, 0xFFFF9EC4)
				g.rr(-50, -132, 100, 40, 12, 0xFF7ED957); g.ci(40, -110, 16, 0xFFFF9A3D)
				g.path(VPath.new().poly([-88, -350, -150, -320, -150, -20, -88, 0]), 0xFFD2EAF8)
				g.rr(-140, -230, 10, 70, 5, 0xFF8AA6BC)
				return
			g.rr(-88, -350, 176, 350, 20, 0xFFE3F4FF)
			g.rr(-88, -350, 176, 350, 20, 0xFFE3F4FF)
			g.ln(-84, -240, 84, -240, 6, 0xFFB7D6EA)
			g.rr(56, -310, 12, 56, 6, 0xFF8AA6BC); g.rr(56, -226, 12, 80, 6, 0xFF8AA6BC)
			g.rr(-60, -300, 40, 40, 6, 0xFFFFD86B); g.rr(-52, -170, 30, 30, 6, 0xFFFF8FA3)
			g.rr(-80, -12, 40, 12, 4, 0xFFB7D6EA); g.rr(40, -12, 40, 12, 4, 0xFFB7D6EA)
		"tub":
			g.rr(-160, -26, 26, 26, 8, 0xFFB0BED0); g.rr(134, -26, 26, 26, 8, 0xFFB0BED0)
			g.rr(-176, -120, 352, 104, 50, 0xFFC3E8FA)
			g.rr(-164, -124, 328, 26, 13, 0xFF8FD3F2)
			g.rr(130, -190, 14, 76, 7, 0xFFB0BED0)
			g.rr(100, -196, 56, 14, 7, 0xFFB0BED0)
			for i in 6:
				g.ci(-120 + i * 38, -128 - (i % 3) * 8, 14 - (i % 2) * 4, Gfx.al(WHITE, 235))
		"toilet":
			g.rr(-42, -170, 84, 70, 16, 0xFFE6EEF7)
			g.rr(-44, -108, 118, 30, 14, 0xFFF4F8FC)
			g.rr(-34, -84, 84, 50, 22, 0xFFE6EEF7)
			g.rr(-20, -34, 60, 34, 10, 0xFFDCE6F0)
			g.rr(-30, -160, 20, 12, 6, 0xFFB0BED0)
		"dresser":
			g.rr(-120, -200, 240, 190, 16, WOOD)
			g.rr(-110, -190, 220, 80, 10, 0xFFE0AC78)
			g.rr(-110, -102, 220, 80, 10, 0xFFE0AC78)
			g.ci(0, -150, 8, 0xFF8A5A30); g.ci(0, -62, 8, 0xFF8A5A30)
			g.rr(-104, -12, 22, 12, 4, WOOD_D); g.rr(82, -12, 22, 12, 4, WOOD_D)
		"stove":
			g.rr(-100, -220, 200, 220, 16, 0xFFDDE4EE)
			g.rr(-100, -220, 200, 30, 12, 0xFFB4BFCD)
			g.rr(-82, -150, 164, 110, 12, 0xFF4B4660)
			g.rr(-70, -138, 140, 84, 8, 0xFF8FA0B8)
			for i in 3:
				g.ci(-50 + i * 50, -180, 9, 0xFF6B6480)
			g.rr(-82, -228, 70, 14, 7, WHITE)
			if st == 1:  # cooking: pot + steam + hot oven
				g.rr(10, -280, 80, 56, 14, 0xFFFF6F61)
				g.rr(2, -286, 96, 14, 7, 0xFFE0564B)
				for i in 3:
					var sy := fmod(T * 60 + i * 30, 90)
					g.ci(30 + i * 20 + sin(T * 3 + i) * 6, -300 - sy, 10 - sy / 12, Gfx.al(WHITE, int(200 - sy * 2)))
				g.rr(-70, -138, 140, 84, 8, 0xFFFF9A5C)
		"desk":
			g.rr(-100, -130, 14, 130, 5, 0xFF8A93A8); g.rr(86, -130, 14, 130, 5, 0xFF8A93A8)
			g.rr(-120, -150, 240, 26, 10, 0xFFFFC66B)
			g.rr(-120, -134, 240, 10, 5, 0xFFE0A24A)
			g.rr(-70, -176, 60, 28, 4, 0xFF6FC3FF); g.rr(-70, -176, 60, 8, 4, 0xFF4B9FE0)
			g.rr(20, -166, 50, 10, 4, 0xFFFF6F8F)
		"plant":
			for i in range(-2, 3):
				g.save(); g.translate(0, -90); g.rotate(i * 26)
				g.ov(0, -50, 20, 54, 0xFF4FBF6B if i % 2 == 0 else 0xFF6FD98A)
				g.restore()
			g.poly(0xFFE8765E, [-42, -92, 42, -92, 32, 0, -32, 0])
			g.rr(-46, -102, 92, 20, 8, 0xFFF0907A)
		"rug":
			g.ov(0, -55, 190, 55, 0xFFFFB3C6)
			g.ov(0, -55, 160, 42, 0xFFFFD6E0)
			g.ov(0, -55, 120, 30, 0xFFFFB3C6)
			g.ov(0, -55, 80, 18, 0xFFFFE9EF)

		# ---------------------------------------------------------------- food
		"cake":
			g.ov(0, -10, 70, 12, WHITE)
			g.rr(-56, -70, 112, 58, 14, 0xFFFFC1D6)
			g.rr(-56, -70, 112, 18, 9, WHITE)
			for i in 4:
				g.rr(-44 + i * 26, -64, 14, 30, 7, WHITE)
			g.rr(-3, -112, 6, 40, 3, 0xFF6FC3FF)
			g.ov(0, -122, 6, 11, 0xFFFFB93D)
			g.ci(-34, -76, 7, 0xFFFF4D6D); g.ci(34, -76, 7, 0xFFFF4D6D)
		"pizza":
			g.ov(0, -52, 62, 50, 0xFFFFC866)
			g.ov(0, -52, 52, 42, 0xFFFF9B54)
			g.ov(0, -52, 46, 36, 0xFFFFD27A)
			g.ci(-20, -62, 9, 0xFFE8454F); g.ci(18, -66, 9, 0xFFE8454F); g.ci(4, -42, 9, 0xFFE8454F); g.ci(-26, -42, 7, 0xFF58B368)
			g.poly(0xFFFFEAB3, [0, -52, 60, -80, 60, -24])
		"burger":
			g.ov(0, -80, 56, 34, 0xFFF3A649)
			g.ci(-20, -92, 3, 0xFFFFF3C4); g.ci(10, -98, 3, 0xFFFFF3C4); g.ci(28, -88, 3, 0xFFFFF3C4)
			g.rr(-60, -62, 120, 16, 8, 0xFF5BBF4C)
			g.rr(-56, -50, 112, 12, 6, 0xFFFF5C5C)
			g.rr(-58, -42, 116, 22, 11, 0xFF7B4B2E)
			g.rr(-56, -24, 112, 22, 11, 0xFFF3A649)
		"icecream":
			g.poly(0xFFE9B36C, [-34, -90, 34, -90, 0, 0])
			for i in 3:
				g.ln(-22 + i * 18, -84, -4 + i * 6, -30, 3, 0xFFC98A44)
			g.ci(0, -104, 36, 0xFFFF9EC4)
			g.ci(0, -142, 30, 0xFF9AE3FF)
			g.ci(6, -168, 7, 0xFFFF4D6D)
		"donut":
			g.ov(0, -42, 56, 40, 0xFFE9B36C)
			g.ov(0, -48, 56, 38, 0xFFFF8FB5)
			g.ov(0, -48, 18, 12, 0xFFFFF0F5)
			for i in 6:
				g.rr(-40 + i * 15, -70 + (i % 3) * 14, 10, 4, 2, WHITE if i % 2 == 0 else 0xFF6FC3FF)
		"apple":
			g.ci(-18, -42, 34, 0xFFFF4D5E); g.ci(18, -42, 34, 0xFFFF4D5E)
			g.rr(-3, -92, 6, 22, 3, 0xFF7B4B2E)
			g.ov(18, -86, 18, 8, 0xFF5BBF4C)
			g.ov(-22, -54, 7, 12, Gfx.al(WHITE, 110))
		"juice":
			g.rr(-28, -108, 56, 106, 10, 0xFFFFA23D)
			g.rr(-28, -108, 56, 30, 10, 0xFFFFE066)
			g.ci(0, -46, 16, 0xFFFF7A1F)
			g.ln(12, -108, 26, -128, 6, 0xFFFF4D6D)
		"cupcake":
			g.ci(0, -62, 34, 0xFFFF9EC4)
			g.ci(0, -90, 24, 0xFFFFC1D8)
			g.ci(0, -112, 8, 0xFFFF4D5E)
			g.poly(0xFFE9B36C, [-38, -52, 38, -52, 28, 0, -28, 0])
			for i in range(-1, 2):
				g.ln(i * 14, -48, i * 10, -6, 3, 0xFFC98A44)

		# ---------------------------------------------------------------- toys
		"ball":
			g.ci(0, -52, 50, WHITE)
			for i in 6:
				g.pie(0, -52, 50, 50, i * 60, 30, 0xFFFF5C73 if i % 2 == 0 else 0xFFFFD43B)
			g.ci(0, -52, 9, WHITE)
		"teddy":
			g.ci(-34, -150, 18, 0xFFB57B4E); g.ci(34, -150, 18, 0xFFB57B4E)
			g.ci(-34, -150, 9, 0xFFE8B48A); g.ci(34, -150, 9, 0xFFE8B48A)
			g.ov(0, -60, 48, 56, 0xFFC68A5A)
			g.ov(0, -52, 30, 34, 0xFFE8B48A)
			g.ci(-42, -20, 20, 0xFFC68A5A); g.ci(42, -20, 20, 0xFFC68A5A)
			g.ci(-52, -80, 17, 0xFFC68A5A); g.ci(52, -80, 17, 0xFFC68A5A)
			g.ov(0, -122, 44, 38, 0xFFC68A5A)
			g.ov(0, -112, 20, 15, 0xFFE8B48A)
			g.ci(-16, -128, 5, INK); g.ci(16, -128, 5, INK)
			g.ov(0, -116, 6, 4, INK)
			g.poly(0xFFFF5C73, [-20, -86, 0, -78, -20, -70])
			g.poly(0xFFFF5C73, [20, -86, 0, -78, 20, -70])
		"balloon":
			g.path(VPath.new().move_to(0, -128).cubic_to(-70, -150, -64, -290, 0, -290)
				.cubic_to(64, -290, 70, -150, 0, -128).close(), 0xFFFF5C73)
			g.ov(-18, -226, 8, 16, Gfx.al(WHITE, 130))
			g.poly(0xFFE8465E, [-7, -126, 7, -126, 0, -138])
			g.path_s(VPath.new().move_to(0, -126).quad_to(-26, -90, 8, -56).quad_to(30, -26, 0, 0), 0xFF8C93A8, 3)
		"car":
			g.rr(-100, -62, 200, 40, 16, 0xFFFF6F61)
			g.rr(-58, -100, 112, 46, 20, 0xFFFF6F61)
			g.rr(-46, -92, 40, 30, 10, 0xFFCDEBFF); g.rr(2, -92, 40, 30, 10, 0xFFCDEBFF)
			g.ci(-56, -22, 22, INK); g.ci(56, -22, 22, INK)
			g.ci(-56, -22, 10, 0xFFDDE4EE); g.ci(56, -22, 10, 0xFFDDE4EE)
			g.ci(94, -44, 6, 0xFFFFE066)
		"blocks":
			g.rr(-66, -62, 62, 62, 8, 0xFFFF5C73)
			g.rr(4, -62, 62, 62, 8, 0xFF4FB3FF)
			g.rr(-32, -124, 62, 62, 8, 0xFFFFD43B)
			g.text("A", -35, -22, 40, WHITE)
			g.text("B", 35, -22, 40, WHITE)
			g.text("C", -1, -82, 40, WHITE)
		"guitar":
			g.rr(-8, -250, 16, 130, 5, 0xFF8A5A30)
			g.rr(-14, -266, 28, 28, 8, 0xFF5B3A1E)
			g.ci(0, -70, 44, 0xFFE8A24A); g.ci(0, -122, 32, 0xFFE8A24A)
			g.ci(0, -90, 15, 0xFF5B3A1E)
			g.rr(-20, -62, 40, 8, 4, 0xFF8A5A30)
			for i in range(-1, 2):
				g.ln(i * 3, -250, i * 3, -64, 1.5, 0xFFFFF3C4)
		"duck":
			g.ov(0, -40, 48, 36, 0xFFFFD43B)
			g.ci(24, -84, 26, 0xFFFFD43B)
			g.ov(52, -76, 16, 8, 0xFFFF9A3D)
			g.ci(30, -92, 4, INK)
			g.ov(-8, -38, 24, 16, 0xFFF3BE1F)
			g.poly(0xFFFFD43B, [-50, -50, -66, -70, -34, -56])
		"rocket":
			g.poly(0xFFFF5C73, [-46, -90, -86, -30, -40, -50])
			g.poly(0xFFFF5C73, [46, -90, 86, -30, 40, -50])
			g.ov(0, -130, 44, 110, 0xFFF0F4FA)
			g.path(VPath.new().move_to(-40, -176).cubic_to(-30, -220, -10, -236, 0, -242)
				.cubic_to(10, -236, 30, -220, 40, -176).quad_to(0, -194, -40, -176).close(), 0xFFFF5C73)
			g.ci(0, -150, 22, 0xFF4B4660); g.ci(0, -150, 16, 0xFF8FD8FF)
			g.rr(-44, -64, 88, 16, 8, 0xFFFF5C73)
			g.poly(0xFFFFB93D, [-26, -26, 26, -26, 0, 4])
			g.poly(0xFFFFE066, [-14, -26, 14, -26, 0, -8])

		# ---------------------------------------------------------------- outdoors
		"tree":
			g.rr(-24, -170, 48, 170, 14, 0xFF9B6B43)
			g.rr(-24, -100, 14, 100, 7, 0xFF86582F)
			g.ci(0, -320, 100, 0xFF4FBF6B)
			g.ci(-84, -250, 68, 0xFF57CB74); g.ci(84, -250, 68, 0xFF45B561)
			g.ci(-30, -380, 62, 0xFF66DA82); g.ci(60, -340, 54, 0xFF57CB74)
			g.ci(-40, -230, 11, 0xFFFF5C73); g.ci(50, -290, 11, 0xFFFF5C73); g.ci(0, -360, 11, 0xFFFF5C73)
		"palm":
			g.path(VPath.new().move_to(-18, 0).quad_to(-6, -220, 34, -330).line_to(60, -322)
				.quad_to(24, -210, 22, 0).close(), 0xFFC29363)
			for i in 6:
				g.ln(-4, -40 - i * 44, 24 + (i % 2) * 2, -40 - i * 44 + 8, 6, 0xFFA87A4E)
			for i in 6:
				g.save(); g.translate(46, -332); g.rotate(-150 + i * 60)
				g.path(VPath.new().move_to(0, 0).quad_to(60, -50, 130, 10).quad_to(70, -14, 0, 14).close(),
					0xFF3FAF5F if i % 2 == 0 else 0xFF57C77A)
				g.restore()
			g.ci(36, -318, 14, 0xFF8A5A30); g.ci(58, -312, 14, 0xFF8A5A30)
		"flower":
			g.rr(-4, -110, 8, 110, 4, 0xFF4FBF6B)
			g.ov(18, -40, 22, 9, 0xFF4FBF6B)
			for i in 6:
				g.save(); g.translate(0, -126); g.rotate(i * 60)
				g.ov(0, -22, 14, 22, 0xFFFF8FC0)
				g.restore()
			g.ci(0, -126, 14, 0xFFFFD43B)
		"bush":
			g.ci(-70, -56, 56, 0xFF4FBF6B); g.ci(70, -56, 56, 0xFF4FBF6B)
			g.ci(0, -76, 64, 0xFF5BD07A); g.ci(0, -50, 54, 0xFF4FBF6B)
			g.ci(-30, -90, 8, 0xFFFF8FA3); g.ci(44, -70, 8, 0xFFFF8FA3); g.ci(-70, -50, 8, 0xFFFFD43B)
		"mushroom":
			g.rr(-22, -62, 44, 62, 16, 0xFFFFF1DC)
			g.pie(0, -62, 52, 52, 180, 180, 0xFFFF5C5C)
			g.ci(-24, -84, 8, WHITE); g.ci(14, -98, 9, WHITE); g.ci(30, -76, 6, WHITE)
		"rock":
			g.ov(0, -40, 78, 40, 0xFFA7B0C0)
			g.ov(22, -56, 42, 28, 0xFFBFC7D5)
			g.ov(-30, -22, 30, 14, 0xFF8E98AB)
		"umbrella":
			g.rr(-5, -280, 10, 280, 4, 0xFFF3F0E8)
			if st == 1:  # closed
				g.poly(0xFFFF5C73, [-18, -180, 18, -180, 6, -300, -6, -300])
				return
			for i in 6:
				g.pie(0, -216, 140, 90, 180 + i * 30, 30, 0xFFFF5C73 if i % 2 == 0 else WHITE)
			g.ci(0, -300, 8, 0xFFF3F0E8)
		"sandcastle":
			g.rr(-80, -70, 160, 70, 8, 0xFFF3D493)
			g.rr(-80, -100, 36, 36, 4, 0xFFF3D493); g.rr(-18, -100, 36, 36, 4, 0xFFF3D493); g.rr(44, -100, 36, 36, 4, 0xFFF3D493)
			g.rr(-48, -128, 96, 66, 8, 0xFFEAC77A)
			for i in 3:
				g.rr(-48 + i * 36, -142, 24, 18, 3, 0xFFEAC77A)
			g.rr(-14, -50, 28, 50, 14, 0xFFC9A257)
			g.ln(0, -142, 0, -170, 3, 0xFF8A5A30)
			g.poly(0xFFFF5C73, [0, -170, 28, -160, 0, -150])
		"swing":
			g.ln(-140, 0, -80, -330, 12, 0xFFC98A44); g.ln(-20, 0, -80, -330, 12, 0xFFC98A44)
			g.ln(140, 0, 80, -330, 12, 0xFFC98A44); g.ln(20, 0, 80, -330, 12, 0xFFC98A44)
			g.ln(-80, -330, 80, -330, 14, 0xFF86582F)
			var sx := sin(T * 2) * 10
			g.ln(-40, -326, -40 + sx, -90, 4, 0xFF6B6480); g.ln(40, -326, 40 + sx, -90, 4, 0xFF6B6480)
			g.rr(-56 + sx, -96, 112, 20, 10, 0xFFFF6F8F)
		"slide":
			g.rr(-150, -300, 14, 300, 6, 0xFF8A93A8); g.rr(-80, -300, 14, 300, 6, 0xFF8A93A8)
			g.rr(-164, -300, 100, 16, 6, 0xFFFFC66B)
			g.path(VPath.new().move_to(-70, -290).quad_to(60, -190, 150, -30).line_to(150, 0).line_to(120, 0)
				.quad_to(30, -150, -70, -250).close(), 0xFF4FB3FF)
			g.rr(-164, -340, 8, 60, 4, 0xFF8A93A8)

		# ---------------------------------------------------------------- stuff
		"backpack":
			g.rr(-48, -140, 96, 140, 30, 0xFF4FB3FF)
			g.rr(-34, -64, 68, 48, 14, 0xFF2E8FDB)
			g.rr(-20, -168, 40, 36, 14, 0xFF2E8FDB)
			g.rr(-50, -120, 12, 60, 6, 0xFF2E8FDB)
		"books":
			g.rr(-66, -26, 132, 26, 6, 0xFFFF6F8F)
			g.rr(-56, -52, 112, 26, 6, 0xFF6FC3FF)
			g.rr(-62, -78, 124, 26, 6, 0xFFFFC93C)
			for i in 3:
				g.rr(-52, -22 - i * 26, 100, 5, 2, Gfx.al(WHITE, 190))
		"globe":
			g.rr(-30, -12, 60, 12, 6, 0xFF8A5A30)
			g.rr(-4, -42, 8, 32, 3, 0xFF8A5A30)
			g.ci(0, -100, 54, 0xFF4FB3FF)
			g.ov(-16, -112, 20, 14, 0xFF7ED957); g.ov(22, -86, 14, 22, 0xFF7ED957); g.ov(-6, -76, 10, 8, 0xFF7ED957)
			g.arc(0, -100, 62, 62, 120, 100, 5, 0xFFFFC93C)
		"bench":
			g.rr(-150, -94, 300, 22, 8, 0xFFC98A44)
			g.rr(-150, -170, 300, 22, 8, 0xFFC98A44)
			g.rr(-150, -138, 300, 22, 8, 0xFFD39A55)
			g.rr(-130, -170, 18, 170, 6, 0xFF6B6480); g.rr(112, -170, 18, 170, 6, 0xFF6B6480)
		"frame":
			g.rr(-62, -150, 124, 150, 10, 0xFFC98A44)
			g.rr(-50, -138, 100, 126, 6, 0xFFBDE9FF)
			g.poly(0xFF7ED957, [-50, -12, -10, -90, 24, -50, 50, -80, 50, -12])
			g.ci(24, -112, 12, 0xFFFFE066)
		"gift":
			g.rr(-56, -90, 112, 90, 8, 0xFFB67CFF)
			if st == 1:  # opened, lid tipped back
				g.rr(-46, -90, 92, 16, 6, 0xFF7A4BC2)
				g.rr(-10, -90, 20, 90, 3, 0xFFFFD43B)
				g.save(); g.rotate(-28, -60, -100)
				g.rr(-62, -132, 124, 30, 8, 0xFF9B5CF0)
				g.restore()
				return
			g.rr(-62, -112, 124, 30, 8, 0xFF9B5CF0)
			g.rr(-10, -112, 20, 112, 3, 0xFFFFD43B)
			g.ci(-18, -126, 16, 0xFFFFD43B); g.ci(18, -126, 16, 0xFFFFD43B); g.ci(0, -118, 9, 0xFFE0B020)
		"trophy":
			g.rr(-36, -22, 72, 22, 6, 0xFF8A5A30)
			g.rr(-8, -60, 16, 40, 4, 0xFFFFC93C)
			g.path(VPath.new().move_to(-44, -150).line_to(44, -150).quad_to(44, -70, 0, -60)
				.quad_to(-44, -70, -44, -150).close(), 0xFFFFC93C)
			g.cis(-44, -112, 14, 0xFFFFC93C, 8); g.cis(44, -112, 14, 0xFFFFC93C, 8)
			g.ci(0, -112, 12, 0xFFFFE680)
		"camera":
			g.rr(-56, -76, 112, 70, 14, 0xFF4B4660)
			g.rr(-56, -76, 112, 24, 12, 0xFFFF6F8F)
			g.ci(0, -38, 24, 0xFF2B2735); g.ci(0, -38, 15, 0xFF6FC3FF); g.ci(-5, -43, 4, WHITE)
			g.rr(-20, -90, 40, 16, 6, 0xFF4B4660)
		"hbed":
			g.ln(-150, -62, -150, 0, 9, 0xFFB0BED0); g.ln(150, -62, 150, 0, 9, 0xFFB0BED0)
			g.ci(-150, -4, 9, 0xFF6B6480); g.ci(150, -4, 9, 0xFF6B6480)
			g.rr(-176, -190, 14, 130, 7, 0xFFB0BED0)
			g.rr(-170, -110, 340, 36, 10, 0xFFB0BED0)
			g.rr(-164, -140, 330, 44, 18, WHITE)
			g.rr(-20, -150, 188, 60, 22, 0xFF6DD3C8)
			g.rr(-20, -150, 188, 16, 8, 0xFF8FE6DC)
			g.rr(-150, -170, 90, 40, 20, 0xFFEAF4FB)
		"ivstand":
			g.ln(0, 0, 0, -330, 7, 0xFFB0BED0)
			g.ln(-40, -6, 40, -6, 8, 0xFFB0BED0)
			g.ci(-40, -4, 7, 0xFF6B6480); g.ci(40, -4, 7, 0xFF6B6480)
			g.rr(-26, -330, 52, 14, 7, 0xFFB0BED0)
			g.rr(-22, -312, 44, 70, 14, Gfx.al(0xFFBDE9FF, 230))
			g.rr(-22, -270, 44, 28, 14, 0xFF7FD0F5)
			g.path_s(VPath.new().move_to(0, -242).quad_to(-22, -190, 6, -150).quad_to(24, -120, 0, -100), 0xFF9AA6BC, 3)
		"crate":
			g.rr(-86, -70, 172, 70, 10, 0xFFD39A62)
			g.ci(-50, -78, 24, 0xFFFF4D5E); g.ci(0, -84, 26, 0xFFFF9A3D); g.ci(50, -78, 24, 0xFF7ED957)
			g.ci(-24, -100, 22, 0xFFFFD43B); g.ci(28, -102, 22, 0xFFFF4D5E)
			g.rr(-90, -50, 180, 50, 8, 0xFFC08450)
			g.ln(-90, -26, 90, -26, 4, 0xFFA8693A)
		"register":
			g.rr(-100, -110, 200, 110, 14, 0xFF7BC4F5)
			g.rr(-104, -122, 208, 22, 10, WHITE)
			g.rr(-30, -172, 78, 56, 10, 0xFF4B4660)
			g.rr(-22, -164, 62, 26, 5, 0xFF8FE6B0)
			g.rr(-40, -130, 100, 18, 6, 0xFF6B6480)
			g.ci(-70, -140, 16, 0xFFFF6F8F)
		"coffee":
			g.rr(-30, -52, 60, 52, 14, WHITE)
			g.ln(30, -40, 44, -34, 8, WHITE); g.ln(44, -34, 30, -18, 8, WHITE)
			g.ov(0, -52, 30, 8, 0xFF8A5A30)
			g.path_s(VPath.new().move_to(-8, -64).quad_to(-18, -76, -6, -86).move_to(8, -64).quad_to(-2, -76, 10, -90), 0xFFB0BED0, 4)
			g.ov(0, -2, 44, 8, 0xFFE0E8F2)
		"surfboard":
			g.path(VPath.new().move_to(0, -300).cubic_to(60, -230, 60, -60, 0, -4)
				.cubic_to(-60, -60, -60, -230, 0, -300).close(), 0xFFFF6F8F)
			g.ln(0, -290, 0, -14, 7, WHITE)
			g.ln(-34, -150, 34, -150, 8, 0xFFFFD43B)
		"popcorn":
			for i in 7:
				g.ci(-34 + (i % 4) * 22, -112 - int(i / 4.0) * 22 - (i % 2) * 8, 20, 0xFFFFF3C4 if i % 2 == 0 else 0xFFFFE9A0)
			g.poly(WHITE, [-48, -100, 48, -100, 38, 0, -38, 0])
			for i in range(-1, 2):
				g.poly(0xFFFF5C73, [i * 28 - 8, -100, i * 28 + 8, -100, i * 22 + 6, 0, i * 22 - 6, 0])
		"cotton":
			g.ln(0, -80, 0, 0, 8, 0xFFF3E1B5)
			g.ci(-30, -134, 30, 0xFFFF9EC4); g.ci(30, -134, 30, 0xFFFF9EC4)
			g.ci(0, -158, 38, 0xFFFFB4D3); g.ci(0, -118, 34, 0xFFFFC6DD); g.ci(-14, -170, 12, 0xFFFFD6E8)
		"coconut":
			g.ci(0, -38, 36, 0xFF8A5A30)
			g.ci(-10, -50, 5, 0xFF5B3A1E); g.ci(6, -54, 5, 0xFF5B3A1E); g.ci(-2, -40, 5, 0xFF5B3A1E)
		"medkit":
			g.rr(-52, -90, 104, 90, 14, WHITE)
			g.rr(-52, -90, 104, 20, 10, 0xFFFF5C73)
			g.rr(-14, -70, 28, 60, 6, 0xFFFF5C73); g.rr(-34, -50, 68, 20, 6, 0xFFFF5C73)
			g.rr(-20, -106, 40, 22, 9, 0xFFCDD6E4)
		"cart":
			g.rr(-80, -100, 150, 72, 12, 0xFFC4CCDA)
			for i in 4:
				g.ln(-60 + i * 40, -96, -52 + i * 38, -34, 3, 0xFF8E98AB)
			g.ln(-80, -100, -96, -150, 9, 0xFF6B6480); g.ln(-110, -150, -80, -150, 9, 0xFF6B6480)
			g.ln(-60, -28, -60, -14, 6, 0xFF6B6480)
			g.ci(-50, -12, 14, INK); g.ci(44, -12, 14, INK)
			g.rr(-30, -138, 40, 38, 8, 0xFFFF8F5C); g.ci(34, -116, 16, 0xFF7ED957)
		"wheelchair":
			g.rr(-56, -150, 18, 80, 8, 0xFF4B4660)
			g.rr(-56, -86, 90, 18, 8, 0xFF6FC3FF)
			g.rr(-54, -150, 70, 66, 14, 0xFF6FC3FF)
			g.cis(-10, -50, 48, 0xFF4B4660, 9)
			g.cis(-10, -50, 34, 0xFFBFC7D5, 3)
			g.ci(70, -16, 16, 0xFF4B4660)
			g.ln(34, -80, 62, -26, 8, 0xFF4B4660)
		"clock":
			g.ci(0, -60, 54, 0xFF8A5A30)
			g.ci(0, -60, 46, WHITE)
			for i in 12:
				var a := i * PI / 6
				g.ln(sin(a) * 38, -60 - cos(a) * 38, sin(a) * 42, -60 - cos(a) * 42, 3, 0xFF6B6480)
			var ma := T * 0.5
			var ha := T * 0.04
			g.ln(0, -60, sin(ma) * 34, -60 - cos(ma) * 34, 4, INK)  # minute hand
			g.ln(0, -60, sin(ha) * 22, -60 - cos(ha) * 22, 6, INK)  # hour hand
			g.ci(0, -60, 5, 0xFFFF5C73)
		_:
			g.rr(-40, -80, 80, 80, 12, 0xFFB0BED0)


static func _tv_screen(g: Gfx, st: int) -> void:
	match st:
		1:  # off
			g.rr(-102, -216, 204, 130, 10, 0xFF3A3448)
			g.ln(-70, -196, -40, -206, 6, Gfx.al(WHITE, 60))
		2:  # cartoon
			g.rr(-102, -216, 204, 130, 10, 0xFFFFB3D1)
			var b := absf(sin(T * 5)) * 10
			g.ci(0, -146 - b, 40, 0xFFFFE066)
			g.ci(-14, -154 - b, 6, INK); g.ci(14, -154 - b, 6, INK)
			g.arc(0, -142 - b, 16, 12, 20, 140, 5, INK)
		3:  # colour bars
			var cols := [WHITE, 0xFFFFE066, 0xFF7FE0F0, 0xFF7ED957, 0xFFFF8FD0, 0xFFFF5C5C, 0xFF6C7BFF]
			var w := 204 / 7.0
			for i in 7:
				g.rect(-102 + i * w, -216, w + 1, 130, cols[i])
		_:  # landscape
			g.rr(-102, -216, 204, 130, 10, 0xFF8FD8FF)
			g.ci(50, -170, 20, 0xFFFFE066)
			g.poly(0xFF7ED957, [-102, -86, -30, -150, 20, -86])
			g.poly(0xFF5BBF4C, [-40, -86, 30, -136, 102, -86])
