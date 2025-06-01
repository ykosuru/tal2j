grammar TAL;

// Lexer Rules
QUESTION        : '?' ;
LPAREN          : '(' ;
RPAREN          : ')' ;
LBRACKET        : '[' ;
RBRACKET        : ']' ;
SEMI            : ';' ;
COMMA           : ',' ;
COLON           : ':' ;
DOT             : '.' ;
ASSIGN          : ':=' ;
MOVE_LR         : '\'' ':=' ;
MOVE_RL         : '\'' '=' ':' ;
EQ              : '=' ;
NEQ             : '<>' ;
LT              : '<' ;
GT              : '>' ;
LTE             : '<=' ;
GTE             : '>=' ;
PLUS            : '+' ;
MINUS           : '-' ;
MUL             : '*' ;
DIV             : '/' ;
MOD             : '\\' ;
LSHIFT          : '\'' '<' '<' '\'' ;
RSHIFT          : '\'' '>' '>' '\'' ;
ADDRESS_OF      : '@' ;
ARROW           : '->' ;

// --- Language Keywords ---
AND_OP          : A N D ;
BEGIN_KW        : B E G I N ;
BLOCK_KW        : B L O C K ;
BY_STMT         : B Y ;
CALL_STMT       : C A L L ;
CALLABLE_KW     : C A L L A B L E ;
DO_STMT         : D O ;
DOWNTO_STMT     : D O W N T O ;
ELSE_STMT       : E L S E ;
END_KW          : E N D ;
ENDIF_STMT      : E N D I F ;
ENDWHILE_STMT   : E N D W H I L E ;
EXTERNAL_KW     : E X T E R N A L ;
EXT_KW          : E X T ;
EXTENSIBLE_KW   : E X T E N S I B L E ;
FIXED_TYPE      : F I X E D ;
FOR_STMT        : F O R ;
FORWARD_KW      : F O R W A R D ;
IF_STMT         : I F ;
INT_TYPE        : I N T ;
INTERRUPT_KW    : I N T E R R U P T ;
LITERAL_KW      : L I T E R A L ;
MAIN_KW         : M A I N ;
NOT_OP          : N O T ;

System: N O T ;
OR_OP           : O R ;
PRIVATE_KW      : P R I V A T E ;
PRIV_KW         : P R I V ;
PROC_KW         : P R O C ;
REAL_TYPE       : R E A L ;
RESIDENT_KW     : R E S I D E N T ;
RETURN_STMT     : R E T U R N ;
STRING_TYPE     : S T R I N G ;
STRUCT_KW       : S T R U C T ;
SUBPROC_KW      : S U B P R O C ;
THEN_STMT       : T H E N ;
TO_STMT         : T O ;
UNTIL_STMT      : U N T I L ;
VARIABLE_KW     : V A R I A B L E ;
WHILE_STMT      : W H I L E ;
WHILE_SCAN_KW   : W H I L E ; // Specific for scan/rscan
UNTIL_SCAN_KW   : U N T I L ; // Specific for scan/rscan

// --- NEW Lexer Rules Added ---
ASSERT_STMT     : A S S E R T ;
CASE_STMT       : C A S E ;
DROP_STMT       : D R O P ;
GOTO_STMT       : G O T O ;
RSCAN_STMT      : R S C A N ;
SCAN_STMT       : S C A N ;
STORE_STMT      : S T O R E ;
USE_STMT        : U S E ;

