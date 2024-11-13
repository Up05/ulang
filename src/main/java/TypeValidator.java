import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;

public class TypeValidator {
    Stack<HashMap<String, String>> scopes;
    HashMap<String, String[]> func_ret_and_args; // I will want to make scoped inner functions. // Also, yes, this sucks

    public TypeValidator() {
        scopes = new Stack<>();
        func_ret_and_args = new HashMap<>();

        scopes.push(new HashMap<>());
    }

    public String validate(Ast ast) {
        switch(ast) {
        case Ast.Root node -> { // I really want to move the Root down to the very bottom :(
            for(Ast child : node.children) validate(child);
        }
        case Ast.Decl node -> {
            for(HashMap<String, String> scope : scopes)
                node.error.assertf(!scope.containsKey(node.name), "Redeclaration", "Variable '%s' has already been declared", node.name);

            scopes.peek().put(node.name, node.typename);
            if(node.value != null)
                assertf_type(node.error, node.typename, node.value, "declaration of " + node.name);
        }
        case Ast.Var node -> {
            return fetch_var_type(node.name);
        }
        case Ast.Const node -> {
            return node.typename;
        }
        case Ast.FnDecl node -> {
            scopes.push(new HashMap<>());
            String[] args = new String[node.args.size() + 1];
            args[0] = node.ret_typename;
            for(int i = 0; i < node.args.size(); i ++ ) {
                args[i + 1] = node.args.get(i).typename;
                validate(node.args.get(i));
            }
            func_ret_and_args.put(node.name, args);

            for(Ast child : node.body) {
                if(child instanceof Ast.Ret ret_node)
                    assertf_type(ret_node.error, node.ret_typename, ret_node.expr, "the return statement of " + node.name);
                else validate(child);
            }
            scopes.pop();
        }
        case Ast.Func node -> {
            String[] args = func_ret_and_args.get(node.name);
            if(args == null)
                return Builtin.RETURN_TYPES.get(node.name);
            int is_var_args = args.length > 1 && args[args.length - 1].equals(SyntaxDefinitions.TYPE_VARARGS) ? 1 : 0;
            node.error.assertf(node.args.length >= args.length - is_var_args - 1, "Missing parameter",
             "Missing parameter(s) in function %s%s", node.name, Debug.stringify_param_types(Arrays.copyOfRange(args, 1, args.length)));
            if(args.length > 1)
                for(int i = 1; i < args.length; i ++) {
                    if(args[i].equals(SyntaxDefinitions.TYPE_VARARGS)) break;
                    assertf_type(node.error, args[i], node.args[i - 1],
                        "parameter #" + (i + 1) + " of function " + node.name);
                }
            return args[0];
        }
        case Ast.UnaOp node -> {
            SyntaxDefinitions.OperatorTypeData types = SyntaxDefinitions.unary_types.get(node.name);
            assertf_type(node.error, types.rhs, node.rhs, "unary operator: '" + node.name + "'");
            return types.out;
        }
        case Ast.BinOp node -> {
            SyntaxDefinitions.OperatorTypeData types = SyntaxDefinitions.binary_types.get(node.name);
            assertf_type(node.error, types.lhs, node.lhs, " left of binary operator: '" + node.name + "'"); // oopsie poopsie
            assertf_type(node.error, types.rhs, node.rhs, "right of binary operator: '" + node.name + "'");
            return types.out;
        }
        case Ast.Array node -> {
            for(Ast value : node.values) {
                assertf_type(value.error, node.typename.substring(3), value, "array element");
            }
            return node.typename;
        }
        case Ast.Key node -> {
            assertf_type(node.error, SyntaxDefinitions.TYPE_NUMBER, node.index, "array index");
            return validate(node.array).substring(3);
        }
        case Ast.If node -> {
            assertf_type(node.error, SyntaxDefinitions.TYPE_BOOLEAN, node.cond, "'if' statement's condition");
            for(Ast child : node.body) validate(child);

        }
        case Ast.For node -> {
            if(node.pre  != null) validate(node.pre);
            if(node.post != null) validate(node.post);
            if(node.pre != null || node.cond != null || node.post != null)
                assertf_type(node.error, SyntaxDefinitions.TYPE_BOOLEAN, node.cond, "'for' statement's middle statement");

            for(Ast child : node.body) validate(child);
        }
        default -> {}
        // Ast.Ret is validated inside Ast.FnDecl. Why would anyone put it outside a function?
        }

        return "";
    }

    private String fetch_var_type(String name) {
        for(HashMap<String, String> scope : scopes) {
            if(scope.containsKey(name))
                return scope.get(name);
        }
        return "";
    }

    private void assertf_type(Error error, String type, Ast expr, String in_where) {

        String type2 = validate(expr);
        if(type == null || type2 == null) {
            System.out.printf("Skipped type checking in '%s' on line %d. In %s\n", error.file, error.line, in_where);
        } else if(!types_match(type, type2)) {
            error.assertf(type.equals(type2), Error.Type.TYPE.name, "Mismatched types: expected '%s', got '%s' in %s", type, type2, in_where);
        }
    }

    private boolean types_match(String a, String b) {
        if(a.equals(SyntaxDefinitions.TYPE_ANY) || b.equals(SyntaxDefinitions.TYPE_ANY)) return true;
        if(a.startsWith("[] ") || b.startsWith("[] "))
            if(a.endsWith("any") || b.endsWith("any")) return true;
        // I hate this... But just because of 'SyntaxDefinitions'
        if(a.equals(SyntaxDefinitions.TYPE_NUMBER) || a.equals(SyntaxDefinitions.TYPE_BOOLEAN) || a.equals(SyntaxDefinitions.TYPE_CHAR))
            if(SyntaxDefinitions.types.get(b).isPrimitive()) return true;
        if(b.equals(SyntaxDefinitions.TYPE_NUMBER) || b.equals(SyntaxDefinitions.TYPE_BOOLEAN) || b.equals(SyntaxDefinitions.TYPE_CHAR))
            if(SyntaxDefinitions.types.get(a).isPrimitive()) return true;
        return a.equals(b);
    }

    public static int get_precedence(Token t) {
        String[][] table_ref = null;
        if(t.type == Lexer.Type.UNARY_OPERATOR) table_ref = SyntaxDefinitions.unary_precedence_table;
        else if(t.type == Lexer.Type.BINARY_OPERATOR) table_ref = SyntaxDefinitions.binary_precedence_table;
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
