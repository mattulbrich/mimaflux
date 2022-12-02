package edu.kit.kastel.formal.mimaflux;

import edu.kit.kastel.formal.mimaflux.MimaAsmParser.FileContext;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        // BailOutErrorStrategy errorStrategy = new BailOutErrorStrategy();
        // parser.setErrorHandler(errorStrategy)
        FileContext content = parser.file();
        ProgramVisitor pv = new ProgramVisitor();
        content.accept(pv);

        this.commands = pv.getCommands();
        LabelResolver lr = new LabelResolver();
        lr.resolve(commands);
        labelMap = lr.getLabelMap();

    }

    public Timeline makeTimeline() {
        State state = new State(commands);

        int start = labelMap.getOrDefault(Constants.START_LABEL, 0);

        state.set(State.IAR, start);

        Timeline timeline = new Timeline(fileContent, commands, state);

        if (MimaFlux.mmargs.verbose) {
            System.out.println(" ---- initial state");
            state.printToConsole();
        }

        int count = 0;
        loop: while(timeline.size() < MimaFlux.mmargs.maxSteps) {
            int ir = state.get(state.get(State.IAR));
            int arg = ir & Constants.ADDRESS_MASK;
            int opcode = ir >> 20;
            int tmp;
            switch(opcode) {
                case 0x0: timeline.set(State.ACCU, ir); timeline.incIAR(); break;
                case 0x1: timeline.set(State.ACCU, state.get(arg)); timeline.incIAR(); break;
                case 0x2: timeline.set(arg, state.get(State.ACCU)); timeline.incIAR(); break;
                case 0x3: op(timeline, arg, Integer::sum); break;
                case 0x4: op(timeline, arg, (x,y) -> x&y); break;
                case 0x5: op(timeline, arg, (x,y) -> x|y); break;
                case 0x6: op(timeline, arg, (x,y) -> x^y); break;
                case 0x7: op(timeline, arg, (x,y) -> x==y?-1:0); break;
                case 0x8: timeline.set(State.IAR, arg); break;
                case 0x9:
                    if((state.get(State.ACCU) & Constants.SIGNBIT) != 0)  {
                        timeline.set(State.IAR, arg);
                    } else {
                        timeline.incIAR();
                    }
                    break;
                case 0xa:
                    tmp = state.get(arg);
                    timeline.set(State.ACCU, state.get(tmp));
                    timeline.incIAR();
                    break;
                case 0xb:
                    tmp = state.get(arg);
                    timeline.set(state.get(tmp), state.get(State.ACCU));
                    timeline.incIAR();
                    break;
                // case 0xc: currently not supported
                case 0xd:
                    tmp = state.get(arg);
                    timeline.set(State.IAR, state.get(tmp));
                    break;

                case 0xf: switch(arg) {
                    case 0x00000: timeline.commit(); break loop;
                    case 0x10000: timeline.set(State.ACCU, ~state.get(State.ACCU)); timeline.incIAR(); break;
                    case 0x20000:
                        tmp = state.get(State.ACCU);
                        tmp = (tmp >> 1) | ((tmp & 1) << (Constants.VALUE_WIDTH - 1));
                        timeline.set(State.ACCU, tmp);
                        timeline.incIAR();
                        break;
                    default: timeline.commit(); break loop;
                }
                break;
                default: timeline.commit(); break loop;
            }
            timeline.commit();
            if (MimaFlux.mmargs.verbose) {
                System.out.println(" ---- After step " + timeline.getPosition());
                state.printToConsole();
            }
        }

        return timeline;
    }

    private void op(Timeline timeline, int arg, BinaryIntFunction fun) {
        State state = timeline.exposeState();
        int op1 = state.get(State.ACCU);
        int op2 = state.get(arg);
        int res = fun.apply(op1, op2) & Constants.VALUE_MASK;
        timeline.set(State.ACCU, res);
        timeline.incIAR();
    }
}
