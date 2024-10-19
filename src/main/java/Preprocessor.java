import java.io.File;
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

        Error error = new Error(Error.Type.PREP, file);
        for(int i = 0; i < tokens.size(); i ++) {
            String t = tokens.get(i);
            if(t.equals("\n")) error.line ++;
            if(t.equals("$") && tokens.get(i + 1).equals("insert")) {
                char q = tokens.get(i + 2).charAt(0);
                error.assertf(q != '(' && q != '<', "Character not allowed", "There must not be '(' or '<' characters after '$insert'! Should be: '$insert \"filename\"'");
                error.assertf(q == '"' || q == '\'', "Missing quotes", "'$insert \"filename\"' must have a filename in quotes! It is currently: '%s'", tokens.get(i + 2));
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
