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

    @Parameter
    public String fileName;

    @Parameter(names = {"-run", "-r"}, description = "Run without graphical user interface")
    public boolean autoRun;

    @Parameter(names = {"-print", "-p"},
            description = "Comma-separated list of memory ranges to be printed after finishing the program [only in -run mode]",
            converter = RangeConverter.class
    )
    public List<Range> printRanges;

    @Parameter(names = {"-set", "-s"},
            description = "..."
    )
    public List<String> assignments;

    @Parameter(names = {"-test", "-t"},
            description = "..."
    )
    public List<String> tests;

    @Parameter(names = "-maxSteps", description = "Maximum number of steps to be recorded by mima flux")
    public int maxSteps = 1000;
}
