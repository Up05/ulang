import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class TokenValidator extends Stage<String> {

    Stack<Error> error_stack = new Stack<>();

    public TokenValidator(ArrayList<String> raw_tokens, String filename) {
        this.tokens = raw_tokens;
        error_stack.push(new Error(Error.Type.COMP, filename));
    }

    public void validate() throws Exception {
        while(curr < tokens.size())
            v_stmt();
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
        if(peek(0).equals("EOF")) {
            next();
            if(!error_stack.isEmpty()) error_stack.pop();
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

    private static final String[] tokens_to_skip = { "}", "]", ")", ";", "," };
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
    private void v_stmt() throws Exception {
        boolean success = false;
        new Monad<>(success, false)
            .bind(this::pop_file)
            .bind(this::push_file)
            .bind(this::inc_newline)
            .bind(this::v_func_decl)
            .bind(this::v_if)
            .bind(this::v_for)
            .bind(this::v_decl)
            .bind(this::v_func_call)
            .bind(this::v_unary_operator)
            .bind(this::v_const)
            .bind(this::v_var)
            .bind(this::skip_some_tokens)
            .bind(this::unexpected_token)
            .unwrap();

        if(are_there(1)) v_binary_operator();
    }

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
        if(!peek(0).equals("{")) v_type();
        assertf(next().equals("{"),             "Missing curly bracket", "Missing block i.e.: '{ ... }' after function: '%s' declaration!", name);
        assertf(has_closing_paren('{', '}'),    "Missing curly bracket", "Missing '}' after function: '%s' declaration!", name);
        return true;
    }

    private Set<String> declared_vars = new HashSet<>();
    private boolean v_decl() throws Exception {
        if(!peek(1).equals(":") && !peek(1).equals("=")) return false;
        String name = next();

        validate_identifier(name, "name of variable: '%s'!", name);

        if(peek(0).equals(":")) {
            declared_vars.add(name);
            next();
            v_type();
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
        new Monad<>(success, false)
            .bind(this::v_const)
            .bind(this::v_func_call)
            .bind(this::v_unary_operator)
            .bind(this::v_var)
            .bind(this::skip_some_tokens)
            .bind(this::unexpected_token)
            .unwrap();

        if(are_there(1)) v_binary_operator();
        return success;
    }

    private boolean v_const() {
        are_there(1);

        char c = peek(0).charAt(0);

        if(Character.isDigit(c)) {
            if(peek(1).equals(".")) {
                are_there(3);
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

        if(peek(0).equals("[")) {
            next();
            assertf(has_closing_paren('[', ']'), "Missing ']'", "Found an array index/initialization, that is missing: ']'!");
            return true;
        }

        return false;
    }

    private boolean v_type() {
        if(peek(0).equals("[")) {
            next();
            if(peek(0).equals("map")) {
                System.out.println("[WARNING] maps are NYI! Do not use [map] <type> <type> for now.");
                next();
            }
            assertf(next().equals("]"), "Missing ']'", "Array type syntax needs both '[' and ']'. Missing: ']'!");
            return true;
        }
        validate_identifier(peek(0), "type: '%s'", next());
        return true;
    }

    private boolean v_var() {
        if(declared_vars.contains(peek(0))) {
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

    private void v_binary_operator() throws Exception {
        if(!Util.array_contains(SyntaxDefinitions.binary_operators, peek(0))) return;
        assertf(are_there(-1), "Missing expression", "Binary operator: '%s' is missing it's left hand side expression, since it is at the very start of a file!", peek(0));
        next();
        v_expr();
    }

    private boolean v_if() throws Exception {
         if(!peek(0).equals("if")) return false;
         next();
         v_expr();
         if(peek(0).equals("do")) {
             next();
             return true;
         }

         assertf(next().equals("{"), "Missing block", "if statement is missing a block after it. You may have forgotten: 'do'. E.g.: 'if true do print(\"test\")'");
         assertf(has_closing_paren('{', '}'), "Missing curly bracket", "if statement is missing a curly bracket('}')");

        return true;
    }

    private boolean v_for() throws Exception {
        if(!peek(0).equals("for")) return false;
        next();

        if(!peek(0).equals(";")) {
            v_stmt();
            if(!peek(0).equals("{")) v_for_validate_the_rest();
        } else v_for_validate_the_rest();

        v_block();

        return true;
    }

    // You cannot have private methods in java, so...
    private void v_for_validate_the_rest() throws Exception {

        assertf(next().equals(";"), "Missing semicolon", "for loop must have either a semicolon, or a block after the first statement, but it has: '%s'", peek(-1));

        if(!peek(0).equals(";")) v_expr();

        assertf(next().equals(";"), "Missing semicolon", "for loop must have either 0 or 2, but it has 1!");
        if(!peek(0).equals("{")) v_stmt();
    }

    private boolean v_block() {
        if(peek(0).equals("do")) {
            next();
            return true;
        }
        assertf(peek(0).equals("{"), "Missing curly bracket", "Expected '{' or 'do', instead found '%s'!", peek(0));
        next();
        assertf(has_closing_paren('{', '}'), "Missing curly bracket", "A block is missing it's closing curly bracket ('}')!");
        return true;
    }

    // TODO: 'in_where' is a bad name
    /** @param in_where is meant to be a formatted string finishing the sentence:
    *   <b>"Found bad character: '%c' in the "</b> with, e.g.: "name of a variable: '%s'"  */
    private void validate_identifier(String token, String in_where, String identifier) {
        char bad_char = Validator.find_bad_char_in_name(token);
        assertf(bad_char == 0, "Invalid identifier", "Found bad character: '%c' in the " + in_where, bad_char, identifier);

        for(String keyword : SyntaxDefinitions.reserved) {
            assertf(!token.equals(keyword), "Reserved identifier", "Found reserved identifier: '%s' in the " + in_where + "\nThis word may be used in the language in the future.", identifier, keyword);
        }
    }

    private boolean has_closing_paren(char opening, char closing) {
        int level = 1;
        for(int i = 0; i + curr < tokens.size(); i ++) {
            // if(are_there(0)) return false;
            if(level == 0) break;
            if(peek(i).charAt(0) == opening) level ++;
            else if(peek(i).charAt(0) == closing) level --;
        }
        return level <= 0;
    }
    // I should validate parens with Stack<Character> or whatever like that, e.g.: [ '(', '{' ] + '}' -> [ '(' ]; [ '(', '{' ] + ')' -> error: mismatched parens

    // I dislike this kind of aliasing, but this is such a common function and there's 'error_stack.peek().' of noise before every call to it...
    private void assertf(boolean expr, String title, String format, Object... args) {
        error_stack.peek().assertf(expr, title, format, args);
    }
}
