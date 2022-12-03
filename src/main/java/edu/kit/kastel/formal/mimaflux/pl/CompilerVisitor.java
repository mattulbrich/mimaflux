package edu.kit.kastel.formal.mimaflux.pl;

import edu.kit.kastel.formal.mimaflux.TokenedException;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.AssignStmContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.BinExpContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.BlockContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.FileContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.FunctionContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.GlobalContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.HaltStmContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.If0StmContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.IfCmpStmContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.LitExpContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.ParenExpContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.ReturnStmContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.UnExpContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.VarExpContext;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.WhileStmContext;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompilerVisitor extends MimaWhileBaseVisitor<Void> {

    private static final int MAX_NEST_LEVEL = 10;
    private List<String> lines = new ArrayList<>();

    private Set<String> globals = new HashSet<>();

    private List<String> locals = new ArrayList<>();
    private int labelCounter;

    private int nestLevel;

    @Override
    public Void visitFile(FileContext ctx) {
        comment("# Fixed variables");
        emit("SP: DS 0x80000");
        for (int i = 0; i <= 10; i++) {
            emit("X%d: DS 0", i);
        }
        emit("Y0 : DS 0");

        comment("# Global variables");
        int countGlobal = 0;
        for (GlobalContext gctx : ctx.global()) {
            String id = gctx.ID().getText();
            String val = "";
            if (gctx.val != null) {
                val = " " + gctx.val.getText();
            }
            emit("%s: DS%s", id, val);
            if(gctx.count != null) {
                int count = Integer.decode(gctx.count.getText());
                emit("* = " + (countGlobal + count));
                countGlobal += count;
            } else {
                countGlobal ++;
            }
        }

        comment("# Program start");
        emit("START: JMS main");
        emit("  HALT");

        for (FunctionContext fctx : ctx.function()) {
            visitFunction(fctx);
        }

        return null;
    }

    @Override
    public Void visitFunction(FunctionContext ctx) {
        comment("# Function " + ctx.name.getText());
        emit(ctx.name.getText() + ": DS 0 ; Ret value");
        locals.clear();
        for (Token arg : ctx.args) {
            locals.add(arg.getText());
        }
        for (Token loc : ctx.locs) {
            locals.add(loc.getText());
        }

        comment("Ret address to SP");
        emit("  LDV " + ctx.name.getText());
        emit("  STIV SP");

        visitBlock(ctx.block());
        return null;
    }

    @Override
    public Void visitHaltStm(HaltStmContext ctx) {
        commentStm(ctx);
        emit("  HALT");
        return null;
    }

    @Override
    public Void visitReturnStm(ReturnStmContext ctx) {
        commentStm(ctx);
        comment("SP[1] into X1");
        emit("  LDC 1");
        emit("  ADD SP");
        emit("  STV X1");
        comment("arg into accu");
        loadVar(ctx.ID().getText());
        comment("store result value and return");
        emit("  STIV X1");
        emit("  JIND SP");
        return null;
    }

    @Override
    public Void visitLitExp(LitExpContext ctx) {
        emit("  LDC " + ctx.getText());
        return null;
    }

    @Override
    public Void visitVarExp(VarExpContext ctx) {
        loadVar(ctx.ID().getText());
        return null;
    }

    @Override
    public Void visitUnExp(UnExpContext ctx) {
        nestLevel++;
        ctx.arg.accept(this);
        switch (ctx.op.getText()) {
            case "~": emit("  NOT"); break;
            case "*": emit("  RAR"); break;
            case "-":
                if(nestLevel > MAX_NEST_LEVEL) {
                    throw new TokenedException(ctx.op, "Nesting");
                }
                emit("  NOT");
                emit("  STV X" + nestLevel);
                emit("  LDC 1");
                emit("  ADD X" + nestLevel);
            default: throw new TokenedException(ctx.op, "Unknown");
        }
        nestLevel--;
        return null;
    }

    @Override
    public Void visitBinExp(BinExpContext ctx) {
        String myX = "X" + nestLevel;
        nestLevel++;
        ctx.arg1.accept(this);
        emit("  STV " + myX);
        ctx.arg2.accept(this);
        switch (ctx.op.getText()) {
            case "+": emit("  ADD " + myX); break;
            case "&": emit("  AND " + myX); break;
            case "|": emit("  OR " + myX); break;
            case "^": emit("  XOR " + myX); break;
            default:
                throw new TokenedException(ctx.op, "Unknown");
        }
        nestLevel--;
        return null;
    }

    @Override
    public Void visitAssignStm(AssignStmContext ctx) {
        commentStm(ctx);
        ctx.expr().accept(this);
        storeVar(ctx.target.getText());
        return null;
    }

    @Override
    public Void visitIf0Stm(If0StmContext ctx) {
        commentStm(ctx);
        ctx.arg1.accept(this);
        visitIfJMN(ctx.thenBlk, ctx.elseBlk);
        return null;
    }

    @Override
    public Void visitIfCmpStm(IfCmpStmContext ctx) {
        commentStm(ctx);
        nestLevel ++;
        switch (ctx.op.getText()) {
            case "=":
                ctx.arg1.accept(this);
                emit("  STV X0");
                ctx.arg2.accept(this);
                emit("  EQL X0");
                break;
            case "<=":
                ctx.arg1.accept(this);
                emit("  STV X0");
                ctx.arg2.accept(this);
                emit("  NOT");
                emit("  ADD X0");
                break;
        }
        nestLevel --;
        visitIfJMN(ctx.thenBlk, ctx.elseBlk);
        return null;
    }

    private void visitIfJMN(BlockContext thenBlk, BlockContext elseBlk) {
        String lblThen = "L" + (labelCounter++);
        String lblAfter = "L" + (labelCounter++);
        emit("  JMN " + lblThen);
        visitBlock(elseBlk);
        emit("  JMP " + lblAfter);
        emit(lblThen + ":");
        visitBlock(thenBlk);
        emit(lblAfter + ":");
    }

    @Override
    public Void visitWhileStm(WhileStmContext ctx) {
        commentStm(ctx);
        String lblLoop = "L" + (labelCounter++);
        String lblAfter = "L" + (labelCounter++);
        emit(lblLoop + ":");
        nestLevel ++;
        switch (ctx.op.getText()) {
            case "=":
                ctx.arg1.accept(this);
                emit("  STV X0");
                ctx.arg2.accept(this);
                emit("  EQL X0");
                break;
            case "<=":
                ctx.arg1.accept(this);
                emit("  STV X0");
                ctx.arg2.accept(this);
                emit("  NOT");
                emit("  ADD X0");
                break;
        }
        nestLevel --;
        emit("  NOT");
        emit("  JMN " + lblAfter);
        visitBlock(ctx.block());
        emit("  JMP " + lblLoop);
        emit(lblAfter + ":");
        return null;
    }

    //region infrastructure

    private void storeVar(String var) {
        int idx = locals.indexOf(var);
        if (idx == -1) {
            if (!globals.contains(var)) {
                System.err.println("UNKNOWN " + var);
            }
            emit("  STV " + var);
        } else {
            emit("  STV X1");
            emit("  LDC " + (idx + 1));
            emit("  ADD SP");
            emit("  STV X0");
            emit("  LDV X1");
            emit("  LDIV X0");
        }
    }

    private void storeVar(String from, String var) {
        int idx = locals.indexOf(var);
        if (idx == -1) {
            if (!globals.contains(var)) {
                System.err.println("UNKNOWN " + var);
            }
            emit("  LDV " + from);
            emit("  STV " + var);
        } else {
            emit("  LDC " + (idx + 1));
            emit("  ADD SP");
            emit("  STV Y0");
            emit("  LDV " + from);
            emit("  LDIV Y0");
        }
    }


    private void loadVar(String var) {
        int idx = locals.indexOf(var);
        if (idx == -1) {
            if (!globals.contains(var)) {
                System.err.println("UNKNOWN " + var);
            }
            emit("  LDV " + var);
        } else {
            emit("  LDC " + (idx + 1));
            emit("  ADD SP");
            emit("  STV Y0");
            emit("  LDIV Y0");
        }
    }

    private void loadVar(String var, String into) {
        loadVar(var);
        emit("  STV " + into);
    }

    public List<String> getCompilation() {
        return lines;
    }

    private void emit(String format, Object... args) {
        emit(String.format(format, args));
    }

    private void emit(String string) {
        lines.add(string);
    }

    private void comment(String msg) {
        if(msg.startsWith("# ")) {
            lines.add("");
            lines.add("; ------------------");
            lines.add("; " + msg.substring(2));
        } else
            lines.add("  ; " + msg);
    }

    private void commentStm(ParserRuleContext ctx) {
        comment("line " + ctx.getStart().getLine() + ": " + ctx.getClass().getSimpleName());
    }
}
