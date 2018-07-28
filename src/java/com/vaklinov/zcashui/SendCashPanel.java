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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;

import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;


/**
 * Provides the functionality for sending cash
 */
public class SendCashPanel
	extends WalletTabPanel
{
	private ZCashClientCaller         clientCaller;
	private StatusUpdateErrorReporter errorReporter;
	private ZCashInstallationObserver installationObserver;
	private BackupTracker             backupTracker;
	private LabelStorage labelStorage;
	
	private JComboBox  balanceAddressCombo     = null;
	private JPanel     comboBoxParentPanel     = null;
	private String[][] lastAddressBalanceData  = null;
	private String[]   comboBoxItems           = null;
	private DataGatheringThread<String[][]> addressBalanceGatheringThread = null;
	
	private JTextField destinationAddressField = null;
	private JTextField destinationAmountField  = null;
	private JTextField destinationMemoField    = null;	
	private JTextField transactionFeeField     = null;	
	
	private JCheckBox  sendChangeBackToSourceAddress = null;
	
	private JButton    sendButton              = null;
	
	private JPanel       operationStatusPanel        = null;
	private JLabel       operationStatusLabel        = null;
	private JProgressBar operationStatusProhgressBar = null;
	private Timer        operationStatusTimer        = null;
	private String       operationStatusID           = null;
	private int          operationStatusCounter      = 0;
	private LanguageUtil langUtil;

	
	public SendCashPanel(ZCashClientCaller clientCaller,  
			             StatusUpdateErrorReporter errorReporter,
			             ZCashInstallationObserver installationObserver,
			             BackupTracker backupTracker,
			             LabelStorage labelStorage)
		throws IOException, InterruptedException, WalletCallException
	{
		langUtil = LanguageUtil.instance();
		this.timers = new ArrayList<Timer>();
		this.threads = new ArrayList<DataGatheringThread<?>>();
		
		this.clientCaller = clientCaller;
		this.errorReporter = errorReporter;
		this.installationObserver = installationObserver;
		this.backupTracker = backupTracker;
		this.labelStorage = labelStorage;
		
		// Build content
		this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		this.setLayout(new BorderLayout());
		JPanel sendCashPanel = new JPanel();
		this.add(sendCashPanel, BorderLayout.NORTH);
		sendCashPanel.setLayout(new BoxLayout(sendCashPanel, BoxLayout.Y_AXIS));
		sendCashPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(new JLabel(langUtil.getString("send.cash.panel.label")));
		tempPanel.add(new JLabel(langUtil.getString("send.cash.panel.label.info")));
		sendCashPanel.add(tempPanel);

		balanceAddressCombo = new JComboBox<>(new String[] { "" });
		comboBoxParentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		comboBoxParentPanel.add(balanceAddressCombo);
		sendCashPanel.add(comboBoxParentPanel);
		
		JLabel dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 3));
		sendCashPanel.add(dividerLabel);

		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(new JLabel(langUtil.getString("send.cash.panel.label.destination.address")));
		sendCashPanel.add(tempPanel);
		
		destinationAddressField = new JTextField(73);
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tempPanel.add(destinationAddressField);
		sendCashPanel.add(tempPanel);
				
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 3));
		sendCashPanel.add(dividerLabel);

		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(new JLabel(langUtil.getString("send.cash.panel.label.memo")));
		tempPanel.add(new JLabel(langUtil.getString("send.cash.panel.label.memo.info")));
		sendCashPanel.add(tempPanel);
		
		destinationMemoField = new JTextField(73);
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tempPanel.add(destinationMemoField);
		sendCashPanel.add(tempPanel);		
		
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 3));
		sendCashPanel.add(dividerLabel);

		// Construct a more complex panel for the amount and transaction fee
		JPanel amountAndFeePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JPanel amountPanel = new JPanel(new BorderLayout());
		amountPanel.add(new JLabel(langUtil.getString("send.cash.panel.label.amount")), BorderLayout.NORTH);
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(destinationAmountField = new JTextField(13));
		destinationAmountField.setHorizontalAlignment(SwingConstants.RIGHT);
		tempPanel.add(new JLabel(" ZEN    "));
		amountPanel.add(tempPanel, BorderLayout.SOUTH);

		JPanel feePanel = new JPanel(new BorderLayout());
		feePanel.add(new JLabel(langUtil.getString("send.cash.panel.label.fee")), BorderLayout.NORTH);
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(transactionFeeField = new JTextField(13));
		transactionFeeField.setText("0.0001"); // Default value
		transactionFeeField.setHorizontalAlignment(SwingConstants.RIGHT);		
		tempPanel.add(new JLabel(" ZEN"));
		feePanel.add(tempPanel, BorderLayout.SOUTH);
		
		JPanel sendChangeBoxPanel = new JPanel(new BorderLayout());
		sendChangeBoxPanel.add(new JLabel(" "), BorderLayout.NORTH);
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(new JLabel("      "));
		tempPanel.add(sendChangeBackToSourceAddress = new JCheckBox(langUtil.getString("send.cash.panel.checkbox.send.change.back")));
		sendChangeBoxPanel.add(tempPanel, BorderLayout.SOUTH);

		amountAndFeePanel.add(amountPanel);
		amountAndFeePanel.add(feePanel);
		amountAndFeePanel.add(sendChangeBoxPanel);
		sendCashPanel.add(amountAndFeePanel);		
		
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 3));
		sendCashPanel.add(dividerLabel);

		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendButton = new JButton(langUtil.getString("send.cash.panel.button.send") + "   \u27A4\u27A4\u27A4"));
		sendCashPanel.add(tempPanel);

		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 5));
		sendCashPanel.add(dividerLabel);
		
		JPanel warningPanel = new JPanel();
		warningPanel.setLayout(new BorderLayout(7, 3));
		JLabel warningL = new JLabel(langUtil.getString("send.cash.panel.label.send.warning"));
		warningPanel.add(warningL, BorderLayout.NORTH);
		sendCashPanel.add(warningPanel);
		
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 15));
		sendCashPanel.add(dividerLabel);
		
		// Build the operation status panel
		operationStatusPanel = new JPanel();
		sendCashPanel.add(operationStatusPanel);
		operationStatusPanel.setLayout(new BoxLayout(operationStatusPanel, BoxLayout.Y_AXIS));
		
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(new JLabel(langUtil.getString("send.cash.panel.label.last.operation.status")));
        tempPanel.add(operationStatusLabel = new JLabel("N/A"));
        operationStatusPanel.add(tempPanel);		
		
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 6));
		operationStatusPanel.add(dividerLabel);

		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(new JLabel(langUtil.getString("send.cash.panel.label.last.operation.progress")));
        tempPanel.add(operationStatusProhgressBar = new JProgressBar(0, 200));
        operationStatusProhgressBar.setPreferredSize(new Dimension(250, 17));
        operationStatusPanel.add(tempPanel);		
        
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 13));
		operationStatusPanel.add(dividerLabel);
		
		// Wire the buttons
		sendButton.addActionListener(new ActionListener() 
		{	
			public void actionPerformed(ActionEvent e) 
			{
				try
			    {
					SendCashPanel.this.sendCash();
				} catch (Exception ex)
				{
					Log.error("Unexpected error: ", ex);
					
					String errMessage = "";
					if (ex instanceof WalletCallException)
					{
						errMessage = ((WalletCallException)ex).getMessage().replace(",", ",\n");
					}
					
					JOptionPane.showMessageDialog(
							SendCashPanel.this.getRootPane().getParent(), 
							langUtil.getString("send.cash.panel.option.pane.error.text",errMessage),
							langUtil.getString("send.cash.panel.option.pane.error.title"),
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});

		// Update the balances via timer and data gathering thread
		this.addressBalanceGatheringThread = new DataGatheringThread<String[][]>(
			new DataGatheringThread.DataGatherer<String[][]>() 
			{
				public String[][] gatherData()
					throws Exception
				{
					long start = System.currentTimeMillis();
					String[][] data = SendCashPanel.this.getAddressPositiveBalanceDataFromWallet();
					long end = System.currentTimeMillis();
					Log.info("Gathering of address/balance table data done in " + (end - start) + "ms." );
					
					return data;
				}
			}, 
			this.errorReporter, 10000, true);
		this.threads.add(addressBalanceGatheringThread);
		
		ActionListener alBalancesUpdater = new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					// TODO: if the user has opened the combo box - this closes it (maybe fix)
					SendCashPanel.this.updateWalletAddressPositiveBalanceComboBox();
				} catch (Exception ex)
				{
					Log.error("Unexpected error: ", ex);
					SendCashPanel.this.errorReporter.reportError(ex);
				}
			}
		};
		Timer timerBalancesUpdater = new Timer(15000, alBalancesUpdater);
		timerBalancesUpdater.setInitialDelay(3000);
		timerBalancesUpdater.start();
		this.timers.add(timerBalancesUpdater);
		
		// Add a popup menu to the destination address field - for convenience
		JMenuItem paste = new JMenuItem(langUtil.getString("send.cash.panel.menu.item.paste"));
		final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(paste);
        paste.addActionListener(new ActionListener() 
        {	
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					String address = (String)Toolkit.getDefaultToolkit().getSystemClipboard().
							         getData(DataFlavor.stringFlavor);
					if ((address != null) && (address.trim().length() > 0))
					{
						SendCashPanel.this.destinationAddressField.setText(address);
					}
				} catch (Exception ex)
				{
					Log.error("Unexpected error", ex);
					// TODO: clipboard exception handling - do it better
					// java.awt.datatransfer.UnsupportedFlavorException: Unicode String
					//SendCashPanel.this.errorReporter.reportError(ex);
				}
			}
		});
        
        this.destinationAddressField.addMouseListener(new MouseAdapter()
        {
        	public void mousePressed(MouseEvent e)
        	{
                if ((!e.isConsumed()) && e.isPopupTrigger())
                {
                    popupMenu.show(e.getComponent(), e.getPoint().x, e.getPoint().y);
                    e.consume();
                };
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
	
	
	private void sendCash()
		throws WalletCallException, IOException, InterruptedException
	{
		if (balanceAddressCombo.getItemCount() <= 0)
		{
			JOptionPane.showMessageDialog(
				SendCashPanel.this.getRootPane().getParent(), 
				langUtil.getString("send.cash.panel.option.pane.no.funds.text"),
				langUtil.getString("send.cash.panel.option.pane.no.funds.title"),
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		if (this.balanceAddressCombo.getSelectedIndex() < 0)
		{
			JOptionPane.showMessageDialog(
				SendCashPanel.this.getRootPane().getParent(),
					langUtil.getString("send.cash.panel.option.pane.select.source.text"),
					langUtil.getString("send.cash.panel.option.pane.select.source.title"),
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		final String sourceAddress = this.lastAddressBalanceData[this.balanceAddressCombo.getSelectedIndex()][1];
		final String destinationAddress = this.destinationAddressField.getText();
		final String memo = this.destinationMemoField.getText();
		final String amount = this.destinationAmountField.getText();
		final String fee = this.transactionFeeField.getText();
		
		Log.info("Send button processing: Parameters are: from address: {0}, to address: {1}, " + 
	             "amount: {2}, memo: {3}, transaction fee: {4}",
	             sourceAddress, destinationAddress, amount, memo, fee);

		// Verify general correctness.
		String errorMessage = null;
		
		if ((sourceAddress == null) || (sourceAddress.trim().length() <= 20))
		{
			errorMessage = langUtil.getString("send.cash.panel.option.pane.error.source.address.invalid");
		} else if (sourceAddress.length() > 512)
		{
			errorMessage = langUtil.getString("send.cash.panel.option.pane.error.source.address.too.long");
		}
		
		// TODO: full address validation
		if ((destinationAddress == null) || (destinationAddress.trim().length() <= 0))
		{
			errorMessage = langUtil.getString("send.cash.panel.option.pane.error.destination.address.invalid");
		} else if (destinationAddress.trim().length() <= 20)
		{
			errorMessage = langUtil.getString("send.cash.panel.option.pane.error.destination.address.too.short");
		} else if (destinationAddress.length() > 512)
		{
			errorMessage = langUtil.getString("send.cash.panel.option.pane.error.destination.address.too.long");
		} else if (destinationAddress.trim().length() != destinationAddress.length())
		{
			errorMessage = langUtil.getString("send.cash.panel.option.pane.error.destination.address.has.spaces");
		}
				
		if ((amount == null) || (amount.trim().length() <= 0))
		{
			errorMessage = langUtil.getString("send.cash.panel.option.pane.error.amount.invalid");
		} else 
		{
			try 
			{
				double d = Double.valueOf(amount);
				if (d < 0)
				{
					errorMessage = langUtil.getString("send.cash.panel.option.pane.error.amount.negative");
				}
			} catch (NumberFormatException nfe)
			{
				errorMessage = langUtil.getString("send.cash.panel.option.pane.error.amount.not.number");
			}
		}
		
		if ((fee == null) || (fee.trim().length() <= 0))
		{
			errorMessage = langUtil.getString("send.cash.panel.option.pane.error.fee.invalid");
		} else 
		{
			try 
			{
				double d = Double.valueOf(fee);
				if (d < 0)
				{
					errorMessage = langUtil.getString("send.cash.panel.option.pane.error.fee.negative");
				}
			} catch (NumberFormatException nfe)
			{
				errorMessage = langUtil.getString("send.cash.panel.option.pane.error.fee.not.number");
			}
		}

		if (errorMessage != null)
		{
			JOptionPane.showMessageDialog(
				SendCashPanel.this.getRootPane().getParent(), 
				errorMessage, langUtil.getString("send.cash.panel.option.pane.error.incorrect.sending.parameters"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		// Prevent accidental sending to non-ZEN addresses (which zend supports) probably because of
		// ZClassic compatibility
		if (!installationObserver.isOnTestNet())
		{
			if (!(destinationAddress.startsWith("zc") || 
				  destinationAddress.startsWith("zn") ||
				  destinationAddress.startsWith("zs")))
			{
				Object[] options = { "OK" };

				JOptionPane.showOptionDialog(
					SendCashPanel.this.getRootPane().getParent(), 
					langUtil.getString("send.cash.panel.option.pane.error.destination.address.incorrect.text", destinationAddress),
					langUtil.getString("send.cash.panel.option.pane.error.destination.address.incorrect.title"),
					JOptionPane.DEFAULT_OPTION, 
					JOptionPane.ERROR_MESSAGE,
					null, 
					options, 
					options[0]);
				
			    return; // Do not send anything!
			}
		}
		
		// If a memo is specified, make sure the destination is a Z address.
		if ((!installationObserver.isOnTestNet()) && 
			(!Util.stringIsEmpty(memo)) &&
			(!Util.isZAddress(destinationAddress)))
		{
	        int reply = JOptionPane.showConfirmDialog(
	        		SendCashPanel.this.getRootPane().getParent(), 
					langUtil.getString("send.cash.panel.option.pane.error.destination.address.notz.text", destinationAddress),
					langUtil.getString("send.cash.panel.option.pane.error.destination.address.notz.title"),
			        JOptionPane.YES_NO_OPTION);
			        
			if (reply == JOptionPane.NO_OPTION) 
			{
			   	return;
			}
		}		
		
        // Warn the user if there are too many fractional digits in the amount and fee
		if (hasExcessiveFractionalDigits(amount))
		{
	        int reply = JOptionPane.showConfirmDialog(
	        		SendCashPanel.this.getRootPane().getParent(), 
					langUtil.getString("send.cash.panel.option.pane.error.destination.amount.fractional.digits", amount),
					langUtil.getString("send.cash.panel.option.pane.error.destination.fractional.digits.title"),
			        JOptionPane.YES_NO_OPTION);
			        
			if (reply == JOptionPane.NO_OPTION) 
			{
			   	return;
			}
		}
		
		if (hasExcessiveFractionalDigits(fee))
		{
	        int reply = JOptionPane.showConfirmDialog(
	        		SendCashPanel.this.getRootPane().getParent(), 
					langUtil.getString("send.cash.panel.option.pane.error.destination.fee.fractional.digits", fee),
					langUtil.getString("send.cash.panel.option.pane.error.destination.fractional.digits.title"),
			        JOptionPane.YES_NO_OPTION);
			        
			if (reply == JOptionPane.NO_OPTION) 
			{
			   	return;
			}
		}
		
		// Get a confirmation from the user about the operation
        String userDir = OSUtil.getSettingsDirectory();
        File sendCashNotToBeShownFlagFile = new File(userDir + File.separator + "sendCashWarningNotToBeShown.flag");
        if (!sendCashNotToBeShownFlagFile.exists())
        {
        	Object[] options = 
        	{ 
        		langUtil.getString("send.cash.panel.option.pane.confirm.operation.button.yes"),
        		langUtil.getString("send.cash.panel.option.pane.confirm.operation.button.no"),
        		langUtil.getString("send.cash.panel.option.pane.confirm.operation.button.not.again")
        	};

    		int option;
    		
    		if (Util.stringIsEmpty(memo))
    		{
    			option = JOptionPane.showOptionDialog(
    				SendCashPanel.this.getRootPane().getParent(), 
    				langUtil.getString("send.cash.panel.option.pane.confirm.operation.text", 
    						           amount, sourceAddress, destinationAddress, fee), 
    			    langUtil.getString("send.cash.panel.option.pane.confirm.operation.title"),
    			    JOptionPane.DEFAULT_OPTION, 
    			    JOptionPane.QUESTION_MESSAGE,
    			    null, 
    			    options, 
    			    options[0]);
    		} else
    		{
    			option = JOptionPane.showOptionDialog(
       				SendCashPanel.this.getRootPane().getParent(), 
       				langUtil.getString("send.cash.panel.option.pane.confirm.operation.text.with.memo", 
       						           amount, sourceAddress, destinationAddress, fee, Util.blockWrapString(memo, 50)), 
       			    langUtil.getString("send.cash.panel.option.pane.confirm.operation.title"),
       			    JOptionPane.DEFAULT_OPTION, 
       			    JOptionPane.QUESTION_MESSAGE,
       			    null, 
       			    options, 
       			    options[0]);    			
    		}
    		
    	    if (option == 2)
    	    {
    	    	sendCashNotToBeShownFlagFile.createNewFile();
    	    }
    	    
    	    if (option == 1)
    	    {
    	    	return;
    	    }
    	    
    	    // 4e075d661a12376b13e9bd95831bc6a002824e029ff50059bd1e28662971e055
        } 
				
        boolean bEncryptedWallet = false;
		// Backend operations are wrapped inside a wait cursor
        Cursor oldCursor = this.getRootPane().getParent().getCursor();
		try
		{
			this.getRootPane().getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			// Check for encrypted wallet
			bEncryptedWallet = this.clientCaller.isWalletEncrypted();
			if (bEncryptedWallet)
			{
				this.getRootPane().getParent().setCursor(oldCursor);
				PasswordDialog pd = new PasswordDialog((JFrame)(SendCashPanel.this.getRootPane().getParent()));
				pd.setVisible(true);
				this.getRootPane().getParent().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				if (!pd.isOKPressed())
				{
					return;
				}
				
				this.clientCaller.unlockWallet(pd.getPassword());
			}
			
			boolean sendChangeBackToAddres = this.sendChangeBackToSourceAddress.isSelected();
			Log.info("Change send back flag: {0}", sendChangeBackToAddres);
			if (sendChangeBackToAddres)
			{
				if (this.warnAndCheckConditionsForSendingBackChange(sourceAddress, destinationAddress, amount, memo, fee))
				{
					String balance = this.clientCaller.getBalanceForAddress(sourceAddress);
					// Call the send method with change going back to source address
					operationStatusID = this.clientCaller.sendCashWithReturnOfChange(sourceAddress, destinationAddress, balance, amount, memo, fee);
				} else
				{
					return; // Stop the operation
				}
			} else
			{
				// Call the wallet send method - old style
				operationStatusID = this.clientCaller.sendCash(sourceAddress, destinationAddress, amount, memo, fee);
			}
					
			// Make sure the keypool has spare addresses
			if ((this.backupTracker.getNumTransactionsSinceLastBackup() % 5) == 0)
			{
				this.clientCaller.keypoolRefill(100);
			}
		} finally
		{
			this.getRootPane().getParent().setCursor(oldCursor);
		}
		
		// Disable controls after send
		sendChangeBackToSourceAddress.setEnabled(false);
		sendButton.setEnabled(false);
		balanceAddressCombo.setEnabled(false);
		destinationAddressField.setEnabled(false);
		destinationAmountField.setEnabled(false);
		destinationMemoField.setEnabled(false);
		transactionFeeField.setEnabled(false);
		
		
		final boolean bEncryptedWalletForThread = bEncryptedWallet;
		// Start a data gathering thread specific to the operation being executed - this is done in a separate 
		// thread since the server responds more slowly during JoinSplits and this blocks the GUI somewhat.
		final DataGatheringThread<Boolean> opFollowingThread = new DataGatheringThread<Boolean>(
			new DataGatheringThread.DataGatherer<Boolean>() 
			{
				public Boolean gatherData()
					throws Exception
				{
					long start = System.currentTimeMillis();
					Boolean result = clientCaller.isSendingOperationComplete(operationStatusID);
					long end = System.currentTimeMillis();
					Log.info("Checking for operation " + operationStatusID + " status done in " + (end - start) + "ms." );
					
					return result;
				}
			}, 
			this.errorReporter, 2000, true);
		
		// Start a timer to update the progress of the operation
		operationStatusCounter = 0;
		operationStatusTimer = new Timer(2000, new ActionListener() 
		{
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					// TODO: Handle errors in case of restarted server while wallet is sending ...
					Boolean opComplete = opFollowingThread.getLastData();
					
					if ((opComplete != null) && opComplete.booleanValue())
					{
						// End the special thread used to follow the operation
						opFollowingThread.setSuspended(true);
						
						SendCashPanel.this.reportCompleteOperationToTheUser(
							amount, sourceAddress, destinationAddress);
						
						// Lock the wallet again 
						if (bEncryptedWalletForThread)
						{
							SendCashPanel.this.clientCaller.lockWallet();
						}
						
						// Restore controls etc.
						operationStatusCounter = 0;
						operationStatusID      = null;
						operationStatusTimer.stop();
						operationStatusTimer = null;
						operationStatusProhgressBar.setValue(0);
						
						sendChangeBackToSourceAddress.setEnabled(true);
						sendButton.setEnabled(true);
						balanceAddressCombo.setEnabled(true);
						destinationAddressField.setEnabled(true);
						destinationAmountField.setEnabled(true);
						transactionFeeField.setEnabled(true);
						destinationMemoField.setEnabled(true);
					} else
					{
						// Update the progress
						operationStatusLabel.setText(langUtil.getString("send.cash.panel.operation.status.progress.label"));
						operationStatusCounter += 2;
						int progress = 0;
						if (operationStatusCounter <= 100)
						{
							progress = operationStatusCounter;
						} else
						{
							progress = 100 + (((operationStatusCounter - 100) * 6) / 10);
						}
						operationStatusProhgressBar.setValue(progress);
					}
					
					SendCashPanel.this.repaint();
				} catch (Exception ex)
				{
					Log.error("Unexpected error: ", ex);
					SendCashPanel.this.errorReporter.reportError(ex);
				}
			}
		});
		operationStatusTimer.setInitialDelay(0);
		operationStatusTimer.start();
	}

	
	public void prepareForSending(String address) 
	{
	    destinationAddressField.setText(address);
	}
	
	
	private void updateWalletAddressPositiveBalanceComboBox()
		throws WalletCallException, IOException, InterruptedException
	{
		String[][] newAddressBalanceData = this.addressBalanceGatheringThread.getLastData();
		
		// The data may be null if nothing is yet obtained
		if (newAddressBalanceData == null)
		{
			return;
		}
		
		lastAddressBalanceData = newAddressBalanceData;
		
		comboBoxItems = new String[lastAddressBalanceData.length];
		for (int i = 0; i < lastAddressBalanceData.length; i++)
		{
			// Form the current combo item for sending cash. If an address label is available, it gets 
			// displayed first. If the overall string is too long it gets cut at 120 chars
			String address = lastAddressBalanceData[i][1];
			String formattedBalance = new DecimalFormat("########0.00######").format(Double.valueOf(lastAddressBalanceData[i][0]));
			String label = this.labelStorage.getLabel(address); // Empty str if not found
			if (label.length() > 0)
			{
				label = "[" + label + "] ";
			}
			String item = label + formattedBalance + " ZEN - " + address;
			if (item.length() > 120)
			{
				item = item.substring(0, 118) + "...";
			}
			
			// Do numeric formatting or else we may get 1.1111E-5
			comboBoxItems[i] = item;
		}
		
		int selectedIndex = balanceAddressCombo.getSelectedIndex();
		boolean isEnabled = balanceAddressCombo.isEnabled();
		this.comboBoxParentPanel.remove(balanceAddressCombo);
		balanceAddressCombo = new JComboBox<>(comboBoxItems);
		comboBoxParentPanel.add(balanceAddressCombo);
		if ((balanceAddressCombo.getItemCount() > 0) &&
			(selectedIndex >= 0) &&
			(balanceAddressCombo.getItemCount() > selectedIndex))
		{
			balanceAddressCombo.setSelectedIndex(selectedIndex);
		}
		balanceAddressCombo.setEnabled(isEnabled);

		this.validate();
		this.repaint();
	}


	private String[][] getAddressPositiveBalanceDataFromWallet()
		throws WalletCallException, IOException, InterruptedException
	{
		// Z Addresses - they are OK
		String[] zAddresses = clientCaller.getWalletZAddresses();
		
		// T Addresses created inside wallet that may be empty
		String[] tAddresses = this.clientCaller.getWalletAllPublicAddresses();
		Set<String> tStoredAddressSet = new HashSet<>();
		for (String address : tAddresses)
		{
			tStoredAddressSet.add(address);
		}
		
		// T addresses with unspent outputs (even if not GUI created)...
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
		
		String[][] tempAddressBalances = new String[zAddresses.length + tAddressesCombined.size()][];
		
		int count = 0;

		for (String address : tAddressesCombined)
		{
			String balance = this.clientCaller.getBalanceForAddress(address);
			if (Double.valueOf(balance) > 0)
			{
				tempAddressBalances[count++] = new String[] 
				{  
					balance, address
				};
			}
		}
		
		for (String address : zAddresses)
		{
			String balance = this.clientCaller.getBalanceForAddress(address);
			if (Double.valueOf(balance) > 0)
			{
				tempAddressBalances[count++] = new String[] 
				{  
					balance, address
				};
			}
		}

		String[][] addressBalances = new String[count][];
		System.arraycopy(tempAddressBalances, 0, addressBalances, 0, count);
		
		return addressBalances;
	}
	
	
	private void reportCompleteOperationToTheUser(String amount, String sourceAddress, String destinationAddress)
		throws InterruptedException, WalletCallException, IOException, URISyntaxException
	{
		if (clientCaller.isCompletedOperationSuccessful(operationStatusID))
		{
			operationStatusLabel.setText(langUtil.getString("send.cash.panel.operation.status.success.label"));
			String TXID = clientCaller.getSuccessfulOperationTXID(operationStatusID);
			
			Object[] options = langUtil.getString("send.cash.panel.operation.complete.report").split(":");
			
			int option = JOptionPane.showOptionDialog(
				SendCashPanel.this.getRootPane().getParent(),
					langUtil.getString("send.cash.panel.operation.complete.report.success.text",
						amount,
						sourceAddress,
						destinationAddress,
						TXID),
					langUtil.getString("send.cash.panel.operation.complete.report.success.title"),
				JOptionPane.DEFAULT_OPTION, 
				JOptionPane.INFORMATION_MESSAGE,
				null, 
				options, 
				options[0]);
			
		    if (option == 1)
		    {
		    	// Copy the transaction ID to clipboard
		    	Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(new StringSelection(TXID), null);
		    } else if (option == 2)
		    {
		    	// Open block explorer
				Log.info("Transaction ID for block explorer is: " + TXID);
				// TODO: code duplication with transactions table
				String urlPrefix = "https://explorer.zensystem.io/tx/";
				if (installationObserver.isOnTestNet())
				{
					urlPrefix = "https://explorer-testnet.zen-solutions.io/tx/";
				}
				Desktop.getDesktop().browse(new URL(urlPrefix + TXID).toURI());
		    }
		    
		    // Call the backup tracker - to remind the user
		    this.backupTracker.handleNewTransaction();
		} else
		{
			String errorMessage = clientCaller.getOperationFinalErrorMessage(operationStatusID); 
			operationStatusLabel.setText(
				langUtil.getString("send.cash.panel.operation.status.error.label", errorMessage));

			JOptionPane.showMessageDialog(
					SendCashPanel.this.getRootPane().getParent(), 
					langUtil.getString("send.cash.panel.option.pane.error.report.text",errorMessage),
					langUtil.getString("send.cash.panel.option.pane.error.report.title"), JOptionPane.ERROR_MESSAGE);

		}
	}
	
	
	// Checks if a number has more than 8 fractional digits. This is not normally allowed for ZEN
	// Input must be a decimal number!
	private boolean hasExcessiveFractionalDigits(String field)
	{
		BigDecimal num = new BigDecimal(field);
		DecimalFormatSymbols decSymbols = new DecimalFormatSymbols(Locale.ROOT);
		DecimalFormat longFormat = new DecimalFormat("############################0.00###############################", decSymbols);
		String formattedNumber = longFormat.format(num);
		String fractionalPart = formattedNumber.substring(formattedNumber.indexOf(".") + 1);
			
		if (fractionalPart.length() > 8)
		{
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * Checks the conditions necessary for sending back the change. Also issues a warning to the user on the nature of this operation.
	 * 
	 * @param sourceAddress
	 * @param destinationAddress
	 * @param amount
	 * @param memo
	 * @param fee
	 * 
	 * @return true if all conditions are met and the user has not cancelled the operation
	 */
	private boolean warnAndCheckConditionsForSendingBackChange(String sourceAddress, String destinationAddress, String amount, String memo, String fee)
		throws WalletCallException, InterruptedException, IOException
	{
		String balance = this.clientCaller.getBalanceForAddress(sourceAddress);
		
		// Get a confirmation from the user about the operation - general warning
        String userDir = OSUtil.getSettingsDirectory();
        File sendChangeBackNotToBeShownFlagFile = new File(userDir + File.separator + "sendBackChangeWarningNotToBeShown.flag");
        if (!sendChangeBackNotToBeShownFlagFile.exists())
        {
        	Object[] options = 
        	{ 
        		langUtil.getString("send.cash.panel.option.pane.confirm.operation.button.yes"),
        		langUtil.getString("send.cash.panel.option.pane.confirm.operation.button.no"),
        		langUtil.getString("send.cash.panel.option.pane.confirm.operation.button.not.again")
        	};

    		int option = JOptionPane.showOptionDialog(
    				SendCashPanel.this.getRootPane().getParent(), 
    				langUtil.getString("send.cash.panel.send.change.back.general.warning"), 
    			    langUtil.getString("send.cash.panel.send.change.back.general.warning.title"),
    			    JOptionPane.DEFAULT_OPTION, 
    			    JOptionPane.WARNING_MESSAGE,
    			    null, 
    			    options, 
    			    options[0]);
    		
    	    if (option == 2)
    	    {
    	    	sendChangeBackNotToBeShownFlagFile.createNewFile();
    	    }
    	    
    	    if (option == 1)
    	    {
    	    	return false;
    	    }
        }
		
		// Make sure the confirmed balance for the address is sufficient
		if (new BigDecimal(balance).subtract(new BigDecimal(amount)).subtract(new BigDecimal(fee)).compareTo(new BigDecimal("0")) < 0)
		{
			JOptionPane.showMessageDialog(
				SendCashPanel.this.getRootPane().getParent(), 
				langUtil.getString("send.cash.panel.insufficient.balance", sourceAddress, balance, amount, fee),
				langUtil.getString("send.cash.panel.insufficient.balance.title"), 
				JOptionPane.ERROR_MESSAGE);
			
			return false;
		}
	
		return true;
	}
}
