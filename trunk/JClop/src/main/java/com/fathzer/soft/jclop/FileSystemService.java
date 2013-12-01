package com.fathzer.soft.jclop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;




import org.slf4j.LoggerFactory;

import com.fathzer.soft.ajlib.utilities.FileUtils;
import com.fathzer.soft.jclop.swing.FileChooserPanel;

public class FileSystemService extends Service {
	protected FileSystemService() {
		super(null, true);
	}

	public static final FileSystemService INSTANCE = new FileSystemService();
	
	@Override
	public String getScheme() {
		return FileChooserPanel.SCHEME;
	}

	@Override
	public File getLocalFile(URI uri) {
		File file = new File(uri);
		try {
			return FileUtils.getCanonical(file);
		} catch (IOException e) {
			LoggerFactory.getLogger(getClass()).warn("Unable to find canonical file for "+uri, e);
			return file;
		}
	}

	@Override
	public String getDisplayable(URI uri) {
		return new File(uri).getPath();
	}

	@Override
	public Collection<Entry> getRemoteEntries(Account account, Cancellable task) throws UnreachableHostException, InvalidConnectionDataException {
		return Collections.emptyList();
	}

	@Override
	public String getConnectionDataURIFragment(Serializable connectionData) {
		return null;
	}

	@Override
	public Serializable getConnectionData(String uriFragment) {
		return null;
	}

	@Override
	public String getRemoteRevision(URI uri) throws JClopException {
		return getLocalRevision(uri);
	}

	@Override
	public boolean download(URI uri, OutputStream out, Cancellable task, Locale locale) throws IOException {
		return true;
	}

	@Override
	public boolean upload(InputStream in, long length, URI uri, Cancellable task, Locale locale) throws IOException {
		return true;
	}
}
