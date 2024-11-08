import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NewBuiltin {

    static Error last_error = null; // This kind of sucks and disallows multi-threading. If I ever add that, I would have to rewrite Interpreter and these functions with a 1st 'Context' param... Oh well...

    public static Object _new(Class type, Object... params) {
        Constructor<?>[] constructors = type.getConstructors();
        main: for(Constructor constructor : constructors) {
            if(constructor.getParameterTypes().length != params.length) continue;

            for(int j = 0; j < params.length; j ++)
                if(!constructor.getParameterTypes()[j].isInstance(params[j])) continue main;

            try {
                return constructor.newInstance(params);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        last_error.prefix = Error.Type.RUNTIME;
        last_error.assertf(false, "Constructor not found",
            "internal function 'Builting._new' could not find a constructor for foreign class: '%s' with parameters: '%s'",
            type.getSimpleName(), Arrays.toString(params));
        return null;
    }
    // Could add _get & _set(obj, prop_name), if needed

    private static void print(String sep, Object... values) {
        StringBuilder b = new StringBuilder();
        for(Object value : values) {
            if(value == null) b.append("null");
            else b.append(value);
            if(value != values[values.length - 1]) b.append(sep);
        }
        System.out.print(b);
    }
    public static void _print(Object... values) { print(", ", values); }
    public static void _println(Object... values) { print("\n", values); System.out.println(); }
    public static void _assert(boolean expr, String fmt, Object... values) {
        last_error.assertf(expr, "Runtime assertion", fmt, values);
    }

    public static void _append(List<Object> list, Object value) { list.add(value); }
    public static List _make_array(int len) { return new ArrayList(Collections.nCopies(len, null)); }
    public static Object _pop(List<Object> list) { return list.removeLast(); }
    public static Object _remove(List<Object> list, int index) { return list.remove(index); }

    public static Object _len(List<Object> list) { return list.size(); }
    public static Object _len(Object[] array)    { return array.length; }
    public static Object _len(String string)     { return string.length(); }
}
