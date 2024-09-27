import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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


public class Parser extends Stage<Token> {
    // t -- current token

    ParserException potentialException = new ParserException("", "", 0);

    Stack<Error> error_stack = new Stack<>();
    // I am fighting Java here. But the alternative is: polluting Ast.java with constructors.
    static <T extends Ast> T make(Class<T> type, Error error) {
        try {
            T ast = type.getDeclaredConstructor().newInstance();
            ast.error = error;
            return ast;
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
            return null;
        }
    }

    Token next() {
        Token t = super.next();

        if(are_there(0)) {
            if(peek(0).type == Lexer.Type.INSERTED_FILE) {
                error_stack.push(new Error(Error.Type.TYPE, peek(0).token));
            } else if(peek(0).is("EOF")) {
                error_stack.pop();
            } else if(peek(0).is("\n")) {
                error_stack.peek().line ++;
            }
        }

        return t;
    }

    public Parser(ArrayList<Token> tokens, String filename) {
        this.tokens = tokens;
        error_stack.push(new Error(Error.Type.TYPE, filename));
        potentialException.file = filename;
    }

    public Ast parse() throws Exception {
        Ast.Root root = make(Ast.Root.class, error_stack.peek());

        while(curr < tokens.size()) {

            if(peek(0).type == Lexer.Type.INFORMATIONAL) {
                if(next() == null) break;
                continue;
            }
            Ast node = null;
            node = new Monad<>(node, null) // This isn't actually "always null"
                .bind(this::parse_func_decl)
                .bind(this::parse_decl)
                .bind(this::parse_for)
                .bind(this::parse_if)
                .bind(this::parse_func)
                .unwrap();
            if(node != null) root.children.add(node);

            curr++; // shouldn't need this eventually...
        }

        return root;
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
        if(curr + 1 >= tokens.size()) return null;
        if(peek(1).type != Lexer.Type.TYPE) return null;
        Ast.Decl node = make(Ast.Decl.class, error_stack.peek());


        node.name = next().token;


        if(peek(0).token.equals("array")) {
            next(); // Technically, arrays are heterogeneous. I'll deal with that at validation & runtime
            node.type = List.class;
            node.typename = "[] " + next().token;
        } else if (peek(0).token.equals("map")) {
            // ! NYI
            next(); next(); next();
            node.type = Map.class;
        } else {
            node.type = SyntaxDefinitions.types.get(peek(0).token);
            node.typename = next().token;
        }
        assertf(node.type != null, "Invalid type: '%s' found!", peek(0).token);

        if(!peek(0).is("=")) return node;
        next(); // skips '='

        if(peek(1).type == Lexer.Type.TYPE) return node; // Oh! So this is why we have commas...


        Ast expr = parse_binary_op(-1);
        assertf(expr != null, "Failed to parse expression '%s' in variable assignment!", peek(0).token);

        if(node.typename.startsWith("[]"))
            ((Ast.Array) expr).typename = node.typename;

        node.value = expr;

        return node;
    }

    private Ast.Assign parse_assign() throws ParserException {
        if(peek(0).type != Lexer.Type.VARIABLE) return null;
        if(curr + 1 >= tokens.size()) return null;
        if(!peek(1).is("=")) return null;
        Ast.Assign node = make(Ast.Assign.class, error_stack.peek());

        node.name = next().token;
        next();
        node.value = parse_binary_op(-1);

        return node;

    }

    private Ast.Var parse_var() throws ParserException {
        // TODO: I guess, check if declared & (maybe not a keyword)
        Ast.Var var = make(Ast.Var.class, error_stack.peek());
        var.name = next().token;
        return var;
    }

