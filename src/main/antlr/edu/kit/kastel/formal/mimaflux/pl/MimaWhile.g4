grammar MimaWhile;

@header {
package edu.kit.kastel.formal.mimaflux.pl;
}

file :
  global*
  function*
  EOF
  ;

global :
  'global' ID ( ':=' val=NUMBER | '[' count=NUMBER ']' )?
  ;

function :
  'function' name=ID '(' ( args+=ID ( ',' args+=ID )* )? ')'
  ( 'local' locs+=ID ( ',' locs +=ID )* )?
  block
  ;

block :
  'begin' statement* 'end'
  ;

expr :
     arg1=expr op=('+' | '&' | '|' | '^') arg2=expr #binExp
  | op=('~' | '*' | '-') arg=expr #unExp
  | (NUMBER | ZERO) #litExp
  | ID #varExp
  | ID '[' expr ']' #arrayExp
  | '(' expr ')' # parenExp
  ;

statement :
    target=ID ':=' expr #assignStm
  | ( target=ID ':=' )? 'call' f=ID '(' ( args+=ID ( ',' args+=ID )* )? ')' #callStm
  | 'return' ID #returnStm
  | 'halt' #haltStm
  | 'if' arg1=expr op=('='|'<=') arg2=expr thenBlk=block ( 'else' elseBlk=block )? #ifCmpStm
  | 'if' arg1=expr '<' ZERO thenBlk=block ( 'else' elseBlk=block )? #if0Stm
  | 'while' arg1=expr op =('='|'<=') arg2=expr block #whileStm
  ;

ZERO : '0';
NUMBER : ( '0' [xX] )? [a-fA-F0-9]+;
ID : [a-z_][A-Za-z_0-9]*;

WS : [ \t\r\n]+ -> skip;
COMMENT : ';' .*? '\n' -> skip;

