import java.util.Stack; 

public class TokenValidator {

    Stack<Error> error_stack = new Stack<>();
    String[] raw_tokens;
    int curr = 0;

    public TokenValidator(String[] raw_tokens, String filename) {
        this.raw_tokens = raw_tokens;
        error_stack.push(new Error(Error.Type.COMP, filename));
    }

    public void validate() throws Exception {
        
        while(curr < raw_tokens.length) {
            
            boolean success = false;
            new Monad(success, false)
                .bind(this::pop_file)
                .bind(this::push_file)
                .unwrap();

        }

    }

    private String peek(int offset) {
        return raw_tokens[curr + offset];
    }

    private String next() {
        return raw_tokens[curr ++];
    }
    private void skip(int count) {
        curr += count;
    }
    private boolean has(int count) {
        return curr + count < raw_tokens.length;
    }

    private boolean push_file() {//{{{
        if(!peek(0).equals("$")) return false;
        next();
        if(peek(0).equals("push")) {
            next();
            error_stack.push(new Error(Error.Type.COMP, next().substring(1, peek(-1).length() - 1)));
            return true;
        }
        return false;
    }//}}}
    private boolean pop_file() throws Exception {//{{{{{{
        if (peek(0).equals("EOF")) {
            error_stack.pop();
            return true;
        }
        return false;
    }//}}}}}}
    private boolean inc_newline() {//{{{
        if(peek(0).equals("\n")) {
            error_stack.peek().line ++;
            return true;
        }
        else return false;
    }//}}}






}
