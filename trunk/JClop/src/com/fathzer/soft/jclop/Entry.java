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
		int result = this.account.getId().compareTo(o.getAccount().getId());
		if (result==0) result = this.displayName.compareTo(o.displayName);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return account.hashCode()+displayName.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Entry)) return super.equals(obj);
		return account.equals(((Entry) obj).getAccount()) && displayName.equals(((Entry) obj).getDisplayName());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return account.getId()+"/"+displayName;
	}
}
