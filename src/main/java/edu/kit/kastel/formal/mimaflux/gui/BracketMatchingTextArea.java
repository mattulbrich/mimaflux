/*
 * This file is part of
 *    ivil - Interactive Verification on Intermediate Language
 *
 * Copyright (C) 2009-2012 Karlsruhe Institute of Technology
 *
 * The system is protected by the GNU General Public License.
 * See LICENSE.TXT (distributed with this file) for details.
 */
// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2009 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package edu.kit.kastel.formal.mimaflux.gui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Highlighter.HighlightPainter;

/**
 * The Class BracketMatchingTextArea provides a GUI TextArea component which
 * automatically highlights matching pairs of parentheses. It behaves like a
 * {@link JTextArea} object in every other respect.
 * 
 * <ul>
 * <li>The following characters are considered as opening parenthesis:
 * <code>( { &lt; [</code>
 * <li>The following characters are considered as closing parenthesis:
 * <code>) } &gt; ]</code>
 * </ul>
 * 
 * It is not checked whether the parenthesis are of the same type. Therefore,
 * <code>{x)</code> is highlighted as well.
 * 
 * @author mulbrich
 */
public class BracketMatchingTextArea extends JTextArea implements CaretListener {

    /**
     * The Constant serialVersionUID needed for serialisation reasons
     */
    private static final long serialVersionUID = 1649172317561172229L;
    
    /**
     * The Constant HIGHLIGHT_COLOR holds the color to be used for the highlighting frame.
     */
    private static final Color HIGHLIGHT_COLOR = Color.LIGHT_GRAY;
    
    /**
     * The Constant PAINTER is the painter which is used to draw the highlighting.
     */
    private static final HighlightPainter PAINTER = new BorderPainter();
    
    /**
     * The Constant OPENING_PARENS holds the characters which serve as opening parenthesis
     */
    private static final String OPENING_PARENS = "({[";
    
    /**
     * The Constant CLOSING_PARENS holds the characters which serve as closing parenthesis.
     */
    private static final String CLOSING_PARENS = ")}]";
    
    /**
     * The highlighter stores the highlights in an object which is used to denote the highlighting.
     */
    private Object theHighlight;
    
    
    /**
     * Constructs a new TextArea.  A default model is set, the initial string
     * is null, and rows/columns are set to 0.
     */
    public BracketMatchingTextArea() {
        super();
        init();
    }

    /**
     * Constructs a new JTextArea with the specified number of rows
     * and columns, and the given model.  All of the constructors
     * feed through this constructor.
     *
     * @param doc the model to use, or create a default one if null
     * @param text the text to be displayed, null if none
     * @param rows the number of rows >= 0
     * @param columns the number of columns >= 0
     * @exception IllegalArgumentException if the rows or columns
     *  arguments are negative.
     */
    public BracketMatchingTextArea(Document doc, String text, int rows,
            int columns) {
        super(doc, text, rows, columns);
        init();
    }

    /**
     * Constructs a new JTextArea with the given document model, and defaults
     * for all of the other arguments (null, 0, 0).
     *
     * @param doc  the model to use
     */
    public BracketMatchingTextArea(Document doc) {
        super(doc);
        init();
    }

    /**
     * Constructs a new empty TextArea with the specified number of
     * rows and columns.  A default model is created, and the initial
     * string is null.
     *
     * @param rows the number of rows >= 0
     * @param columns the number of columns >= 0
     * @exception IllegalArgumentException if the rows or columns
     *  arguments are negative.
     */
    public BracketMatchingTextArea(int rows, int columns) {
        super(rows, columns);
        init();
    }

    /**
     * Constructs a new TextArea with the specified text and number
     * of rows and columns.  A default model is created.
     *
     * @param text the text to be displayed, or null
     * @param rows the number of rows >= 0
     * @param columns the number of columns >= 0
     * @exception IllegalArgumentException if the rows or columns
     *  arguments are negative.
     */
    public BracketMatchingTextArea(String text, int rows, int columns) {
        super(text, rows, columns);
        init();
    }

    /**
     * Constructs a new TextArea with the specified text displayed.
     * A default model is created and rows/columns are set to 0.
     *
     * @param text the text to be displayed, or null
     */
    public BracketMatchingTextArea(String text) {
        super(text);
        init();
    }

