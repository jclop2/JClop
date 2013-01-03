package com.fathzer.soft.jclop.swing;

import net.astesana.ajlib.utilities.LocalizationData;

public class MessagePack {
	public static final String KEY_PREFIX = "com.fathzer.soft.jclop"; //$NON-NLS-1$
	public static final String DEFAULT_BUNDLE_NAME = "com.fathzer.soft.jclop.swing.messages"; //$NON-NLS-1$
	public static LocalizationData DEFAULT = new LocalizationData(DEFAULT_BUNDLE_NAME);
	
	public static final String DELETE = "com.fathzer.soft.jclop.Chooser.delete";
	public static final String DELETE_MESSAGE = "com.fathzer.soft.jclop.Chooser.delete.message";
	public static final String DELETE_TOOLTIP = "com.fathzer.soft.jclop.Chooser.delete.tooltip";
	public static final String REFRESH_TOOLTIP = "com.fathzer.soft.jclop.Chooser.refresh.tooltip";
	public static final String CONNECTING = "com.fathzer.soft.jclop.connecting";
	public static final String CONNECTION_ERROR = "com.fathzer.soft.jclop.connectionFailed";
	public static final String UPLOADING = "com.fathzer.soft.jclop.uploading";
	public static final String DOWNLOADING = "com.fathzer.soft.jclop.downloading";
}
