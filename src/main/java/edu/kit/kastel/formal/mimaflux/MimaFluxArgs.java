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

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

import java.util.List;

public class MimaFluxArgs {
    private static final String INDENT = "      ";

    record Range(int from, int to) {
    }

    @Parameter(names = { "-help", "-h" }, help = true, description = "Show this usage text")
    public boolean help;

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

    @Parameter(description = "[<filename>]\n" + INDENT +
            "The name of the assembly file to be loaded into the debugger. " +
            "In -run mode, this file argument must be provided, in GUI mode it is optional.")
    public String fileName;

    @Parameter(names = {"-run", "-r"}, description = "Run without graphical user interface")
    public boolean autoRun;

    @Parameter(names = {"-print", "-p"},
            description = "Arg: <addr>-<addr>.\n" + INDENT +
                    "Print the provided memory ranges after finishing the program. " +
                    "Can be specified multiple times for multiple ranges. [only in -run mode]",
            converter = RangeConverter.class
    )
    public List<Range> printRanges;

    @Parameter(names = {"-set", "-s"},
            description = "Arg: <addr>=<val>.\n" + INDENT +
                    "Set a memory location to a specified value. The address addr and " +
                    "the value val can be a number or a label (defined in the assembly code). " +
                    "Can be specified multiple times for multiple ranges."
    )
    public List<String> assignments;

    @Parameter(names = {"-test", "-t"},
            description = "Arg: <addr>=<val>.\n" + INDENT +
                    "Specify a test to be checked at the end of the run. The address and " +
                    "the value val can be a number or a label (defined in the assembly code). " +
                    "Can be specified multiple times for multiple ranges. If at least one address " +
                    "specified in a test contains a different than the specified value, the program " +
                    "terminates with a non-zero exit code [only in -run mode]\""
    )
    public List<String> tests;

    @Parameter(names = {"-loadTest", "-l"},
             description = "Arg: <file>#<name>.\n" + INDENT +
                     "Load the test case named <name> specified in <file> into the GUI.")
    public String loadTest;

    @Parameter(names = {"-verify", "-v"},
            description = "Arg: <filename>.\n" + INDENT +
                    "Run all tests specified in the argument file. A test case " +
                    "is specified as follows:\n" + INDENT +
                    "  <name>: <in1>=<val> <in2>=<val> ... ==> <out1>=<val> <out2>=<val>\n" + INDENT +
                    "[implies -run mode]"
    )
    public String verifyFile;

    @Parameter(names = "-gbi_hack")
    public boolean gbi_hack;

    @Parameter(names = "-maxSteps", description = "Maximum number of steps to be recorded by mima flux")
    public int maxSteps = 1000;
}
