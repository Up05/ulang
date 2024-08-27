import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

// TODO move to some other place
public class Declarations {

    Map<String, Class> types = Map.of (
        "bool", Boolean.class,
        "num",  Double.class,
        "char", Character.class,
        "string", String.class
    );

}



