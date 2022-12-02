package edu.kit.kastel.formal.mimaflux.gui;

import javax.swing.JFormattedTextField.AbstractFormatter;
import javax.swing.text.DefaultFormatter;
import javax.swing.text.DefaultFormatterFactory;
import java.text.ParseException;

class HexFormatter extends DefaultFormatter {

    public static class Factory extends DefaultFormatterFactory {
        @Override
        public AbstractFormatter getDefaultFormatter() {
            return new HexFormatter();
        }
    }


    @Override
    public Object stringToValue(String text) throws ParseException {
        try {
            int result = Integer.valueOf(text, 16);
            return result & 0xff;
        } catch (NumberFormatException nfe) {
            throw new ParseException(text, 0);
        }
    }

    @Override
    public String valueToString(Object value) throws ParseException {
        return String.format("%02X", value);
    }
}
