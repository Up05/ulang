import org.lwjgl.opengl.GL33;

import java.io.*;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.BitSet;

import static org.lwjgl.opengl.GL11C.glClearColor;

public class Main {

    static Path base = Paths.get("").toAbsolutePath();

    public static void main(String[] args) throws Exception {
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
*/
