package edu.kit.kastel.formal.mimaflux;

import edu.kit.kastel.formal.mimaflux.MimaAsmParser.Adr_specContext;
import edu.kit.kastel.formal.mimaflux.MimaAsmParser.CommandContext;

import java.util.ArrayList;
import java.util.List;

public class ProgramVisitor extends MimaAsmBaseVisitor<Void> {

    List<Command> commands = new ArrayList<>();

    private int curAddress = 0;

    @Override
    public Void visitAdr_spec(Adr_specContext ctx) {
        curAddress = Integer.decode(ctx.NUMBER().getText());
        if(curAddress >= Constants.ADDRESS_MASK) {
            throw new TokenedException(ctx.NUMBER().getSymbol(), "Address out of range");
        }
        return null;
    }

    @Override
    public Void visitCommand(CommandContext ctx) {
        String label = null;
        if (ctx.label != null) {
            label = ctx.label.getText();
        }
        int valueArg = 0;
        if (ctx.numberArg != null) {
            valueArg = Integer.decode(ctx.numberArg.getText());
        }

        String labelArg = null;
        if (ctx.idArg != null) {
            labelArg = ctx.idArg.getText();
        }

        String instr;
        if (ctx.mnemomicWith() == null) {
            instr = ctx.mnemomicWithout().getText();
        } else {
            instr = ctx.mnemomicWith().getText();
        }

        commands.add(new Command(curAddress, label, instr, labelArg, valueArg, ctx));
        curAddress ++;
        return null;
    }

    public List<Command> getCommands() {
        return commands;
    }
}
