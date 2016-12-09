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

STRING     : '"' ('\\"' | ~'"')* '"';
NEWLINE : '\r'? '\n' -> channel(HIDDEN);
WS : (' ' | '\t' | '\r' | '\n')+ -> channel(HIDDEN);
COMMENT : '#' ~('\r' | '\n')* NEWLINE? -> channel(HIDDEN);
WORD : '('
    | ')'
    | '$['
    | ']$'
    | '${'
    | '}$'
    | ~[()\[\]{}$\n \t\r]+;
