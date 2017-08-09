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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;

import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.OSUtil;
import com.vaklinov.zcashui.StatusUpdateErrorReporter;
import com.vaklinov.zcashui.WalletTabPanel;
import com.vaklinov.zcashui.ZCashClientCaller;
import com.vaklinov.zcashui.ZCashUI;
import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;
import com.vaklinov.zcashui.msg.Message.DIRECTION_TYPE;


/**
 * Main panel for messaging
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class MessagingPanel
	extends WalletTabPanel
{
	private JFrame parentFrame;
	private ZCashClientCaller clientCaller;
	private StatusUpdateErrorReporter errorReporter;
	
	private MessagingStorage messagingStorage;
	
	private JContactListPanel contactList;
	
	private JLabel conversationLabel;
	private JTextArea conversationTextArea;
	
	private JTextArea writeMessageTextArea;
	

	
	public MessagingPanel(JFrame parentFrame, ZCashClientCaller clientCaller, StatusUpdateErrorReporter errorReporter)
		throws IOException, InterruptedException, WalletCallException
	{
		super();
		
		this.parentFrame      = parentFrame;
		this.clientCaller     = clientCaller;
		this.errorReporter    = errorReporter;
		this.messagingStorage = new MessagingStorage();
		
		
		// Start building UI
		this.setLayout(new BorderLayout(0, 0));
		this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		final JSplitPane textAndContactsPane = new JSplitPane();
		//textAndContactsPane.setDividerLocation(480); // TODO - not sure
		this.add(textAndContactsPane, BorderLayout.CENTER);
		
		this.contactList = new JContactListPanel(this, this.messagingStorage, this.errorReporter);
		textAndContactsPane.setRightComponent(this.contactList);
		
		JPanel conversationPanel = new JPanel(new BorderLayout(0, 0));
		conversationPanel.add(
			new JScrollPane(
				this.conversationTextArea = new JTextArea("xfgfffffffffffffffff"),
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), 
			BorderLayout.CENTER);
		this.conversationTextArea.setLineWrap(true);
		JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		upperPanel.add(this.conversationLabel = new JLabel("Conversation with Rolf Versluis:"));
		upperPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		conversationPanel.add(upperPanel, BorderLayout.NORTH);		
		
		textAndContactsPane.setLeftComponent(conversationPanel);
		SwingUtilities.invokeLater(new Runnable() { // TODO: does not work... or maybe it does
			@Override
			public void run() {
				textAndContactsPane.setDividerLocation(570);			
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
		sendLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		writePanel.add(sendLabel, BorderLayout.NORTH);
		writePanel.add(new JLabel(""), BorderLayout.EAST); // dummy
		writeAndSendPanel.add(writePanel, BorderLayout.CENTER);
		
		JPanel sendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JPanel sendButtonPanel = new JPanel();
		sendButtonPanel.setLayout(new BoxLayout(sendButtonPanel, BoxLayout.Y_AXIS));
		JLabel filler = new JLabel(" ");
		filler.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		sendButtonPanel.add(filler); // TODO: filler
		JButton sendButton = new JButton("Send message \u27A4");
		JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendButton);
		sendButtonPanel.add(tempPanel);
		JProgressBar sendProgress = new JProgressBar();
		sendProgress.setPreferredSize(
			new Dimension(sendButton.getPreferredSize().width, 
					      sendProgress.getPreferredSize().height * 2 / 3));
		sendProgress.setValue(23); // TODO: dummy value
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendProgress);
		sendButtonPanel.add(tempPanel);
		JLabel sendResultLabel = new JLabel("<html><span style=\"font-size:0.8em;\">" +
				"Sending status: SUCCESS &nbsp;" +
			    "</span>  ");
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendResultLabel);
		sendButtonPanel.add(tempPanel);
		
		sendPanel.add(sendButtonPanel);
		writeAndSendPanel.add(sendPanel, BorderLayout.EAST);
		
		// Attach logic
		
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
		
		for (Message msg : messages)
		{
			text.append(msg.getDirection() == DIRECTION_TYPE.SENT ? "\u21E8 " : "\u21E6 ");
			text.append("(");
			text.append(msg.getTime().toString()); // TODO: correct date further
			text.append(") ");
			text.append(msg.getDirection() == DIRECTION_TYPE.SENT ? 
					       ownIdentity.getNickname() : contact.getNickname());
			text.append(": ");
			text.append(msg.getMessage());
			text.append("\n");
		}
		
		this.conversationTextArea.setText(text.toString());
		
		this.conversationLabel.setText("Conversation with: " + contact.getDiplayString());
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
	                "Welcome to ZENCash messaging. As start you will need to create a new messaging\n" + 
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
	                "(This mesage will be shown only once.)",
	                "Welcome to messaging", JOptionPane.INFORMATION_MESSAGE);
		        	        
		        // Show the GUI dialog to edit an initially empty messaging identity
		        this.openOwnIdentityDialog();
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
				
				// TODO: maybe set wait cursor here
				// Create the T/Z addresses to be used for messaging
				String TAddress = this.clientCaller.createNewAddress(false);
				String ZAddress = this.clientCaller.createNewAddress(true);
				
				ownIdentity.setSenderidaddress(TAddress);
				ownIdentity.setSendreceiveaddress(ZAddress);
			}
			
			// Dialog will automatically save the identity if the user chooses so 
			OwnIdentityEditDialog ownIdentityDialog = new OwnIdentityEditDialog(
				this.parentFrame, ownIdentity, this.messagingStorage, this.errorReporter, identityIsBeingCreated);
			ownIdentityDialog.setVisible(true);
		} catch (Exception ex)
		{
			Log.error("Unexpected error in editing own messaging identity!", ex);
			this.errorReporter.reportError(ex, false);
		}
	}
}
