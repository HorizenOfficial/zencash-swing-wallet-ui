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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;


/**
 * Executes a command and returns the result.
 */
public class CommandExecutor 
{	
	private String args[];
	
	public CommandExecutor(String args[])
		throws IOException
	{
		this.args = args;
	}		
	
	
	public Process startChildProcess() 
		throws IOException 
	{
	    return Runtime.getRuntime().exec(args);
	}
	
	
	public String execute()
		throws IOException, InterruptedException
	{
		final StringBuffer result = new StringBuffer();
		
		Runtime rt = Runtime.getRuntime();
		Process proc = rt.exec(args);

		final Reader in = new InputStreamReader(new BufferedInputStream(proc.getInputStream()));

		final Reader err = new InputStreamReader(proc.getErrorStream());

		Thread inThread = new Thread(
			new Runnable() 
			{	
				@Override
				public void run()
				{
					try
					{
						int c;
						while ((c = in.read()) != -1) 
						{
						    result.append((char)c);
						}
					} catch (IOException ioe)
					{
						// TODO: log or handle the exception
					}
				}
			}
		);
		inThread.start();

		Thread errThread =  new Thread(
			new Runnable() 
			{	
			    @Override
				public void run() 
				{
			    	try 
				    {
						int c;
						while ((c = err.read()) != -1) 
						{
							result.append((char)c);
						}
					} catch (IOException ioe)
					{
						// TODO: log or handle the exception
					}
				}
			}
		);
		errThread.start();
		
		proc.waitFor();
		inThread.join();
		errThread.join();
		
		return result.toString();
	}
}
