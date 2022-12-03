package edu.kit.kastel.formal.mimaflux.pl;

import edu.kit.kastel.formal.mimaflux.TokenedException;
import edu.kit.kastel.formal.mimaflux.pl.MimaWhileParser.ArrayExpContext;
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

    public static final int STACK_BOUND = 10;
    private List<String> lines = new ArrayList<>();

    private Set<String> globals = new HashSet<>();

    private List<String> locals = new ArrayList<>();
    private int labelCounter;

    private BoundedStack boundedStack = new BoundedStack(STACK_BOUND);

    @Override
    public Void visitFile(FileContext ctx) {
        comment("# Internal and stack variables");
        emit("SP: DS 0x80000");
        for (int i = 0; i <= STACK_BOUND; i++) {
            emit("X%d: DS 0", i);
        }

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
        comment("return at function end, value is indetermined");
        emit("  JIND SP");
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
        String x0 = boundedStack.current();
        comment("SP[1] into X1");
        emit("  LDC 1");
        emit("  ADD SP");
        emit("  STV " + x0);
        comment("arg into accu");
        loadVar(ctx.ID().getText());
        comment("store result value and return");
        emit("  STIV " + x0);
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
        ctx.arg.accept(this);
        String x0 = boundedStack.current();
        switch (ctx.op.getText()) {
            case "~": emit("  NOT"); break;
            case "*": emit("  RAR"); break;
            case "-":
                emit("  NOT");
                emit("  STV " + x0);
                emit("  LDC 1");
                emit("  ADD " + x0);
            default: throw new TokenedException(ctx.op, "Unknown");
        }
        return null;
    }

    @Override
    public Void visitBinExp(BinExpContext ctx) {
        ctx.arg1.accept(this);
        String x0 = boundedStack.inc();
        emit("  STV " + x0);
        ctx.arg2.accept(this);
        switch (ctx.op.getText()) {
            case "+": emit("  ADD " + x0); break;
            case "&": emit("  AND " + x0); break;
            case "|": emit("  OR " + x0); break;
            case "^": emit("  XOR " + x0); break;
            default:
                throw new TokenedException(ctx.op, "Unknown");
        }
        boundedStack.dec();
        return null;
    }

    @Override
    public Void visitArrayExp(ArrayExpContext ctx) {
        commentStm(ctx);
        ctx.expr().accept(this);
        String myX = boundedStack.inc();
        emit("  STV " + myX);
        loadVar(ctx.ID().getText());
        emit("  ADD " + myX);
        emit("  STV " + myX);
        emit("  LDIV " + myX);
        boundedStack.dec();
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
        ctx.arg1.accept(this);
        String x0 = boundedStack.inc();
        emit("  STV " + x0);
        ctx.arg2.accept(this);

        switch (ctx.op.getText()) {
            case "=":
                emit("  EQL " + x0);
                break;
            case "<=":
                emit("  NOT");
                emit("  ADD " + x0);
                break;
        }
        boundedStack.dec();
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
        ctx.arg1.accept(this);
        String x0 = boundedStack.inc();
        emit("  STV " + x0);
        ctx.arg2.accept(this);

        switch (ctx.op.getText()) {
            case "=":
                emit("  EQL X0");
                break;
            case "<=":
                emit("  NOT");
                emit("  ADD X0");
                break;
        }
        boundedStack.dec();
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
            String x0 = boundedStack.current();
            String x1 = boundedStack.current(1);
            emit("  STV " + x0);
            emit("  LDC " + (idx + 1));
            emit("  ADD SP");
            emit("  STV " + x0);
            emit("  LDV " + x1);
            emit("  LDIV " + x0);
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
            String x0 = boundedStack.current();
            emit("  LDC " + (idx + 1));
            emit("  ADD SP");
            emit("  STV " + x0);
            emit("  LDIV " + x0);
        }
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
