package edu.kit.kastel.formal.mimaflux.gui;

import javax.swing.*;
import java.awt.*;

public class BulletIcon implements Icon {

    public static final int SIZE = 12;
    public static final Color COLOR = new Color(0xD44903);

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.setColor(COLOR);
        g.fillOval(x, y, SIZE, SIZE);
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
