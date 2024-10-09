import java.util.*;

// What a useful class!
public class Builtin {

    public Map<Integer, String> error_names = new HashMap<>();

    public static final Map<String, String> RETURN_TYPES = Map.of(
        "register_error", SyntaxDefinitions.TYPE_BOOLEAN,
        "pop", SyntaxDefinitions.TYPE_ANY, // THIS SUCKS MAN...
        "remove", SyntaxDefinitions.TYPE_ANY, // THIS SUCKS MAN...
        "len", SyntaxDefinitions.TYPE_NUMBER,
        "make_array", "[] " + SyntaxDefinitions.TYPE_ANY
    );

    public void print(List<Object> args) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            b.append(args.get(i).toString());
            if(i != args.size() - 1) b.append(", ");
        }
        System.out.print(b);
    }

    public void println(List<Object> args) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            b.append(args.get(i).toString());
            if(i != args.size() - 1) b.append(", ");
        }
        System.out.println(b);
    }

    public void handle(List<Object> args) {
        int value = ((Number) args.get(0)).intValue();
        System.out.printf("[ERROR] code: %d\n%s\n", value, error_names.getOrDefault(value, ""));
        if(value > 0) System.exit(1);
    }

    public boolean register_error(List<Object> args) {
        int number = ((Number) args.get(0)).intValue();
        if(error_names.containsKey(number)) return false;
        error_names.put(number, (String) args.get(1));
        return true;
    }

    public void append(List<Object> args) {
        List list = (List) args.get(0);
        list.add(args.get(1));
    }

    public List make_array(List<Object> args) {
        return new ArrayList(Collections.nCopies(((Number) args.get(0)).intValue(), 0));
    }

    public Object pop(List<Object> args) {
        List list = (List) args.get(0);
        return list.remove(list.size() - 1);
    }

    public Object remove(List<Object> args) {
        List list = (List) args.get(0);
        return list.remove(((Number) args.get(1)).intValue());
    }

    public Object len(List<Object> args) {
        List list = (List) args.get(0);
        return list.size();
    }

}
