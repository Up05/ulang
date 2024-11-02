import java.util.ArrayList;
import java.util.Stack;
import java.util.HashSet;

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

public class Lexer extends Stage<String> {
    enum Type {
        INFORMATIONAL,
        INSERTED_FILE,
        SYMBOL,

        CONTAINER,
        CONTAINER_INDEX,

        BINARY_OPERATOR,
        UNARY_OPERATOR,

        CONSTANT, // a.k.a. literal
        VARIABLE,

        TYPE,
        TYPE_DECL,
        FOREIGN_PATH,

        FUNCTION_DECL,
        FUNCTION_CALL,

        KEYWORD,

    }

    private final HashSet<String> declared_vars = new HashSet<>();
    private ArrayList<Token> lexed_tokens = new ArrayList<>();

    private Stack<Error> error_stack = new Stack<>(); // I don't think, that I use this anywhere and I shouldn't either way

    public Lexer(ArrayList<String> raw_tokens, String filename) {
        this.tokens = raw_tokens;
        error_stack.push(new Error(Error.Type.COMP, filename));
    }

    private boolean EOF() {
        return peek(0).equals("EOF");
    }

    public ArrayList<Token> lex() throws Exception {

        while(curr < tokens.size()) {
            if (EOF()) {
                error_stack.pop();
                next();
                continue;
            }

            if (peek(0).equals("\n")) {
                // potential_exception.line++;
                error_stack.peek().line ++;
                lexed_tokens.add(new Token(peek(0), Type.INFORMATIONAL));
                next();
            }

            boolean success = false;
            new Monad<>(success, false)
                .bind(this::lex_preproc)
                .bind(this::lex_keyword)
                .bind(this::lex_info)
                .bind(this::lex_type)
                .bind(this::lex_type_def)
                .bind(this::lex_operator)
                .bind(this::lex_delim)
                .bind(this::lex_const)
                .bind(this::lex_func_decl)
                .bind(this::lex_func_call)
                .bind(this::lex_list)
                .bind(this::lex_var)
                .bind(this::lexer_skip_bad)
                .unwrap();

        }
        lexed_tokens.add(new Token(tokens.get(tokens.size() - 1), Type.INFORMATIONAL)); // 'EOF'
        return lexed_tokens;
    }

