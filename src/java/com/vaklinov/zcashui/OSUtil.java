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


import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;


/**
 * Utilities - may be OS dependent.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class OSUtil
{

	// Returns the name of the zcashd server - may vary depending on the OS.
	public static String getZCashd()
	{
		String zcashd = "zend";
		
		if (SystemUtils.IS_OS_WINDOWS)
		{
			zcashd += ".exe";
		}
		
		return zcashd;
	}
	
	
	// Returns the name of the zen-cli tool - may vary depending on the OS.
	public static String getZCashCli()
	{
		String zcashcli = "zen-cli";
		
		if (SystemUtils.IS_OS_WINDOWS)
		{
			zcashcli += ".exe";
		}
		
		return zcashcli;
	}


	// Returns the directory that the wallet program was started from
	public static String getProgramDirectory()
		throws IOException
	{
		// TODO: this way of finding the dir is JAR name dependent - tricky, may not work
		// if program is repackaged as different JAR!
		final String JAR_NAME = "ZENCashSwingWalletUI.jar";
		String cp = SystemUtils.JAVA_CLASS_PATH;
		if ((cp != null) && (cp.indexOf(File.pathSeparator) == -1) &&
			(cp.endsWith(JAR_NAME)))
		{
			File pd = new File(cp.substring(0, cp.length() - JAR_NAME.length()));

			if (pd.exists() && pd.isDirectory())
			{
				return pd.getCanonicalPath();
			}
		}
		
		// Try with a full class-path, now containing more libraries
		// This too is very deployment specific
		if (cp.indexOf(File.pathSeparator) != -1)
		{
			String cp2 = cp;
			if (cp2.endsWith(File.pathSeparator))
			{
				cp2 = cp2.substring(0, cp2.length() - 1);
			}
			
			if (cp2.startsWith(File.pathSeparator))
			{
				cp2 = cp2.substring(1);
			}
			
			final String CP_JARS = JAR_NAME + File.pathSeparator + "bitcoinj-core-0.14.5.jar" +
					                          File.pathSeparator + "sqlite-jdbc-3.21.0.jar";
			if (cp2.endsWith(CP_JARS))
			{
				String cpStart = cp2.substring(0, cp2.length() - CP_JARS.length());
				if (cpStart.endsWith(File.separator))
				{
					cpStart = cpStart.substring(0, cpStart.length() - 1);
				}
				int startIndex = cpStart.lastIndexOf(File.pathSeparator);
				if (startIndex < 0)
				{
					startIndex = 0;
				}
				
				if (cpStart.length() > startIndex)
				{
					File pd = new File(cpStart.substring(startIndex));
					return pd.getCanonicalPath();
				}
			}			
		}

		// Current dir of the running JVM (expected)
		String userDir = SystemUtils.USER_DIR;
		if (userDir != null)
		{
			File ud = new File(userDir);

			if (ud.exists() && ud.isDirectory())
			{
				return ud.getCanonicalPath();
			}
		}

		// TODO: tests and more options

		return new File(".").getCanonicalPath();
	}
	
	
	public static File getUserHomeDirectory()
		throws IOException
	{
        return new File(System.getProperty("user.home"));
	}


	public static String getBlockchainDirectory()
		throws IOException
	{
		if (SystemUtils.IS_OS_MAC)
		{
			return new File(System.getProperty("user.home") + "/Library/Application Support/Zen").getCanonicalPath();
		} else if (SystemUtils.IS_OS_WINDOWS)
		{
			return new File(System.getenv("APPDATA") + "\\Zen").getCanonicalPath();
		} else
		{
			return new File(System.getProperty("user.home") + "/.zen").getCanonicalPath();
		}
	}


	// Directory with program settings to store as well as logging
	public static String getSettingsDirectory()
		throws IOException
	{
	    File userHome = SystemUtils.getUserHome();
	    File dir;

	    if (SystemUtils.IS_OS_MAC)
	    {
	        dir = new File(userHome, "Library/Application Support/ZENCashSwingWalletUI");
	    } else if (SystemUtils.IS_OS_WINDOWS)
		{
			dir = new File(System.getenv("LOCALAPPDATA") + "\\ZENCashSwingWalletUI");
		} else
	    {
	        dir = new File(userHome.getCanonicalPath() + File.separator + ".ZENCashSwingWalletUI");
	    }
	    
		if (!dir.exists())
		{
			if (!dir.mkdirs())
			{
				Log.warning("Could not create settings directory: " + dir.getCanonicalPath());
			}
		}

		return dir.getCanonicalPath();
	}


	public static String getSystemInfo()
		throws IOException, InterruptedException
	{

		if (SystemUtils.IS_OS_MAC)
		{
			CommandExecutor uname = new CommandExecutor(new String[] { "uname", "-sr" });
		    return uname.execute() + "; " + 
		           System.getProperty("os.name") + " " + System.getProperty("os.version");
		} else if (SystemUtils.IS_OS_WINDOWS)
		{
			// TODO: More detailed Windows information
			return SystemUtils.OS_NAME;
		} else
		{
			CommandExecutor uname = new CommandExecutor(new String[] { "uname", "-srv" });
		    return uname.execute();
		}
	}


	// Can be used to find zend/zen-cli if it is not found in the same place as the wallet JAR
	// Null if not found
	public static File findZCashCommand(String command)
		throws IOException
	{
	    File f;
	    
	    // Try with system property zcash.location.dir - may be specified by caller
	    String ZCashLocationDir = System.getProperty("zen.location.dir");
	    if ((ZCashLocationDir != null) && (ZCashLocationDir.trim().length() > 0))
	    {
	        f = new File(ZCashLocationDir + File.separator + command);
	        if (f.exists() && f.isFile())
	        {
	            return f.getCanonicalFile();
	        }
	    }
	    

	    if (SystemUtils.IS_OS_UNIX)
	    {
	    	// The following search directories apply to UNIX-like systems only
			final String dirs[] = new String[]
			{
				"/usr/bin/", // Typical Ubuntu
				"/bin/",
				"/usr/local/bin/",
				"/usr/local/zen/bin/",
				"/usr/lib/zen/bin/",
				"/opt/local/bin/",
				"/opt/local/zen/bin/",
				"/opt/zen/bin/"
			};
	
			for (String d : dirs)
			{
				f = new File(d + command);
				if (f.exists())
				{
					return f;
				}
			}
			
	    } else if (SystemUtils.IS_OS_WINDOWS)
	    {
	    	// A probable Windows directory is a ZCash dir in Program Files
	    	String programFiles = System.getenv("PROGRAMFILES");
	    	if ((programFiles != null) && (!programFiles.isEmpty()))
	    	{
	    		File pf = new File(programFiles);
	    		if (pf.exists() && pf.isDirectory())
	    		{
	    			File ZDir = new File(pf, "Zen");
	    			if (ZDir.exists() && ZDir.isDirectory())
	    			{
	    				File cf = new File(ZDir, command);
	    				if (cf.exists() && cf.isFile())
	    				{
	    					return cf;
	    				}
	    			}
	    		}
	    	}
	    }
		
		// Try in the current directory
		f = new File("." + File.separator + command);
		if (f.exists() && f.isFile())
		{
			return f.getCanonicalFile();
		}
			

		// TODO: Try to find it with which/PATH
		
		return null;
	}
}
