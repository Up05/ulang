import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class Declarations {

    class Var {
        Class type;
        Object val;
    }

    class Func {
        Class[] args; // Var???
        int precedence;
        // FuncBody body;

        public Func(int precedence, Class... args) {
            this.args = args;
            this.precedence = precedence;
        }
    }

    class JavaFunc {
        String path;
    }

    Map<String, Class> types = Map.of (
        "bool", Boolean.class,
        "num",  Double.class,
        "char", Character.class,
        "string", String.class
    );

    Map<String, Var> variables = new HashMap<>();

    Map<String, Func[]> functions = Map.of (
        "+", new Func[] { new Func(1, Double.class, Double.class) },
        "-", new Func[] { new Func(1, Double.class, Double.class) },
        "*", new Func[] { new Func(2, Double.class, Double.class) },
        "/", new Func[] { new Func(2, Double.class, Double.class) }
    );

}



