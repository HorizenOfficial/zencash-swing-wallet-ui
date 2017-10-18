/************************************************************************************************
 *   ____________ _   _  _____          _      _____ _    _ _______          __   _ _      _   
 *  |___  /  ____| \ | |/ ____|        | |    / ____| |  | |_   _\ \        / /  | | |    | |  
 *     / /| |__  |  \| | |     __ _ ___| |__ | |  __| |  | | | |  \ \  /\  / /_ _| | | ___| |_ 
 *    / / |  __| | . ` | |    / _` / __| '_ \| | |_ | |  | | | |   \ \/  \/ / _` | | |/ _ \ __|
 *   / /__| |____| |\  | |___| (_| \__ \ | | | |__| | |__| |_| |_   \  /\  / (_| | | |  __/ |_ 
 *  /_____|______|_| \_|\_____\__,_|___/_| |_|\_____|\____/|_____|   \/  \/ \__,_|_|_|\___|\__|
 *                                                                                             
 * Copyright (c) 2017 Ivan Vaklinov <ivan@vaklinov.com>
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
package com.vaklinov.zcashui.msg;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.StatusUpdateErrorReporter;
import com.vaklinov.zcashui.Util;
import com.vaklinov.zcashui.ZCashClientCaller;
import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;


/**
 * Dialog showing the messaging options and allowing them to be edited.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class CreateGroupDialog
	extends JDialog
{
	protected MessagingPanel msgPanel;
	protected JFrame parentFrame;
	protected MessagingStorage storage;
	protected StatusUpdateErrorReporter errorReporter;
	protected ZCashClientCaller caller;
	
	protected boolean isOKPressed = false;
	protected String  key    = null;
	
	protected JLabel     keyLabel = null;
	protected JTextField keyField = null;
	
	protected JLabel upperLabel;
	protected JLabel lowerLabel;
	
	protected JProgressBar progress = null;
	
	JButton okButon;
	JButton cancelButon;
	
	protected MessagingIdentity createdGroup = null;
	
	public CreateGroupDialog(MessagingPanel msgPanel, JFrame parentFrame, MessagingStorage storage, StatusUpdateErrorReporter errorReporter, ZCashClientCaller caller)
		throws IOException
	{
		super(parentFrame);
		
		this.msgPanel      = msgPanel;
		this.parentFrame   = parentFrame;
		this.storage       = storage;
		this.errorReporter = errorReporter;
		this.caller = caller;
		
		this.setTitle("Add messaging group...");
		this.setModal(true);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
		controlsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel tempPanel = new JPanel(new BorderLayout(0, 0));
		tempPanel.add(this.upperLabel = new JLabel(
			"<html>Please enter a key phrase that identifies a messaging group. " + 
		    "Such a key phrase is usually a #HashTag<br/>or similar item known to the " +
			" group of people participating:</html>"), BorderLayout.CENTER);
		controlsPanel.add(tempPanel);
		
		JLabel dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 8));
		controlsPanel.add(dividerLabel);
		
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(keyField = new JTextField(60));
		controlsPanel.add(tempPanel);
		
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 8));
		controlsPanel.add(dividerLabel);

		tempPanel = new JPanel(new BorderLayout(0, 0));
		tempPanel.add(this.lowerLabel = new JLabel(
			"<html>The group key phrase will be converted into a group Z address that " +
		    "all participants share to receive <br/>messages. The addition of a messaging " + 
			"group may take considerable time, so please be patient...</html>"), 
			BorderLayout.CENTER);
		controlsPanel.add(tempPanel);
		
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 8));
		controlsPanel.add(dividerLabel);
		
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(progress = new JProgressBar());
		controlsPanel.add(tempPanel);
		
		this.getContentPane().setLayout(new BorderLayout(0, 0));
		this.getContentPane().add(controlsPanel, BorderLayout.NORTH);

		// Form buttons
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
		okButon = new JButton("Create group");
		buttonPanel.add(okButon);
		buttonPanel.add(new JLabel("   "));
		cancelButon = new JButton("Cancel");
		buttonPanel.add(cancelButon);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		okButon.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CreateGroupDialog.this.processOK();
			}
		});
		
		cancelButon.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				CreateGroupDialog.this.setVisible(false);
				CreateGroupDialog.this.dispose();
				
				CreateGroupDialog.this.isOKPressed = false;
				CreateGroupDialog.this.key = null;
			}
		});
		
		
		this.pack();
		this.setLocation(100, 100);
		this.setLocationRelativeTo(parentFrame);
	}

	
	protected void processOK()
	{
		final String keyPhrase = CreateGroupDialog.this.keyField.getText();
		
		if ((keyPhrase == null) || (keyPhrase.trim().length() <= 0))
		{
			JOptionPane.showMessageDialog(
				CreateGroupDialog.this.getParent(), 
				"The group key phrase is empty. Please enter it into the text field.", "Empty...", 
				JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		CreateGroupDialog.this.isOKPressed = true;
		CreateGroupDialog.this.key = keyPhrase;
				
		// Start import
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.progress.setIndeterminate(true);
		this.progress.setValue(1);
			
		this.okButon.setEnabled(false);
		this.cancelButon.setEnabled(false);
		
		CreateGroupDialog.this.keyField.setEditable(false);
			
		new Thread(new Runnable() 
		{
			@Override
			public void run() 
			{
				try
				{
					createGroupForKeyPhrase(keyPhrase);	
				} catch (Exception e)
				{
					Log.error("An error occurred when importing private key for group phrase", e);
					
					JOptionPane.showMessageDialog(
							CreateGroupDialog.this.getRootPane().getParent(), 
						"An error occurred when importing private key for group phrase. Error message is:\n" +
						e.getClass().getName() + ":\n" + e.getMessage() + "\n\n" +
						"Please ensure that zend is running and the key is in the correct \n" + 
						"form. You may try again later...\n", 
						"Error in importing private key/group phrase", JOptionPane.ERROR_MESSAGE);
				} finally
				{
					CreateGroupDialog.this.setVisible(false);
					CreateGroupDialog.this.dispose();
				}
			}
		}).start();
	}
	
	
	public boolean isOKPressed()
	{
		return this.isOKPressed;
	}
	
	
	public String getKey()
	{
		return this.key;
	}
	
	
	public MessagingIdentity getCreatedGroup()
	{
		return this.createdGroup;
	}
	
	
	private void createGroupForKeyPhrase(String keyPhrase)
		throws IOException, InterruptedException, WalletCallException
	{
		String key = Util.convertGroupPhraseToZPrivateKey(keyPhrase);
		
		// There is no way (it seems) to find out what Z address was added - we need to
		// analyze which one it is.
		// TODO: This relies that noone is importing keys at the same time!
		Set<String> addressesBeforeAddition = new HashSet<String>();
		for (String address: this.caller.getWalletZAddresses())
		{
			addressesBeforeAddition.add(address);
		}
		
		CreateGroupDialog.this.caller.importPrivateKey(key);
		
		Set<String> addressesAfterAddition = new HashSet<String>();
		for (String address: this.caller.getWalletZAddresses())
		{
			addressesAfterAddition.add(address);
		}

		addressesAfterAddition.removeAll(addressesBeforeAddition);
		
		String ZAddress = (addressesAfterAddition.size() > 0) ?
			addressesAfterAddition.iterator().next() :
			this.findZAddressForImportKey(key);
		MessagingIdentity existingIdentity = this.findExistingGroupBySendReceiveAddress(ZAddress);
		
		if (existingIdentity == null)
		{
			Log.info("Newly added messaging group \"{0}\" address is: {1}", keyPhrase, ZAddress);
			// Add a group personality etc.
			MessagingIdentity newID = new MessagingIdentity();
			newID.setGroup(true);
			newID.setNickname(keyPhrase);
			newID.setSendreceiveaddress(ZAddress);
			newID.setSenderidaddress("");
			newID.setFirstname("");
			newID.setMiddlename("");
			newID.setSurname("");
			newID.setEmail("");
			newID.setStreetaddress("");
			newID.setFacebook("");
			newID.setTwitter("");
			
			this.storage.addContactIdentity(newID);
			
			CreateGroupDialog.this.createdGroup = newID;
			
			JOptionPane.showMessageDialog(
				CreateGroupDialog.this,  
				"The messaging group with key phrase:\n" +
				keyPhrase + "\n" +
				"has been added successfully. All messages sent by individual users to the " +
				"group will be sent to Z address:\n"
				+ ZAddress + "\n\n" +
				"IMPORTANT: Do NOT send any ZEN to this address except in cases of messaging transactions. Any\n" +
				"funds sent to this address may be spent by any user who has access to the group key phrase!",
				"Group added successfully...",
				JOptionPane.INFORMATION_MESSAGE);
		} else
		{
			CreateGroupDialog.this.createdGroup = existingIdentity;
			// TODO: Group was already added it seems - see if it can be made more reliable
			JOptionPane.showMessageDialog(
				CreateGroupDialog.this,  
				"The messaging group with key phrase:\n" +
				keyPhrase + "\n" +
				"already exists",
				"Group already exists...",
				JOptionPane.INFORMATION_MESSAGE);
		}	
		
		SwingUtilities.invokeLater(new Runnable() 
		{	
			@Override
			public void run() 
			{
				try
				{
					CreateGroupDialog.this.msgPanel.getContactList().reloadMessagingIdentities();
				} catch (Exception e)
				{
					Log.error("Unexpected error in reloading contacts after gathering messages: ", e);
					CreateGroupDialog.this.errorReporter.reportError(e);
				}
			}
		});
	}
	
	
	/**
	 * Finds a group identity for a send/receive address.
	 *  
	 * @param address
	 * 
	 * @return identity for the address or null
	 */
	private MessagingIdentity findExistingGroupBySendReceiveAddress(String address)
		 throws IOException
	{
		MessagingIdentity identity = null;
		
		for (MessagingIdentity id : this.storage.getContactIdentities(false))
		{
			if (id.isGroup())
			{
				if (id.getSendreceiveaddress().equals(address))
				{
					identity = id;
					break;
				}
			}
		}
		
		return identity;
	}
	
	
	/**
	 * Checks the wallet's private keys to find what address corresponds to a key.
	 * 
	 * @param key to search for
	 * 
	 * @return address for the key or null;
	 */
	private String findZAddressForImportKey(String key)
		throws InterruptedException, WalletCallException, IOException
	{
		String address = null;
		
		for (String zAddr : this.caller.getWalletZAddresses())
		{
			String privKey = this.caller.getZPrivateKey(zAddr);
			if (privKey.equals(key))
			{
				address = zAddr;
				break;
			}
		}
		
		return address;
	}
} 