    private boolean lexer_skip_bad() {
        System.out.println("Could not lex token: '" + peek(0) + "'");
        skip(1);
        return false;
    }
    private Boolean lex_preproc() {
        if(!peek(0).equals("$")) return false;
        next();
        if(peek(0).equals("push")) {
            next();
            String name = next().substring(1, peek(-1).length() - 1);
            lexed_tokens.add(new Token(name, Type.INSERTED_FILE));
            error_stack.push(new Error(Error.Type.COMP, name));
            return true;
        }
        return false;
    }
    private boolean lex_keyword() {
        if(Util.array_contains(SyntaxDefinitions.keywords, peek(0)))
            return lexed_tokens.add(new Token(next(), Type.KEYWORD));
        return false;
    }
    private boolean lex_info() {
        if(Util.array_contains(SyntaxDefinitions.informational, peek(0)))
            return lexed_tokens.add(new Token(next(), Type.INFORMATIONAL));
        return false;
    }
    private boolean lex_delim() {
        if(Util.array_contains(SyntaxDefinitions.delimiters, peek(0)))
            return lexed_tokens.add(new Token(next(), Type.SYMBOL));
        return false;
    }
    private boolean lex_func_decl() { // TODO: Totally redid the function, so it works without calling parse_type in the normal parse_expr
        if(!peek(0).equals("func")) return false;
        next(); // skips 'func'
        lexed_tokens.add(new Token(next(), Type.FUNCTION_DECL));

        next(); // skips '('
        while(true) {
            if (peek(0).equals(",")) next();
            if (peek(0).equals(")")) break;
            lex_var();
        }
        next(); // skips ')'

        if(!peek(0).equals("=") && !peek(0).equals("{"))
            lex_type(); // return type

        if(peek(0).equals("=")) {
            next(); // skips '='
            lexed_tokens.add(new Token(next(), Type.KEYWORD));
            lexed_tokens.add(new Token(next(), Type.FOREIGN_PATH));
        }

        return true;
    }
    private boolean lex_func_call() {
        if(are_there(2) && peek(1).equals("(")) {
            lexed_tokens.add(new Token(next(), Type.FUNCTION_CALL));
            return true;
        }
        return false;
    }
    private boolean lex_var() {
        if(peek(0).equals(".") && peek(1).equals(".") && peek(2).equals(".")) {
            lexed_tokens.add(new Token("varargs", Type.VARIABLE));
            skip(3);
            return true;
        }

        boolean is_var =
            declared_vars.contains(peek(0)) ||
                are_there(2) && peek(1).equals(":") || peek(1).equals("=");
        if(!is_var) return false;

        declared_vars.add(peek(0));
        lexed_tokens.add(new Token(next(), Type.VARIABLE)); // <name>

        if(peek(0).equals(":")) { // For declaration
            next();
            lex_type();
        } else if(peek(0).equals("[")) { // For usage
            lexed_tokens.add(new Token("[", Type.CONTAINER_INDEX));
            next();
        }
        return true;
    }
    private boolean is_type() {
        if(peek(0).equals("[") && peek(1).equals("]")) {
            skip(2);
            boolean is_type = is_type();
            skip(-2);
            return is_type; }

        return SyntaxDefinitions.types.containsKey(peek(0));
    }
    private boolean lex_type() {
        if(!is_type()) return false;
        if(peek(0).equals("[")) {
            next();
            if(peek(0).equals("]")) { // declarations, e.g.: a : [] num
                next();
                if(peek(0).equals("[")) {
                    lexed_tokens.add(new Token("array", Type.TYPE));
                    lex_type();
                    return true;
                }
                lexed_tokens.add(new Token("array", Type.TYPE));
                lexed_tokens.add(new Token(next(), Type.TYPE));
            } else if(peek(0).equals("map")) { // maps, e.g.: a : [map] string num
                lexed_tokens.add(new Token("map", Type.TYPE));
                // assert skip + 4 == ']'
                next(); // skips ']'
                lexed_tokens.add(new Token(next(), Type.TYPE));
                lexed_tokens.add(new Token(next(), Type.TYPE));
            } else {
                skip(-1);
                return false;
            }
        } else {
            lexed_tokens.add(new Token(next(), Type.TYPE));
        }
        return true;
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
        if(are_there(3) && peek(1).equals(".") && is_const(peek(2))) {
            // The one copy... ಥ_ಥ  // Half a month later: not anymore ¯\_(ツ)_/¯
            lexed_tokens.add(new Token(next() + next() + next(), Type.CONSTANT));
        } else
            lexed_tokens.add(new Token(next(), Type.CONSTANT));
        return true;
    }
    private boolean lex_operator() {
        int unary = matching_operator_chars(SyntaxDefinitions.unary_operators),
            binary = matching_operator_chars(SyntaxDefinitions.binary_operators);

        if(unary > 0 && binary > 0) { // ambiguous
            if(lexed_tokens.isEmpty()) {
                lexed_tokens.add(new Token(concat_tokens(unary), Type.UNARY_OPERATOR));
                return true;
            }

            Type prev_token = lexed_tokens.get(lexed_tokens.size() - 1).type;
            if(Util.array_contains(SyntaxDefinitions.tokens_potentially_before_binary_operator, prev_token)) {
                lexed_tokens.add(new Token(concat_tokens(binary), Type.BINARY_OPERATOR));
                return true;
            }
        }

        if(unary > 0) {
            lexed_tokens.add(new Token(concat_tokens(unary), Type.UNARY_OPERATOR));
        } else if(binary > 0) {
            lexed_tokens.add(new Token(concat_tokens(binary), Type.BINARY_OPERATOR));
        } else return false;
        return true;
    }
    private boolean lex_list() {
        if(peek(0).equals("["))
            return lexed_tokens.add(new Token(next(), Type.CONTAINER));
        return false;
    }
    private boolean lex_type_def() {
        // type File = foreign "java.util.File"
        if(!peek(0).equals("typedef")) return false;
        next();

        SyntaxDefinitions.types.put(peek(0), null); // this will be cleared whenever parsing begins.
        lexed_tokens.add(new Token(next(), Type.TYPE_DECL));

        next(); // skips '='
        next(); // we just assume 'foreign'

        lexed_tokens.add(new Token(peek(0), Type.FOREIGN_PATH));
        return true;
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

    public static int get_precedence(Token t) {
        String[][] table_ref = null;
        if(t.type == Type.UNARY_OPERATOR) table_ref = SyntaxDefinitions.unary_precedence_table;
        else if(t.type == Type.BINARY_OPERATOR) table_ref = SyntaxDefinitions.binary_precedence_table;
        else System.out.println("It is only possible to get precedence of unary & binary operators!");

        for(int i = 0; i < table_ref.length; i ++) {
            for(int j = 0; j < table_ref[i].length; j ++) {
                if(table_ref[i][j].equals(t.token)) return i;
                // precedence CAN be 0 here and I don't really see a problem with that.
            }
        }

        return 1;
    }

}
