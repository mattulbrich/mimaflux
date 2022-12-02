package edu.kit.kastel.formal.mimaflux;

import java.awt.*;

public class Constants {
    public static final String START_LABEL = "START";

    public static final int VALUE_WIDTH = 24;
    public static final int VALUE_RANGE = 1 << VALUE_WIDTH;
    public static final int VALUE_MASK = VALUE_RANGE - 1;
    public static final int SIGNBIT = 1 << (VALUE_WIDTH - 1);
    public static final String UNKNOWN_OPCODE = "???";
    public static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);

    public static final int ADDRESS_WIDTH = 20;
    public static final int ADDRESS_RANGE = 1 << ADDRESS_WIDTH;
    public static final int ADDRESS_MASK = ADDRESS_RANGE - 1;
}
