package com.fathzer.soft.jclop;

public class Entry implements Comparable<Entry> {
	private String displayName;
	private Account account;
	
	public Entry(Account account, String displayName) {
		this.displayName = displayName;
		this.account = account;
	}

	/**
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	/**
	 * @return the account
	 */
	public Account getAccount() {
		return account;
	}
	
	@Override
	public int compareTo(Entry o) {
		return this.displayName.compareTo(o.displayName);
	}
}