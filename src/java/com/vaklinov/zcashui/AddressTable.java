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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;



/**
 * Table to be used for addresses - specifically.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class AddressTable 
	extends DataTable 
{	
	LabelStorage labelStorage;
	
	public AddressTable(final Object[][] rowData, final Object[] columnNames, 
			            final ZCashClientCaller caller, LabelStorage labelStorage)
	{
		super(rowData, columnNames);
		
		this.labelStorage = labelStorage;
		
		int accelaratorKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        
		JMenuItem obtainPrivateKey = new JMenuItem("Obtain private key...");
		obtainPrivateKey.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, accelaratorKeyMask));
        popupMenu.add(obtainPrivateKey);
        
        obtainPrivateKey.addActionListener(new ActionListener() 
        {	
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				if ((lastRow >= 0) && (lastColumn >= 0))
				{
					try
					{
						String address = AddressTable.this.getModel().getValueAt(lastRow, 3).toString();
						boolean isZAddress = Util.isZAddress(address);
						
						// Check for encrypted wallet
						final boolean bEncryptedWallet = caller.isWalletEncrypted();
						if (bEncryptedWallet)
						{
							PasswordDialog pd = new PasswordDialog((JFrame)(AddressTable.this.getRootPane().getParent()));
							pd.setVisible(true);
							
							if (!pd.isOKPressed())
							{
								return;
							}
							
							caller.unlockWallet(pd.getPassword());
						}
						
						String privateKey = isZAddress ?
							caller.getZPrivateKey(address) : caller.getTPrivateKey(address);
							
						// Lock the wallet again 
						if (bEncryptedWallet)
						{
							caller.lockWallet();
						}
							
						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						clipboard.setContents(new StringSelection(privateKey), null);
						
						JOptionPane.showMessageDialog(
							AddressTable.this.getRootPane().getParent(), 
							(isZAddress ? "Z (Private)" : "T (Transparent)") +  " address:\n" +
							address + "\n" + 
							"has private key:\n" +
							privateKey + "\n\n" +
							"The private key has also been copied to the clipboard.", 
							"Private key information", JOptionPane.INFORMATION_MESSAGE);

						
					} catch (Exception ex)
					{
						Log.error("Unexpected error: ", ex);
			            JOptionPane.showMessageDialog(
			                AddressTable.this.getRootPane().getParent(),
					        "Error in obtaining private key:" + "\n" +
					         ex.getMessage() + "\n\n",
					        "Error in obtaining private key!",
					        JOptionPane.ERROR_MESSAGE);
					}
				} else
				{
					// Log perhaps
				}
			}
		});
        
        
		JMenuItem setLabel = new JMenuItem("Set label...");
		setLabel.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, accelaratorKeyMask));
        popupMenu.add(setLabel);
        
        setLabel.addActionListener(new ActionListener() 
        {	
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				if ((lastRow >= 0) && (lastColumn >= 0))
				{
					try
					{
			            TableModel model = AddressTable.this.getModel();
			            
			            String oldLabel = (String)model.getValueAt(lastRow, 0);
						String label = (String) JOptionPane.showInputDialog(AddressTable.this,
			                    "Please enter a label for the address:",
			                    "Label of the address...",
			                    JOptionPane.PLAIN_MESSAGE, null, null, oldLabel);

			            model.setValueAt(label, lastRow, 0);
			            
			            AddressTable.this.invalidate();
			            AddressTable.this.repaint();
						
					} catch (Exception ex)
					{
						Log.error("Unexpected error: ", ex);
			            JOptionPane.showMessageDialog(
			                AddressTable.this.getRootPane().getParent(),
					        "Error in setting label:" + "\n" + ex.getMessage() + "\n\n",
					        "Error in obtaining private key!",
					        JOptionPane.ERROR_MESSAGE);
					}
				} else
				{
					// Log perhaps
				}
			}
		});


        
        // Model listener for labels
        this.getModel().addTableModelListener(new TableModelListener() 
        {	
			@Override
			public void tableChanged(TableModelEvent e) 
			{
				// Make sure we respond only to editing labels
				if ((e.getType() == TableModelEvent.UPDATE) &&
					(e.getFirstRow() == e.getLastRow()) &&
					(e.getColumn() == 0))
				{
					TableModel model = AddressTable.this.getModel();
					String address = model.getValueAt(e.getFirstRow(), 3).toString();
					String newLabel = model.getValueAt(e.getFirstRow(), 0).toString();
					
					try
					{
						AddressTable.this.labelStorage.setLabel(address, newLabel);
					}
					catch (Exception ex)
					{
						Log.error("Unexpected error: ", ex);
				           JOptionPane.showMessageDialog(
				               AddressTable.this.getRootPane().getParent(),
					           "Error in editing label:" + "\n" +
					           ex.getMessage() + "\n\n",
					           "Error in editing label!",
					           JOptionPane.ERROR_MESSAGE);
					}		
				}
			}
		});
        
	} // End constructor

	
	// Make sure labels may be edited
	@Override
    public boolean isCellEditable(int row, int column) 
    {                
        return column == 0;
    }
}
