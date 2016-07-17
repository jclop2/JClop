package com.fathzer.soft.jclop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.soft.ajlib.utilities.FileUtils;
import com.fathzer.soft.ajlib.utilities.NullUtils;
import com.fathzer.soft.jclop.swing.MessagePack;

/** A persistence service.
 * <br>There's two kinds of services :<ul>
 * <li>Cloud services, with accounts synchronized with a local cache.</li>
 * <li>Local services, that typically store data onto disk</li>
 * </ul>
 * <br>Limitation: Account can't contain folder.
 * @author Jean-Marc Astesana
 * Licence GPL v3
 */
public abstract class Service {
	private static final Logger LOGGER = LoggerFactory.getLogger(Service.class);
	public static final String UTF_8 = "UTF-8";
	static final String ZIP_SUFFIX = ".zip"; //$NON-NLS-1$
	static final String FILE_PREFIX = "f_";
	private static final String CACHE_PREFIX = "cache"; //$NON-NLS-1$
	private static final String SYNCHRONIZED_CACHE_PREFIX = "sync"; //$NON-NLS-1$

	public static final String URI_DOMAIN = "cloud.jclop.fathzer.com";

	private File root;
	private boolean local;
	private Collection<Account> accounts;

	/** Constructor.
	 * @param root The root folder of the services (the place where all accounts of all services are cached).<br>
	 * null if the service doesn't need any cache.
	 * @param local true if the service is a local one, false if it stores its data in the cloud.
	 * <br>Each service can use a folder in the root folder, where all the accounts it manages are cached.
	 * <br>This folder will be named with the service scheme.
	 * <br>For example, if your root is "/home/user/.MyApp/cache" and getScheme() returns "MyService",
	 * the cached data will be placed in "/home/user/.MyApp/cache/MyService".
	 * @throws IOException If an error occurs while accessing to the local cache
	 * @throws IllegalArgumentException if it is not possible to create the service folder (or service folder exists but is a file, not a folder)
	 * @see #getScheme()  
	 */
	protected Service(File root, boolean local) throws IOException {
		this.local = local;
		if (!local) {
			root = new File(root, getScheme());
			if (!root.exists()) {
				if (!root.mkdirs()) {
					throw new IOException("Unable to create cache directory");
				}
			}
			if (!root.isDirectory()) {
				throw new IllegalArgumentException();
			}
			this.root = root;
			/** Forces the account list to be rebuild from the file cache content.
			 * <br>This method doesn't call the cloud service to get the list of remote accounts
			 */
			File[] files = root.listFiles();
			accounts = new ArrayList<Account>();
			for (File file : files) {
				if (file.isDirectory()) {
					try {
						accounts.add(new Account(this, file));
					} catch (Exception e) {
						// Something is wrong in the account folder, ignore it
						LOGGER.warn(file+" is ignored", e);
					}
				}
			}
		}
	}
	
	public final boolean isLocal() {
		return local;
	}
	
	/** Creates a new account.
	 * @param id The account's id
	 * @param displayName The account's display name
	 * @param connectionData The data necessary to connect with the account
	 * @return The created account
	 * @throws IllegalArgumentException if an account with the same id already exists
	 */
	public synchronized Account newAccount(String id, String displayName, Serializable connectionData) {
		if (id==null) {
			throw new NullPointerException();
		}
		for (Account acc : accounts) {
			if (acc.getId().equals(id)) {
				throw new IllegalArgumentException(); 
			}
		}
		Account account = new Account(this, id, displayName, connectionData);
		account.serialize();
		accounts.add(account);
		return account;
	}
	
	/** Deletes an account.
	 * @param account The account to delete. If the account doesn't exist, this method does nothing.
	 */
	public synchronized void delete(Account account) {
		for (Account acc : accounts) {
			if (acc.getId().equals(account.getId())) {
				FileUtils.deleteDirectory(account.getRoot());
				accounts.remove(account);
				break;
			}
		}
	}
	
	/** Gets the available accounts.
	 * @return A collection of available accounts
	 */
	public final synchronized Collection<Account> getAccounts() {
		return accounts;
	}
	
