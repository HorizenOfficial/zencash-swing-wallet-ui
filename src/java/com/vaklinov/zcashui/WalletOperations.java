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

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;
import com.vaklinov.zcashui.arizen.models.Address;
import com.vaklinov.zcashui.arizen.repo.ArizenWallet;
import com.vaklinov.zcashui.arizen.repo.WalletRepo;


/**
 * Provides miscellaneous operations for the wallet file.
 * 
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class WalletOperations
{	
	private ZCashUI parent;
	private JTabbedPane tabs;
	private DashboardPanel dashboard;
	private SendCashPanel  sendCash;
	private AddressesPanel addresses;
	
	private ZCashInstallationObserver installationObserver;
	private ZCashClientCaller         clientCaller;
	private StatusUpdateErrorReporter errorReporter;


	public WalletOperations(ZCashUI parent,
			                JTabbedPane tabs,
			                DashboardPanel dashboard,
			                AddressesPanel addresses,
			                SendCashPanel  sendCash,
			                
			                ZCashInstallationObserver installationObserver, 
			                ZCashClientCaller clientCaller,
			                StatusUpdateErrorReporter errorReporter) 
        throws IOException, InterruptedException, WalletCallException 
	{
		this.parent    = parent;
		this.tabs      = tabs;
		this.dashboard = dashboard;
		this.addresses = addresses;
		this.sendCash  = sendCash;
		
		this.installationObserver = installationObserver;
		this.clientCaller = clientCaller;
		this.errorReporter = errorReporter;
	}

	
	public void encryptWallet()
	{
		try
		{			
			if (this.clientCaller.isWalletEncrypted())
			{
		        JOptionPane.showMessageDialog(
		            this.parent,
		            "The wallet.dat file being used is already encrypted. " +
		            "This \noperation may be performed only on a wallet that " + 
		            "is not\nyet encrypted!",
		            "Wallet is already encrypted...",
		            JOptionPane.ERROR_MESSAGE);
		        return;
			}
			
			PasswordEncryptionDialog pd = new PasswordEncryptionDialog(this.parent);
			pd.setVisible(true);
			
			if (!pd.isOKPressed())
			{
				return;
			}
			
			Cursor oldCursor = this.parent.getCursor();
			try
			{
				
				this.parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				this.dashboard.stopThreadsAndTimers();
				this.sendCash.stopThreadsAndTimers();
				
				this.clientCaller.encryptWallet(pd.getPassword());
				
				this.parent.setCursor(oldCursor);
			} catch (WalletCallException wce)
			{
				this.parent.setCursor(oldCursor);
				Log.error("Unexpected error: ", wce);
				
				JOptionPane.showMessageDialog(
					this.parent, 
					"An unexpected error occurred while encrypting the wallet!\n" +
					"It is recommended to stop and restart both zend and the GUI wallet! \n" +
					"\n" + wce.getMessage().replace(",", ",\n"),
					"Error in encrypting wallet...", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			JOptionPane.showMessageDialog(
				this.parent, 
				"The wallet has been encrypted sucessfully and zend has stopped.\n" +
				"The GUI wallet will be stopped as well. Please restart both. In\n" +
				"addtion the internal wallet keypool has been flushed. You need\n" +
				"to make a new backup..." +
				"\n",
				"Wallet is now encrypted...", JOptionPane.INFORMATION_MESSAGE);
			
			this.parent.exitProgram();
			
		} catch (Exception e)
		{
			this.errorReporter.reportError(e, false);
		}
	}
	
	
	public void backupWallet()
	{
		try
		{
			this.issueBackupDirectoryWarning();
			
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Backup wallet to file...");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setCurrentDirectory(OSUtil.getUserHomeDirectory());
			 
			int result = fileChooser.showSaveDialog(this.parent);
			 
			if (result != JFileChooser.APPROVE_OPTION) 
			{
			    return;
			}
			
			File f = fileChooser.getSelectedFile();
			
			Cursor oldCursor = this.parent.getCursor();
			String path = null;
			try
			{
				this.parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							
				path = this.clientCaller.backupWallet(f.getName());
				
				this.parent.setCursor(oldCursor);
			} catch (WalletCallException wce)
			{
				this.parent.setCursor(oldCursor);
				Log.error("Unexpected error: ", wce);
				
				JOptionPane.showMessageDialog(
					this.parent, 
					"An unexpected error occurred while backing up the wallet!" +
					"\n" + wce.getMessage().replace(",", ",\n"),
					"Error in backing up wallet...", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			JOptionPane.showMessageDialog(
				this.parent, 
				"The wallet has been backed up successfully to file: " + f.getName() + "\n" +
				"in the backup directory provided to zend (-exportdir=<dir>).\nFull path is: " + 
				path,
				"Wallet is backed up...", JOptionPane.INFORMATION_MESSAGE);
			
		} catch (Exception e)
		{
			this.errorReporter.reportError(e, false);
		}
	}
	
	
	public void exportWalletPrivateKeys()
	{
		// TODO: Will need corrections once encryption is reenabled!!!
		
		try
		{
			this.issueBackupDirectoryWarning();
			
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Export wallet private keys to file...");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setCurrentDirectory(OSUtil.getUserHomeDirectory());
			 
			int result = fileChooser.showSaveDialog(this.parent);
			 
			if (result != JFileChooser.APPROVE_OPTION) 
			{
			    return;
			}
			
			File f = fileChooser.getSelectedFile();
			
			Cursor oldCursor = this.parent.getCursor();
			String path = null;
			try
			{
				this.parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							
				path = this.clientCaller.exportWallet(f.getName());
				
				this.parent.setCursor(oldCursor);
			} catch (WalletCallException wce)
			{
				this.parent.setCursor(oldCursor);
				Log.error("Unexpected error: ", wce);
				
				JOptionPane.showMessageDialog(
					this.parent, 
					"An unexpected error occurred while exporting wallet private keys!" +
					"\n" + wce.getMessage().replace(",", ",\n"),
					"Error in exporting wallet private keys...", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			JOptionPane.showMessageDialog(
				this.parent, 
				"The wallet private keys have been exported successfully to file:\n" + 
				f.getName() + "\n" +
				"in the backup directory provided to zend (-exportdir=<dir>).\nFull path is: " + 
				path + "\n" +
				"You need to protect this file from unauthorized access. Anyone who\n" +
				"has access to the private keys can spend the ZENCash balance!",
				"Wallet private key export...", JOptionPane.INFORMATION_MESSAGE);
			
		} catch (Exception e)
		{
			this.errorReporter.reportError(e, false);
		}
	}

	
	public void importWalletPrivateKeys()
	{
		// TODO: Will need corrections once encryption is re-enabled!!!
		
	    int option = JOptionPane.showConfirmDialog(  
		    this.parent,
		    "Private key import is a potentially slow operation. It may take\n" +
		    "several minutes during which the GUI will be non-responsive.\n" +
		    "The data to import must be in the format used by the option:\n" +
		    "\"Export private keys...\"\n\n" +
		    "Are you sure you wish to import private keys?",
		    "Private key import notice...",
		    JOptionPane.YES_NO_OPTION);
		if (option == JOptionPane.NO_OPTION)
		{
		  	return;
		}
		
		try
		{
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Import wallet private keys from file...");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			 
			int result = fileChooser.showOpenDialog(this.parent);
			 
			if (result != JFileChooser.APPROVE_OPTION) 
			{
			    return;
			}
			
			File f = fileChooser.getSelectedFile();
			
			Cursor oldCursor = this.parent.getCursor();
			try
			{
				this.parent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							
				this.clientCaller.importWallet(f.getCanonicalPath());
				
				this.parent.setCursor(oldCursor);
			} catch (WalletCallException wce)
			{
				this.parent.setCursor(oldCursor);
				Log.error("Unexpected error: ", wce);
				
				JOptionPane.showMessageDialog(
					this.parent, 
					"An unexpected error occurred while importing wallet private keys!" +
					"\n" + wce.getMessage().replace(",", ",\n"),
					"Error in importing wallet private keys...", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			JOptionPane.showMessageDialog(
				this.parent, 
				"Wallet private keys have been imported successfully from location:\n" +
				f.getCanonicalPath() + "\n\n",
				"Wallet private key import...", JOptionPane.INFORMATION_MESSAGE);
			
		} catch (Exception e)
		{
			this.errorReporter.reportError(e, false);
		}
	}
	
	
	public void showPrivateKey()
	{
		if (this.tabs.getSelectedIndex() != 1)
		{
			JOptionPane.showMessageDialog(
				this.parent, 
				"Please select an address in the \"Own addresses\" tab " +
				"to view its private key",
				"Please select an address...", JOptionPane.INFORMATION_MESSAGE);
			this.tabs.setSelectedIndex(1);
			return;
		}
		
		String address = this.addresses.getSelectedAddress();
		
		if (address == null)
		{
			JOptionPane.showMessageDialog(
				this.parent, 
				"Please select an address in the table of addresses " +
				"to view its private key",
				"Please select an address...", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		try
		{
			// Check for encrypted wallet
			final boolean bEncryptedWallet = this.clientCaller.isWalletEncrypted();
			if (bEncryptedWallet)
			{
				PasswordDialog pd = new PasswordDialog((JFrame)(this.parent));
				pd.setVisible(true);
				
				if (!pd.isOKPressed())
				{
					return;
				}
				
				this.clientCaller.unlockWallet(pd.getPassword());
			}
			
			boolean isZAddress = Util.isZAddress(address);
			
			String privateKey = isZAddress ?
				this.clientCaller.getZPrivateKey(address) : this.clientCaller.getTPrivateKey(address);
				
			// Lock the wallet again 
			if (bEncryptedWallet)
			{
				this.clientCaller.lockWallet();
			}
				
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection(privateKey), null);
			
			JOptionPane.showMessageDialog(
				this.parent, 
				(isZAddress ? "Z (Private)" : "T (Transparent)") +  " address:\n" +
				address + "\n" + 
				"has private key:\n" +
				privateKey + "\n\n" +
				"The private key has also been copied to the clipboard.", 
				"Private key information", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception ex)
		{
			this.errorReporter.reportError(ex, false);
		}
	}
	
	
	public void importSinglePrivateKey()
	{
		try
		{
			SingleKeyImportDialog kd = new SingleKeyImportDialog(this.parent, this.clientCaller);
			kd.setVisible(true);
			
		} catch (Exception ex)
		{
			this.errorReporter.reportError(ex, false);
		}
	}



	/**
	 * export to Arizen wallet
	 */
	public void exportToArizenWallet()
	{
		final JDialog dialog = new JDialog(this.parent, "Exporting Arizen wallet");
		final JLabel exportLabel = new JLabel();
		final WalletRepo arizenWallet = new ArizenWallet();
		try {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileNameExtensionFilter("Arizen wallet file", "uawd"));
			fileChooser.setDialogTitle("Export wallet to Arizen wallet unencrypted format...");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setCurrentDirectory(OSUtil.getUserHomeDirectory());
			int result = fileChooser.showDialog(this.parent, "Export");

			if (result != JFileChooser.APPROVE_OPTION) {
				return;
			}

			File chooseFile = fileChooser.getSelectedFile();
			String fullPath = chooseFile.getAbsolutePath();
			if (!fullPath.endsWith(".uawd"))
				fullPath += ".uawd";

			final File f = new File(fullPath);
			if (f.exists()) {
				int r = JOptionPane.showConfirmDialog((Component) null,
						String.format("The file %s already exists, do you want proceed and delete it?", f.getName()),
						"Alert", JOptionPane.YES_NO_OPTION);
				if (r == 1) {
					return;
				}
				Files.delete(f.toPath());
			}
			final String strFullpath = fullPath;

			dialog.setSize(300, 75);
			dialog.setLocationRelativeTo(parent);
			dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			dialog.setLayout(new BorderLayout());

			JProgressBar progressBar = new JProgressBar();
			progressBar.setIndeterminate(true);
			dialog.add(progressBar, BorderLayout.CENTER);
			exportLabel.setText("Exporting wallet...");
			exportLabel.setHorizontalAlignment(JLabel.CENTER);
			exportLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

			dialog.add(exportLabel, BorderLayout.SOUTH);
			dialog.setVisible(true);

			SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
				@Override
				public Boolean doInBackground() {
					try {
						arizenWallet.createWallet(f);
						Thread.sleep(750);
						updateProgressText("Reading addresses and private keys...");
						String[] zaddress = clientCaller.getWalletZAddresses();
						String[] taddress = clientCaller.getWalletAllPublicAddresses();
						String[] tAddressesWithUnspentOuts = clientCaller.getWalletPublicAddressesWithUnspentOutputs();

						Set<Address> addressPublicSet = new HashSet<Address>();
						Set<Address> addressPrivateSet = new HashSet<Address>();

						Map<String, Address> tMap = new HashMap<String, Address>();
						Map<String, Address> zMap = new HashMap<String, Address>();

						for (String straddr : taddress) {
							String pk = clientCaller.getTPrivateKey(straddr);
							String pkHex = Util.wifToHex(pk);
							String balance = clientCaller.getBalanceForAddress(straddr);
							Address addr = new Address(Address.ADDRESS_TYPE.TRANSPARENT, straddr, pkHex, balance);
							tMap.put(straddr, addr);
						}

						for (String straddr : tAddressesWithUnspentOuts) {
							String pk = clientCaller.getTPrivateKey(straddr);
							String pkHex = Util.wifToHex(pk);
							String balance = clientCaller.getBalanceForAddress(straddr);
							Address addr = new Address(Address.ADDRESS_TYPE.TRANSPARENT, straddr, pkHex, balance);
							tMap.put(straddr, addr);
						}

						for (String straddr : zaddress) {
							String pk = clientCaller.getZPrivateKey(straddr);
							String balance = clientCaller.getBalanceForAddress(straddr);
							String pkHex = Util.wifToHex(pk);
							Address addr = new Address(Address.ADDRESS_TYPE.PRIVATE, straddr, pkHex, balance);
							zMap.put(straddr, addr);
						}
						addressPublicSet.addAll(tMap.values());
						addressPrivateSet.addAll(zMap.values());
						Thread.sleep(500);

						updateProgressText("Writing addresses and private keys...");
						arizenWallet.insertAddressBatch(addressPublicSet);
						arizenWallet.insertAddressBatch(addressPrivateSet);
						Thread.sleep(1000);

						updateProgressText("Wallet exported");
						Thread.sleep(750);

						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								dialog.dispose();
								JOptionPane.showConfirmDialog(parent,
										new Object[]{String.format("The Arizen wallet is exported to: %s", strFullpath),
												"Using Arizen to import select: Import UNENCRYPTED Arizen wallet",
												"The wallet will be imported and encrypted"},
										"Export Arizen wallet", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE);
							}
						});

					} catch (Exception e) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								dialog.dispose();
							}
						});
						errorReporter.reportError(e, false);
					} finally {
						try {
							if (arizenWallet != null && arizenWallet.isOpen()) {
								arizenWallet.close();
							}
						} catch (Exception ex) {
							errorReporter.reportError(ex, false);
						}
					}
					return true;
				}

				private void updateProgressText(String text) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							exportLabel.setText(text);
						}
					});

				}
			};

			worker.execute();

		} catch (Exception ex) {
			errorReporter.reportError(ex, false);
		}
	}



	private void issueBackupDirectoryWarning()
		throws IOException
	{
        String userDir = OSUtil.getSettingsDirectory();
        File warningFlagFile = new File(userDir + File.separator + "backupInfoShownNG.flag");
        if (warningFlagFile.exists())
        {
            return;
        } 
            
        int reply = JOptionPane.showOptionDialog(
            this.parent,
            "For security reasons the wallet may be backed up/private keys exported only if\n" +
            "the zend parameter -exportdir=<dir> has been set. If you started zend \n" +
            "manually, you ought to have provided this parameter. When zend is started \n" +
            "automatically by the GUI wallet the directory provided as parameter to -exportdir\n" +
            "is the user home directory: " + OSUtil.getUserHomeDirectory().getCanonicalPath() +"\n" +
            "Please navigate to the directory provided as -exportdir=<dir> and select a\n"+ 
            "filename in it to backup/export private keys. If you select another directory\n" +
            "instead, the destination file will still end up in the directory provided as \n" +
            "-exportdir=<dir>. If this parameter was not provided to zend, the process\n" +
            "will fail with a security check error. The filename needs to consist of only\n" + 
            "alphanumeric characters (e.g. dot is not allowed).\n",
            "Wallet backup directory information", 
	        JOptionPane.YES_NO_OPTION,
	        JOptionPane.INFORMATION_MESSAGE, 
	        null, new String[] { "Do not show this again", "OK" }, 
	        JOptionPane.NO_OPTION);
	        
	    if (reply == JOptionPane.NO_OPTION) 
	    {
	    	return;
	    }
	    
	    warningFlagFile.createNewFile();
	}
}
