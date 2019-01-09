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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;


// Manages the load/store operations for address labels
public class LabelStorage
{
		private static final String LABELS_FILE_NAME = "wallet.dat.labels";
		
		// Address -> label
		private Properties labels;
		
		public LabelStorage()
			throws IOException
		{
			this.labels = new Properties();
			this.loadLabels();
		}
		
		
		public synchronized String getLabel(String address)
		{
			String label = "";
			
			if (this.labels.containsKey(address))
			{
				label = this.labels.getProperty(address);
			}
			
			return label;
		}
		
		
		public synchronized void setLabel(String address, String label)
			throws IOException
		{
			if (!this.getLabel(address).equals(label))
			{
				this.labels.setProperty(address, label);
				this.storeLabels();
			}
		}
		
		
		private synchronized void loadLabels()
			throws IOException
		{
			File labelsFile = new File(OSUtil.getSettingsDirectory() + File.separator + LABELS_FILE_NAME);
			if (!labelsFile.exists())
			{
				Log.info("Wallet labels file does not exist: {0}", labelsFile.getCanonicalPath());
				return;
			}
			
		    InputStream in = null;
			try
			{
				in = new BufferedInputStream(new FileInputStream(labelsFile));
				this.labels.load(in);
			} finally
			{
				if (in != null)
				{
					in.close();
				}
			}
		}
		
		
		private synchronized void storeLabels()
			throws IOException
		{
			Util.renameFileForMultiVersionBackup(new File(OSUtil.getSettingsDirectory()), LABELS_FILE_NAME);
			
			File newLabelsFile = new File(OSUtil.getSettingsDirectory() + File.separator + LABELS_FILE_NAME);
			OutputStream out = null;
			try
			{
				out = new BufferedOutputStream(new FileOutputStream(newLabelsFile));
				this.labels.store(out, "Horizen GUI wallet address labels");
			} finally
			{
				if (out != null)
				{
					out.close();
				}
			}
		}
}
