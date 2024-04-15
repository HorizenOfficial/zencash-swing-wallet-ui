/************************************************************************************************
 *   ____________ _   _  _____          _      _____ _    _ _______          __   _ _      _   
 *  |___  /  ____| \ | |/ ____|        | |    / ____| |  | |_   _\ \        / /  | | |    | |  
 *     / /| |__  |  \| | |     __ _ ___| |__ | |  __| |  | | | |  \ \  /\  / /_ _| | | ___| |_ 
 *    / / |  __| | . ` | |    / _` / __| '_ \| | |_ | |  | | | |   \ \/  \/ / _` | | |/ _ \ __|
 *   / /__| |____| |\  | |___| (_| \__ \ | | | |__| | |__| |_| |_   \  /\  / (_| | | |  __/ |_ 
 *  /_____|______|_| \_|\_____\__,_|___/_| |_|\_____|\____/|_____|   \/  \/ \__,_|_|_|\___|\__|
 *      
 * Copyright (c) 2023 Horizen Foundation                                 
 * Copyright (c) 2016-2021 Zen Blockchain Foundation
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.Timer;

import com.vaklinov.zcashui.OSUtil.OS_TYPE;
import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;


/**
 * Transaction details panel (separate UI TAB).
 */
public class TransactionsDetailPanel
	extends WalletTabPanel
{
	private JFrame parentFrame;
	private JTabbedPane parentTabs;
	
	private ZCashClientCaller clientCaller;
	private StatusUpdateErrorReporter errorReporter;
	private ZCashInstallationObserver installationObserver;
		
	private String OSInfo              = null;
		
	private JTable transactionsTable   = null;
	private JScrollPane transactionsTablePane  = null;
	private String[][] lastTransactionsData = null;
	private DataGatheringThread<String[][]> transactionGatheringThread = null;
	private LanguageUtil langUtil;
	
	// Storage of labels
	private LabelStorage labelStorage;

	public TransactionsDetailPanel(JFrame parentFrame,
		                  JTabbedPane parentTabs,
			              ZCashInstallationObserver installationObserver,
			              ZCashClientCaller clientCaller,
			              StatusUpdateErrorReporter errorReporter,
			              DataGatheringThread<String[][]> transactionGatheringThread,
			              LabelStorage labelStorage)
		throws IOException, InterruptedException, WalletCallException
	{
		this.parentFrame          = parentFrame;
		this.parentTabs           = parentTabs;
		
		this.clientCaller  = clientCaller;
		this.errorReporter = errorReporter;
		this.installationObserver = installationObserver;
		this.transactionGatheringThread = transactionGatheringThread;
		this.labelStorage = labelStorage;
		
		this.timers = new ArrayList<Timer>();
		this.threads = new ArrayList<DataGatheringThread<?>>();

		langUtil = LanguageUtil.instance();
		// Build content
		JPanel dashboard = this;
		dashboard.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		dashboard.setLayout(new BorderLayout(0, 0));

		// Table of transactions
		lastTransactionsData = getTransactionsDataFromWallet();
		dashboard.add(transactionsTablePane = new JScrollPane(
				         transactionsTable = this.createTransactionsTable(lastTransactionsData)),
				      BorderLayout.CENTER);
		
		ActionListener alTransactions = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{					
					TransactionsDetailPanel.this.updateWalletTransactionsTable();
				} catch (Exception ex)
				{
					Log.error("Unexpected error: ", ex);
					TransactionsDetailPanel.this.errorReporter.reportError(ex);
				}
			}
		};
		Timer t = new Timer(5000, alTransactions);
		t.start();
		this.timers.add(t);
	}
	
	
	// Called from outside when the user want to zoom on a transaction
	public void selectTransactionWithID(String transactionID)
	{
		int index = -1;
		
		if (this.lastTransactionsData != null)
		{
			int i = 0;
			for (String[] currentTX : this.lastTransactionsData)
			{
				if ((currentTX[6] != null) && (currentTX[6].equals(transactionID)))
				{
					index = i;
					break;
				}
				i++;
			}
			
			this.transactionsTable.clearSelection();
			this.transactionsTable.setRowSelectionInterval(index, index);
			
			// Switch TABs
			this.parentTabs.setSelectedIndex(1);
		}
	}
	
	
	private void updateWalletTransactionsTable()
		throws WalletCallException, IOException, InterruptedException
	{
		String[][] newTransactionsDataOrig = this.transactionGatheringThread.getLastData();
		
		// May be null - not even gathered once
		if (newTransactionsDataOrig == null)
		{
			return;
		}
		
		// Copy the data to a new array
		String[][] newTransactionsData = new String[newTransactionsDataOrig.length][];
		// Add wallet labels to the transaction data
		for (int i = 0; i < newTransactionsDataOrig.length; i++)
		{
			newTransactionsData[i] = newTransactionsDataOrig[i].clone();
			String address = newTransactionsData[i][5];
			if ((address != null) && (address.length() > 0))
			{
				String label = this.labelStorage.getLabel(address);
				if ((label != null) && (label.length() > 0))
				{
					address = label + " - " + address;
				}
			}
			newTransactionsData[i][5] = address;
		}
			
		if (Util.arraysAreDifferent(lastTransactionsData, newTransactionsData))
		{
			Log.info("Updating table of transactions...");
			this.remove(transactionsTablePane);
			this.add(transactionsTablePane = new JScrollPane(
			             transactionsTable = this.createTransactionsTable(newTransactionsData)),
			         BorderLayout.CENTER);
		}

		lastTransactionsData = newTransactionsData;

		this.validate();
		this.repaint();
	}


	private JTable createTransactionsTable(String rowData[][])
		throws WalletCallException, IOException, InterruptedException
	{
		String columnNames[] = langUtil.getString("transactions.detail.panel.column.names").split(":");
        JTable table = new TransactionTable(
        	rowData, columnNames, this.parentFrame, this.clientCaller, this.installationObserver); 
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(190);
        table.getColumnModel().getColumn(1).setPreferredWidth(145);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(210);
        table.getColumnModel().getColumn(4).setPreferredWidth(405);
        table.getColumnModel().getColumn(5).setPreferredWidth(800);

        return table;
	}


	// TODO: duplication with dashboard ...
	private String[][] getTransactionsDataFromWallet()
		throws WalletCallException, IOException, InterruptedException
	{
		// Get available public+private transactions and unify them.
		String[][] publicTransactions = this.clientCaller.getWalletPublicTransactions();
		String[][] zReceivedTransactions = this.clientCaller.getWalletZReceivedTransactions();

		String[][] allTransactions = new String[publicTransactions.length + zReceivedTransactions.length][];

		int i  = 0;

		for (String[] t : publicTransactions)
		{
			allTransactions[i++] = t;
		}

		for (String[] t : zReceivedTransactions)
		{
			allTransactions[i++] = t;
		}
		
		// Sort transactions by date
		Arrays.sort(allTransactions, new Comparator<String[]>() {
			public int compare(String[] o1, String[] o2)
			{
				Date d1 = new Date(0);
				if ((!"N/A".equals(o1[4])) && Util.isNumeric(o1[4]))
				{
					d1 = new Date(Long.valueOf(o1[4]).longValue() * 1000L);
				}

				Date d2 = new Date(0);
				if (!"N/A".equals(o2[4]) && Util.isNumeric(o2[4]))
				{
					d2 = new Date(Long.valueOf(o2[4]).longValue() * 1000L);
				}

				if (d1.equals(d2))
				{
					return 0;
				} else
				{
					return d2.compareTo(d1);
				}
			}
		});
		
		
		// Confirmation symbols
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

		DecimalFormat df = new DecimalFormat("########0.00######");
		
		// Change the direction and date etc. attributes for presentation purposes
		for (String[] trans : allTransactions)
		{
			// Direction
			if ("receive".equals(trans[1]))
			{
				trans[1] = "\u21E8 IN";
			} else if ("send".equals(trans[1]))
			{
				trans[1] = "\u21E6 OUT";
			} else if ("generate".equals(trans[1]))
			{
				trans[1] = "\u2692\u2699 MINED";
			} else if ("immature".equals(trans[1]))
			{
				trans[1] = "\u2696 Immature";
			};

			// Date
			if ((!"N/A".equals(trans[4])) && Util.isNumeric(trans[4]))
			{
				trans[4] = new Date(Long.valueOf(trans[4]).longValue() * 1000L).toLocaleString();
			}
			
			// Amount
			try
			{
				double amount = Double.valueOf(trans[3]);
				if (amount < 0d)
				{
					amount = -amount;
				}
				trans[3] = df.format(amount);
			} catch (NumberFormatException nfe)
			{
				Log.error("Error occurred while formatting amount: " + trans[3] + 
						           " - " + nfe.getMessage() + "!");
			}
			
			// Confirmed?
			try
			{
				boolean isConfirmed = !"0".equals(trans[2].trim()); 
				
				trans[2] = isConfirmed ? (langUtil.getString("transactions.detail.panel.yes", confirmed))
									   : (langUtil.getString("transactions.detail.panel.no", notConfirmed));
			} catch (NumberFormatException nfe)
			{
				Log.error("Error occurred while formatting confirmations: " + trans[2] + 
						           " - " + nfe.getMessage() + "!");
			}
		}


		return allTransactions;
	}
	
} // End class
