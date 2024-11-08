package main

import "core:fmt"
print   :: fmt.print
printf  :: fmt.printf
println :: fmt.println

import "core:math"
import "core:time"
import "core:math/linalg"
import "core:strings"

import glfw "vendor:glfw"
import gl   "vendor:OpenGL"
import glf  "vendor:glfw/bindings"

vec :: [3] f32

window: glfw.WindowHandle

main :: proc() {
	using glf

	assert(Init() == true, "Unable to initialize glfw")
	WindowHint(glfw.RESIZABLE, 1)

	window = CreateWindow(600, 600, "ulang Example #1", nil, nil)
	assert(window != nil, "Unable to create window")
	defer glfw.Terminate()

	SetKeyCallback(window, proc "c" (window: glfw.WindowHandle, key, scancode, action, mods: i32) {
		if key == glfw.KEY_ESCAPE && action == glfw.RELEASE do SetWindowShouldClose(window, true) 
	})

	MakeContextCurrent(window)
	SwapInterval(1)
	ShowWindow(window)
	gl.load_up_to(4, 5, glfw.gl_set_proc_address);

	gl.ClearColor(0.05, 0.05, 0.05, 0)

	verts: [1024*2 * 3] f32 
	make_uniform_sphere(verts[:], {}, 0.8, samples = len(verts) / 3) 

	vbo: u32
	gl.GenBuffers(1, &vbo)
	gl.BindBuffer(gl.ARRAY_BUFFER, vbo)
	gl.VertexAttribPointer(0, 3, gl.FLOAT, false, 3 * size_of(verts[0]), 0)
	gl.EnableVertexAttribArray(0)

	program := gl.CreateProgram()
	vert_shader := make_shader(#load("vert.glsl", string), gl.VERTEX_SHADER, program)
	frag_shader := make_shader(#load("frag.glsl", string), gl.FRAGMENT_SHADER, program)
	gl.LinkProgram(program)
	
	for !WindowShouldClose(window) {
		gl.Clear(gl.COLOR_BUFFER_BIT + gl.DEPTH_BUFFER_BIT)

		gl.BufferData(gl.ARRAY_BUFFER, len(verts) * size_of(verts[0]), raw_data(verts[:]), gl.DYNAMIC_DRAW)
		gl.UseProgram(program)

		gl.DrawArrays(gl.POINTS, 0, i32(len(verts) / 1))

		make_uniform_sphere(verts[:], {}, 0.8, 
			linalg.matrix3_rotate_f32(f32(f64(time.now()._nsec % 10e9) / 10e9 * math.TAU), 
			vec { 0, 1, 0 }))

		SwapBuffers(window)
		PollEvents()
		time.accurate_sleep(15e6)
	}

}

make_shader :: proc(shader_src: string, type: u32, program: u32) -> u32 {
	shader_src := strings.clone_to_cstring(shader_src)
	shader: u32 = gl.CreateShader(type)
	gl.ShaderSource(shader, 1, &shader_src, nil)
	gl.CompileShader(shader)
	
	ok: i32 
	gl.GetShaderiv(shader, gl.COMPILE_STATUS, &ok)
	if ok == 0 {
		log: [512] u8
		gl.GetShaderInfoLog(shader, len(log), nil, raw_data(log[:]))
		printf("%s", log)
	}

	gl.AttachShader(program, shader)

	return shader
}

make_uniform_sphere :: proc(
	verts: [] f32,
	pos: vec, 
	radius: f32, 
	transform: matrix[3, 3] f32 = linalg.MATRIX3F32_IDENTITY, 
	samples := 0) -> [] f32 
{
	samples := samples
	if samples == 0 do samples = len(verts) / 3
	phi := math.PI * (math.sqrt_f32(5) - 1)
		
	for i in 0..<samples {
		v: vec
		v.y = 1 - (f32(i) / f32(samples - 1)) * 2
		long_radius := math.sqrt_f32(1 - v.y * v.y) // longitude_
		angle : f32 = phi * f32(i)
		v.x = math.cos(angle) * long_radius
		v.z = math.sin(angle) * long_radius
		v = pos + linalg.matrix_mul_vector(transform, v) * radius // .mul(transform)
		verts[i*3+0] = v.x
		verts[i*3+1] = v.y
		verts[i*3+2] = v.z
		
	}
	return verts
}




