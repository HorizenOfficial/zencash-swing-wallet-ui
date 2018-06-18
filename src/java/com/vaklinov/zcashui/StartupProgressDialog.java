// Code was originally written by developer - https://github.com/zlatinb
// Taken from repository https://github.com/zlatinb/zcash-swing-wallet-ui under an MIT license
package com.vaklinov.zcashui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.rmi.server.ExportException;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.vaklinov.zcashui.OSUtil.OS_TYPE;
import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;


public class StartupProgressDialog extends JFrame {
    

    private static final int POLL_PERIOD = 1500;
    private static final int STARTUP_ERROR_CODE = -28;
    
    private BorderLayout borderLayout1 = new BorderLayout();
    private JLabel imageLabel = new JLabel();
    private JLabel progressLabel = new JLabel();
    private JPanel southPanel = new JPanel();
    private BorderLayout southPanelLayout = new BorderLayout();
    private JProgressBar progressBar = new JProgressBar();
    private ImageIcon imageIcon;
    
    private final ZCashClientCaller clientCaller;
    public static String commands = "";
    public static boolean runOnce = parseRunOnce();
    
    public StartupProgressDialog(ZCashClientCaller clientCaller) 
    {
        this.clientCaller = clientCaller;
        
        URL iconUrl = this.getClass().getClassLoader().getResource("images/ZEN-yellow.orange-logo.png");
        imageIcon = new ImageIcon(iconUrl);
        imageLabel.setIcon(imageIcon);
        imageLabel.setBorder(BorderFactory.createEmptyBorder(16, 40, 8, 40));
        Container contentPane = getContentPane();
        contentPane.setLayout(borderLayout1);
        southPanel.setLayout(southPanelLayout);
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        contentPane.add(imageLabel, BorderLayout.NORTH);
		JLabel zcashWalletLabel = new JLabel(LanguageUtil.instance().getString("startup.progress.dialog.label"));
		zcashWalletLabel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
		// todo - place in a panel with flow center
		JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 1));
		tempPanel.add(zcashWalletLabel);
		contentPane.add(tempPanel, BorderLayout.CENTER);
        contentPane.add(southPanel, BorderLayout.SOUTH);
        progressBar.setIndeterminate(true);
        southPanel.add(progressBar, BorderLayout.NORTH);
        progressLabel.setText(LanguageUtil.instance().getString("startup.progress.dialog.progressbar.label"));
        southPanel.add(progressLabel, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);
        
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }
    
    public void waitForStartup() throws IOException,
        InterruptedException,WalletCallException,InvocationTargetException {
        
        // special handling of Windows/Mac OS app launch
    	OS_TYPE os = OSUtil.getOSType();
        if ((os == OS_TYPE.WINDOWS) || (os == OS_TYPE.MAC_OS)) 
        {
            ProvingKeyFetcher keyFetcher = new ProvingKeyFetcher();
            keyFetcher.fetchIfMissing(this);
        }
        
        Log.info("Splash: checking if zend is already running...");
        boolean shouldStartZCashd = false;
        try {
            clientCaller.getDaemonRawRuntimeInfo();
        } catch (IOException e) { 
        	// Relying on a general exception may be unreliable
        	// may be thrown for an unexpected reason!!! - so message is checked
        	if (e.getMessage() != null && 
        		e.getMessage().toLowerCase(Locale.ROOT).contains("error: couldn't connect to server"))
        	{
        		shouldStartZCashd = true;
        	}
        }
        
        if (!shouldStartZCashd) {
        	Log.info("Splash: zend already running...");
            // What if started by hand but taking long to initialize???
//            doDispose();
//            return;
        } else
        {
        	Log.info("Splash: zend will be started...");
        }

        //Read commands from file.
        Log.info("Startup: Reading commands from file");
        readAndShowProcess();
        
        final Process daemonProcess = 
        	shouldStartZCashd ? clientCaller.startDaemon() : null;
        
        Thread.sleep(POLL_PERIOD); // just a little extra
        
        int iteration = 0;
        while(true) {
        	iteration++;
            Thread.sleep(POLL_PERIOD);
            
            JsonObject info = null;
            
            try
            {
            	info = clientCaller.getDaemonRawRuntimeInfo();
            } catch (IOException e)
            {
            	if (iteration > 4)
            	{
            		throw e;
            	} else
            	{
            		continue;
            	}
            }
            
            JsonValue code = info.get("code");
            if (code == null || (code.asInt() != STARTUP_ERROR_CODE))
                break;
            final String message = info.getString("message", "???");
            setProgressText(message);
            
        }

        // doDispose(); - will be called later by the main GUI
        
        if (daemonProcess != null) // Shutdown only if we started it
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            	Log.info("Stopping zend because we started it - now it is alive: " + 
                		           StartupProgressDialog.this.isAlive(daemonProcess));
                try 
                {
                    clientCaller.stopDaemon();
	                long start = System.currentTimeMillis();
	                
	                while (!StartupProgressDialog.this.waitFor(daemonProcess, 3000))
	                {
	                	long end = System.currentTimeMillis();
	                	Log.info("Waiting for " + ((end - start) / 1000) + " seconds for zend to exit...");
	                	
	                	if (end - start > 15 * 1000)
	                	{
                            Log.info("Stopping Daemon . . .");
	                		clientCaller.stopDaemon();
	                		daemonProcess.destroy();
	                	}
	                	
	                	if (end - start > 1 * 60 * 1000)
	                	{
	                		break;
	                	}
	                }
	            
	                if (StartupProgressDialog.this.isAlive(daemonProcess)) {
	                	Log.info("zend is still alive although we tried to stop it. " +
	                                           "Hopefully it will stop later!");
	                        //System.out.println("zend is still alive, killing forcefully");
	                        //daemonProcess.destroyForcibly();
	                    } else
	                    	Log.info("zend shut down successfully");
                } catch (Exception bad) {
                	Log.error("Couldn't stop zend!", bad);
                }
            }
        });
        
    }
    
    public void doDispose() {
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setVisible(false);
				dispose();
			}
		});
    }
    
    public void setProgressText(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Log.info("Startup progress info: {0}", text);
				progressLabel.setText(text);
			}
	     });
    }
    
    
    // Custom code - to allow JDK7 compilation.
    public boolean isAlive(Process p) 
    {
    	if (p == null)
    	{
    		return false;
    	}
    	
        try 
        {
            int val = p.exitValue();
            
            return false;
        } catch (IllegalThreadStateException itse) 
        {
            return true;
        }
    }
    
    
    // Custom code - to allow JDK7 compilation.
    public boolean waitFor(Process p, long interval)
    {
		synchronized (this) 
		{
			long startWait = System.currentTimeMillis();
			long endWait = startWait;
			do
			{
				boolean ended = !isAlive(p);
				
				if (ended)
				{
					return true; // End here
				}
				
				try
				{
					this.wait(100);
				} catch (InterruptedException ie)
				{
					// One of the rare cases where we do nothing
					Log.error("Unexpected error: ", ie);
				}
				
				endWait = System.currentTimeMillis();
			} while ((endWait - startWait) <= interval);
		}
		
		return false;
    }

    public void readAndShowProcess() {
        try {
            File commandsFile = new File(OSUtil.getSettingsDirectory() + File.separator + "commands.conf");
            if (commandsFile.exists()) {

                try (BufferedReader reader = new BufferedReader(new FileReader(commandsFile))) {

                    Log.info("Reading commands from file: " + commandsFile.getPath());
                    String _commands;
                    if((_commands = reader.readLine()) != null) {
                        commands = _commands;
                    }

                    Log.info("Done reading commands from file: " + commandsFile.getPath());

                    Log.info("Commands: " + commands + " Run: " + runOnce);

                    if (runOnce) {
                        Log.info("Emptying commands.conf");
                        try (PrintWriter pw = new PrintWriter(commandsFile)) {
                            pw.print("");
                        } catch (IOException e) {
                            Log.error("Unexpected IO Error:", e);
                        }
                    }
                } catch (IOException ex) {
                    Log.error("Unexpected IO Error:", ex);
                }

        }
        }catch(IOException e){
            Log.error("Unexpected IO Error:", e);
        }
    }

    public static boolean parseRunOnce(){
        String run_option = "";
        try{
            File commandsFile = new File(OSUtil.getSettingsDirectory() + File.separator + "commands.conf");
            try(Scanner filescanner = new Scanner(commandsFile)){

                while(filescanner.hasNextLine()){
                    String line = filescanner.nextLine();

                    if (line.startsWith("RunOnce=")){
                        run_option = line.substring(line.lastIndexOf("=") + 1);
                    }
                }

            }catch (IOException e){
                Log.error("Unexpected IO Error:", e);
            }

        }catch (FileNotFoundException e){
            Log.error("File not found Error:", e);
        }catch (IOException e){
            Log.error("Unexpected IO Error:", e);
        }
        return (run_option.trim().equals("1")) ? true : false;
    }
}
