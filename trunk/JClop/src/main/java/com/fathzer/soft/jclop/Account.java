package com.fathzer.soft.jclop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;



import org.slf4j.LoggerFactory;

import com.fathzer.soft.ajlib.utilities.NullUtils;
import com.fathzer.soft.jclop.Service;

/** An account in the Cloud, cached in a local folder.
 * @see Service
 */
public final class Account {
	private static final String INFO_FILENAME = ".info";

	private File root;
	Service service;
	private String displayName;
	private String id;
	private boolean serialized;
	protected Serializable connectionData;
	protected long quota;
	protected long used;
	
	Account(Service service, File file) throws IOException {
		if (!file.isDirectory()) throw new IllegalArgumentException();
		this.root = file;
		this.id = URLDecoder.decode(file.getName(), Service.UTF_8);
		ObjectInputStream stream = new ObjectInputStream(new FileInputStream(new File(this.root, INFO_FILENAME)));
		try {
			this.displayName = (String) stream.readObject();
			this.connectionData = (Serializable) stream.readObject();
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		} finally {
			stream.close();
		}
		this.service = service;
		this.quota = -1;
		this.used = -1;
		this.serialized = true;
	}
	
	Account(Service service, String id, String displayName, Serializable connectionData) {
		this.service = service;
		this.id = id;
		this.displayName = displayName;
		this.connectionData = connectionData;
		this.quota = -1;
		this.used = -1;
		this.serialized = false;
		try {
			this.root = new File(service.getCacheRoot(), URLEncoder.encode(id, Service.UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** Serialize the account data to the cache.
	 * <br>If the serialization fails the account is flag as not serialized
	 * @see #isSerialized()
	 */
	synchronized void serialize() {
		serialized = false;
		if (this.root.isFile()) this.root.delete();
		this.root.mkdirs();
		if (!this.root.isDirectory()) return;
		File connectionDataFile = new File(this.root, INFO_FILENAME);
		try {
			ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(connectionDataFile));
			try {
				stream.writeObject(this.displayName);
				stream.writeObject(this.connectionData);
			} finally {
				stream.close();
			}
		} catch (IOException e) {
			LoggerFactory.getLogger(Account.class).warn("Unable to serialize account "+this.displayName, e);
			return;
		}
		serialized = true;
	}
	
	/** Tests whether this account data has been successfully written to the cache until its last modification.
	 * <br>The account data is display name and connection data. Folder content is not manage by attribute 
	 * @return true if the account is serialized
	 */
	public synchronized boolean isSerialized() {
		return this.serialized;
	}
	
	/** Gets this account's display name.
	 * @return a String
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	/** Gets the unique account's id.
	 * @return a String
	 */
	public String getId() {
		return this.id;
	}

	/** Gets the service that hosts this account. 
	 * @return A service
	 */
	public Service getService() {
		return this.service;
	}
	
	/** Gets this account's connection data.
	 * @return A serializable
	 */
	public Serializable getConnectionData() {
		return this.connectionData;
	}

	/** Gets the account quota in bytes.
	 * <br>Please note that this method should return quickly. This means, it should not connect with the server
	 * in order to have the information. This method should return a negative number until the remote data is initialized
	 * by getRemoteFiles.
	 * @return The quota in bytes or a negative number if the service is not able to give this information
	 */
	public long getQuota() {
		return quota;
	}

	/** Sets the account quota in bytes.
	 * @param quota The account quota in bytes. Zero or a negative value means the quota is unknown.
	 */
	public void setQuota(long quota) {
		this.quota = quota;
	}

	/** Gets the size used in bytes.
	 * <br>Please note that this method should return quickly. This means, it should not connect with the server
	 * in order to have the information. This method should return a negative number until the remote data is initialized
	 * by getRemoteFiles.
	 * @return The used size in bytes or a negative number if the service is not able to give this information
	 */
	public long getUsed() {
		return used;
	}
	
	/** Sets the used size of the account.
	 * @param used The size used in the account in bytes. A negative value means this size is unknown.
	 */
	public void setUsed(long used) {
		this.used = used;
	}

	public Collection<Entry> getLocalEntries() {
		Collection<Entry> result = new ArrayList<Entry>();
		File[] files = this.root.listFiles();
		if (files!=null) {
			for (File file : files) {
				if (file.isDirectory()) {
					Entry entry = service.getLocalEntry(this, file);
					if (entry!=null) result.add(entry);
				}
			}
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getId().hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Account)) return super.equals(obj);
		return getId().equals(((Account)obj).getId());
	}

//	public Collection<Entry> getRemoteFiles(Cancellable task) throws UnreachableHostException {
//		return this.service.getRemoteFiles(this, task);
//	}
	
	File getRoot() {
		return this.root;
	}

	/** Sets the display name of this account.
	 * @param displayName The new account display name
	 */
	public void setDisplayName(String displayName) {
		if (!NullUtils.areEquals(displayName,this.displayName)) {
			this.displayName = displayName;
			serialize();
		}
	}

	/** Sets the connection data of this account.
	 * @param connectionData The new connection data name
	 */
	public void setConnectionData(Serializable connectionData) {
		if (!NullUtils.areEquals(connectionData,this.connectionData)) {
			this.connectionData = connectionData;
			serialize();
		}
	}
}
