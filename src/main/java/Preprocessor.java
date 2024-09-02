import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.ArrayList;

public class Preprocessor {


    public static ArrayList<String> preprocess(Path base, String file) throws Exception {

        String text = Files.readString(base.resolve(file));

        ArrayList<String> tokens = new ArrayList<>();

        // TODO: Pass in the Error, eventually
        String[] oops = new Tokenizer().tokenize(text, file);
        Collections.addAll(tokens, oops);

        for(int i = 0; i < tokens.size(); i ++) {
            String t = tokens.get(i);
            if(t.equals("$") && tokens.get(i + 1).equals("insert")) {
                char q = tokens.get(i + 2).charAt(0);
                if(q != '"' && q != '\'') continue; // throw preprocessor error of "$inject missing filename to be injected. It must be in quotes, e.g.: '$inject \"std.u\""  
                String filename = tokens.get(i + 2);
                filename = filename.substring(1, filename.length() - 1);
                ArrayList<String> new_tokens = preprocess(base, filename);
                tokens.set(i + 1, "push");
                tokens.addAll(i + 3, new_tokens);
                i += 2;
            }           


        }
        return tokens;

    }

}