	File getCacheRoot() {
		return root;
	}

	/** Get the entry corresponding to a local cache file.
	 * @param account The account where the path is located.
	 * @param file The local cache file (actually it's the cache folder related to a remote file) 
	 * @return an entry or null if the entry is not a valid cache file.
	 */
	final Entry getLocalEntry(Account account, File file) {
		try {
			// If the file is not starting with the file prefix, ignore it.
			if (!file.getName().startsWith(FILE_PREFIX)) {
				return null;
			}
			String[] files = file.list();
			boolean ok = false;
			if (files!=null) {
				for (String fileName : files) {
					if (isValidFile(fileName)) {
						ok = true;
						break;
					}
				}
			}
			// If the folder contains no valid file, try to repair the cache folder (delete the file)
			//if (!ok) file.delete(); // Not sure it was a good idea
			return ok?new Entry(account, URLDecoder.decode(file.getName().substring(FILE_PREFIX.length()), UTF_8)) : null;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/** Gets the local file where is stored the cached data.
	 * @param uri an URI
	 * @return a File
	 */
	public File getLocalFile(URI uri) {
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
		if ((files==null) || (files.length==0)) {
			return new File(cacheDirectory, CACHE_PREFIX+ZIP_SUFFIX);
		}
		// There's at least one file in the cache, return the most recent (delete others)
		File result = null;
		for (String f : files) {
			File candidate = new File(cacheDirectory, f);
			if (isValidFile(f) && ((result==null) || (candidate.lastModified()>result.lastModified()))) {
				if (result!=null) {
					result.delete();
				}
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
	 * <br>So, when we update a file, we have to store it in a file that will denote that updated data is not synchronized yet.
	 * <br>Later, the synchronization process will change the name of this file in order to mark it synchronized. 
	 * @param uri an URI
	 * @return a file
	 * @see #getLocalFile(URI)
	 */
	public final File getLocalFileForWriting(URI uri) {
		File file = getLocalFile(uri);
		File parentFile = file.getParentFile();
		if (file.getName().startsWith(SYNCHRONIZED_CACHE_PREFIX)) {
			String name = file.getName().substring(SYNCHRONIZED_CACHE_PREFIX.length());
			file = new File(parentFile, CACHE_PREFIX+name);
		}
		//Be aware that there could be some corrupted data in the cache folder.
		//We will try to repair it (example: the parent folder should be a file, not a folder !)
		if (parentFile.isFile()) {
			parentFile.delete();
		}
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		return file;
	}
	
	/** Gets the revision on which the local cache of an URI is based.
	 * <br>This revision was the remote one last time local and cloud copies were successfully synchronized. 
	 * @param uri The URI.
	 * @return A String that identifies the revision or null if the local cache doesn't exist or was never been synchronized with the cloud source.
	 */
	public final String getLocalRevision(URI uri) {
		if (local) {
			return getLocalFile(uri).exists() ? "1":null;
		}
		File file = getLocalFile(uri);
		if (!file.exists()) {
			return null;
		}
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
		return local || getLocalFile(uri).getName().startsWith(SYNCHRONIZED_CACHE_PREFIX);
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
	 * <br>If remote path does not end with ".zip", it returns null in order to have this file ignored. 
	 * <br>If you override this method to change that behavior, don't forget to override getRemotePath too. 
	 * @param account The account
	 * @param remotePath The remote path
	 * @return An entry or null if the entry should be ignored
	 * @see #getRemoteEntries(Account, Cancellable)
	 * @see #getRemotePath(Entry)
	 */
	protected Entry getRemoteEntry(Account account, String remotePath) {
		if (!remotePath.endsWith(ZIP_SUFFIX)) {
			return null;
		}
		remotePath = remotePath.substring(0, remotePath.length()-ZIP_SUFFIX.length());
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
	 * <br>By default, this method returns the uri without the account id, the connection data and the URI domain.
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
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			builder.append(URLEncoder.encode(path, UTF_8));
			return builder.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/** Gets the scheme of uri managed by this service.
	 * <br>Files are identified by uri. This method returns the scheme of the uri of files managed by this service.
	 * You're free to define what scheme you want but scheme is considered as a unique id for services.
	 * @return the service uri scheme
	 */
	public abstract String getScheme();

	/** Gets an Entry from its URI.
	 * @param uri An URI
	 * @return an Entry.
	 * @throws IllegalArgumentException if the uri is not supported or has a wrong format
	 */
	public abstract Entry getEntry(URI uri);
	
	/** Gets the entries that are stored remotely by the cloud service.
	 * @param account The account
	 * @param task A Cancellable instance that will report the progress or null.
	 * @return A collection of entries 
	 * @throws JClopException if something goes wrong.
	 * @see Account#getLocalEntries()
	 */
	public abstract Collection<Entry> getRemoteEntries(Account account, Cancellable task) throws JClopException;
	
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
	
	/** Gets the remote revision of an URI.
	 * <br>The remote revision is unique id that identifies the revision of a file.
	 * <br>There's no order relation between revisions.
	 * @param uri The uri
	 * @return a String or null if the entry does not exist remotely
	 * @throws JClopException if something goes wrong.
	 */
	public abstract String getRemoteRevision(URI uri) throws JClopException;

	/** Downloads data from a cloud uri.
	 * @param uri The entry to download.
	 * @param out The stream where to download.
	 * @param task The task that ask the download or null if no cancellable task is provided. Please make sure to report the progress and cancel the download if the task is cancelled.
	 * @param locale The locale that will be used to set the name of task phases. This argument can be null if task is null too.
	 * @return true if the upload is done, false if it was cancelled
	 * @throws JClopException if something goes wrong while accessing the URI.
	 * @throws IOException if something goes wrong while writing to the output stream.
	 */
	public abstract boolean download(URI uri, OutputStream out, Cancellable task, Locale locale) throws JClopException, IOException;
	
	/** Uploads data to a cloud destination uri.
	 * @param in The inputStream from which to read to uploaded bytes
	 * @param length The number of bytes to upload
	 * @param uri The URI where to upload.
	 * @param task The task that ask the download or null if no cancellable task is provided. Please make sure to report the progress and cancel the upload if the task is cancelled.
 	 * @param locale The locale that will be used to set the name of task phases. This argument can be null if task is null too.
	 * @return true if the upload is done, false if it was cancelled
	 * @throws JClopException if something goes wrong while accessing the URI.
	 * @throws IOException if something goes wrong while reading from the input stream.
	 */
	public abstract boolean upload(InputStream in, long length, URI uri, Cancellable task, Locale locale) throws JClopException, IOException;
	
	/** Downloads data from a cloud uri to a cache file.
	 * Whatever is the synchronization state, this method forces the remote file to replace current cached file.
	 * @param uri The entry to download.
	 * @param task The task that ask the download or null if no cancellable task is provided. Please make sure to report the progress and cancel the download if the task is cancelled.
	 * @param locale The locale that will be used to set the name of task phases. This argument can be null if task is null too.
	 * @return true if the upload is done, false if it was cancelled
	 * @throws JClopException if something goes wrong while accessing the URI.
	 * @throws IOException if something goes wrong while writing to the local cache.
	 */
	public final boolean download(URI uri, Cancellable task, Locale locale) throws JClopException, IOException {
		if (local) {
			return true;
		}
		File file = getLocalFile(uri);
		file.getParentFile().mkdirs();
		String revision = null;
		String downloadedRevision = ""; //$NON-NLS-1$
		// We do not download directly to the target file, to prevent file from being corrupted if the copy fails
		File tmpFile = new File(file.getParent(), file.getName()+".tmp"); //$NON-NLS-1$
		boolean done = true;
		while (done && !NullUtils.areEquals(revision, downloadedRevision)) {
			// While the downloaded revision is not the last one on the server (maybe the remote file is updated while we download it)
			downloadedRevision = getRemoteRevision(uri);
			OutputStream out = new FileOutputStream(tmpFile);
			try {
				done = download(uri, out, task, locale);
			} finally {
				out.close();
			}
			revision = getRemoteRevision(uri);
		}
		if (done) {
			file.delete();
			tmpFile.renameTo(file);
			setLocalRevision(uri, revision);
		} else {
			tmpFile.delete();
		}
		return done;
	}

	/** Uploads an URI from the cache to the remote service.
	 * Whatever is the synchronization state, this method forces the cached file to replace current remote file.
	 * @param uri The URI to upload
	 * @param task A cancellable to report the progress or cancel the task.
 	 * @param locale The locale that will be used to set the name of task phases. This argument can be null if task is null too.
	 * @return true if the uri was successfully uploaded
	 * @throws JClopException if something goes wrong while accessing the URI.
	 * @throws IOException if something goes wrong while reading from the local cache.
	 */
	public final boolean upload(URI uri, Cancellable task, Locale locale) throws JClopException, IOException {
		if (local) {
			return true;
		}
		File file = getLocalFile(uri);
		long length = file.length();
		boolean done = false;
		FileInputStream stream = new FileInputStream(file);
		try {
			done = upload(stream, length, uri, task, locale);
		} finally {
			stream.close();
		}
		if (done) {
			setLocalRevision(uri, getRemoteRevision(uri));
		}
		return done;
	}
	
	/** Synchronizes local cache and remote resource.
	 * @param uri The remote URI
	 * @return The synchronization state
	 * @throws FileNotFoundException if neither the remote resource nor its cache file does exist 
	 * @throws JClopException if something goes wrong while accessing the URI.
	 * @throws IOException if something goes wrong while accessing the local cache.
	 */
	public SynchronizationState synchronize(URI uri, Cancellable task, Locale locale) throws JClopException, IOException {
		String remoteRevision = getRemoteRevision(uri);
		String localRevision = getLocalRevision(uri);
//System.out.println("remote rev: "+remoteRevision+", local rev:"+localRevision);
		File file = getLocalFile(uri);
		if (remoteRevision==null) {
			// If remote uri doesn't exist
			if (!file.exists()) {
				// The local cache doesn't exist
				throw new FileNotFoundException();
			}
			if (localRevision==null) {
				// The local cache was never synchronized -> Upload the cache to server
				upload(uri, task, locale); 
				return SynchronizationState.SYNCHRONIZED;
			} else {
				// Remote was deleted
				return SynchronizationState.REMOTE_DELETED;
			}
		} else {
			// The remote uri exists
			if (remoteRevision.equals(localRevision)) {
				// Cache and remote have the same origin 
				if (isSynchronized(uri)) {
					// The cache and the remote are the same
					return SynchronizationState.SYNCHRONIZED;
				} else {
					// cache was changed but not yet uploaded
					upload(uri, task, locale);
					return SynchronizationState.SYNCHRONIZED;
				}
			} else {
				// Cache and remote have not the same origin
				if (!file.exists()) { // The local cache doesn't exist
					download(uri, task, locale);
					return SynchronizationState.SYNCHRONIZED;
				} else {
					// The local cache exists
					if (isSynchronized(uri)) {
						// The local cache was already synchronized
						// This means the cloud has been modified after the cache was synchronized
						download(uri, task, locale);
						return SynchronizationState.SYNCHRONIZED;
					} else {
						// The local cache was not synchronized with the remote uri
						return SynchronizationState.CONFLICT;
					}
				}
			}
		}
	}

	public String getMessage(String key, Locale locale) {
		return MessagePack.DEFAULT.getString(key, locale);
	}

	/** Deletes the local cache of an uri.
	 * <br>If the local cache doesn't exists, does nothing.
	 * @param uri
	 */
	public void deleteLocal(URI uri) {
		FileUtils.deleteDirectory(getLocalFile(uri).getParentFile());
	}

	/** Gets an account by its id.
	 * @param id The account id
	 * @return An account or null if the account is unknown
	 */
	public synchronized Account getAccount(String id) {
		for (Account account : accounts) {
			if (account.getId().equals(id)) {
				return account;
			}
		}
		return null;
	}
}
