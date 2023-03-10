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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.soft.ajlib.utilities.FileUtils;
import com.fathzer.soft.jclop.swing.FileChooserPanel;

public class FileSystemService extends Service {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemService.class);

	protected FileSystemService() throws IOException {
		super(null, true);
	}

	public static final FileSystemService INSTANCE;
	
	static {
		FileSystemService instance = null;
		try {
			instance = new FileSystemService();
		} catch (IOException e) {
			LOGGER.error("Unexpected exception", e);
		}
		INSTANCE = instance;
	}
	
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
			LOGGER.warn("Unable to find canonical file for "+uri, e);
			return file;
		}
	}

	@Override
	public String getDisplayable(URI uri) {
		return uri.getPath();
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

	@Override
	public Entry getEntry(URI uri) {
		if (!uri.getScheme().equals(getScheme())) {
			throw new IllegalArgumentException();
		}
		return new Entry(null, uri.getPath());
	}
}
