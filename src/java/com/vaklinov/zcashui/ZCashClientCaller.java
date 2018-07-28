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


import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.ParseException;
import com.eclipsesource.json.WriterConfig;
import com.vaklinov.zcashui.OSUtil.OS_TYPE;


/**
 * Calls zcash-cli
 */
public class ZCashClientCaller
{
	public static class WalletBalance
	{
		public double transparentBalance;
		public double privateBalance;
		public double totalBalance;

		public double transparentUnconfirmedBalance;
		public double privateUnconfirmedBalance;
		public double totalUnconfirmedBalance;
	}


	public static class NetworkAndBlockchainInfo
	{
		public int numConnections;
		public Date lastBlockDate;
	}


	public static class WalletCallException
		extends Exception
	{
		public WalletCallException(String message)
		{
			super(message);
		}

		public WalletCallException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}


	// ZCash client program and daemon
	private File zcashcli, zcashd;
	
	// Table caching the wallet transaction times - to speed up performance
	// TXID -> UNIX time as string
	private Map<String, String> transactionTimes = Collections.synchronizedMap(
		new HashMap<String, String>());
	private long lastTransactionTimesAccess = System.currentTimeMillis();

	// Table caching the wallet transaction confirmations - to speed up performance
	// TXID -> confirmations as string
	private Map<String, String> transactionConfirmations = Collections.synchronizedMap(
		new HashMap<String, String>());
	private long lastTransactionConfirmationsAccess = System.currentTimeMillis();

	

	public ZCashClientCaller(String installDir)
		throws IOException
	{
		// Detect daemon and client tools installation
		File dir = new File(installDir);
	    zcashcli = new File(dir, OSUtil.getZCashCli());

		if (!zcashcli.exists())
		{
			zcashcli = OSUtil.findZCashCommand(OSUtil.getZCashCli());
		}

		if ((zcashcli == null) || (!zcashcli.exists()))
		{
			throw new IOException(
				"The ZENCash installation directory " + installDir + " needs to contain " +
				"the command line utilities zend and zen-cli. zen-cli is missing!");
		}
		
		zcashd = new File(dir, OSUtil.getZCashd());
		if (!zcashd.exists())
		{
		    zcashd = OSUtil.findZCashCommand(OSUtil.getZCashd());
		}
		
		if (zcashd == null || (!zcashd.exists()))
		{
		    throw new IOException(
		    	"The ZENCash command line utility " + zcashcli.getCanonicalPath() + 
		    	" was found, but zend was not found!");
		}
	}

	
	public synchronized Process startDaemon() 
		throws IOException, InterruptedException 
	{
		String exportDir = OSUtil.getUserHomeDirectory().getCanonicalPath();
		
	    CommandExecutor starter = new CommandExecutor(
	        new String[] 
	        {
	        	zcashd.getCanonicalPath(), 
	        	"-exportdir=" + wrapStringParameter(exportDir)
	        });
	    
	    return starter.startChildProcess();
	}
	
	
	public /*synchronized*/ void stopDaemon() 
		throws IOException,InterruptedException 
	{
	    CommandExecutor stopper = new CommandExecutor(
	            new String[] { zcashcli.getCanonicalPath(), "stop" });
	    
	    String result = stopper.execute();
	    Log.info("Stop command issued: " + result);
	}
	

