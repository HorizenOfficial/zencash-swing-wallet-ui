/************************************************************************************************
 *  _________          _     ____          _           __        __    _ _      _   _   _ ___
 * |__  / ___|__ _ ___| |__ / ___|_      _(_)_ __   __ \ \      / /_ _| | | ___| |_| | | |_ _|
 *   / / |   / _` / __| '_ \\___ \ \ /\ / / | '_ \ / _` \ \ /\ / / _` | | |/ _ \ __| | | || |
 *  / /| |__| (_| \__ \ | | |___) \ V  V /| | | | | (_| |\ V  V / (_| | | |  __/ |_| |_| || |
 * /____\____\__,_|___/_| |_|____/ \_/\_/ |_|_| |_|\__, | \_/\_/ \__,_|_|_|\___|\__|\___/|___|
 *                                                 |___/
 *
 * Copyright (c) 2018 Ivan Vaklinov <ivan@vaklinov.com>
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
import javax.swing.JTable;
import javax.swing.Timer;

import com.vaklinov.zcashui.OSUtil.OS_TYPE;
import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;


/**
 * Transaction details panel (separate UI TAB).
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class TransactionsDetailPanel
	extends WalletTabPanel
{
	private JFrame parentFrame;
	private ZCashClientCaller clientCaller;
	private StatusUpdateErrorReporter errorReporter;
	private ZCashInstallationObserver installationObserver;
		
	private String OSInfo              = null;
		
	private JTable transactionsTable   = null;
	private JScrollPane transactionsTablePane  = null;
	private String[][] lastTransactionsData = null;
	private DataGatheringThread<String[][]> transactionGatheringThread = null;
	

	public TransactionsDetailPanel(JFrame parentFrame,
			              ZCashInstallationObserver installationObserver,
			              ZCashClientCaller clientCaller,
			              StatusUpdateErrorReporter errorReporter,
			              DataGatheringThread<String[][]> transactionGatheringThread)
		throws IOException, InterruptedException, WalletCallException
	{
		this.parentFrame          = parentFrame;
		this.clientCaller  = clientCaller;
		this.errorReporter = errorReporter;
		this.installationObserver = installationObserver;
		this.transactionGatheringThread = transactionGatheringThread;
		
		this.timers = new ArrayList<Timer>();
		this.threads = new ArrayList<DataGatheringThread<?>>();

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
	
	
	private void updateWalletTransactionsTable()
		throws WalletCallException, IOException, InterruptedException
	{
		String[][] newTransactionsData = this.transactionGatheringThread.getLastData();
		
		// May be null - not even gathered once
		if (newTransactionsData == null)
		{
			return;
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
		String columnNames[] = { "Type", "Direction", "Confirmed?", "Amount", "Date", "Destination Address"};
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


	// TODO: duplication with dashboard - to be eliminated before release!
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
				if (!o1[4].equals("N/A"))
				{
					d1 = new Date(Long.valueOf(o1[4]).longValue() * 1000L);
				}

				Date d2 = new Date(0);
				if (!o2[4].equals("N/A"))
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
			if (trans[1].equals("receive"))
			{
				trans[1] = "\u21E8 IN";
			} else if (trans[1].equals("send"))
			{
				trans[1] = "\u21E6 OUT";
			} else if (trans[1].equals("generate"))
			{
				trans[1] = "\u2692\u2699 MINED";
			} else if (trans[1].equals("immature"))
			{
				trans[1] = "\u2696 Immature";
			};

			// Date
			if (!trans[4].equals("N/A"))
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
				boolean isConfirmed = !trans[2].trim().equals("0"); 
				
				trans[2] = isConfirmed ? ("Yes " + confirmed) : ("No  " + notConfirmed);
			} catch (NumberFormatException nfe)
			{
				Log.error("Error occurred while formatting confirmations: " + trans[2] + 
						           " - " + nfe.getMessage() + "!");
			}
		}


		return allTransactions;
	}
	
} // End class
