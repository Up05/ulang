import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.*;

public class NewBuiltin {

    static Error last_error = null; // This kind of sucks and disallows multi-threading. If I ever add that, I would have to rewrite Interpreter and these functions with a 1st 'Context' param... Oh well...
    static HashMap<Integer, Constructor<?>> constructor_cache = new HashMap<>();

    // Does not work with numbers of different types and especially: 'Double' & 'int', PROBABLY
    public static Object _new(Class type, Object... params) {
        Constructor<?> cached = constructor_cache.get(type.hashCode() + Util.summed_array_type_hash(params));
        if(cached != null)
            try { return cached.newInstance(params); } catch(Exception e) { e.printStackTrace(); System.exit(1); }

        Constructor<?>[] constructors = type.getConstructors();
        main: for(Constructor constructor : constructors) {
            if(constructor.getParameterTypes().length != params.length) continue;
            for(int j = 0; j < params.length; j ++) {
                Class c_type = constructor.getParameterTypes()[j];
                if( !c_type.getSimpleName().equalsIgnoreCase(params[j].getClass().getSimpleName()) &&
                    !c_type.isAssignableFrom(params[j].getClass()) &&
                    !params[j].getClass().isAssignableFrom(c_type)) continue main;
            }
            try {
                constructor_cache.put(type.hashCode() + Util.summed_array_type_hash(params), constructor);
                return constructor.newInstance(params);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        StringBuilder b = new StringBuilder();
        for(Constructor constructor : type.getConstructors()) {
            b.append("\n\t");
            b.append(Debug.stringify_param_types(constructor.getParameterTypes()));
        }
        last_error.prefix = Error.Type.RUNTIME;
        last_error.assertf(false, "Constructor not found",
            "internal function 'Builting._new' could not find a constructor for foreign class: \n" +
                "'%s' with parameters: '%s' with types: '%s'\nPossible constructors: %s",
            type.getSimpleName(), Arrays.toString(params), Debug.stringify_param_types_obj(params), b.toString());

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

    public static Object _make_array_java(Class type, int len) {
        return (Object) Array.newInstance(type, len);
    }

    public static void _set_f64_arr(double[] arr, int index, double obj) { arr[index] = obj; }
    // This is so sad :(, I already have the data in that exact format, so, e.g.:
    // in Odin or C, this would be list.buf or list[:]
    public static double[] _copy_to_f64_array(List<Object> list) {
        double[] arr = new double[list.size()];
        for(int i = 0; i < list.size(); i ++) {
            arr[i] = ((Number) list.get(i)).doubleValue();
        }
        return arr;
    }
    // Will need to change foreign signature and everything.
}
