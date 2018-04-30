package com.vaklinov.zcashui.arizen.repo;

import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.arizen.models.Address;
import com.vaklinov.zcashui.arizen.models.Address.ADDRESS_TYPE;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Set;


/**
 * Arizen wallet implementation
 *
 * @author Pier Stabilini <pier@zensystem.io>
 */
public class ArizenWallet implements WalletRepo {

	Connection conn = null;
		
	private static String settingNotification = "1";
	private static String settingExplorer = "https://explorer.zensystem.io/";
	private static String settingApi = "https://explorer.zensystem.io/insight-api-zen/";
	
    private static String sqlInsertPublicAddress = "INSERT INTO wallet(pk, addr, lastbalance, name) VALUES(?,?,?,?)";
    private static String sqlInsertPrivateAddress = "INSERT INTO zwallet(spk, addr, lastbalance, name) VALUES(?,?,?,?)";
	
	@Override
	public void createWallet(File f) throws Exception {					
			String url = "jdbc:sqlite:" + f.getAbsolutePath();
			conn = DriverManager.getConnection(url);
			Statement stmt = conn.createStatement();
			String sqlWallet = "CREATE TABLE wallet " + "(id INTEGER PRIMARY KEY AUTOINCREMENT, pk TEXT, addr TEXT UNIQUE, lastbalance REAL, name TEXT);";
			stmt.execute(sqlWallet);
			String sqlZWallet = "CREATE TABLE zwallet " + "(id INTEGER PRIMARY KEY AUTOINCREMENT, pk TEXT, spk TEXT, addr TEXT UNIQUE, lastbalance REAL, name TEXT);";
			stmt.execute(sqlZWallet);
			
			String sqlContacts = "CREATE TABLE contacts (id INTEGER PRIMARY KEY AUTOINCREMENT, addr TEXT UNIQUE, name TEXT, nick TEXT);";
			stmt.execute(sqlContacts);
			String sqlSettings = "CREATE TABLE settings (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT UNIQUE, value TEXT);";
			stmt.execute(sqlSettings);
			String sqlTransactions = "CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, txid TEXT, time INTEGER, address TEXT, vins TEXT, vouts TEXT, amount REAL, block INTEGER);";
			stmt.execute(sqlTransactions);
			
			String st1 = "INSERT INTO settings(name, value) VALUES (?, ?)"; 
			PreparedStatement pstmt = conn.prepareStatement(st1);
			pstmt.setString(1, "settingsExplorer");
			pstmt.setString(2, settingExplorer);
			pstmt.execute();

			pstmt.setString(1, "settingsNotifications");
			pstmt.setString(2, settingNotification);
			pstmt.execute();

			pstmt.setString(1, "settingsApi");
			pstmt.setString(2, settingApi);
			pstmt.execute();
			
	}

	@Override
	public void insertAddress(Address address) throws Exception {		
			String sql = address.getType() == Address.ADDRESS_TYPE.TRANSPARENT ? sqlInsertPublicAddress : sqlInsertPrivateAddress;					
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, address.getPrivateKey());
	        pstmt.setString(2, address.getAddress());
	        pstmt.setDouble(3, Double.parseDouble(address.getBalance()));
	        pstmt.setString(4, "");
			pstmt.execute();
	}
	
	@Override
	public void insertAddressBatch(Set<Address> addressSet) throws Exception{		
			Address.ADDRESS_TYPE type = addressSet.iterator().next().getType();
			String sql = type == Address.ADDRESS_TYPE.TRANSPARENT ? sqlInsertPublicAddress : sqlInsertPrivateAddress;
			PreparedStatement pstmt = conn.prepareStatement(sql);
			for (Address address : addressSet) {
				pstmt.setString(1, address.getPrivateKey());
				pstmt.setString(2, address.getAddress());
				pstmt.setDouble(3, Double.parseDouble(address.getBalance()));
				pstmt.setString(4, "");
				pstmt.execute();
			}
	}

	@Override
	public void deleteAddress(String addr) throws Exception{
		Log.info("not yet implemented");
	}

	@Override
	public Set<Address> listAddresses(ADDRESS_TYPE type) throws Exception{
		Log.info("not yet implemented");
		return null;
	}

	@Override
	public void openWallet(File f)  throws Exception{
		Log.info("not yet implemented");
	}

	@Override
	public void close() throws Exception {
             if (conn != null) {
                 conn.close();
             }
	}

	@Override
	public boolean isOpen() {
		if (conn != null) {
			return true;
		}
		return false;		
	}
}
