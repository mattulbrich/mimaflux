package edu.kit.kastel.formal.mimaflux;

import edu.kit.kastel.formal.mimaflux.MimaAsmParser.CommandContext;

public record Command(int address, String label, String instruction,
                      String labelArg, int valueArg,
                      CommandContext ctx) {
    public Command updateArg(int val) {
        return new Command(address, label, instruction, labelArg, val, ctx);
    }

    public int getMnemonicLine() {
        if(ctx.mnemomicWith() == null) {
            return ctx.mnemomicWithout().getStart().getLine();
        } else {
            return ctx.mnemomicWith().getStart().getLine();
        }
    }
}
