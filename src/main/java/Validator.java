// Technically, part of Lexer, so it uses LexerException

import java.util.ArrayList;

public class Validator {
    LexerException exception = new LexerException("", "", 1);

    public void validate(ArrayList<Token> tokens, String filename) throws LexerException {
        exception.file = filename;

        for(int i = 0; i < tokens.size(); i ++) {
            Token t = tokens.get(i);
            if(t.is("\n")) exception.line ++;

            switch(t.type) {
            case VARIABLE, FUNCTION_DECL, FUNCTION_CALL, TYPE -> {
                char bad = find_bad_char_in_name(t.token);
                assertf(bad == '\0', "Found a bad char: '%c' in name: '%s'! Identifiers can only contain letters, numbers and '_', plus a name can't start with a number", bad, t.token);
            }
            case CONSTANT -> {
                if (!Character.isDigit(t.token.charAt(0))) continue;
                boolean is_int_too_long = t.token.length() <= 20 || t.token.contains(".");
                warnf(is_int_too_long, "The integer: '%s' is too large to fit into a 64 bit int! Please consider using a different language or a 'float'", t.token);
                char bad = find_bad_char_in_number(t.token);
                warnf(bad == '\0', "Found a bad char: '%c' in number: '%s'! Currently, underscores and scientific notation are not implemented", bad, t.token);
            }
            case KEYWORD -> {
                if(t.is("if") || t.is("for")) {
                    boolean found_block = false;
                    for(int j = i + 1; j < tokens.size(); j++) {
                        if(tokens.get(j).is("{") || tokens.get(j).is("do")) {
                            found_block = true;
                            break;
                        }
                        warnf(!tokens.get(j).is("("), "Statements like 'for' and 'if' do not have perentheses here. They will be read as an expression");
                    }
                    assertf(found_block, "Found '%s' without a corresponding block! Use 'if <expr> do <one-liner>' or 'if <expr> { ... }'", t.token);
                }
            }


            }
        }

    }


    private char find_bad_char_in_name(String token) {
        // tokens can't really be of zero length.
        if(Character.isDigit(token.charAt(0))) return token.charAt(0);

        for(char r : token.toCharArray())
            if(!Character.isLetterOrDigit(r) && r != '_') return r;
        return '\0';
    }

    private char find_bad_char_in_number(String token) {
        for(char r : token.toCharArray()) {
            if(Character.isDigit(r)) continue;
            if(r == '.') continue;
            // scientific notation ('e'/'E') should be it's own token
            return r;
        }
        return '\0';
    }

    private void assertf(boolean expr, String format, Object... values) throws LexerException {
        if(expr) return;
        exception.message = String.format(format, values) + "\n";
        throw exception;
    }

    private void warnf(boolean expr, String format, Object... values) {
        if(expr) return;
        exception.message = String.format(format, values);
        System.out.println("[LEXER WARNING] " + exception.message);
    }
}
