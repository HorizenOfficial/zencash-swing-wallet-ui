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

public class AdvancedSettingsPanel extends JPanel {

    private JCheckBox cbAdvanceSettings = null;
    private JTextArea taCommandLineParameters = null;

    private JLabel lbAdvancedSettings = null;
    private JLabel lbWarningLabel = null;
    private JLabel lbStatusBar = null;

    private JButton btnApplySettings = null;
    private JButton btnLinkCommands = null;
    private LanguageUtil langUtil;

    private ZCashUI parent;
    private final ZCashClientCaller clientCaller;


    public AdvancedSettingsPanel(ZCashUI parent, ZCashClientCaller clientCaller)
            throws IOException, InterruptedException, URISyntaxException {
        super();
        langUtil = LanguageUtil.instance();
        this.parent = parent;
        this.clientCaller = clientCaller;

        // UI Assignment
        lbAdvancedSettings = new JLabel(langUtil.getString("domain.fronting.status.header.text"));
        lbWarningLabel = new JLabel(langUtil.getString("domain.fronting.message.warning"));
        lbStatusBar = new JLabel(langUtil.getString("domain.fronting.status.label.no.changes"));

        cbAdvanceSettings = new JCheckBox("Enable advanced mode");

        taCommandLineParameters = new JTextArea(6, 10);
        taCommandLineParameters.setBackground(Color.lightGray);
        taCommandLineParameters.setEditable(false);
        //Do not save commands.
        //taCommandLineParameters.setText(StartupProgressDialog.commands);

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
                        //TODO: Restart needs improvement in all platforms.
                        //parent.restartProgram();
                    }

                } catch (Exception ex) {
                    Log.error("Unexpected error:", ex);
                }
            }
        });

        //Wire checkbox
        cbAdvanceSettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                try {
                    if (cbAdvanceSettings.isSelected()) {
                        taCommandLineParameters.setEditable(true);
                        taCommandLineParameters.setBackground(Color.white);
                        btnApplySettings.setEnabled(true);
                    } else {
                        taCommandLineParameters.setEditable(false);
                        taCommandLineParameters.setBackground(Color.lightGray);
                        btnApplySettings.setEnabled(false);
                    }
                } catch (Exception ex) {
                    Log.error("Unexpected Error:", ex);
                }
            }
        });

    }

    public void writeAndNotifyProcess() {
        try {
            File commandsFile = new File(OSUtil.getSettingsDirectory() + File.separator + "commands.conf");

            Log.info("Preparing commands to file: " + commandsFile.getPath());
            try(FileOutputStream streamCommands = new FileOutputStream(commandsFile)) {

                Log.info("Writing to File: " + commandsFile.getPath());
                byte[] commandsInByte = taCommandLineParameters.getText().getBytes();
                streamCommands.write(commandsInByte);

                streamCommands.flush();
                Log.info("Done writing commands to file: " + commandsFile.getPath());

                lbStatusBar.setText(langUtil.getString("domain.fronting.status.label.restart"));

            } catch (IOException ex) {
                Log.error("Unexpected IO Error:", ex);
            }
        }catch(IOException e){
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
