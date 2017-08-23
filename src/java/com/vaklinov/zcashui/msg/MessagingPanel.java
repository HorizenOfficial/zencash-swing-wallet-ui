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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import com.vaklinov.zcashui.DataGatheringThread;
import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.OSUtil;
import com.vaklinov.zcashui.SendCashPanel;
import com.vaklinov.zcashui.StatusUpdateErrorReporter;
import com.vaklinov.zcashui.Util;
import com.vaklinov.zcashui.WalletTabPanel;
import com.vaklinov.zcashui.ZCashClientCaller;
import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;
import com.vaklinov.zcashui.msg.Message.DIRECTION_TYPE;
import com.vaklinov.zcashui.msg.Message.VERIFICATION_TYPE;


/**
 * Main panel for messaging
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class MessagingPanel
	extends WalletTabPanel
{
	private JFrame parentFrame;
	private SendCashPanel sendCashPanel; 
	private JTabbedPane parentTabs;
	
	private ZCashClientCaller clientCaller;
	private StatusUpdateErrorReporter errorReporter;
	
	private MessagingStorage messagingStorage;
	
	private JContactListPanel contactList;
	
	private JLabel conversationLabel;
	private JTextPane conversationTextPane;
	
	private JTextArea writeMessageTextArea;
	private JButton sendButton;
	private JLabel sendResultLabel;
	private JProgressBar sendMessageProgressBar;
	
	private Timer operationStatusTimer;
	
	private DataGatheringThread<Object> receivedMesagesGatheringThread = null;
	
	
	public MessagingPanel(JFrame parentFrame, SendCashPanel sendCashPanel, JTabbedPane parentTabs, 
			              ZCashClientCaller clientCaller, StatusUpdateErrorReporter errorReporter)
		throws IOException, InterruptedException, WalletCallException
	{
		super();
		
		this.parentFrame      = parentFrame;
		this.sendCashPanel    = sendCashPanel;
		this.parentTabs       = parentTabs;
		
		this.clientCaller     = clientCaller;
		this.errorReporter    = errorReporter;
		this.messagingStorage = new MessagingStorage();
		
		
		// Start building UI
		this.setLayout(new BorderLayout(0, 0));
		this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		final JSplitPane textAndContactsPane = new JSplitPane();
		this.add(textAndContactsPane, BorderLayout.CENTER);
		
		this.contactList = new JContactListPanel(this, this.messagingStorage, this.errorReporter);
		textAndContactsPane.setRightComponent(this.contactList);
		
		JPanel conversationPanel = new JPanel(new BorderLayout(0, 0));
		conversationPanel.add(
			new JScrollPane(
				this.conversationTextPane = new JTextPane(),
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), 
			BorderLayout.CENTER);
		this.conversationTextPane.setEditable(false);
		this.conversationTextPane.setContentType("text/html");
		JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		upperPanel.add(this.conversationLabel = new JLabel(
			"<html><span style=\"font-size:1.2em;font-style:italic;\">Conversation ...</span>"));
		upperPanel.add(new JLabel(
    			"<html><span style=\"font-size:1.6em;font-style:italic;\">&nbsp;</span>"));
		upperPanel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		conversationPanel.add(upperPanel, BorderLayout.NORTH);		
		
		textAndContactsPane.setLeftComponent(conversationPanel);
		SwingUtilities.invokeLater(new Runnable() { 
			@Override
			public void run() {
				textAndContactsPane.setDividerLocation(590);			
				}
		});
		
		
		JPanel writeAndSendPanel = new JPanel(new BorderLayout(0, 0));
		this.add(writeAndSendPanel, BorderLayout.SOUTH);
		
		JPanel writePanel = new JPanel(new BorderLayout(0, 0));
		this.writeMessageTextArea = new JTextArea(3, 50);
		this.writeMessageTextArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		this.writeMessageTextArea.setLineWrap(true);
		writePanel.add(
			new JScrollPane(this.writeMessageTextArea,
					        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), 
			BorderLayout.CENTER);
		JLabel sendLabel = new JLabel("Message to send:");
		MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
		if (ownIdentity != null)
		{
			sendLabel.setText("Message to send as: " + ownIdentity.getDiplayString());
		}
		sendLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		writePanel.add(sendLabel, BorderLayout.NORTH);
		writePanel.add(new JLabel(""), BorderLayout.EAST); // dummy
		writeAndSendPanel.add(writePanel, BorderLayout.CENTER);
		
		JPanel sendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		JPanel sendButtonPanel = new JPanel();
		sendButtonPanel.setLayout(new BoxLayout(sendButtonPanel, BoxLayout.Y_AXIS));
		JLabel filler = new JLabel(" ");
		filler.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		sendButtonPanel.add(filler); // filler
		sendButton = new JButton("Send message  \u27A4\u27A4\u27A4");
		JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendButton);
		sendButtonPanel.add(tempPanel);
		sendMessageProgressBar = new JProgressBar(0, 200);
		sendMessageProgressBar.setPreferredSize(
			new Dimension(sendButton.getPreferredSize().width, 
					      sendMessageProgressBar.getPreferredSize().height * 2 / 3));
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendMessageProgressBar);
		sendButtonPanel.add(tempPanel);
		sendResultLabel = new JLabel(
				"<html><span style=\"font-size:0.8em;\">" +
				"Send status: &nbsp;</span>");
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendResultLabel);
		sendButtonPanel.add(tempPanel);
		
		sendPanel.add(sendButtonPanel);
		writeAndSendPanel.add(sendPanel, BorderLayout.EAST);
		
		// Attach logic
		sendButton.addActionListener(new ActionListener() 
		{	
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				MessagingPanel.this.sendMessageAndHandleErrors();
			}
		});
		
		// Start the thread to periodically gather messages
		this.receivedMesagesGatheringThread = new DataGatheringThread<Object>(
			new DataGatheringThread.DataGatherer<Object>() 
			{
				public String[][] gatherData()
					throws Exception
				{
					long start = System.currentTimeMillis();
					
					MessagingPanel.this.collectAndStoreNewReceivedMessagesAndHandleErrors();
					
					long end = System.currentTimeMillis();
					Log.info("Gathering of received messages done in " + (end - start) + "ms." );
						
					return null;
				}
			}, 
			this.errorReporter, 120 * 1000, true);
		this.threads.add(receivedMesagesGatheringThread);
	}
	
	
	/**
	 * Loads all messages for a specific contact and displays them in the conversation text area.
	 * 
	 * @param conact
	 */
	public void displayMessagesForContact(MessagingIdentity contact)
		throws IOException
	{
		MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
		List<Message> messages = this.messagingStorage.getAllMessagesForContact(contact);
		
		Date now = new Date();
		StringBuilder text = new StringBuilder();

		final SimpleDateFormat defaultFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		final SimpleDateFormat shortFormat = new SimpleDateFormat("HH:mm:ss");
		
		for (Message msg : messages)
		{
			String color = msg.getDirection() ==  DIRECTION_TYPE.SENT ? "blue" : "red";

			String stamp = defaultFormat.format(msg.getTime()); // TODO: correct date further
			if (Math.abs(now.getTime() - msg.getTime().getTime()) < (24L * 3600 * 1000)) // 24 h
			{
				if (now.getDay() == msg.getTime().getDay())
				{
					stamp = shortFormat.format(msg.getTime());
				}
			}
			
			String preparedMessage = null;
			
			if (this.isZENIdentityMessage(msg.getMessage()))
			{
				MessagingIdentity msgID = new MessagingIdentity(
					Util.parseJsonObject(msg.getMessage()).get("zenmessagingidentity").asObject());
				
				preparedMessage = "<span style=\"color:green;\">" +
					"Special messaging identity message. Contains details of contact: " +
					msgID.getDiplayString() +
					"</span>";
			} else
			{
				// Replace line end characters, for multi-line messages
				preparedMessage = Util.escapeHTMLValue(msg.getMessage());
				preparedMessage = preparedMessage.replace("\n", "<br/>");
			};
			
			text.append("<span style=\"color:" + color +";\">");
			text.append("<span style=\"font-weight:bold;font-size:1.5em;\">");
			text.append(msg.getDirection() == DIRECTION_TYPE.SENT ? "\u21E8 " : "\u21E6 ");
			text.append("</span>");
			text.append("(");
			text.append(stamp); 
			text.append(") ");
			if ((msg.getDirection() == DIRECTION_TYPE.RECEIVED) && 
				(msg.getVerification() == VERIFICATION_TYPE.UNVERIFIED))
			{
				text.append("<span style=\"font-weight:bold;\">");
				text.append("[WARNING: Message signature is unverified.] ");
				text.append("</span>");

			} else if ((msg.getDirection() == DIRECTION_TYPE.RECEIVED) && 
					   (msg.getVerification() == VERIFICATION_TYPE.VERIFICATION_FAILED))
			{
				text.append("<span style=\"font-weight:bold;font-size:1.25em;\">");
				text.append("[ERROR: Message signature is invalid! Message may be forged!] ");
				text.append("</span>");
			}
			text.append("<span style=\"font-weight:bold;\">");
			text.append(msg.getDirection() == DIRECTION_TYPE.SENT ? 
					    Util.escapeHTMLValue(ownIdentity.getNickname()) : 
					    Util.escapeHTMLValue(contact.getNickname()));
			text.append("</span>");
			text.append(": ");
			text.append("</span>");
			text.append(preparedMessage);
			text.append("<br/>");
		}
		
		this.conversationTextPane.setText("<html>" + text.toString() + "</html>");
		this.conversationLabel.setText(
			"<html><span style=\"font-size:1.25em;font-style:italic;\">Conversation with: " + 
		    contact.getDiplayString() + "</span>");
	}
	

	/**
	 * Called when the TAB is selected - currently shows the welcome mesage
	 */
	public void tabSelected()
	{
		try
		{
			if (this.messagingStorage.getOwnIdentity() == null)
			{
		        JOptionPane.showMessageDialog(
	                this.parentFrame,
	                "Welcome to ZENCash messaging. As a start you will need to create a new messaging\n" + 
	                "identity for yourself. As a part of this mesaging identity a pair of T+Z addresses\n" +
	                "will be created. The T address is to be used for identifying you to other users.\n" +
	                "It must never be used for other financial transactions since this might reduce or\n" +
	                "fully compromise your privacy. The Z address is to be used to send and receive\n" +
	                "messages.\n\n" +
	                "When creating a new messaging identity it is only mandatory to specify a nick-name\n" +
	                "for yourself. All other items such as names/addresses etc. are optional. The \n" +
	                "information in the mesaging identity is meant to be shared with other users so \n" +
	                "you need to be careful about the details you disclose.\n\n" +
	                "Once your messaging identity has been created you can export it to a file using the\n" +
	                "menu option Messaging >> Export own identity. This file may then be shared with\n" +
	                "other users who wish to import it. To establish contact with other users you need to\n" +
	                "import their messaging identity, using the menu option Messaging >> Import contact \n" +
	                "identity.\n\n" +
	                "Your messaging history will be saved and maintained in directory:\n" +
	                OSUtil.getSettingsDirectory() + File.separator + "messaging" + "\n" +
	                "You need to ensure that no unauthorized users have access to it on this computer.\n\n" +
	                "(This mesage will be shown only once.)",
	                "Welcome to messaging", JOptionPane.INFORMATION_MESSAGE);
		        	        
		        // Show the GUI dialog to edit an initially empty messaging identity
		        this.openOwnIdentityDialog();
		        
		        // Offer the user to export his messaging identity
		        int reply = JOptionPane.showConfirmDialog(
		        	this.parentFrame, 
		        	"Your messaging identity has been created successfully. Would you\n" +
		        	"like to export it to a JSON file at this time? You need to export\n" +
		        	"it and give this file to other users in order to establish contact.", 
		        	"Export messaging identity?", 
		        	JOptionPane.YES_NO_OPTION);
		        
		        if (reply == JOptionPane.YES_OPTION) 
		        {
		        	this.exportOwnIdentity();
		        }
			} else
			{
				// Own identity exists, check balance of T address !!! - must be none
				MessagingIdentity ownIdentity =  this.messagingStorage.getOwnIdentity();
				Cursor oldCursor = this.getCursor();
				String balance = null;
				try
				{
     				this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		    	    balance = this.clientCaller.getBalanceForAddress(ownIdentity.getSenderidaddress());
				} finally
				{
					this.setCursor(oldCursor);
				}
				
		    	if (Double.valueOf(balance) > 0)
		    	{
			        JOptionPane.showMessageDialog(
					    this.parentFrame,
					    "The T address used to identify you in messaging must have NO ZEN balance: \n" +
					    ownIdentity.getSendreceiveaddress() + "\n" +
					    "However it currently has a non-zero balance! This might mean that you \n" +
					    "accidentally used this T address in non-messaging transactions. It might\n" +
					    "also mean that someone sent ZEN to it deliberately. To minimize the chance\n" +
					    "of compromising your privacy you must transfer all ZEN from this T address\n" +
					    "to some Z address ASAP!", 
						"Messaging identification address has balance!", 
						JOptionPane.WARNING_MESSAGE);
		    	}
			}
		} catch (Exception ex)
		{
			Log.error("Unexpected error in messagign TAB selection processing", ex);
			this.errorReporter.reportError(ex, false);
		}
	}
	
	
	
	/**
	 * Shows the UI dialog to edit+save one's own identity.
	 */
	public void openOwnIdentityDialog()
	{
		try
		{
			MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
			boolean identityIsBeingCreated = false;
			
			if (ownIdentity == null)
			{
				identityIsBeingCreated = true;
				ownIdentity = new MessagingIdentity();
				
				Cursor oldCursor = this.getCursor();
				try
				{
     				this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				
				     // Create the T/Z addresses to be used for messaging - sometimes the wallet returns old
     				 // T addresses, so we iterate
				     String TAddress = null;
				     for (int i = 0; i < 10; i++)
				     {
				    	 TAddress = this.clientCaller.createNewAddress(false);
				    	 String balance = this.clientCaller.getBalanceForAddress(TAddress);
				    	 if (Double.valueOf(balance) <= 0)
				    	 {
				    		 break;
				    	 }
				     }
				    
				     String ZAddress = this.clientCaller.createNewAddress(true);
				     
					// TODO: update address book (later on)
						
					ownIdentity.setSenderidaddress(TAddress);
					ownIdentity.setSendreceiveaddress(ZAddress);
				} finally
				{
					this.setCursor(oldCursor);
				}				
			}
			
			// Dialog will automatically save the identity if the user chooses so 
			OwnIdentityEditDialog ownIdentityDialog = new OwnIdentityEditDialog(
				this.parentFrame, ownIdentity, this.messagingStorage, this.errorReporter, identityIsBeingCreated);
			ownIdentityDialog.setVisible(true);
			
			if (identityIsBeingCreated)
			{
		        JOptionPane.showMessageDialog(
			        this.parentFrame,
			        "The Z address used to send/receive messages needs to be supplied with ZEN: \n" +
			        ownIdentity.getSendreceiveaddress() + "\n" +
			        "You will be redirected to the UI tab for sending ZEN to add some balance to it. You need only\n" +
			        "a small amount e.g. typically 0.1 ZEN is suffucient to send 500 messages. After sending some\n" +
			        "ZEN you need to wait for the transaciton to be confirmed (typically takes 2.5 minutes). It is\n" +
			        "recommended to send ZEN to this Z address in two or more separate transactions (though one \n" +
			        "transaction is sufficient).", 
				    "Z address to send/receive messages needs to be supplied with ZEN...", 
				    JOptionPane.INFORMATION_MESSAGE);
				        
					sendCashPanel.prepareForSending(ownIdentity.getSendreceiveaddress());
		            parentTabs.setSelectedIndex(2);				
			}
			
		} catch (Exception ex)
		{
			Log.error("Unexpected error in editing own messaging identity!", ex);
			this.errorReporter.reportError(ex, false);
		}
	}
	
	
	/**
	 * Exports a user's own identity to a file.
	 */
	public void exportOwnIdentity()
	{
		try
		{
			MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
			
			if (ownIdentity == null)
			{
		        JOptionPane.showMessageDialog(
	        		this.parentFrame,
	        		"Your messaging identity is missing! Maybe it has not been created yet.\n" +
	        		"Use the menu option \"Messaging >> Own identity\" to crate it!", 
	        		"Messaging identity is not available", JOptionPane.ERROR_MESSAGE);
		        return;
			}
			
			String nick = ownIdentity.getNickname();
			String filePrefix = "";
			
			for (char c : nick.toCharArray())
			{
				if (Character.isJavaIdentifierStart(c) || Character.isDigit(c))
				{
					filePrefix += c;
				}
			}
			
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Export messaging identity to JSON file ...");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setSelectedFile(
				new File(OSUtil.getUserHomeDirectory(), filePrefix + "_messaging_identity.json"));
			 
			int result = fileChooser.showSaveDialog(this.parentFrame);
			 
			if (result != JFileChooser.APPROVE_OPTION) 
			{
			    return;
			}
			
			File f = fileChooser.getSelectedFile();
			
			JsonObject identityObject = new JsonObject();
			identityObject.set("zenmessagingidentity", ownIdentity.toJSONObject());
			String identityString = identityObject.toString(WriterConfig.PRETTY_PRINT);
			
			FileOutputStream fos = null;
			try
			{
				fos = new FileOutputStream(f);
				OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
				osw.write(identityString);
				osw.flush();
			} finally
			{
				if (fos != null)
				{
					fos.close();
				}
			}
			
			JOptionPane.showMessageDialog(
				this.parentFrame, 
				"Your messaging identity has been succesfully exported to file: \n" + 
				f.getName() + "\n" +
				"You may give this file to other users to establish contact with them.\n" +
				"They may in turn import it into their wallet/messenger application.",
				"Messaging identity is successfully exported...", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception ex)
		{
			Log.error("Unexpected error in exporting own messaging identity to file!", ex);
			this.errorReporter.reportError(ex, false);
		}
	}
	
	
	/**
	 * Imports a contact's identity from file.
	 */
	public void importContactIdentity()
	{
		try
		{
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Import contact's messaging identity from file...");
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			 
			int result = fileChooser.showOpenDialog(this.parentFrame);
			 
			if (result != JFileChooser.APPROVE_OPTION) 
			{
			    return;
			}
			
			File f = fileChooser.getSelectedFile();

			JsonObject topIdentityObject = null;
			
			Reader r = null;
			try
			{
				r = new InputStreamReader(new FileInputStream(f), "UTF-8");
				topIdentityObject = Util.parseJsonObject(r);
			} finally
			{
				if (r != null)
				{
					r.close();
				}
			}
			
			// Validate the fields inside the objects, make sure this is indeed an identity
			// verify mandatory etc.
			JsonValue innerValue = topIdentityObject.get("zenmessagingidentity");
			JsonObject innerIdentity = (innerValue != null) ? innerValue.asObject() : null;
			
			if ((innerValue == null) || (innerIdentity == null) ||
				(innerIdentity.get("nickname") == null) ||
				(innerIdentity.get("sendreceiveaddress") == null) ||
				(innerIdentity.get("senderidaddress") == null))
			{
				JOptionPane.showMessageDialog(
					this.parentFrame, 
					"The selected JSON file has a wrong format or is not a messaging identity file!", 
					"Messaging identity has wrong format!", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			MessagingIdentity contactIdentity = new MessagingIdentity(innerIdentity);
			
			// Search through the existing contact identities, to make sure we are not adding it a second time
			for (MessagingIdentity mi : this.messagingStorage.getContactIdentities())
			{
				if (mi.isIdenticalTo(contactIdentity))
				{  
			        int choice = JOptionPane.showConfirmDialog(
		        		this.parentFrame,
		        		"There is already a contact in your contact list with the same identity. \n\n" +
		        		"Existing contact identity: " + mi.getDiplayString() + "\n" +
		        		"Contact identity being imported: " + contactIdentity.getDiplayString() + "\n\n" +
		        		"Two identities are consiered the same if their T/Z addresses are the same. \n" +
		        		"Do you want to replace the details of the existing messaging identity, with\n" +
		        		"the one being imported?", 
		        		"The same contact identity is already available", JOptionPane.YES_NO_OPTION);
			        
			        if (choice == JOptionPane.YES_OPTION) 
			        {
			        	this.messagingStorage.updateContactIdentityForSenderIDAddress(
			        		contactIdentity.getSenderidaddress(), contactIdentity);
						JOptionPane.showMessageDialog(
							this.parentFrame, 
							"Your contact's messaging identity has been successfully updated.\n", 
							"Messaging identity is successfully updated", JOptionPane.INFORMATION_MESSAGE);
						this.contactList.reloadMessagingIdentities();
			        }
			        
			        // In any case - not a new identity to add
			        return;
				}
			}
			
			// Check for the existence of an "Unknown" type of identity already - that could be
			// updated. Search can be done by T address only.
			MessagingIdentity existingUnknownID = 
				this.messagingStorage.getContactIdentityForSenderIDAddress(contactIdentity.getSenderidaddress());
			if (existingUnknownID != null)
			{
		        int choice = JOptionPane.showConfirmDialog(
		        	this.parentFrame,
		        	"There is a contact in your contact list with the same sender identification address \n" +
		        	"but with yet unknown/not yet imported full identity:\n\n" +
		        	"Existing contact identity: " + existingUnknownID.getDiplayString() + "\n" +
		        	"Contact identity being imported: " + contactIdentity.getDiplayString() + "\n\n" +
		        	"Please confirm that you want to update the details of the existing contact identity\n" +
		        	"with the one being imported?", 
		        	"Contact with the same sender identification address is already available.", 
		        	JOptionPane.YES_NO_OPTION);
			        
			    if (choice == JOptionPane.YES_OPTION) 
			    {
			       	this.messagingStorage.updateContactIdentityForSenderIDAddress(
			       		contactIdentity.getSenderidaddress(), contactIdentity);
					JOptionPane.showMessageDialog(
						this.parentFrame, 
						"Your contact's messaging identity has been successfully updated.\n", 
						"Messaging identity is successfully updated", JOptionPane.INFORMATION_MESSAGE);
					this.contactList.reloadMessagingIdentities();
			    }
				
				return;
			}
		
			// Add the new identity normally!
			this.messagingStorage.addContactIdentity(contactIdentity);
			
			int sendIDChoice = JOptionPane.showConfirmDialog(
				this.parentFrame, 
				"Your contact's messaging identity has been successfully imported: \n" + 
				contactIdentity.getDiplayString() + "\n" +
				"You can now send and receive messages from this contact. Do you wish\n" +
				"to send a limited sub-set of your contact details to this new contact\n" +
				"as a special message?\n\n" +
				"This will allow him/her to establish contact with you without manaully\n" +
				"importing your messaging identity (the way you imported his identity).",
				"Successfully imported. Send your identity over?", JOptionPane.YES_NO_OPTION);
			
			this.contactList.reloadMessagingIdentities();
			
			if (sendIDChoice == JOptionPane.YES_OPTION)
			{
				// Only a limited set of values is sent over the wire, due tr the limit of 330
				// characters. // TODO: use protocol versions with larger messages
				MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
				JsonObject innerIDObject = new JsonObject();
				innerIDObject.set("nickname",           ownIdentity.getNickname());
				innerIDObject.set("firstname",          ownIdentity.getFirstname());
				innerIDObject.set("surname",            ownIdentity.getSurname());
				innerIDObject.set("senderidaddress",    ownIdentity.getSenderidaddress());
				innerIDObject.set("sendreceiveaddress", ownIdentity.getSendreceiveaddress());
				JsonObject outerObject = new JsonObject();
				outerObject.set("zenmessagingidentity", innerIDObject);
				String identityString = outerObject.toString();
				
				// Check and send the messaging identity as a message
				if (identityString.length() <= 330) // Protocol V1 restriction
				{
					this.sendMessage(identityString, contactIdentity);
				} else
				{
					JOptionPane.showMessageDialog(
						this.parentFrame, 
						"The size of your messaging identity is unfortunately too large to be sent\n" +
						"as a message. Your contact will have to import your messaging identity\n" +
						"manaully from a json file...", 
						"Messaging identity size is too large!", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
			
		} catch (Exception ex)
		{
			Log.error("Unexpected error in importing contact messaging identity from file!", ex);
			this.errorReporter.reportError(ex, false);
		}
	}

	
	private void sendMessageAndHandleErrors()
	{
		try
		{
			sendMessage(null, null);
		} catch (Exception e)
		{
			Log.error("Unexpected error in sending message (wrapper): ", e);
			this.errorReporter.reportError(e);
		}
	}
	
	
	// String textToSend - if null, taken from the text area
	// MessagingIdentity remoteIdentity - if null selection is taken
	private void sendMessage(String textToSend, MessagingIdentity remoteIdentity)
		throws IOException, WalletCallException, InterruptedException
	{
		// Make sure contacts are available
		if (this.contactList.getNumberOfContacts() <= 0)
		{
	        JOptionPane.showMessageDialog(
        		this.parentFrame,
        		"You have no messaging contacts in your contact list. To use messaging\n" +
        		"you need to add at least one contact. You can add a contact by importing\n" +
        		"their messaging identity using the menu item Messaging >> Import contact \n" +
                "identity.",
	        	"No messaging contacts available...", JOptionPane.ERROR_MESSAGE);					
			return;			
		}
		
		final MessagingIdentity contactIdentity = 
			(remoteIdentity != null) ? remoteIdentity : this.contactList.getSelectedContact();
		// Make sure there is a selection
		if (contactIdentity == null)
		{
	        JOptionPane.showMessageDialog(
        		this.parentFrame,
        		"No messaging contact is selected in the contact list (on the right side of the UI).\n" +
        		"In order to send an outgoing message you need to select a contact to send it to!",
	        	"No messaging contact is selected...", JOptionPane.ERROR_MESSAGE);					
			return;			
		}
		
		// Make sure contact identity is full (not Unknown with no address to send to)
		if (Util.stringIsEmpty(contactIdentity.getSendreceiveaddress()))
		{
	        JOptionPane.showMessageDialog(
        		this.parentFrame,
        		"The messaging contact selected: " + contactIdentity.getDiplayString() + "\n" +
        		"seems to have no valid Z address for sending and receiving messages. \n" +
        		"Most likely the reason is that this contact's messaging identity is not \n" +
        		"imported yet. Message cannot be sent!",
	        	"Selected contact has to Z address to send message to!", JOptionPane.ERROR_MESSAGE);					
			return;
		}
		
		// Get the text to send as a message
		if (textToSend == null)
		{
			textToSend = this.writeMessageTextArea.getText();
		}
		
		if (textToSend.length() <= 0)
		{
	        JOptionPane.showMessageDialog(
        		this.parentFrame,
        		"You have not written any text for a message to be sent. Please write some text\n" +
        		"in the message text field...", 
	        	"Message text is empty", JOptionPane.ERROR_MESSAGE);					
			return;
		}
		
		// Disable sending controls, set status.
		this.sendButton.setEnabled(false);
		this.writeMessageTextArea.setEnabled(false);
				
		// Form the JSON message to be sent
		MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity(); 
		
		// Check to make sure the sending address has some funds!!!
		final double minimumBalance = 0.0002d; // TODO: for now hard coded - must be configurable
		Double balance = Double.valueOf(
			this.clientCaller.getBalanceForAddress(ownIdentity.getSendreceiveaddress()));
		Double unconfirmedBalance = Double.valueOf(
			this.clientCaller.getUnconfirmedBalanceForAddress(ownIdentity.getSendreceiveaddress()));
		
		if ((balance < minimumBalance) && (unconfirmedBalance < minimumBalance))
		{
			Log.warning("Sending address has balance: {0} and unconfirmed balance: {1}",
					    balance, unconfirmedBalance);
	        JOptionPane.showMessageDialog(
	        	this.parentFrame,
	        	"The Z address used to send/receive messages has insufficient balance: \n" +
	        	ownIdentity.getSendreceiveaddress() + "\n" +
	        	"You will be redirected to the UI tab for sending ZEN to add some balance to it. You need only\n" +
	        	"a small amount e.g. typically 0.1 ZEN is suffucient to send 500 messages. After sending some\n" +
	        	"ZEN you need to wait for the transaciton to be confirmed (typically takes 2.5 minutes). It is\n" +
	        	"recommended to send ZEN to this Z address in two or more separate transactions (though one \n" +
	        	"transaction is sufficient).", 
		        "Z address to send/receive messages has insufficient balance...", JOptionPane.ERROR_MESSAGE);
		        
	            // Restore controls and move to the send cash tab etc.
		        this.sendButton.setEnabled(true);
				this.writeMessageTextArea.setEnabled(true);
				
				sendCashPanel.prepareForSending(ownIdentity.getSendreceiveaddress());
	            parentTabs.setSelectedIndex(2);				
				return;
		}
		
		if ((balance < minimumBalance) && (unconfirmedBalance >= minimumBalance))
		{
			Log.warning("Sending address has balance: {0} and unconfirmed balance: {1}",
				        balance, unconfirmedBalance);
	        JOptionPane.showMessageDialog(
	        	this.parentFrame,
	        	"The Z address used to send/receive messages has insufficient confirmed balance: \n" +
	        	ownIdentity.getSendreceiveaddress() + "\n" +
	        	"This usually means that the previous mesasaging transaction is not yet confirmed. You\n" +
	        	"need to wait for the transaciton to be confirmed (typically takes 2.5 minutes). This\n" +
	        	"problem may be avoided if you send ZEN to this Z address in two or more separate \n" +
	        	"transactions (when you supply the ZEN balance to be used for messaging).", 
		        "Z address to send/receive messages has insufficient confirmed balance...", JOptionPane.ERROR_MESSAGE);
		        
	            // Restore controls and move to the send cash tab etc.
		        this.sendButton.setEnabled(true);
				this.writeMessageTextArea.setEnabled(true);
				
				return;
		}
		
		// Sign a HEX encoded message ... to avoid possibl UNICODE issues
		String signature = this.clientCaller.signMessage(
			ownIdentity.getSenderidaddress(), Util.encodeHexString(textToSend).toUpperCase());
		
		final JsonObject jsonInnerMessage = new JsonObject();
		jsonInnerMessage.set("ver", 1d);
		jsonInnerMessage.set("from", ownIdentity.getSenderidaddress());
		jsonInnerMessage.set("message", textToSend);
		jsonInnerMessage.set("sign", signature);
		JsonObject jsonOuterMessage = new JsonObject();
		jsonOuterMessage.set("zenmsg", jsonInnerMessage);
		
		String memoString = jsonOuterMessage.toString();
		
		// Check the size of the message to be sent, error if it exceeds.
		final int maxSendingLength = 512;
		int overallSendingLength = memoString.getBytes("UTF-8").length; 
		if (overallSendingLength > maxSendingLength)
		{
			Log.warning("Text length of exceeding message: {0}", textToSend.length());
			int difference = Math.abs(maxSendingLength - overallSendingLength);
			// We give exact size and advice on reduction...
	        JOptionPane.showMessageDialog(
        		this.parentFrame,
        		"The text of the message you have written is too long to be sent. When\n" +
        		"packaged as a memo it comes up to " + overallSendingLength + 
        		" bytes (maximum is " + maxSendingLength + " bytes)\n\n" + 
        		"Advice: try to reduce the message length by " + difference + " characters. The current\n" +
        		"version of the ZEN messaging protocol supports approximately 330\n" +
        		"characters per message (number is not exact - depends on character\n" + 
        		"encoding specifics).", 
	        	"Message size exceeds currently supported limits...", JOptionPane.ERROR_MESSAGE);
	        // Restore controls and exit
	        this.sendButton.setEnabled(true);
			this.writeMessageTextArea.setEnabled(true);
			return;
		}
			
		// TODO: amount and fee are hard coded for now
	    final String operationStatusID = this.clientCaller.sendMessage(
	    	ownIdentity.getSendreceiveaddress(), contactIdentity.getSendreceiveaddress(), "0.0001", memoString);
		
		// Start a data gathering thread specific to the operation being executed - this is done is a separate 
		// thread since the server responds more slowly during JoinSPlits and this blocks he GUI somewhat.
		final DataGatheringThread<Boolean> opFollowingThread = new DataGatheringThread<Boolean>(
			new DataGatheringThread.DataGatherer<Boolean>() 
			{
				public Boolean gatherData()
					throws Exception
				{
					long start = System.currentTimeMillis();
					Boolean result = MessagingPanel.this.clientCaller.isSendingOperationComplete(operationStatusID);
					long end = System.currentTimeMillis();
					Log.info("Checking for messaging operation " + operationStatusID + " status done in " + (end - start) + "ms." );
					
					return result;
				}
			}, 
			this.errorReporter, 2000, true);

		// Start a timer to update the progress of the operation
		this.operationStatusTimer = new Timer(2000, new ActionListener() 
		{
			public int operationStatusCounter = 0;
			
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				try
				{
					Boolean opComplete = opFollowingThread.getLastData();
					
					if ((opComplete != null) && opComplete.booleanValue())
					{
						// End the special thread used to follow the operation
						opFollowingThread.setSuspended(true);
						
						if (clientCaller.isCompletedOperationSuccessful(operationStatusID))
						{
							sendResultLabel.setText(
								"<html><span style=\"font-size:0.8em;\">Send status: &nbsp;" +
								"<span style=\"color:green;font-weight:bold\">SUCCESSFUL</span></span></html>");
						} else
						{
							String errorMessage = clientCaller.getOperationFinalErrorMessage(operationStatusID); 
							sendResultLabel.setText(
								"<html><span style=\"font-size:0.8em;\">Send status: &nbsp;" +
								"<span style=\"color:red;font-weight:bold\">ERROR! </span></span></html>");
							JOptionPane.showMessageDialog(
								MessagingPanel.this.getRootPane().getParent(), 
								"An error occurred when sending message to contact: " + contactIdentity.getDiplayString() + ". \n" +
								"Error message is: " +	errorMessage + "\n\n" +
								"If the problem persists, you may need technical support :( ...\n", 
								"Error in sending message", JOptionPane.ERROR_MESSAGE);
						}
								
								
						// Restore controls etc. final actions - reenable
						sendMessageProgressBar.setValue(0);
						operationStatusTimer.stop();						 
						sendButton.setEnabled(true);
						writeMessageTextArea.setEnabled(true);
						writeMessageTextArea.setText(""); // clear message from text area
					    
					    // Save message as outgoing
						Message msg = new Message(jsonInnerMessage);
						msg.setTime(new Date());
						msg.setDirection(DIRECTION_TYPE.SENT);
						// TODO: We can get the transaction ID for outgoing messages but is is probably unnecessary
						msg.setTransactionID(""); 
						messagingStorage.writeNewSentMessageForContact(contactIdentity, msg);
					    
					    // Update conversation text pane
						displayMessagesForContact(contactIdentity);
								
					} else
					{
						// Update the progress
						sendResultLabel.setText(
							"<html><span style=\"font-size:0.8em;\">Send status: &nbsp;" +
							"<span style=\"color:orange;font-weight:bold\">IN PROGRESS</span></span></html>");
						operationStatusCounter += 2;
						int progress = 0;
						if (operationStatusCounter <= 100)
						{
							progress = operationStatusCounter;
						} else
						{
							progress = 100 + (((operationStatusCounter - 100) * 6) / 10);
						}
						sendMessageProgressBar.setValue(progress);
					}
							
					MessagingPanel.this.repaint();
				} catch (Exception ex)
				{
					Log.error("Unexpected error in sending message: ", ex);
					MessagingPanel.this.errorReporter.reportError(ex);
				}
			}
		}); // End timer operation
		operationStatusTimer.setInitialDelay(0);
		operationStatusTimer.start();	    
	}
	
	
	private void collectAndStoreNewReceivedMessagesAndHandleErrors()
	{
		try
		{
			collectAndStoreNewReceivedMessages();
		} catch (Exception e)
		{
			Log.error("Unexpected error in gathering received messages (wrapper): ", e);
			this.errorReporter.reportError(e);
		}
	}
	
	
	private void collectAndStoreNewReceivedMessages()
		throws IOException, WalletCallException, InterruptedException
	{
		// Get the transaction IDs from all received transactions in the local storage
		// TODO: optimize/cache this
		Set<String> storedTransactionIDs = new HashSet<String>();
		for (MessagingIdentity identity : this.messagingStorage.getContactIdentities())
		{
			for (Message localMessage : this.messagingStorage.getAllMessagesForContact(identity))
			{
				if ((localMessage.getDirection() == DIRECTION_TYPE.RECEIVED) &&
					(!Util.stringIsEmpty(localMessage.getTransactionID())))
				{
					storedTransactionIDs.add(localMessage.getTransactionID());
				}
			}
		}
		
		MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity(); 
		if (ownIdentity == null)
		{
			Log.warning("Own messaging identity does not exist yet. No received messages collected!");
			return;
		}

		String ZAddress = ownIdentity.getSendreceiveaddress();
		// Get all known transactions received from the wallet
		// TODO: there seems to be no way to limit the number of transactions returned!
		JsonObject[] walletTransactions = this.clientCaller.getTransactionMessagingDataForZaddress(ZAddress);
		
		// Filter the transactions to obtain only those that have memos parsable as JSON
		// and being real messages. In addition only those remain that are not registered before
		List<Message> filteredMessages = new ArrayList<Message>();
		for (JsonObject trans : walletTransactions)
		{
			String memoHex = trans.getString("memo", "ERROR");
			String transactionID = trans.getString("txid",  "ERROR");
			if (!memoHex.equals("ERROR"))
			{
				String decodedMemo = Util.decodeHexMemo(memoHex);
				JsonObject jsonMessage = null;
				try
				{
					if (decodedMemo != null)
					{
						jsonMessage = Util.parseJsonObject(decodedMemo);
					}
				} catch (Exception ex)
				{
					// TODO: maybe log unparsable memos 
				}
				
				if (jsonMessage != null)
				{
					if ((jsonMessage.get("zenmsg") != null) &&
						(!storedTransactionIDs.contains(transactionID)))
					{
						// Finally test that the message has all attributes required
						// TODO: handle anonymous messages differently
						Message message = new Message(jsonMessage.get("zenmsg").asObject());
						// Set additional message attributes not available over the wire
						message.setDirection(DIRECTION_TYPE.RECEIVED);
						message.setTransactionID(transactionID);
						String UNIXDate = this.clientCaller.getWalletTransactionTime(transactionID);
						message.setTime(new Date(Long.valueOf(UNIXDate).longValue() * 1000L));
						
						if ((!Util.stringIsEmpty(message.getFrom()))    &&
							(!Util.stringIsEmpty(message.getMessage())) &&
							(!Util.stringIsEmpty(message.getSign())))
						{
							// TODO: additional sanity check that T/Z addresses are valid etc.
							filteredMessages.add(message);
						} else
						{
							// Warn of unexpected message content
							Log.warning("Ignoring received mesage with invalid or incomplete content: {0}",
									    jsonMessage.toString());
						}
					}
				}
			}
		}

		// Finally we have all messages that are new and unprocessed. For every message we find out
		// who the sender is, verify it and store it
		boolean bNewContactCreated = false;
		message_loop:
		for (Message message : filteredMessages)
		{
			MessagingIdentity contactID = 
				this.messagingStorage.getContactIdentityForSenderIDAddress(message.getFrom());
			if (contactID == null)
			{
				// Update list of contacts with an unknown remote user ... to be updated later
				Log.warning("Message is from unknown contact: {0} . " + 
						    "A new Unknown_xxx contact will be created!", 
						    message.toJSONObject(false).toString());
				contactID = this.messagingStorage.createAndStoreUnknownContactIdentity(message.getFrom());
			    bNewContactCreated = true;
			}
			
			// Verify the message signature
			if (this.clientCaller.verifyMessage(message.getFrom(), message.getSign(), 
				                                Util.encodeHexString(message.getMessage()).toUpperCase()))
			{
				// Handle the special case of a messaging identity sent as payload - update identity then
				if (this.isZENIdentityMessage(message.getMessage()))
				{
					this.updateAndStoreExistingIdentityFromIDMessage(contactID, message.getMessage());
				}
				message.setVerification(VERIFICATION_TYPE.VERIFICATION_OK);
			} else
			{
				//Set verification status permanently - store even invalid messages
				Log.error("Message signature is invalid {0} . Message will be stored as invalid!", 
						   message.toJSONObject(false).toString());
				message.setVerification(VERIFICATION_TYPE.VERIFICATION_FAILED);
			}
			
		    this.messagingStorage.writeNewReceivedMessageForContact(contactID, message);
		} // End for (Message message : filteredMessages)
		
		if (bNewContactCreated)
		{
			SwingUtilities.invokeLater(new Runnable() 
			{	
				@Override
				public void run() 
				{
					try
					{
						MessagingPanel.this.contactList.reloadMessagingIdentities();
					} catch (Exception e)
					{
						Log.error("Unexpected error in reloading contacts after gathering messages: ", e);
						MessagingPanel.this.errorReporter.reportError(e);
					}
				}
			});
		}		
		
		SwingUtilities.invokeLater(new Runnable() 
		{	
			@Override
			public void run() 
			{
				try
				{
					// Reload the messages for the currently selected user
					final MessagingIdentity selectedContact = MessagingPanel.this.contactList.getSelectedContact();
					if (selectedContact != null)
					{
						MessagingPanel.this.displayMessagesForContact(selectedContact);
					}
				} catch (Exception e)
				{
					Log.error("Unexpected error in updating message pane after gathering messages: ", e);
					MessagingPanel.this.errorReporter.reportError(e);
				}
			}
		});
	}
	
	
	/**
	 * Checks if a message contains a ZEN messaging identity in it.
	 * 
	 * @param message
	 * 
	 * @return true if a ZEN identity is inside
	 */
	public boolean isZENIdentityMessage(String message)
	{
		if (message == null)
		{
			return false;
		}
		
		if (!message.trim().startsWith("{"))
		{
			return false;
		}
		
		JsonObject jsonMessage = null;
		try
		{
			jsonMessage = Util.parseJsonObject(message);
		} catch (Exception ex)
		{
			return false;
		}
		
		if (jsonMessage.get("zenmessagingidentity") == null)
		{
			return false;
		}
		
		JsonObject innerMessage = jsonMessage.get("zenmessagingidentity").asObject();
		if ((innerMessage.get("nickname") == null)           ||
			(innerMessage.get("sendreceiveaddress") == null) ||
			(innerMessage.get("senderidaddress") == null))
		{
			return false;
		}
		
		// All conditions met - return true
		return true;
	}
	

	// Copies the fields sent over the wire - limited set - some day all fields will be sent
	// Sender ID address is assumed to be the same
	public void updateAndStoreExistingIdentityFromIDMessage(MessagingIdentity existingIdentity, String idMessage)
		throws IOException
	{
		MessagingIdentity newID = new MessagingIdentity(
			Util.parseJsonObject(idMessage).get("zenmessagingidentity").asObject());

		if (!Util.stringIsEmpty(newID.getSenderidaddress()))
		{
			existingIdentity.setSenderidaddress(newID.getSenderidaddress());
		}
		
		if (!Util.stringIsEmpty(newID.getSendreceiveaddress()))
		{
			existingIdentity.setSendreceiveaddress(newID.getSendreceiveaddress());
		}
		
		if (!Util.stringIsEmpty(newID.getNickname()))
		{
			existingIdentity.setNickname(newID.getNickname());
		}
		
		if (!Util.stringIsEmpty(newID.getFirstname()))
		{
			existingIdentity.setFirstname(newID.getFirstname());
		}
		
		if (!Util.stringIsEmpty(newID.getMiddlename()))
		{
			existingIdentity.setMiddlename(newID.getMiddlename());
		}
		
		if (!Util.stringIsEmpty(newID.getSurname()))
		{
			existingIdentity.setSurname(newID.getSurname());
		}
		
		this.messagingStorage.updateContactIdentityForSenderIDAddress(
			existingIdentity.getSenderidaddress(), existingIdentity);
	}
	
}
