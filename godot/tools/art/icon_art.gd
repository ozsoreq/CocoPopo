## Chunky one-colour glyphs for round buttons (about 50 units radius, drawn white and tinted in game),
## and the speech-bubble emotes shown above characters.
extends RefCounted

const Gfx := preload("res://tools/art/gfx.gd")
const VPath := preload("res://tools/art/vpath.gd")

enum { HOME, CAMERA, BROOM, PEOPLE, CUBE, PLUS, MINUS, CHECK, DICE, CLOSE, TRASH, EDIT, SMILE, FLIP, COPY,
	BACK, STAR, SHIRT, FACE, HAIR, SPARK, PERSON_PLUS, SAVE, PLAY, CHAIR, ZZZ, HAND, INBOX, BUBBLES, SLIDEDOWN, UP }
const COUNT := 31
const EMOTES := 7


static func icon(g: Gfx, id: int, col: int) -> void:
	var w := 9.0
	match id:
		HOME:
			g.poly(col, [-30, -2, 0, -30, 30, -2])
			g.rr(-20, -4, 40, 30, 6, col)
			g.rr(-6, 8, 12, 18, 4, 0x55000000)
		CAMERA:
			g.rr(-30, -16, 60, 42, 10, col)
			g.rr(-12, -26, 24, 14, 5, col)
			g.ci(0, 5, 13, 0x66000000)
			g.ci(0, 5, 8, col)
		BROOM:
			g.ln(14, -30, -4, 4, 8, col)
			g.poly(col, [-22, 4, 6, 4, 14, 30, -30, 30])
			for i in 3:
				g.ln(-18 + i * 10, 12, -20 + i * 11, 28, 2.5, 0x66000000)
		PEOPLE:
			g.ci(0, -14, 14, col)
			g.pie(0, 32, 28, 28, 180, 180, col)
		PERSON_PLUS:
			g.ci(-6, -14, 12, col)
			g.rr(-26, 2, 40, 28, 14, col)
			g.ln(26, -20, 26, 2, 7, col); g.ln(15, -9, 37, -9, 7, col)
		CUBE:
			g.poly(col, [0, -30, 28, -16, 0, -2, -28, -16])
			g.poly(Gfx.dk(col, 0.12), [-28, -12, -2, 2, -2, 32, -28, 18])
			g.poly(Gfx.dk(col, 0.28), [28, -12, 2, 2, 2, 32, 28, 18])
		PLUS:
			g.ln(-20, 0, 20, 0, w, col); g.ln(0, -20, 0, 20, w, col)
		MINUS:
			g.ln(-20, 0, 20, 0, w, col)
		CHECK:
			g.ln(-22, 2, -6, 18, 11, col); g.ln(-6, 18, 24, -16, 11, col)
		DICE:
			g.rr(-26, -26, 52, 52, 12, col)
			for d in [[-12, -12], [12, 12], [0, 0], [12, -12], [-12, 12]]:
				g.ci(d[0], d[1], 5, 0x77000000)
		CLOSE:
			g.ln(-18, -18, 18, 18, w, col); g.ln(18, -18, -18, 18, w, col)
		TRASH:
			g.rr(-20, -12, 40, 40, 8, col)
			g.rr(-26, -22, 52, 8, 4, col)
			g.rr(-8, -30, 16, 10, 4, col)
			for i in range(-1, 2):
				g.ln(i * 10, -2, i * 10, 20, 4, 0x66000000)
		EDIT:
			g.save(); g.rotate(45)
			g.rr(-9, -30, 18, 46, 4, col)
			g.poly(col, [-9, 20, 9, 20, 0, 34])
			g.restore()
		SMILE:
			g.cis(0, 0, 24, col, 7)
			g.ci(-9, -7, 4.5, col); g.ci(9, -7, 4.5, col)
			g.arc(0, 2, 13, 11, 20, 140, 6, col)
		FLIP:
			g.ln(-26, -8, 24, -8, 7, col); g.poly(col, [26, -8, 10, -22, 10, 6])
			g.ln(26, 14, -24, 14, 7, col); g.poly(col, [-26, 14, -10, 0, -10, 28])
		COPY:
			g.rr(-26, -26, 34, 38, 8, col)
			g.rr(-8, -10, 34, 38, 8, Gfx.dk(col, 0.1))
		BACK:
			g.ln(14, -22, -14, 0, 11, col); g.ln(-14, 0, 14, 22, 11, col)
		STAR:
			var p: VPath = VPath.new()
			for i in 10:
				var a := -PI / 2 + i * PI / 5
				var r := 30.0 if i % 2 == 0 else 13.0
				if i == 0:
					p.move_to(cos(a) * r, sin(a) * r)
				else:
					p.line_to(cos(a) * r, sin(a) * r)
			g.path(p.close(), col)
		SHIRT:
			g.poly(col, [-14, -26, 14, -26, 32, -10, 22, 4, 14, -2, 14, 28, -14, 28, -14, -2, -22, 4, -32, -10])
		FACE:
			g.ci(0, 0, 26, col)
			g.ci(-9, -5, 4.5, 0x88000000); g.ci(9, -5, 4.5, 0x88000000)
			g.arc(0, 4, 12, 9, 20, 140, 5, 0x88000000)
		HAIR:
			g.pie(0, 6, 30, 30, 180, 180, col)
			g.rr(-30, 4, 14, 24, 7, col); g.rr(16, 4, 14, 24, 7, col)
		SPARK:
			g.path(VPath.new().move_to(0, -30).quad_to(4, -4, 30, 0).quad_to(4, 4, 0, 30).quad_to(-4, 4, -30, 0)
				.quad_to(-4, -4, 0, -30).close(), col)
			g.ci(22, -22, 6, col)
		CHAIR:
			g.rr(-20, -32, 10, 52, 5, col)
			g.rr(-22, -2, 44, 10, 5, col)
			g.rr(14, 4, 8, 26, 4, col); g.rr(-20, 4, 8, 26, 4, col)
		ZZZ:
			g.text("z", -12, 14, 34, col)
			g.text("z", 10, -2, 26, col)
			g.text("z", 24, -16, 18, col)
		HAND:
			g.ov(0, 6, 20, 22, col)
			for i in 4:
				g.rr(-18 + i * 10, -28, 9, 26, 4, col)
			g.ov(-22, 4, 7, 12, col)
		INBOX:
			g.rr(-26, -4, 52, 30, 6, col)
			g.ln(0, -32, 0, -6, 7, col)
			g.poly(col, [-12, -14, 12, -14, 0, 2])
		BUBBLES:
			g.cis(-10, 8, 16, col, 5); g.cis(14, -10, 11, col, 5); g.cis(16, 16, 7, col, 4)
		SLIDEDOWN:
			g.path_s(VPath.new().move_to(-26, -22).quad_to(4, -18, 18, 14), col, 8)
			g.poly(col, [26, 24, 4, 16, 22, 2])
		UP:
			g.ln(0, 24, 0, -12, 9, col)
			g.poly(col, [-18, -6, 18, -6, 0, -28])
		PLAY:
			g.poly(col, [-14, -28, 30, 0, -14, 28])
		SAVE:  # heart
			g.path(VPath.new().move_to(0, 26).cubic_to(-40, -4, -26, -30, 0, -14).cubic_to(26, -30, 40, -4, 0, 26).close(), col)


