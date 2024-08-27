import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Preprocessor {

    public static String preprocess(String text) throws IOException {
        int i = 0;
        while(true) {
            i = text.indexOf("$", i);

            if(i == -1) break;
            if(text.charAt(i - 1) == '\\') {
                i ++; continue;
            }

            if(text.startsWith("$insert(", i)) {
                String text2 = insert(text, i);
                i += text2.length() - text.length() + 1;
                text = text2;
            }

        };
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
