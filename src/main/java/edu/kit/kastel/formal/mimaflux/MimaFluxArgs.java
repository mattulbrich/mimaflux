package edu.kit.kastel.formal.mimaflux;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

import java.util.List;

public class MimaFluxArgs {
    record Range(int from, int to) {
    }

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
            description = "Comma-separated list of memory ranges to be printed after finishing the program",
            converter = RangeConverter.class
    )
    public List<Range> printRanges;

    @Parameter(names = "-maxSteps", description = "Maximum number of steps to be recorded by flux compensator")
    public int maxSteps = 1000;
}
