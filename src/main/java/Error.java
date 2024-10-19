public class Error {

    private static final String RESET = "\u001B[0m";
    private static final String RED   = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW= "\u001B[33m";
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
        line = 1;
    }

    /** '\n' in `msg` are automatically indented. (replaced* with \t\n) */
    public void assertf(boolean expr, String name, String msg, Object... args) {
        if(expr) return;

        System.out.println(RED + prefix.name + CYAN + "in file '" + file + "' ln:" + line + " " + RED + name);
        System.out.print('\t');
        System.out.println(msg.replaceAll("\n", "\n\t").formatted(args) + RESET); // no real need for replace or regex here, but it's fine.
        System.exit(65); // sysexits.h
    }

    public void warnf(boolean expr, String name, String msg, Object... args) {
        if(expr) return;

        System.out.println(YELLOW + prefix.name.replace("ERROR", "WARNING") + CYAN + "in file '" + file + "' ln:" + line + " " + RED + name);
        System.out.print('\t');
        System.out.println(msg.replaceAll("\n", "\n\t").formatted(args) + RESET); // no real need for replace or regex here, but it's fine.
    }

}
