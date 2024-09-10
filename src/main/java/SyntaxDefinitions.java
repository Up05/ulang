import java.util.Map;

public class SyntaxDefinitions {

    static final Lexer.Type[] tokens_potentially_before_binary_operator = { Lexer.Type.CONSTANT, Lexer.Type.VARIABLE, Lexer.Type.FUNCTION_CALL, Lexer.Type.SYMBOL };

    // HashSets may be faster here, but they might just as well be dramatically slower
    static final String[] reserved = { "int", "i64", "f64", "i32", "f32" };

    static final String[]  unary_operators = { "+", "-", "!" };
    static final String[] binary_operators = { ".", "@", "+", "-", "*", "/", "%", "||", "&&", "!=", "==", "<=", ">=", "<", ">", }; // Yes it's duplicate, but flattened -- faster*
    static final String[] informational    = { "\n", "EOF" };
    static final String[] delimiters       = { ":", "=", "(", ")", "]", "{", "}", ";", "," };
    static final String[] keywords         = { "do", "if", "for", "return" };

    static String[][] unary_precedence_table = {
        { "!" },
        { "+", "-" }
    };
    static String[][] binary_precedence_table = {
        { "or_else"},
        { "@" },
        { "&&", "||" },
        { "==", "!=", "<", ">", "<=", ">=" },
        { "+", "-" },
        { "*", "/", "%" },
        { "." }
    };

    static Map<String, Class> types = Map.of (
        "bool", Boolean.class,
        "num",  Double.class,
        "char", Character.class,
        "string", String.class
    );
}
