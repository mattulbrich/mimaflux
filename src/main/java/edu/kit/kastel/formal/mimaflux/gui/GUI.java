package edu.kit.kastel.formal.mimaflux.gui;

import edu.kit.kastel.formal.mimaflux.Command;
import edu.kit.kastel.formal.mimaflux.Interpreter;
import edu.kit.kastel.formal.mimaflux.MimaFlux;
import edu.kit.kastel.formal.mimaflux.State;
import edu.kit.kastel.formal.mimaflux.Timeline;
import edu.kit.kastel.formal.mimaflux.UpdateListener;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.codicons.Codicons;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class GUI extends JFrame implements UpdateListener {
    public static final int MIN_FIELD_WIDTH = 70;

    private static final String STEP_LABEL_PATTERN = "Step %d of %d    ";
    private static final Object[] TABLE_HEADERS = { "Address", "Value", "Instruction" };
    public static final int ROW_COUNT = 1 << 12;
    private static final Font TABLE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    private static final FileFilter MIMA_FILE_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            return f.getName().endsWith(".mima");
        }

        @Override
        public String getDescription() {
            return "Mima Files (.mima)";
        }
    };

    private BreakpointManager breakpointManager = new BreakpointManager();

    private BreakpointPane code;
    private Timeline timeline;
    private DefaultTableModel tableModel;
    private JSpinner pageSpinner;
    private JCheckBox hexMode;
    private JLabel stepLabel;
    private JTextField accuField;
    private JTextField iarField;
    private JLabel nextInstruction;
    private String lastFilename;

    public GUI(Timeline timeline) {
        super("Mima Flux Compensator -- Time Travel Debugger");
        this.lastFilename = MimaFlux.mmargs.fileName;
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

        memoryChanged(Timeline.STEP, 0);
        memoryChanged(State.ACCU, 0);
        memoryChanged(State.IAR, timeline.get(State.IAR));

        refillTable();
    }

    private void refillTable() {
        int page = ((Number)pageSpinner.getValue()).intValue() * ROW_COUNT;

        for (int i = 0; i < ROW_COUNT; i++) {
            int adr = page | i;
            tableModel.setValueAt(String.format("0x%05x", adr), i, 0);
            tableModel.setValueAt(formatValue(timeline.get(adr)), i, 1);
            tableModel.setValueAt(State.toInstruction(timeline.get(adr)), i, 2);
        }
    }

    private String formatValue(int val) {
        boolean isHex = hexMode.isSelected();
        return String.format(isHex ? "0x%06x" : "%d", val);
    }

    private void initGui() {
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());

        this.code = new BreakpointPane(breakpointManager, true);
        code.setBreakPointResource(this);
        code.setMinimumSize(new Dimension(200, 400));

        JPanel memPanel = makeMemPanel();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(code), memPanel);
        split.setResizeWeight(1);
        cp.add(split, BorderLayout.CENTER);

        cp.add(makeButtonPanel(), BorderLayout.NORTH);
        setSize(900, 700);
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
        accuField = new JTextField();
        accuField.setEditable(false);
        accuField.setFont(TABLE_FONT);
        accuField.setBackground(UIManager.getColor("Table.background"));
        result.add(accuField, gbc);
        gbc.ipadx = 0;
        gbc.gridy++;
        gbc.gridx = 0;

        result.add(new JLabel("IAR = "), gbc);
        gbc.gridx++;
        gbc.ipadx = MIN_FIELD_WIDTH;
        iarField = new JTextField();
        iarField.setEditable(false);
        iarField.setFont(TABLE_FONT);
        iarField.setBackground(UIManager.getColor("Table.background"));
        result.add(iarField, gbc);
        gbc.ipadx = 0;

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0.;
        gbc.gridwidth = 3;
        {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
            p.add(new JLabel("Next instruction: "));
            nextInstruction = new JLabel();
            nextInstruction.setFont(TABLE_FONT);
            p.add(nextInstruction);
            result.add(p, gbc);
        }
        gbc.gridy ++;
        {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            this.hexMode = new JCheckBox("Hex/Dec");
            hexMode.setSelected(true);
            hexMode.addChangeListener(e -> refillTable());
            p.add(hexMode);

            this.stepLabel = new JLabel(" ");
            p.add(stepLabel);

            p.add(new JLabel("      Memory page: "));
            this.pageSpinner = new JSpinner();
            pageSpinner.setModel(new SpinnerNumberModel(0, 0, 0xff, 1));
            pageSpinner.addChangeListener(e -> refillTable());
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
        buttonPanel.add(b("Menu", Codicons.MENU, this::showMenu));
        buttonPanel.addSeparator(new Dimension(50,0));
        buttonPanel.add(b("Go to initial state", Codicons.DEBUG_RESTART, e -> timeline.setPosition(0)));
        buttonPanel.add(b("Continue backwards", Codicons.DEBUG_REVERSE_CONTINUE, e -> continueToBreakpoint(-1)));
        buttonPanel.add(b("Step backwards", Codicons.DEBUG_STEP_BACK, e -> timeline.addToPosition(-1)));
        buttonPanel.add(b("Step forwards", Codicons.DEBUG_STEP_OVER, e -> timeline.addToPosition(1)));
        buttonPanel.add(b("Continue forwards", Codicons.DEBUG_CONTINUE, e-> continueToBreakpoint(+1)));
        buttonPanel.add(b("Go to terminal state", Codicons.DEBUG_START, e-> timeline.setPosition(timeline.countStates() - 1)));

        return buttonPanel;
    }

    private void showMenu(ActionEvent e) {
        JPopupMenu popup = new JPopupMenu();
        popup.add("Load ...").addActionListener(this::chooseFile);
        popup.add("Reload last file").addActionListener(this::reload);
        popup.add("Exit").addActionListener(ev -> System.exit(0));
        Component comp = (Component) e.getSource();
        popup.show(comp, comp.getX(), comp.getY() + comp.getHeight());
    }

    private void reload(ActionEvent actionEvent) {
        if (lastFilename == null) {
            JOptionPane.showMessageDialog(this,
                    "No file has been loaded so far that could be reloaded.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            loadFile(lastFilename);
        }
    }

    private void chooseFile(ActionEvent e) {
        JFileChooser jfc = new JFileChooser(".");
        jfc.addChoosableFileFilter(MIMA_FILE_FILTER);
        int result = jfc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = jfc.getSelectedFile();
            loadFile(file.toString());
        }
    }

    private void loadFile(String file) {
        try {
            Interpreter interpreter = new Interpreter(file);
            interpreter.preCompile();
            interpreter.parse();
            timeline = interpreter.makeTimeline();
            setTimeline(timeline);
            this.lastFilename = file;
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Error while executing mima file.", JOptionPane.ERROR_MESSAGE);
            MimaFlux.logStacktrace(ex);
        }

    }

    private void continueToBreakpoint(int offset) {
        int pos;
        do {
            timeline.addToPosition(offset);
            Command command = timeline.findIARCommand();
            if (command != null) {
                int line = command.getMnemonicLine();
                if(breakpointManager.hasBreakpoint(this, line - 1)) {
                    break;
                }
            }
            pos = timeline.getPosition();
        } while(pos > 0 && pos < timeline.countStates());
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
        switch(addr) {
            case Timeline.STEP:
                stepLabel.setText(String.format(STEP_LABEL_PATTERN, val, timeline.countStates()));
                return;
            case State.ACCU:
                accuField.setText(formatValue(val));
                return;
            case State.IAR:
                iarField.setText(formatValue(val));
                nextInstruction.setText(State.toInstruction(timeline.get(val)));
                code.removeHighlights();
                Command command = timeline.findIARCommand();
                if (command != null) {
                    int line = command.getMnemonicLine();
                    code.addHighlight(line - 1);
                }
                return;
        }

        int page = addr >> 12;
        if (page != (Integer) pageSpinner.getValue()) {
            return;
        }
        addr &= ROW_COUNT-1;
        tableModel.setValueAt(formatValue(val), addr, 1);
        tableModel.setValueAt(State.toInstruction(val), addr, 2);
    }
}
