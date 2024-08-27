
@FunctionalInterface
interface Supplier<R> {
    R get() throws Exception;
}

// I could have named this "Pipe", but sometimes, you just have to embrace the functional bro in you...
// After added 'bad': that "Pipe" looks more and more appealing...
public class Monad<T> {
    private T data;
    private T bad;

    public Monad(T initial, T bad_state) {
        data = initial;
        bad = bad_state;
    }

    public Monad<T> bind(Supplier<T> func) throws Exception {
        if(data == bad) data = func.get();
        return this;
    }

    public T unwrap() {
        return data;
    }

}
