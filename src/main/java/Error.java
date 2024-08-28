public class Error {
    enum Type {
        COMP("[COMPILE-TIME ERROR] "),
        RUNTIME("[RUNTIME ERROR] "),
        TYPE("[TYPE ERROR] ");

        String name;
        Type(String text) {
            name = text;
        }
    }

    VirtualFile file;
    Type prefix; // [COMPILE-TIME ERROR] / [RUNTIME ERROR] / [TYPE ERROR]

    private static final String RESET = "\u001B[0m";
    private static final String RED   = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN  = "\u001B[36m";

    /** '\n' in `msg` are automatically indented. (replaced* with \t\n) */
    public void assertf(boolean expr, String name, String msg, Object... args) {
        if(expr) return;

        System.out.println(RED + prefix + CYAN + "at '" + file.name + "' ln:" + file.line + " " + name);
        System.out.print('\t');
        System.out.println(msg);
        System.out.println(msg.replaceAll("\n", "\t\n").formatted(args));
    }

    public void increment_line() {
        file.line ++;
        if(file.line >= file.len)
            file = file.parent;
        else if(file.line >= file.children.getFirst().from)
            file = file.children.pollFirst();
    }

}
