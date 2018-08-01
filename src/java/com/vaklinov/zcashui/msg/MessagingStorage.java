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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.WriterConfig;
import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.OSUtil;
import com.vaklinov.zcashui.Util;


/**
 * Stores the information about messages, identities etc in a dir structure. 
 * The standard directories are:
 * 
 * ~/.ZENCashSwingWalletUI/messaging - root dir
 * ~/.ZENCashSwingWalletUI/messaging/messagingoptions.json - options
 * ~/.ZENCashSwingWalletUI/messaging/ownidentity.json - own identity
 * ~/.ZENCashSwingWalletUI/messaging/ownidentity.json.bak.1 - own identity most recent backup
 * ~/.ZENCashSwingWalletUI/messaging/ownidentity.json.bak.9 - own identity oldest backup
 * ~/.ZENCashSwingWalletUI/messaging/contact_XXXX - a single contact named 0000 to 9999
 * ~/.ZENCashSwingWalletUI/messaging/contact_XXXX/identity.json - contact's identity
 * ~/.ZENCashSwingWalletUI/messaging/contact_XXXX/sent - sent messages dir
 * ~/.ZENCashSwingWalletUI/messaging/contact_XXXX/received - received messages dir
 * ~/.ZENCashSwingWalletUI/messaging/ignored_contacts - dir where ignored msg identities reside
 * ~/.ZENCashSwingWalletUI/messaging/ignored_contacts/UUID.json - single ignored identity.
 * 
 * The sent/received directories have a substructure of type:
 * sent/XXXX/message_xxx.json - where XXXX is between 0000 and 9999, xxx is between 000 and 999 
 */
public class MessagingStorage
{
	private File rootDir;
	private File ignoredContactsDir;
	
	private List<SingleContactStorage> contactsList;
	
	private List<MessagingIdentity> ignoredContacts;
	
