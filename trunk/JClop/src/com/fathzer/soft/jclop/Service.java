package com.fathzer.soft.jclop;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;

import net.astesana.ajlib.utilities.StringUtils;

/** This class represents a cloud service, with accounts synchronized with a local cache.
 * <br>Limitation: Account can't contain folder.
 * @author Jean-Marc Astesana
 * Licence GPL v3
 */
public abstract class Service {
	static final String UTF_8 = "UTF-8";
	public static final String URI_DOMAIN = "cloud.astesana.net";

	private File root;
	private Collection<Account> accounts;

	/** Constructor.
	 * @param root The root folder of the service (the place where all accounts are cached).<br>
	 * @throws IllegalArgumentException if it is not possible to create the folder (or root is a file, not a folder)  
	 */
	protected Service(File root) {
		if (!root.exists()) root.mkdirs();
		if (!root.isDirectory()) throw new IllegalArgumentException();
		this.root = root;
		refreshAccounts();
	}
	
	/** Builds an account instance that is cached in a folder passed in argument.
	 * @param folder The folder where the account is cached
	 * @return An account, or null if the folder doesn't not contain a valid account.
	 * @see Account#Account(Service, File)
	 */
	private Account buildAccount(File folder) {
		try {
			return new Account(this, folder);
		} catch (Exception e) {
			return null; //FIXME
		}
	}
	
	/** Gets the available accounts.
	 * @return A collection of available accounts
	 */
	public Collection<Account> getAccounts() {
		return accounts;
	}
	
	/** Forces the account list to be rebuild from the file cache content.
	 */
	public void refreshAccounts() {
		File[] files = root.listFiles();
		accounts = new ArrayList<Account>();
		for (File file : files) {
			if (file.isDirectory()) {
				Account candidate = buildAccount(file);
				if (candidate!=null) accounts.add(candidate);
			}
		}
	}
	
	File getCacheRoot() {
		return root;
	}

	public abstract String getScheme();

	/** Gets the remote path of a local one.
	 * <br>By default, this method returns the local path.
	 * @param localPath The remote path
	 * @return the remote path
	 * @see #getLocalPath(String)
	 */
	public String getRemotePath(String localPath) {
		return localPath;
	}

	/** Gets the local path related to a remote path.
	 * <br>This method can be used by getRemoteFiles method in order to filter remote files.
	 * <br>By default, this method returns the remote path.
	 * @param remotePath The remote path
	 * @return the local path or null if the entry should be ignored
	 * @see #getRemoteFiles(Account, Cancellable)
	 */
	public String getLocalPath(String remotePath) {
		return remotePath;
	}
	
	/** Gets the URI of an entry.
	 * @param entry An entry
	 * @return The entry's URI
	 */
	public final URI getURI(Entry entry) {
		try {
			Account account = entry.getAccount();
			String path = getRemotePath(entry.getDisplayName());
			StringBuilder builder = new StringBuilder();
			builder.append(getScheme());
			builder.append("://");
			builder.append(URLEncoder.encode(account.getId(), UTF_8));
			builder.append(":");
			builder.append(getConnectionDataURIFragment(account.getConnectionData()));
			builder.append('@');
			builder.append(URI_DOMAIN);
			builder.append('/');
			builder.append(URLEncoder.encode(account.getDisplayName(), UTF_8));
			builder.append('/');
			if (path.startsWith("/")) path = path.substring(1);
			builder.append(URLEncoder.encode(path, UTF_8));
			return new URI(builder.toString());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/** Gets an Entry from its URI.
	 * @param uri An URI
	 * @return an Entry or null if the entry is not allowed by the service (getLocalPath returns null).
	 * @see #getLocalPath(String)
	 */
	public final Entry getEntry(URI uri) {
		try {
			String path = URLDecoder.decode(uri.getPath().substring(1), UTF_8);
			int index = path.indexOf('/');
			String accountName = path.substring(0, index);
			path = getLocalPath(path.substring(index));
			if (path==null) return null;
			String[] split = StringUtils.split(uri.getUserInfo(), ':');
			String accountId = URLDecoder.decode(split[0], UTF_8);
			for (Account account : getAccounts()) {
				if (account.getId().equals(accountId)) {
					return new Entry(account, path);
				}
			}
			// The account is unknown
			Serializable connectionData = getConnectionData(split[1]);
			Account account = new Account(this, accountId, accountName, connectionData, -1, -1);
			return new Entry(account, path);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public abstract Collection<Entry> getRemoteFiles(Account account, Cancellable task) throws UnreachableHostException;

	public abstract String getConnectionDataURIFragment(Serializable connectionData);
	public abstract Serializable getConnectionData(String uriFragment);
}
