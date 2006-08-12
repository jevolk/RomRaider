package enginuity.logger;

import enginuity.Settings;
import enginuity.logger.definition.EcuParameter;
import enginuity.logger.definition.EcuParameterImpl;
import enginuity.logger.definition.convertor.EcuParameterConvertor;
import enginuity.logger.definition.convertor.GenericTemperatureConvertor;
import enginuity.logger.definition.convertor.ThrottleOpeningAngleConvertor;
import enginuity.logger.query.LoggerCallback;
import static enginuity.util.HexUtil.asHex;

import javax.swing.*;
import static javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED;
import static javax.swing.JSplitPane.HORIZONTAL_SPLIT;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

public final class EcuLogger extends JFrame implements WindowListener, PropertyChangeListener {
    private final Settings settings = new Settings();
    private final LoggerController CONTROLLER = new LoggerControllerImpl(settings);
    private final JTable dataTable = new JTable();
    private final JTextArea dataTextArea = new JTextArea();
    private final JComboBox portsComboBox = new JComboBox();

    public EcuLogger(String title) {
        super(title);

        //build left and right components of split pane
        JComponent leftComponent = buildLeftComponent();
        JComponent rightComponent = buildRightComponent();

        //build split pane
        JSplitPane splitPane = buildSplitPane(leftComponent, rightComponent);
        splitPane.addPropertyChangeListener(this);

        //add to container
        getContentPane().add(splitPane);

        // add test address to log (0x000008 = coolant temp, 8bit)
        final EcuParameter ecuParam1 = new EcuParameterImpl("Coolant Temperature", "Coolant temperature in degrees C", "0x000008", new GenericTemperatureConvertor());
        CONTROLLER.addLogger(ecuParam1, new LoggerCallback() {
            public void callback(byte[] value, EcuParameterConvertor convertor) {
                dataTextArea.append(ecuParam1.getName() + " (" + ecuParam1.getAddress() + ") => " + asHex(value) + " => " + convertor.convert(value)
                        + " " + convertor.getUnits() + "\n");
            }
        });

        // add test address to log (0x000015 = throttle opening, 8bit)
        final EcuParameter ecuParam = new EcuParameterImpl("Throttle Opening", "Throttle opening angle in percent", "0x000015", new ThrottleOpeningAngleConvertor());
        CONTROLLER.addLogger(ecuParam, new LoggerCallback() {
            public void callback(byte[] value, EcuParameterConvertor convertor) {
                dataTextArea.append(ecuParam.getName() + " (" + ecuParam.getAddress() + ") => " + asHex(value) + " => " + convertor.convert(value)
                        + " " + convertor.getUnits() + "\n");
            }
        });
    }

    public static void main(String... args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI() {
        //set look and feel
        setLookAndFeel();

        //make sure we have nice window decorations.
        setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        //instantiate the controlling class.
        EcuLogger ecuLogger = new EcuLogger("Enginuity ECU Logger");

        //set remaining window properties
        ecuLogger.setSize(new Dimension(1000, 600));
        ecuLogger.setIconImage(new ImageIcon("./graphics/enginuity-ico.gif").getImage());
        ecuLogger.setDefaultCloseOperation(EXIT_ON_CLOSE);
        ecuLogger.addWindowListener(ecuLogger);

        //display the window
        ecuLogger.setLocationRelativeTo(null); //center it
        ecuLogger.setVisible(true);
    }

    private static void setLookAndFeel() {
        try {
            //use the system look and feel.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JComponent buildLeftComponent() {
        JTable parameterList = new JTable();
        return new JScrollPane(parameterList, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    private JComponent buildRightComponent() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(buildControlToolbar(), BorderLayout.NORTH);
        rightPanel.add(buildTabbedDataPane(), BorderLayout.CENTER);
        return rightPanel;
    }

    private JSplitPane buildSplitPane(JComponent leftComponent, JComponent rightComponent) {
        JSplitPane splitPane = new JSplitPane(HORIZONTAL_SPLIT, leftComponent, rightComponent);
        splitPane.setDividerSize(2);
        splitPane.setDividerLocation(250);
        return splitPane;
    }

    private JPanel buildControlToolbar() {
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(buildStartButton());
        controlPanel.add(buildStopButton());
        controlPanel.add(buildPortsComboBox());
        return controlPanel;
    }

    private JComboBox buildPortsComboBox() {
        refreshPortsComboBox();
        portsComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                settings.setLoggerPort((String) portsComboBox.getSelectedItem());
                CONTROLLER.stop();
            }
        });
        return portsComboBox;
    }

    private void refreshPortsComboBox() {
        List<String> ports = CONTROLLER.listSerialPorts();
        for (String port : ports) {
            portsComboBox.addItem(port);
        }
        settings.setLoggerPort((String) portsComboBox.getSelectedItem());
    }

    private JButton buildStartButton() {
        JButton startButton = new JButton("Start");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                settings.setLoggerPort((String) portsComboBox.getSelectedItem());
                CONTROLLER.start();
            }
        });
        return startButton;
    }

    private JButton buildStopButton() {
        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                CONTROLLER.stop();
            }
        });
        return stopButton;
    }

    private JTabbedPane buildTabbedDataPane() {
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
        tabbedPane.add("Data", buildDataTab());
        tabbedPane.add("Graph", buildGraphTab());
        tabbedPane.add("Dashboard", buildDashboardTab());
        return tabbedPane;
    }

    private JComponent buildDataTab() {
        //return new JScrollPane(dataTable, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
        return new JScrollPane(dataTextArea, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
    }

    private JComponent buildGraphTab() {
        JPanel graphPanel = new JPanel();
        return new JScrollPane(graphPanel, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
    }

    private JComponent buildDashboardTab() {
        JPanel dashboardPanel = new JPanel();
        return new JScrollPane(dashboardPanel, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
    }

    public void windowOpened(WindowEvent windowEvent) {
    }

    public void windowClosing(WindowEvent windowEvent) {
        CONTROLLER.stop();
    }

    public void windowClosed(WindowEvent windowEvent) {
    }

    public void windowIconified(WindowEvent windowEvent) {
    }

    public void windowDeiconified(WindowEvent windowEvent) {
    }

    public void windowActivated(WindowEvent windowEvent) {
    }

    public void windowDeactivated(WindowEvent windowEvent) {
    }

    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
    }

}