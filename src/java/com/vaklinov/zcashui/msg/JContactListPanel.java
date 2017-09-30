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
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.StatusUpdateErrorReporter;


/**
 * Main panel for messaging
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class JContactListPanel
	extends JPanel
{
	private MessagingPanel   parent;
	private MessagingStorage mesagingStorage;
	private ContactList      list;
	private StatusUpdateErrorReporter errorReporter;
	
	public JContactListPanel(MessagingPanel parent, MessagingStorage messagingStorage, 
			                 StatusUpdateErrorReporter errorReporter)
		throws IOException
	{
		super();
		
		this.parent = parent;
		this.mesagingStorage = messagingStorage;
		this.errorReporter   = errorReporter;
		
		this.setLayout(new BorderLayout(0, 0));
		
		list = new ContactList();
		list.setIdentities(this.mesagingStorage.getContactIdentities(true));
		this.add(new JScrollPane(list), BorderLayout.CENTER);
		
		JPanel upperPanel = new JPanel(new BorderLayout(0, 0));
		upperPanel.add(new JLabel(
			"<html><span style=\"font-size:1.2em;font-style:italic;\">Contact list: &nbsp;</span>"),
			BorderLayout.WEST);
		URL addIconUrl = this.getClass().getClassLoader().getResource("images/add12.png");
        ImageIcon addIcon = new ImageIcon(addIconUrl);
        URL removeIconUrl = this.getClass().getClassLoader().getResource("images/remove12.png");
        ImageIcon removeIcon = new ImageIcon(removeIconUrl);
        JButton addButton = new JButton(addIcon);
        addButton.setToolTipText("Add contact...");
        JButton removeButton = new JButton(removeIcon);
        removeButton.setToolTipText("Remove contact...");
        JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        tempPanel.add(removeButton);
        tempPanel.add(addButton);
        upperPanel.add(tempPanel, BorderLayout.EAST);
        
        upperPanel.add(new JLabel(
    			"<html><span style=\"font-size:1.6em;font-style:italic;\">&nbsp;</span>"),
    			BorderLayout.CENTER);
		upperPanel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
		this.add(upperPanel, BorderLayout.NORTH);
		
		// Add a listener for adding a contact
		addButton.addActionListener(new ActionListener() 
		{	
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				JContactListPanel.this.parent.importContactIdentity();
			}
		});
		
		// Add a listener for removing a contact
		removeButton.addActionListener(new ActionListener() 
		{	
			@Override
			public void actionPerformed(ActionEvent e) 
			{
				JContactListPanel.this.parent.removeSelectedContact();
			}
		});
		
		// Take care of updating the messages on selection
		list.addListSelectionListener(new ListSelectionListener() 
		{	
			@Override
			public void valueChanged(ListSelectionEvent e) 
			{
				try
				{
					MessagingIdentity id = JContactListPanel.this.list.getSelectedValue();
					
					if (id == null)
					{
						return; // Nothing selected
					}
					
					JContactListPanel.this.parent.displayMessagesForContact(id);
				} catch (IOException ioe)
				{
					Log.error("Unexpected error: ", ioe);
					JContactListPanel.this.errorReporter.reportError(ioe, false);
				}
			}
		});
	}
	
	
	public void reloadMessagingIdentities()
		throws IOException
	{
		list.setIdentities(this.mesagingStorage.getContactIdentities(true));
		list.revalidate();
	}
	
	
	public int getNumberOfContacts()
	{
		return list.getModel().getSize();
	}
	
	
	// Null if nothing selected
	public MessagingIdentity getSelectedContact()
	{
		return this.list.getSelectedValue();
	}
	
	
	private static class ContactList
		extends JList<MessagingIdentity>
	{
		ImageIcon contactBlackIcon;
		JLabel    renderer;
		
		public ContactList()
		{
			super();
			
			this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
	        URL iconUrl = this.getClass().getClassLoader().getResource("images/contact-black.png");
	        contactBlackIcon = new ImageIcon(iconUrl);
	        
	        renderer = new JLabel();
	        renderer.setOpaque(true);
		}
		
		
		public void setIdentities(List<MessagingIdentity> identities)
		{
			List<MessagingIdentity> localIdentities = new ArrayList<MessagingIdentity>();
			localIdentities.addAll(identities);
			
			Collections.sort(
				localIdentities,
				new Comparator<MessagingIdentity>() 
				{ 
					@Override
					public int compare(MessagingIdentity o1, MessagingIdentity o2) 
					{
						return o1.getDiplayString().compareTo(o2.getDiplayString());
					}
				}
			);
			
			DefaultListModel<MessagingIdentity> newModel = new DefaultListModel<MessagingIdentity>();
			for (MessagingIdentity id : localIdentities)
			{
				newModel.addElement(id);
			}
			
			this.setModel(newModel);
		}
		
		
		@Override
		public ListCellRenderer<MessagingIdentity> getCellRenderer() 
		{
			return new ListCellRenderer<MessagingIdentity>() 
			{
				@Override
				public Component getListCellRendererComponent(JList<? extends MessagingIdentity> list,
						MessagingIdentity id, int index, boolean isSelected, boolean cellHasFocus) 
				{					
					renderer.setText(id.getDiplayString());
					renderer.setIcon(contactBlackIcon);
					
					if (isSelected) 
					{
						renderer.setBackground(list.getSelectionBackground());
					} else 
					{
						// TODO: list background issues on Linux - if used directly
						renderer.setBackground(new Color(list.getBackground().getRGB()));  
					}
					
					return renderer;
				}
			};
		}
	} // End private static class ContactList
	
} // End public class JContactListPanel
