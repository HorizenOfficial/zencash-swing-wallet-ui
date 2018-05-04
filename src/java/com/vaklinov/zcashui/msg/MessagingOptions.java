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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import com.vaklinov.zcashui.Util;


/**
 * Encapsulates the messaging options that may be set.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class MessagingOptions
{
	private boolean automaticallyAddUsersIfNotExplicitlyImported;
	private double  amountToSend;
	private double  transactionFee;
	
	
	public MessagingOptions()
	{
		// Default values set if not loade etc.
		this.automaticallyAddUsersIfNotExplicitlyImported = true;
		this.amountToSend = this.transactionFee = 0.0001d;
	}
	
	
	public MessagingOptions(JsonObject obj)
		 throws IOException
	{
		this.copyFromJSONObject(obj);
	}
	
	
	public MessagingOptions(File f)
		throws IOException
	{
		Reader r = null;
			
		try
		{
			r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			JsonObject obj = Util.parseJsonObject(r);
			
			this.copyFromJSONObject(obj);
		} finally
		{
			if (r != null)
			{
				r.close();
			}
		}
	}


	public void copyFromJSONObject(JsonObject obj)
		throws IOException
	{
		// Mandatory fields!
		this.automaticallyAddUsersIfNotExplicitlyImported = 
			obj.getBoolean("automaticallyaddusersifnotexplicitlyimported", true);
		this.amountToSend   = obj.getDouble("amounttosend",   0.0001d);
		this.transactionFee = obj.getDouble("transactionfee", 0.0001d);
	}
	
	
	public JsonObject toJSONObject()
	{
		JsonObject obj = new JsonObject();
		
		obj.set("automaticallyaddusersifnotexplicitlyimported",
				this.automaticallyAddUsersIfNotExplicitlyImported);
		obj.set("amounttosend",	this.amountToSend);
		obj.set("transactionfee",	this.transactionFee);
		
		return obj;
	}
	
	
	public void writeToFile(File f)
		throws IOException
	{
		Writer w = null;
		
		try
		{
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
			w.write(this.toJSONObject().toString(WriterConfig.PRETTY_PRINT));
		} finally
		{
			if (w != null)
			{
				w.close();
			}
		}
	}


	public boolean isAutomaticallyAddUsersIfNotExplicitlyImported() 
	{
		return automaticallyAddUsersIfNotExplicitlyImported;
	}


	public void setAutomaticallyAddUsersIfNotExplicitlyImported(boolean automaticallyAddUsersIfNotExplicitlyImported) 
	{
		this.automaticallyAddUsersIfNotExplicitlyImported = automaticallyAddUsersIfNotExplicitlyImported;
	}


	public double getAmountToSend() 
	{
		return amountToSend;
	}


	public void setAmountToSend(double amountToSend) 
	{
		this.amountToSend = amountToSend;
	}


	public double getTransactionFee() 
	{
		return transactionFee;
	}


	public void setTransactionFee(double transactionFee) 
	{
		this.transactionFee = transactionFee;
	}
		
}