// --- Directive Keywords ---
ABORT_DIR_KEYWORD       : A B O R T ;
ABSLIST_DIR_KEYWORD     : A B S L I S T ;
ASSERTION_DIR_KEYWORD   : A S S E R T I O N ;
CODE_DIR_KEYWORD        : C O D E ;
COMPACT_DIR_KEYWORD     : C O M P A C T ;
CPU_DIR_KEYWORD         : C P U ;
CROSSREF_DIR_KEYWORD    : C R O S S R E F ;
DATAPAGES_DIR_KEYWORD   : D A T A P A G E S ;
DECS_DIR_KEYWORD        : D E C S ;
DEFEXPAND_DIR_KEYWORD   : D E F E X P A N D ;
DUMPCONS_DIR_KEYWORD    : D U M P C O N S ;
ERRORS_DIR_KEYWORD      : E R R O R S ;
EXTENDSTACK_DIR_KEYWORD : E X T E N D S T A C K ;
GMAP_DIR_KEYWORD        : G M A P ;
ICODE_DIR_KEYWORD       : I C O D E ;
IF_DIR_KEYWORD          : I F ;
ENDIF_DIR_KEYWORD       : E N D I F ;
IFNOT_DIR_KEYWORD       : I F N O T ;
INHIBITXX_DIR_KEYWORD   : I N H I B I T X X ;
INNERLIST_DIR_KEYWORD   : I N N E R L I S T ;
INSPECT_DIR_KEYWORD     : I N S P E C T ;
LIBRARY_DIR_KEYWORD     : L I B R A R Y ;
LINES_DIR_KEYWORD       : L I N E S ;
LIST_DIR_KEYWORD        : L I S T ;
LMAP_DIR_KEYWORD        : L M A P ;
MAP_DIR_KEYWORD         : M A P ;
NOCODE_DIR_KEYWORD      : N O C O D E ;
NOLIST_DIR_KEYWORD      : N O L I S T ;
PAGE_DIR_KEYWORD        : P A G E ;
PRINTSYM_DIR_KEYWORD    : P R I N T S Y M ;
RELOCATE_DIR_KEYWORD    : R E L O C A T E ;
RESETTOG_DIR_KEYWORD    : R E S E T T O G ;
ROUND_DIR_KEYWORD       : R O U N D ;
RP_DIR_KEYWORD          : R P ;
SAVEABEND_DIR_KEYWORD   : S A V E A B E N D ;
SEARCH_DIR_KEYWORD      : S E A R C H ;
SECTION_DIR_KEYWORD     : S E C T I O N ;
SETTOG_DIR_KEYWORD      : S E T T O G ;
SOURCE_DIR_KEYWORD      : S O U R C E ;
STACK_DIR_KEYWORD       : S T A C K ;
SUPPRESS_DIR_KEYWORD    : S U P P R E S S ;
SYMBOLS_DIR_KEYWORD     : S Y M B O L S ;
SYNTAX_DIR_KEYWORD      : S Y N T A X ;
WARN_DIR_KEYWORD        : W A R N ;

ENDBLOCK_KW             : E N D B L O C K ;
ENDSTRUCT_KW            : E N D S T R U C T ;

DOLLAR_SIGN     : '$' ;

// Literals
INT_LITERAL     : DIGIT+ 'D'? ;
HEX_LITERAL     : '%' 'H' HEX_DIGIT+ ;
OCTAL_LITERAL   : '%' DIGIT_OCTAL+ ;
BINARY_LITERAL  : '%' 'B' BIN_DIGIT+ ;

REAL_LITERAL    : (DIGIT+ DOT DIGIT* | DOT DIGIT+) EXPONENT? | DIGIT+ EXPONENT ;
fragment EXPONENT: E (PLUS | MINUS)? DIGIT+ ;

