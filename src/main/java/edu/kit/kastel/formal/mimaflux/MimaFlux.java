package edu.kit.kastel.formal.mimaflux;

import com.beust.jcommander.JCommander;
import edu.kit.kastel.formal.mimaflux.gui.GUI;

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

