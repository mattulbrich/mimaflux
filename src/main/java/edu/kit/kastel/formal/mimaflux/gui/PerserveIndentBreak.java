package edu.kit.kastel.formal.mimaflux.gui;

import javax.swing.*;
import javax.swing.text.Document;

import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * This Class adds the ability to keep the indentation on adding a new line.
 */
class PerserveIndentBreak extends AbstractAction {
    /**
     * {@code true} cleans the current indent in the line and adds a new line, {@code false} only removes the indent and stays in the line
     */
    private static final boolean ADD_NEWLINE_AFTER_WHITESPACE = true;

    private final BreakpointPane code;
    private final Action originalInsertLineBreak;

    public PerserveIndentBreak(BreakpointPane code, Action originalInsertLineBreak) {
        super();
        this.code = code;
        this.originalInsertLineBreak = originalInsertLineBreak;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            int caretPos = code.getCaretPosition();
            Document doc = code.getDocument();
            String text = doc.getText(0, caretPos);

            // current line is everything after the last, previous \n, or the entire line if there is no previous new line
            int lastNewlineIndex = text.lastIndexOf('\n');
            String currentLine = (lastNewlineIndex == -1) ? text : text.substring(lastNewlineIndex + 1);

            // loop thru all charactets in the current line until there is a non-whitespace symbol
            StringBuilder indent = new StringBuilder("\n");
            boolean containsText = false;
            for (char c : currentLine.toCharArray()) {
                if (c == ' ' || c == '\t') {
                    indent.append(c);
                } else {
                    containsText = true;
                    break;
                }
            }

            // check if the line contains text or is just made up of white space
            if (containsText) {
                // Insert the actual indent and afterwards the indent matching the previous line
                doc.insertString(caretPos, indent.toString(), null);
            } else if (currentLine.length() == 0) {
                // insert a new line on an empty line
                doc.insertString(caretPos, "\n", null);
            } else {
                // remove the current indent if only white space is present
                doc.remove(lastNewlineIndex + 1, currentLine.length());

                if (ADD_NEWLINE_AFTER_WHITESPACE) {
                    doc.insertString(caretPos, "\n", null);
                }
            }
        } catch (Exception ex) {
            // Fallback to default behavior if anything goes wrong
            if (originalInsertLineBreak != null) {
                originalInsertLineBreak.actionPerformed(e);
            }
        }
    }
}
