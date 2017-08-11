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
import java.util.Date;
import java.util.List;

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
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import com.vaklinov.zcashui.DataGatheringThread;
import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.OSUtil;
import com.vaklinov.zcashui.SendCashPanel;
import com.vaklinov.zcashui.StatusUpdateErrorReporter;
import com.vaklinov.zcashui.Util;
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
	private JTextPane conversationTextPane;
	
	private JTextArea writeMessageTextArea;
	private JButton sendButton;
	private JLabel sendResultLabel;
	private JProgressBar sendMessageProgressBar;
	
	private Timer operationStatusTimer;

	
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
		upperPanel.add(this.conversationLabel = new JLabel("Conversation ..."));
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
		sendButton = new JButton("Send message \u27A4");
		JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendButton);
		sendButtonPanel.add(tempPanel);
		sendMessageProgressBar = new JProgressBar();
		sendMessageProgressBar.setPreferredSize(
			new Dimension(sendButton.getPreferredSize().width, 
					      sendMessageProgressBar.getPreferredSize().height * 2 / 3));
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(sendMessageProgressBar);
		sendButtonPanel.add(tempPanel);
		sendResultLabel = new JLabel(
				"<html><span style=\"font-size:0.8em;\">" +
				"Sending status: &nbsp;</span>");
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
			
			text.append("<span style=\"color:" + color +";\">");
			text.append("<span style=\"font-weight:bold;font-size:1.5em;\">");
			text.append(msg.getDirection() == DIRECTION_TYPE.SENT ? "\u21E8 " : "\u21E6 ");
			text.append("</span>");
			text.append("(");
			text.append(stamp); 
			text.append(") ");
			text.append("<span style=\"font-weight:bold;\">");
			text.append(msg.getDirection() == DIRECTION_TYPE.SENT ? 
					    Util.escapeHTMLValue(ownIdentity.getNickname()) : 
					    Util.escapeHTMLValue(contact.getNickname()));
			text.append("</span>");
			text.append(": ");
			text.append("</span>");
			text.append(Util.escapeHTMLValue(msg.getMessage()));
			text.append("<br/>");
		}
		
		this.conversationTextPane.setText("<html>" + text.toString() + "</html>");
		
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
				
				// TODO: update address book
				
				// TODO: make sure T address has no balance - wallet may return a used adderss!
				
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
				topIdentityObject = Json.parse(r).asObject();
			} finally
			{
				if (r != null)
				{
					r.close();
				}
			}
			
			// TODO: validate the fields inside the objects, make sure this is indeed an identity
			// verify mandatory etc.
			
			JsonObject innerIdentity = topIdentityObject.get("zenmessagingidentity").asObject();
			MessagingIdentity contactIdentity = new MessagingIdentity(innerIdentity);
			
			// Search through the existing contact identities, to make sure we are not adding it a second time
			for (MessagingIdentity mi : this.messagingStorage.getContactIdentities())
			{
				if (mi.isIdenticalTo(contactIdentity))
				{
					// TODO: maybe allow updating the partner identity fields that are different  
			        JOptionPane.showMessageDialog(
		        		this.parentFrame,
		        		"There is already a contact in your contact list with the same identity. \n" +
		        		"Two identities are consiered the same if their T/Z addresses are the same. \n" +
		        		"Import is cancelled!", 
		        		"The same contact identity is already available", JOptionPane.ERROR_MESSAGE);					
					return;
				}
			}
		
			this.messagingStorage.addContactIdentity(contactIdentity);
			
			JOptionPane.showMessageDialog(
				this.parentFrame, 
				"Your partner's messaging identity has been successfully imported: \n" + 
				contactIdentity.getDiplayString() + "\n" +
				"You can now send and receive messages from this contact.",
				"Messaging identity is successfully imported...", JOptionPane.INFORMATION_MESSAGE);
			
			this.contactList.reloadMessagingIdentities();
			
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
			
		} catch (Exception e)
		{
			
		}
	}
	
	
	private void sendMessage()
		throws IOException, WalletCallException, InterruptedException
	{
		// Make sure contacts are available
		if (this.contactList.getNumberOfCOntacts() <= 0)
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
		
		final MessagingIdentity contactIdentity = this.contactList.getSelectedContact();
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
		
		// Get the text to send as a message
		String textToSend = this.writeMessageTextArea.getText();
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
		
		// TODO: check to make sure the sending address has some funds!!!
		
		// Form the JSON message to be sent
		MessagingIdentity ownIdentity = this.messagingStorage.getOwnIdentity();
		
		// TODO: maybe sign a HEX encoded message ... change the spec as well.
		String signature = this.clientCaller.signMessage(ownIdentity.getSenderidaddress(), textToSend);
		
		final JsonObject jsonInnerMessage = new JsonObject();
		jsonInnerMessage.set("ver", 1d);
		jsonInnerMessage.set("from", ownIdentity.getSenderidaddress());
		jsonInnerMessage.set("message", textToSend);
		jsonInnerMessage.set("sign", signature);
		JsonObject jsonOuterMessage = new JsonObject();
		jsonOuterMessage.set("zenmsg", jsonInnerMessage);
		
		String memoString = jsonOuterMessage.toString();
		
		// Check the size of the message to be sent, error if it exceeds.
		if (memoString.getBytes("UTF-8").length > 512)
		{
	        JOptionPane.showMessageDialog(
        		this.parentFrame,
        		"The text of the message you have written is too long to be sent. The current\n" +
        		"version of the ZEN messaging protocol supports approximately 330 characters\n" +
        		"per message (number is not exact - depends on character encoding specifics).", 
	        	"Message size exceeds currently supported limits...", JOptionPane.ERROR_MESSAGE);					
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
								"<html><span style=\"font-size:0.8em;\">Sending status: &nbsp;" +
								"<span style=\"color:green;font-weight:bold\">SUCCESSFUL</span></span></html>");
						} else
						{
							String errorMessage = clientCaller.getOperationFinalErrorMessage(operationStatusID); 
							sendResultLabel.setText(
								"<html><span style=\"font-size:0.8em;\">Sending status: &nbsp;" +
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
						msg.setTransactionID(null); // TODO: see if we can get the transaction ID for outgoing
						messagingStorage.writeNewSentMessageForContact(contactIdentity, msg);
					    
					    // TODO: update conversation text pane
								
					} else
					{
						// Update the progress
						sendResultLabel.setText(
							"<html><span style=\"font-size:0.8em;\">Sending status: &nbsp;" +
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
					Log.error("Unexpected error: ", ex);
					MessagingPanel.this.errorReporter.reportError(ex);
				}
			}
		}); // End timer operation
		operationStatusTimer.setInitialDelay(0);
		operationStatusTimer.start();	    
	}
}
