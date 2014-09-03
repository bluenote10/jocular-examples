#version 330

uniform mat4 Projection = mat4(1);
uniform mat4 ModelView = mat4(1);
uniform vec4 Color = vec4(1, 0, 0, 1);
uniform vec3 EyePosition = vec3(0);
layout(location = 0) in vec3 Position;

out vec4 vColor;

void main() {
  vColor = Color;
  vec3 pointEyePosition = EyePosition - Position;
  float dist = length(pointEyePosition);
//  dist = pow(2.0, dist);
  gl_PointSize = 100.0 / dist;
  gl_Position = Projection * ModelView * vec4(Position, 1);
  float size = gl_Position.w;
  gl_PointSize = 48.0 / size;
}
