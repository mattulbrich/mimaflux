package edu.kit.kastel.formal.mimaflux;

import edu.kit.kastel.formal.mimaflux.MimaAsmParser.FileContext;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class Interpreter {
    private final String fileName;
    private String fileContent;
    private Map<String, Integer> labelMap;
    private List<Command> commands;

    public Interpreter(String fileName) {
        this.fileName = fileName;
    }

    public void parse() throws IOException {

        this.fileContent = Files.readString(Paths.get(fileName));

        CharStream input = CharStreams.fromString(fileContent);
        MimaAsmLexer lexer = new MimaAsmLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MimaAsmParser parser = new MimaAsmParser(tokens);
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RuntimeException("line " + line + ":" +
                        charPositionInLine + ": " + msg);
            }
        });
        FileContext content = parser.file();
        ProgramVisitor pv = new ProgramVisitor();
        content.accept(pv);

        this.commands = pv.getCommands();
        LabelResolver lr = new LabelResolver();
        lr.resolve(commands);
        labelMap = lr.getLabelMap();

    }

    public Timeline makeTimeline() {

        TimelineBuilder builder = new TimelineBuilder(fileContent, labelMap, commands);
        State state = builder.exposeState();

        if (MimaFlux.mmargs.verbose) {
            System.out.println(" ---- initial state");
            state.printToConsole();
        }

        int count = 0;
        loop: while(builder.size() < MimaFlux.mmargs.maxSteps) {
            int ir = state.get(state.get(State.IAR));
            int arg = ir & Constants.ADDRESS_MASK;
            int opcode = ir >> 20;
            int tmp;
            switch(opcode) {
                case 0x0: builder.set(State.ACCU, ir); builder.incIAR(); break;
                case 0x1: builder.set(State.ACCU, state.get(arg)); builder.incIAR(); break;
                case 0x2: builder.set(arg, state.get(State.ACCU)); builder.incIAR(); break;
                case 0x3: op(builder, arg, Integer::sum); break;
                case 0x4: op(builder, arg, (x,y) -> x&y); break;
                case 0x5: op(builder, arg, (x,y) -> x|y); break;
                case 0x6: op(builder, arg, (x,y) -> x^y); break;
                case 0x7: op(builder, arg, (x,y) -> x==y?-1:0); break;
                case 0x8: builder.set(State.IAR, arg); break;
                case 0x9:
                    if((state.get(State.ACCU) & Constants.SIGNBIT) != 0)  {
                        builder.set(State.IAR, arg);
                    } else {
                        builder.incIAR();
                    }
                    break;
                case 0xa:
                    tmp = state.get(arg);
                    builder.set(State.ACCU, state.get(tmp));
                    builder.incIAR();
                    break;
                case 0xb:
                    tmp = state.get(arg);
                    builder.set(tmp, state.get(State.ACCU));
                    builder.incIAR();
                    break;
                case 0xc:
                    builder.set(arg, state.get(State.IAR) + 1);
                    builder.set(State.IAR, (arg + 1) & Constants.ADDRESS_MASK);
                    break;
                case 0xd:
                    tmp = state.get(arg);
                    builder.set(State.IAR, state.get(tmp));
                    break;

                case 0xf: switch(arg) {
                    case 0x00000: builder.commit(); break loop;
                    case 0x10000:
                        builder.set(State.ACCU, (~state.get(State.ACCU)) & Constants.VALUE_MASK);
                        builder.incIAR();
                        break;
                    case 0x20000:
                        tmp = state.get(State.ACCU);
                        tmp = (tmp >> 1) | ((tmp & 1) << (Constants.VALUE_WIDTH - 1));
                        builder.set(State.ACCU, tmp);
                        builder.incIAR();
                        break;
                    default: builder.commit(); break loop;
                }
                break;
                default: builder.commit(); break loop;
            }
            builder.commit();
            if (MimaFlux.mmargs.verbose) {
                System.out.println(" ---- After step " + builder.size());
                state.printToConsole();
            }
        }
        MimaFlux.log(" ---- Finished interpretation");

        return builder.build();
    }

    private void op(TimelineBuilder builder, int arg, BinaryIntFunction fun) {
        State state = builder.exposeState();
        int op1 = state.get(State.ACCU);
        int op2 = state.get(arg);
        int res = fun.apply(op1, op2) & Constants.VALUE_MASK;
        builder.set(State.ACCU, res);
        builder.incIAR();
    }
}
