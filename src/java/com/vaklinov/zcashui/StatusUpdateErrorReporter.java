/************************************************************************************************
 *   ____________ _   _  _____          _      _____ _    _ _______          __   _ _      _   
 *  |___  /  ____| \ | |/ ____|        | |    / ____| |  | |_   _\ \        / /  | | |    | |  
 *     / /| |__  |  \| | |     __ _ ___| |__ | |  __| |  | | | |  \ \  /\  / /_ _| | | ___| |_ 
 *    / / |  __| | . ` | |    / _` / __| '_ \| | |_ | |  | | | |   \ \/  \/ / _` | | |/ _ \ __|
 *   / /__| |____| |\  | |___| (_| \__ \ | | | |__| | |__| |_| |_   \  /\  / (_| | | |  __/ |_ 
 *  /_____|______|_| \_|\_____\__,_|___/_| |_|\_____|\____/|_____|   \/  \/ \__,_|_|_|\___|\__|
 *                                       
 * Copyright (c) 2016-2021 Zen Blockchain Foundation
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

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Reporter for periodic errors. Will later have options to filter errors etc.
 */
public class StatusUpdateErrorReporter 
{	
	private JFrame parent;
	private long lastReportedErrroTime = 0;
	
	public StatusUpdateErrorReporter(JFrame parent)
	{
		this.parent = parent;
	}
	
	public void reportError(Exception e)
	{
		reportError(e, true);
	}	
	
	public void reportError(Exception e, boolean isDueToAutomaticUpdate)
	{
		Log.error("Unexpected error: ", e);
		
		// TODO: Error logging
		long time = System.currentTimeMillis();
		
		// TODO: More complex filtering/tracking in the future
		if (isDueToAutomaticUpdate && (time - lastReportedErrroTime) < (45 * 1000))
		{
			return;
		}
		
		if (isDueToAutomaticUpdate)
		{
			lastReportedErrroTime = time;
		}
		
		String settingsDirectory = ".ZENCashSwingWalletUI";
		
		try
		{
			settingsDirectory = OSUtil.getSettingsDirectory();
		} catch (Exception e2)
		{
			Log.error("Secondary error: ", e2);
		}
		
		JOptionPane.showMessageDialog(
			parent, 
			LanguageUtil.instance().getString("status.update.error.reporter.panel.message",settingsDirectory, e.getMessage()),
			LanguageUtil.instance().getString("status.update.error.reporter.panel.title"),
			JOptionPane.ERROR_MESSAGE);
	}
}
