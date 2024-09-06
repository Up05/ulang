import java.util.ArrayList;
import java.util.Stack;

public class TokenValidator {

    Stack<Error> error_stack = new Stack<>();
    ArrayList<String> raw_tokens;
    int curr = 0;

    public TokenValidator(ArrayList<String> raw_tokens, String filename) {
        this.raw_tokens = raw_tokens;
        error_stack.push(new Error(Error.Type.COMP, filename));
    }

    public void validate() throws Exception {
        
        while(curr < raw_tokens.size()) {
            
            boolean success = false;
            new Monad<>(success, false)
                .bind(this::pop_file)
                .bind(this::push_file)
                .bind(this::inc_newline)
                .bind(this::v_func_decl)
                .bind(this::v_decl)
                .bind(this::v_func_call)
                .bind(this::v_unary_operator)
                .bind(this::v_binary_operator)
                .bind(this::skip_some_tokens)
                .bind(this::unexpected_token)
                .unwrap();

        }

    }

    private String peek(int offset) {
        return raw_tokens.get(curr + offset);
    }
    private String next() {
        return raw_tokens.get(curr ++);
    }
    private void skip(int count) {
        curr += count;
    }
    private boolean expect(int count) {
        return curr + count < raw_tokens.size() && curr + count >= 0;
    }

    private boolean push_file() {
        if(!peek(0).equals("$")) return false;
        next();
        if(peek(0).equals("push")) {
            next();
            error_stack.push(new Error(Error.Type.COMP, next().substring(1, peek(-1).length() - 1)));
            return true;
        }
        return false;
    }
    private boolean pop_file() {
        if (peek(0).equals("EOF")) {
            next();
            error_stack.pop();
            return true;
        }
        return false;
    }
    private boolean inc_newline() {
        if(peek(0).equals("\n")) {
            next();
            error_stack.peek().line ++;
            return true;
        }
        else return false;
    }

    private static final String[] tokens_to_skip = { "}", ";", "," };
    private boolean skip_some_tokens() {
        if(Util.array_contains(tokens_to_skip, peek(0))) {
            next();
            return true;
        }
        return false;
    }
    private boolean unexpected_token() {
        assertf(false, "Unexpected token", "Found an unexpected token: '%s'!", peek(0));
        return true;
    }
    // v_ -- validate_
    private boolean v_func_decl() throws Exception {
        if(!peek(0).equals("func")) return false;
        next();
        String name = peek(0);

        validate_identifier(next(), "name of function: '%s'!", name);
        assertf(next().equals("("),             "Missing parenthesis",  "Missing '(' in function: '%s' declaration!", name);
        assertf(has_closing_paren('(', ')'),    "Missing parenthesis",  "missing ')' in function: '%s' declaration!", name);
        while(!peek(0).equals(")")) {
            v_decl();
            if(peek(0).equals(",")) next();
        }
        next();
        if(!peek(0).equals("{")) validate_identifier(peek(0), "return type: '%s'!", next());
        assertf(next().equals("{"),             "Missing curly bracket", "Missing block i.e.: '{ ... }' after function: '%s' declaration!", name);
        assertf(has_closing_paren('{', '}'),    "Missing curly bracket", "Missing '}' after function: '%s' declaration!", name);
        return true;
    }

    private boolean v_decl() throws Exception {
        if(!peek(1).equals(":") && !peek(1).equals("=")) return false;
        String name = next();

        validate_identifier(name, "name of variable: '%s'!", name);

        if(peek(0).equals(":")) {
            validate_identifier(peek(1), "type: '%s'", peek(1));
            next(); // kind of out of order, skips ':' when should skip '<type>'
            next();
        }

        if(peek(0).equals("=")) {
            next();
            v_expr();
        }

        return true;
    }

    private boolean v_expr() throws Exception {
        boolean success = false;
        // Monad but only like const, function call, variable & stuff
        success = v_const();
        new Monad<>(success, false)
            .bind(this::v_const)
            .bind(this::v_func_decl)
            .bind(this::v_unary_operator)
            .bind(this::v_binary_operator)
            .bind(this::skip_some_tokens)
            .bind(this::unexpected_token)
            .unwrap();

        return success;
    }

    private boolean v_const() {
        expect(1);

        char c = peek(0).charAt(0);

        if(Character.isDigit(c)) {
            if(peek(1).equals(".")) {
                expect(3);
                assertf(Character.isDigit(peek(2).charAt(0)), "Missing decimal!", "Floating point numbers cannot be written as: '123.' or '.123', you must use: '123.0'!");
                char decimal_problem = Validator.find_bad_char_in_number(peek(2));
                assertf(decimal_problem == 0, "Invalid number", "Found bad symbol: '%c' in the decimal place of: '%s'", decimal_problem, peek(0) + peek(1) + peek(2));
                skip(3);
            } else {
                char int_problem = Validator.find_bad_char_in_number(peek(0));
                assertf(int_problem == 0, "Invalid number", "Found bad symbol: '%c' in the decimal place of: '%s'", int_problem, peek(0));
                next();
            }
            return true;
        }

        if(c == '"' || c == '\'') {
            next();
            return true; // I don't know what to assert here...
        }

        if(peek(0).equals("true") || peek(0).equals("false")) {
            next();
            return true;
        }

        return false;
    }

    private boolean v_func_call() throws Exception {
        if(!peek(1).equals("(")) return false;
        String name = next();
        validate_identifier(name, "function: '%s' call", name);
        next(); // skips '('
        while(!peek(0).equals(")")) {
            if(peek(0).equals(",")) next();
            v_expr();
        }
        next(); // skips ')'
        return true;
    }

    private boolean v_unary_operator() throws Exception {
        if(!Util.array_contains(SyntaxDefinitions.unary_operators, peek(0))) return false;
        next();
        v_expr();
        return true;
    }

    private boolean v_binary_operator() throws Exception {
        if(!Util.array_contains(SyntaxDefinitions.binary_operators, peek(0))) return false;
        assertf(expect(-1), "Missing expression", "Binary operator: '%s' is missing it's left hand side expression, since it is at the very start of a file!", peek(0));
        next();
        v_expr();
        return true;
    }

    private boolean v_if() throws Exception {
        // if(!peek(0).equals("if")) return false;
        return false;
    }

    // TODO: 'in_where' is a bad name
    /** @param in_where is meant to be a formatted string finishing the sentence:
    *   <b>"Found bad character: '%c' in the "</b> with, e.g.: "name of a variable: '%s'"  */
    private void validate_identifier(String token, String in_where, String identifier) {
        char bad_char = Validator.find_bad_char_in_name(token);
        assertf(bad_char == 0, "Invalid identifier", "Found bad character: '%c' in the " + in_where, bad_char, identifier);

    }

    private boolean has_closing_paren(char opening, char closing) {
        int level = 1;
        for(int i = 0; i + curr < raw_tokens.size(); i ++) {
            if(peek(i).charAt(0) == opening) level ++;
            else if(peek(i).charAt(0) == closing) level --;
        }
        if(level > 0) return false;
        return true;
    }
    // I should validate parens with Stack<Character> or whatever like that, e.g.: [ '(', '{' ] + '}' -> [ '(' ]; [ '(', '{' ] + ')' -> error: mismatched parens

    // I dislike this kind of aliasing, but this is such a common function and there's 'error_stack.peek().' of noise before every call to it...
    private void assertf(boolean expr, String title, String format, Object... args) {
        error_stack.peek().assertf(expr, title, format, args);
    }
}