    private Ast.Const parse_const() {
        if(peek(0).type != Lexer.Type.CONSTANT) return null;

        String t = next().token;
        Ast.Const node = make(Ast.Const.class, error_stack.peek());

        if(Character.isDigit(t.charAt(0))) {
            node.value = Double.parseDouble(t);
            node.typename = "num"; // this sucks
        } else if(t.startsWith("'") || t.startsWith("\"")) {
            node.value = t.substring(1, t.length() - 1);
            node.typename = "string";
        } else { // if(t.equals("true") || t.equals("false"))
            node.value = t.equals("true");
            node.typename = "bool";
        }
        return node;
    }

    private Ast.UnaOp parse_unary_op() throws ParserException {
        if(peek(0).type != Lexer.Type.UNARY_OPERATOR) return null;
        Ast.UnaOp node = make(Ast.UnaOp.class, error_stack.peek());

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
        Ast.BinOp node = make(Ast.BinOp.class, error_stack.peek());

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
        Ast.Func node = make(Ast.Func.class, error_stack.peek());
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
        Ast.FnDecl node = make(Ast.FnDecl.class, error_stack.peek());
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

        if(peek(0).type == Lexer.Type.TYPE) {
            node.ret = SyntaxDefinitions.types.get(peek(0).token);
            node.ret_typename = next().token;
        }

        node.body = parse_block();
        return node;
    }

    private Ast.For parse_for() throws ParserException {
        if(!peek(0).is("for")) return null;
        next();
        Ast.For node = make(Ast.For.class, error_stack.peek());
        node.body = new ArrayList<>();
        if(!peek(0).is("{") && !peek(0).is("do")) node.pre = parse_expr();
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
        if(node.cond == null && node.post == null) {
            if (node.pre != null) {
                node.cond = node.pre;
                node.pre = null;
            } else {
                Ast.Const bool = make(Ast.Const.class, error_stack.peek());
                bool.value = true;
                node.cond = bool;
            }
        }

        node.body = parse_block();

        return node;
    }

    private Ast.If parse_if() throws ParserException {
        if(!peek(0).is("if")) return null;
        next();
        Ast.If node = make(Ast.If.class, error_stack.peek());
        node.cond = parse_binary_op(-1);
        node.body = parse_block();
        return node;
    }

    private Ast.Ret parse_return() throws  ParserException {
        if(!peek(0).is("return")) return null;
        next();
        Ast.Ret node = make(Ast.Ret.class, error_stack.peek());
        node.expr = parse_binary_op(-1);
        return node;
    }

    private List<Ast> parse_block() throws ParserException {
        List<Ast> block = new ArrayList<>();
        Token start = next();
        while(curr < tokens.size() && !peek(0).is("}")) {
            Ast node = parse_expr();
            if(node != null) block.add(node);
            if(are_there(1) && start.is("do") && (peek(0).type == Lexer.Type.INFORMATIONAL || peek(0).is(";"))) break;
        }
        if(start.is("{") && are_there(1)) next(); // skips '}'
        return block;
    }

    // This should be a binary operator, but I currently do not have logic for consuming ']'
    private Ast.Key parse_access() throws ParserException {
        if(peek(1).type != Lexer.Type.CONTAINER_INDEX) return null;
        Ast.Key node = make(Ast.Key.class, error_stack.peek());
        node.array = parse_var();
        next(); // skips '['
        node.index = parse_binary_op(-1);
        next(); // skips ']'
        return node;
    }

    private Ast.Array parse_array() throws ParserException {
        if(peek(0).type != Lexer.Type.CONTAINER) return null;
        Ast.Array array = make(Ast.Array.class, error_stack.peek());
        array.values = new ArrayList<>();
        next(); /// skips '['
        while(!peek(0).is("]")) {
            if(peek(0).is(",")) next();
            array.values.add(parse_binary_op(-1));
        }
        next(); // skips ']'
        return array;
    }

    // TODO: Remove. But first, finish* TypeValidator
    private void assertf(boolean expr, String format, Object... values) throws ParserException {
        if(expr) return;
        potentialException.message = String.format(format, values) + "\n";
        throw potentialException;
    }
}
