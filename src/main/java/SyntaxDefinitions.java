import java.util.*;

public class SyntaxDefinitions {

    static final Lexer.Type[] tokens_potentially_before_binary_operator = { Lexer.Type.CONSTANT, Lexer.Type.VARIABLE, Lexer.Type.FUNCTION_CALL };

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
    static final String TYPE_TYPE    = "type";
    static final String TYPE_VARARGS = "...";
    static final String TYPE_ANY     = "any";

    static HashMap<String, Class> types = new HashMap<>();
    static public void reset_types() {
        types.clear();
        types.putAll(Map.of(
            TYPE_NUMBER,  Double.class,
            TYPE_BOOLEAN, boolean.class,
            TYPE_STRING,  String.class,
            TYPE_CHAR,    char.class,
            TYPE_TYPE,    Class.class,
            TYPE_VARARGS, Object[].class,
            TYPE_ANY,     Object.class));
    }
    static { reset_types(); }

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
        //                                    OUTPUT TYPE  INPUT 1      INPUT 2      // Why tf did I order them like that???
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

        binary_types.put("==", entype_operator(TYPE_BOOLEAN, TYPE_ANY, TYPE_ANY));
        binary_types.put("!=", entype_operator(TYPE_BOOLEAN, TYPE_ANY, TYPE_ANY));
    }

    static Boolean is_sep(String token) {
        return token.equals("\n") || token.equals(";");
    }

    // I still do not want to couple this language to Java...
    static HashMap<String, Class> primitive_java_types = new HashMap<>();
    static {
        primitive_java_types.put("byte",    byte.class);
        primitive_java_types.put("char",    char.class);
        primitive_java_types.put("boolean", boolean.class);
        primitive_java_types.put("short",   short.class);
        primitive_java_types.put("int",     int.class);
        primitive_java_types.put("long",    long.class);
        primitive_java_types.put("float",   float.class);
        primitive_java_types.put("double",  double.class);
    }

    private static <T> HashSet from_many(T... objs) {
        return new HashSet<>(Arrays.asList(objs));
    }

}
