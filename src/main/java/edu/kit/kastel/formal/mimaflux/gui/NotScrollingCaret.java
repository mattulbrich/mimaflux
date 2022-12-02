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

import java.awt.Rectangle;

import javax.swing.text.DefaultCaret;

/**
 * The Class NotScrollingCaret is a simple wrapper around an arbitrary caret
 * which only changes the method {@link #adjustVisibility(Rectangle)}.
 * When the caret changes, no action is to be taken. 
 */
public class NotScrollingCaret extends DefaultCaret {

    private static final long serialVersionUID = 3397724566759902358L;

    /**
     * Instantiates a new not scrolling caret.
     */
    public NotScrollingCaret() {
        super();
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p> For this implementation, we do not want to move the display at any time.
     * Therefore, this method does nothing. 
     */
    @Override protected void adjustVisibility(Rectangle nloc) {
        // do nothing
    }

}
