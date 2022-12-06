/*
 * This file is part of the tool MimaFlux.
 * https://github.com/mattulbrich/mimaflux
 *
 * MimaFlux is a time travel debugger for the Minimal Machine
 * used in Informatics teaching at a number of schools.
 *
 * The system is protected by the GNU General Public License Version 3.
 * See the file LICENSE in the main directory of the project.
 *
 * (c) 2016-2022 Karlsruhe Institute of Technology
 *
 * Adapted for Mima by Mattias Ulbrich
 */
grammar MimaAsm;

@header {
package edu.kit.kastel.formal.mimaflux;
}

file :
  line* EOF
  ;

line :
    adr_spec
  | label_spec
  | command
  ;

adr_spec :
  '*' '=' NUMBER
  ;

label_spec :
  ID '=' NUMBER
  ;

command :
  (label=ID ':')? ( mnemomicWith (idArg=ID | numberArg=NUMBER)
              | mnemomicWithout)
  ;

mnemomicWith :
  LDC | DS | LDV | STV | LDIV | STIV | ADD | AND | OR | XOR | JMP | JMN |
  JIND | EQL | JMS
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
JMS : 'JMS';
HALT : 'HALT';
NOT : 'NOT';
RAR : 'RAR';
EQL: 'EQL';

NUMBER : ( '0' [xX][a-fA-F0-9]+ | [0-9]+ );
ID : [A-Za-z_][A-Za-z_0-9]*;

WS : [ \t\r\n]+ -> skip;
COMMENT : ';' .*? '\n' -> skip;

