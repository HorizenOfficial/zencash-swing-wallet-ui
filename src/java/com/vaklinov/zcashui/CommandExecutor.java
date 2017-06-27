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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;


/**
 * Executes a command and retruns the result.
 * 
 * @author Ivan Vaklinov <ivan@vaklinov.com>
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

		final Reader in = new InputStreamReader(proc.getInputStream());

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