## Kinds: 0 heart, 1 star, 2 music, 3 "!", 4 "?", 5 sleepy, 6 sparkle.
static func emote(g: Gfx, kind: int) -> void:
	g.ol(4)
	g.poly(0xFFFFFFFF, [-14, 20, 14, 20, 0, 42])
	g.rr(-62, -48, 124, 82, 36, 0xFFFFFFFF)
	g.ol(0)
	match kind % 7:
		0:
			g.path(VPath.new().move_to(0, 20).cubic_to(-46, -6, -26, -34, 0, -14).cubic_to(26, -34, 46, -6, 0, 20).close(), 0xFFFF5C73)
		1:
			_small(g, STAR, -7, 0.9, 0xFFFFC93C)
		2:  # music note
			g.ln(10, -26, 10, 10, 7, 0xFF6C7BFF)
			g.ln(10, -26, 28, -18, 7, 0xFF6C7BFF)
			g.ov(-2, 10, 14, 10, 0xFF6C7BFF)
		3:
			g.text("!", 0, 14, 64, 0xFFFF9A3D)
		4:
			g.text("?", 0, 14, 64, 0xFF4FB3FF)
		5:
			g.text("z z z", 0, 10, 40, 0xFF8A93C8)
		_:
			_small(g, SPARK, -7, 0.85, 0xFFB67CFF)


static func _small(g: Gfx, id: int, y: float, s: float, col: int) -> void:
	g.save(); g.translate(0, y); g.scale(s, s)
	icon(g, id, col)
	g.restore()
