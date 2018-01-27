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


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import com.vaklinov.zcashui.OSUtil.OS_TYPE;
import com.vaklinov.zcashui.ZCashClientCaller.NetworkAndBlockchainInfo;
import com.vaklinov.zcashui.ZCashClientCaller.WalletBalance;
import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;
import com.vaklinov.zcashui.ZCashInstallationObserver.DAEMON_STATUS;
import com.vaklinov.zcashui.ZCashInstallationObserver.DaemonInfo;


/**
 * Dashboard ...
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class DashboardPanel
	extends WalletTabPanel
{
	// Static icon resources
	private static ImageIcon inputTransactionIcon = new ImageIcon(
		DashboardPanel.class.getClassLoader().getResource("images/tx_input.png"));
	private static ImageIcon outputTransactionIcon = new ImageIcon(
		DashboardPanel.class.getClassLoader().getResource("images/tx_output.png"));
	private static ImageIcon inoutTransactionIcon = new ImageIcon(
		DashboardPanel.class.getClassLoader().getResource("images/tx_inout.png"));
	private static ImageIcon minedTransactionIcon = new ImageIcon(
		DashboardPanel.class.getClassLoader().getResource("images/tx_mined.png"));
	
	
	private JFrame parentFrame;
	private ZCashInstallationObserver installationObserver;
	private ZCashClientCaller clientCaller;
	private StatusUpdateErrorReporter errorReporter;
	private BackupTracker backupTracker;
	
	private JPanel upperLogoAndWarningPanel = null;
	private JLabel networkAndBlockchainLabel = null;
	private DataGatheringThread<NetworkAndBlockchainInfo> netInfoGatheringThread = null;
	private JPanel blockcahinWarningPanel = null;
	private JLabel blockcahinWarningLabel = null;
	private ExchangeRatePanel exchangeRatePanel = null;

	private Boolean walletIsEncrypted   = null;
	private Integer blockchainPercentage = null;
	
	private String OSInfo              = null;
	private JLabel daemonStatusLabel   = null;
	private DataGatheringThread<DaemonInfo> daemonInfoGatheringThread = null;
	
	private JLabel walletBalanceLabel  = null;
	private DataGatheringThread<WalletBalance> walletBalanceGatheringThread = null;
	
	private JScrollPane transactionsTablePane  = null;
	private String[][] lastTransactionsData = null;
	private DataGatheringThread<String[][]> transactionGatheringThread = null;
	

	public DashboardPanel(JFrame parentFrame,
			              ZCashInstallationObserver installationObserver,
			              ZCashClientCaller clientCaller,
			              StatusUpdateErrorReporter errorReporter,
			              BackupTracker backupTracker)
		throws IOException, InterruptedException, WalletCallException
	{
		this.parentFrame          = parentFrame;
		this.installationObserver = installationObserver;
		this.clientCaller  = clientCaller;
		this.errorReporter = errorReporter;
		this.backupTracker = backupTracker;
		
		this.timers = new ArrayList<Timer>();
		this.threads = new ArrayList<DataGatheringThread<?>>();

		// Build content
		JPanel dashboard = this;
		dashboard.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		dashboard.setLayout(new BorderLayout(0, 0));

		// Upper panel with wallet balance
		upperLogoAndWarningPanel = new JPanel();
		upperLogoAndWarningPanel.setLayout(new BorderLayout(3, 3)); 
		
		JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 9));
		JLabel logoLabel = new JLabel(new ImageIcon(
			this.getClass().getClassLoader().getResource("images/ZEN-yellow.orange-logo-small.png")));
		tempPanel.add(logoLabel);
		JLabel zcLabel = new JLabel("<html><span style=\"font-size:3.3em;font-weight:bold;font-style:italic;\">&nbsp;ZENCash Wallet&nbsp;</span></html>");
		tempPanel.add(zcLabel); 
		tempPanel.setToolTipText("Powered by ZENCash");
		upperLogoAndWarningPanel.add(tempPanel, BorderLayout.WEST);		
		dashboard.add(upperLogoAndWarningPanel, BorderLayout.NORTH);

        PresentationPanel roundedLeftPanel = new PresentationPanel();
        JPanel leftInsidePanel = new JPanel();
        leftInsidePanel.setLayout(new BorderLayout(8, 8));
        leftInsidePanel.add(walletBalanceLabel = new JLabel(), BorderLayout.NORTH);
        leftInsidePanel.add(new JLabel(" "), BorderLayout.CENTER);
        leftInsidePanel.add(this.exchangeRatePanel = new ExchangeRatePanel(errorReporter), BorderLayout.SOUTH);
        roundedLeftPanel.add(leftInsidePanel);
        tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tempPanel.add(roundedLeftPanel);
        dashboard.add(tempPanel, BorderLayout.WEST);
        
		// List of transactions 
        tempPanel = new JPanel(new BorderLayout(0, 0));
        tempPanel.setBorder(BorderFactory.createEmptyBorder(0, 14, 8, 4));
        tempPanel.add(new LatestTransactionsPanel(), BorderLayout.CENTER);
		dashboard.add(tempPanel, BorderLayout.CENTER);

		// Lower panel with installation status
		JPanel installationStatusPanel = new JPanel();
		installationStatusPanel.setLayout(new BorderLayout(3, 3));
		//installationStatusPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		PresentationPanel daemonStatusPanel = new PresentationPanel();
		daemonStatusPanel.add(daemonStatusLabel = new JLabel());
		installationStatusPanel.add(daemonStatusPanel, BorderLayout.WEST);
		
		PresentationPanel networkAndBlockchainPanel = new PresentationPanel();
		networkAndBlockchainPanel.add(networkAndBlockchainLabel = new JLabel());
		installationStatusPanel.add(networkAndBlockchainPanel, BorderLayout.EAST);		
		
		dashboard.add(installationStatusPanel, BorderLayout.SOUTH);

		// Thread and timer to update the daemon status
		this.daemonInfoGatheringThread = new DataGatheringThread<DaemonInfo>(
			new DataGatheringThread.DataGatherer<DaemonInfo>() 
			{
				public DaemonInfo gatherData()
					throws Exception
				{
					long start = System.currentTimeMillis();
					DaemonInfo daemonInfo = DashboardPanel.this.installationObserver.getDaemonInfo();
					long end = System.currentTimeMillis();
					Log.info("Gathering of dashboard daemon status data done in " + (end - start) + "ms." );
					
					return daemonInfo;
				}
			}, 
			this.errorReporter, 2000, true);
		this.threads.add(this.daemonInfoGatheringThread);
		
		ActionListener alDeamonStatus = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					DashboardPanel.this.updateDaemonStatusLabel();
				} catch (Exception ex)
				{
					Log.error("Unexpected error: ", ex);
					DashboardPanel.this.errorReporter.reportError(ex);
				}
			}
		};
		Timer t = new Timer(1000, alDeamonStatus);
		t.start();
		this.timers.add(t);
		
		// Thread and timer to update the wallet balance
		this.walletBalanceGatheringThread = new DataGatheringThread<WalletBalance>(
			new DataGatheringThread.DataGatherer<WalletBalance>() 
			{
				public WalletBalance gatherData()
					throws Exception
				{
					long start = System.currentTimeMillis();
					WalletBalance balance = DashboardPanel.this.clientCaller.getWalletInfo();
					long end = System.currentTimeMillis();
					
					// TODO: move this call to a dedicated one-off gathering thread - this is the wrong place
					// it works but a better design is needed.
					if (DashboardPanel.this.walletIsEncrypted == null)
					{
					    DashboardPanel.this.walletIsEncrypted = DashboardPanel.this.clientCaller.isWalletEncrypted();
					}
					
					Log.info("Gathering of dashboard wallet balance data done in " + (end - start) + "ms." );
					
					return balance;
				}
			}, 
			this.errorReporter, 8000, true);
		this.threads.add(this.walletBalanceGatheringThread);
		
		ActionListener alWalletBalance = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					DashboardPanel.this.updateWalletStatusLabel();
				} catch (Exception ex)
				{
					Log.error("Unexpected error: ", ex);
					DashboardPanel.this.errorReporter.reportError(ex);
				}
			}
		};
		Timer walletBalanceTimer =  new Timer(2000, alWalletBalance);
		walletBalanceTimer.setInitialDelay(1000);
		walletBalanceTimer.start();
		this.timers.add(walletBalanceTimer);

		// Thread and timer to update the transactions table
		this.transactionGatheringThread = new DataGatheringThread<String[][]>(
			new DataGatheringThread.DataGatherer<String[][]>() 
			{
				public String[][] gatherData()
					throws Exception
				{
					long start = System.currentTimeMillis();
					String[][] data =  DashboardPanel.this.getTransactionsDataFromWallet();
					long end = System.currentTimeMillis();
					Log.info("Gathering of dashboard wallet transactions table data done in " + (end - start) + "ms." );
					
					return data;
				}
			}, 
			this.errorReporter, 20000);
		this.threads.add(this.transactionGatheringThread);
		
		ActionListener alTransactions = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{					
					DashboardPanel.this.updateWalletTransactionsTable();
				} catch (Exception ex)
				{
					Log.error("Unexpected error: ", ex);
					DashboardPanel.this.errorReporter.reportError(ex);
				}
			}
		};
		t = new Timer(5000, alTransactions);
		t.start();
		this.timers.add(t);

		// Thread and timer to update the network and blockchain details
		this.netInfoGatheringThread = new DataGatheringThread<NetworkAndBlockchainInfo>(
			new DataGatheringThread.DataGatherer<NetworkAndBlockchainInfo>() 
			{
				public NetworkAndBlockchainInfo gatherData()
					throws Exception
				{
					long start = System.currentTimeMillis();
					NetworkAndBlockchainInfo data =  DashboardPanel.this.clientCaller.getNetworkAndBlockchainInfo();
					long end = System.currentTimeMillis();
					Log.info("Gathering of network and blockchain info data done in " + (end - start) + "ms." );
					
					return data;
				}
			}, 
			this.errorReporter, 10000, true);
		this.threads.add(this.netInfoGatheringThread);
		
		ActionListener alNetAndBlockchain = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					DashboardPanel.this.updateNetworkAndBlockchainLabel();
				} catch (Exception ex)
				{
					Log.error("Unexpected error: ", ex);
					DashboardPanel.this.errorReporter.reportError(ex);
				}
			}
		};
		Timer netAndBlockchainTimer = new Timer(5000, alNetAndBlockchain);
		netAndBlockchainTimer.setInitialDelay(1000);
		netAndBlockchainTimer.start();
		this.timers.add(netAndBlockchainTimer);
	}
	
	
	// May be null!
	public Integer getBlockchainPercentage()
	{
		return this.blockchainPercentage;
	}
	
	
	public DataGatheringThread<String[][]> getTransactionGatheringThread()
	{
		return this.transactionGatheringThread;
	}
	

	private void updateDaemonStatusLabel()
		throws IOException, InterruptedException, WalletCallException
	{
		DaemonInfo daemonInfo = this.daemonInfoGatheringThread.getLastData();
		
		// It is possible there has been no gathering initially
		if (daemonInfo == null)
		{
			return;
		}
		
		String daemonStatus = "<span style=\"color:green;font-weight:bold\">RUNNING</span>";
		if (daemonInfo.status != DAEMON_STATUS.RUNNING)
		{
			daemonStatus = "<span style=\"color:red;font-weight:bold\">NOT RUNNING</span>";
		}
		
		String runtimeInfo = "";
		
		// If the virtual size/CPU are 0 - do not show them
		String virtual = "";
		if (daemonInfo.virtualSizeMB > 0)
		{
			virtual = ", Virtual: " + daemonInfo.virtualSizeMB + " MB";
		}
		
		String cpuPercentage = "";
		if (daemonInfo.cpuPercentage > 0)
		{
			cpuPercentage = ", CPU: " + daemonInfo.cpuPercentage + "%";
		}
		
		if (daemonInfo.status == DAEMON_STATUS.RUNNING)
		{
			runtimeInfo = "<span style=\"font-size:0.8em\">" +
					      "Resident: " + daemonInfo.residentSizeMB + " MB" + virtual +
					       cpuPercentage + "</span>";
		}

		// TODO: what if ZCash directory is non-default...
		File walletDAT = new File(OSUtil.getBlockchainDirectory() + "/wallet.dat");
		if (this.installationObserver.isOnTestNet())
		{
			walletDAT = new File(OSUtil.getBlockchainDirectory() + "/testnet3" + "/wallet.dat");
		}
		
		if (this.OSInfo == null)
		{
			this.OSInfo = OSUtil.getSystemInfo();
		}
		
		String walletEncryption = "";
		// TODO: Use a one-off data gathering thread - better design
		if (this.walletIsEncrypted != null)
		{
			walletEncryption = 
					"<span style=\"font-size:0.8em\">" + 
			        " (" + (this.walletIsEncrypted ? "" : "not ") + "encrypted)" +
			        "</span>";
		}
		
		String text =
			"<html><span style=\"font-weight:bold;color:#303030\">zend</span> status: " + 
		    daemonStatus + ",  " + runtimeInfo + " <br/>" +
			"Wallet: <span style=\"font-weight:bold;color:#303030\">" + walletDAT.getCanonicalPath() + "</span>" + 
			walletEncryption + " <br/> " +
			"<span style=\"font-size:3px\"><br/></span>" +
			"<span style=\"font-size:0.8em\">" +
			"Installation: " + OSUtil.getProgramDirectory() + ", " +
	        "Blockchain: " + OSUtil.getBlockchainDirectory() + " <br/> " +
		    "System: " + this.OSInfo + " </span> </html>";
		this.daemonStatusLabel.setText(text);
	}

	
	private void updateNetworkAndBlockchainLabel()
		throws IOException, InterruptedException
	{
		NetworkAndBlockchainInfo info = this.netInfoGatheringThread.getLastData();
			
		// It is possible there has been no gathering initially
		if (info == null)
		{
			return;
		}
		
		// TODO: Get the start date right after ZCash release - from first block!!!
		final Date startDate = new Date("06 Nov 2016 02:00:00 GMT");
		final Date nowDate = new Date(System.currentTimeMillis());
		
		long fullTime = nowDate.getTime() - startDate.getTime();
		long remainingTime = nowDate.getTime() - info.lastBlockDate.getTime();
		
		String percentage = "100";
		if (remainingTime > 20 * 60 * 1000) // After 20 min we report 100% anyway
		{
			double dPercentage = 100d - (((double)remainingTime / (double) fullTime) * 100d);
			if (dPercentage < 0)
			{
				dPercentage = 0;
			} else if (dPercentage > 100d)
			{
				dPercentage = 100d;
			}
			
			DecimalFormat df = new DecimalFormat("##0.##");
			percentage = df.format(dPercentage);
			
			// Also set a member that may be queried
			this.blockchainPercentage = new Integer((int)dPercentage);
		} else
		{
			this.blockchainPercentage = 100;
		}
		
		// Just in case early on the call returns some junk date
		if (info.lastBlockDate.before(startDate))
		{
			// TODO: write log that we fix minimum date! - this condition should not occur
			info.lastBlockDate = startDate;
		}
		
		String connections = " \u26D7";
		String tickSymbol = " \u2705";
		OS_TYPE os = OSUtil.getOSType();
		// Handling special symbols on Mac OS/Windows 
		// TODO: isolate OS-specific symbol stuff in separate code
		if ((os == OS_TYPE.MAC_OS) || (os == OS_TYPE.WINDOWS))
		{
			connections = " \u21D4";
			tickSymbol = " \u2606";
		}
		
		String tick = "";
		if (percentage.equals("100"))
		{
			tick = "<span style=\"font-weight:bold;font-size:1.4em;color:green\">" + tickSymbol + "</span>";
		}
		
		String netColor = "red";
		if (info.numConnections > 0)
		{
			netColor = "#cc3300";
		}
		
		if (info.numConnections > 2)
		{
			netColor = "black";
		}	
		
		if (info.numConnections > 6)
		{
			netColor = "green";
		}		
				
		String text =
			"<html> " +
		    "Blockchain synchronized: <span style=\"font-weight:bold\">" + 
			percentage + "% </span> " + tick + " <br/>" +
			"Up to: <span style=\"font-size:0.8em;font-weight:bold\">" + 
		    info.lastBlockDate.toLocaleString() + "</span>  <br/> " + 
			"<span style=\"font-size:1px\"><br/></span>" + 
			"Network: <span style=\"font-weight:bold\">" + info.numConnections + " connections</span>" +
			"<span style=\"font-size:1.7em;color:" + netColor + "\">" + connections + "</span>";
		this.networkAndBlockchainLabel.setText(text);
		
		// Possibly show a blockchain synchronization warning
		if (this.blockchainPercentage < 100)
		{
			String warningText = 					
					"<html><span style=\"font-size:1em;font-weight:bold;color:red;\">" +
				    "WARNING: The blockchain is not 100% synchronized. The visible<br/>" +
				    "transactions and wallet balaance reflect an old state of the<br/>" +
				    "wallet as of " + info.lastBlockDate.toLocaleString() + " !" +
				    "</span></html>";
			
			if (this.blockcahinWarningPanel == null)
			{
				// Create a new warning panel
				JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		        PresentationPanel warningPanel = new PresentationPanel();
		        this.blockcahinWarningLabel = new JLabel(warningText);
				warningPanel.add(this.blockcahinWarningLabel);
				tempPanel.add(warningPanel);
				this.blockcahinWarningPanel = tempPanel;
				this.upperLogoAndWarningPanel.add(this.blockcahinWarningPanel, BorderLayout.EAST);
			} else if (this.blockcahinWarningLabel != null)
			{
				this.blockcahinWarningLabel.setText(warningText);
			}
		} else
		{
			if (this.blockcahinWarningPanel != null)
			{
				this.upperLogoAndWarningPanel.remove(this.blockcahinWarningPanel);
				this.upperLogoAndWarningPanel.revalidate();
				this.blockcahinWarningPanel = null;
				this.blockcahinWarningLabel = null;
			}
		}
	}
	

	private void updateWalletStatusLabel()
		throws WalletCallException, IOException, InterruptedException
	{
		WalletBalance balance = this.walletBalanceGatheringThread.getLastData();
		
		// It is possible there has been no gathering initially
		if (balance == null)
		{
			return;
		}
		
		// Format double numbers - else sometimes we get exponential notation 1E-4 ZEN
		DecimalFormat df = new DecimalFormat("########0.00######");
		
		String transparentBalance = df.format(balance.transparentBalance);
		String privateBalance = df.format(balance.privateBalance);
		String totalBalance = df.format(balance.totalBalance);
		
		String transparentUCBalance = df.format(balance.transparentUnconfirmedBalance);
		String privateUCBalance = df.format(balance.privateUnconfirmedBalance);
		String totalUCBalance = df.format(balance.totalUnconfirmedBalance);

		String color1 = transparentBalance.equals(transparentUCBalance) ? "" : "color:#cc3300;";
		String color2 = privateBalance.equals(privateUCBalance)         ? "" : "color:#cc3300;";
		String color3 = totalBalance.equals(totalUCBalance)             ? "" : "color:#cc3300;";
		
		Double usdBalance = (this.exchangeRatePanel != null) ? this.exchangeRatePanel.getUsdPrice() : null;
		String usdBalanceStr = "";
		if (usdBalance != null)
		{
			usdBalance = usdBalance * balance.totalUnconfirmedBalance;
			usdBalanceStr = "<br/>" + "<span style=\"font-family:monospace;font-size:1.8em;" + color3 + "\">" +
			                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + 
		                    "<span style=\"font-weight:bold;font-size:2.1em;\">" + df.format(usdBalance) + " USD</span></span>";
		}
		
		String text =
			"<html>" + 
		    "<span style=\"font-family:monospace;font-size:1.8em;font-weight:bold;" + color1 + "\">BALANCE:</span><br/> " +
		    "<span style=\"font-family:monospace;font-size:0.7em;font-weight:bold;" + color1 + "\"></span><br/> " +
		    "<span style=\"font-family:monospace;font-size:1.8em;" + color1 + "\">Transparent: <span style=\"font-size:1.8em;\">" + 
				transparentUCBalance + " ZEN </span></span><br/> " +
			"<span style=\"font-family:monospace;font-size:1.8em;" + color2 + "\">Private (Z): <span style=\"font-weight:bold;font-size:1.8em;\">" + 
		    	privateUCBalance + " ZEN </span></span><br/> " +
			"<hr/>" +
		    "<span style=\"font-family:monospace;font-size:1.8em;" + color3 + "\">Total (Z+T): <span style=\"font-weight:bold;font-size:2.1em;\">" + 
		    	totalUCBalance + " ZEN </span></span>" +
		    usdBalanceStr +
			"<br/>  </html>";
		
		this.walletBalanceLabel.setText(text);
				
		String toolTip = null;
		if ((!transparentBalance.equals(transparentUCBalance)) ||
		    (!privateBalance.equals(privateUCBalance))         ||
		    (!totalBalance.equals(totalUCBalance)))
		{
			toolTip = "<html>" +
					  "Unconfirmed (unspendable) balance is being shown due to an<br/>" + 
		              "ongoing transaction! Actual confirmed (spendable) balance is:<br/>" +
		              "<span style=\"font-size:5px\"><br/></span>" +
					  "Transparent: " + transparentBalance + " ZEN<br/>" +
		              "Private ( Z ): <span style=\"font-weight:bold\">" + privateBalance + " ZEN</span><br/>" +
					  "Total ( Z+T ): <span style=\"font-weight:bold\">" + totalBalance + " ZEN</span>" +
					  "</html>";
		}
		
		this.walletBalanceLabel.setToolTipText(toolTip);
		
		if (this.parentFrame.isVisible())
		{
			this.backupTracker.handleWalletBalanceUpdate(balance.totalBalance);
		}
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
			// TODO: replace list model for transactions
			/*
			this.remove(transactionsTablePane);
			this.add(transactionsTablePane = new JScrollPane(
			             transactionsTable = this.createTransactionsTable(newTransactionsData)),
			         BorderLayout.CENTER);
		     */
		}

		lastTransactionsData = newTransactionsData;

		this.validate();
		this.repaint();
	}



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
	
	
	// Specific panel class for showing the exchange rates and values in FIAT
	class ExchangeRatePanel
		extends JPanel
	{
		private DataGatheringThread<JsonObject> zenDataGatheringThread = null;
		
		private DataTable table;
		private JScrollPane tablePane;
		
		private Double lastUsdPrice;
		
		public ExchangeRatePanel(StatusUpdateErrorReporter errorReporter)
		{			
			// Start the thread to gather the exchange data
			this.zenDataGatheringThread = new DataGatheringThread<JsonObject>(
				new DataGatheringThread.DataGatherer<JsonObject>() 
				{
					public JsonObject gatherData()
						throws Exception
					{
						long start = System.currentTimeMillis();
						JsonObject exchangeData = ExchangeRatePanel.this.getExchangeDataFromRemoteService();
						long end = System.currentTimeMillis();
						Log.info("Gathering of ZEN Exchange data done in " + (end - start) + "ms." );
							
						return exchangeData;
					}
				}, 
				errorReporter, 60000, true);
			
			this.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 3));
			this.recreateExchangeTable();
			
			// Start the timer to update the table
			ActionListener alExchange = new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e)
				{
					try
					{					
						ExchangeRatePanel.this.recreateExchangeTable();
					} catch (Exception ex)
					{
						Log.error("Unexpected error: ", ex);
						DashboardPanel.this.errorReporter.reportError(ex);
					}
				}
			};
			Timer t = new Timer(30000, alExchange); // TODO: add timer for disposal ???
			t.setInitialDelay(1000);
			t.start();
		}
		
		
		private void recreateExchangeTable()
		{
			if (this.table != null)
			{
				this.remove(this.tablePane);
			}
						
			this.table = new DataTable(getExchangeDataInTableForm(), 
	                                   new String[] { "Exchange information", "Value" });
			Dimension d = this.table.getPreferredSize();
			d.setSize((d.getWidth() * 26) / 10, d.getHeight()); // TODO: better sizing
			this.table.setPreferredScrollableViewportSize(d);
			this.table.setFillsViewportHeight(false);
            this.add(this.tablePane = new JScrollPane(this.table));
		}
		
		
		// Forms the exchange data for a table
		private Object[][] getExchangeDataInTableForm()
		{
			JsonObject data = this.zenDataGatheringThread.getLastData();
			if (data == null)
			{
				data = new JsonObject();
			}
			
			String usdPrice = data.getString("price_usd", "N/A");
			try
			{
				Double usdPriceD = Double.parseDouble(usdPrice);
				usdPrice = new DecimalFormat("########0.00").format(usdPriceD);
				this.lastUsdPrice = usdPriceD;
			} catch (NumberFormatException nfe) { /* Do nothing */ }
			
			String usdMarketCap = data.getString("market_cap_usd", "N/A");
			try
			{
				Double usdMarketCapD = Double.parseDouble(usdMarketCap) / 1000000;
				usdMarketCap = new DecimalFormat("########0.000").format(usdMarketCapD) + " million";
			} catch (NumberFormatException nfe) { /* Do nothing */ }
			
			// Query the object for individual fields
			String tableData[][] = new String[][]
			{
				{ "Current price in USD:",     usdPrice},
				{ "Current price in BTC:",     data.getString("price_btc",          "N/A") },
				{ "ZEN capitalization (USD):", usdMarketCap },
				{ "Daily change (USD price):", data.getString("percent_change_24h", "N/A") + "%"},
			};
			
			return tableData;
		}
		
		
		private Double getUsdPrice()
		{
			return this.lastUsdPrice;
		}
		
				
		// Obtains the ZEN exchange data as a JsonObject
		private JsonObject getExchangeDataFromRemoteService()
		{
			JsonObject data = new JsonObject();
			
			try
			{
				URL u = new URL("https://api.coinmarketcap.com/v1/ticker/zencash");
				Reader r = new InputStreamReader(u.openStream(), "UTF-8");
				JsonArray ar = Json.parse(r).asArray();
				data = ar.get(0).asObject();
			} catch (Exception ioe)
			{
				Log.warning("Could not obtain ZEN exchange information from coinmarketcap.com due to: {0} {1}", 
						    ioe.getClass().getName(), ioe.getMessage());
			}
			
			return data;
		}
	}
	
	
	// Specific panel class for the latest transactions
	class LatestTransactionsPanel
		extends JPanel
	{
		LatestTransactionsList transactionList = null;
		
		public LatestTransactionsPanel()
		{
			final JPanel content = new JPanel();
			content.setLayout(new BorderLayout(3,  3));
			content.add(new JLabel("<html><span style=\"font-size:1.5em;font-weight:bold;font-style:italic;\">Latest transactions:</span></html>"),
					    BorderLayout.NORTH);
			transactionList = new LatestTransactionsList();
			JPanel tempPanel = new JPanel(new BorderLayout(0,  0));
			tempPanel.add(transactionList, BorderLayout.NORTH);
			content.add(tempPanel, BorderLayout.CENTER); 
			
			ActionListener al = new ActionListener() 
			{
				@Override
				public void actionPerformed(ActionEvent e) 
				{
					String[][] transactions = transactionGatheringThread.getLastData();
					if (transactions != null)
					{
						transactionList.updateTransactions(transactions);
					}
				}
			};
			
			Timer latestTransactionsTimer =  new Timer(8000, al);
			latestTransactionsTimer.setInitialDelay(2000);
			latestTransactionsTimer.start();
						
			this.setLayout(new GridLayout(1, 1));
			this.add(content);
		}
		
		
		class LatestTransactionsList
			extends JList<String[]>
		{
			public LatestTransactionsList()
			{
				super();
				this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				this.setBackground(new JPanel().getBackground());
			}
			
			
			public void updateTransactions(String[][] transactions)
			{
				DefaultListModel<String[]> model = new DefaultListModel<String[]>();
				
				// By default only 5 transactions are shown
				int i = 0;
				for (String[] trans : transactions)
				{
					if (++i > 5)
					{
						break;
					}
					
					model.addElement(trans);
				}
				
				this.setModel(model);
			}
			
			
			@Override
			public ListCellRenderer<String[]> getCellRenderer() 
			{
				return new ListCellRenderer<String[]>() 
				{
					@Override
					public Component getListCellRendererComponent(
							JList<? extends String[]> list,
							String[] data, int index, boolean isSelected, boolean cellHasFocus) 
					{					
						return new SingleTransactionPanel(data);
					}
				};
			}

		}
		
		
		class SingleTransactionPanel
			extends JPanel
		{			
			// TODO: depends on the format of the gathering thread
			public SingleTransactionPanel(String[] transactionFeilds)
			{
				this.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 3));
				this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
				
				String destinationAddress = transactionFeilds[5];
				if (destinationAddress.length() > 35)
				{
					destinationAddress = destinationAddress.substring(0, 33) + "...";
				}
				
				
				ImageIcon icon = inoutTransactionIcon;
				if (transactionFeilds[1] != null)
				{
					if (transactionFeilds[1].contains("IN"))
					{
						icon = inputTransactionIcon;
					} else if (transactionFeilds[1].contains("OUT"))
					{
						icon = outputTransactionIcon;
					}
				}
				
				JLabel imgLabel = new JLabel();
				imgLabel.setIcon(icon);
				this.add(imgLabel);
				
				JLabel transacitonInfo = new JLabel(
						"<html><span>" +
						"Type: " + transactionFeilds[0] + ",&nbsp;" +
						"Direction: " + transactionFeilds[1] + "<br/>" +
						"Amount: <span style=\"font-weight:bold\">" + transactionFeilds[3] + " ZEN</span>,&nbsp;" +
						"Date: " + transactionFeilds[4] + "<br/>" +
						"Destination: " + destinationAddress + "<br/>" +
						"</span></html>");
				this.add(transacitonInfo);
			}
		}
	}
	
	
} // End class
