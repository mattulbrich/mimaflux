/*
 * This file is part of the tool MimaFlux.
 * https://github.com/mattulbrich/mimaflux
 *
 * MimaFlux is a time travel debugger for the Minimal Machine
 * used in Informatics teaching at a number of schools.
 *
 * The system is protected by the GNU General Public License Version 3.
 * See the file LICENSE in the main directory of the project.
 *
 * (c) 2016-2022 Karlsruhe Institute of Technology
 *
 * Adapted for Mima by Mattias Ulbrich
 */
package edu.kit.kastel.formal.mimaflux;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import edu.kit.kastel.formal.mimaflux.gui.GUI;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.List;

public class MimaFlux {

    public static MimaFluxArgs mmargs;

    public static void main(String[] args) throws Exception {
        try {
            Timeline timeline = null;
            mmargs = new MimaFluxArgs();
            JCommander jc = JCommander.newBuilder()
                    .addObject(mmargs)
                    .build();
            jc.parse(args);

            if (mmargs.help) {
                jc.usage();
                System.exit(0);
            }

            if (mmargs.fileName == null) {
                if (mmargs.autoRun) {
                    exit("A filename must be provided in -run mode.");
                }
            } else {
                Interpreter interpreter = new Interpreter();
                interpreter.parseFile(mmargs.fileName);
                if(mmargs.autoRun) {
                    setInitialValues(mmargs.assignments, interpreter);
                }
                timeline = interpreter.makeTimeline();
            }

            if (mmargs.autoRun) {
                timeline.setPosition(timeline.countStates() - 1);
                timeline.exposeState().printToConsole(timeline.getLabelMap());
                ensureTests(timeline);
                System.exit(0);
            } else {
                GUI gui = new GUI(timeline);
                gui.setVisible(true);
            }
        } catch (NoSuchFileException ex) {
            exit(new IOException("File not found: " + ex.getMessage(), ex));
        } catch (ParameterException parameterException) {
            System.err.println(parameterException.getMessage());
            parameterException.usage();
        } catch (Exception ex) {
            exit(ex);
        }
    }

    private static void ensureTests(Timeline timeline) {
        if (mmargs.tests == null) {
            return;
        }

        for (String test : mmargs.tests) {
            try {
                System.err.println("Checking " + test);
                String[] parts = test.trim().split(" *= *");
                if (parts.length != 2) {
                    throw new IllegalArgumentException();
                }
                Integer resolved = timeline.getLabelMap().get(parts[0]);
                if (resolved == null) {
                    resolved = Integer.decode(parts[0]);
                }
                Integer val = Integer.decode(parts[1]);

                int observed = timeline.exposeState().get(resolved);
                if(observed != val) {
                    System.err.printf(" ... violated. Expected value %d (0x%x) at address %s, but observed %d (0x%x).",
                            val, val, parts[0], observed, observed);
                    exit("Test failed.");
                } else {
                    System.err.println(" ... checked.");
                }
            } catch (Exception exception) {
                logStacktrace(exception);
                throw new IllegalArgumentException("Wrong test specification: " + test);
            }
        }
    }

    private static void setInitialValues(List<String> assignments, Interpreter interpreter) {
        if (assignments == null) {
            return;
        }
        for (String assignment : assignments) {
            try {
                String[] parts = assignment.trim().split(" *= *");
                if (parts.length != 2) {
                    throw new IllegalArgumentException();
                }
                Integer resolved = interpreter.getLabelMap().get(parts[0]);
                if (resolved == null) {
                    resolved = Integer.decode(parts[0]);
                }
                Integer val = Integer.decode(parts[1]);
                interpreter.addPresetValue(resolved, val);
            } catch (Exception exception) {
                logStacktrace(exception);
                throw new IllegalArgumentException("Wrong set specification: " + assignment);
            }
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

