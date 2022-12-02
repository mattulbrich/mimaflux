package edu.kit.kastel.formal.mimaflux;

import org.antlr.v4.runtime.ParserRuleContext;

public record Command(int address, String label, String instruction,
                      String labelArg, int valueArg,
                      ParserRuleContext ctx) {
    public Command updateArg(int val) {
        return new Command(address, label, instruction, labelArg, val, ctx);
    }
}
