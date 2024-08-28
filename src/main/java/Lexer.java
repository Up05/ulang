import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

class LexerException extends Exception {
    String message, file;
    int line;

    public LexerException(String message, String filename, int lineNumber) {
        this.message = message;
        file = filename;
        line = lineNumber;
    }

    @Override
    public String getMessage() {
        return String.format("(%s:%d) %s", file, line, message);
    }
}

public class Lexer {
    enum Type {
        UNKNOWN,
        INFORMATIONAL,
        SYMBOL,

        CONTAINER,
        CONTAINER_INDEX,

        BINARY_OPERATOR,
        UNARY_OPERATOR,

        CONSTANT, // a.k.a. literal
        VARIABLE,
        TYPE,

        FUNCTION_DECL,
        FUNCTION_CALL,

        KEYWORD;

        public boolean is_any(Type... types) {
            for(Type t : types)
                if(this == t) return true;
            return false;
        }
    }

    public static boolean could_be_expr(Token token) {
        return token.type.is_any(Type.CONSTANT, Type.VARIABLE, Type.FUNCTION_CALL, Type.UNARY_OPERATOR) || token.is("(");
    }
    static private final Type[] tokens_potentially_before_binary_operator = { Type.CONSTANT, Type.VARIABLE, Type.FUNCTION_CALL, Type.SYMBOL };

    static private final String[]  unary_operators = { "+", "-", "!" };
    static private final String[] binary_operators = { ".", "@", "+", "-", "*", "/", "%", "||", "&&", "!=", "==", "<=", ">=", "<", ">", }; // Yes it's duplicate, but flattened -- faster*
    static private final String[] informational    = { "\n", "EOF" };
    static private final String[] delimiters       = { ":", "=", "(", ")", "]", "{", "}", ";", "," };
    static private final String[] keywords         = { "do", "if", "for", "return" };
    // for init; loop start; loop end do loop middle

    LexerException potential_exception = new LexerException("", "", 0);
    private HashSet<String> declared_vars = new HashSet<>();
    private String[] raw_tokens;
    private ArrayList<Token> tokens = new ArrayList<>();
    int current_token = 0;

    public Lexer(String[] raw_tokens, String filename) {
        potential_exception.file = filename;
        potential_exception.line = 1;
        this.raw_tokens = raw_tokens;
    }

    private String peek(int offset) {
        return raw_tokens[current_token + offset];
    }

    private String next() {
        return raw_tokens[current_token ++];
    }
    private void skip(int count) {
        current_token += count;
    }
    private boolean has(int count) {
        return current_token + count < raw_tokens.length;
    }
    private boolean EOF() {
        return current_token + 1 >= raw_tokens.length;
    }

    // unnecessary work, but the parser should become simpler
    public ArrayList<Token> lex() throws Exception {

        while(current_token < raw_tokens.length) {
            if (EOF()) break;

            if (peek(0).equals("\n")) {
                potential_exception.line++;
                tokens.add(new Token(peek(0), Type.INFORMATIONAL));
                next();
            }

            boolean success = false;
            new Monad<>(success, false)
                .bind(this::lex_keyword)
                .bind(this::lex_info)
                .bind(this::lex_operator)
                .bind(this::lex_delim)
                .bind(this::lex_const)
                .bind(this::lex_list)
                .bind(this::lex_type)
                // .bind(this::lex_return_type)
                .bind(this::lex_func_decl)
                .bind(this::lex_func_call)
                .bind(this::lex_var)
                .bind(this::lexer_skip_bad)
                .unwrap();

        }
        tokens.add(new Token(raw_tokens[raw_tokens.length - 1], Type.INFORMATIONAL)); // 'EOF'
        return tokens;
    }

    private boolean lexer_skip_bad() {
        System.out.println("Could not lex token: '" + peek(0) + "'");
        skip(1);
        return false;
    }

    private boolean lex_keyword() {
        if(Util.array_contains(keywords, peek(0)))
            return tokens.add(new Token(next(), Type.KEYWORD));
        return false;
    }
    private boolean lex_info() {
        if(Util.array_contains(informational, peek(0)))
            return tokens.add(new Token(next(), Type.INFORMATIONAL));
        return false;
    }
    private boolean lex_delim() {
        if(Util.array_contains(delimiters, peek(0)))
            return tokens.add(new Token(next(), Type.SYMBOL));
        return false;
    }
    // lex const
    private boolean lex_return_type() {
        if (has(2) && peek(0).equals("-") && peek(1).equals(">")) {
            tokens.add(new Token(peek(2), Type.TYPE));
            skip(3);
            return true;
        }
        return false;
    }
    // lex operator
    private boolean lex_func_decl() {
        if(peek(0).equals("func")) {
            tokens.add(new Token(peek(1), Type.FUNCTION_DECL));
            skip(2);
            return true;
        }
        return false;
    }
    private boolean lex_func_call() {
        if(has(2) && peek(1).equals("(")) {
            tokens.add(new Token(next(), Type.FUNCTION_CALL));
            return true;
        }
        return false;
    }

