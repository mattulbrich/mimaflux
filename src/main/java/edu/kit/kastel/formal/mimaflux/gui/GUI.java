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
package edu.kit.kastel.formal.mimaflux.gui;

import edu.kit.kastel.formal.mimaflux.Command;
import edu.kit.kastel.formal.mimaflux.Interpreter;
import edu.kit.kastel.formal.mimaflux.MimaFlux;
import edu.kit.kastel.formal.mimaflux.State;
import edu.kit.kastel.formal.mimaflux.Timeline;
import edu.kit.kastel.formal.mimaflux.UpdateListener;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.codicons.Codicons;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GUI extends JFrame implements UpdateListener {
    private static final String STEP_LABEL_PATTERN = "Step %d of %d    ";
    private static final Object[] TABLE_HEADERS = { "Address", "Value", "Instruction" };
    public static final int ROW_COUNT = 1 << 12;
    private static final Font TABLE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 14);

    private static final FileFilter MIMA_ASM_FILE_FILTER =
            new FileNameExtensionFilter("Mima ASM Files (.mima)", "mima");


    private BreakpointManager breakpointManager = new BreakpointManager();

    private BreakpointPane code;
    private Timeline timeline;
    private DefaultTableModel tableModel;
    private JSpinner pageSpinner;
    private JComboBox<RepreState> repreMode;
    private JLabel stepLabel;
    private JTextField accuField;
    private JTextField iarField;
    private JLabel nextInstruction;
    private String lastFilename;
    private List<JComponent> componentsToDisable = new ArrayList<>();
    private JPanel optionalPanel;
    private boolean modifiedSinceLoad;

    public GUI(Timeline timeline) {
        super("Mima Flux Capacitor -- Time Travel Debugger");
        this.lastFilename = MimaFlux.mmargs.fileName;
        initGui();
        setTimeline(timeline);
        modifiedSinceLoad = false;
    }

    private void setTimeline(Timeline timeline) {

        if (timeline == null) {
            this.timeline = null;
            setModified(true);
            return;
        }
        timeline.addListener(this);
        code.setText(timeline.getFileContent());
        this.timeline = timeline;

        memoryChanged(Timeline.STEP, 0);
        memoryChanged(State.ACCU, 0);
        memoryChanged(State.IAR, timeline.get(State.IAR));

        setModified(false);
        refillTable();
    }

    private void setModified(boolean b) {
        componentsToDisable.forEach(x -> x.setEnabled(!b));
        ((CardLayout)optionalPanel.getLayout()).show(optionalPanel, b ? "modified" : "normal");
        if(b) {
            code.removeHighlights();
        }
    }

    private void refillTable() {
        int page = ((Number)pageSpinner.getValue()).intValue() * ROW_COUNT;

        iarField.setText(formatValue(timeline.get(State.IAR)));
        accuField.setText(formatValue(timeline.get(State.ACCU)));

        for (int i = 0; i < ROW_COUNT; i++) {
            int adr = page | i;
            String name = timeline.getNameFor(adr);
            if (name == null) {
                name = "";
            } else {
                name = " (" + name + ")";
            }
            tableModel.setValueAt(String.format("0x%05x%s", adr, name), i, 0);
            tableModel.setValueAt(formatValue(timeline.get(adr)), i, 1);
            tableModel.setValueAt(State.toInstruction(timeline.get(adr)), i, 2);
        }
    }

    private String formatValue(int val) {
        switch ((RepreState) Objects.requireNonNull(repreMode.getSelectedItem())) {
            case BIN:
                return String.format("%24s", Integer.toBinaryString(val))
                    .replace(' ', '0');
            case DEC:
                return String.format("%d", val);
            case HEX:
                return String.format("0x%06x", val);
            default:
                return "";
        }
    }

    private void initGui() {
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());

        this.code = new BreakpointPane(breakpointManager, true);
        this.code.setEditable(true);
        this.code.getDocument().addDocumentListener((UniDocListener) x -> {
            setTimeline(null);
            modifiedSinceLoad = true;
        });
        code.setBreakPointResource(this);
        code.setMinimumSize(new Dimension(200, 400));

        JPanel memPanel = makeMemPanel();

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(new JScrollPane(code), BorderLayout.CENTER);
        JLabel label = new JLabel("Breakpoints can be set/unset by right-clicking onto a line number");
        label.setFont(UIManager.getFont("TextField.font"));
        label.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        leftPanel.add(label, BorderLayout.SOUTH);

        this.optionalPanel = new JPanel(new CardLayout());
        optionalPanel.add(memPanel, "normal");
        JLabel disabled = new JLabel("Program has been modified ...");
        disabled.setBorder(BorderFactory.createEmptyBorder(5,30,5,5));
        optionalPanel.add(disabled, "modified");

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, optionalPanel);
        split.setResizeWeight(1);
        cp.add(split, BorderLayout.CENTER);

        cp.add(makeButtonPanel(), BorderLayout.NORTH);
        setSize(900, 700);
        setLocationRelativeTo(null);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (sureChangesLost()) {
                    System.exit(0);
                }
            }
        });

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

        gbc.weightx = 0;
        result.add(new JLabel("ACCU = "), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        accuField = new JTextField();
        accuField.setEditable(false);
        accuField.setFont(TABLE_FONT);
        accuField.setBackground(UIManager.getColor("Table.background"));
        result.add(accuField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        result.add(new JLabel("IAR = "), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        iarField = new JTextField();
        iarField.setEditable(false);
        iarField.setFont(TABLE_FONT);
        iarField.setBackground(UIManager.getColor("Table.background"));
        result.add(iarField, gbc);

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
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
            this.stepLabel = new JLabel(" ");
            p.add(stepLabel);
            result.add(p, gbc);
        }
        gbc.gridy ++;
        {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            this.repreMode = new JComboBox(RepreState.values());
            repreMode.addActionListener(e -> refillTable());
            p.add(repreMode);

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
        buttonPanel.add(button("Menu", null, Codicons.MENU, this::showMenu, false));
        buttonPanel.addSeparator(new Dimension(50,0));
        buttonPanel.add(button("Go to initial state", null, Codicons.DEBUG_RESTART, this::gotoStart, false));
        buttonPanel.add(button("Continue backwards until breakpoint",  KeyStroke.getKeyStroke("F5"), Codicons.DEBUG_REVERSE_CONTINUE, e -> continueToBreakpoint(-1), true));
        buttonPanel.add(button("Step backwards",  KeyStroke.getKeyStroke("F6"), Codicons.DEBUG_STEP_BACK, e -> timeline.addToPosition(-1), true));
        buttonPanel.add(button("Step forwards",  KeyStroke.getKeyStroke("F8"), Codicons.DEBUG_STEP_OVER, e -> timeline.addToPosition(1), true));
        buttonPanel.add(button("Continue forwards until breakpoint",  KeyStroke.getKeyStroke("F9"), Codicons.DEBUG_CONTINUE, e-> continueToBreakpoint(+1), true));
        buttonPanel.add(button("Go to terminal state",  null, Codicons.DEBUG_START, e-> timeline.setPosition(timeline.countStates() - 1), true));

        return buttonPanel;
    }

    private void gotoStart(ActionEvent actionEvent) {
        if (timeline == null) {
            loadString(code.getText());
        } else {
            timeline.setPosition(0);
        }
    }

    private void showMenu(ActionEvent e) {
        JPopupMenu popup = new JPopupMenu();
        popup.add("Load ...").addActionListener(this::chooseFile);
        popup.add("Reload last file").addActionListener(this::reload);
        popup.add("Save").addActionListener(this::saveLastFile);
        popup.add("Save As ...").addActionListener(this::saveAs);
        popup.addSeparator();
        popup.add("Exit").addActionListener(ev -> {
            if(sureChangesLost()) System.exit(0);
        });
        Component comp = (Component) e.getSource();
        popup.show(comp, comp.getX(), comp.getY() + comp.getHeight());
    }

    private void saveAs(ActionEvent actionEvent) {
        JFileChooser jfc = new JFileChooser(".");
        jfc.addChoosableFileFilter(MIMA_ASM_FILE_FILTER);
        jfc.setFileFilter(MIMA_ASM_FILE_FILTER);
        int result = jfc.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = jfc.getSelectedFile();
            saveFile(file.toString());
        }
    }

    private void saveLastFile(ActionEvent actionEvent) {
        if(lastFilename != null) {
            saveFile(lastFilename);
        } else {
            saveAs(actionEvent);
        }
    }

    private void saveFile(String fileName) {
        try {
            Files.writeString(Paths.get(fileName), code.getText());
            this.lastFilename = fileName;
            modifiedSinceLoad = false;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Error while saving file.", JOptionPane.ERROR_MESSAGE);
            MimaFlux.logStacktrace(ex);
        }
    }

    private void reload(ActionEvent actionEvent) {
        if (lastFilename == null) {
            JOptionPane.showMessageDialog(this,
                    "No file has been loaded so far that could be reloaded.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            if(sureChangesLost()) {
                loadFile(lastFilename);
            }
        }
    }

    private void chooseFile(ActionEvent e) {
        if(!sureChangesLost()) {
            return;
        }
        JFileChooser jfc = new JFileChooser(".");
        jfc.addChoosableFileFilter(MIMA_ASM_FILE_FILTER);
        jfc.setFileFilter(MIMA_ASM_FILE_FILTER);
        int result = jfc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = jfc.getSelectedFile();
            loadFile(file.toString());
        }
    }

    private boolean sureChangesLost() {
        if (!modifiedSinceLoad) {
            return true;
        }
        int answer = JOptionPane.showConfirmDialog(this,
                "Loading a file or exiting will destroy the modifications you made in the editor. Continue?",
                "Are you sure?", JOptionPane.YES_NO_OPTION);
        return answer == JOptionPane.YES_OPTION;
    }

    private void loadFile(String file) {
        String content = null;
        try {
            content = Files.readString(Paths.get(file));
            this.lastFilename = file;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Error while loading file.", JOptionPane.ERROR_MESSAGE);
            MimaFlux.logStacktrace(ex);
        }
        loadString(content);
        modifiedSinceLoad = false;
    }

    private void loadString(String content) {
        try {
            Interpreter interpreter = new Interpreter();
            interpreter.parseString(content);
            timeline = interpreter.makeTimeline();
            setTimeline(timeline);

            if (timeline.countStates() == MimaFlux.mmargs.maxSteps) {
                JOptionPane.showMessageDialog(this,
                        new Object[] {
                                "This timeline reaches the maximum number of steps.",
                                "Perhaps an infinite loop? Consider using '-maxStep' to increase this bound." },
                "Warning", JOptionPane.WARNING_MESSAGE);
            }

        } catch (ParseCancellationException parseCancel) {
            JOptionPane.showMessageDialog(this,
                    parseCancel.getCause().getMessage(),
                    "Error while executing mima file.", JOptionPane.ERROR_MESSAGE);
            MimaFlux.logStacktrace(parseCancel);
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

    private JButton button(String text, KeyStroke keyStroke, Ikon ikon, ActionListener listener, boolean needsProgram) {
        JButton res = new JButton(FontIcon.of(ikon, 28));
        res.setDisabledIcon(FontIcon.of(ikon, 28, Color.lightGray));
        res.setToolTipText(text);
        res.addActionListener(listener);
        res.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5,10,5, 10),
                res.getBorder()));
        res.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, text);
        res.getActionMap().put(text, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listener.actionPerformed(e);
            }
        });
        if(needsProgram) {
            componentsToDisable.add(res);
        }
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
