package edu.kit.kastel.formal.mimaflux.gui;

import edu.kit.kastel.formal.mimaflux.Constants;
import edu.kit.kastel.formal.mimaflux.Timeline;
import edu.kit.kastel.formal.mimaflux.UpdateListener;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.codicons.Codicons;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;

public class GUI extends JFrame implements UpdateListener {
    public static final int MIN_FIELD_WIDTH = 70;
    private static final Object[] TABLE_HEADERS = { "Address", "Value", "Instruction" };
    public static final int ROW_COUNT = 1 << 12;
    private static final Font TABLE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 14);

    private BreakpointManager breakpointManager = new BreakpointManager();

    private BreakpointPane code;
    private Timeline timeline;
    private DefaultTableModel tableModel;
    private JSpinner pageSpinner;

    public GUI(Timeline timeline) {
        super("Mima Flux Compensator -- Time Travel Debugger");
        initGui();
        setTimeline(timeline);
    }

    private void setTimeline(Timeline timeline) {
        this.timeline = timeline;
        if (timeline == null) {
            return;
        }
        timeline.addListener(this);
        code.setText(timeline.getFileContent());

        refillTable();
    }

    private void refillTable() {
        int page = ((Number)pageSpinner.getValue()).intValue() << Constants.ADDRESS_WIDTH;

        for (int i = 0; i < ROW_COUNT; i++) {
            int adr = page | i;
            tableModel.setValueAt(String.format("0x%06x", adr), i, 0);
        }
    }

    private void initGui() {
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());

        this.code = new BreakpointPane(breakpointManager, true);
        code.setBreakPointResource(this);
        code.setMinimumSize(new Dimension(200, 400));

        JPanel memPanel = makeMemPanel();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(code), memPanel);
        split.setDividerLocation(.5);
        cp.add(split, BorderLayout.CENTER);

        cp.add(makeButtonPanel(), BorderLayout.NORTH);
        pack();
        setLocationRelativeTo(null);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private JPanel makeMemPanel() {
        JPanel result = new JPanel(new GridBagLayout());
        this.tableModel = new DefaultTableModel(ROW_COUNT, 3);
        tableModel.setColumnIdentifiers(TABLE_HEADERS);
        JTable memTable = new JTable(tableModel);
        memTable.setFont(TABLE_FONT);
        memTable.setEnabled(false);

        GridBagConstraints gbc = new GridBagConstraints(0, 0,
                1, 1, 0., 0.,
                GridBagConstraints.EAST, GridBagConstraints.BOTH,
                new Insets(0,0,0,0), 0,0);

        result.add(new JLabel("ACCU = "), gbc);
        gbc.gridx++;
        gbc.ipadx = MIN_FIELD_WIDTH;
        result.add(new JTextField("           "), gbc);
        gbc.ipadx = 0;
        gbc.gridy++;
        gbc.gridx = 0;

        result.add(new JLabel("IAR = "), gbc);
        gbc.gridx++;
        gbc.ipadx = MIN_FIELD_WIDTH;
        result.add(new JTextField("           "), gbc);
        gbc.ipadx = 0;

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0.;
        gbc.gridwidth = 3;
        {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
            p.add(new JLabel("Next instruction: "));
            p.add(new JLabel("XXX"));
            result.add(p, gbc);
        }
        gbc.gridy ++;
        {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            p.add(new JCheckBox("Hex/Dec"));
            p.add(new JLabel("      Memory page: "));
            this.pageSpinner = new JSpinner();
            pageSpinner.setModel(new SpinnerNumberModel(0, 0, 0xff, 1));
            JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) pageSpinner.getEditor();
            JFormattedTextField tf = editor.getTextField();
            tf.setFormatterFactory(new HexFormatter.Factory());
            p.add(pageSpinner, gbc);
            result.add(p, gbc);
        }

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.weightx = gbc.weighty = 1.0;

        result.add(new JScrollPane(memTable), gbc);
        result.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        return result;
    }

    private Container makeButtonPanel() {
        JToolBar buttonPanel = new JToolBar();
        buttonPanel.setFloatable(false);
        buttonPanel.add(b("Menu", Codicons.MENU, e -> {}));
        buttonPanel.addSeparator(new Dimension(50,0));
        buttonPanel.add(b("Go to initial state", Codicons.DEBUG_RESTART, e->timeline.setPosition(0)));
        buttonPanel.add(b("Continue backwards", Codicons.DEBUG_REVERSE_CONTINUE, e-> jump(-100)));
        buttonPanel.add(b("Step backwards", Codicons.DEBUG_STEP_BACK, e -> jump(-10)));
        buttonPanel.add(b("Step forwards", Codicons.DEBUG_STEP_OVER, e-> jump(-1)));
        buttonPanel.add(b("Continue forwards", Codicons.DEBUG_CONTINUE, e-> jump(1)));
        buttonPanel.add(b("Go to terminal state", Codicons.DEBUG_START, e-> jump(10)));
        return buttonPanel;
    }

    private void jump(int offset) {
        timeline.setPosition(timeline.getPosition() + offset);
    }

    private JButton b(String text, Ikon ikon, ActionListener listener) {
        JButton res = new JButton(FontIcon.of(ikon, 28));
        res.setToolTipText(text);
        res.addActionListener(listener);
        res.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5,10,5, 10),
                res.getBorder()));
        return res;
    }

    @Override
    public void memoryChanged(int addr, int val) {

    }
}
