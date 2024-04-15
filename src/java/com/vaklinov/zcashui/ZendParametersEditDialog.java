/************************************************************************************************
 *   ____________ _   _  _____          _      _____ _    _ _______          __   _ _      _   
 *  |___  /  ____| \ | |/ ____|        | |    / ____| |  | |_   _\ \        / /  | | |    | |  
 *     / /| |__  |  \| | |     __ _ ___| |__ | |  __| |  | | | |  \ \  /\  / /_ _| | | ___| |_ 
 *    / / |  __| | . ` | |    / _` / __| '_ \| | |_ | |  | | | |   \ \/  \/ / _` | | |/ _ \ __|
 *   / /__| |____| |\  | |___| (_| \__ \ | | | |__| | |__| |_| |_   \  /\  / (_| | | |  __/ |_ 
 *  /_____|______|_| \_|\_____\__,_|___/_| |_|\_____|\____/|_____|   \/  \/ \__,_|_|_|\___|\__|
 *  
 * Copyright (c) 2023 Horizen Foundation                                                                                           
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

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


/**
 * Dialog showing the zend startup options and allowing them to be edited.
 */
public class ZendParametersEditDialog
	extends JDialog
{
	protected JFrame parentFrame;
	protected StatusUpdateErrorReporter errorReporter;
	
	protected JLabel infoLabel;
	protected JPanel buttonPanel;
	
	protected JTextArea optionsEditArea;
	
	private LanguageUtil langUtil;
	
	public ZendParametersEditDialog(JFrame parentFrame, StatusUpdateErrorReporter errorReporter)
		throws IOException
	{
		this.parentFrame   = parentFrame;
		this.errorReporter = errorReporter;
		
		this.langUtil = LanguageUtil.instance();
		
		this.setTitle(langUtil.getString("zend.cmd.params.dialog.title"));
		this.setModal(true);
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			
		this.getContentPane().setLayout(new BorderLayout(0, 0));
			
		JPanel tempPanel = new JPanel(new BorderLayout(0, 0));
		tempPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		infoLabel = new JLabel(langUtil.getString("zend.cmd.params.dialog.info"));
		infoLabel.addMouseListener(new MouseAdapter() 
		{
			@Override
			public void mouseClicked(MouseEvent e) 
			{
				try
				{
					Desktop.getDesktop().browse(new URI(
						"https://github.com/HorizenOfficial/zencash-swing-wallet-ui/blob/feature/zend-cmd-options/docs/zend.pdf"));
				}
				catch(Exception ex)
				{
					errorReporter.reportError(ex);
				}
			}
		});
	    tempPanel.add(infoLabel, BorderLayout.CENTER);
		this.getContentPane().add(tempPanel, BorderLayout.NORTH);
			
		JPanel detailsPanel = new JPanel();
		detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
		
		// Load the content of the current options file
		List<String> zendParams = Util.loadZendParameters(true);
		while (zendParams.size() < 6)
		{
			zendParams.add("");
		}
		
		StringBuilder editContent = new StringBuilder();
		for (String param : zendParams)
		{
			editContent.append(param);
			editContent.append("\n");
		}
		
		this.optionsEditArea = new JTextArea(editContent.toString());
		JScrollPane pane = new JScrollPane(this.optionsEditArea);
		detailsPanel.add(pane);
		
		detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		this.getContentPane().add(detailsPanel, BorderLayout.CENTER);

		buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 3));
		JButton closeButon = new JButton(langUtil.getString("zend.cmd.params.dialog.close.button"));
		buttonPanel.add(closeButon);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		closeButon.addActionListener(new ActionListener()
		{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					ZendParametersEditDialog.this.setVisible(false);
					ZendParametersEditDialog.this.dispose();
				}
		});
		
		JButton saveButon = new JButton(langUtil.getString("zend.cmd.params.dialog.save.button"));
		buttonPanel.add(saveButon);
		saveButon.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					// Process the logic of saving the data
					if (ZendParametersEditDialog.this.saveZendParameters())
					{
						ZendParametersEditDialog.this.setVisible(false);
						ZendParametersEditDialog.this.dispose();
					}
				} catch (Exception ex)
				{
					Log.error("Unexpected error in editing own messaging identity!", ex);
					ZendParametersEditDialog.this.errorReporter.reportError(ex, false);
				}
			}
		});

		this.pack();
		this.setLocation(100, 100);
		this.setLocationRelativeTo(parentFrame);
		
		Dimension currentSize = this.getSize();
		if ((currentSize.width > 1100) || (currentSize.height > 500))
		{
			this.setSize(new Dimension(Math.min(currentSize.width, 1100), Math.min(currentSize.height, 500)));
	        this.validate();
			this.repaint();
		}
	}

	
	/**
	 * Handles the saving of the options possibly edited by the user.
	 * 
	 * @return true if successufl, false if cancelled
	 * 
	 * @throws IOException
	 */
	private boolean saveZendParameters()
		throws IOException
	{
		// Get the new zend parameters from the text area
		List<String> optionsToSet = new ArrayList<String>();
		// Read current text - meaningful lines
		LineNumberReader r = null;
		try
		{
			r = new LineNumberReader(new StringReader(this.optionsEditArea.getText()));
			String line;
			while ((line = r.readLine()) != null)
			{
				line = line.trim();
				
				if ((line.length() <= 0) || line.startsWith("#"))
				{
					continue;
				}
				optionsToSet.add(line);
			}
			
		} finally
		{
			if (r != null)
			{
				r.close();
			}
		}

		// Check for multiple options on the same line
		for (String option : optionsToSet)
		{
			if (option.contains(" ") &&
			    ((option.indexOf("=") != option.lastIndexOf("=")) ||
			     (option.indexOf("-") != option.lastIndexOf("-"))))
			{
				// Warn on possible multiple options per line
			    int answer = JOptionPane.showConfirmDialog(  
		            ZendParametersEditDialog.this,
					langUtil.getString("zend.cmd.params.dialog.double.option.warning.text", option),
					langUtil.getString("zend.cmd.params.dialog.double.option.warning.title"),
					JOptionPane.YES_NO_OPTION);
				if (answer == JOptionPane.NO_OPTION)
				{
			        return false;
				}
			}
		}		
		
		// Load the existing file into individual lines (no exceptions).
		List<String> zendFullFileLines = Util.loadZendParameters(false);
		
		// Double iteration to replace the options that have changed
		for (String optionToSet : optionsToSet)
		{
			boolean bFound = false;
			
			all_file_options_loop:
			for (int oo = 0; oo < zendFullFileLines.size(); oo++)
			{
				String currentOptionToCheck = zendFullFileLines.get(oo);
				
				if ((currentOptionToCheck.trim().length() <= 0) ||
					 currentOptionToCheck.trim().startsWith("#"))
				{
					continue all_file_options_loop;
				}
				
				if (getParamName(optionToSet).equals(getParamName(currentOptionToCheck)) &&
					(!isMultiOccurrenceOption(currentOptionToCheck)))
				{
					if ((!optionToSet.equals(currentOptionToCheck)))
					{
						Log.info("Saving user-modified zend option at line {0}: {1}", oo, optionToSet);
						zendFullFileLines.set(oo, optionToSet);
					}

					bFound = true;
				}
				
				if (optionToSet.equals(currentOptionToCheck) && isMultiOccurrenceOption(currentOptionToCheck))
				{
					bFound = true; // Found but not replaced
				}
			}
			
			if (!bFound)
			{
				Log.info("Adding user-created zend option at line {0}: {1}", zendFullFileLines.size(), optionToSet);
				zendFullFileLines.add(optionToSet);
			}
		}
		
		// Handle options that may be deleted - another double iteration
		all_file_options_loop2:
		for (int oo = 0; oo < zendFullFileLines.size(); oo++)
		{
			String currentOptionToCheck = zendFullFileLines.get(oo);
				
			if ((currentOptionToCheck.trim().length() <= 0) ||
				 currentOptionToCheck.trim().startsWith("#"))
			{
				continue all_file_options_loop2;
			}
				
			boolean bRemove = true;
			
			for (String optionToSet : optionsToSet)
			{
				if (getParamName(optionToSet).equals(getParamName(currentOptionToCheck)) &&
					(!isMultiOccurrenceOption(currentOptionToCheck)))
				{
					bRemove = false;
				}
				
				if (optionToSet.equals(currentOptionToCheck) && isMultiOccurrenceOption(currentOptionToCheck))
				{
					bRemove = false;
				}
			}
			
			if (bRemove)
			{
				Log.info("Removing user-deleted zend option at line {0}: {1}", oo, currentOptionToCheck);
				zendFullFileLines.set(oo, "");
			}
		}

		
		// Finally save the file
    	String settingsDir = OSUtil.getSettingsDirectory();
    	File dir = new File(settingsDir);
		File zendOptionsFile = new File(dir, "zend-cmd-options.conf");

		Util.renameFileForMultiVersionBackup(dir, zendOptionsFile.getName());
		
		PrintWriter configOut = null;
		try
		{
			configOut =  new PrintWriter(zendOptionsFile, "UTF-8");
			int emptyLineCount = 0;
			for (String line : zendFullFileLines)
			{
				if (line.trim().length() > 0)
				{
					emptyLineCount = 0;
				} else
				{
					emptyLineCount++;
				}
				
				if (emptyLineCount <= 2)
				{
					configOut.println(line);
				}
			}
		} finally
		{
			Log.info("Successfully created new version of file: {0}", zendOptionsFile.getCanonicalPath());
			configOut.close();
		}
		
		// Inform the user of successful save - and the need to restart
		JOptionPane.showMessageDialog(
			ZendParametersEditDialog.this, 
			langUtil.getString("zend.cmd.params.dialog.success.text", zendOptionsFile.getCanonicalPath()),
			langUtil.getString("zend.cmd.params.dialog.success.title"),
            JOptionPane.INFORMATION_MESSAGE);
		
		return true;
	}
	
	
	/**
	 * Distinguishes between single and multi-occurrece options for the purpose
	 * of replacing them. 
	 * 
	 * @param fullParam fill config line
	 * 
	 * @return
	 */
	private boolean isMultiOccurrenceOption(String fullParam)
	{
		String paramName = this.getParamName(fullParam);
		 
		return "addnode".equals(paramName); // For now only addnode seems to be a multi-occur option
	}
	
	
	/**
	 * Utility: given a full option specification, provides the option name only
	 * 
	 * @param fullParam must not be null
	 * 
	 * @return param name (as best as can be identified)
	 */
	private String getParamName(String fullParam)
	{
		String param = fullParam.trim();
		while (param.startsWith("-"))
		{
			param = param.substring(1);
		}
		
		if (param.contains("="))
		{
			param = param.substring(0, param.indexOf("="));
		}
		
		return param;
	}
} 
