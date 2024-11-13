import org.joml.Matrix3d;
import org.joml.Vector3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33;

import java.io.*;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import static org.lwjgl.opengl.GL11C.glClearColor;
import static org.lwjgl.opengl.GL11C.glDrawArrays;

public class Main {

    static Path base = Paths.get("").toAbsolutePath();

    public static void main(String[] args) throws Exception {

        // if(Math.random() > 0.000001) { GLFW_TEST(); return; }

        if(args.length < 1) {
            System.out.println (
                """ 
                    Usage:
                    ulang <file>
               
                    You must provide a path to a file!
                    """
            );
            return;
        }

        base = base.resolve(Path.of(args[0]));

        if(Files.isDirectory(base)) {
            System.out.println("Please provide a path to a file instead of a directory!");
            return;
        }

        Path in_file = base;

        base = base.getParent();
        if(!Files.exists(base.resolve("std.u"))) {
            Path std = base.resolve("std.u");
            try {
                InputStream included = Main.class.getResourceAsStream("std.u");
                Files.copy(included, std);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> prepped_tokens = Preprocessor.preprocess(base, in_file.toString());

        System.out.println("---".repeat(24));
        Debug.print_tokens(prepped_tokens);

        new TokenValidator(prepped_tokens, in_file.getFileName().toString()).validate();

        System.out.println("---".repeat(24));

        ArrayList<Token> tokens = new Lexer(prepped_tokens, in_file.getFileName().toString()).lex();
        Debug.print_lexed_tokens(tokens);
        SyntaxDefinitions.reset_types();

        Ast ast = new Parser(tokens, in_file.getFileName().toString()).parse();

        Debug.send_ast_to_vis(ast);

        new TypeValidator().validate(ast);

        Interpreter interpreter = new Interpreter((Ast.Root) ast);
        interpreter.interpret(ast);

    }

    private static void GLFW_TEST() throws IOException {
        GLFW.glfwInit();
        long window = GLFW.glfwCreateWindow(600, 600, "test", 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();
        GL33.glClearColor(0.05f, 0.05f, 0.05f, 0f);


        DoubleBuffer verts = BufferUtils.createDoubleBuffer(3 * 3);

        double[] positions = new double[]{
            0.0f, 0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f
        };
        verts.put(positions);
        verts.flip();

        int vbo = GL33.glGenBuffers();
        GL33.glBindBuffer(34962, vbo);
        GL33.glEnableVertexAttribArray(0);
        GL33.glVertexAttribPointer(0, 3, GL33.GL_DOUBLE, false, 0, 0);

        int program = GL33.glCreateProgram();
        make_shader("vert.glsl", 35633, program);
        make_shader("frag.glsl", 35632, program);
        GL33.glLinkProgram(program);
        GL33.glUseProgram(program);


        while(!GLFW.glfwWindowShouldClose(window)) {
            GL33.glClear(16384 + 256);

            GL33.glBufferData(GL33.GL_ARRAY_BUFFER, verts, GL33.GL_DYNAMIC_DRAW);
            GL33.glDrawArrays(GL33.GL_TRIANGLES, 0, 3);
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    public static void make_shader(String file, int type, int program) throws IOException {
        int shader = GL33.glCreateShader(type);
        GL33.glShaderSource(shader, new String(Files.readAllBytes(Path.of(file))));
        GL33.glCompileShader(shader);

        int ok = GL33.glGetShaderi(shader, GL33.GL_COMPILE_STATUS);
        if(ok == 0) {
            System.out.println(GL33.glGetShaderInfoLog(shader));
        }

        new Vector3d().mul(new Matrix3d().rotateY(2));

        GL33.glAttachShader(program, shader);
    }
}

/*
* cli
* tokenizer
* lexer
* validator  (handle missing function brackets???)
* parser/ast
* interpreter
* compiler to compiler & OS agnostic C ?
*
* Make Commons.java
* test
* clean up Lexer (especially)
* \ before " and ' in tokenizer
* exception stack traces are for when they are inited, not thrown, for some reason
* CompileException and RuntimeException ???
* Better interface for declared types
* Calculate lines correctly (for errors in parser) Maybe just put line thing in next()?
* Check array type at runtime when using [] & append
*
* Future: switch blocks
*
*
*
* TODO: array literals must be only the same line right now, since it only skips a comma...
*/
