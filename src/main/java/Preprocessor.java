import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.LinkedList;
import java.util.StringTokenizer;

class VirtualFile {
    String name;
    int line = 1,
        from, // line that this file starts at in the parent file
        len;  // number of lines this file has

    Deque<VirtualFile> children = new LinkedList<>();
    VirtualFile parent;
}

public class Preprocessor {

    public VirtualFile root;
    public String text;

    public Preprocessor(Path base, String relative) throws IOException {
        root.name = String.valueOf(relative);
        // I hope to dear god and everything else that this DOES NOT compile and invoke the Regex check for every character!!!
        String[] lines = Files.readString(base.resolve(relative), StandardCharsets.UTF_8).split("\n");

        for(String line : lines) {
            int $ = 0;
            while(true) {
                $ = line.indexOf('$', $);
                if($ == -1) break;

                if(line.startsWith("$insert(")) {
                    // Preprocessor inner = insert2(Path base, line.substring( line.indexOf('(', $ + 6) + 1, line.indexOf('(', $ + 7) ));
                    // root.children.add(inner.root);
                    // line.replace doesn't work because (1) doesn't it clone? (2) what if there are 2 things in the same line, this'd detect the inner ones first

                }

            }
        }


    }


    public static String preprocess(String text) throws IOException {
        int i = 0;
        while(true) {
            i = text.indexOf("$", i);

            if(i == -1) break;

            if(text.startsWith("$insert(", i)) {
                String text2 = insert(text, i);
                i += text2.length() - text.length();
                text = text2;
            }

            i ++;
        }
        return text;
    }

    private static String insert(String str, int at) throws IOException {
        if(str.charAt(at) != '$') System.out.println("Preprocessor warning: str[at] is not '$', it is instead: '" + str.charAt(at) + "'");

        int end = str.indexOf(")", at);
        String filename = str.substring(str.indexOf("(", at) + 1, end);
        String other = Files.readString(Paths.get(filename));
        other = preprocess(other);
        return str.substring(0, at) + other + str.substring(end);
    }

}
