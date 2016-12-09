grammar Ublu;		

prog:	token*;
string:	STRING;
token:  string
        | '('
        | ')'
        | '$['
        | ']$'
        | '${'
        | '}$'
        | WORD;

STRING : '"' (ESC | ~ ["\\\n])* '"' ;
fragment ESC : '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

NEWLINE : '\r'? '\n' -> channel(HIDDEN);
WS : (' ' | '\t' | '\r' | '\n')+ -> channel(HIDDEN);
COMMENT : '#' ~('\r' | '\n')* NEWLINE? -> channel(HIDDEN);
WORD : '('
    | ')'
    | '$['
    | ']$'
    | '${'
    | '}$'
    | ~[()\[\]{}$\n \t\r"]+;
