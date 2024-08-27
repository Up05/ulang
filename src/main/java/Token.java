class Token {
    String token;
    Lexer.Type type;

    public Token(String v, Lexer.Type t) {
        token = v;
        type = t;
    }

    public boolean is(String s) {
        return token.equals(s);
    }
}
