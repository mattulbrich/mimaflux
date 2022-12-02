/*
 * This file is part of
 *    ivil - Interactive Verification on Intermediate Language
 *
 * Copyright (C) 2009-2012 Karlsruhe Institute of Technology
 *
 * The system is protected by the GNU General Public License.
 * See LICENSE.TXT (distributed with this file) for details.
 */
package edu.kit.kastel.formal.mimaflux.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

/**
 * A simple border that can be used to add linenumbers to text areas.
 * 
 * It paints a line to the left of the content and the linenumbers left of the
 * line. Its size adapts to the length of the text in the text area.
 * 
 * <p>
 * Only works if {@link JTextArea#getLineWrap()} returns false.
 */
public class LineNrBorder extends EmptyBorder {

    /**
     * The space left and right of the line
     */
    private static final int SEPARATION = 4;
    
    /**
     * The minimal width for the border.
     */
    private static final int MIN_WIDTH = 20;

    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = 1461567609517301384L;

    /**
     * The color to paint in
     */
    private Color color;
    
    /**
     * The current left inset is based on that number of digits
     */
    private int numberOfDigits = 0;

    /**
     * Instantiates a new line-no-border.
     * 
     * @param color
     *            the color to paint the numbers in.
     */
    public LineNrBorder(Color color) {
        super(0, 33, 0, 0);
        this.color = color;
    }

    /*
     * recalculate the left inset. Possible revalidate and repaint the textArea.
     */
    private void calcLeftInset(Graphics g, JTextArea textArea) {
        int maxLine = textArea.getLineCount();
        
        int l = maxLine;
        int digits = 0;
        while(l > 0) {
            digits ++;
            l /= 10;
        }
        
        if(digits != numberOfDigits) {
            FontMetrics fm = g.getFontMetrics();
            left = fm.stringWidth(Integer.toString(maxLine)) + 3*SEPARATION;
            left = Math.max(left, MIN_WIDTH);
            textArea.revalidate();
            textArea.repaint();
            numberOfDigits = digits;
        }            
    }

    /*
     * test
     */
    public static void main(String[] args) {
        JFrame f = new JFrame();
        JTextArea ta = new JTextArea();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            sb.append("\n");
        }
        ta.setText(sb.toString());
        ta.setBorder(new LineNrBorder(Color.lightGray));
        f.add(new JScrollPane(ta));
        f.setVisible(true);
        f.setSize(300, 300);
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width,
            int height) {
        
        assert c instanceof JTextArea : 
                "This works only with textareas at the moment";
        JTextArea textArea = (JTextArea) c;
        calcLeftInset(g, textArea);
    	
    	Graphics2D g2 = (Graphics2D)g;
    	g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
    			RenderingHints.VALUE_ANTIALIAS_ON);


        g.setFont(c.getFont());
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        int step = fm.getHeight();
        int descent = fm.getDescent();

        int maxLine = textArea.getLineCount();
        
        for (int i = step, line = 1; line <= maxLine; i += step, line++) {
            String lineNoStr = Integer.toString(line);
            int w = fm.stringWidth(lineNoStr);
            g.drawString(lineNoStr, left-2*SEPARATION - w, i - descent);
        }
        g.drawLine(left-SEPARATION, 0, left-SEPARATION, height);

    }

    /**
     * Gets the color of the border
     * 
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets the color of the border.
     * 
     * @param color
     *            the new color
     */
    public void setColor(Color color) {
        this.color = color;
    }
}
