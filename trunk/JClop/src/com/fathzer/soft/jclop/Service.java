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

import com.fathzer.soft.jclop.swing.MessagePack;

import net.astesana.ajlib.utilities.StringUtils;

/** A cloud service, with accounts synchronized with a local cache.
 * <br>Limitation: Account can't contain folder.
 * @author Jean-Marc Astesana
 * Licence GPL v3
 */
public abstract class Service {
	static final String UTF_8 = "UTF-8";
	static final String ZIP_SUFFIX = ".zip"; //$NON-NLS-1$
	static final String FILE_PREFIX = "f_";
	private static final String CACHE_PREFIX = "cache"; //$NON-NLS-1$
	private static final String SYNCHRONIZED_CACHE_PREFIX = "sync"; //$NON-NLS-1$

	public static final String URI_DOMAIN = "cloud.astesana.net";

	private File root;
	private Collection<Account> accounts;

	/** Constructor.
	 * @param root The root folder of the services (the place where all accounts of all services are cached).<br>
	 * <br>Each service uses a folder in the root folder, where all the accounts it manages are cached.
	 * <br>This folder will be named with the service scheme.
	 * <br>For example, if you root is "/home/user/.MyApp/cache" and getScheme() returns "MyService",
	 * the cached data will be placed in "/home/user/.MyApp/cache/MyService".
	 * @throws IllegalArgumentException if it is not possible to create the service folder (or service is a file, not a folder)
	 * @see #getScheme()  
	 */
	protected Service(File root) {
		root = new File(root, getScheme());
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
	final Entry getLocalEntry(Account account, String localPath) {
		try {
			if (!localPath.startsWith(FILE_PREFIX)) return null;
			return new Entry(account, URLDecoder.decode(localPath.substring(FILE_PREFIX.length()), UTF_8));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** Gets the local file where is stored the cached data.
	 * @param uri an URI
	 * @return a File
	 */
	public final File getLocalFile(URI uri) {
		// Implementation trick:
		// We need to store the base revision of the cached file. We will store it using the file name.
		// This file will be stored in a folder which name is easy to deduced from the entry name.
		String fileName;
		try {
			Entry entry = getEntry(uri);
			fileName = entry.getAccount().getRoot().getName()+"/"+FILE_PREFIX+URLEncoder.encode(entry.getDisplayName(), UTF_8);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		File cacheDirectory = new File(this.root, fileName);
		if (cacheDirectory.isFile()) {
			// hey ... there's a file where it should be a folder !!!
			// Cache is corrupted, try to repair it
			cacheDirectory.delete();
		}
		if (!cacheDirectory.exists()) {
			cacheDirectory.mkdirs();
		}
		String[] files = cacheDirectory.list();
		// If there's no cache file, return the default cache file
		if ((files==null) || (files.length==0)) return new File(cacheDirectory, CACHE_PREFIX+ZIP_SUFFIX);
		// There's at least one file in the cache, return the most recent (delete others)
		File result = null;
		for (String f : files) {
			File candidate = new File(cacheDirectory, f);
			if (isValidFile(f) && ((result==null) || (candidate.lastModified()>result.lastModified()))) {
				if (result!=null) result.delete();
				result = candidate;
			} else {
				candidate.delete();
			}
		}
		return result!=null?result:new File(cacheDirectory, CACHE_PREFIX+ZIP_SUFFIX);
	}
	
	private boolean isValidFile(String fileName) {
		return (fileName.startsWith(SYNCHRONIZED_CACHE_PREFIX) || fileName.startsWith(CACHE_PREFIX)) && fileName.endsWith(ZIP_SUFFIX);
	}
	
	/** Gets the file where the URI should be written.
	 * <br>File name is used to store the synchronization state of an uri.
	 * <br>So, when we update a file, we have store it in a file that will denote the updated data is not synchronized yet.
	 * <br>Later, the synchronization process will change the name of this file in order to mark it synchronized. 
	 * @param uri an URI
	 * @return a file
	 * @see #getLocalFile(URI)
	 */
	public final File getLocalFileForWriting(URI uri) {
		File file = getLocalFile(uri);
		if (file.getName().startsWith(SYNCHRONIZED_CACHE_PREFIX)) {
			String name = file.getName().substring(SYNCHRONIZED_CACHE_PREFIX.length());
			file = new File(file.getParent(), CACHE_PREFIX+name);
		}
		return file;
	}
	
	/** Gets the revision on which the local cache of an URI is based.
	 * <br>This revision was the remote one last time local and remote copies were successfully synchronized. 
	 * @param uri The URI.
	 * @return A String that identifies the revision or null if the local cache doesn't exist or was never been synchronized with the remote source.
	 * @throws IOException
	 */
	public final String getLocalRevision(URI uri) throws IOException {
		File file = getLocalFile(uri);
		if (!file.exists()) return null;
		String name = file.getName();
		String revision = name.substring(name.startsWith(CACHE_PREFIX) ? CACHE_PREFIX.length() : SYNCHRONIZED_CACHE_PREFIX.length());
		revision = revision.substring(0, revision.length()-ZIP_SUFFIX.length());
		return revision.length()==0?null:revision;
	}

	/** Sets the local cache revision of an URI.
	 * <br>At the end of the synchronization process, the local cache will be marked as having the same revision as the remote URI.
	 * @param uri the URI
	 * @param revision The new revision (should never be null).
	 */
	public final void setLocalRevision(URI uri, String revision) {
		File file = getLocalFile(uri);
		file.renameTo(new File(file.getParent(), SYNCHRONIZED_CACHE_PREFIX+revision+ZIP_SUFFIX));
	}
	
	/** Tests whether the local cache was synchronized.
	 * <br>The important word here is <b>"was"</b>. This means that this method do not connect to the remote service
	 * in order to compare the local and the remote revision.
	 * <br>In a scenario where the local file was synchronized, then the remote URI was updated later, this method will returns true whenever the local file
	 * is not a copy of the remote one. 
	 * @param uri The URI
	 * @return true if the file was synchronized.
	 */
	public final boolean isSynchronized(URI uri) {
		return getLocalFile(uri).getName().startsWith(SYNCHRONIZED_CACHE_PREFIX);
	}
	
	/** Gets the remote path of an entry.
	 * <br>By default, this method returns the local path preceded by a '/' and followed by ".zip", indicating that the file is zipped on the cloud storage.
	 * <br>If you override this method to change that, don't forget to override getRemoteEntry too. 
	 * @param entry The entry
	 * @return the remote path
	 * @see #getRemoteEntry(Account, String)
	 */
	public final String getRemotePath(Entry entry) {
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
	
	/** Gets the entries that are stored remotly by the service.
	 * @param account The account
	 * @param task A Cancellable instance that will report the progress or null.
	 * @return A collection of entries 
	 * @throws UnreachableHostException if the host is unreachable (the Internet connection is down).
	 * @throws InvalidConnectionDataException if the connection data of the account is refused by the server (for example, password if wrong).
	 */
	public abstract Collection<Entry> getRemoteEntries(Account account, Cancellable task) throws UnreachableHostException, InvalidConnectionDataException;
	
	/** Gets the URI fragment equivalent to some connection data.
	 * @param connectionData The connection data
	 * @return a string, an URI fragment.
	 * <br>As the fragment is a part of an URI, it should be encoded to not contain reserved characters.
	 * @see #getConnectionData(String)
	 */
	public abstract String getConnectionDataURIFragment(Serializable connectionData);
	
	/** Converts an URI fragment to connection data.
	 * <br>Connection data is data that is used to connect to an account (for example, with a ftp service, the user password).
	 * @param uriFragment The URI fragment.
	 * <br>Please note that the URI fragment is a part of an URI. So, it should have been encoded to remove URI reserved chars.
	 * @return The connection data
	 * @see #getConnectionDataURIFragment(Serializable)
	 */
	public abstract Serializable getConnectionData(String uriFragment);
	
	/** Gets the remote revision of an entry.
	 * <br>The remote revision is unique id that identifies the revision of a file.
	 * <br>There's no order relation between revisions.
	 * @param entry
	 * @return a String or null if the entry does not exist remotely
	 * @throws IOException 
	 */
	public abstract String getRemoteRevision(Entry entry) throws IOException;

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
	
	public String getMessage(String key, Locale locale) {
		return MessagePack.DEFAULT.getString(key, locale);
	}
}
