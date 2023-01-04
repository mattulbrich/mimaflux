package edu.kit.kastel.formal.mimaflux.gui;

/**
 * Options for representing numbers.
 */
public enum RepreState {

    /**
     * Hexadecimal
     */
    HEX("Hex"),

    /**
     * Decimal
     */
    DEC("Dec"),

    /**
     * Binary
     */
    BIN("Bin");

    private final String label;

    RepreState(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }
}
