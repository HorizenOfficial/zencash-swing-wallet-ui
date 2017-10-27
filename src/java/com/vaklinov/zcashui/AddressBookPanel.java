// Code was originally written by developer - https://github.com/zlatinb
// Taken from repository https://github.com/zlatinb/zcash-swing-wallet-ui under an MIT licemse
package com.vaklinov.zcashui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public class AddressBookPanel extends JPanel {
    
    private static class AddressBookEntry {
        final String name,address;
        AddressBookEntry(String name, String address) {
            this.name = name;
            this.address = address;
        }
    }
    
    private final List<AddressBookEntry> entries =
            new ArrayList<>();

    private final Set<String> names = new HashSet<>();
    
    private JTable table;
    
    private JButton sendCashButton, deleteContactButton,copyToClipboardButton;
    
    private final SendCashPanel sendCashPanel;
    private final JTabbedPane tabs;
    
    private JPanel buildButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        panel.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
        
        JButton newContactButton = new JButton("New contact...");
        newContactButton.addActionListener(new NewContactActionListener());
        panel.add(newContactButton);
                
        sendCashButton = new JButton("Send ZEN");
        sendCashButton.addActionListener(new SendCashActionListener());
        sendCashButton.setEnabled(false);
        panel.add(sendCashButton);
        
        copyToClipboardButton = new JButton("Copy address to clipboard");
        copyToClipboardButton.setEnabled(false);
        copyToClipboardButton.addActionListener(new CopyToClipboardActionListener());
        panel.add(copyToClipboardButton);
        
        deleteContactButton = new JButton("Delete contact");
        deleteContactButton.setEnabled(false);
        deleteContactButton.addActionListener(new DeleteAddressActionListener());
        panel.add(deleteContactButton);
        
        return panel;
    }

    private JScrollPane buildTablePanel() {
        table = new JTable(new AddressBookTableModel(),new DefaultTableColumnModel());
        TableColumn nameColumn = new TableColumn(0);
        TableColumn addressColumn = new TableColumn(1);
        table.addColumn(nameColumn);
        table.addColumn(addressColumn);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // one at a time
        table.getSelectionModel().addListSelectionListener(new AddressListSelectionListener());
        table.addMouseListener(new AddressMouseListener());
        
        // TODO: isolate in utility
		TableCellRenderer renderer = table.getCellRenderer(0, 0);
		Component comp = renderer.getTableCellRendererComponent(table, "123", false, false, 0, 0);
		table.setRowHeight(new Double(comp.getPreferredSize().getHeight()).intValue() + 2);
        
        JScrollPane scrollPane = new JScrollPane(table);
        return scrollPane;
    }

    public AddressBookPanel(SendCashPanel sendCashPanel, JTabbedPane tabs) throws IOException {
        this.sendCashPanel = sendCashPanel;
        this.tabs = tabs;
        BoxLayout boxLayout = new BoxLayout(this,BoxLayout.Y_AXIS);
        setLayout(boxLayout);
        add(buildTablePanel());
        add(buildButtonsPanel());
       
        loadEntriesFromDisk();
    }
    
    private void loadEntriesFromDisk() throws IOException {
        File addressBookFile = new File(OSUtil.getSettingsDirectory(),"addressBook.csv");
        if (!addressBookFile.exists())
            return;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(addressBookFile))) {
            String line;
            while((line = bufferedReader.readLine()) != null) {
                // format is address,name - this way name can contain commas ;-)
                int addressEnd = line.indexOf(',');
                if (addressEnd < 0)
                    throw new IOException("Address Book is corrupted!");
                String address = line.substring(0, addressEnd);
                String name = line.substring(addressEnd + 1);
                if (!names.add(name))
                    continue; // duplicate
                entries.add(new AddressBookEntry(name,address));
            }
        }
        
        Log.info("loaded "+entries.size()+" address book entries");
    }
    
    private void saveEntriesToDisk() {
    	Log.info("Saving "+entries.size()+" addresses");
        try {
            File addressBookFile = new File(OSUtil.getSettingsDirectory(),"addressBook.csv");
            try (PrintWriter printWriter = new PrintWriter(new FileWriter(addressBookFile))) {
                for (AddressBookEntry entry : entries) 
                    printWriter.println(entry.address+","+entry.name);
            }
        } catch (IOException bad) {
        	// TODO: report error to the user!
        	Log.error("Saving Address Book Failed!!!!", bad);
        }
    }
    
    private class DeleteAddressActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int row = table.getSelectedRow();
            if (row < 0)
                return;
            AddressBookEntry entry = entries.get(row);
            entries.remove(row);
            names.remove(entry.name);
            deleteContactButton.setEnabled(false);
            sendCashButton.setEnabled(false);
            copyToClipboardButton.setEnabled(false);
            table.repaint();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    saveEntriesToDisk();
                }
            });
        }
    }
    
    private class CopyToClipboardActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int row = table.getSelectedRow();
            if (row < 0)
                return;
            AddressBookEntry entry = entries.get(row);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(entry.address), null);
        }
    }
    
    private class NewContactActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String name = (String) JOptionPane.showInputDialog(AddressBookPanel.this,
                    "Please enter the name of the contact:",
                    "Add new contact step 1",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "");
            if (name == null || "".equals(name))
                return; // cancelled

            // TODO: check for dupes
            names.add(name);
            
            String address = (String) JOptionPane.showInputDialog(AddressBookPanel.this,
                    "Please enter the t-address or z-address of "+name,
                    "Add new contact step 2",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    null,
                    "");
            if (address == null || "".equals(address))
                return; // cancelled
            entries.add(new AddressBookEntry(name,address));
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    table.invalidate();
                    table.revalidate();
                    table.repaint();
                	
                    saveEntriesToDisk();
                }
            });
        }
    }
    
    private class SendCashActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int row = table.getSelectedRow();
            if (row < 0)
                return;
            AddressBookEntry entry = entries.get(row);
            sendCashPanel.prepareForSending(entry.address);
            tabs.setSelectedIndex(2);
        }
    }

    private class AddressMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isConsumed() || (!e.isPopupTrigger()))
                return;

            int row = table.rowAtPoint(e.getPoint());
            int column = table.columnAtPoint(e.getPoint());
            table.changeSelection(row, column, false, false);
            AddressBookEntry entry = entries.get(row);
            
            JPopupMenu menu = new JPopupMenu();
            
            JMenuItem sendCash = new JMenuItem("Send ZEN to "+entry.name);
            sendCash.addActionListener(new SendCashActionListener());
            menu.add(sendCash);
            
            JMenuItem copyAddress = new JMenuItem("Copy address to clipboard");
            copyAddress.addActionListener(new CopyToClipboardActionListener());
            menu.add(copyAddress);
            
            JMenuItem deleteEntry = new JMenuItem("Delete "+entry.name+" from contacts");
            deleteEntry.addActionListener(new DeleteAddressActionListener());
            menu.add(deleteEntry);
            
            menu.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
            e.consume();
        }
        
        public void mouseReleased(MouseEvent e)
        {
        	if ((!e.isConsumed()) && e.isPopupTrigger())
        	{
        		mousePressed(e);
            }
        }
    }
    
    private class AddressListSelectionListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            int row = table.getSelectedRow();
            if (row < 0) {
                sendCashButton.setEnabled(false);
                deleteContactButton.setEnabled(false);
                copyToClipboardButton.setEnabled(false);
                return;
            }
            String name = entries.get(row).name;
            sendCashButton.setText("Send ZEN to "+name);
            sendCashButton.setEnabled(true);
            deleteContactButton.setText("Delete contact "+name);
            deleteContactButton.setEnabled(true);
            copyToClipboardButton.setEnabled(true);
        }
        
    }

    private class AddressBookTableModel extends AbstractTableModel {

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            switch(columnIndex) {
            case 0 : return "name";
            case 1 : return "address";
            default:
                throw new IllegalArgumentException("invalid column "+columnIndex);
            }
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            AddressBookEntry entry = entries.get(rowIndex);
            switch(columnIndex) {
            case 0 : return entry.name;
            case 1 : return entry.address;
            default:
                throw new IllegalArgumentException("bad column "+columnIndex);
            }
        }
    }
}