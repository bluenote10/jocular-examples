#version 440

uniform float Time;
uniform vec3 Color = vec3(218.0 / 255.0, 165.0 / 255.0, 32.0 / 255.0);

float time = Time;
in vec2 vTexCoord;
out vec4 fragColor;

#define PI 3.14159
#define HALF_PI (3.14159 / 2.0)
#define TWO_PI (3.14159 * 2.0)


// some noise functions for fast developing
float rand11(float p)
{
    return fract(sin(p * 591.32) * 43758.5357);
}
float rand12(vec2 p)
{
    return fract(sin(dot(p.xy, vec2(12.9898, 78.233))) * 43758.5357);
}
vec2 rand21(float p)
{
    return fract(vec2(sin(p * 591.32), cos(p * 391.32)));
}
vec2 rand22(in vec2 p)
{
    return fract(vec2(sin(p.x * 591.32 + p.y * 154.077), cos(p.x * 391.32 + p.y * 49.077)));
}

float noise11(float p)
{
    float fl = floor(p);
    return mix(rand11(fl), rand11(fl + 1.0), fract(p));//smoothstep(0.0, 1.0, fract(p)));
}
float fbm11(float p)
{
    return noise11(p) * 0.5 + noise11(p * 2.0) * 0.25 + noise11(p * 5.0) * 0.125;
}
vec3 noise31(float p)
{
    return vec3(noise11(p), noise11(p + 18.952), noise11(p - 11.372)) * 2.0 - 1.0;
}

// something that looks a bit like godrays coming from the surface
float sky(vec3 p)
{
    float a = atan(p.x, p.z);
    float t = time * 0.1;
    float v = rand11(floor(a * 4.0 + t)) * 0.5 + rand11(floor(a * 8.0 - t)) * 0.25 + rand11(floor(a * 16.0 + t)) * 0.125;
    return v;
}

vec3 voronoi(in vec2 x)
{
    vec2 n = floor(x); // grid cell id
    vec2 f = fract(x); // grid internal position
    vec2 mg; // shortest distance...
    vec2 mr; // ..and second shortest distance
    float md = 8.0, md2 = 8.0;
    for(int j = -1; j <= 1; j ++)
    {
        for(int i = -1; i <= 1; i ++)
        {
            vec2 g = vec2(float(i), float(j)); // cell id
            vec2 o = rand22(n + g); // offset to edge point
            vec2 r = g + o - f;
            
            float d = max(abs(r.x), abs(r.y)); // distance to the edge
            
            if(d < md)
                {md2 = md; md = d; mr = r; mg = g;}
            else if(d < md2)
                {md2 = d;}
        }
    }
    return vec3(n + mg, md2 - md);
}

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

void main(void)
{
  vec2 ndc = vTexCoord * 2.0 - 1.0;
  float dist = length(ndc);
  float maxAlpha = 0.8;
  
  float alpha = constrainRange(vec3(0.7, 0.85, 0.02), dist);
  if (alpha < 0.0) {
    discard;
  }
  vec2 normalized = normalize(ndc);
  float angle = atan(normalized.y, normalized.x);
  vec3 noise = voronoi(vec2(angle + PI, Time / 8.0));
  alpha *= constrainRange(vec3(0.02, 1.0, 0.02), noise.z);
  if (alpha < 0.0) {
    discard;
  }
  fragColor = vec4(Color, maxAlpha * alpha);
}