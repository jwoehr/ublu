package ublu.lexer;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

public class Lexer {
    private static String[] parse(ANTLRInputStream stream) throws IOException {
        UbluLexer lexer = new UbluLexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        UbluParser parser = new UbluParser(tokens);
        UbluParser.ProgContext tree = parser.prog();
        List<String> output = new ArrayList<String>();
        for (ParseTree child: tree.children) {
            output.add(child.getText());
        }
        return output.toArray(new String[output.size()]);
    }
    public static String[] parseFile(String filename) throws IOException {
        ANTLRFileStream file = new ANTLRFileStream(filename);
        return parse(file);
    }
    public static String[] parseString(String data) throws IOException {
        ANTLRInputStream input = new ANTLRInputStream(data);
        return parse(input);
    }
}
