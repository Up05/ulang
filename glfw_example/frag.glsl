#version 330 core
out vec4 FragColor;
in float brightness;

void main() {
	FragColor = vec4(.5f, .8f, .4f, 1f);
	FragColor.xyz *= brightness;
}
