/************************************************************************************************
 *   ____________ _   _  _____          _      _____ _    _ _______          __   _ _      _   
 *  |___  /  ____| \ | |/ ____|        | |    / ____| |  | |_   _\ \        / /  | | |    | |  
 *     / /| |__  |  \| | |     __ _ ___| |__ | |  __| |  | | | |  \ \  /\  / /_ _| | | ___| |_ 
 *    / / |  __| | . ` | |    / _` / __| '_ \| | |_ | |  | | | |   \ \/  \/ / _` | | |/ _ \ __|
 *   / /__| |____| |\  | |___| (_| \__ \ | | | |__| | |__| |_| |_   \  /\  / (_| | | |  __/ |_ 
 *  /_____|______|_| \_|\_____\__,_|___/_| |_|\_____|\____/|_____|   \/  \/ \__,_|_|_|\___|\__|
 *                                       
 * Copyright (c) 2016-2018 The ZEN Developers
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


import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;

import com.vaklinov.zcashui.OSUtil.OS_TYPE;
import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;


/**
 * Addresses panel - shows T/Z addresses and their balances.
 */
public class AddressesPanel
		extends WalletTabPanel
{
	private JFrame parentFrame;
	private ZCashClientCaller clientCaller;
	private StatusUpdateErrorReporter errorReporter;

	private JTable addressBalanceTable   = null;
	private JScrollPane addressBalanceTablePane  = null;

	String[][] lastAddressBalanceData = null;

	private DataGatheringThread<String[][]> balanceGatheringThread = null;

	private long lastInteractiveRefresh;

	private LanguageUtil langUtil;

	// Table of validated addresses with their validation result. An invalid or watch-only address should not be shown
	// and should be remembered as invalid here
	private Map<String, Boolean> validationMap = new HashMap<String, Boolean>();
	
	
	// Storage of labels
	private LabelStorage labelStorage;
	
	private ZCashInstallationObserver installationObserver;


	public AddressesPanel(JFrame parentFrame, ZCashClientCaller clientCaller, StatusUpdateErrorReporter errorReporter, LabelStorage labelStorage,
			              ZCashInstallationObserver installationObserver)
			throws IOException, InterruptedException, WalletCallException
	{
		this.parentFrame = parentFrame;
		this.clientCaller = clientCaller;
		this.errorReporter = errorReporter;
		this.installationObserver = installationObserver;
		
		this.labelStorage = labelStorage;
		

		this.lastInteractiveRefresh = System.currentTimeMillis();

		this.langUtil = LanguageUtil.instance();

		// Build content
		JPanel addressesPanel = this;
		addressesPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		addressesPanel.setLayout(new BorderLayout(0, 0));

		// Build panel of buttons
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
		buttonPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

		JButton newTAddressButton = new JButton(langUtil.getString("panel.address.button.new.address"));
		buttonPanel.add(newTAddressButton);
		JButton newZAddressButton = new JButton(langUtil.getString("panel.address.button.new.z.address"));
		buttonPanel.add(newZAddressButton);
		buttonPanel.add(new JLabel("           "));
		JButton refreshButton = new JButton(langUtil.getString("panel.address.button.refresh"));
		buttonPanel.add(refreshButton);

		addressesPanel.add(buttonPanel, BorderLayout.SOUTH);

		// Table of addresses
		lastAddressBalanceData = getAddressBalanceDataFromWallet();
		addressesPanel.add(addressBalanceTablePane = new JScrollPane(
						addressBalanceTable = this.createAddressBalanceTable(lastAddressBalanceData)),
				BorderLayout.CENTER);

		JPanel warningPanel = new JPanel();
		warningPanel.setLayout(new BorderLayout(3, 3));
		warningPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		JLabel warningL = new JLabel(langUtil.getString("panel.address.label.warning"));
		warningPanel.add(warningL, BorderLayout.NORTH);
		addressesPanel.add(warningPanel, BorderLayout.NORTH);

		// Thread and timer to update the address/balance table
		this.balanceGatheringThread = new DataGatheringThread<String[][]>(
				new DataGatheringThread.DataGatherer<String[][]>()
				{
					public String[][] gatherData()
							throws Exception
					{
						long start = System.currentTimeMillis();
						String[][] data = AddressesPanel.this.getAddressBalanceDataFromWallet();
						long end = System.currentTimeMillis();
						Log.info("Gathering of address/balance table data done in " + (end - start) + "ms." );

						return data;
					}
				},
				this.errorReporter, 25000);
		this.threads.add(this.balanceGatheringThread);

		ActionListener alBalances = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					AddressesPanel.this.updateWalletAddressBalanceTableAutomated();
				} catch (Exception ex)
				{
					Log.error("Unexpected error: ", ex);
					AddressesPanel.this.errorReporter.reportError(ex);
				}
			}
		};
		Timer t = new Timer(5000, alBalances);
		t.start();
		this.timers.add(t);

		// Button actions
		refreshButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Cursor oldCursor = null;
				try
				{
					// TODO: dummy progress bar ... maybe
					oldCursor = AddressesPanel.this.getCursor();
					AddressesPanel.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

					AddressesPanel.this.updateWalletAddressBalanceTableInteractive();

					AddressesPanel.this.setCursor(oldCursor);
				} catch (Exception ex)
				{
					if (oldCursor != null)
					{
						AddressesPanel.this.setCursor(oldCursor);
					}

					Log.error("Unexpected error: ", ex);
					AddressesPanel.this.errorReporter.reportError(ex, false);
				}
			}
		});

		newTAddressButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				createNewAddress(false);
			}
		});

		newZAddressButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				createNewAddress(true);
			}
		});

	}


	// Null if not selected
	public String getSelectedAddress()
	{
		String address = null;

		int selectedRow = this.addressBalanceTable.getSelectedRow();

		if (selectedRow != -1)
		{
			address = this.addressBalanceTable.getModel().getValueAt(selectedRow, 3).toString();
		}

		return address;
	}


	private void createNewAddress(boolean isZAddress)
	{
		Cursor oldCursor = this.getCursor();
		try
		{
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			
			// Check for encrypted wallet
			final boolean bEncryptedWallet = this.clientCaller.isWalletEncrypted();
			if (bEncryptedWallet && isZAddress)
			{
				this.setCursor(oldCursor);
				PasswordDialog pd = new PasswordDialog((JFrame)(this.getRootPane().getParent()));
				pd.setVisible(true);

				if (!pd.isOKPressed())
				{
					return;
				}

				this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				this.clientCaller.unlockWallet(pd.getPassword());
			}

			// zend has a bug that sometimes supposedly newly returned addresses have actually
			// been used as change addresses.
			String address = null;
			double dBalance = 0;
			do
			{
				address = this.clientCaller.createNewAddress(isZAddress);
				Log.info("Newly obtained address is: {0}", address);
				String sBalance = this.clientCaller.getBalanceForAddress(address);
				if (!Util.stringIsEmpty(sBalance))
				{
					dBalance = Double.parseDouble(sBalance);
				}
				
				if (dBalance > 0)
				{
					Log.warning("New address {0} generated by zend has been used before. Will generate another!", address);
				}
			} while (dBalance > 0);

			// Lock the wallet again
			if (bEncryptedWallet && isZAddress)
			{
				this.clientCaller.lockWallet();
			}

			String backupMessage = "";
			if (isZAddress)
			{
				backupMessage = langUtil.getString("panel.address.message.backup");
			}

			this.setCursor(oldCursor);
			
            String label = (String) JOptionPane.showInputDialog(AddressesPanel.this,
            		langUtil.getString("panel.address.label.input.text"),
            		langUtil.getString("panel.address.label.input.title"),
                    JOptionPane.PLAIN_MESSAGE, null, null, "");
			
            if (!Util.stringIsEmpty(label))
            {
            	this.labelStorage.setLabel(address, label);
            }
            
			JOptionPane.showMessageDialog(
					this.getRootPane().getParent(),
					langUtil.getString("panel.address.option.pane.text", (isZAddress ? "Z (Private)" : "T (Transparent)"),
							address, backupMessage),
					langUtil.getString("panel.address.option.pane.title"),
					JOptionPane.INFORMATION_MESSAGE);

			this.updateWalletAddressBalanceTableInteractive();
		} catch (Exception e)
		{
			this.setCursor(oldCursor);
			Log.error("Unexpected error: ", e);
			AddressesPanel.this.errorReporter.reportError(e, false);
		}
	}

	// Interactive and non-interactive are mutually exclusive
	private synchronized void updateWalletAddressBalanceTableInteractive()
			throws WalletCallException, IOException, InterruptedException
	{
		this.lastInteractiveRefresh = System.currentTimeMillis();

		String[][] newAddressBalanceData = this.getAddressBalanceDataFromWallet();

		if (Util.arraysAreDifferent(lastAddressBalanceData, newAddressBalanceData))
		{
			Log.info("Updating table of addresses/balances I...");
			this.remove(addressBalanceTablePane);
			this.add(addressBalanceTablePane = new JScrollPane(
							addressBalanceTable = this.createAddressBalanceTable(newAddressBalanceData)),
					BorderLayout.CENTER);
			lastAddressBalanceData = newAddressBalanceData;

			this.validate();
			this.repaint();
		}
	}


	// Interactive and non-interactive are mutually exclusive
	private synchronized void updateWalletAddressBalanceTableAutomated()
			throws WalletCallException, IOException, InterruptedException
	{
		// Make sure it is > 1 min since the last interactive refresh
		if ((System.currentTimeMillis() - lastInteractiveRefresh) < (60 * 1000))
		{
			return;
		}

		String[][] newAddressBalanceData = this.balanceGatheringThread.getLastData();

		if ((newAddressBalanceData != null) &&
				Util.arraysAreDifferent(lastAddressBalanceData, newAddressBalanceData))
		{
			Log.info("Updating table of addresses/balances A...");
			this.remove(addressBalanceTablePane);
			this.add(addressBalanceTablePane = new JScrollPane(
							addressBalanceTable = this.createAddressBalanceTable(newAddressBalanceData)),
					BorderLayout.CENTER);
			lastAddressBalanceData = newAddressBalanceData;
			this.validate();
			this.repaint();
		}
	}


	private JTable createAddressBalanceTable(String rowData[][])
			throws WalletCallException, IOException, InterruptedException
	{
		// Create new row data - to make sure we avoid update problems
		String rowDataNew[][] = new String[rowData.length][];
		for (int i = 0; i < rowData.length; i++)
		{
			rowDataNew[i] = new String[rowData[i].length];
			for (int j = 0; j < rowData[i].length; j++)
			{
				rowDataNew[i][j] = rowData[i][j];
			}
		}
		
		String columnNames[] = langUtil.getString("panel.address.table.create.address.header").split(":");
        JTable table = new AddressTable(rowDataNew, columnNames, this.clientCaller, this.labelStorage, this.installationObserver);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(280);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(1000);

		return table;
	}


	private String[][] getAddressBalanceDataFromWallet()
			throws WalletCallException, IOException, InterruptedException
	{
		// Z Addresses - they are OK
		String[] zAddresses = clientCaller.getWalletZAddresses();

		// T Addresses listed with the list received by addr command
		String[] tAddresses = this.clientCaller.getWalletAllPublicAddresses();
		Set<String> tStoredAddressSet = new HashSet<>();
		for (String address : tAddresses)
		{
			tStoredAddressSet.add(address);
		}

		// T addresses with unspent outputs - just in case they are different
		String[] tAddressesWithUnspentOuts = this.clientCaller.getWalletPublicAddressesWithUnspentOutputs();
		Set<String> tAddressSetWithUnspentOuts = new HashSet<>();
		for (String address : tAddressesWithUnspentOuts)
		{
			tAddressSetWithUnspentOuts.add(address);
		}

		// Combine all known T addresses
		Set<String> tAddressesCombined = new HashSet<>();
		tAddressesCombined.addAll(tStoredAddressSet);
		tAddressesCombined.addAll(tAddressSetWithUnspentOuts);

		String[][] addressBalances = new String[zAddresses.length + tAddressesCombined.size()][];

		// Format double numbers - else sometimes we get exponential notation 1E-4 ZEN
		DecimalFormat df = new DecimalFormat("########0.00######");

		String confirmed    = "\u2690";
		String notConfirmed = "\u2691";

		// Windows does not support the flag symbol (Windows 7 by default)
		// TODO: isolate OS-specific symbol codes in a separate class
		OS_TYPE os = OSUtil.getOSType();
		if (os == OS_TYPE.WINDOWS)
		{
			confirmed = " \u25B7";
			notConfirmed = " \u25B6";
		}

		int i = 0;

		for (String address : tAddressesCombined)
		{
			String addressToDisplay = address;
			// Make sure the current address is not watch-only or invalid
			if (!this.validationMap.containsKey(address))
			{
				boolean validationResult = this.clientCaller.isWatchOnlyOrInvalidAddress(address);
				this.validationMap.put(address, new Boolean(validationResult));

				if (validationResult)
				{
					JOptionPane.showMessageDialog(
							this.parentFrame,
		                langUtil.getString("panel.address.option.pane.validation.error.text", address),
							langUtil.getString("panel.address.option.pane.validation.error.title"),
							JOptionPane.ERROR_MESSAGE);
				}
			}

			boolean watchOnlyOrInvalid = this.validationMap.get(address).booleanValue();
			if (watchOnlyOrInvalid)
			{
				Log.error("The following address is invalid or a watch-only address: {0}. It will not be displayed!", address);
				addressToDisplay = "<INVALID OR WATCH-ONLY ADDRESS> !!!";
			}
			// End of check for invalid/watch only addresses

			String confirmedBalance = this.clientCaller.getBalanceForAddress(address);
			String unconfirmedBalance = this.clientCaller.getUnconfirmedBalanceForAddress(address);
			boolean isConfirmed =  (confirmedBalance.equals(unconfirmedBalance));
			String balanceToShow = df.format(Double.valueOf(
					isConfirmed ? confirmedBalance : unconfirmedBalance));

			addressBalances[i++] = new String[]
			{
				            this.labelStorage.getLabel(addressToDisplay),
							balanceToShow,
							isConfirmed ? (langUtil.getString("panel.address.option.pane.yes", confirmed))
										: (langUtil.getString("panel.address.option.pane.no", notConfirmed)),
						    addressToDisplay
			};
		}

		for (String address : zAddresses)
		{
			String confirmedBalance = this.clientCaller.getBalanceForAddress(address);
			String unconfirmedBalance = this.clientCaller.getUnconfirmedBalanceForAddress(address);
			boolean isConfirmed =  (confirmedBalance.equals(unconfirmedBalance));
			String balanceToShow = df.format(Double.valueOf(
					isConfirmed ? confirmedBalance : unconfirmedBalance));

			addressBalances[i++] = new String[]
			{
				            this.labelStorage.getLabel(address),
							balanceToShow,
							isConfirmed ? (langUtil.getString("panel.address.option.pane.yes", confirmed))
										: (langUtil.getString("panel.address.option.pane.no", notConfirmed)),
							address
			};
		}

		return addressBalances;
	}

}
