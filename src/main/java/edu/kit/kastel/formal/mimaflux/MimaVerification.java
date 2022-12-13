package edu.kit.kastel.formal.mimaflux;

import edu.kit.kastel.formal.mimaflux.TestSpecParser.FileContext;
import edu.kit.kastel.formal.mimaflux.TestSpecParser.LabelSpecContext;
import edu.kit.kastel.formal.mimaflux.TestSpecParser.SpecContext;
import edu.kit.kastel.formal.mimaflux.TestSpecParser.TestContext;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.io.IOException;
import java.util.List;

public class MimaVerification {
    private String verifyFilename;
    private String fileName;

    public int verify(String verifyFilename, String fileName) throws IOException {
        this.verifyFilename = verifyFilename;
        this.fileName = fileName;

        FileContext file = parse(verifyFilename);
        log("Verifying from " + verifyFilename);
        int result = 0;
        for (TestContext testContext : file.test()) {
            result += verifyTest(testContext);
        }
        return result;
    }

    public void setInitialValues(String verifyFilename, String testcase, Interpreter interpreter) throws IOException {
        interpreter.getLabelMap().put("_accu", State.ACCU);
        interpreter.getLabelMap().put("_iar", State.IAR);
        FileContext file = parse(verifyFilename);
        for (TestContext testContext : file.test()) {
            if(testContext.name.getText().equals(testcase)) {
                setInitialValues(testContext.labels, testContext.pre, interpreter);
                return;
            }
        }
        MimaFlux.exit(String.format("Testcase %s not found in %s.", testcase, verifyFilename));
    }

    private int verifyTest(TestContext testContext) {
        log("------------------");
        String name = testContext.name.getText();
        log("TEST CASE: " + name);
        try {
            Interpreter interpreter = new Interpreter();
            interpreter.parseFile(fileName);
            interpreter.getLabelMap().put("_accu", State.ACCU);
            interpreter.getLabelMap().put("_iar", State.IAR);
            setInitialValues(testContext.labels, testContext.pre, interpreter);
            Timeline timeline = interpreter.makeTimeline();
            timeline.setPosition(timeline.countStates() - 1);
            return checkPostConditions(testContext, interpreter, timeline);
        } catch (Exception exception) {
            log(" ... Exception (try -verbose)");
            MimaFlux.logStacktrace(exception);
            return 1;
        }
    }

    private int checkPostConditions(TestContext testContext, Interpreter interpreter, Timeline timeline) {
        for (SpecContext specContext : testContext.post) {
            String addr = specContext.addr.getText();
            String valStr = specContext.val.getText();
            Integer resolved = interpreter.getLabelMap().get(addr);
            if (resolved == null) {
                resolved = Integer.decode(addr);
            }
            Integer val = Integer.decode(valStr);

            log(" Checking: " + addr + " = " + valStr);

            int observed = timeline.exposeState().get(resolved);
            if (observed != val) {
                log(String.format("  ... violated. Expected value %d (0x%x) at address %s, but observed %d (0x%x).",
                        val, val, addr, observed, observed));
                log("  Try invoking mimaflux with '-loadTest " + verifyFilename + "#" +
                        testContext.name.getText() + " " + fileName + "'");
                log("Test failed.");
                return 1;
            } else {
                log(" ... checked.");
            }
        }
        return 0;
    }

    private void setInitialValues(List<LabelSpecContext> labelSpecs, List<SpecContext> pre, Interpreter interpreter) {
        for (LabelSpecContext labelSpec : labelSpecs) {
            String label = labelSpec.label.getText();
            String valStr = labelSpec.val.getText();
            Integer value = Integer.decode(valStr);
            log(" Setting: " + label + " -> " + valStr);
            interpreter.addLabelValue(label, value & Constants.ADDRESS_MASK);
        }
        for (SpecContext specContext : pre) {
            String addr = specContext.addr.getText();
            String valStr = specContext.val.getText();
            Integer resolved = interpreter.getLabelMap().get(addr);
            if (resolved == null) {
                resolved = Integer.decode(addr);
            }
            Integer val = Integer.decode(valStr);
            log(" Setting: " + addr + " := " + valStr);
            interpreter.addPresetValue(resolved, val);
        }
    }

    private void log(String msg) {
        System.out.println(msg);
    }

    private FileContext parse(String filename) throws IOException {

        CharStream input = CharStreams.fromFileName(filename);
        TestSpecLexer lexer = new TestSpecLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TestSpecParser parser = new TestSpecParser(tokens);
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RuntimeException("line " + line + ":" +
                        charPositionInLine + ": " + msg);
            }
        });
        FileContext content = parser.file();
        return content;
    }

}
