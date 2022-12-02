grammar MimaAsm;

@header {
package edu.kit.kastel.formal.mimaflux;
}

file :
  line* EOF
  ;

line :
    adr_spec
  | command
  ;

adr_spec :
  '*' '=' NUMBER
  ;

command :
  (label=ID ':')? ( mnemomicWith (idArg=ID | numberArg=NUMBER)
              | mnemomicWithout)
  ;

mnemomicWith :
  LDC | DS | LDV | STV | LDIV | STIV | ADD | AND | OR | XOR | JMP | JMN |
  JIND | EQL
  ;

mnemomicWithout :
  HALT | NOT | RAR | DS
  ;

LDC : 'LDC';
DS : 'DS';
LDV : 'LDV';
STV : 'STV';
LDIV : 'LDIV';
STIV : 'STIV';
ADD : 'ADD';
AND : 'AND';
OR : 'OR';
XOR : 'XOR';
JMP : 'JMP';
JMN : 'JMN';
JIND : 'JIND';
HALT : 'HALT';
NOT : 'NOT';
RAR : 'RAR';
EQL: 'EQL';

NUMBER : ( '0' [xX] )? [0-9]+;
ID : [A-Za-z_][A-Za-z_0-9]*;

WS : [ \t\r\n]+ -> skip;
COMMENT : ';' .*? '\n' -> skip;

