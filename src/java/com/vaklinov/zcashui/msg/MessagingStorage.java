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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.OSUtil;


/**
 * Stores the information about messages, identities etc in a dir structure. 
 * The standard directories are:
 * 
 * ~/.ZENCashSwingWalletUI/messaging - root dir
 * ~/.ZENCashSwingWalletUI/messaging/ownidentity.json - own identity
 * ~/.ZENCashSwingWalletUI/messaging/ownidentity.json.bak.1 - own identity most recent backup
 * ~/.ZENCashSwingWalletUI/messaging/ownidentity.json.bak.9 - own identity oldest backup
 * ~/.ZENCashSwingWalletUI/messaging/contact_XXXX - a single contact named 0000 to 9999
 * ~/.ZENCashSwingWalletUI/messaging/contact_XXXX/identity.json - contact's identity
 * ~/.ZENCashSwingWalletUI/messaging/contact_XXXX/sent - sent messages dir
 * ~/.ZENCashSwingWalletUI/messaging/contact_XXXX/received - received messages dir
 * 
 * The sent/received directories have a substructure of type:
 * sent/XXXX/message_xxx.json - where XXXX is between 0000 and 9999, xxx is between 000 and 999 
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class MessagingStorage
{
	private File rootDir;
	
	private List<SingleContactStorage> contactsList = new ArrayList<SingleContactStorage>();
		
	
	public MessagingStorage()
		throws IOException
	{
		this.rootDir = new File(OSUtil.getSettingsDirectory() + File.separator + "messaging");
		
		if (!rootDir.exists())
		{
			if (!rootDir.mkdirs())
			{
				throw new IOException("Could not create directory: " + rootDir.getAbsolutePath());
			}
		}
		
		File contactDirs[] = rootDir.listFiles(new FileFilter() 
		{	
			@Override
			public boolean accept(File pathname) 
			{
				return pathname.isDirectory() && pathname.getName().matches("contact_[0-9]{4}");
			}
		});
		
		for (File dir : contactDirs)
		{
			contactsList.add(new SingleContactStorage(dir));
		}
	}
	
	
	public MessagingIdentity getOwnIdentity()
		throws IOException
	{
		// TODO: cache in memory
		File identityFile = new File(rootDir, "ownidentity.json");
		
		if (!identityFile.exists())
		{
			return null;
		}
			
		return new MessagingIdentity(identityFile);
	}
		
		
	public void updateOwnIdentity(MessagingIdentity newIdentity)
		throws IOException
	{
		File identityFile = new File(rootDir, "ownidentity.json");	
			
		// TODO: save up to 9 backups etc.
		newIdentity.writeToFile(identityFile);
	}
	
	
	public List<MessagingIdentity> getContactIdentities()
		throws IOException
	{
		List<MessagingIdentity> identities = new ArrayList<MessagingIdentity>();
		
		for (SingleContactStorage contact : this.contactsList)
		{
			identities.add(contact.getIdentity());
		}
		
		return identities;
	}
	
	
	public void addContactIdentity(MessagingIdentity identity)
		throws IOException
	{
		File contactDirs[] = this.rootDir.listFiles(new FileFilter() 
		{	
			@Override
			public boolean accept(File pathname) 
			{
				return pathname.isDirectory() && pathname.getName().matches("contact_[0-9]{4}");
			}
		});
		
		String contactDirName = String.valueOf(contactDirs.length);
		while (contactDirName.length() < 4)
		{
			contactDirName = "0" + contactDirName;
		}
		
		contactDirName = "contact_" + contactDirName;
		
		SingleContactStorage contactStorage = new SingleContactStorage(new File(this.rootDir, contactDirName));
		contactStorage.updateIdentity(identity);
		this.contactsList.add(contactStorage);
	}
	
	
	/**
	 * Returns all known messages for a certain contact in ascending date order. 
	 * If identity not found etc. thorws an exception
	 * 
	 * @param conact
	 * 
	 * @return all known messages for a certain contact in ascending date order.
	 */
	public List<Message> getAllMessagesForContact(MessagingIdentity contact)
		throws IOException
	{
		// Find the contact
		SingleContactStorage contactStorage = null;
		for (SingleContactStorage scs : this.contactsList)
		{
			if (scs.getIdentity().isIdenticalTo(contact))
			{
				contactStorage = scs;
			}
		}
		
		List<Message> messages = new ArrayList<Message>();
		
		// Should never happen but ...
		if (contactStorage == null)
		{
			Log.warning("Could not find messaging identity in the contact list {0}", 
					    contact.toJSONObject().toString());
			throw new IOException("Could not find messaging identity in the contact list " +
					              contact.toJSONObject().toString());
		}
		
		messages.addAll(contactStorage.getAllReceivedMessages());
		messages.addAll(contactStorage.getAllSentMessages());
		
		// Finally sort them
		Collections.sort(messages,
			new Comparator<Message>() 
			{
				@Override
				public int compare(Message o1, Message o2) 
				{
					return o1.getTime().compareTo(o2.getTime());
				}
			}
		);
		
		return messages;
	}
	
	
	public void writeNewSentMessageForContact(MessagingIdentity contact, Message msg)
		throws IOException
	{
		// Find the contact
		SingleContactStorage contactStorage = null;
		for (SingleContactStorage scs : this.contactsList)
		{
			if (scs.getIdentity().isIdenticalTo(contact))
			{
				contactStorage = scs;
			}
		}

		contactStorage.sentMessages.writeNewMessage(msg);
	}
	
	
	// TODO more Get all messages by identity or by T adderss we shall see what is needed
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	// Stores the details of a single contact
	static class SingleContactStorage
	{
		private File rootDir;
		
		private SentOrReceivedMessagesStore sentMessages;
		private SentOrReceivedMessagesStore receivedMessages;
		
		
		public SingleContactStorage(File rootDir)
			throws IOException
		{
			this.rootDir = rootDir;
			
			if (!rootDir.exists())
			{
				if (!rootDir.mkdirs())
				{
					throw new IOException("Could not create directory: " + rootDir.getAbsolutePath());
				}
			}
		
			this.sentMessages     = new SentOrReceivedMessagesStore(new File(rootDir, "sent"));
			this.receivedMessages = new SentOrReceivedMessagesStore(new File(rootDir, "received"));
		}
		
		
		public MessagingIdentity getIdentity()
			throws IOException
		{
			// TODO: todo cache identity in memory
			File identityFile = new File(rootDir, "identity.json");
			
			return new MessagingIdentity(identityFile);
		}
		
		
		public void updateIdentity(MessagingIdentity newIdentity)
			throws IOException
		{
			File identityFile = new File(rootDir, "identity.json");	
			
			// TODO: save up to 9 backups etc.
			newIdentity.writeToFile(identityFile);
		}
		
		
		public List<Message> getAllSentMessages()
		    throws IOException
		{
			return this.sentMessages.getAllMessages();
		}
		
		
		public void writeNewSentMessage(Message msg)
			throws IOException
		{
			this.sentMessages.writeNewMessage(msg);
		}

		
		public List<Message> getAllReceivedMessages()
		    throws IOException
		{
			return this.receivedMessages.getAllMessages();
		}

		
		public void writeNewReceivedMessage(Message msg)
			throws IOException
		{
			this.receivedMessages.writeNewMessage(msg);
		}

	}
	
	
	// Stores messages of one type - sent/received for one contact
	static class SentOrReceivedMessagesStore
	{
		private File rootDir;
		private int currentOutputDirForWrite;
		
		public SentOrReceivedMessagesStore(File rootDir)
		    throws IOException
		{
			this.rootDir = rootDir;
			
			if (!rootDir.exists())
			{
				if (!rootDir.mkdirs())
				{
					throw new IOException("Could not create directory: " + rootDir.getAbsolutePath());
				}
			}
			
			// Use the dir with the highest number created so far
			this.currentOutputDirForWrite = 0;
			File currentDirs[] = rootDir.listFiles(new FileFilter() 
			{	
				@Override
				public boolean accept(File pathname) 
				{
					return pathname.isDirectory() && pathname.getName().matches("[0-9]{4}");
				}
			});
			
			for (File dir : currentDirs)
			{
				if (Integer.parseInt(dir.getName()) > this.currentOutputDirForWrite)
				{
					this.currentOutputDirForWrite = Integer.parseInt(dir.getName());
				}
			}
		}
		
		
		// Returns all messages in ascending time order
		public List<Message> getAllMessages()
			throws IOException
		{
			List<Message> allMessages = new ArrayList<Message>();
			
			File currentDirs[] = this.rootDir.listFiles(new FileFilter() 
			{	
				@Override
				public boolean accept(File pathname) 
				{
					return pathname.isDirectory() && pathname.getName().matches("[0-9]{4}");
				}
			});
			
			for (File dir : currentDirs)
			{
				this.collectMessagesFromDir(dir, allMessages);
			}
			
			Collections.sort(allMessages,
				new Comparator<Message>() 
				{
					public int compare(Message m1, Message m2)
					{
						return m1.getTime().compareTo(m2.getTime());
					}
				}
			);
			
			return allMessages;
		}
		
		
		public void writeNewMessage(Message msg)
			throws IOException
		{
			File dir = this.getCurrentDirForWrite();
			
			// See how many message files currently exist (000 -> 999)
			File messages[] = dir.listFiles(new FileFilter() 
			{	
				@Override
				public boolean accept(File pathname) 
				{
					return pathname.isFile();
				}
			});
			
			String name = String.valueOf(messages.length);
			while (name.length() < 3)
			{
				name = "0" + name;
			}
			name = "message_" + name + ".json";
			
			msg.writeToFile(new File(dir, name));
		}
		
		
		private void collectMessagesFromDir(File dir, List<Message> messages)
			throws IOException
		{
			File messageFiles[] = dir.listFiles(new FileFilter() 
			{	
				@Override
				public boolean accept(File pathname) 
				{
					return pathname.isFile();
				}
			});

			for (File f : messageFiles)
			{
				messages.add(new Message(f));
			}
		}
		
		
		private File getCurrentDirForWrite()
			throws IOException
		{
			String name = String.valueOf(this.currentOutputDirForWrite);
			while (name.length() < 4)
			{
				name = "0" + name;
			}
			
			File dir = new File(this.rootDir, name);
			
			if (!dir.exists())
			{
				if (!dir.mkdirs())
				{
					throw new IOException("Could not create directory: " + dir.getAbsolutePath());
				}
			} else
			{
				// Make sure there are not too many messages
				// TODO: This could be avoided
				File messages[] = dir.listFiles(new FileFilter() 
				{	
					@Override
					public boolean accept(File pathname) 
					{
						return pathname.isFile();
					}
				});
				
				if (messages.length > 999)
				{
					this.currentOutputDirForWrite++;
					
					return getCurrentDirForWrite(); // Recurse 
				}
			}
			
			return dir;
		}
		
	} // End static class SentOrReceivedMessagesStore
}
