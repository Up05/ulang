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

    private static final String[] tokens_to_skip = { "}", "]", ")", ";", ",", "return" };
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
            .bind(this::v_typedef)
            .bind(this::v_const)
            .bind(this::v_var)
            .bind(this::skip_some_tokens)
            .bind(this::unexpected_token)
            .unwrap();

        if(are_there(1)) v_access();
        if(are_there(1)) v_binary_operator();
    }

    private boolean v_func_decl() throws Exception {
        if(!peek(0).equals("func")) return false;
        next();
        String name = peek(0);

        validate_identifier(next(), "name of function: '%s'!", name);
        assertf(next().equals("("),             "Missing parenthesis",  "Missing '(' in function: '%s' declaration!", name);
        assertf(has_closing_paren('(', ')'),    "Missing parenthesis",  "missing ')' in function: '%s' declaration!", name);
        int tries = 0;
        while(!peek(0).equals(")")) {
            v_decl();

            // super out of the blue, but I encountered it, so chances are, others will too...
            // either way, this class is meant as more of a "scripted" thing to catch all the human errors
            // (as well as to not need to reconfirm assumptions elsewhere)
            assertf(!peek(0).equals("]"), "Brackets on wrong side of type", "The '%s []' should be '[] %s' in function: '%s' declaration", peek(-2), peek(-2), name);
            v_not_hung(tries, "the parameters of function " + name + " declaration");
            if(peek(0).equals(",")) next();
            tries ++;
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
            // next(); // Why was this here???
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
            // .bind(this::unexpected_token)
            .unwrap();


        if(are_there(1)) v_access();
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
        }
        if(peek(0).equals("[")) v_type();
        else validate_identifier(peek(0), "type: '%s'", next());
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
        int tries = 0;
        while(!peek(0).equals(")")) {
            if(peek(0).equals(",")) next();
            v_expr();
            v_not_hung(tries ++, "arguments of function call");
        }
        next(); // skips ')'
        return true;
    }

    private boolean v_unary_operator() throws Exception {
        int unary = matching_operator_chars(SyntaxDefinitions.unary_operators);
        if(unary == 0) return false;
        skip(unary + 1);
        v_expr();
        return true;
    }

    private void v_binary_operator() throws Exception {
        int binary = matching_operator_chars(SyntaxDefinitions.binary_operators);
        if(binary == 0) return;
        assertf(are_there(-1), "Missing expression", "Binary operator: '%s' is missing it's left hand side expression, since it is at the very start of a file!", peek(0));
        skip(binary);
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

        if(!peek(0).equals("{")) {
            if(peek(0).equals(";")) {
                v_for_validate_the_rest();
            } else {
                v_stmt();
                if(!peek(0).equals("{"))
                    v_for_validate_the_rest();
            }
        }

        v_block();
        return true;
    }

    // You cannot have private methods in java, so...
    private void v_for_validate_the_rest() throws Exception {

        assertf(next().equals(";"), "Missing semicolon", "for loop must have either a semicolon, or a block after the first statement, but it has: '%s'", peek(-1));

        if(!peek(0).equals(";")) v_expr();

        assertf(next().equals(";"), "Missing semicolon", "for loop must have either 0 or 2, but it has 1!");

        if(!peek(0).equals("{"))
            v_stmt();
    }

    private void v_access() throws Exception {
        if(!peek(0).equals("[")) return;
        next();
        v_expr();
        next();
        if(peek(0).equals("=")) next();
    }

    private boolean v_typedef() throws Exception {
        if(!peek(0).equals("type")) return false;
        next();
        String typename = next();
        validate_identifier(typename, "'type' definition statement", typename);
        assertf(next().equals("="), "Expected '='", "Expected '=' after 'type %s'", typename);
        assertf(next().equals("foreign"), "Only 'foreign' allowed", "Structures are NYI in the language. You must declare types by using: 'type %s = foreign java.path.Class'", typename);
        assertf(peek(0).charAt(0) == '"' || peek(0).charAt(0) == '\'',
            "'type' definition path not in quotes", "When specifying a path in 'type' statement, please put the path in quotes, since you can do e.g.: '[[[I' to get 'int[][][]'");
        return true;
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

    /** @param in_where is meant to be a formatted string finishing the sentence:
    *   <b>"Found bad character: '%c' in the "</b> with, e.g.: "name of a variable: '%s'"  */

    private void validate_identifier(String token, String in_where, String identifier) {
        char bad_char = Validator.find_bad_char_in_name(token);
        assertf(bad_char == 0, "Invalid identifier", "Found bad character: '%c' in the " + in_where, bad_char, identifier);

        for(String keyword : SyntaxDefinitions.reserved) {
            assertf(!token.equals(keyword), "Reserved identifier", "Found reserved identifier: '%s' in the " + in_where + "\nThis word may be used in the language in the future.", identifier, keyword);
        }
    }

    // I did not copy her, it’s not true! It’s bullsh*t! I did not copy her! I did not!
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

    private String tokens_around_curr(int count) {
        StringBuilder b = new StringBuilder();
        for(int i = count; i > 0; i --) b.append(peek(-i));
        for(int i = 0; i < count; i ++) b.append(peek(i));
        return b.toString();
    }

    private void v_not_hung(int tries, String in_where) {
        assertf(tries < 32767, "Token validator hung...", // just a nice number.
            """
            There is, likely, an unexpected symbol in %s!
            Example: 'func func_name(arg1 : type1, array : [] type2) { ...'.
            The program is stuck on token: '%s', around '%s' code.
            """, in_where, peek(0), tokens_around_curr(2)
        );
    }

    // I dislike this kind of aliasing, but this is such a common function and there's 'error_stack.peek().' of noise before every call to it...
    private void assertf(boolean expr, String title, String format, Object... args) {
        error_stack.peek().assertf(expr, title, format, args);
    }
}