	public synchronized JsonObject getDaemonRawRuntimeInfo() 
		throws IOException, InterruptedException, WalletCallException 
	{
	    CommandExecutor infoGetter = new CommandExecutor(
	            new String[] { zcashcli.getCanonicalPath(), "getinfo"} );
	    String info = infoGetter.execute();
	    
	    if (info.trim().toLowerCase(Locale.ROOT).startsWith("error: couldn't connect to server"))
	    {
	    	throw new IOException(info.trim());
	    }
	    
	    if (info.trim().toLowerCase(Locale.ROOT).startsWith("error: "))
	    {
	        info = info.substring(7);
	        
		    try 
		    {
		        return Json.parse(info).asObject();
		    } catch (ParseException pe)
		    {
		    	Log.error("unexpected daemon info: " + info);
		        throw new IOException(pe);
		    }
	    } else if (info.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
	    {
	    	return Util.getJsonErrorMessage(info);
	    } else
	    {
		    try 
		    {
		        return Json.parse(info).asObject();
		    } catch (ParseException pe)
		    {
		    	Log.info("unexpected daemon info: " + info);
		        throw new IOException(pe);
		    }
	    }
	}

	
	public synchronized WalletBalance getWalletInfo()
		throws WalletCallException, IOException, InterruptedException
	{
		WalletBalance balance = new WalletBalance();

		JsonObject objResponse = this.executeCommandAndGetJsonObject("z_gettotalbalance", null);

    	balance.transparentBalance = Double.valueOf(objResponse.getString("transparent", "-1"));
    	balance.privateBalance     = Double.valueOf(objResponse.getString("private", "-1"));
    	balance.totalBalance       = Double.valueOf(objResponse.getString("total", "-1"));

        objResponse = this.executeCommandAndGetJsonObject("z_gettotalbalance", "0");

    	balance.transparentUnconfirmedBalance = Double.valueOf(objResponse.getString("transparent", "-1"));
    	balance.privateUnconfirmedBalance     = Double.valueOf(objResponse.getString("private", "-1"));
    	balance.totalUnconfirmedBalance       = Double.valueOf(objResponse.getString("total", "-1"));

		return balance;
	}


	public synchronized String[][] getWalletPublicTransactions()
		throws WalletCallException, IOException, InterruptedException
	{
		String notListed = "\u26D4";
		
		OS_TYPE os = OSUtil.getOSType();
		if (os == OS_TYPE.WINDOWS)
		{
			notListed = " \u25B6";
		}
		
	    JsonArray jsonTransactions = executeCommandAndGetJsonArray(
	    	"listtransactions", wrapStringParameter(""), "300");
	    String strTransactions[][] = new String[jsonTransactions.size()][];
	    for (int i = 0; i < jsonTransactions.size(); i++)
	    {
	    	strTransactions[i] = new String[7];
	    	JsonObject trans = jsonTransactions.get(i).asObject();

	    	// Needs to be the same as in getWalletZReceivedTransactions()
	    	// TODO: someday refactor to use object containers
	    	strTransactions[i][0] = "\u2606T (Public)";
	    	strTransactions[i][1] = trans.getString("category", "ERROR!");
	    	strTransactions[i][2] = trans.get("confirmations").toString();
	    	strTransactions[i][3] = trans.get("amount").toString();
	    	strTransactions[i][4] = trans.get("time").toString();
	    	strTransactions[i][5] = trans.getString("address", notListed + " (Z address not listed by wallet!)");
	    	strTransactions[i][6] = trans.get("txid").toString();

	    }

	    return strTransactions;
	}


	public synchronized String[] getWalletZAddresses()
		throws WalletCallException, IOException, InterruptedException
	{
		JsonArray jsonAddresses = executeCommandAndGetJsonArray("z_listaddresses", null);
		String strAddresses[] = new String[jsonAddresses.size()];
		for (int i = 0; i < jsonAddresses.size(); i++)
		{
		    strAddresses[i] = jsonAddresses.get(i).asString();
		}

	    return strAddresses;
	}


	public synchronized String[][] getWalletZReceivedTransactions()
		throws WalletCallException, IOException, InterruptedException
	{
		String[] zAddresses = this.getWalletZAddresses();

		List<String[]> zReceivedTransactions = new ArrayList<String[]>();

		for (String zAddress : zAddresses)
		{
		    JsonArray jsonTransactions = executeCommandAndGetJsonArray(
		    	"z_listreceivedbyaddress", wrapStringParameter(zAddress), "0");
		    for (int i = 0; i < jsonTransactions.size(); i++)
		    {
		    	String[] currentTransaction = new String[7];
		    	JsonObject trans = jsonTransactions.get(i).asObject();

		    	String txID = trans.getString("txid", "ERROR!");
		    	// Needs to be the same as in getWalletPublicTransactions()
		    	// TODO: someday refactor to use object containers
		    	currentTransaction[0] = "\u2605Z (Private)";
		    	currentTransaction[1] = "receive";
		    	
		    	// Transaction confirmations cached - cleared every 10 min
		    	if ((System.currentTimeMillis() - this.lastTransactionConfirmationsAccess) > (10 * 60 * 1000))
		    	{
		    		this.lastTransactionConfirmationsAccess = System.currentTimeMillis();
		    		this.transactionConfirmations.clear();
		    	}
		    	String confirmations = this.transactionConfirmations.get(txID);
		    	if ((confirmations == null) || confirmations.equals("0"))
		    	{
		    		currentTransaction[2] = this.getWalletTransactionConfirmations(txID);
		    		this.transactionConfirmations.put(txID, currentTransaction[2]);
		    	} else
		    	{
		    		currentTransaction[2] = confirmations;
		    	}
		    	
		    	currentTransaction[3] = trans.get("amount").toString();
		    	
		    	// Transaction time is cached - cleared every 10 min
		    	if ((System.currentTimeMillis() - this.lastTransactionTimesAccess) > (10 * 60 * 1000))
		    	{
		    		this.lastTransactionTimesAccess = System.currentTimeMillis();
		    		this.transactionTimes.clear();
		    	}
		    	String time = this.transactionTimes.get(txID);
		    	if ((time == null) || (time.equals("-1")))
		    	{
		    		currentTransaction[4] = this.getWalletTransactionTime(txID);
		    		this.transactionTimes.put(txID, currentTransaction[4]);
		    	} else
		    	{
		    		currentTransaction[4] = time;
		    	}
		    	
		    	currentTransaction[5] = zAddress;
		    	currentTransaction[6] = trans.get("txid").toString();

		    	zReceivedTransactions.add(currentTransaction);
		    }
		}

		return zReceivedTransactions.toArray(new String[0][]);
	}

	
	public synchronized JsonObject[] getTransactionMessagingDataForZaddress(String ZAddress)
		throws WalletCallException, IOException, InterruptedException
	{
	    JsonArray jsonTransactions = executeCommandAndGetJsonArray(
		    	"z_listreceivedbyaddress", wrapStringParameter(ZAddress), "0");
	    List<JsonObject> transactions = new ArrayList<JsonObject>();
		for (int i = 0; i < jsonTransactions.size(); i++)
		{
		   	JsonObject trans = jsonTransactions.get(i).asObject();
	    	transactions.add(trans);
	    }
		
		return transactions.toArray(new JsonObject[0]);
	}
	

	// ./src/zcash-cli listunspent only returns T addresses it seems
	public synchronized String[] getWalletPublicAddressesWithUnspentOutputs()
		throws WalletCallException, IOException, InterruptedException
	{
		JsonArray jsonUnspentOutputs = executeCommandAndGetJsonArray("listunspent", "0");

		Set<String> addresses = new HashSet<>();
	    for (int i = 0; i < jsonUnspentOutputs.size(); i++)
	    {
	    	JsonObject outp = jsonUnspentOutputs.get(i).asObject();
	    	addresses.add(outp.getString("address", "ERROR!"));
	    }

	    return addresses.toArray(new String[0]);
     }


	// ./zcash-cli listreceivedbyaddress 0 true
	public synchronized String[] getWalletAllPublicAddresses()
		throws WalletCallException, IOException, InterruptedException
	{
		JsonArray jsonReceivedOutputs = executeCommandAndGetJsonArray("listreceivedbyaddress", "0", "true");

		Set<String> addresses = new HashSet<>();
		for (int i = 0; i < jsonReceivedOutputs.size(); i++)
		{
		   	JsonObject outp = jsonReceivedOutputs.get(i).asObject();
		   	addresses.add(outp.getString("address", "ERROR!"));
		}

		return addresses.toArray(new String[0]);
    }

	
	public synchronized Map<String, String> getRawTransactionDetails(String txID)
		throws WalletCallException, IOException, InterruptedException
	{
		JsonObject jsonTransaction = this.executeCommandAndGetJsonObject(
			"gettransaction", wrapStringParameter(txID));

		Map<String, String> map = new HashMap<String, String>();

		for (String name : jsonTransaction.names())
		{
			this.decomposeJSONValue(name, jsonTransaction.get(name), map);
		}
				
		return map;
	}
	
    public synchronized String getMemoField(String acc, String txID)
		throws WalletCallException, IOException, InterruptedException
	{
		JsonArray jsonTransactions = this.executeCommandAndGetJsonArray(
			"z_listreceivedbyaddress", wrapStringParameter(acc));
			
        for (int i = 0; i < jsonTransactions.size(); i++)
        {
            if (jsonTransactions.get(i).asObject().getString("txid",  "ERROR!").equals(txID))
            {
            	if (jsonTransactions.get(i).asObject().get("memo") == null)
            	{
            		return null;
            	}
            	
                String memoHex = jsonTransactions.get(i).asObject().getString("memo", "ERROR!");
                String decodedMemo = Util.decodeHexMemo(memoHex);
                
                // Return only if not null - sometimes multiple incoming transactions have the same ID
                // if we have loopback send etc.
                if (decodedMemo != null)
                {
                	return decodedMemo;
                }
            }
        }

        return null;
	}
	
    
	public synchronized void keypoolRefill(int count)
		throws WalletCallException, IOException, InterruptedException
	{
		String result = this.executeCommandAndGetSingleStringResponse(
			"keypoolrefill", String.valueOf(count));
	}
    
	
	public synchronized String getRawTransaction(String txID)
		throws WalletCallException, IOException, InterruptedException
	{
		JsonObject jsonTransaction = this.executeCommandAndGetJsonObject(
			"gettransaction", wrapStringParameter(txID));

		return jsonTransaction.toString(WriterConfig.PRETTY_PRINT);
	}


	// return UNIX time as string
	public synchronized String getWalletTransactionTime(String txID)
		throws WalletCallException, IOException, InterruptedException
	{
		JsonObject jsonTransaction = this.executeCommandAndGetJsonObject(
			"gettransaction", wrapStringParameter(txID));

		return String.valueOf(jsonTransaction.getLong("time", -1));
	}
	
	
	public synchronized String getWalletTransactionConfirmations(String txID)
		throws WalletCallException, IOException, InterruptedException
	{
		JsonObject jsonTransaction = this.executeCommandAndGetJsonObject(
			"gettransaction", wrapStringParameter(txID));

		return jsonTransaction.get("confirmations").toString();
	}
	
	
	// Checks if a certain T address is a watch-only address or is otherwise invalid.
	public synchronized boolean isWatchOnlyOrInvalidAddress(String address)
		throws WalletCallException, IOException, InterruptedException
	{
		JsonObject response = this.executeCommandAndGetJsonValue("validateaddress", wrapStringParameter(address)).asObject();

		if (response.getBoolean("isvalid", false))
		{
			return response.getBoolean("iswatchonly", true);
		}
		
		return true;
	}
	

	// Returns confirmed balance only!
	public synchronized String getBalanceForAddress(String address)
		throws WalletCallException, IOException, InterruptedException
	{
	    JsonValue response = this.executeCommandAndGetJsonValue("z_getbalance", wrapStringParameter(address));

		return String.valueOf(response.toString());
	}


	public synchronized String getUnconfirmedBalanceForAddress(String address)
		throws WalletCallException, IOException, InterruptedException
	{
	    JsonValue response = this.executeCommandAndGetJsonValue("z_getbalance", wrapStringParameter(address), "0");

		return String.valueOf(response.toString());
	}


	public synchronized String createNewAddress(boolean isZAddress)
		throws WalletCallException, IOException, InterruptedException
	{
	    String strResponse = this.executeCommandAndGetSingleStringResponse((isZAddress ? "z_" : "") + "getnewaddress");

		return strResponse.trim();
	}


	// Returns OPID - this method is a bit old and could be improved, however it is known to work and would better not
	// be changed unless there is a bug.
	public synchronized String sendCash(String from, String to, String amount, String memo, String transactionFee)
		throws WalletCallException, IOException, InterruptedException
	{
		Log.info("Starting operation send-cash. Parameters are: from address: {0}, to address: {1}, " + 
	             "amount: {2}, memo: {3}, transaction fee: {4}",
				 from, to, amount, memo, transactionFee);
		
		StringBuilder hexMemo = new StringBuilder();
		for (byte c : memo.getBytes("UTF-8"))
		{
			String hexChar = Integer.toHexString((int)c);
			if (hexChar.length() < 2)
			{
				hexChar = "0" + hexChar;
			}
			hexMemo.append(hexChar);
		}

		JsonObject toArgument = new JsonObject();
		toArgument.set("address", to);
		if (hexMemo.length() >= 2)
		{
			toArgument.set("memo", hexMemo.toString());
		}

		// The JSON Builder has a problem with double values that have no fractional part
		// it serializes them as integers that ZCash does not accept. So we do a replacement
		// TODO: find a better/cleaner way to format the amount
		toArgument.set("amount", "\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF");

		JsonArray toMany = new JsonArray();
		toMany.add(toArgument);
		
		String amountPattern = "\"amount\":\"\uFFFF\uFFFF\uFFFF\uFFFF\uFFFF\"";
		// Make sure our replacement hack never leads to a mess up
		String toManyBeforeReplace = toMany.toString();
		int firstIndex = toManyBeforeReplace.indexOf(amountPattern);
		int lastIndex = toManyBeforeReplace.lastIndexOf(amountPattern);
		if ((firstIndex == -1) || (firstIndex != lastIndex))
		{
			throw new WalletCallException("Error in forming z_sendmany command: " + toManyBeforeReplace);
		}

		DecimalFormatSymbols decSymbols = new DecimalFormatSymbols(Locale.ROOT);
		
		// Properly format the transaction fee as a number
		if ((transactionFee == null) || (transactionFee.trim().length() <= 0))
		{
			transactionFee = "0.0001"; // Default value
		} else
		{
			transactionFee = new DecimalFormat(
				"########0.00######", decSymbols).format(Double.valueOf(transactionFee));
		}

	    // This replacement is a hack to make sure the JSON object amount has double format 0.00 etc.
	    // TODO: find a better way to format the amount
		String toManyArrayStr =	toMany.toString().replace(
		    amountPattern,
			"\"amount\":" + new DecimalFormat("########0.00######", decSymbols).format(Double.valueOf(amount)));
		
		String[] sendCashParameters = new String[]
	    {
		    this.zcashcli.getCanonicalPath(), "z_sendmany", wrapStringParameter(from),
		    wrapStringParameter(toManyArrayStr),
		    // Default min confirmations for the input transactions is 1
		    "1",
		    // transaction fee
		    transactionFee
		};
		
		// Safeguard to make sure the monetary amount does not differ after formatting
		BigDecimal bdAmout = new BigDecimal(amount);
		JsonArray toManyVerificationArr = Json.parse(toManyArrayStr).asArray();
		BigDecimal bdFinalAmount = 
			new BigDecimal(toManyVerificationArr.get(0).asObject().getDouble("amount", -1));
		BigDecimal difference = bdAmout.subtract(bdFinalAmount).abs();
		if (difference.compareTo(new BigDecimal("0.000000015")) >= 0)
		{
			throw new WalletCallException("Error in forming z_sendmany command: Amount differs after formatting: " + 
		                                  amount + " | " + toManyArrayStr);
		}

		Log.info("The following send command will be issued: " +
                sendCashParameters[0] + " " + sendCashParameters[1] + " " +
                sendCashParameters[2] + " " + sendCashParameters[3] + " " +
                sendCashParameters[4] + " " + sendCashParameters[5] + ".");
		
		// Create caller to send cash
	    CommandExecutor caller = new CommandExecutor(sendCashParameters);
	    String strResponse = caller.execute();

		if (strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error:") ||
			strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
		{
		  	throw new WalletCallException("Error response from wallet: " + strResponse);
		}

		Log.info("Sent cash with the following command: " +
                sendCashParameters[0] + " " + sendCashParameters[1] + " " +
                sendCashParameters[2] + " " + sendCashParameters[3] + " " +
                sendCashParameters[4] + " " + sendCashParameters[5] + "." +
                " Got result: [" + strResponse + "]");

		return strResponse.trim();
	}
	
	
	/**
	 * Sends ZEN from a source address to a destination address. The change is sent back to the source address.
	 * The amount of change is calculated based on the existing confirmed balance for the address (parameter).
	 * This may not be 100% accurate if the blockchain is not synchronized.
	 * 
	 * @param from source address (T/Z)
	 * @param to destination address (T/Z)
	 * @param balance current confirmed balance of the source address
	 * @param amount ZEN amount to send
	 * @param memo text memo to include in the transaction
	 * @param transactionFee transaction see to include
	 * 
	 * @return a zend operation ID for the send operation
	 * 
	 * @throws WalletCallException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public synchronized String sendCashWithReturnOfChange(String from, String to, String balance, 
			                                              String amount, String memo, String transactionFee)
		throws WalletCallException, IOException, InterruptedException
	{
		Log.info("Starting operation send cash with return of change. Parameters are: from address: {0}, to address: {1}, " + 
	             "current balance: {2}, amount: {3}, memo: {4}, transaction fee: {5}",
				 from, to, balance, amount, memo, transactionFee);
		
		final DecimalFormatSymbols decSymbols = new DecimalFormatSymbols(Locale.ROOT);
		final DecimalFormat sendNumberFormat = new DecimalFormat("########0.00######", decSymbols);
		
		String hexMemo = Util.encodeHexString(memo);
		// Form the main amount to send
		JsonObject toDestinationArgument = new JsonObject();
		toDestinationArgument.set("address", to);
		if (hexMemo.length() >= 2)
		{
			toDestinationArgument.set("memo", hexMemo.toString());
		}
		String formattedAmountToSend = sendNumberFormat.format(new BigDecimal(amount));
		String formattedTransactionFee = sendNumberFormat.format(new BigDecimal(transactionFee));
		toDestinationArgument.set("amount", formattedAmountToSend);
		// Form the return amount - based on the balance
		JsonObject backToSourceArgument = new JsonObject();
		backToSourceArgument.set("address", from);
		BigDecimal changeToSendBackBD = new BigDecimal(balance).
			subtract(new BigDecimal(formattedAmountToSend)).subtract(new BigDecimal(formattedTransactionFee));
		boolean changeIsZero = false;
		if (changeToSendBackBD.compareTo(new BigDecimal("0")) < 0)
		{
			throw new WalletCallException("Error: change was calculated negative");
		} else if (changeToSendBackBD.compareTo(new BigDecimal("0")) == 0)
		{
			Log.info("Change was calculated exactly zero - will not be sent back!");
			changeIsZero = true;
		}
		
		String formattedChangeToReturn = sendNumberFormat.format(changeToSendBackBD);
		backToSourceArgument.set("amount", formattedChangeToReturn);

		// Array of two addresses to send to
		JsonArray toMany = new JsonArray();
		toMany.add(toDestinationArgument);
		if (!changeIsZero)
		{
			toMany.add(backToSourceArgument);
		}
		
		String toManyArrayStr =	toMany.toString(WriterConfig.MINIMAL);		
		String[] sendCashParameters = new String[]
	    {
		    this.zcashcli.getCanonicalPath(), "z_sendmany", wrapStringParameter(from),
		    wrapStringParameter(toManyArrayStr),
		    // Default min confirmations for the input transactions is 1
		    "1",
		    // transaction fee
		    formattedTransactionFee
		};
		
		// Safeguard to make sure the monetary amount does not differ after formatting
		BigDecimal bdAmout = new BigDecimal(amount); // original amount used for check
		JsonArray toManyVerificationArr = Json.parse(toManyArrayStr).asArray();
		BigDecimal bdFinalAmount = 
			new BigDecimal(toManyVerificationArr.get(0).asObject().getString("amount", "-1"));
		BigDecimal amountCheckDifference = bdAmout.subtract(bdFinalAmount).abs();
		if (amountCheckDifference.compareTo(new BigDecimal("0.0")) > 0) // MUST be exact
		{
			throw new WalletCallException("Error in forming z_sendmany command: Main amount differs after formatting: " + 
					                      formattedAmountToSend + " | \n" + toManyArrayStr);
		}		
		// Amount + change + fee = balance // This must also match
		BigDecimal bdFinalChange = changeIsZero ?  
			new BigDecimal("0") : new BigDecimal(toManyVerificationArr.get(1).asObject().getString("amount", "-1"));
		amountCheckDifference = bdFinalChange.add(bdFinalAmount).add(new BigDecimal(formattedTransactionFee)).
			subtract(new BigDecimal(balance)).abs(); // Original balance used after formatting
		if (amountCheckDifference.compareTo(new BigDecimal("0.0")) > 0) // MUST be exact
		{
			throw new WalletCallException("Error in forming z_sendmany command: Sum differs after formatting: " + 
					                      formattedAmountToSend + " | \n" + toManyArrayStr);
		}		
		
		Log.info("The following send command (with change return) will be issued: " +
                sendCashParameters[0] + " " + sendCashParameters[1] + " " +
                sendCashParameters[2] + " " + sendCashParameters[3] + " " +
                sendCashParameters[4] + " " + sendCashParameters[5] + ".");
		
		// Create caller to send cash
	    CommandExecutor caller = new CommandExecutor(sendCashParameters);
	    String strResponse = caller.execute();

		if (strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error:") ||
			strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
		{
		  	throw new WalletCallException("Error response from wallet: " + strResponse);
		}

		Log.info("Sent cash (with change back) with the following command: " +
                sendCashParameters[0] + " " + sendCashParameters[1] + " " +
                sendCashParameters[2] + " " + sendCashParameters[3] + " " +
                sendCashParameters[4] + " " + sendCashParameters[5] + "." +
                " Got result: [" + strResponse + "]");

		return strResponse.trim();
	}
	
	
	// Returns OPID
	public synchronized String sendMessage(String from, String to, double amount, double fee, String memo)
		throws WalletCallException, IOException, InterruptedException
	{
		String hexMemo = Util.encodeHexString(memo);
		JsonObject toArgument = new JsonObject();
		toArgument.set("address", to);
		if (hexMemo.length() >= 2)
		{
			toArgument.set("memo", hexMemo.toString());
		}
		
		DecimalFormatSymbols decSymbols = new DecimalFormatSymbols(Locale.ROOT);

		// TODO: The JSON Builder has a problem with double values that have no fractional part
		// it serializes them as integers that ZCash does not accept. This will work with the 
		// fractional amounts always used for messaging
		toArgument.set("amount", new DecimalFormat("########0.00######", decSymbols).format(amount));

		JsonArray toMany = new JsonArray();
		toMany.add(toArgument);
		
		String toManyArrayStr =	toMany.toString();		
		String[] sendCashParameters = new String[]
	    {
		    this.zcashcli.getCanonicalPath(), "z_sendmany", wrapStringParameter(from),
		    wrapStringParameter(toManyArrayStr),
		    // Default min confirmations for the input transactions is 1
		    "1",
		    // transaction fee
		    new DecimalFormat("########0.00######", decSymbols).format(fee)
		};
				
		// Create caller to send cash
	    CommandExecutor caller = new CommandExecutor(sendCashParameters);
	    String strResponse = caller.execute();

		if (strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error:") ||
			strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
		{
		  	throw new WalletCallException("Error response from wallet: " + strResponse);
		}

		Log.info("Sending cash message with the following command: " +
                sendCashParameters[0] + " " + sendCashParameters[1] + " " +
                sendCashParameters[2] + " " + sendCashParameters[3] + " " +
                sendCashParameters[4] + " " + sendCashParameters[5] + "." +
                " Got result: [" + strResponse + "]");

		return strResponse.trim();
	}


	// Returns the message signature
	public synchronized String signMessage(String address, String message)
		throws WalletCallException, IOException, InterruptedException
	{
	    String response = this.executeCommandAndGetSingleStringResponse(
	    	"signmessage", wrapStringParameter(address), wrapStringParameter(message));

		return response.trim();
	}
	
	
	// Verifies a message - true if OK
	public synchronized boolean verifyMessage(String address, String signature, String message)
		throws WalletCallException, IOException, InterruptedException
	{
	    String response = this.executeCommandAndGetSingleStringResponse(
	    	"verifymessage", 
	    	wrapStringParameter(address), 
	    	wrapStringParameter(signature), 
	    	wrapStringParameter(message));

		return response.trim().equalsIgnoreCase("true");
	}


	public synchronized boolean isSendingOperationComplete(String opID)
	    throws WalletCallException, IOException, InterruptedException
	{
		JsonArray response = this.executeCommandAndGetJsonArray(
			"z_getoperationstatus", wrapStringParameter("[\"" + opID + "\"]"));
		JsonObject jsonStatus = response.get(0).asObject();

		String status = jsonStatus.getString("status", "ERROR");

		Log.info("Operation " + opID + " status is " + response + ".");

		if (status.equalsIgnoreCase("success") ||
			status.equalsIgnoreCase("error") ||
			status.equalsIgnoreCase("failed"))
		{
			return true;
		} else if (status.equalsIgnoreCase("executing") || status.equalsIgnoreCase("queued"))
		{
			return false;
		} else
		{
			throw new WalletCallException("Unexpected status response from wallet: " + response.toString());
		}
	}


	public synchronized boolean isCompletedOperationSuccessful(String opID)
	    throws WalletCallException, IOException, InterruptedException
	{
		JsonArray response = this.executeCommandAndGetJsonArray(
			"z_getoperationstatus", wrapStringParameter("[\"" + opID + "\"]"));
		JsonObject jsonStatus = response.get(0).asObject();

		String status = jsonStatus.getString("status", "ERROR");

		Log.info("Operation " + opID + " status is " + response + ".");

		if (status.equalsIgnoreCase("success"))
		{
			return true;
		} else if (status.equalsIgnoreCase("error") || status.equalsIgnoreCase("failed"))
		{
			return false;
		} else
		{
			throw new WalletCallException("Unexpected final operation status response from wallet: " + response.toString());
		}
	}
	
	
	public synchronized String getSuccessfulOperationTXID(String opID)
        throws WalletCallException, IOException, InterruptedException
	{
		String TXID = null;
		JsonArray response = this.executeCommandAndGetJsonArray(
			"z_getoperationstatus", wrapStringParameter("[\"" + opID + "\"]"));
		JsonObject jsonStatus = response.get(0).asObject();
		JsonValue  opResultValue = jsonStatus.get("result"); 
		
		if (opResultValue != null)
		{
			JsonObject opResult = opResultValue.asObject();
			if (opResult.get("txid") != null)
			{
				TXID = opResult.get("txid").asString();
			}
		}
		
		return TXID;
	}


	// May only be called for already failed operations
	public synchronized String getOperationFinalErrorMessage(String opID)
	    throws WalletCallException, IOException, InterruptedException
	{
		JsonArray response = this.executeCommandAndGetJsonArray(
			"z_getoperationstatus", wrapStringParameter("[\"" + opID + "\"]"));
		JsonObject jsonStatus = response.get(0).asObject();

		JsonObject jsonError = jsonStatus.get("error").asObject();
		return jsonError.getString("message", "ERROR!");
	}


	public synchronized NetworkAndBlockchainInfo getNetworkAndBlockchainInfo()
		throws WalletCallException, IOException, InterruptedException
	{
		NetworkAndBlockchainInfo info = new NetworkAndBlockchainInfo();

		String strNumCons = this.executeCommandAndGetSingleStringResponse("getconnectioncount");
		info.numConnections = Integer.valueOf(strNumCons.trim());

		String strBlockCount = this.executeCommandAndGetSingleStringResponse("getblockcount");
		String lastBlockHash = this.executeCommandAndGetSingleStringResponse("getblockhash", strBlockCount.trim());
		JsonObject lastBlock = this.executeCommandAndGetJsonObject("getblock", wrapStringParameter(lastBlockHash.trim()));
		info.lastBlockDate = new Date(Long.valueOf(lastBlock.getLong("time", -1) * 1000L));

		return info;
	}


	public synchronized void lockWallet()
		throws WalletCallException, IOException, InterruptedException
	{
		String response = this.executeCommandAndGetSingleStringResponse("walletlock");

		// Response is expected to be empty
		if (response.trim().length() > 0)
		{
			throw new WalletCallException("Unexpected response from wallet: " + response);
		}
	}


	// Unlocks the wallet for 5 minutes - meant to be followed shortly by lock!
	// TODO: tests with a password containing spaces
	public synchronized void unlockWallet(String password)
		throws WalletCallException, IOException, InterruptedException
	{
		String response = this.executeCommandAndGetSingleStringResponse(
			"walletpassphrase", wrapStringParameter(password), "300");

		// Response is expected to be empty
		if (response.trim().length() > 0)
		{
			throw new WalletCallException("Unexpected response from wallet: " + response);
		}
	}


    // Wallet locks check - an unencrypted wallet will give an error
	// zcash-cli walletlock
	// error: {"code":-15,"message":"Error: running with an unencrypted wallet, but walletlock was called."}
	public synchronized boolean isWalletEncrypted()
   		throws WalletCallException, IOException, InterruptedException
    {
		String[] params = new String[] { this.zcashcli.getCanonicalPath(), "walletlock" };
		CommandExecutor caller = new CommandExecutor(params);
    	String strResult = caller.execute();

    	 if (strResult.trim().length() <= 0)
    	 {
    		 // If it could be locked with no result - obviously encrypted
    		 return true;
    	 } else if (strResult.trim().toLowerCase(Locale.ROOT).startsWith("error:"))
    	 {
    		 // Expecting an error of an unencrypted wallet
    		 String jsonPart = strResult.substring(strResult.indexOf("{"));
   			 JsonValue response = null;
   			 try
   			 {
   			   	response = Json.parse(jsonPart);
   		 	 } catch (ParseException pe)
   			 {
   			   	 throw new WalletCallException(jsonPart + "\n" + pe.getMessage() + "\n", pe);
   			 }

   			 JsonObject respObject = response.asObject();
   			 if ((respObject.getDouble("code", -1) == -15) &&
   				 (respObject.getString("message", "ERR").indexOf("unencrypted wallet") != -1))
   			 {
   				 // Obviously unencrypted
   				 return false;
   			 } else
   			 {
   	    		 throw new WalletCallException("Unexpected response from wallet: " + strResult);
   			 }
    	 } else if (strResult.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
    	 {
   			 JsonObject respObject = Util.getJsonErrorMessage(strResult);
   			 if ((respObject.getDouble("code", -1) == -15) &&
   				 (respObject.getString("message", "ERR").indexOf("unencrypted wallet") != -1))
   			 {
   				 // Obviously unencrypted
   				 return false;
   			 } else
   			 {
   	    		 throw new WalletCallException("Unexpected response from wallet: " + strResult);
   			 }
    	 } else
    	 {
    		 throw new WalletCallException("Unexpected response from wallet: " + strResult);
    	 }
    }


	/**
	 * Encrypts the wallet. Typical success/error use cases are:
	 *
	 * ./zcash-cli encryptwallet "1234"
	 * wallet encrypted; Bitcoin server stopping, restart to run with encrypted wallet.
	 * The keypool has been flushed, you need to make a new backup.
	 *
	 * ./zcash-cli encryptwallet "1234"
	 * error: {"code":-15,"message":"Error: running with an encrypted wallet, but encryptwallet was called."}
	 *
	 * @param password
	 */
	public synchronized void encryptWallet(String password)
		throws WalletCallException, IOException, InterruptedException
	{
		String response = this.executeCommandAndGetSingleStringResponse(
			"encryptwallet", wrapStringParameter(password));
		Log.info("Result of wallet encryption is: \n" + response);
		// If no exception - obviously successful
	}
	
	
	public synchronized String backupWallet(String fileName)
		throws WalletCallException, IOException, InterruptedException
	{
		Log.info("Backup up wallet to location: " + fileName);
		String response = this.executeCommandAndGetSingleStringResponse(
			"backupwallet", wrapStringParameter(fileName));
		// If no exception - obviously successful		
		return response;
	}
	
	
	public synchronized String exportWallet(String fileName)
		throws WalletCallException, IOException, InterruptedException
	{
		Log.info("Export wallet keys to location: " + fileName);
		String response = this.executeCommandAndGetSingleStringResponse(
			"z_exportwallet", wrapStringParameter(fileName));
		// If no exception - obviously successful		
		return response;
	}
	
	
	public synchronized void importWallet(String fileName)
		throws WalletCallException, IOException, InterruptedException
	{
		Log.info("Import wallet keys from location: " + fileName);
		String response = this.executeCommandAndGetSingleStringResponse(
			"z_importwallet", wrapStringParameter(fileName));
		// If no exception - obviously successful		
	}
	
	
	public synchronized String getTPrivateKey(String address)
		throws WalletCallException, IOException, InterruptedException
	{
		String response = this.executeCommandAndGetSingleStringResponse(
			"dumpprivkey", wrapStringParameter(address));
		
		return response.trim();
	}
	
	
	public synchronized String getZPrivateKey(String address)
	    throws WalletCallException, IOException, InterruptedException
	{
		String response = this.executeCommandAndGetSingleStringResponse(
			"z_exportkey", wrapStringParameter(address));
		
		return response.trim();
	}
	
	
	// Imports a private key - tries both possibilities T/Z
	public synchronized String importPrivateKey(String key)
		throws WalletCallException, IOException, InterruptedException
	{
		// First try a Z key
		String[] params = new String[] 
		{ 
			this.zcashcli.getCanonicalPath(),
			"-rpcclienttimeout=5000",
			"z_importkey", 
			wrapStringParameter(key) 
		};
		CommandExecutor caller = new CommandExecutor(params);
    	String strResult = caller.execute();
		
		if (Util.stringIsEmpty(strResult) || 
			(!strResult.trim().toLowerCase(Locale.ROOT).contains("error")))
		{
			return strResult == null ? "" : strResult.trim();
		}
		
		// Obviously we have an error trying to import a Z key
		if (strResult.trim().toLowerCase(Locale.ROOT).startsWith("error") &&
			(strResult.indexOf("{") != -1))
		{
   		 	 // Expecting an error of a T address key
   		 	 String jsonPart = strResult.substring(strResult.indexOf("{"));
  		     JsonValue response = null;
  			 try
  			 {
  			   	response = Json.parse(jsonPart);
  		 	 } catch (ParseException pe)
  			 {
  			   	 throw new WalletCallException(jsonPart + "\n" + pe.getMessage() + "\n", pe);
  			 }

  			 JsonObject respObject = response.asObject();
  			 if ((respObject.getDouble("code", +123) == -1) &&
  				 (respObject.getString("message", "ERR").indexOf("wrong network type") != -1))
  			 {
  				 // Obviously T address - do nothing here
  			 } else
  			 {
  	    		 throw new WalletCallException("Unexpected response from wallet: " + strResult);
  			 }
		} else if (strResult.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
		{
 			 JsonObject respObject = Util.getJsonErrorMessage(strResult);
 			 if ((respObject.getDouble("code", +123) == -1) &&
 				 (respObject.getString("message", "ERR").indexOf("wrong network type") != -1))
 			 {
 				 // Obviously T address - do nothing here
 			 } else
 			 {
 	    		 throw new WalletCallException("Unexpected response from wallet: " + strResult);
 			 }
		} else
		{
			throw new WalletCallException("Unexpected response from wallet: " + strResult);
		}
		
		// Second try a T key
		strResult = this.executeCommandAndGetSingleStringResponse(
			"-rpcclienttimeout=5000", "importprivkey", wrapStringParameter(key));
		
		if (Util.stringIsEmpty(strResult) || 
			(!strResult.trim().toLowerCase(Locale.ROOT).contains("error")))
		{
			return strResult == null ? "" : strResult.trim();
		}
		
		// Obviously an error
		throw new WalletCallException("Unexpected response from wallet: " + strResult);
	}
	

	private JsonObject executeCommandAndGetJsonObject(String command1, String command2)
		throws WalletCallException, IOException, InterruptedException
	{
		JsonValue response = this.executeCommandAndGetJsonValue(command1, command2);

		if (response.isObject())
		{
			return response.asObject();
		} else
		{
			throw new WalletCallException("Unexpected non-object response from wallet: " + response.toString());
		}

	}


	private JsonArray executeCommandAndGetJsonArray(String command1, String command2)
		throws WalletCallException, IOException, InterruptedException
	{
		return this.executeCommandAndGetJsonArray(command1, command2, null);
	}


	private JsonArray executeCommandAndGetJsonArray(String command1, String command2, String command3)
		throws WalletCallException, IOException, InterruptedException
	{
		JsonValue response = this.executeCommandAndGetJsonValue(command1, command2, command3);

		if (response.isArray())
		{
			return response.asArray();
		} else
		{
			throw new WalletCallException("Unexpected non-array response from wallet: " + response.toString());
		}
	}


	private JsonValue executeCommandAndGetJsonValue(String command1, String command2)
			throws WalletCallException, IOException, InterruptedException
	{
		return this.executeCommandAndGetJsonValue(command1, command2, null);
	}


	private JsonValue executeCommandAndGetJsonValue(String command1, String command2, String command3)
		throws WalletCallException, IOException, InterruptedException
	{
		String strResponse = this.executeCommandAndGetSingleStringResponse(command1, command2, command3);

		JsonValue response = null;
		try
		{
		  	response = Json.parse(strResponse);
		} catch (ParseException pe)
		{
		  	throw new WalletCallException(strResponse + "\n" + pe.getMessage() + "\n", pe);
		}

		return response;
	}


	private String executeCommandAndGetSingleStringResponse(String command1)
		throws WalletCallException, IOException, InterruptedException
	{
		return this.executeCommandAndGetSingleStringResponse(command1, null);
	}


	private String executeCommandAndGetSingleStringResponse(String command1, String command2)
		throws WalletCallException, IOException, InterruptedException
	{
		return this.executeCommandAndGetSingleStringResponse(command1, command2, null);
	}
	
	
	private String executeCommandAndGetSingleStringResponse(String command1, String command2, String command3)
		throws WalletCallException, IOException, InterruptedException
	{
		return executeCommandAndGetSingleStringResponse(command1, command2, command3, null);
	}


	private String executeCommandAndGetSingleStringResponse(
			                        String command1, String command2, String command3, String command4)
		throws WalletCallException, IOException, InterruptedException
	{
		String[] params;
		if (command4 != null)
		{
			params = new String[] { this.zcashcli.getCanonicalPath(), command1, command2, command3, command4 };
		} else if (command3 != null)
		{
			params = new String[] { this.zcashcli.getCanonicalPath(), command1, command2, command3 };
		} else if (command2 != null)
		{
			params = new String[] { this.zcashcli.getCanonicalPath(), command1, command2 };
		} else
		{
			params = new String[] { this.zcashcli.getCanonicalPath(), command1 };
		}

	    CommandExecutor caller = new CommandExecutor(params);

		String strResponse = caller.execute();
		if (strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error:")       ||
			strResponse.trim().toLowerCase(Locale.ROOT).startsWith("error code:"))
		{
		  	throw new WalletCallException("Error response from wallet: " + strResponse);
		}

		return strResponse;
	}
	
	
	// Used to wrap string parameters on the command line - not doing so causes problems on Windows.
	public static String wrapStringParameter(String param)
	{
		OS_TYPE os = OSUtil.getOSType();
		
		// Fix is made for Windows only
		if (os == OS_TYPE.WINDOWS)
		{
			param = "\"" + param.replace("\"", "\\\"") + "\"";
		}
		
		return param;
	}
	
	
	private void decomposeJSONValue(String name, JsonValue val, Map<String, String> map)
	{
		if (val.isObject())
		{
			JsonObject obj = val.asObject();
			for (String memberName : obj.names())
			{
				this.decomposeJSONValue(name + "." + memberName, obj.get(memberName), map);
			}
		} else if (val.isArray())
		{
			JsonArray arr = val.asArray();
			for (int i = 0; i < arr.size(); i++)
			{
				this.decomposeJSONValue(name + "[" + i + "]", arr.get(i), map);
			}
		} else
		{
			map.put(name, val.toString());
		}
	}
}
