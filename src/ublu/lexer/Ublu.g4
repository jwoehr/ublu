grammar Ublu;		

@header {
package ublu.lexer;
}

prog: (string | word)*;
string:	STRING;
word:  WORD;

STRING : '"' (ESC | ~["\n\t\b\r\f])* '"';
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
