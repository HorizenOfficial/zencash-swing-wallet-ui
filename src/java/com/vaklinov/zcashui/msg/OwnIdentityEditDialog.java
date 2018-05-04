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


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.StatusUpdateErrorReporter;


/**
 * Dialog used to edit one's own identity
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class OwnIdentityEditDialog
	extends IdentityInfoDialog
{
	private MessagingStorage storage;
	private StatusUpdateErrorReporter errorReporter;
	
	public OwnIdentityEditDialog(JFrame parent, MessagingIdentity identity, 
			                     MessagingStorage storage, StatusUpdateErrorReporter errorReporter, boolean identityIsBeingCreated)
	{
		super(parent, identity);

		this.storage       = storage;
		this.errorReporter = errorReporter;
		
		this.setTitle("Own messaging identity - edit...");
		
		this.infoLabel.setText(
			"<html><span style=\"font-size:0.97em;\">" +
			"The fields below make up your messaging identity. This information is meant to be " +
			"shared with other users.<br/> The only mandatory field is the \"Nick name\"." +
			"</span>");
		
		nicknameTextField.setEditable(true);
		firstnameTextField.setEditable(true);
		middlenameTextField.setEditable(true);
		surnameTextField.setEditable(true);
		emailTextField.setEditable(true);
		streetaddressTextField.setEditable(true);
		facebookTextField.setEditable(true);
		twitterTextField.setEditable(true);
				
		// Build the save and Cancel buttons
		if (identityIsBeingCreated)
		{
			// If the identity is being created only save is allowed
			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			this.buttonPanel.removeAll();
		}
		
		JButton saveButon = new JButton("Save & close");
		buttonPanel.add(saveButon);
		saveButon.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					// Check for validity and save the data - T/Z addresses are not changed!
					String nick = OwnIdentityEditDialog.this.nicknameTextField.getText();
					if ((nick == null) || nick.trim().length() <= 0)
					{
				        JOptionPane.showMessageDialog(
			        		OwnIdentityEditDialog.this.parentFrame,
			        		"The nick name field is empty. It is mandatory - please fill it.",
			                "Mandatory data missing", JOptionPane.ERROR_MESSAGE);
				        return;
					}
					
					// TODO: check validity of fields to avoid entering rubbish (e.g. invalid e-mail)
					
					// Save all identity fields from the text fields
					MessagingIdentity id = OwnIdentityEditDialog.this.identity;
					id.setNickname(OwnIdentityEditDialog.this.nicknameTextField.getText());
					id.setFirstname(OwnIdentityEditDialog.this.firstnameTextField.getText());
					id.setMiddlename(OwnIdentityEditDialog.this.middlenameTextField.getText());
					id.setSurname(OwnIdentityEditDialog.this.surnameTextField.getText());
					id.setEmail(OwnIdentityEditDialog.this.emailTextField.getText());
					id.setStreetaddress(OwnIdentityEditDialog.this.streetaddressTextField.getText());
					id.setFacebook(OwnIdentityEditDialog.this.facebookTextField.getText());
					id.setTwitter(OwnIdentityEditDialog.this.twitterTextField.getText());
					
					// Save the identity
					OwnIdentityEditDialog.this.storage.updateOwnIdentity(id);
					
					OwnIdentityEditDialog.this.setVisible(false);
					OwnIdentityEditDialog.this.dispose();
				} catch (Exception ex)
				{
					Log.error("Unexpected error in editing own messaging identity!", ex);
					OwnIdentityEditDialog.this.errorReporter.reportError(ex, false);
				}
			}
		});

		this.pack();
		this.setLocation(100, 100);
		this.setLocationRelativeTo(parent);
	}
	
} // End public class OwnIdentityEditDialog
