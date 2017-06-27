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

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;

import com.eclipsesource.json.JsonObject;

/**
 * Utilities - generally reusable across classes.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class Util
{
	// Compares two string arrays (two dimensional).
	public static boolean arraysAreDifferent(String ar1[][], String ar2[][])
	{
		if (ar1 == null)
		{
			if (ar2 != null)
			{
				return true;
			}
		} else if (ar2 == null)
		{
			return true;
		}
		
		if (ar1.length != ar2.length)
		{
			return true;
		}
		
		for (int i = 0; i < ar1.length; i++)
		{
			if (ar1[i].length != ar2[i].length)
			{
				return true;
			}
			
			for (int j = 0; j < ar1[i].length; j++)
			{
				String s1 = ar1[i][j];
				String s2 = ar2[i][j];
				
				if (s1 == null)
				{
					if (s2 != null)
					{
						return true;
					}
				} else if (s2 == null)
				{
					return true;
				} else
				{
					if (!s1.equals(s2))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	
	// Turns a 1.0.7+ error message to a an old JSOn style message
	// info - new style error message
	public static JsonObject getJsonErrorMessage(String info)
	    throws IOException
	{
    	JsonObject jInfo = new JsonObject();
    	
    	// Error message here comes from ZCash 1.0.7+ and is like:
    	//zcash-cli getinfo
    	//error code: -28
    	//error message:
    	//Loading block index...
    	LineNumberReader lnr = new LineNumberReader(new StringReader(info));
    	int errCode =  Integer.parseInt(lnr.readLine().substring(11).trim());
    	jInfo.set("code", errCode);
    	lnr.readLine();
    	jInfo.set("message", lnr.readLine().trim());
    	
    	return jInfo;
	}
	
}
