public class Error {

    private static final String RESET = "\u001B[0m";
    private static final String RED   = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN  = "\u001B[36m";

    enum Type {
        PREP("[PREPROCESSOR ERROR] "),
        COMP("[COMPILE-TIME ERROR] "),
        RUNTIME("[RUNTIME ERROR] "),
        TYPE("[TYPE ERROR] ");

        String name;
        Type(String text) {
            name = text;
        }
    }

    Type prefix; // [COMPILE-TIME ERROR] / [RUNTIME ERROR] / [TYPE ERROR]
    int line;
    String file;
    
    public Error(Type type, String file) {
        prefix = type;
        this.file = file;
        line = 0;
    }

    /** '\n' in `msg` are automatically indented. (replaced* with \t\n) */
    public void assertf(boolean expr, String name, String msg, Object... args) {
        if(expr) return;

        System.out.println(RED + prefix + CYAN + "at '" + file+ "' ln:" + line + " " + name);
        System.out.print('\t');
        System.out.println(msg);
        System.out.println(msg.replaceAll("\n", "\t\n").formatted(args));
    }

}
