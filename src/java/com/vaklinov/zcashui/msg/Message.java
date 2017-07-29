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
import java.util.Date;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;


/**
 * Encapsulates a zen message
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class Message
{
	public static enum DIRECTION_TYPE
	{
		SENT, RECEIVED
	};
	
	// Wire protocol fields
	private Integer version;
	private String  from;
	private String  message;
	private String  sign;
	
	// Addiitonal internal fields
	private String  transactionID;
	private Date    time;
	// TODO: direction
	
	
	public Message()
	{
		// Empty
	}
	
	
	public Message(JsonObject obj)
	{
		this.copyFromJSONObject(obj);
	}
	
	
	public Message(File f)
		throws IOException
	{
		Reader r = null;
			
		try
		{
			r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
			JsonObject obj = Json.parse(r).asObject();
			
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
	{
		this.version       = obj.getInt("version",          1);
		this.from          = obj.getString("from",          null);
		this.message       = obj.getString("message",       null);
		this.sign          = obj.getString("sign",          null);
		this.transactionID = obj.getString("transactionID", null);
		this.time          = new Date(obj.getLong("time",   0));
	}
	
	
	public JsonObject toJSONObject(boolean forWireProtocol)
	{
		JsonObject obj = new JsonObject();
		
		obj.set("version",       version);
		obj.set("from",          from);
		obj.set("message",       message);
		obj.set("sign",          sign);
		
		if (!forWireProtocol)
		{
			obj.set("transactionID", transactionID);
			obj.set("time",          this.time.getTime());
		}
		
		return obj;
	}
	
	
	public void writeToFile(File f)
		throws IOException
	{
		Writer w = null;
		
		try
		{
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
			w.write(this.toJSONObject(false).toString());
		} finally
		{
			if (w != null)
			{
				w.close();
			}
		}
	}


	public Integer getVersion() 
	{
		return version;
	}


	public void setVersion(Integer version) 
	{
		this.version = version;
	}


	public String getFrom() 
	{
		return from;
	}


	public void setFrom(String from) 
	{
		this.from = from;
	}


	public String getMessage() 
	{
		return message;
	}


	public void setMessage(String message) 
	{
		this.message = message;
	}


	public String getSign() 
	{
		return sign;
	}


	public void setSign(String sign) 
	{
		this.sign = sign;
	}


	public String getTransactionID() 
	{
		return transactionID;
	}


	public void setTransactionID(String transactionID) 
	{
		this.transactionID = transactionID;
	}


	public Date getTime() 
	{
		return time;
	}


	public void setTime(Date time) 
	{
		this.time = time;
	}
	
}
