package edu.kit.kastel.formal.mimaflux;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import edu.kit.kastel.formal.mimaflux.gui.GUI;

import javax.swing.text.BadLocationException;
import java.util.List;

public class MimaFlux {

    public static MimaFluxArgs mmargs;

    public static void main(String[] args) throws Exception {
        try {
            Timeline timeline = null;
            mmargs = new MimaFluxArgs();
            JCommander.newBuilder()
                    .addObject(mmargs)
                    .build()
                    .parse(args);

            if (mmargs.fileName == null) {
                if(mmargs.autoRun) {
                    exit("A filename must be provided in -run mode.");
                }
            } else {
                Interpreter interpreter = new Interpreter(mmargs.fileName);
                interpreter.parse();
                timeline = interpreter.makeTimeline();

            }

            if(mmargs.autoRun) {
                timeline.exposeState().printToConsole();
            } else {
                GUI gui = new GUI(timeline);
                gui.setVisible(true);
            }
        } catch (Exception ex) {
            exit(ex);
        }
    }

    public static void log(String msg) {
        if (mmargs.verbose) {
            System.err.println(msg);
        }
    }

    public static void exit(String msg) {
        System.err.println(msg);
        if (mmargs.verbose) {
            new Throwable().printStackTrace();
        }
        System.exit(1);
    }

    public static void exit(Exception exception) {
        if (mmargs.verbose) {
            exception.printStackTrace();
        } else {
            System.err.println(exception.getMessage());
        }
        System.exit(1);
    }

    public static void logStacktrace(Exception exception) {
        if (mmargs.verbose) {
            exception.printStackTrace();
        }
    }
}

class MimaFluxArgs {
    record Range(int from, int to) { }

    static class RangeConverter implements IStringConverter<Range> {
        @Override
        public Range convert(String value) {
            String[] parts = value.trim().split(" *- *", 2);
            int from = Integer.decode(parts[0]);
            if (parts.length == 1) {
                return new Range(from, from);
            } else {
                int to = Integer.decode(parts[1]);
                return new Range(from, to);
            }
        }
    }

    @Parameter(names = "-verbose", description = "Give more logs on the console")
    public boolean verbose = false;

    @Parameter
    public String fileName;

    @Parameter(names = { "-run", "-r" }, description = "Run without graphical user interface")
    public boolean autoRun;

    @Parameter(names = { "-print", "-p" },
            description = "Comma-separated list of memory ranges to be printed after finishing the program",
            converter = RangeConverter.class
    )
    public List<Range> printRanges;

    @Parameter(names = "-maxSteps", description = "Maximum number of steps to be recorded by flux compensator")
    public int maxSteps = 1000;
}
