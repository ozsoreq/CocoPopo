shader_type canvas_item;
// White pulsing outline used to highlight drop targets.
uniform float amount = 0.0;
uniform float width = 10.0;

void fragment() {
	vec4 c = texture(TEXTURE, UV);
	if (amount > 0.0 && c.a < 0.6) {
		float a = 0.0;
		vec2 px = TEXTURE_PIXEL_SIZE * width;
		for (int i = 0; i < 16; i++) {
			float ang = float(i) * 0.3927;
			a = max(a, texture(TEXTURE, UV + vec2(cos(ang), sin(ang)) * px).a);
			a = max(a, texture(TEXTURE, UV + vec2(cos(ang), sin(ang)) * px * 0.5).a);
		}
		COLOR = mix(c, vec4(1.0, 1.0, 1.0, a * 0.95), amount * (1.0 - c.a));
	} else {
		COLOR = c;
	}
}
