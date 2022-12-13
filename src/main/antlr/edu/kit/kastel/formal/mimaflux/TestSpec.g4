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
grammar TestSpec;

@header {
package edu.kit.kastel.formal.mimaflux;
}

file :
  test* EOF
  ;

test :
  name=ID() ':'
  labels+=labelSpec*
  pre+=spec* '==>' post+=spec+
  ;

spec :
  addr=(ID | NUMBER) '=' val=NUMBER
  ;

labelSpec :
  label=ID '->' val=NUMBER
  ;

NUMBER : ( '0' [xX][a-fA-F0-9]+ | ( '-' )? [0-9]+ );
ID : [A-Za-z_][A-Za-z_0-9]*;

WS : [ \t\r\n]+ -> skip;
COMMENT : ';' .*? '\n' -> skip;

UNKNOWN_CHARACTER : . ;