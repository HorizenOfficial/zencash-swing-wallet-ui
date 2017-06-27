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


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;


/**
 * Dialog to get the user password for encrypted wallets - for unlock.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class PasswordDialog
	extends JDialog
{
	protected boolean isOKPressed = false;
	protected String  password    = null;
	
	protected JLabel     passwordLabel = null;
	protected JTextField passwordField = null;
	
	protected JLabel upperLabel;
	protected JLabel lowerLabel;
	
	protected JPanel freeSlotPanel;
	protected JPanel freeSlotPanel2;
	
	public PasswordDialog(JFrame parent)
	{
		super(parent);
		
		this.setTitle("Password...");
	    this.setLocation(parent.getLocation().x + 50, parent.getLocation().y + 50);
		this.setModal(true);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		JPanel controlsPanel = new JPanel();
		controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.Y_AXIS));
		controlsPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel tempPanel = new JPanel(new BorderLayout(0, 0));
		tempPanel.add(this.upperLabel = new JLabel("<html>The wallet is encrypted and protected with a password. " +
		                         "Please enter the password to unlock it temporarily during " +
				                 "the operation</html>"), BorderLayout.CENTER);
		controlsPanel.add(tempPanel);
		
		JLabel dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 8));
		controlsPanel.add(dividerLabel);
		
		tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		tempPanel.add(passwordLabel = new JLabel("Password: "));
		tempPanel.add(passwordField = new JPasswordField(30));
		controlsPanel.add(tempPanel);
		
		dividerLabel = new JLabel("   ");
		dividerLabel.setFont(new Font("Helvetica", Font.PLAIN, 8));
		controlsPanel.add(dividerLabel);
		
		this.freeSlotPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		controlsPanel.add(this.freeSlotPanel);
		
		this.freeSlotPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		controlsPanel.add(this.freeSlotPanel2);

		tempPanel = new JPanel(new BorderLayout(0, 0));
		tempPanel.add(this.lowerLabel = new JLabel("<html><span style=\"font-weight:bold\">" + 
		                         "WARNING: Never enter your password on a public/shared " +
		                         "computer or one that you suspect has been infected with malware! " +
				                 "</span></html>"), BorderLayout.CENTER);
		controlsPanel.add(tempPanel);
		
		this.getContentPane().setLayout(new BorderLayout(0, 0));
		this.getContentPane().add(controlsPanel, BorderLayout.NORTH);

		// Form buttons
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
		JButton okButon = new JButton("OK");
		buttonPanel.add(okButon);
		buttonPanel.add(new JLabel("   "));
		JButton cancelButon = new JButton("Cancel");
		buttonPanel.add(cancelButon);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		okButon.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				PasswordDialog.this.processOK();
			}
		});
		
		cancelButon.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				PasswordDialog.this.setVisible(false);
				PasswordDialog.this.dispose();
				
				PasswordDialog.this.isOKPressed = false;
				PasswordDialog.this.password = null;
			}
		});
		
		this.setSize(450, 190);
		this.validate();
		this.repaint();
	}
	
	
	protected void processOK()
	{
		String pass = PasswordDialog.this.passwordField.getText();
		
		if ((pass == null) || (pass.trim().length() <= 0))
		{
			JOptionPane.showMessageDialog(
				PasswordDialog.this.getParent(), 
				"The password is empty. Please enter it into the text field.", "Empty...", 
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		PasswordDialog.this.setVisible(false);
		PasswordDialog.this.dispose();
		
		PasswordDialog.this.isOKPressed = true;
		PasswordDialog.this.password = pass;
	}
	
	
	public boolean isOKPressed()
	{
		return this.isOKPressed;
	}
	
	
	public String getPassword()
	{
		return this.password;
	}
}
