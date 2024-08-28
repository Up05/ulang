import java.util.ArrayList;

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


}
