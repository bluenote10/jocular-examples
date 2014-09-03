#version 330

uniform sampler2D sampler;
uniform float Alpha = 1.0;
uniform vec3 Color = vec3(0.64, 0.5, 1.0);

out vec4 FragColor;


float constrainRange(vec3 v, float value) {
  float start = v.x;
  float end = v.y;
  float fade = v.z;
  if (value < start - fade || value > end + fade) {
    return -1;
  }

  float alpha = 1.0;
  if (value < start) {
    float fadeDelta = start - value;
    alpha = smoothstep(1.0, 0.0, fadeDelta / fade);
  }
  if (value > end) {
    float fadeDelta = value - end;
    alpha = smoothstep(1.0, 0.0, fadeDelta / fade);
  }
  return alpha;
}


void main() {
    vec4 c = texture(sampler, gl_PointCoord);
    c.a = min(Alpha, c.a);
    if (c.a < 1.0) {
      discard;
    }
    FragColor = c;
}

