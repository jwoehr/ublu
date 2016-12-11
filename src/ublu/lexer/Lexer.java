import java.io.IOException;
import java.util.List;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

public class Lexer {
    public List<ParseTree> parseFile(String filename) {
        try {
            ANTLRFileStream file = new ANTLRFileStream(filename);
            UbluLexer lexer = new UbluLexer(file);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            UbluParser parser = new UbluParser(tokens);
            UbluParser.ProgContext tree = parser.prog();
            return tree.children;
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
