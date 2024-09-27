import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Abstract Syntax Tree
public class Ast {
    Error error;

    static class Root  extends Ast { ArrayList<Ast> children = new ArrayList<>(); }

    static class Func  extends Ast { String name; Ast[] args; }
    static class BinOp extends Ast { String name; Ast lhs, rhs; } // left-hand-side is technically for equality(assign here). Idk if '=' will be an operator...
    static class UnaOp extends Ast { String name; Ast rhs; }

    static class Array extends Ast { List<Ast> values; String typename; }
    static class Table extends Ast { Map<Ast, Ast> map; }
    static class Key   extends Ast { Ast.Var array; Ast index; } // This is for both: Array and Table
    // static class JavaFunc extends Ast { String path; } // Because I might compile to C. I will NEED built-ins either way

    static class Decl   extends Ast { String name; Class type; String typename; Ast value; }
    static class FnDecl extends Ast { String name; Class ret; String ret_typename; List<Ast.Decl> args; List<Ast> body; }

    static class Assign extends Ast { String name; Ast value; }
    static class Var    extends Ast { String name; }
    static class Const  extends Ast { Object value; String typename; }

    static class For    extends Ast { Ast pre, cond, post; List<Ast> body; }
    static class If     extends Ast { Ast cond; List<Ast> body; }
    static class Ret    extends Ast { Ast expr; } // [ret]urn. FeelsBadMan
}