	MessagingIdentity cachedOwnIdentity;
		
	
	public MessagingStorage()
		throws IOException
	{
		this.cachedOwnIdentity = null;
		
		this.rootDir = new File(OSUtil.getSettingsDirectory() + File.separator + "messaging");
		
		if (!rootDir.exists())
		{
			if (!rootDir.mkdirs())
			{
				throw new IOException("Could not create directory: " + rootDir.getAbsolutePath());
			}
		}
		
		this.ignoredContactsDir = new File(this.rootDir, "ignored_contacts");
		
		if (!ignoredContactsDir.exists())
		{
			if (!ignoredContactsDir.mkdirs())
			{
				throw new IOException("Could not create directory: " + ignoredContactsDir.getAbsolutePath());
			}
		}
		
		this.reloadContactListFromStorage();
		
		this.reloadIgnoredContactsFromStorage();
	}
	
	
	public void addIgnoredContact(MessagingIdentity contact)
		throws IOException
	{
		String fileName = UUID.randomUUID().toString() + ".json";
		File contactFile = new File(this.ignoredContactsDir, fileName);
		
		contact.writeToFile(contactFile);
		
		this.reloadIgnoredContactsFromStorage(); // Acceptable since it will be rare
	}
	
	
	// If a message is from an ignored contact - returns it, else null
	public MessagingIdentity getIgnoredContactForMessage(Message msg)
	{
		MessagingIdentity contact = null;
		
		for (MessagingIdentity id : this.ignoredContacts)
		{
			if (id.isAnonymous())
			{
				if (msg.isAnonymous() && (!Util.stringIsEmpty(id.getThreadID())) && 
					id.getThreadID().equals(msg.getThreadID()))
				{
					contact = id;
					break;
				}
			} else
			{
				if ((!msg.isAnonymous()) && (!Util.stringIsEmpty(id.getSenderidaddress())) && 
					id.getSenderidaddress().equals(msg.getFrom()))
				{
					contact = id;
					break;					
				}
			}
		}
		
		return contact;
	}
	
	
	public MessagingOptions getMessagingOptions()
		throws IOException
	{
		File optionsFile = new File(rootDir, "messagingoptions.json");
			
		if (!optionsFile.exists())
		{
			return new MessagingOptions();
		}
				
		// Caching is not required - rarely used
		return new MessagingOptions(optionsFile);
	}

	
	public void updateMessagingOptions(MessagingOptions newOptions)
		throws IOException
	{
		final String OPTIONS_FILE_NAME = "messagingoptions.json";
			
		File optionsFile = new File(rootDir, OPTIONS_FILE_NAME);	
		Util.renameFileForMultiVersionBackup(rootDir, OPTIONS_FILE_NAME);
		newOptions.writeToFile(optionsFile);
	}
	
	
	public MessagingIdentity getOwnIdentity()
		throws IOException
	{
		if (this.cachedOwnIdentity != null)
		{
			return this.cachedOwnIdentity;
		}
		
		File identityFile = new File(rootDir, "ownidentity.json");
		
		if (!identityFile.exists())
		{
			return null;
		}
			
		this.cachedOwnIdentity = new MessagingIdentity(identityFile);
		return this.cachedOwnIdentity;
	}
		
		
	public void updateOwnIdentity(MessagingIdentity newIdentity)
		throws IOException
	{
		final String OWN_IDENTITY = "ownidentity.json";
		
		File identityFile = new File(rootDir, OWN_IDENTITY);	
			
		Util.renameFileForMultiVersionBackup(rootDir, OWN_IDENTITY);
		
		newIdentity.writeToFile(identityFile);
		
		this.cachedOwnIdentity = newIdentity;
	}
	
	
	public List<MessagingIdentity> getContactIdentities(boolean includeAnonymous)
		throws IOException
	{
		List<MessagingIdentity> identities = new ArrayList<MessagingIdentity>();
		
		for (SingleContactStorage contact : this.contactsList)
		{
			MessagingIdentity id = contact.getIdentity();
			if ((!id.isAnonymous()) || includeAnonymous)
			{
				identities.add(id);
			}
		}
		
		return identities;
	}
	
	
	public MessagingIdentity getContactIdentityForSenderIDAddress(String senderIDAddress)
		throws IOException
	{
		List<MessagingIdentity> allIdentities = this.getContactIdentities(false);
		
		MessagingIdentity id = null;
		
		for (MessagingIdentity tempID : allIdentities)
		{
			if ((!tempID.isAnonymous()) && tempID.getSenderidaddress().equals(senderIDAddress))
			{
				id = tempID;
			}
		}
		
		return id;		
	}
	
	
	public void updateContactIdentityForSenderIDAddress(String senderIDAddress, MessagingIdentity newID)
		throws IOException
	{
		for (SingleContactStorage contact : this.contactsList)
		{
			MessagingIdentity tempID = contact.getIdentity();
			
			if ((!tempID.isAnonymous()) && tempID.getSenderidaddress().equals(senderIDAddress))
			{
				tempID.copyFromJSONObject(newID.toJSONObject(false));
				contact.updateIdentity(tempID);
			}
		}			
	}
	
	
	public void updateGroupContactIdentityForSendReceiveAddress(String sendReceiveAddress, MessagingIdentity newID)
		throws IOException
	{
		for (SingleContactStorage contact : this.contactsList)
		{
			MessagingIdentity tempID = contact.getIdentity();
			
			if ((tempID.isGroup()) && tempID.getSendreceiveaddress().equals(sendReceiveAddress))
			{
				tempID.copyFromJSONObject(newID.toJSONObject(false));
				contact.updateIdentity(tempID);
			}
		}			
	}
	
	
	/**
	 * Checks if a particular sender's ID is ignored. This makes sense only if the
	 * current contact is a group. The ID may be an anonymous sender UUID or a
	 * normal from address.
	 * 
	 * @param senderID
	 * @param groupID
	 * @return true if a particular sender's ID is ignored.
	 * 
	 * @throws IOException
	 */
	public boolean isSenderIdentityIgnoredForGroup(String senderID, MessagingIdentity groupID)
		throws IOException
	{
		for (SingleContactStorage contact : this.contactsList)
		{
			MessagingIdentity tempID = contact.getIdentity();
			
			if ((tempID.isGroup()) && tempID.isIdenticalTo(groupID))
			{
				return contact.isGroupSenderIDIgnored(senderID);
			}
		}			

		return false;
	}
	
	
	/**
	 * Adds a new ignored sender ID. This makes sense only if the
	 * current contact is a group. The ID may be an anonymous sender UUID or a
	 * normal from address.
	 * 
	 * @param senderID to add
	 */
	public void addIgnoredSenderIdentityForGroup(String senderID, MessagingIdentity groupID)
		throws IOException
	{
		for (SingleContactStorage contact : this.contactsList)
		{
			MessagingIdentity tempID = contact.getIdentity();
			
			if ((tempID.isGroup()) && tempID.isIdenticalTo(groupID))
			{
				contact.addGroupIgnoredSenderID(senderID);
				return;
			}
		}	
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
		
		int contactDirIndex = contactDirs.length;
		String contactDirName;
		
		// We need to make sure the dir does not exist. If it does maybe users were removed before etc
		// so we increment!
		do
		{
			contactDirName = String.valueOf(contactDirIndex++);
			while (contactDirName.length() < 4)
			{
				contactDirName = "0" + contactDirName;
			}
			
			contactDirName = "contact_" + contactDirName;
		} while (new File(this.rootDir, contactDirName).exists());
		
		SingleContactStorage contactStorage = new SingleContactStorage(new File(this.rootDir, contactDirName));
		contactStorage.updateIdentity(identity);
		this.contactsList.add(contactStorage);
	}
	
	
	/**
	 * Creates and stores permanently an anonymous contact identity for a user who is yet unknown.
	 * 
	 * @param senderIDAdderss the known sender ID address
	 * 
	 * @return the identity created and stored
	 * 
	 * @throws IOException
	 */
	public MessagingIdentity createAndStoreUnknownContactIdentity(String senderIDAdderss)
		throws IOException
	{
		MessagingIdentity newID = new MessagingIdentity();
		
		String nickName = null;
		naming_loop:
		for (int i = 1; i <= 1000; i++) // TODO: more reliable naming scheme
		{
			nickName = "Unknown_" + i;
			for (MessagingIdentity existignID : this.getContactIdentities(true))
			{
				if (nickName.equalsIgnoreCase(existignID.getNickname()))
				{
					continue naming_loop;
				}
			}
			break naming_loop;
		}
		
		newID.setNickname(nickName);
		newID.setFirstname(senderIDAdderss.substring(0, 10) + "...");
		newID.setSenderidaddress(senderIDAdderss);
		newID.setSendreceiveaddress(""); // Empty - unknown
		
		// All fields need to be filled
		newID.setMiddlename("");
		newID.setSurname("");
		newID.setEmail("");
		newID.setStreetaddress("");
		newID.setFacebook("");
		newID.setTwitter("");
		
		this.addContactIdentity(newID);
		
		return newID;
	}
	
	
	/**
	 * Finds a messaging identity that corresponds to a particular thread id. It may be a normal
	 * identity with anonymous messages sent to it (outgoing case) or an anonymous identity 
	 * (incoming case).
	 * 
	 * @param threadID
	 * 
	 * @return a messaging identity that corresponds to a particular thread id
	 * 
	 * @throws IOException
	 */
	public MessagingIdentity findAnonymousOrNormalContactIdentityByThreadID(String threadID)
		throws IOException
	{
		List<MessagingIdentity> allIdentities = this.getContactIdentities(true);
		
		MessagingIdentity id = null;
		
		for (MessagingIdentity tempID : allIdentities)
		{
			if ((!Util.stringIsEmpty(tempID.getThreadID())) && tempID.getThreadID().equals(threadID))
			{
				id = tempID;
			}
		}
		
		return id;	
	}
	
	
	public MessagingIdentity createAndStoreAnonumousContactIdentity(String threadID, String returnAddress)
		throws IOException
	{
		MessagingIdentity newID = new MessagingIdentity();
		newID.setAnonymous(true);
		newID.setThreadID(threadID);
			
		String nickName = null;
		naming_loop:
		for (int i = 1; i <= 1000; i++) // TODO: more reliable naming scheme
		{
			nickName = "Anonymous_" + i;
			for (MessagingIdentity existignID : this.getContactIdentities(true))
			{
				if (nickName.equalsIgnoreCase(existignID.getNickname()))
				{
					continue naming_loop;
				}
			}
			break naming_loop;
		}
			
		newID.setNickname(nickName);
		newID.setFirstname(threadID.substring(0, 10) + "...");
		newID.setSendreceiveaddress(returnAddress);
		newID.setSenderidaddress("");
			
		// All fields need to be filled
		newID.setMiddlename("");
		newID.setSurname("");
		newID.setEmail("");
		newID.setStreetaddress("");
		newID.setFacebook("");
		newID.setTwitter("");
		
		this.addContactIdentity(newID);
			
		return newID;
	}
	
	
	public void updateAnonumousContactIdentityForThreadID(String threadID, MessagingIdentity newID)
		throws IOException
	{
		for (SingleContactStorage contact : this.contactsList)
		{
			MessagingIdentity tempID = contact.getIdentity();
			
			if ((tempID.isAnonymous()) && tempID.getThreadID().equals(threadID))
			{
				tempID.copyFromJSONObject(newID.toJSONObject(false));
				contact.updateIdentity(tempID);
				break;
			}
		}			
	}
	

