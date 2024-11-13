import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Debug {

    public static void print_tokens(ArrayList<String> tokens) {
        // int max_length = 0;
        // for(String t : tokens) if(t.length() > max_length) max_length = t.length();
        // System.out.printf("count: %d, max len: %d\n", tokens.length, max_length);

        for(String t : tokens) {
            if(t.equals("\n")) { System.out.println(); continue; }
            System.out.print(t + "  ");
        }
        System.out.println();
    }

    public static void print_lexed_tokens(ArrayList<Token> tokens) {

        for(Token t : tokens) {
            if(t.token.equals("\n")) { System.out.println(); continue; }
            System.out.print(t.token + ":" + t.type.name() + "  ");
        }
        System.out.println();
    }

    private static void indent(int times){
        System.out.print("  ".repeat(times));
    }

    public static void send_ast_to_vis(Ast ast) throws IOException {
        DatagramSocket sender = new DatagramSocket();

        String out = zip(ast);
        byte bytes[] = out.getBytes(StandardCharsets.UTF_8);
        sender.send(new DatagramPacket(bytes, bytes.length, new InetSocketAddress("localhost", 8779)));

        sender.close();
    }

    public static String zip(Ast ast) {
        StringBuilder b = new StringBuilder();

        b.append('(');

        if(ast == null) {
            b.append("null").append(')');
            return b.toString();
        }

        switch(ast) {
        case Ast.Root node -> {
            b.append("root");
            for(Ast child : node.children) b.append(zip(child));
        }
        case Ast.Func node -> {
            b.append(node.name);
            for(Ast child : node.args) b.append(zip(child));
        }
        case Ast.UnaOp node -> {
            b.append(node.name);
            b.append(zip(node.rhs));
        }
        case Ast.BinOp node -> {
            b.append(node.name);
            b.append(zip(node.lhs));
            b.append(zip(node.rhs));
        }
        case Ast.Key node -> {
            b.append("[");
            b.append(zip(node.array));
            b.append(zip(node.index));
            if(node.assignment != null)
                b.append("(=").append(zip(node.assignment)).append(')');
        }
        case Ast.Array node -> {
            b.append("array");
            for(Ast element : node.values) b.append(zip(element));
        }
        case Ast.Decl node -> {
            // b.append(node.name).append(": ").append(node.type.getSimpleName());
            b.append(node.name).append(": ").append(node.typename);
            b.append(zip(node.value));
        }
        case Ast.FnDecl node -> {
            b.append(node.name).append(" -> ").append(node.ret_typename);
            for(Ast arg : node.args) b.append(zip(arg));
            if(node.foreign) b.append("(@ ").append(node.path).append(')');
            else for(Ast child : node.body) b.append(zip(child));
        }
        case Ast.For node -> {
            b.append("for")
                .append(zip(node.pre))
                .append(zip(node.cond))
                .append(zip(node.post));
            for(Ast child : node.body) b.append(zip(child));
        }
        case Ast.If node -> {
            b.append("if").append(zip(node.cond));
            for(Ast child : node.body) b.append(zip(child));
        }
        case Ast.Ret node -> {
            b.append("return").append(zip(node.expr));
        }

        // case Ast.Const node -> b.append(node.value).append(": ").append(node.value.getClass().getSimpleName());
        case Ast.Const node -> b.append(node.value).append(": ").append(node.typename);
        case Ast.Var node -> {
            b.append('$').append(node.name);
            if(node.assignment != null) b.append(" = ").append(zip(node.assignment));
        }
        default -> b.append("junk");
        }

        b.append(")");
        return b.toString();
    }

    public static String stringify_param_types_obj(Object[] params) {
        String[] typenames = new String[params.length];
        for(int i = 0; i < params.length; i ++) typenames[i] = params[i].getClass().getSimpleName();
        return stringify_param_types(typenames);
    }

    public static String stringify_param_types(Class[] params) {
        String[] typenames = new String[params.length];
        for(int i = 0; i < params.length; i ++) typenames[i] = params[i].getSimpleName();
        return stringify_param_types(typenames);
    }

    public static String stringify_param_types(String[] types) {
        StringBuilder b = new StringBuilder();
        b.append('(');
        for(int i = 0; i < types.length; i ++) {
            b.append(types[i]);
            if(i != types.length - 1) b.append(", ");
        }
        b.append(')');
        return b.toString();

    }

}
