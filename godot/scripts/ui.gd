class_name UI
extends RefCounted
## Small UI helpers shared by screens.

const OUTLINE := Color("2a1f2e")

static func font(size := 40) -> SystemFont:
	var f := SystemFont.new()
	f.font_names = PackedStringArray(["Nunito", "Baloo 2", "Fredoka", "Arial Rounded MT Bold", "Roboto", "sans-serif"])
	f.font_weight = 800
	return f

static func label(text: String, size: int, col: Color, outline := 0, outline_col := OUTLINE) -> Label:
	var l := Label.new()
	l.text = text
	l.add_theme_font_override("font", font())
	l.add_theme_font_size_override("font_size", size)
	l.add_theme_color_override("font_color", col)
	if outline > 0:
		l.add_theme_constant_override("outline_size", outline)
		l.add_theme_color_override("font_outline_color", outline_col)
	return l