    private boolean lex_var() {
        boolean is_var =
            declared_vars.contains(peek(0)) ||
            has(2) && peek(1).equals(":") || peek(1).equals("=");
        if(!is_var) return false;

        declared_vars.add(peek(0));
        tokens.add(new Token(next(), Type.VARIABLE)); // <name>

        if(peek(0).equals(":")) { // For declaration
            next();
            lex_type();
        } else if(peek(0).equals("[")) { // For usage
            tokens.add(new Token("[", Type.CONTAINER_INDEX));
            next();
        }
        return true;
    }

    private boolean lex_type() {

        if(peek(0).equals("[")) {
            next();
            if(peek(0).equals("]")) { // declarations, e.g.: a : [] num
                next();
                if(!types.containsKey(peek(0))) { skip(-2); return false; }
                tokens.add(new Token("array", Type.TYPE));
                tokens.add(new Token(next(), Type.TYPE));
            } else if(peek(0).equals("map")) { // maps, e.g.: a : [map] string num
                tokens.add(new Token("map", Type.TYPE));
                // assert skip + 4 == ']'
                next(); // skips ']'
                tokens.add(new Token(next(), Type.TYPE));
                tokens.add(new Token(next(), Type.TYPE));
            } else {
                skip(-1);
                return false;
            }
            return true;
        } else if(types.containsKey(peek(0))) {
            tokens.add(new Token(next(), Type.TYPE));
            return true;
        }
        return false;
    }

    private boolean is_const(String token) {
        char first = token.charAt(0);
        return
            Character.isDigit(first) ||
            first == '\'' || first == '"' ||
            token.equals("true") || token.equals("false");
    }
    private boolean lex_const() {
        if(!is_const(peek(0))) return false;
        if(has(3) && peek(1).equals(".") && is_const(peek(2))) {
            // The one copy... ಥ_ಥ  // Half a month later: not anymore ¯\_(ツ)_/¯
            tokens.add(new Token(next() + next() + next(), Type.CONSTANT));
        } else
            tokens.add(new Token(next(), Type.CONSTANT));
        return true;
    }

    private boolean lex_operator() {
        int unary = matching_operator_chars(unary_operators),
            binary = matching_operator_chars(binary_operators);

        if(unary > 0 && binary > 0) { // ambiguous
            if(tokens.isEmpty()) {
                tokens.add(new Token(concat_tokens(unary), Type.UNARY_OPERATOR));
                return true;
            }

            Type prev_token = tokens.get(tokens.size() - 1).type;
            if(Util.array_contains(tokens_potentially_before_binary_operator, prev_token)) {
                tokens.add(new Token(concat_tokens(binary), Type.BINARY_OPERATOR));
                return true;
            }
        }

        if(unary > 0) {
            tokens.add(new Token(concat_tokens(unary), Type.UNARY_OPERATOR));
        } else if(binary > 0) {
            tokens.add(new Token(concat_tokens(binary), Type.BINARY_OPERATOR));
        } else return false;
        return true;
    }

    private boolean lex_list() {
        if(peek(0).equals("["))
            return tokens.add(new Token(next(), Type.CONTAINER));
        return false;
    }

    private int matching_operator_chars(String[] all) {
        for(String op : all) {
            boolean matches = true;
            for(int i = 0; i < op.length(); i ++) {
                if(op.charAt(i) != peek(i).charAt(0)) {
                    matches = false;
                    break;
                }
            }
            if(matches) return op.length();
        }
        return 0;
    }

    private String concat_tokens(int amount) {
        StringBuilder b = new StringBuilder();
        for(; amount > 0; amount --)
            b.append(next());
        return b.toString();
    }

    // I swear, I've written and deleted this function > 5 times by now...
    private boolean looks_like_variable(String t) {
        if(Character.isDigit(t.charAt(0))) return false;
        for(char r : t.toCharArray())
            if(!Character.isLetterOrDigit(r) && r != '_') return false;
        return true;
    }


    private void assertf(boolean expr, String format, Object... values) throws LexerException {
        if(expr) return;
        potential_exception.message = String.format(format, values) + "\n";
        throw potential_exception;
    }

    private static String unary_precedence_table[][] = {
        { "!" },
        { "+", "-" }
    };

    private static String binary_precedence_table[][] = {
        { "or_else"},
        { "@" },
        { "&&", "||" },
        { "==", "!=", "<", ">", "<=", ">=" },
        { "+", "-" },
        { "*", "/", "%" },
        { "." }
    };

    public static int get_precedence(Token t) {
        String[][] table_ref = null;
        if(t.type == Type.UNARY_OPERATOR) table_ref = unary_precedence_table;
        else if(t.type == Type.BINARY_OPERATOR) table_ref = binary_precedence_table;
        else System.out.println("It is only possible to get precedence of unary & binary operators!");

        for(int i = 0; i < table_ref.length; i ++) {
            for(int j = 0; j < table_ref[i].length; j ++) {
                if(table_ref[i][j].equals(t.token)) return i;
                // precedence CAN be 0 here and I don't really see a problem with that.
            }
        }

        return 1;
    }

    static Map<String, Class> types = Map.of (
        "bool", Boolean.class,
        "num",  Double.class,
        "char", Character.class,
        "string", String.class
    );

}