STRING_LITERAL  : '"' ( ('""') | ~["\r\n] )*? '"' ;

// Identifiers
IDENTIFIER      : [a-zA-Z^] [a-zA-Z0-9^_]* ;

// Comments
COMMENT         : '!' .*? ('!' | EOF | ('\r'? '\n')) -> channel(HIDDEN) ;

// Whitespace
WS              : [ \t\r\n]+ -> skip ;

// Case-insensitive letter fragments
fragment A:('a'|'A'); fragment B:('b'|'B'); fragment C:('c'|'C'); fragment D:('d'|'D');
fragment E:('e'|'E'); fragment F:('f'|'F'); fragment G:('g'|'G'); fragment H:('h'|'H');
fragment I:('i'|'I'); fragment J:('j'|'J'); fragment K:('k'|'K'); fragment L:('l'|'L');
fragment M:('m'|'M'); fragment N:('n'|'N'); fragment O:('o'|'O'); fragment P:('p'|'P');
fragment Q:('q'|'Q'); fragment R:('r'|'R'); fragment S:('s'|'S'); fragment T:('t'|'T');
fragment U:('u'|'U'); fragment V:('v'|'V'); fragment W:('w'|'W'); fragment X:('x'|'X');
fragment Y:('y'|'Y'); fragment Z:('z'|'Z');

fragment DIGIT          : [0-9] ;
fragment DIGIT_OCTAL    : [0-7] ;
fragment BIN_DIGIT      : [01] ;
fragment HEX_DIGIT      : [0-9a-fA-F] ;

// Parser Rules
program
    : programElement* EOF
    ;

programElement
    : directiveLine
    | topLevelDeclaration
    | procedureDefinition
    ;

topLevelDeclaration
    : (literalDeclaration | variableDeclaration | structDeclaration | nameDeclaration) SEMI?
    | blockDeclaration
    ;

directiveLine
    : QUESTION directiveElement (COMMA directiveElement)*
    ;

directiveElement
    : directiveKeyword (directiveParameterPart)?
    | IDENTIFIER (directiveParameterPart)?
    ;

directiveKeyword
    : PAGE_DIR_KEYWORD | SOURCE_DIR_KEYWORD | NOLIST_DIR_KEYWORD | LIST_DIR_KEYWORD |
      SYMBOLS_DIR_KEYWORD | NOCODE_DIR_KEYWORD | IF_DIR_KEYWORD | ENDIF_DIR_KEYWORD
    ;

directiveParameterPart
    : EQ expression | LPAREN paramList? RPAREN | STRING_LITERAL
    ;

paramList
    : expression (COMMA expression)*
    ;

declaration
    : literalDeclaration
    | variableDeclaration
    | structDeclaration
    | nameDeclaration
    ;

nameDeclaration
    : 'NAME' IDENTIFIER
    ;

blockDeclaration
    : BLOCK_KW IDENTIFIER SEMI
      (declaration SEMI?)*
      (procedureDefinition)*
      (statement SEMI?)*
      (END_KW | ENDBLOCK_KW) SEMI? (COMMENT)?
    ;

builtinFunctionCall
    : DOLLAR_SIGN IDENTIFIER LPAREN actualParameterList? RPAREN
    ;

literalDeclaration
    : LITERAL_KW literalItem (COMMA literalItem)* SEMI?
    ;

literalItem
    : IDENTIFIER EQ expression
    ;

variableDeclaration
    : typeSpecifier variableDeclarator (COMMA variableDeclarator)*
    ;

variableDeclarator
    : indirectionSpecifier? IDENTIFIER (arraySpecifier)? (ASSIGN expression)?
    ;

indirectionSpecifier
    : DOT
    | DOT EXT_KW
    ;

typeSpecifier
    : (INT_TYPE | STRING_TYPE | FIXED_TYPE | REAL_TYPE)
      (LPAREN (INT_LITERAL | MUL | fpoint) RPAREN)?
    | (STRUCT_KW indirectionSpecifier? IDENTIFIER (LPAREN IDENTIFIER RPAREN)?)
    ;

fpoint
    : expression
    ;

arraySpecifier
    : LBRACKET expression COLON expression RBRACKET
    ;

arrayLiteral
    : LBRACKET (expression (COMMA expression)*)? RBRACKET
    ;

structDeclaration
    : STRUCT_KW indirectionSpecifier? IDENTIFIER SEMI
      structDefinitionBody
      (END_KW | ENDSTRUCT_KW) SEMI?
    ;

structMemberStructDeclaration
    : STRUCT_KW IDENTIFIER SEMI?
      structDefinitionBody
    ;

structDefinitionBody
    : BEGIN_KW
      (structMemberDeclaration SEMI?)*
      END_KW
    ;

structMemberDeclaration
    : variableDeclaration
    | structMemberStructDeclaration
    | literalDeclaration
    ;

procedureDefinition
    : typeSpecifier? (PROC_KW | SUBPROC_KW) IDENTIFIER (LPAREN formalParameterList? RPAREN)? procedureAttributes? SEMI
      (formalParameterTypeDeclaration)*
      procedureBody?
    ;

formalParameterList
    : formalParameter (COMMA formalParameter)*
    ;

formalParameter
    : typeSpecifier indirectionSpecifier? IDENTIFIER
    | IDENTIFIER
    ;

formalParameterTypeDeclaration
    : typeSpecifier indirectionSpecifier? IDENTIFIER (COMMA indirectionSpecifier? IDENTIFIER)* SEMI
    ;

formalParameterNameList
    : IDENTIFIER (COMMA IDENTIFIER)*
    ;

procedureAttributes
    : (MAIN_KW | VARIABLE_KW | EXTENSIBLE_KW | RESIDENT_KW | CALLABLE_KW | PRIV_KW | INTERRUPT_KW | FORWARD_KW | EXTERNAL_KW)+
    ;

procedureBody
    : BEGIN_KW (declaration SEMI)* (statement SEMI?)* END_KW SEMI?
    ;

statement
    : assignmentStatement
    | rscanStatement
    | scanStatement
    | ifStatement
    | whileStatement
    | callStatement
    | returnStatement
    | blockStatement
    | directiveLine
    | emptyStatement
    | forStatement
    | doStatement
    | assertStatement
    | caseStatement
    | dropStatement
    | gotoStatement
    | storeStatement
    | useStatement
    | expression
    | error
    ;

emptyStatement
    : SEMI
    ;

blockStatement
    : BEGIN_KW (statement SEMI?)* END_KW SEMI?
    ;

assignmentStatement
    : lvalue (ASSIGN | MOVE_LR | MOVE_RL) expression (FOR_STMT expression)?
    ;

lvalue
    : qualifiedName (arrayAccess)* (bitFieldSpecifier)?
    ;

bitFieldSpecifier
    : DOT LT expression (COLON expression)? GT
    ;

ifStatement
    : IF_STMT expression THEN_STMT
      statement
      (ELSE_STMT statement)?
      (ENDIF_STMT SEMI?)?
    ;

whileStatement
    : WHILE_STMT expression DO_STMT
      statement
      (ENDWHILE_STMT SEMI?)?
    ;

forStatement
    : FOR_STMT IDENTIFIER ASSIGN expression (TO_STMT | DOWNTO_STMT) expression (BY_STMT expression)? DO_STMT statement
    ;

doStatement
    : DO_STMT statement UNTIL_STMT expression
    ;

callStatement
    : CALL_STMT IDENTIFIER (LPAREN actualParameterList? RPAREN)? SEMI?
    ;

returnStatement
    : RETURN_STMT expression? SEMI?
    ;

assertStatement
    : ASSERT_STMT expression SEMI?
    ;

caseStatement
    : CASE_STMT expression OF_KW
      BEGIN_KW
      (caseAlternative)+
      END_KW SEMI?
    ;

caseAlternative
    : caseValueList ARROW statement
    ;

caseValueList
    : expression (COMMA expression)*
    ;

caseLabelList
    : caseLabel (COMMA caseLabel)*
    ;

caseLabel
    : expression COLON statement SEMI?
    ;

dropStatement
    : DROP_STMT IDENTIFIER SEMI?
    ;

gotoStatement
    : GOTO_STMT IDENTIFIER SEMI?
    ;

scanStatement
    : SCAN_STMT expression (WHILE_STMT | UNTIL_STMT) expression ARROW ADDRESS_OF IDENTIFIER SEMI?
    ;

rscanStatement  
    : RSCAN_STMT expression (WHILE_STMT | UNTIL_STMT) expression ARROW ADDRESS_OF IDENTIFIER SEMI?
    ;

storeStatement
    : STORE_STMT lvalue SEMI?
    ;

useStatement
    : USE_STMT IDENTIFIER SEMI?
    ;

OF_KW
    : O F ;

actualParameterList
    : expression (COMMA expression)*
    ;

expression
    : logicalOrExpression
    ;

logicalOrExpression
    : logicalAndExpression (OR_OP logicalAndExpression)*
    ;

logicalAndExpression
    : relationalExpression (AND_OP relationalExpression)*
    ;

relationalExpression
    : additiveExpression ( (EQ | NEQ | LT | GT | LTE | GTE) additiveExpression (FOR_STMT expression)? )?
    ;

additiveExpression
    : multiplicativeExpression ( (PLUS | MINUS) multiplicativeExpression )*
    ;

multiplicativeExpression
    : shiftExpression ( (MUL | DIV | MOD) shiftExpression )*
    ;

shiftExpression
    : unaryExpression ( (LSHIFT | RSHIFT) unaryExpression )*
    ;

unaryExpression
    : (PLUS | MINUS | NOT_OP) unaryExpression
    | primaryExpression
    ;

primaryExpression
    : literal
    | arrayLiteral
    | IDENTIFIER (arrayAccess)*
    | qualifiedName (arrayAccess)*
    | functionCall
    | builtinFunctionCall
    | ADDRESS_OF qualifiedName (arrayAccess)*
    | LPAREN expression RPAREN
    ;

qualifiedName
    : indirectionSpecifier? IDENTIFIER (DOT IDENTIFIER)*
    ;

arrayAccess
    : LBRACKET expression RBRACKET
    ;

functionCall
    : IDENTIFIER LPAREN actualParameterList? RPAREN
    ;

literal
    : INT_LITERAL
    | STRING_LITERAL
    | REAL_LITERAL
    | HEX_LITERAL
    | OCTAL_LITERAL
    | BINARY_LITERAL
    ;

error : .+? ;

