import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

        case Ast.Var node -> eval(node);
        case Ast.Key node -> index_into(node);
        // delete this eventually
        default -> { }
        }
    }

    // To check where variable was declared in a different scope or not to check whether variable was declared in a different scope?
    private void declare_var(Ast.Decl node) {
        for(HashMap<String, Object> scope : scopes) {
            node.error.prefix = Error.Type.RUNTIME;
            node.error.assertf(!scope.containsKey(node), "Redeclaration of a variable", "Variable '%s' has been redeclared", node.name);
        }

        Object val = eval(node.value);
        if(val != null)
            node.error.assertf(is_of_type(node.type, val.getClass(), node.typename), "Mismatched types",
                "Got '%s', expected '%s' or '%s'!", val.getClass().getName(), node.typename, node.type.getSimpleName());

        scopes.peek().put(node.name, val);
    }

    private Object eval(Ast ast) {
        if(ast == null) return null;
        switch(ast) {
        case Ast.Const node -> {
            if(Objects.equals(node.typename, SyntaxDefinitions.TYPE_TYPE)) return SyntaxDefinitions.types.get(node.value);
            return node.value;
        }
        case Ast.Var node -> {
            for(HashMap<String, Object> scope : scopes) { // I would love this to be reversed :(
                Object val = scope.get(node.name);
                if(val != null) {
                    if(node.assignment != null) scope.put(node.name, eval(node.assignment));
                    return val;
                }
            }
            temp_assertf(false, "Trying to get the value of an undeclared variable: " + node.name); // This should not get past the Validator
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
        scopes.push(new HashMap<>()); // intermediate

        Ast.FnDecl decl = null;
        for(Ast child : root.children) {
            if(child instanceof Ast.FnDecl decl_node)
                if(node.name.equals(decl_node.name))
                    decl = decl_node;
        }
        node.error.assertf(decl != null, "Function is not declared", "Trying to call an undeclared function '%s'!", node.name);

        for(Ast.Decl arg : decl.args) {
            declare_var(arg);
        }

        for(int i = 0; i < node.args.length; i ++) {
            if(decl.args.get(i).typename.equals(SyntaxDefinitions.TYPE_VARARGS)) {
                Object[] arguments = new Object[node.args.length - i];
                for(int j = i; j < node.args.length; j ++) {
                    arguments[j - i] = node.args[j];
                }
                scopes.peek().put(decl.args.get(i).name, arguments);
                break;
            }

            scopes.peek().put(decl.args.get(i).name, eval(node.args[i]));
        }
        if(!decl.args.isEmpty() && decl.args.getLast().typename.equals(SyntaxDefinitions.TYPE_VARARGS) && decl.args.size() + 1 == node.args.length) {
            scopes.peek().put(decl.args.getLast().name, new Object[0]);
        }

        if(decl.foreign) {
            NewBuiltin.last_error = node.error;
            int dot = decl.path.lastIndexOf(".");
            node.error.assertf(dot != -1, "Expected class to be specified", "You must specify a class path in `foreign \"path.to.class.function\"` expression!");
            try {
                Class cl = Class.forName(decl.path.substring(0, dot));
                Method method = find_matching_method(cl, decl.path.substring(dot + 1), decl);

                Class[] arg_types = method.getParameterTypes();
                Object[] arguments = new Object[arg_types.length];

                Object this_obj  = null;
                int param_offset = 0;

                if(!Modifier.isStatic(method.getModifiers())) {
                    this_obj = eval(node.args[0]); param_offset = 1;
                    temp_assertf(cl.isInstance(this_obj), "The foreign function: '" + method.getName() + "' is not static, so it's first argument must be the 'this' object!");
                }
                for(int i = param_offset; i < arg_types.length + param_offset; i ++) {
                    if(decl.args.get(i).typename.equals(SyntaxDefinitions.TYPE_VARARGS) &&
                        arg_types[i].equals(Object[].class)) {
                        arguments[i] = new Object[node.args.length - i];
                        for(int j = i; j < node.args.length; j ++)
                            ((Object[]) arguments[i - param_offset])[j - i] = eval(node.args[j]);
                        break;
                    }
                    arguments[i - param_offset] = try_cast_some(eval(node.args[i]), arg_types[i - param_offset]);
                }
                Object val = method.invoke(this_obj, arguments);
                scopes.pop();
                return val;
            } catch (Exception e) {
                System.out.println("HANDLED ERROR");
                System.out.printf("In function: '%s'\n  %s\n", node.name, Debug.zip(node));
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            for(Ast child : decl.body) {
                if(child instanceof Ast.Ret ret_stmt) {
                    Object val = eval(ret_stmt.expr);
                    scopes.pop();
                    return val;
                }
                interpret(child);
            }
        }

        scopes.pop();
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
        case "==": return a.equals(b);
        case "!=": return !a.equals(b);

        case "+": return ((Number) a).doubleValue() + ((Number) b).doubleValue();
        case "-": return ((Number) a).doubleValue() - ((Number) b).doubleValue();
        case "*": return ((Number) a).doubleValue() * ((Number) b).doubleValue();
        case "/": return ((Number) a).doubleValue() / ((Number) b).doubleValue();
        case "%": return ((Number) a).doubleValue() % ((Number) b).doubleValue();
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
                "Index '%d' is out of bounds for length '%d'!", i, list.size()); // Could do "Debug.get_token_name"
            if(node.assignment != null) list.set(i, eval(node.assignment));
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

    private Method find_matching_method(Class parent, String name, Ast.FnDecl decl) {
        Method[] methods = parent.getMethods();
        for(Method method : methods) {
            if(!method.getName().equals(name)) continue;
            Boolean is_static = Modifier.isStatic(method.getModifiers());

            Class[] params = method.getParameterTypes();
            if(params.length != decl.args.size() - (!is_static?1:0)) continue;

            boolean all_match = true;
            for(int i = 0; i < params.length; i ++) {
                Ast.Decl arg = decl.args.get(i + (!is_static?1:0));
                all_match &= is_of_type(params[i], arg.type, arg.typename);
            }
            if(all_match) return method;
        }

        Class[] param_types = new Class[decl.args.size()];
        for(int i = 0; i < decl.args.size(); i ++) param_types[i] = decl.args.get(i).type;

        decl.error.assertf(false,
            "Missing foreign method", "Could not find a suitable overload for foreign method\n%s%s\n" +
            "Here are some potential methods for class '%s': \n%s",
            name, Debug.stringify_param_types(param_types), parent.getName(), debug_get_similar_methods(parent, name));
        return null;
    }

    // I don't generally like this kind of name, I prefer: 'are_types_matching' or smth, but can't think of anything better rn
    private boolean is_of_type(Class a, Class b, String b_typename) {
        return a.isAssignableFrom(b) || b.isAssignableFrom(a)
            || b_typename.equals(SyntaxDefinitions.TYPE_ANY)
            || (is_number(a) && is_number(b))
            || a.getSimpleName().equalsIgnoreCase(b.getSimpleName());
    }
    // Fast approaching JavaScript levels of fun here...
    private boolean is_number(Class c) {
        return Number.class.isAssignableFrom(c) || c.isPrimitive();
    }

    // After a month of usage, I have noticed, that this does absolute fucking nothing.
    public static Object try_cast_some(Object any, Class needed) {
        if(needed.equals(byte.class))    return ((Number) any).byteValue();
        if(needed.equals(short.class))   return ((Number) any).shortValue();
        if(needed.equals(int.class))     return ((Number) any).intValue();
        if(needed.equals(long.class))    return ((Number) any).longValue();
        if(needed.equals(float.class))   return ((Number) any).floatValue();
        if(needed.equals(double.class))  return ((Number) any).doubleValue();
        if(needed.equals(Byte.class))    return Byte.   valueOf(((Number) any).byteValue());
        if(needed.equals(Short.class))   return Short.  valueOf(((Number) any).shortValue());
        if(needed.equals(Integer.class)) return Integer.valueOf(((Number) any).intValue());
        if(needed.equals(Long.class))    return Long.   valueOf(((Number) any).longValue());
        if(needed.equals(Float.class))   return Float.  valueOf(((Number) any).floatValue());
        if(needed.equals(Double.class))  return Double. valueOf(((Number) any).doubleValue());

        if(needed.equals(CharSequence.class) && any.getClass().equals(String.class)) return ((String) any).subSequence(0, ((String) any).length());
        return any;
    }

    private String debug_get_similar_methods(Class parent, String name) {
        StringBuilder b = new StringBuilder();
        Method[] methods = parent.getMethods();
        for(Method method : methods) {
            if(!method.getName().equals(name)) continue;
            b.append('\t').append(method.getName());
            Class[] params = method.getParameterTypes();
            b.append(Debug.stringify_param_types(params));
            b.append("  or  ");
            b.append(method.getName()).append('(');
            int i = 0;
            for(Class param : params) {
                String keys = Util.get_keys_by_value(SyntaxDefinitions.types, param);
                b.append(keys);
                if(keys.length() == 0) {
                    if(param.isPrimitive()) b.append("num");
                    else b.append("<unknown type>");
                }
                if(i != params.length - 1) b.append(", ");
                i ++;
            }
            b.append(')').append('\n');
        }
        return b.toString();
    }

    private void temp_assertf(boolean cond, String msg) {
        if(cond) return;

        System.out.println("[INTERPRETER TEMP ASSERTF] " + msg);
    }
}
