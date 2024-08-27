import java.util.ArrayList;
import java.util.Map;

class TokenizerException extends Exception {
    String message, file;
    int line;

    public TokenizerException(String message, String filename, int lineNumber){
        this.message = message;
        file = filename;
        line = lineNumber;
    }

    @Override
    public String getMessage() {
        return String.format("(%s:%d) %s", file, line, message);
    }
}

public class Tokenizer {
    // r -- [r]une ( in non-utf8 context, this is just a char "instance" )
    // ch -- [ch]ar
    // t -- current token

    TokenizerException potentialException = new TokenizerException("", "", 1);

    boolean a_char_was_escaped = false; // This is a hack. Although backslashes are quite special...

    public String[] tokenize(String raw, String filename) throws TokenizerException {
        ArrayList<String> tokens = new ArrayList<>();
        potentialException.file = filename;

        int just_in_case = 0;
        while(true) {

            raw = raw.substring(index_of_not(raw, new char[] {}, ' ', '\t', '\r')); // Mac OS 9...

            String t = token(raw);
            tokens.add(t);
            if(t.equals("\n")) potentialException.line ++;
            else if(t.startsWith("#")) tokens.set(tokens.size() - 1, "\n");
            else if(t.equals("EOF")) break; // so I don't have to worry about boundaries
            raw = raw.substring(t.length() + (a_char_was_escaped ? 1 : 0));
            a_char_was_escaped = false;

            just_in_case ++;
            if(just_in_case > 1000) break;
        }

        String[] out = new String[tokens.size()];
        return tokens.toArray(out);
    }

    private final Map<Character, Character> escape_sequences = Map.of('"', '"', '\'', '\'', '\\', '\\', 'n', '\n', 'r', '\r', 't', '\t' );

    private String token(String slice) throws TokenizerException {
        if(slice.length() == 0) return "EOF";


        // tokens tend to be short, so it should be much better than splitting up a whole string or a slice(0, indexOf) with toCharArray
        switch(slice.charAt(0)) {
        case '\\':
            if(escape_sequences.containsKey(slice.charAt(1)))
                return String.valueOf(escape_sequences.get(slice.charAt(1)));
            potentialException.message = "Found a dangling backslash or an unexpected escape sequence after the backslash!";
            throw potentialException;
        case '"':
            int q = index_of_non_escaped(slice, '"', 1);
            if(q == -1) {
                potentialException.message = "Mismatched double quote!";
                throw potentialException;
            }
            return slice.substring(0, q + 1);
        case '\'':
            q = index_of_non_escaped(slice, '\'', 1); // Java saves scope between cases, lil' bit silly, but okay...
            if (q == -1) {
                potentialException.message = "Mismatched single quote!";
                throw potentialException;
            }
            return slice.substring(0, q + 1);
        case '#':
            int nl = slice.indexOf(System.lineSeparator());
            if(nl == -1) nl = slice.length();
            else nl += System.lineSeparator().length(); // + a way to escape new lines. Alternative to C's '\' or CMD's '^'
            return slice.substring(0, nl);
        default:
            char r = slice.charAt(0);
            if(Character.isLetterOrDigit(r) || r == '_') { // I don't like "$normalVariable", like you can do in C/Java/JS (think JQuery)
                return slice.substring(0, index_of_not(slice, new char[]{'a', 'z', 'A', 'Z', '0', '9'}, '_'));
            } else {
                return slice.substring(0, 1);
            }
        }
    }
    /** between -- char[] { char[2] { 'a', 'z' }, { '0', '9' } } */
    private int index_of_not(String str, char[] between, char... chars) {
        int i = 0;
        for(; i < str.length(); i ++) {
            char r = str.charAt(i);
            boolean any = false;

            for(char ch : chars) {
                if (r == ch) {
                    any = true;
                    break;
                }
            }

            for(int j = 0; j < between.length - 1; j += 2) {
                if (r >= between[j] && r <= between[j + 1]) {
                    any = true;
                    break;
                }
            }
            if(!any) return i;
        }
        return i;
    }

    private int index_of_non_escaped(String str, char r, int from) {

        int q;
        do {
            q = str.indexOf(r, from);
            from ++;
        } while (
            q > 0 && str.charAt(q - 1) == '\\' &&
            ( q < 2 || str.charAt(q - 2) != '\\' )
        );
        return q;
    }
}
