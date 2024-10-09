import java.lang.reflect.Method;
import java.util.*;

public class Interpreter {
    Stack<HashMap<String, Object>> scopes;
    Ast.Root root;
    Builtin builtin = new Builtin();

    public Interpreter(Ast.Root root) {
        scopes = new Stack<>();
        scopes.push(new HashMap<>());
        this.root = root;
    }

    public void interpret(Ast ast) {
        if(ast == null) return;
        switch(ast) {
        case Ast.Root node -> {
            for(Ast child : node.children) interpret(child);
        }
        case Ast.Decl node -> declare_var(node);
        case Ast.Assign node -> assign(node);
        // Don't need to do anything here... Functions & Types truly should have their own ASTs...
        case Ast.FnDecl ignored -> { }
        case Ast.Func node -> call(node);
        // I only had this here because of the exec operator: `@`
        // But now I realize, that it is actually consistent for the user
        // They would not know if their variable or constant was read or ignored
        // If it is the whole statement and nothing was done with it.
        case Ast.BinOp node -> eval_binary_operator(node);
        case Ast.UnaOp node -> eval_unary_operator(node);
        case Ast.For node -> interpret_for(node);
        case Ast.If node -> interpret_if(node);

        case Ast.Var node -> eval(node); // Why is this here???
        // delete this eventually
        default -> throw new IllegalStateException("Unexpected value: " + ast);
        }
    }

    // To check where variable was declared in a different scope or not to check whether variable was declared in a different scope?
    private void declare_var(Ast.Decl node) {
        Object val = eval(node.value);
        temp_assertf(is_of_type(val, node.type), "Mismatched types! " + (val == null ? "null" : val.getClass().getSimpleName()) + " : " + node.type.getSimpleName());
        scopes.peek().put(node.name, val);
    }

    private void assign(Ast.Assign node) {
        for(HashMap<String, Object> scope : scopes) {
            if(scope.containsKey(node.name)) {
                scope.put(node.name, eval(node.value));
            }
        }
    }

    private Object eval(Ast ast) {
        if(ast == null) return null;
        switch(ast) {
        case Ast.Const node -> {
            return node.value;
        }
        case Ast.Var node -> {
            for(HashMap<String, Object> scope : scopes) { // I would love this to be reversed :(
                Object val = scope.get(node.name);
                if(val != null) return val;
            }
            temp_assertf(false, "Trying to get the value of an undeclared variable"); // This should not get past the Validator
        }
        case Ast.Func node -> {
            return call(node);
        }
        case Ast.UnaOp node -> {
            return eval_unary_operator(node);
        }
        case Ast.BinOp node -> {
            return eval_binary_operator(node);
        }
        case Ast.Array node -> {
            return eval_array(node);
        }
        case Ast.Key node -> {
            return index_into(node);
        }

        default -> throw new IllegalStateException("Unexpected value: " + ast);
        }
        return null;
    }

    private Object call(Ast.Func node) {

        Ast.FnDecl decl = null;
        for(Ast child : root.children) {
            if(child instanceof Ast.FnDecl decl_node)
                if(node.name.equals(decl_node.name))
                    decl = decl_node;
        }

        if(decl == null) {
            return call_builtin(node);
        }

        scopes.push(new HashMap<>());

        for(Ast.Decl arg : decl.args) {
            declare_var(arg);
        }

        for(Ast arg : node.args) {
            for(Ast.Decl decl_arg : decl.args) {
                Object v = eval(arg);
                scopes.peek().put(decl_arg.name, v);
            }
        }

        for(Ast child : decl.body) {
            if(child instanceof Ast.Ret ret_stmt)
                return eval(ret_stmt.expr); // type-checking, what's that?             <-- TODO
            interpret(child);
        }

        assert decl.ret == null;
        return null;
    }

    private Object call_builtin(Ast.Func node) {

        Method[] potential = Builtin.class.getDeclaredMethods();

        List<Object> args = new ArrayList<>();
        for(Ast arg : node.args) args.add(eval(arg));

        try {
            for (Method func : potential) {
                if (func.getName().equals(node.name))
                    return func.invoke(builtin, args);
            }
        } catch (Exception e) {
            System.out.println("No way!!!");
            e.printStackTrace();
        }
        return null;
    }

    private Object eval_unary_operator(Ast.UnaOp node) {
        Object a = eval(node.rhs);

        switch(node.name) {
        case "!": return !(Boolean) a;
        case "+": return a;
        case "-": return (a instanceof Long i) ? -i :- (Double) a;
        }
        return null;
    }

    private Object eval_binary_operator(Ast.BinOp node) {
        Object
            a = eval(node.lhs),
            b = eval(node.rhs);

        // I hate this so much more than anyone else ever could...
        switch(node.name) {
        case "&&": return (Boolean) a && (Boolean) b;
        case "||": return (Boolean) a || (Boolean) b;
        case "<" : return ((Number) a).doubleValue() < ((Number) b).doubleValue();
        case ">" : return ((Number) a).doubleValue() > ((Number) b).doubleValue();
        case "<=": return ((Number) a).doubleValue() <= ((Number) b).doubleValue();
        case ">=": return ((Number) a).doubleValue() >= ((Number) b).doubleValue();
        case "==": return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        case "!=": return ((Number) a).doubleValue() != ((Number) b).doubleValue();

        case "+": return ((Number) a).doubleValue() + ((Number) b).doubleValue();
        case "-": return ((Number) a).doubleValue() - ((Number) b).doubleValue();
        case "*": return ((Number) a).doubleValue() * ((Number) b).doubleValue();
        case "/": return ((Number) a).doubleValue() / ((Number) b).doubleValue();
        case "%": return null;
        }
        return null;
    }

    private List eval_array(Ast.Array node) {
        List list = new ArrayList();
        for(Ast element :  node.values)
            list.add(eval(element));
        return list;
    }

    private Object index_into(Ast.Key node) {
        Object container = eval(node.array);
        if(container instanceof List list) {
            int i = ((Number) eval(node.index)).intValue();
            node.error.assertf(i > -1 && i < list.size(), "Index out of bounds",
                "Index '%d' is out of bounds for length '%d' in '%s'!", i, list.size(), node.array.name);
            return list.get(i);
        } else if(container instanceof Map map) {
            temp_assertf(false, "NYI");
        }
        return null;
    }

    private void interpret_for(Ast.For node) {
        scopes.add(new HashMap<>());
        interpret(node.pre);
        while((Boolean) eval(node.cond)) {
            for(Ast child : node.body) interpret(child);
            interpret(node.post);
        }
        scopes.pop();
    }

    private void interpret_if(Ast.If node) {
       scopes.add(new HashMap<>());
        if((Boolean) eval(node.cond))  // Is this even legal? Is this copyright infringement!?
            for (Ast child : node.body) interpret(child);
       scopes.pop();
    }

    // I don't generally like this kind of name, I prefer: 'are_types_matching' or smth, but can't think of anything better rn
    private boolean is_of_type(Object a, Class b) {
        if(a == null) return true;
        // MAYBE cast-able?
        return b.isInstance(a);
        // return a.getClass().equals(b);

    }

    private void temp_assertf(boolean cond, String msg) {
        if(cond) return;

        System.out.println(msg);
    }
}
