import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ParserException extends Exception {
    String message, file;
    int line;

    public ParserException(String message, String filename, int lineNumber) {
        this.message = message;
        file = filename;
        line = lineNumber;
    }

    @Override
    public String getMessage() {
        return String.format("[PARSER %s:%d] %s", file, line, message);
    }
}


public class Parser {
    // t -- current token

    ParserException potentialException = new ParserException("", "", 0);
    private static class ResultDEPRECATED {
        Ast ast;
        int skip;
        boolean ok = false;

        ResultDEPRECATED() { }
        ResultDEPRECATED(Ast ast, int skip) { ok = true; this.ast = ast; this.skip = skip; }
    }

    private Declarations decl = new Declarations();
    private ArrayList<Token> tokens;
    private int current_token = 0;

    public Parser(ArrayList<Token> tokens, String filename) {
        this.tokens = tokens;
        potentialException.file = filename;
    }

    public Ast parse() throws ParserException {
        Ast.Root root = new Ast.Root();
        Ast node = root;
        int to_skip = 0;

        while (current_token < tokens.size()) {

            if(peek(0).is("\n")) potentialException.line ++;
            if(peek(0).type == Lexer.Type.INFORMATIONAL) {
                if(next() == null) break;
                continue;
            }

            // This would be so much better, if Java had out params...
            // Result res; // sizeof Result should be around 16 bytes, so it's fine to keep creating it

            // node = parse_binary_op(-1);
            // if(node != null) root.children.add(node);
            // node = parse_decl();
            node = parse_func_decl();
            if(node != null) {
                root.children.add(node);
                continue;
            }
            node = parse_decl();
            if(node != null) {
                root.children.add(node);
                continue;
            }
            node = parse_for();
            if(node != null) {
                root.children.add(node);
                continue;
            }
            node = parse_if();
            if(node != null) {
                root.children.add(node);
                continue;
            }
            node = parse_func();
            if(node != null)
                root.children.add(node);

            current_token ++; // shouldn't need this eventually...
        }

        return root;
    }

    private Token next() {
        // if(current_token >= tokens.size()) return null;
        return tokens.get(current_token ++);
    }

    private Token peek(int o) {
        // if(current_token + o >= tokens.size()) return null;
        return tokens.get(current_token + o);
    }

    private boolean has(int count) {
        return current_token + count < tokens.size();
    }

    private int paren_level = 0;
    private Ast parse_expr() throws ParserException {
        Ast node = null;
        if(peek(0).is("(")) {
            next();
            node = parse_binary_op(-1);
            next();
            if(node != null) return node;
        }

        if(peek(0).type == Lexer.Type.INFORMATIONAL) {
            next();
            return null;
        }

        try {
            node = new Monad<>(node, null)
                .bind(this::parse_access)
                .bind(this::parse_for)
                .bind(this::parse_if)
                .bind(this::parse_return)
                .bind(this::parse_decl)
                .bind(this::parse_assign)
                .bind(this::parse_func)
                .bind(this::parse_const)
                .bind(this::parse_unary_op)
                .bind(this::parse_array)
                .bind(this::parse_var)
                .unwrap();
        } catch(ParserException e) {
            throw e;
        } catch(Exception ignored) { ignored.printStackTrace(); }

        return node;
    }

    private Ast.Decl parse_decl() throws ParserException {
        if(peek(0).type != Lexer.Type.VARIABLE) return null;
        if(current_token + 1 >= tokens.size()) return null;
        if(peek(1).type != Lexer.Type.TYPE) return null;
        Ast.Decl node = new Ast.Decl();

        node.name = next().token;

        if(peek(0).token.equals("array")) {
            next(); next(); // Yes, currently, arrays are heterogeneous, this is mainly for the potential compiler
            node.type = List.class;
        } else if (peek(0).token.equals("map")) {
            next(); next(); next();
            node.type = Map.class;
        } else {
            node.type = decl.types.get(next().token);
        }
        assertf(node.type != null, "Invalid type: '%s' found!", peek(0).token);

        if(!peek(0).is("=")) return node;
        next(); // skips '='

        if(peek(1).type == Lexer.Type.TYPE) return node; // Oh! So this is why we have commas...


        Ast expr = parse_binary_op(-1);
        assertf(expr != null, "Failed to parse expression '%s' in variable assignment!", peek(0).token);

        node.value = expr;

        return node;
    }

    private Ast.Assign parse_assign() throws ParserException {
        if(peek(0).type != Lexer.Type.VARIABLE) return null;
        if(current_token + 1 >= tokens.size()) return null;
        if(!peek(1).is("=")) return null;
        Ast.Assign node = new Ast.Assign();

        node.name = next().token;
        next();
        node.value = parse_binary_op(-1);

        return node;

    }

    private Ast.Var parse_var() throws ParserException {
        // TODO: I guess, check if declared & (maybe not a keyword)
        Ast.Var var = new Ast.Var();
        var.name = next().token;
        return var;
    }

    // Future: private Ast parse_array() throws ParserException { }

    private Ast.Const parse_const() {
        if(peek(0).type != Lexer.Type.CONSTANT) return null;

        String t = next().token;
        Ast.Const c = new Ast.Const();

        if(Character.isDigit(t.charAt(0)))
            c.value = Double.parseDouble(t);
        else if(t.startsWith("'") || t.startsWith("\""))
            c.value = t.substring(1, t.length() - 1);
        else // if(t.equals("true") || t.equals("false"))
            c.value = t.equals("true");
        return c;
    }

