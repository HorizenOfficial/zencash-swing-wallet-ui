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
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;

import com.vaklinov.zcashui.StatusUpdateErrorReporter;
import com.vaklinov.zcashui.WalletTabPanel;
import com.vaklinov.zcashui.ZCashClientCaller;
import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;


/**
 * Main panel for messaging
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class MessagingPanel
	extends WalletTabPanel
{
	private ZCashClientCaller clientCaller;
	private StatusUpdateErrorReporter errorReporter;
	
	private MessagingStorage messagingStorage;
	
	private JContactListPanel contactList;
	

	
	public MessagingPanel(ZCashClientCaller clientCaller, StatusUpdateErrorReporter errorReporter)
		throws IOException, InterruptedException, WalletCallException
	{
		super();
		
		this.clientCaller = clientCaller;
		this.errorReporter = errorReporter;
		
		this.messagingStorage = new MessagingStorage();
		
		this.setLayout(new BorderLayout(0, 0));
		this.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		
		final JSplitPane textAndContactsPane = new JSplitPane();
		//textAndContactsPane.setDividerLocation(480); // TODO - not sure
		this.add(textAndContactsPane, BorderLayout.CENTER);
		
		this.contactList = new JContactListPanel(this.messagingStorage);
		textAndContactsPane.setRightComponent(this.contactList);
		
		JPanel conversationPanel = new JPanel(new BorderLayout(0, 0));
		conversationPanel.add(
			new JScrollPane(new JTextArea("xfgfffffffffffffffff")), BorderLayout.CENTER);
		JPanel upperPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		upperPanel.add(new JLabel("Conversation with Rolf Versluis:"));
		upperPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		conversationPanel.add(upperPanel, BorderLayout.NORTH);		
		
		textAndContactsPane.setLeftComponent(conversationPanel);
		SwingUtilities.invokeLater(new Runnable() { // TODO: does not work
			@Override
			public void run() {
				textAndContactsPane.setDividerLocation(570);			
				}
		});
		
		
		JPanel writeAndSendPanel = new JPanel(new BorderLayout(0, 0));
		this.add(writeAndSendPanel, BorderLayout.SOUTH);
		
		JPanel writePanel = new JPanel(new BorderLayout(0, 0));
		JTextArea messageTextArea = new JTextArea(3, 50);
		messageTextArea.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		writePanel.add(new JScrollPane(messageTextArea), BorderLayout.CENTER);
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
	}
	
	
}
