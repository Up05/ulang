#version 330 core
layout (location = 0) in vec3 aPos;
out float brightness;

void main() {
	gl_Position = vec4(aPos.x, aPos.y, aPos.z, 1.0f);
	vec3 normal = aPos.xyz;
	brightness = dot(normal, normalize(vec3(10f, 10f, 4f))) / 2f + 0.5f;
}

float dot(vec3 a, vec3 b) {
	return a.x * b.x + a.y * b.y + a.z * b.z;
}

vec3 normalize(vec3 a) {
	float len = sqrt(a.x*a.x + a.y*a.y + a.z*a.z);
	return a.xyz / len;
}


