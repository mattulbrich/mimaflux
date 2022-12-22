package edu.kit.kastel.formal.mimaflux.gui;

import javax.swing.*;

/**
 * UI component for choosing a number representation.
 */
public class RepreComboBox extends JComboBox<RepreComboBox.RepreState> {

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

    /**
     * Creates a new {@link RepreComboBox} UI component.
     */
    public RepreComboBox() {
        super(RepreState.values());
    }
}
