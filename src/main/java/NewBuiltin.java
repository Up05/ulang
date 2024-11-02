import java.lang.reflect.Constructor;
import java.util.Arrays;

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

    // TODO: rewrite the Builtin :(

}