    private Ast.UnaOp parse_unary_op() throws ParserException {
        if(peek(0).type != Lexer.Type.UNARY_OPERATOR) return null;
        Ast.UnaOp node = new Ast.UnaOp();

        node.name = next().token;
        Ast expr = parse_expr();
        assertf(expr != null, "Expression after the unary operator: '%s' is invalid", peek(0).token);

        node.rhs = expr;
        return node;
    }

    // J. Blow's/Pratt algorithm.
    // From my understanding, the 2 solutions are, basically, the same and Pratt was much earlier,
    // but I took this from: youtube.com/watch?v=fIPO4G42wYE
    private Ast parse_binary_op(int min_prec) throws ParserException {
        Ast left = parse_expr();

        if(left == null) return null;

        while(true) {
            Ast node = parse_increasing_precedence(left, min_prec);
            if(node == left) break; // I should go back to .equals()
            left = node;
        }
        return left;
    }

    private Ast parse_increasing_precedence(Ast left, int min_prec) throws ParserException {
        // skip_parens();
        Token next = peek(0);
        Ast.BinOp node = new Ast.BinOp();

        if(next == null || next.type != Lexer.Type.BINARY_OPERATOR) return left;

        int prec = Lexer.get_precedence(next);
        if(min_prec >= prec) return left;

        next();

        node.lhs = left;
        node.name = next.token;
        node.rhs = parse_binary_op(prec);

        return node;
    }

    private Ast.Func parse_func() throws ParserException {
        if(peek(0).type != Lexer.Type.FUNCTION_CALL) return null;
        Ast.Func node = new Ast.Func();
        node.name = next().token;
        ArrayList<Ast> args = new ArrayList<>();
        next(); // skips '('
        while(!peek(0).is(")") || paren_level > 0) {
            if(peek(0).is(",")) next();
            args.add(parse_binary_op(-1));
        }
        next(); // skips ')'
        node.args = new Ast[args.size()];
        args.toArray(node.args);
        return node;
    }

    private Ast.FnDecl parse_func_decl() throws ParserException {
        if(peek(0).type != Lexer.Type.FUNCTION_DECL) return null;
        Ast.FnDecl node = new Ast.FnDecl();
        node.name = next().token;
        node.args = new ArrayList<>();
        node.body = new ArrayList<>();

        next(); // skips '('
        while(!peek(0).is(")")) {
            Ast.Decl arg = parse_decl();
            assertf(arg != null, "Failed to parse a function parameter: %s", peek(0).token);
            node.args.add(arg);
        }
        next(); // skips ')'

        // TODO: Read the return type
        if(peek(0).type == Lexer.Type.TYPE) {
            node.ret = decl.types.get(next().token);
        }

        node.body = parse_block();
        return node;
    }

    private Ast.For parse_for() throws ParserException {
        if(!peek(0).is("for")) return null;
        next();
        Ast.For node = new Ast.For();
        node.body = new ArrayList<>();

        node.pre = parse_expr();
        if(peek(0).is(";")) {
            next();
            node.cond = parse_binary_op(-1);
        }
        if(peek(0).is(";")) {
            next();
            node.post = parse_expr();
        }

        // Think of while(!glfwWindowShouldClose(window)) { ... }, here you ~~could~~ do: for !glfwWindowShowClose(window) { ... }
        // Very much taken from Odin
        if(node.pre != null && node.cond == null && node.post == null) node.cond = node.pre;

        node.body = parse_block();

        return node;
    }

    private Ast.If parse_if() throws ParserException {
        if(!peek(0).is("if")) return null;
        next();
        Ast.If node = new Ast.If();
        node.cond = parse_binary_op(-1);
        node.body = parse_block();
        return node;
    }

    private Ast.Ret parse_return() throws  ParserException {
        if(!peek(0).is("return")) return null;
        next();
        Ast.Ret node = new Ast.Ret();
        node.expr = parse_binary_op(-1);
        return node;
    }

    private List<Ast> parse_block() throws ParserException {
        List<Ast> block = new ArrayList<>();
        Token start = next();
        while(current_token < tokens.size() && !peek(0).is("}")) {
            Ast node = parse_expr();
            if(node != null) block.add(node);
            if(has(1) && start.is("do") && (peek(0).type == Lexer.Type.INFORMATIONAL || peek(0).is(";"))) break;
        }
        if(start.is("{") && has(1)) next(); // skips '}'
        return block;
    }

    // This should be a binary operator, but I currently do not have logic for consuming ']'
    private Ast.Key parse_access() throws ParserException {
        if(peek(1).type != Lexer.Type.CONTAINER_INDEX) return null;
        Ast.Key node = new Ast.Key();
        node.array = parse_var();
        next(); // skips '['
        node.index = parse_binary_op(-1);
        next(); // skips ']'
        return node;
    }

    private Ast.Array parse_array() throws ParserException {
        if(peek(0).type != Lexer.Type.CONTAINER) return null;
        Ast.Array array = new Ast.Array();
        array.values = new ArrayList<>();
        next(); /// skips '['
        while(!peek(0).is("]")) {
            if(peek(0).is(",")) next();
            array.values.add(parse_binary_op(-1));
        }
        next(); // skips ']'
        return array;
    }


    private void assertf(boolean expr, String format, Object... values) throws ParserException {
        if(expr) return;
        potentialException.message = String.format(format, values) + "\n";
        throw potentialException;
    }
}
