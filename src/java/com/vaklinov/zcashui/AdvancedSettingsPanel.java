package com.vaklinov.zcashui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class AdvancedSettingsPanel extends JPanel {

    private JCheckBox cbAdvanceSettings = null;
    private JCheckBox cbApplyOptionOnce = null;
    private JTextArea taCommandLineParameters = null;

    private JLabel lbAdvancedSettings = null;
    private JLabel lbWarningLabel = null;
    private JLabel lbStatusBar = null;
    private JLabel lbSpacer = null;

    private JButton btnApplySettings = null;
    private JButton btnLinkCommands = null;
    private LanguageUtil langUtil;

    private ZenCashUI parent;
    private final ZCashClientCaller clientCaller;


    public AdvancedSettingsPanel(ZenCashUI parent, ZCashClientCaller clientCaller)
            throws IOException, InterruptedException, URISyntaxException {
        super();
        langUtil = LanguageUtil.instance();
        this.parent = parent;
        this.clientCaller = clientCaller;

        // UI Assignment
        lbAdvancedSettings = new JLabel(langUtil.getString("domain.fronting.status.header.text"));
        lbWarningLabel = new JLabel(langUtil.getString("domain.fronting.message.warning"));
        lbStatusBar = new JLabel(langUtil.getString("domain.fronting.status.label.no.changes"));
        lbSpacer = new JLabel("  ");

        cbAdvanceSettings = new JCheckBox("Enable advanced mode");
        cbApplyOptionOnce = new JCheckBox("Apply option/s once");
        cbApplyOptionOnce.setEnabled(false);
        cbApplyOptionOnce.setSelected(clientCaller.runOnce);

        taCommandLineParameters = new JTextArea(6, 10);
        taCommandLineParameters.setBackground(Color.lightGray);
        taCommandLineParameters.setEditable(false);

        btnApplySettings = new JButton("Apply");
        btnApplySettings.setEnabled(false);

        //URI
        final URI uri = new URI("https://github.com/ZencashOfficial/zen/blob/development/doc/commands.md");

        Border borderEmpty = BorderFactory.createEmptyBorder();
        btnLinkCommands = new JButton();
        btnLinkCommands.setText("<html><font color=\"#ff0000\"><U>Link to options document</U></font></html>");
        btnLinkCommands.setBorderPainted(false);
        btnLinkCommands.setOpaque(false);
        btnLinkCommands.setContentAreaFilled(false);
        btnLinkCommands.setBorder(borderEmpty);
        btnLinkCommands.setBackground(Color.lightGray);
        btnLinkCommands.setToolTipText(uri.toString());

        //TODO: Start building UI.
        this.setLayout(new BorderLayout(0, 0));
        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        JPanel panelAdvancedSettings = new JPanel();

        this.add(panelAdvancedSettings, BorderLayout.NORTH);
        panelAdvancedSettings.setLayout(new BoxLayout(panelAdvancedSettings, BoxLayout.Y_AXIS));
        panelAdvancedSettings.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        JPanel panelLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelLabel.add(lbAdvancedSettings);
        panelAdvancedSettings.add(panelLabel);

        JPanel panelWarningLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelWarningLabel.add(lbWarningLabel);

        panelAdvancedSettings.add(panelWarningLabel);

        JPanel panelAdvancedMode = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelAdvancedMode.add(cbAdvanceSettings);
        panelAdvancedMode.add(lbSpacer);
        panelAdvancedMode.add(cbApplyOptionOnce);
        panelAdvancedSettings.add(panelAdvancedMode);

        panelAdvancedSettings.add(taCommandLineParameters);

        JPanel panelApplyChanges = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        panelApplyChanges.add(btnApplySettings, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0.1;
        constraints.gridx = 1;
        constraints.gridy = 0;
        panelApplyChanges.add(lbStatusBar, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 0;
        constraints.gridx = 2;
        constraints.gridy = 0;
        panelApplyChanges.add(btnLinkCommands, constraints);

        panelAdvancedSettings.add(panelApplyChanges);

        //Wire Link button
        btnLinkCommands.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                open(uri);
            }
        });

        //Wire the apply button
        btnApplySettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {

                    int option = JOptionPane.showConfirmDialog(
                            AdvancedSettingsPanel.this.getRootPane().getParent(),
                            langUtil.getString("domain.fronting.message.confirmation"),
                            "Warning",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE
                    );
                    if (option == JOptionPane.YES_OPTION) {
                        writeAndNotifyProcess();
                        lbStatusBar.setText(langUtil.getString("domain.fronting.status.label.restart"));
                        //TODO: Restart needs improvement in all platforms.
                        //parent.restartProgram();
                    }

                } catch (Exception ex) {
                    Log.error("Unexpected error:", ex);
                }
            }
        });

        //Wire enable advanced settings checkbox
        cbAdvanceSettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    if (cbAdvanceSettings.isSelected()) {
                        taCommandLineParameters.setEditable(true);
                        taCommandLineParameters.setBackground(Color.white);
                        btnApplySettings.setEnabled(true);
                        cbApplyOptionOnce.setEnabled(true);
                    } else {
                        taCommandLineParameters.setEditable(false);
                        taCommandLineParameters.setBackground(Color.lightGray);
                        btnApplySettings.setEnabled(false);
                        cbApplyOptionOnce.setEnabled(false);
                    }
                } catch (Exception ex) {
                    Log.error("Unexpected Error:", ex);
                }
            }
        });

    }

    public void writeAndNotifyProcess() {
        try {
            Properties prop = new Properties();
            int restart_check = cbApplyOptionOnce.isSelected() ? 1 : 0;

            //Set properties value.
            prop.setProperty("ZendCommands", taCommandLineParameters.getText());
            prop.setProperty("RunOnce", String.valueOf(restart_check));

            prop.store(new FileOutputStream(OSUtil.getSettingsDirectory() + File.separator + "commands.conf"), null);

        } catch (IOException e) {
            Log.error("Unexpected IO Error:", e);
        }
    }

    private static void open(URI uri)   {
        if (Desktop.isDesktopSupported()){
            try {
                Desktop.getDesktop().browse(uri);
            }catch (IOException e){
                Log.error("Exception: " + e);
            }
        }
        else{
           Log.error("Desktop not Supported");
        }
    }
}