	/**
	 * Returns all known messages for a certain contact in ascending date order. 
	 * If identity not found etc. throws an exception
	 * 
	 * @param contact
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
					    contact.toJSONObject(false).toString());
			throw new IOException("Could not find messaging identity in the contact list " +
					              contact.toJSONObject(false).toString());
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
	
	
	public void writeNewReceivedMessageForContact(MessagingIdentity contact, Message msg)
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
		contactStorage.receivedMessages.writeNewMessage(msg);
	}
	
	
	// Deletes a certain contact and reloads the contact list
	public void deleteContact(MessagingIdentity contact)
		throws IOException
	{
		for (SingleContactStorage scs : this.contactsList)
		{
			if (scs.getIdentity().isIdenticalTo(contact))
			{
				Util.deleteDirectory(scs.getRootDir());
				this.reloadContactListFromStorage();
				break;
			}
		}
	}
	
	
	private void reloadContactListFromStorage()
		throws IOException
	{
			this.contactsList = new ArrayList<SingleContactStorage>();
			
			File contactDirs[] = this.rootDir.listFiles(new FileFilter() 
			{	
				@Override
				public boolean accept(File pathname) 
				{
					return pathname.isDirectory() && pathname.getName().matches("contact_[0-9]{4}");
				}
			});
			
			for (File dir : contactDirs)
			{
				this.contactsList.add(new SingleContactStorage(dir));
		    }
	}
		
		
	private void reloadIgnoredContactsFromStorage()
		throws IOException
	{
			this.ignoredContacts = new ArrayList<MessagingIdentity>();
			
			File ignoredContacts[] = this.ignoredContactsDir.listFiles(new FileFilter() 
			{	
				@Override
				public boolean accept(File pathname) 
				{
					return pathname.isFile() && pathname.getName().endsWith(".json");
				}
			});
			
			for (File contactFile : ignoredContacts)
			{
				this.ignoredContacts.add(new MessagingIdentity(contactFile));
			}
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	// Stores the details of a single contact
	// Root dir may be like:
	// ~/.ZENCashSwingWalletUI/messaging/contact_XXXX
	static class SingleContactStorage
	{
		final String IGNORED_GROUP_IDS = "ignored_group_ids.json";
		
		private File rootDir;
		
		private SentOrReceivedMessagesStore sentMessages;
		private SentOrReceivedMessagesStore receivedMessages;
		
		private MessagingIdentity cachedIdentity;
		
		private Set<String> cachedIgnoredGroupSenderIDs;
		
		
		public SingleContactStorage(File rootDir)
			throws IOException
		{
			this.cachedIdentity = null;
			this.cachedIgnoredGroupSenderIDs = null;
			
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
			if (this.cachedIdentity != null)
			{
				return this.cachedIdentity;
			}
			
			File identityFile = new File(rootDir, "identity.json");
			
			this.cachedIdentity = new MessagingIdentity(identityFile);
			
			return this.cachedIdentity;
		}
		
		
		public void updateIdentity(MessagingIdentity newIdentity)
			throws IOException
		{
			final String IDENTITY = "identity.json";
			File identityFile = new File(rootDir, IDENTITY);	
			
			Util.renameFileForMultiVersionBackup(rootDir, IDENTITY);
			
			newIdentity.writeToFile(identityFile);
			
			this.cachedIdentity = newIdentity;
		}
		
		
		/**
		 * Checks if a particular sender's ID is ignored. This makes sense only if the
		 * current contact is a group. The ID may be an anonymous sender UUID or a
		 * normal from address.
		 * 
		 * @param senderID
		 * 
		 * @return true if a particular sender's ID is ignored
		 */
		public boolean isGroupSenderIDIgnored(String senderID)
			throws IOException
		{
			this.preloadCachedIgnoredGroupSenderIDs();
			boolean ignored = this.cachedIgnoredGroupSenderIDs.contains(senderID);
			return ignored;
		}
		

