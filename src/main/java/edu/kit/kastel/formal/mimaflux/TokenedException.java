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
package edu.kit.kastel.formal.mimaflux;

import org.antlr.v4.runtime.Token;

public class TokenedException extends RuntimeException {
    private final Token token;

    public TokenedException(Token token, String msg) {
        super(msg);
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " near line " + token.getLine() + ":" + token.getCharPositionInLine();
    }
}
