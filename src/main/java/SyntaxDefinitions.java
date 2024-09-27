import java.util.HashMap;
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

    // This is basically just to not make very many string copies... If I had more types, this should, probably, be an enum
    static final String TYPE_NUMBER  = "num";
    static final String TYPE_BOOLEAN = "bool";
    static final String TYPE_STRING  = "string";
    static final String TYPE_CHAR    = "char";

    static Map<String, Class> types = Map.of(
        TYPE_NUMBER,  Double.class,
        TYPE_BOOLEAN, Boolean.class,
        TYPE_STRING,  String.class,
        TYPE_CHAR,    Character.class
    );

    static class OperatorTypeData {
        String out, lhs, rhs;
    }
    static private OperatorTypeData entype_operator(String out, String lhs, String rhs) {
        OperatorTypeData data = new OperatorTypeData();
        data.lhs = lhs;
        data.rhs = rhs;
        data.out = out;
        return data;
    }
    static private OperatorTypeData entype_operator(String out, String rhs) { return entype_operator(out, null, rhs); }
    static Map<String, OperatorTypeData> unary_types = Map.of(
        "+", entype_operator(TYPE_NUMBER, TYPE_NUMBER),
        "-", entype_operator(TYPE_NUMBER, TYPE_NUMBER),
        "!", entype_operator(TYPE_BOOLEAN, TYPE_BOOLEAN)
    );

    static HashMap<String, OperatorTypeData> binary_types = new HashMap<>();
    static {
        binary_types.put("+", entype_operator(TYPE_NUMBER, TYPE_NUMBER, TYPE_NUMBER));
        binary_types.put("-", entype_operator(TYPE_NUMBER, TYPE_NUMBER, TYPE_NUMBER));
        binary_types.put("*", entype_operator(TYPE_NUMBER, TYPE_NUMBER, TYPE_NUMBER));
        binary_types.put("/", entype_operator(TYPE_NUMBER, TYPE_NUMBER, TYPE_NUMBER));
        binary_types.put("%", entype_operator(TYPE_NUMBER, TYPE_NUMBER, TYPE_NUMBER));

        binary_types.put("||", entype_operator(TYPE_BOOLEAN, TYPE_BOOLEAN, TYPE_BOOLEAN));
        binary_types.put("&&", entype_operator(TYPE_BOOLEAN, TYPE_BOOLEAN, TYPE_BOOLEAN));

        binary_types.put(">",  entype_operator(TYPE_BOOLEAN, TYPE_NUMBER, TYPE_NUMBER));
        binary_types.put("<",  entype_operator(TYPE_BOOLEAN, TYPE_NUMBER, TYPE_NUMBER));
        binary_types.put(">=", entype_operator(TYPE_BOOLEAN, TYPE_NUMBER, TYPE_NUMBER));
        binary_types.put("<=", entype_operator(TYPE_BOOLEAN, TYPE_NUMBER, TYPE_NUMBER));
    }
}
