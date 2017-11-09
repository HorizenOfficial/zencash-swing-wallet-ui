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

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.vaklinov.zcashui.CommandExecutor;
import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.OSUtil;
import com.vaklinov.zcashui.OSUtil.OS_TYPE;
import com.vaklinov.zcashui.ZCashInstallationObserver;
import com.vaklinov.zcashui.ZCashInstallationObserver.DAEMON_STATUS;
import com.vaklinov.zcashui.ZCashInstallationObserver.DaemonInfo;


/**
 * Encapsulates access to IPFS for file sharing.
 *
 * @author Ivan Vaklinov <ivan@vaklinov.com>
 */
public class IPFSWrapper
{
	private JFrame parentFrame;
	
	private Process IPFSProcess;
	
	private final Pattern ipfsUrlPattern = Pattern.compile("http://localhost:8080/ipfs/[a-zA-Z0-9]{15,100}"); 
	
	
	public IPFSWrapper(JFrame parentFrame)
	{
		this.parentFrame = parentFrame;
		this.IPFSProcess = null;
	}
	
	
	public boolean isIPFSURL(String url)
	{
		return ipfsUrlPattern.matcher(url).matches();
	}
	
	
	public String replaceIPFSHTMLLinks(String html)
	{
		Matcher m = ipfsUrlPattern.matcher(html);
		StringBuffer sb = new StringBuffer(html.length());
		while (m.find()) 
		{
		    String link = m.group(0);
		    link = "<a href=\"" + link + "\">" + link + "</a>";
		    m.appendReplacement(sb, Matcher.quoteReplacement(link));
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	
	public void followIPFSLink(URL u)
		throws IOException, InterruptedException, URISyntaxException
	{
		if (this.ensureIPFSIsRunning())
		{
			Log.info("Opening IPFS link: {0}", u.toString());
			Desktop.getDesktop().browse(u.toURI());
		} else
		{
			Log.info("NOT opening IPFS link: {0} due to IPFS not running!!!", u.toString());
		}
	}
	

	// Returns null or [name](link)
	public String shareFileViaIPFS()
		throws IOException, InterruptedException
	{
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Share file via IPFS...");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		 
		int result = fileChooser.showOpenDialog(this.parentFrame);
		 
		if (result != JFileChooser.APPROVE_OPTION) 
		{
		    return null;
		}
		
		File f = fileChooser.getSelectedFile();
		
		Cursor oldCursor = this.parentFrame.getCursor();
		try
		{
			this.parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						
			Log.info("Sharing file: {0}", f.getCanonicalPath());
			
			if (!this.ensureIPFSIsRunning())
			{
				return null;
			}
			
			String ipfs = this.getIPFSFullExecutablePath();
			
			CommandExecutor exec = new CommandExecutor(new String[]
			{
				ipfs, "add", "--quieter", f.getCanonicalPath()  
			});
			
			String strResponse = exec.execute().trim();
			
			Log.info("IPFS hash is: " + strResponse);
			
			this.parentFrame.setCursor(oldCursor);
			
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(new StringSelection("http://localhost:8080/ipfs/" + strResponse), null);
			
			JOptionPane.showMessageDialog(
				this.parentFrame, 
				"The file " + f.getName() + " has been shared successfully via IPFS.\n" +
				"It may be reached by other users via IPFS link: \n" +
				"http://localhost:8080/ipfs/" + strResponse + "\n\n" +
				"The link has been added to the messaging text box and also copied \n" +
				"to the clipboard.", 
				"File shared successfully", JOptionPane.INFORMATION_MESSAGE);
			
			return "[" + f.getName() + "](" +
			       "http://localhost:8080/ipfs/" + strResponse + ")";
		} catch (Exception wce)
		{
			Log.error("Unexpected error: ", wce);
			
			JOptionPane.showMessageDialog(
				this.parentFrame, 
				"An unexpected error occurred while sharing file via IPFS!" +
				"\n" + wce.getMessage().replace(",", ",\n"),
				"Error in importing wallet private keys...", JOptionPane.ERROR_MESSAGE);
			return null;
		} finally
		{
			this.parentFrame.setCursor(oldCursor);
		}
	}
	
	
	// true if started OK
	private boolean ensureIPFSIsRunning()
		throws IOException, InterruptedException
	{
		if (!isIPFSWrapperRunning())
		{
			if (!this.getUserConsentToStartIPFS())
			{
				return false;
			}
			
			return this.startIPFS();
		}
		
		return true;
	}
	
	
	// true if started OK
	private boolean startIPFS()
		throws IOException, InterruptedException
	{
		// TODO: warn user if executable and dir are missing!
		
		// TODO: check IPFS config and possibly initialize!
		
		CommandExecutor starter = new CommandExecutor(
		    new String[] 
		    {
		        this.getIPFSFullExecutablePath(), "daemon"
		    }
		);
		    
		this.IPFSProcess = starter.startChildProcess();
		
		// Wait 30 sec to make sure the daemon is started
		// TODO: better way to find out if it is started
		Cursor oldCursor = this.parentFrame.getCursor();
		this.parentFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		Thread.sleep(30 * 1000);
		this.parentFrame.setCursor(oldCursor);
		
        Runtime.getRuntime().addShutdownHook(new Thread() 
        {
            public void run() 
            {
            	Log.info("Stopping IPFS...");
                try 
                {
                    IPFSWrapper.this.IPFSProcess.destroy();
                } catch (Exception bad) 
                {
                	Log.error("Couldn't stop IPFS!", bad);
                }
            }
        });
        
        return true;
	}
	
	
	// Returns true if the user agrees
	private boolean getUserConsentToStartIPFS()
		throws IOException
	{
        String userDir = OSUtil.getSettingsDirectory();
        File ipfsMessageFlagFile = new File(userDir + File.separator + "ipfsInfoShown.flag");
        if (ipfsMessageFlagFile.exists())
        {
            return true;
        } 
		
		Object[] options = { "Yes", "No", "Yes and do not show this message again" };

		int option = JOptionPane.showOptionDialog(
			this.parentFrame, 
			"This operation will start an IPFS server on your PC to enable file sharing.\n"      +
			"As a result your PC will become a node in the Inter-Planetary File System that\n"   +
			"enables distributed sharing of information. Before proceeding with IPFS, please\n"  +
			"make sure you understand the full implications of this by getting familiar with\n"  +
			"the details of IPFS at this web site: https://ipfs.io/\n"                           +
			"\n"                                                                                 +
			"The IPFS server needs TCP ports 4001 and 8080 on the system for its own use!\n"     +
			"The IPFS server will be stopped automatically if you quit the ZENCash wallet. To\n" +
			"ensure that your contacts can reach the data you share, you may not quit the\n"     +
			"wallet for as long as you expect your contacts to access the data.\n"               +
			"\n"                                                                                 +
			"Do you wish to start an IPFS server on your PC?", 
			"Confirm starting an IPFS server...",
			JOptionPane.DEFAULT_OPTION, 
			JOptionPane.INFORMATION_MESSAGE,
			null, 
			options, 
			options[0]);
		
	    if (option == 2)
	    {
	    	ipfsMessageFlagFile.createNewFile();
	    }
		
	    return (option != 1);
	}
	
	
	private boolean isIPFSWrapperRunning()
		throws IOException, InterruptedException
	{
		DaemonInfo info = this.getIPFSDaemonInfo();
		
		return info.status == DAEMON_STATUS.RUNNING;
	}
	
	
	private DaemonInfo getIPFSDaemonInfo()
		throws IOException, InterruptedException
	{
		OS_TYPE os = OSUtil.getOSType();
		
		if (os == OS_TYPE.WINDOWS)
		{
			return ZCashInstallationObserver.getDaemonInfoForWindowsOS("ipfs");
		} else
		{
			return ZCashInstallationObserver.getDaemonInfoForUNIXLikeOS("ipfs");
		}
	}
	
	
	private String getIPFSFullExecutablePath()
		throws IOException
	{
		return this.getIPFSDirectory() + File.separator + this.getIPFSExecutableName();
	}
	
	
	private String getIPFSDirectory()
		throws IOException
	{
		String walletBase = OSUtil.getProgramDirectory();
		
		return walletBase + File.separator + "go-ipfs";
	}
	
	
	private String getIPFSExecutableName()
	{
		String ipfs = "ipfs";		
		OS_TYPE os = OSUtil.getOSType();
		if (os == OS_TYPE.WINDOWS)
		{
			ipfs += ".exe";
		}
		
		return ipfs;
	}
		
}
