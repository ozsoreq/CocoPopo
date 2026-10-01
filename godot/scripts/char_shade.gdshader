shader_type canvas_item;
// Soft volume for character parts: light from the top, gentle shade towards the bottom,
// applied only to the coloured fill (dark outlines and details stay crisp).
uniform float light = 0.10;
uniform float shade = 0.16;

void fragment() {
	vec4 tex = texture(TEXTURE, UV);
	float lum = dot(tex.rgb, vec3(0.299, 0.587, 0.114));
	float fillw = smoothstep(0.35, 0.6, lum);
	float g = smoothstep(0.0, 1.0, UV.y);
	vec3 shaded = tex.rgb * (1.0 + light * (1.0 - g) - shade * g * g);
	tex.rgb = mix(tex.rgb, shaded, fillw);
	COLOR = tex * COLOR;
}
