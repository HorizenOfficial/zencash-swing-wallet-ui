/************************************************************************************************
 *  _________          _     ____          _           __        __    _ _      _   _   _ ___
 * |__  / ___|__ _ ___| |__ / ___|_      _(_)_ __   __ \ \      / /_ _| | | ___| |_| | | |_ _|
 *   / / |   / _` / __| '_ \\___ \ \ /\ / / | '_ \ / _` \ \ /\ / / _` | | |/ _ \ __| | | || |
 *  / /| |__| (_| \__ \ | | |___) \ V  V /| | | | | (_| |\ V  V / (_| | | |  __/ |_| |_| || |
 * /____\____\__,_|___/_| |_|____/ \_/\_/ |_|_| |_|\__, | \_/\_/ \__,_|_|_|\___|\__|\___/|___|
 *                                                 |___/
 *
 * Copyright (c) 2016 Ivan Vaklinov <ivan@vaklinov.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 **********************************************************************************/
package com.vaklinov.zcashui;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellRenderer;



/**
 * Table to be used for transactions, addresses etc.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class DataTable 
	extends JTable 
{
	protected int lastRow = -1;
	protected int lastColumn = -1;
	
	protected JPopupMenu popupMenu;
	
	public DataTable(final Object[][] rowData, final Object[] columnNames)
	{
		super(rowData, columnNames);
		
		// TODO: isolate in utility
		TableCellRenderer renderer = this.getCellRenderer(0, 0);
		Component comp = renderer.getTableCellRendererComponent(this, "123", false, false, 0, 0);
		this.setRowHeight(new Double(comp.getPreferredSize().getHeight()).intValue() + 2);
		
		popupMenu = new JPopupMenu();
		int accelaratorKeyMask = Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask();
		
		JMenuItem copy = new JMenuItem("Copy value");
        popupMenu.add(copy);
        copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, accelaratorKeyMask));
        copy.addActionListener(new ActionListener() 
        {	
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				if ((lastRow >= 0) && (lastColumn >= 0))
				{
					String text = DataTable.this.getValueAt(lastRow, lastColumn).toString();
				
					Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
					clipboard.setContents(new StringSelection(text), null);
				} else
				{
					// Log perhaps
				}
			}
		});
        
        
		JMenuItem exportToCSV = new JMenuItem("Export data to CSV...");
        popupMenu.add(exportToCSV);
        exportToCSV.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, accelaratorKeyMask));
        exportToCSV.addActionListener(new ActionListener() 
        {	
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					DataTable.this.exportToCSV();						
				} catch (Exception ex)
				{
					Log.error("Unexpected error: ", ex);
					// TODO: better error handling
					JOptionPane.showMessageDialog(
							DataTable.this.getRootPane().getParent(), 
							"An unexpected error occurred when exporting data to CSV file.\n" +
							"\n" +
							ex.getMessage(),
							"Error in CSV export", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
        
        
        this.addMouseListener(new MouseAdapter()
        {
        	public void mousePressed(MouseEvent e)
        	{
                if ((!e.isConsumed()) && e.isPopupTrigger())
                {
                    JTable table = (JTable)e.getSource();
                    lastColumn = table.columnAtPoint(e.getPoint());
                    lastRow = table.rowAtPoint(e.getPoint());
                    
                    if (!table.isRowSelected(lastRow))
                    {
                        table.changeSelection(lastRow, lastColumn, false, false);
                    }

                    popupMenu.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
                    e.consume();
                } else
                {
                	lastColumn = -1;
                	lastRow    = -1;
                }
        	}
        	
            public void mouseReleased(MouseEvent e)
            {
            	if ((!e.isConsumed()) && e.isPopupTrigger())
            	{
            		mousePressed(e);
            	}
            }
        });
        
//        this.addKeyListener(new KeyAdapter() 
//		{			
//			@Override
//			public void keyTyped(KeyEvent e) 
//			{
//				if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
//				{
//					System.out.println("Context menu invoked...");;
//					popupMenu.show(e.getComponent(), e.getComponent().getX(), e.getComponent().getY());
//				}
//			}
//		});
	}

	
	// Exports the table data to a CSV file
	private void exportToCSV()
		throws IOException
	{
        final String ENCODING = "UTF-8";
		
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Export data to CSV file...");
		fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
		 
		int result = fileChooser.showSaveDialog(this.getRootPane().getParent());
		 
		if (result != JFileChooser.APPROVE_OPTION) 
		{
		    return;
		}
		
		File f = fileChooser.getSelectedFile();
		
		FileOutputStream fos = new FileOutputStream(f);
		fos.write(new byte[] { (byte)0xEF, (byte)0xBB, (byte)0xBF } );
		
		// Write header
		StringBuilder header = new StringBuilder();
		for (int i = 0; i < this.getColumnCount(); i++)
		{
			String columnName = this.getColumnName(i);
			header.append(columnName);
			
			if (i < (this.getColumnCount() - 1))
			{
				header.append(",");
			}
		}
		header.append("\n");
		fos.write(header.toString().getBytes(ENCODING));
		
		// Write rows
		for (int row = 0; row < this.getRowCount(); row++)
		{
			StringBuilder rowBuf = new StringBuilder();
			for (int col = 0; col < this.getColumnCount(); col++)
			{
				rowBuf.append(this.getValueAt(row, col).toString());
				
				if (col < (this.getColumnCount() - 1))
				{
					rowBuf.append(",");
				}
			}
			rowBuf.append("\n");
			fos.write(rowBuf.toString().getBytes(ENCODING));
		}
		
		fos.close();
		
		JOptionPane.showMessageDialog(
			this.getRootPane().getParent(), 
			"The data has been exported successfully as CSV to location:\n" +
			f.getCanonicalPath(),
			"Export successful...", JOptionPane.INFORMATION_MESSAGE);
	}
}
