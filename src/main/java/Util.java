import java.util.ArrayList;
import java.util.Map;

public class Util {

    // why is this not in the std lib?
    public static <T> boolean array_contains(T [] arr, T v){
        for(int i = 0; i < arr.length; i++) if(arr[i].equals(v)) return true;
        return false;
    }

    public static <T> T get_or_null(ArrayList<T> list, int index) {
        // try-catch, I would assume, is slow
        if(index < 0 || index >= list.size()) return null;
        return list.get(index);
    }

    public static boolean any_of(char c, char... ranges) {
        for(int i = 0; i < ranges.length; i += 2) {
            if(c >= ranges[i] && c <= ranges[i + 1]) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean is_any_of(T a, T... values) {
        for(T v : values) if(a.equals(v)) return true;
        return false;
    }

    public static <K, V> String get_keys_by_value(Map<K, V> map, V value) {
        StringBuilder b = new StringBuilder();
        Boolean needs_seperator = false;
        for(Map.Entry<K, V> entry : map.entrySet()) {
            if(entry.getValue().equals(value)) {
                if(needs_seperator) {
                    b.append('|');
                    needs_seperator = false;
                } else {
                    needs_seperator = true;
                }
                b.append(entry.getKey());
            }
        }
        return b.toString();
    }

}

