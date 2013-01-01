package com.fathzer.soft.jclop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import net.astesana.ajlib.utilities.StringUtils;

/** This class represents a cloud service, with accounts synchronized with a local cache.
 * <br>Limitation: Account can't contain folder.
 * @author Jean-Marc Astesana
 * Licence GPL v3
 */
public abstract class Service {
	static final String UTF_8 = "UTF-8";
	static final String ZIP_SUFFIX = ".zip";
	static final String FILE_PREFIX = "f_";
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
	
	/** Gets the scheme of uri managed by this service.
	 * <br>Files are identified by uri. This method returns the scheme of the uri of files managed by this service.
	 * The scheme is considered as unique id for services.
	 * @return the service uri scheme
	 */
	public abstract String getScheme();

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

	/** Get the entry corresponding to a local cache file.
	 * @param account The account where the path is located.
	 * @param localPath The path of the local cache file, related to the account 
	 * @return an entry or null if the entry is not a valid cache file.
	 */
	protected final Entry getLocalEntry(Account account, String localPath) {
		try {
			if (!localPath.startsWith(FILE_PREFIX)) return null;
			return new Entry(account, URLDecoder.decode(localPath.substring(FILE_PREFIX.length()), UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	public final String getLocalPath(Entry entry) {
		try {
			return entry.getAccount().getRoot().getName()+"/"+FILE_PREFIX+URLEncoder.encode(entry.getDisplayName(), UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** Gets the remote path of an entry.
	 * <br>By default, this method returns the local path preceded by a '/' and followed by ".zip", indicating that the file is zipped on the cloud storage.
	 * <br>If you override this method to change that, don't forget to override getRemoteEntry too. 
	 * @param entry The entry
	 * @return the remote path
	 * @see #getRemoteEntry(Account, String)
	 */
	public String getRemotePath(Entry entry) {
		return '/'+entry.getDisplayName()+ZIP_SUFFIX;
	}

	/** Converts a remote path to an entry.
	 * <br>This method can be used by getRemoteFiles method in order to filter remote files.
	 * <br>By default, this method returns the remote path without its ".zip" suffix. If the path begins with a '/', it is removed.
	 * <br>If you override this method to change that, don't forget to override getRemotePath too. 
	 * @param account The account
	 * @param remotePath The remote path
	 * @return An entry or null if the entry should be ignored
	 * @see #getRemoteEntries(Account, Cancellable)
	 * @see #getRemotePath(Entry)
	 */
	protected Entry getRemoteEntry(Account account, String remotePath) {
		if (remotePath.endsWith(ZIP_SUFFIX)) remotePath = remotePath.substring(0, remotePath.length()-ZIP_SUFFIX.length());
		return new Entry (account, remotePath.charAt(0)=='/'?remotePath.substring(1):remotePath);
	}
	
	/** Gets the URI of an entry.
	 * @param entry An entry
	 * @return The entry's URI
	 */
	public final URI getURI(Entry entry) {
		try {
			return new URI(toString(entry, false));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/** Gets a string representation of an URI that can safely be displayed on a screen.
	 * <br>URI may contains secret informations (example: password).
	 * @param uri The uri.
	 * @return a String. Let say entry is the entry corresponding to the uri, this implementation returns the getScheme()://entry.getAccountName()/entry.getDisplayName().
	 */
	public String getDisplayable(URI uri) {
		return toString(getEntry(uri), true);
	}

	private String toString(Entry entry, boolean secret) {
		try {
			Account account = entry.getAccount();
			String path = entry.getDisplayName();
			StringBuilder builder = new StringBuilder();
			builder.append(getScheme());
			builder.append("://");
			if (!secret) {
				builder.append(URLEncoder.encode(account.getId(), UTF_8));
				builder.append(":");
				builder.append(getConnectionDataURIFragment(account.getConnectionData()));
				builder.append('@');
				builder.append(URI_DOMAIN);
				builder.append('/');
			}
			builder.append(URLEncoder.encode(account.getDisplayName(), UTF_8));
			builder.append('/');
			if (path.startsWith("/")) path = path.substring(1);
			builder.append(URLEncoder.encode(path, UTF_8));
			return builder.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/** Gets an Entry from its URI.
	 * @param uri An URI
	 * @return an Entry or null if the entry is not allowed by the service (getLocalPath returns null).
	 * @throws IllegalArgumentException if the uri is not supported or has a wrong format
	 */
	public final Entry getEntry(URI uri) {
		if (!uri.getScheme().equals(getScheme())) throw new IllegalArgumentException();
		try {
			String path = URLDecoder.decode(uri.getPath().substring(1), UTF_8);
			int index = path.indexOf('/');
			String accountName = path.substring(0, index);
			path = path.substring(index+1);
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
	
	public abstract Collection<Entry> getRemoteEntries(Account account, Cancellable task) throws UnreachableHostException;
	public abstract String getConnectionDataURIFragment(Serializable connectionData);
	public abstract Serializable getConnectionData(String uriFragment);
	
	/** Downloads the uri to a file.
	 * @param entry The entry to download.
	 * @param out The stream where to download
	 * @param task The task that ask the download or null if no cancellable task is provided. Please make sure to report the progress and cancel the download if the task is cancelled.
	 * @param locale The locale that will be used to set the name of task phases. This argument can be null if task is null too.
	 * @return true if the upload is done, false if it was cancelled
	 * @throws IOException 
	 */
	public abstract boolean download(Entry entry, OutputStream out, Cancellable task, Locale locale) throws IOException;

	/** Uploads a file to a destination uri.
	 * @param in The inputStream from which to read to uploaded bytes
	 * @param length The number of bytes to upload
	 * @param entry The entry where to upload.
	 * @param task The task that ask the download or null if no cancellable task is provided. Please make sure to report the progress and cancel the upload if the task is cancelled.
 	 * @param locale The locale that will be used to set the name of task phases. This argument can be null if task is null too.
	 * @return true if the upload is done, false if it was cancelled
	 * @throws IOException 
	 */
	public abstract boolean upload(InputStream in, long length, Entry entry, Cancellable task, Locale locale) throws IOException;
}
