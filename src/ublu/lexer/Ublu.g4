grammar Ublu;		

@header {
package ublu.lexer;
}

prog: (string | word)*;
string:	STRING;
word:  WORD;

// Parse the full string into its final representation
STRING : '"' (ESC | ~[\\"\b\f\n\r\t])* '"' {
    String str = getText();
    StringBuilder output = new StringBuilder();
    str = str.substring(1, str.length() - 1);
    while (!str.isEmpty()) {
        int index = str.indexOf("\\");
        if (index == -1) {
            output.append(str);
            break;
        } else {
            int skip = 2;
            output.append(str.substring(0, index));
            switch (str.charAt(index + 1)) {
                case '\\':
                    output.append('\\');
                    break;
                case '"':
                    output.append('"');
                    break;
                case 'b':
                    output.append('\b');
                    break;
                case 'f':
                    output.append('\f');
                    break;
                case 'n':
                    output.append('\n');
                    break;
                case 'r':
                    output.append('\r');
                    break;
                case 't':
                    output.append('\t');
                    break;
                case 'u':
                    skip = 6;
                    String chars = str.substring(index + 2, index + 6);
                    int codepoint = Integer.parseInt(chars, 16);
                    output.append(Character.toChars(codepoint));
                    break;
            }
            str = str.substring(index + skip);
        }
    }
    setText(output.toString());
    };
fragment ESC : '\\' ([\\"bfnrt] | UNICODE);
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

WS : (' ' | '\t' | '\r' | '\n')+ -> skip;
COMMENT : '#' ~('\r' | '\n')* -> skip;
WORD : '('
    | ')'
    | '$['
    | ']$'
    | '${'
    | '}$'
    | ~[()\[\]{}$\n \t\r"]+;