		/**
		 * Adds a new ignored sender ID. This makes sense only if the
		 * current contact is a group. The ID may be an anonymous sender UUID or a
		 * normal from address.
		 * 
		 * @param senderID to add
		 */
		public void addGroupIgnoredSenderID(String senderID)
			throws IOException
		{
			this.preloadCachedIgnoredGroupSenderIDs();
			
			File ignoredIDsFile = new File(rootDir, IGNORED_GROUP_IDS);	
			
			Util.renameFileForMultiVersionBackup(rootDir, IGNORED_GROUP_IDS);
			
			this.cachedIgnoredGroupSenderIDs.add(senderID);
			
			JsonArray ar = new JsonArray();
			for (String id : this.cachedIgnoredGroupSenderIDs)
			{
				ar.add(id);
			}
			
			OutputStream os = null;
			try
			{
				os = new BufferedOutputStream(new FileOutputStream(ignoredIDsFile));
				OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
				ar.writeTo(osw, WriterConfig.PRETTY_PRINT);
				osw.flush();
			} finally 
			{
				if (os != null)
				{
					os.close();
				}
			}
		}
		
		
		private void preloadCachedIgnoredGroupSenderIDs()
			throws IOException
		{
			if (this.cachedIgnoredGroupSenderIDs == null)
			{
				this.cachedIgnoredGroupSenderIDs = new HashSet<String>();
				
				File ignoredIDsFile = new File(rootDir, IGNORED_GROUP_IDS);	
				
				if (!ignoredIDsFile.exists())
				{
					return;
				}
				
				InputStream is = null;
				try
				{
					is = new BufferedInputStream(new FileInputStream(ignoredIDsFile));
					InputStreamReader isr = new InputStreamReader(is, "UTF-8");
					JsonArray ar = Json.parse(isr).asArray(); // TODO: repackage to checked exception
					
					for (int i = 0; i < ar.size(); i++)
					{
						String val = ar.get(i).toString();
						
						if (val.startsWith("\"")) // Strip the string of the quotes
						{
							val = val.substring(1);
						}
						
						if (val.endsWith("\""))
						{
							val = val.substring(0, val.length() - 1);
						}
						
						this.cachedIgnoredGroupSenderIDs.add(val);
					}
				} finally
				{
					if (is != null)
					{
						is.close();
					}
				}
			}
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

		
		public File getRootDir()
		{
			return this.rootDir;
		}
	}
	
	
	// Stores messages of one type - sent/received for one contact
	// Root directory may be like:
	// ~/.ZENCashSwingWalletUI/messaging/contact_XXXX/sent
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
			// TODO: This could be avoided - cache current number of files
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
				// TODO: This could be avoided - cache current number of files
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
