/************************************************************************************************
 *  _________          _     ____          _           __        __    _ _      _   _   _ ___
 * |__  / ___|__ _ ___| |__ / ___|_      _(_)_ __   __ \ \      / /_ _| | | ___| |_| | | |_ _|
 *   / / |   / _` / __| '_ \\___ \ \ /\ / / | '_ \ / _` \ \ /\ / / _` | | |/ _ \ __| | | || |
 *  / /| |__| (_| \__ \ | | |___) \ V  V /| | | | | (_| |\ V  V / (_| | | |  __/ |_| |_| || |
 * /____\____\__,_|___/_| |_|____/ \_/\_/ |_|_| |_|\__, | \_/\_/ \__,_|_|_|\___|\__|\___/|___|
 *                                                 |___/
 *
 * Copyright (c) 2016-2017 Ivan Vaklinov <ivan@vaklinov.com>
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

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


public class Log 
{
	private static PrintStream fileOut;
	
	private static Set<String> oneTimeMessages = new HashSet<String>();

	static 
	{
		try
		{
			// Initialize log to a file
			String settingsDir = OSUtil.getSettingsDirectory();
			Date today = new Date();
			String logFile = settingsDir + File.separator + 
			         "ZENCashGUIWallet_" +
			         (int)(today.getYear() + 1900) + "_" +
			         (int)(today.getMonth() + 1) + "_" +
			         "debug.log";
			fileOut = new PrintStream(new FileOutputStream(logFile, true));
		}
		catch (IOException ioe)
		{
			fileOut = null;
			System.out.println("Error in initializing file logging!!!");
			ioe.printStackTrace();
		}
	}
	
	public static void debug(String message, Object ... args)
	{
		printMessage("DEBUG", message, null, args);
	}
	
	
	public static void trace(String message, Object ... args)
	{
		printMessage("TRACE", message, null, args);
	}
	
	
	public static void info(String message, Object ... args)
	{
		printMessage("INFO", message, null, args);
	}

	
	public static void warning(String message, Object ... args)
	{
		warning(message, null, args);
	}

	
	public static void warning(String message, Throwable t, Object ... args)
	{
		printMessage("WARNING", message, t, args);
	}
	
	
	public static void warningOneTime(String message, Object ... args)
	{
		printMessage(true, "WARNING", message, null, args);
	}
	

	public static void error(String message, Object ... args)
	{
		error(message, null, args);
	}

	
	public static void error(String message, Throwable t, Object ... args)
	{
		printMessage("ERROR", message, t, args);
	}

	
	private static void printMessage(String messageClass, String message,
                                     Throwable t, Object ... args)
	{
		printMessage(false, messageClass, message, t, args);
	}
	
	
	private static void printMessage(boolean oneTimeOnly, String messageClass, String message,
			                         Throwable t, Object ... args)
	{
		// TODO: Too much garbage collection
		for (int i = 0; i < args.length; i++)
		{
			if (args[i] != null)
			{
				message = message.replace("{" + i  + "}", args[i].toString());
			}
		}
		message += " ";
		
		if (oneTimeOnly) // One time messages logged only once!
		{
			if (oneTimeMessages.contains(message))
			{
				return;
			} else
			{
				oneTimeMessages.add(message);
			}
		}
		
		String prefix =
			"[" + Thread.currentThread().getName() + "] " +
		    "[" + (new Date()).toString() + "] ";
		
		messageClass = "[" + messageClass + "] ";
		
		String throwable = "";
		if (t != null)
		{
			CharArrayWriter car = new CharArrayWriter(500);
			PrintWriter pr = new PrintWriter(car);
			pr.println();  // One line extra before the exception.
			t.printStackTrace(pr);
			pr.close();
			throwable = new String(car.toCharArray()); 
		}
			
		System.out.println(prefix + messageClass + message + throwable);
		
		if (fileOut != null)
		{
			fileOut.println(prefix + messageClass + message + throwable);
			fileOut.flush();
		}
	}
}
