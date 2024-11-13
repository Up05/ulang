#version 330 core
out vec4 FragColor;
in float brightness;

void main() {
	FragColor = vec4(1.2f, 1.5f, 1.1f, 1f);
	FragColor.xyz *= brightness;
}
