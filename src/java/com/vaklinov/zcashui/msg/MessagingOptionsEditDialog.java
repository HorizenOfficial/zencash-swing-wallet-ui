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
package com.vaklinov.zcashui.msg;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;

import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.StatusUpdateErrorReporter;
import com.vaklinov.zcashui.Util;


/**
 * Dialog showing the messaging options and allowing them to be edited.
 */
public class MessagingOptionsEditDialog
	extends JDialog
{
	protected JFrame parentFrame;
	protected MessagingStorage storage;
	protected StatusUpdateErrorReporter errorReporter;
	
	protected JLabel infoLabel;
	protected JPanel buttonPanel;
	
	protected JTextField amountTextField;
	protected JTextField transactionFeeTextField;
	protected JCheckBox  automaticallyAddUsers;
	
	public MessagingOptionsEditDialog(JFrame parentFrame, MessagingStorage storage, StatusUpdateErrorReporter errorReporter)
		throws IOException
	{
		this.parentFrame   = parentFrame;
		this.storage       = storage;
		this.errorReporter = errorReporter;
		
		this.setTitle("Messaging options");
		this.setModal(true);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		MessagingOptions options = this.storage.getMessagingOptions();
			
		this.getContentPane().setLayout(new BorderLayout(0, 0));
			
		JPanel tempPanel = new JPanel(new BorderLayout(0, 0));
		tempPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		infoLabel = new JLabel(
				"<html><span style=\"font-size:0.93em;\">" +
				"The options below pertain to messaging. It is possible to set the amount of ZEN<br/>" +
				"to be sent with every messaging transaction and also the transaction fee. It is<br/>" + 
			    "also possible to decide if users are to be automatically added to the contact list.<br/><br/>" +
			    "</span>");
	    tempPanel.add(infoLabel, BorderLayout.CENTER);
		this.getContentPane().add(tempPanel, BorderLayout.NORTH);
			
		JPanel detailsPanel = new JPanel();
		detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
		
		addFormField(detailsPanel, "Automatically add users to contact list:",   
				     automaticallyAddUsers = new JCheckBox());
		addFormField(detailsPanel, "ZEN amount to send with every message:",   amountTextField = new JTextField(12));
		addFormField(detailsPanel, "Transaction fee:",  transactionFeeTextField = new JTextField(12));
		
		DecimalFormatSymbols decSymbols = new DecimalFormatSymbols(Locale.ROOT);
		automaticallyAddUsers.setSelected(options.isAutomaticallyAddUsersIfNotExplicitlyImported());
		amountTextField.setText(new DecimalFormat("########0.00######", decSymbols).format(options.getAmountToSend()));
		transactionFeeTextField.setText(new DecimalFormat("########0.00######", decSymbols).format(options.getTransactionFee()));
		
		detailsPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		this.getContentPane().add(detailsPanel, BorderLayout.CENTER);

		// Lower buttons - by default only close is available
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 3));
		JButton closeButon = new JButton("Close");
		buttonPanel.add(closeButon);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		closeButon.addActionListener(new ActionListener()
		{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					MessagingOptionsEditDialog.this.setVisible(false);
					MessagingOptionsEditDialog.this.dispose();
				}
		});
		
		JButton saveButon = new JButton("Save & close");
		buttonPanel.add(saveButon);
		saveButon.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					String amountToSend = MessagingOptionsEditDialog.this.amountTextField.getText();
					String transactionFee = MessagingOptionsEditDialog.this.transactionFeeTextField.getText();
					
					if ((!MessagingOptionsEditDialog.this.verifyNumericField("amount to send", amountToSend)) ||
						(!MessagingOptionsEditDialog.this.verifyNumericField("transaction fee", transactionFee)))
					{
						return;
					}
					
					MessagingOptions options = MessagingOptionsEditDialog.this.storage.getMessagingOptions();
					
					options.setAmountToSend(Double.parseDouble(amountToSend));				
					options.setTransactionFee(Double.parseDouble(transactionFee));
					options.setAutomaticallyAddUsersIfNotExplicitlyImported(
						MessagingOptionsEditDialog.this.automaticallyAddUsers.isSelected());
					
					MessagingOptionsEditDialog.this.storage.updateMessagingOptions(options);
					
					MessagingOptionsEditDialog.this.setVisible(false);
					MessagingOptionsEditDialog.this.dispose();
				} catch (Exception ex)
				{
					Log.error("Unexpected error in editing own messaging identity!", ex);
					MessagingOptionsEditDialog.this.errorReporter.reportError(ex, false);
				}
			}
		});

		this.pack();
		this.setLocation(100, 100);
		this.setLocationRelativeTo(parentFrame);
	}

	
	private boolean verifyNumericField(String name, String value)
	{
		if (Util.stringIsEmpty(value))
		{
	        JOptionPane.showMessageDialog(
        		this.parentFrame,
        		"Field \"" + name + "\" is empty. It is mandatory. Please fill it.",
                "Mandatory data missing", JOptionPane.ERROR_MESSAGE);
	        return false;
		}
		
		try
		{
			double dVal = Double.parseDouble(value);
			
			if (dVal < 0)
			{
		        JOptionPane.showMessageDialog(
		        	this.parentFrame,
		        	"Field \"" + name + "\" has a value that is negative. Please enter a positive number!",
		            "Field is negative", JOptionPane.ERROR_MESSAGE);
		        return false;			
			}
		} catch (NumberFormatException nfe)
		{
	        JOptionPane.showMessageDialog(
	        	this.parentFrame,
	        	"Field \"" + name + "\" has a value that is not numeric. Please enter a number!",
	            "Field is not numeric", JOptionPane.ERROR_MESSAGE);
		    return false;			
		}
		
		return true;
	}
	
	
	private void addFormField(JPanel detailsPanel, String name, JComponent field)
	{
		JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
		JLabel tempLabel = new JLabel(name, JLabel.RIGHT);
		// TODO: hard sizing of labels may not scale!
		final int width = new JLabel("ZEN amount to send with every message:").getPreferredSize().width + 30;
		tempLabel.setPreferredSize(new Dimension(width, tempLabel.getPreferredSize().height));
		tempPanel.add(tempLabel);
		tempPanel.add(field);
		detailsPanel.add(tempPanel);
	}
	
} 
