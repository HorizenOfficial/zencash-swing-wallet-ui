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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
 * Dashboard panel - shows summary information.
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
	private static ImageIcon unConfirmedTXIcon = new ImageIcon(
			DashboardPanel.class.getClassLoader().getResource("images/tr_unconfirmed.png"));
	private static ImageIcon confirmedTXIcon = new ImageIcon(
			DashboardPanel.class.getClassLoader().getResource("images/tr_confirmed.png"));
	private static ImageIcon lockClosedIcon = new ImageIcon(
			DashboardPanel.class.getClassLoader().getResource("images/lock_closed_s.png"));
	private static ImageIcon lockOpenIcon = new ImageIcon(
			DashboardPanel.class.getClassLoader().getResource("images/lock_opengreen_s.png"));
	private static ImageIcon connect_0_Icon = new ImageIcon(
			DashboardPanel.class.getClassLoader().getResource("images/connect0_16.png"));
	private static ImageIcon connect_1_Icon = new ImageIcon(
			DashboardPanel.class.getClassLoader().getResource("images/connect1_16.png"));
	private static ImageIcon connect_2_Icon = new ImageIcon(
			DashboardPanel.class.getClassLoader().getResource("images/connect2_16.png"));
	private static ImageIcon connect_3_Icon = new ImageIcon(
			DashboardPanel.class.getClassLoader().getResource("images/connect3_16.png"));
	private static ImageIcon connect_4_Icon = new ImageIcon(
			DashboardPanel.class.getClassLoader().getResource("images/connect4_16.png"));
	
	// Confirmation symbols
	private static String confirmedSymbol    = "\u2690";
	private static String notConfirmedSymbol = "\u2691";
	
	static
	{
		// Windows does not support the flag symbol (Windows 7 by default)
		// TODO: isolate OS-specific symbol codes in a separate class
		OS_TYPE os = OSUtil.getOSType();
		if (os == OS_TYPE.WINDOWS)
		{
			confirmedSymbol = " \u25B7";
			notConfirmedSymbol = " \u25B6";
		}
	}

	
	private JFrame parentFrame;
	private TransactionsDetailPanel detailsPabelForSelection = null;
	
	private ZCashInstallationObserver installationObserver;
	private ZCashClientCaller clientCaller;
	private StatusUpdateErrorReporter errorReporter;
	private BackupTracker backupTracker;
	private LabelStorage labelStorage;
	
	private JPanel upperLogoAndWarningPanel = null;
	
	private JLabel networkAndBlockchainLabel = null;
	private JLabel blockchain100PercentLabel = null;
	private JLabel networkConnectionsIconLabel = null;
	
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
	
	private DataGatheringThread<String[][]> transactionGatheringThread = null;
	private LanguageUtil langUtil;


	public DashboardPanel(JFrame parentFrame,
			              ZCashInstallationObserver installationObserver,
			              ZCashClientCaller clientCaller,
			              StatusUpdateErrorReporter errorReporter,
			              BackupTracker backupTracker,
			              LabelStorage labelStorage)
		throws IOException, InterruptedException, WalletCallException
	{
		this.parentFrame          = parentFrame;
		this.installationObserver = installationObserver;
		this.clientCaller  = clientCaller;
		this.errorReporter = errorReporter;
		this.backupTracker = backupTracker;
		this.labelStorage = labelStorage;
		
		this.timers = new ArrayList<Timer>();
		this.threads = new ArrayList<DataGatheringThread<?>>();

		this.langUtil = LanguageUtil.instance();
		// Build content
		JPanel dashboard = this;
		dashboard.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		dashboard.setLayout(new BorderLayout(0, 0));

		// Upper panel with wallet balance
		upperLogoAndWarningPanel = new JPanel();
		upperLogoAndWarningPanel.setLayout(new BorderLayout(3, 3));

		JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 16));
		JLabel logoLabel = new JLabel(new ImageIcon(
				this.getClass().getClassLoader().getResource("images/ZEN-yellow.orange-logo-small.png")));
		tempPanel.add(logoLabel);
		JLabel zcLabel = new JLabel(langUtil.getString("panel.dashboard.main.label"));
		tempPanel.add(zcLabel);
		tempPanel.setToolTipText(langUtil.getString("panel.dashboard.tooltip"));
		upperLogoAndWarningPanel.add(tempPanel, BorderLayout.WEST);
		dashboard.add(upperLogoAndWarningPanel, BorderLayout.NORTH);

		JPanel roundedLeftPanel = new JPanel();
		roundedLeftPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 0));
		JPanel leftInsidePanel = new JPanel();
		leftInsidePanel.setLayout(new BorderLayout(8, 8));
		leftInsidePanel.add(walletBalanceLabel = new JLabel(), BorderLayout.NORTH);
		roundedLeftPanel.add(leftInsidePanel);
		tempPanel = new JPanel(new BorderLayout(0, 0));
		tempPanel.add(roundedLeftPanel, BorderLayout.NORTH);
		tempPanel.add(this.exchangeRatePanel = new ExchangeRatePanel(errorReporter), BorderLayout.CENTER);
		dashboard.add(tempPanel, BorderLayout.WEST);
        
		// List of transactions 
        tempPanel = new JPanel(new BorderLayout(0, 0));
        tempPanel.setBorder(BorderFactory.createEmptyBorder(0, 14, 8, 4));
        tempPanel.add(new LatestTransactionsPanel(), BorderLayout.CENTER);
		dashboard.add(tempPanel, BorderLayout.CENTER);

		// Lower panel with installation status
		JPanel installationStatusPanel = new JPanel();
		installationStatusPanel.setLayout(new BorderLayout(3, 3));
		PresentationPanel daemonStatusPanel = new PresentationPanel();
		daemonStatusPanel.add(daemonStatusLabel = new JLabel());
		installationStatusPanel.add(daemonStatusPanel, BorderLayout.WEST);
		
		// Build the network and blockchain labels - could be better!
		JPanel netandBCPanel = new JPanel(new BorderLayout(0, 0));
		netandBCPanel.setOpaque(false);
		netandBCPanel.add(networkAndBlockchainLabel = new JLabel(), BorderLayout.CENTER);
		JPanel netandBCIconsPanel = new JPanel(new BorderLayout(0, 0));
		netandBCIconsPanel.setOpaque(false);
		this.blockchain100PercentLabel = new JLabel(" ");
		netandBCIconsPanel.add(this.blockchain100PercentLabel, BorderLayout.NORTH);
		this.networkConnectionsIconLabel = new JLabel(" ");
		this.networkConnectionsIconLabel.setIcon(this.connect_0_Icon);
		netandBCIconsPanel.add(this.networkConnectionsIconLabel, BorderLayout.SOUTH);
		netandBCPanel.add(netandBCIconsPanel, BorderLayout.EAST);
		PresentationPanel networkAndBlockchainPanel = new PresentationPanel();
		networkAndBlockchainPanel.add(netandBCPanel);
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
					String[][] data = DashboardPanel.this.getTransactionsDataFromWallet();
					long end = System.currentTimeMillis();
					Log.info("Gathering of dashboard wallet transactions table data done in " + (end - start) + "ms." );
					
					return data;
				}
			}, 
			this.errorReporter, 20000);
		this.threads.add(this.transactionGatheringThread);
		
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
	
	
	public void setDetailsPanelForSelection(TransactionsDetailPanel detailsPanel)
	{
		this.detailsPabelForSelection = detailsPanel;
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
		
		String daemonStatus = langUtil.getString("panel.dashboard.deamon.status.running");
		if (daemonInfo.status != DAEMON_STATUS.RUNNING)
		{
			daemonStatus = langUtil.getString("panel.dashboard.deamon.status.not.running");
		}
		
		String runtimeInfo = "";
		
		// If the virtual size/CPU are 0 - do not show them
		String virtual = "";
		if (daemonInfo.virtualSizeMB > 0)
		{
			virtual = langUtil.getString("panel.dashboard.deamon.info.virtual", daemonInfo.virtualSizeMB);
		}
		
		String cpuPercentage = "";
		if (daemonInfo.cpuPercentage > 0)
		{
			cpuPercentage = langUtil.getString("panel.dashboard.deamon.info.cpu", daemonInfo.cpuPercentage);
		}
		
		if (daemonInfo.status == DAEMON_STATUS.RUNNING)
		{
			runtimeInfo = langUtil.getString("panel.dashboard.deamon.runtime.info",
					daemonInfo.residentSizeMB, virtual, cpuPercentage);
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
			String encryptionText =
					(this.walletIsEncrypted ? "" :
							langUtil.getString("panel.dashboard.deamon.status.not")) +
							langUtil.getString("panel.dashboard.deamon.status.encrypted");

			walletEncryption =langUtil.getString("panel.dashboard.deamon.status.walletencrypted.text", encryptionText);

		}
		
		String text =langUtil.getString("panel.dashboard.deamon.status.text",
				daemonStatus, runtimeInfo, walletDAT.getCanonicalPath(),
				walletEncryption, OSUtil.getProgramDirectory(),	OSUtil.getBlockchainDirectory(),
				this.OSInfo);

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
		if (remainingTime > 30 * 60 * 1000) // After 30 min we report 100% anyway
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
				
		String text =langUtil.getString("panel.dashboard.network.blockchain.label",
				percentage, info.lastBlockDate.toLocaleString(), info.numConnections );

		this.networkAndBlockchainLabel.setText(text);
		
		// Connections check (typically not an open node with more than 8)
		int numConnections = info.numConnections;
		if (numConnections > 8)
		{
			numConnections = 8;
		}
		
		// Set the correct number of connections (icon)
		switch (numConnections)
		{
		case 8:
		case 7:
			this.networkConnectionsIconLabel.setIcon(connect_4_Icon);
			break;
		case 6:
		case 5:
			this.networkConnectionsIconLabel.setIcon(connect_3_Icon);
			break;
		case 4:
		case 3:
			this.networkConnectionsIconLabel.setIcon(connect_2_Icon);
			break;
		case 2:
		case 1:
			this.networkConnectionsIconLabel.setIcon(connect_1_Icon);
			break;
		case 0:
		default:
			this.networkConnectionsIconLabel.setIcon(connect_0_Icon);
		}
		
		// Set the blockchain synchronization icon
		if (this.blockchainPercentage < 100)
		{
			this.blockchain100PercentLabel.setIcon(null);	
		} else
		{
			this.blockchain100PercentLabel.setIcon(this.confirmedTXIcon);
		}
		
		// Possibly show a blockchain synchronization warning
		if (this.blockchainPercentage < 100)
		{
			String warningText = langUtil.getString("panel.dashboard.synchronisation.warning",
					info.lastBlockDate.toLocaleString());

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
				this.upperLogoAndWarningPanel.repaint();
				this.revalidate(); // The entire dashboard panel
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
			DecimalFormat usdDF = new DecimalFormat("########0.00");
			String formattedUSDVal = usdDF.format(usdBalance);
			
			// make sure ZEN and USD are aligned
			int diff = totalUCBalance.length() - formattedUSDVal.length();
			while (diff-- > 0)
			{
				formattedUSDVal += "&nbsp;";
			}
			
			// TODO: Remove
			//System.out.println("formattedUSDVal = [" + formattedUSDVal + "]");
			usdBalanceStr = langUtil.getString("panel.dashboard.marketcap.usd.balance.string", color3, formattedUSDVal);
		}
		
		String text = langUtil.getString("panel.dashboard.marketcap.usd.balance.text",
				color1, transparentUCBalance, color2, privateUCBalance,
				color3, totalUCBalance, usdBalanceStr);

		// TODO: Remove
		//System.out.println("totalUCBalance = [" + totalUCBalance + "]");
		//System.out.println("usdBalanceStr = [" + usdBalanceStr + "]");
		//System.out.println("FULL TEXT: " + text);
		
		this.walletBalanceLabel.setText(text);
				
		String toolTip = null;
		if ((!transparentBalance.equals(transparentUCBalance)) ||
		    (!privateBalance.equals(privateUCBalance))         ||
		    (!totalBalance.equals(totalUCBalance)))
		{
			toolTip = langUtil.getString("panel.dashboard.balance.tooltip", transparentBalance, privateBalance, totalBalance);
		}
		
		this.walletBalanceLabel.setToolTipText(toolTip);
		
		if (this.parentFrame.isVisible())
		{
			this.backupTracker.handleWalletBalanceUpdate(balance.totalBalance);
		}
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
				if ((!o1[4].equals("N/A")) && (Util.isNumeric(o1[4])))
				{
					d1 = new Date(Long.valueOf(o1[4]).longValue() * 1000L);
				}

				Date d2 = new Date(0);
				if (!o2[4].equals("N/A") && Util.isNumeric(o2[4]))
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
			if ((!trans[4].equals("N/A")) && Util.isNumeric(trans[4]))
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
				
				trans[2] = isConfirmed ?
						(langUtil.getString("panel.dashboard.table.transactions.confirmed.yes") + confirmedSymbol) :
						(langUtil.getString("panel.dashboard.table.transactions.confirmed.no") + notConfirmedSymbol);
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
			
			this.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 18));
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
	                                   new String[] {
														langUtil.getString("panel.dashboard.marketcap.column.exchange.info"),
											   			langUtil.getString("panel.dashboard.marketcap.column.exchange.value")
													});
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
				{ langUtil.getString("panel.dashboard.marketcap.price.usd"),     usdPrice},
				{ langUtil.getString("panel.dashboard.marketcap.price.btc"),     data.getString("price_btc",          "N/A") },
				{ langUtil.getString("panel.dashboard.marketcap.capitalisation"), usdMarketCap },
				{ langUtil.getString("panel.dashboard.marketcap.daily.change"), data.getString("percent_change_24h", "N/A") + "%"},
				{ langUtil.getString("panel.dashboard.marketcap.weekly.change"), data.getString("percent_change_7d", "N/A") + "%"},
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
		String[][] transactions = null;
		
		public LatestTransactionsPanel()
			throws InterruptedException, IOException, WalletCallException
		{
			final JPanel content = new JPanel();
			content.setLayout(new BorderLayout(3,  3));
			content.add(new JLabel(langUtil.getString("panel.dashboard.transactions.label")),
					    BorderLayout.NORTH);
			transactionList = new LatestTransactionsList();
			JPanel tempPanel = new JPanel(new BorderLayout(0,  0));
			tempPanel.add(transactionList, BorderLayout.NORTH);
			content.add(tempPanel, BorderLayout.CENTER); 
			
			// Pre-fill transaction list once
			this.transactions = getTransactionsDataFromWallet();
			transactionList.updateTransactions(this.transactions);
			
			ActionListener al = new ActionListener() 
			{
				@Override
				public void actionPerformed(ActionEvent e) 
				{
					LatestTransactionsPanel.this.transactions = transactionGatheringThread.getLastData();
					if (LatestTransactionsPanel.this.transactions != null)
					{
						transactionList.updateTransactions(LatestTransactionsPanel.this.transactions);
					}
				}
			};
			
			Timer latestTransactionsTimer =  new Timer(8000, al);
			latestTransactionsTimer.setInitialDelay(8000);
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
				
				this.addMouseListener(new MouseAdapter()
		        {
		        	public void mousePressed(MouseEvent e)
		        	{
		                if ((!e.isConsumed()) && (e.isPopupTrigger() || (e.getClickCount() == 2))) 
		                {
		                	LatestTransactionsList list = (LatestTransactionsList)e.getSource();
		                    
		                	// Select also via the right mouse button - seems not to work well
		                    /*{
		                        if (SwingUtilities.isRightMouseButton(e))
		                        {
		                            int row = list.locationToIndex(e.getPoint());
		                            if (row > 0)
		                            {
		                            	list.setSelectedIndex(row);
		                            }
		                        }
		                    }*/
		                	
		                	if (list.getSelectedValue() != null)
		                    {
		                    	String[] transaction = list.getSelectedValue();
		                    	// Select the right transaction here
		                    	if (detailsPabelForSelection != null)
		                    	{
		                    		detailsPabelForSelection.selectTransactionWithID(transaction[6]);
		                    	}
		                    	e.consume();
		                    } 		                    
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
			public SingleTransactionPanel(String[] transactionFields)
			{
				this.setLayout(new FlowLayout(FlowLayout.LEFT, 3, 3));
				this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
				
				String destinationAddress = transactionFields[5];

				String label = DashboardPanel.this.labelStorage.getLabel(destinationAddress);
				if ((label != null) && (label.length() > 0))
				{
					destinationAddress = label + " - " + destinationAddress;
				}
				
				if (destinationAddress.length() > 37)
				{
					destinationAddress = destinationAddress.substring(0, 37) + "...";
				}
				
				// Set the correct icon for input/output
				ImageIcon inOutIcon = inoutTransactionIcon;
				if (transactionFields[1] != null)
				{
					if (transactionFields[1].contains("IN"))
					{
						inOutIcon = inputTransactionIcon;
					} else if (transactionFields[1].contains("OUT"))
					{
						inOutIcon = outputTransactionIcon;
					}
				}
				
				JLabel imgLabel = new JLabel();
				imgLabel.setIcon(inOutIcon);
				this.add(imgLabel);
				
				// Set the two icons for public/private and confirmations
				ImageIcon confirmationIcon = 
					transactionFields[2].contains((langUtil.getString("panel.dashboard.table.transactions.confirmed.yes"))) 
					? confirmedTXIcon : unConfirmedTXIcon;
				ImageIcon pubPrivIcon = 
						transactionFields[0].contains("Private") ? lockClosedIcon : lockOpenIcon;
				JPanel iconsPanel = new JPanel(new BorderLayout(0, 1));
				iconsPanel.add(new JLabel(pubPrivIcon), BorderLayout.SOUTH);
				iconsPanel.add(new JLabel(confirmationIcon), BorderLayout.NORTH);
				this.add(iconsPanel);
				
				this.add(new JLabel("<html>&nbsp;</html>"));
				
				// Set the transaction information
				JLabel transactionInfo = new JLabel(
						langUtil.getString("panel.dashboard.transactions.info",
								transactionFields[0],
								transactionFields[1],
								transactionFields[2],
								transactionFields[3],
								transactionFields[4],
								destinationAddress
						)
				);
				this.add(transactionInfo);
			}
		}
	}
	
	
} // End class