    /*
     * Initialize the object. set the highlighting. add listeners
     */
    private void init() {
        addCaretListener(this);
        
        addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if(e.isControlDown() && e.getKeyChar() == '\n') {
                    fireActionPerformed();
                    e.consume();
                }
            }
        });
        
        setBorder(BorderFactory.createEtchedBorder());
        
        DefaultHighlighter highlight = new DefaultHighlighter();
        // highlight.setDrawsLayeredHighlights(false);
        setHighlighter(highlight);
        try {
            theHighlight = highlight.addHighlight(0, 0, PAINTER);
        } catch (BadLocationException e) {
            // may not happen even if document is empty
            throw new Error(e);
        }
    }

    /* 
     * check if the caret is on a paren and if so, find the corresponding partner.
     * update the highlighting if such a partner exists. 
     */
    public void caretUpdate(CaretEvent e) {
        try {
            int dot = getCaretPosition();
            String text = getText();
            char charOn = dot == text.length() ? 0 : text.charAt(dot);
            char charBefore = dot == 0 ? 0 : text.charAt(dot-1);
            int begin = -1;
            int end = -1;
            
            if(OPENING_PARENS.indexOf(charOn) != -1) {
                end = findMatchingClose(dot) + 1;
                begin = dot;
            } else if(CLOSING_PARENS.indexOf(charBefore) != -1) {
                end = dot;
                begin = findMatchingOpen(dot-1);
            }
            
            if(begin != -1 && end != -1) {
                assert begin < end : "begin=" + begin + " end=" + end;
                getHighlighter().changeHighlight(theHighlight, begin, end);
            } else {
                getHighlighter().changeHighlight(theHighlight, 0, 0);
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
            try {
                getHighlighter().changeHighlight(theHighlight, 0, 0);
            } catch (BadLocationException ex2) {
                // may not happen even if document is empty
                throw new Error(ex2);
            }
        }
    }

    /**
     * Find matching close paren.
     * 
     * Go through the string and find the closing partner. There may be other
     * open/close parens in between
     * 
     * @param dot
     *                position to start search from (must be an opening paren)
     * @return either the index of the closing partner or -1 if it does not
     *         exist.
     */
    private int findMatchingClose(int dot) {
        int count = 0;
        String text = getText();
        
        do {
            if(OPENING_PARENS.indexOf(text.charAt(dot)) != -1)
                count ++;
            else if(CLOSING_PARENS.indexOf(text.charAt(dot)) != -1)
                count --;
        
            if(count == 0)
                return dot;
            
            dot ++;
        } while(dot < text.length());
        return -1;
    }

    /**
     * Find matching open paren.
     * 
     * Go backward through the string and find the opening partner. There may be
     * other open/close parens in between
     * 
     * @param dot
     *                position to start search from (must be a closing paren)
     * @return either the index of the opening partner or -1 if it does not
     *         exist.
     */
    private int findMatchingOpen(int dot) {
        int count = 0;
        String text = getText();
        
        do {
            if(OPENING_PARENS.indexOf(text.charAt(dot)) != -1)
                count --;
            else if(CLOSING_PARENS.indexOf(text.charAt(dot)) != -1)
                count ++;
        
            if(count == 0)
                return dot;
            
            dot --;
        } while(dot >= 0);
        return -1;
    }
    
    /* 
     * for testing
     */
    public static void main(String[] args) {
        JFrame f = new JFrame("Test bracket matching text area");
        BracketMatchingTextArea uut = new BracketMatchingTextArea("nothing", 10, 10);
        uut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.err.println(e);
            }
        });
        f.getContentPane().add(uut);
        f.setSize(200, 200);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    /**
     * The Class BorderPainter is a simple highlight painter that just draws a rectangle around the selection.
     * 
     */
    static private class BorderPainter implements HighlightPainter {

        /**
         * The code is copied from {@link DefaultHighlighter#DefaultPainter
         * #paint(Graphics)}.
         */
        public void paint(Graphics g, int offs0, int offs1, Shape bounds,
                JTextComponent c) {
            
            // dont render if empty
            if(offs0 == offs1)
                return;
            
            try {
                // --- determine locations ---
                TextUI mapper = c.getUI();
                Rectangle p0 = mapper.modelToView(c, offs0);
                Rectangle p1 = mapper.modelToView(c, offs0+1);
                Rectangle q0 = mapper.modelToView(c, offs1-1);
                Rectangle q1 = mapper.modelToView(c, offs1);
                
                Rectangle p = p0.union(p1);
                Rectangle q = q0.union(q1);

                g.setColor(HIGHLIGHT_COLOR);
                g.drawRect(p.x, p.y, p.width-1, p.height-1);
                g.drawRect(q.x, q.y, q.width-1, q.height-1);
            } catch (BadLocationException e) {
                // can't render
            }
        }
    }
    
    public void addActionListener(ActionListener actionListener) {
        listenerList.add(ActionListener.class, actionListener);
    }
    
    public void removeActionListener(ActionListener actionListener) {
        listenerList.remove(ActionListener.class, actionListener);
    }
    
    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created.
     * The listener list is processed in last to
     * first order.
     * @see EventListenerList
     * @see JTextField#fireActionPerformed()
     */
    protected void fireActionPerformed() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        int modifiers = 0;
        AWTEvent currentEvent = EventQueue.getCurrentEvent();
        if (currentEvent instanceof InputEvent) {
            modifiers = ((InputEvent)currentEvent).getModifiers();
        } else if (currentEvent instanceof ActionEvent) {
            modifiers = ((ActionEvent)currentEvent).getModifiers();
        }
        ActionEvent e =
            new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                            "",
                            EventQueue.getMostRecentEventTime(), modifiers);
                            
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }          
        }
    }

}
