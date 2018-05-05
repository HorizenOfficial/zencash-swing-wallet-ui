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
package com.vaklinov.zcashui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;


/**
 * Tracks important user actions and reminds the user to back up the wallet depending on
 * the content of the current user activity.
 */
public class BackupTracker
{
	private static final String TRANSACTIONS_COUNTER_FILE      = "transactionsCountSinceBackup.txt";
	private static final int    NUM_TRANSACTIONS_WIHOUT_BACKUP = 50;
	
	private JFrame parentFrame;

	private LanguageUtil langUtil;
	
	public BackupTracker(JFrame parentFrame)
	{
		this.parentFrame = parentFrame;
		this.langUtil = LanguageUtil.instance();
	}
	
	
	/**
	 * Called when the wallet balance is updated. 
	 */
	public synchronized void handleWalletBalanceUpdate(double balance)
		throws IOException
	{
		if ((balance > 0) && (!transactionsCounterFileExists()))
		{
			this.promptToDoABackup();
		}
		
		if (!transactionsCounterFileExists())
		{
			this.writeNumTransactionsSinceLastBackup(0);
		}
	}
	

	/**
	 * Called upon sending funds.
	 */
	public synchronized void handleNewTransaction()
		throws IOException
	{
		if (!transactionsCounterFileExists())
		{
			this.writeNumTransactionsSinceLastBackup(0);
		} else
		{
			int numTransactionsSinceLastBackup = this.getNumTransactionsSinceLastBackup();
			numTransactionsSinceLastBackup++;
			if (numTransactionsSinceLastBackup > NUM_TRANSACTIONS_WIHOUT_BACKUP)
			{
				this.promptToDoABackup();
			}
			this.writeNumTransactionsSinceLastBackup(numTransactionsSinceLastBackup);
		}
	}
	
	
	/**
	 * Called when a new backup is made
	 */
	public synchronized void handleBackup()
		throws IOException
	{
		this.writeNumTransactionsSinceLastBackup(0);
	}
	
	
	private void promptToDoABackup()
	{
		JOptionPane.showMessageDialog(
			this.parentFrame, 
			langUtil.getString("backup.tracker.option.pane.text"),
			langUtil.getString("backup.tracker.option.pane.title"),
			JOptionPane.INFORMATION_MESSAGE);
	}

	
	private boolean transactionsCounterFileExists() 
		throws IOException
	{
		String dir = OSUtil.getSettingsDirectory();
		File counter = new File(dir + File.separator + TRANSACTIONS_COUNTER_FILE);
		return counter.exists();
	}
	
	
	private void writeNumTransactionsSinceLastBackup(int numTransactions)
		throws IOException
	{
		String dir = OSUtil.getSettingsDirectory();
		File counter = new File(dir + File.separator + TRANSACTIONS_COUNTER_FILE);

		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(counter);
			fos.write(String.valueOf(numTransactions).getBytes("ISO-8859-1"));
		} finally
		{
			if (fos != null)
			{
				fos.close();
			}
		}
		
	}	
	
	
	public int getNumTransactionsSinceLastBackup()
		throws IOException
	{
		int countNum = 0;
		String dir = OSUtil.getSettingsDirectory();
		File counter = new File(dir + File.separator + TRANSACTIONS_COUNTER_FILE);
		
		if (counter.exists())
		{
			byte[] bytes = Util.loadFileInMemory(counter);
			String countAsString = new String(bytes, "ISO-8859-1");
			
			try
			{
				countNum = Integer.parseInt(countAsString.trim());
			} catch (NumberFormatException nfe)
			{
				// No error but only a logged message
				Log.error("Transaction counter file {0} contains invalid numeric data: {1}", 
						  TRANSACTIONS_COUNTER_FILE, countAsString);
			}
		}
				
		return countNum;
	}
}
