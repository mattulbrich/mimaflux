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

// Original header:

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
