import java.util.List;

// As in a stage of a pipeline
public class Stage<T> {

    List<T> tokens;
    int curr = 0;

    T peek(int offset) {
        return tokens.get(curr + offset);
    }

    T next() {
        return tokens.get(curr ++);
    }

    void skip(int count) {
        curr += count;
    }

    boolean are_there(int count) {
        return curr + count < tokens.size() && curr + count >= 0;
    }

}
