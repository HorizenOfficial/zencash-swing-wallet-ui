package com.vaklinov.zcashui.arizen.models;
/**
 * 
 * @author Pier Stabilini <pier@zensystem.io>
 *
 */
public class Address {
	
	public enum ADDRESS_TYPE {
		TRANSPARENT, PRIVATE;
	}
	
	private String address;
	private String privateKey;
	private String balance;
	private ADDRESS_TYPE type;
	
	public Address() {		
	}	
	
	public Address(ADDRESS_TYPE type, String address, String privateKey, String balance) {
		this.type = type;
		this.address = address;
		this.privateKey = privateKey;
		this.balance = balance;
	}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getPrivateKey() {
		return privateKey;
	}
	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}
	public String getBalance() {
		return balance;
	}
	public void setBalance(String balance) {
		this.balance = balance;
	}
	public ADDRESS_TYPE getType() {
		return type;
	}
	public void setType(ADDRESS_TYPE type) {
		this.type = type;
	}
	
}
