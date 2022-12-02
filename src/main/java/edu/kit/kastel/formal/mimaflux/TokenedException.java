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
