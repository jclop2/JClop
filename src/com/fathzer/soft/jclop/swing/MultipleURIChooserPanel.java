package com.fathzer.soft.jclop.swing;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;

import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.astesana.ajlib.utilities.NullUtils;

/** An URI chooser.
 * <br>This is a extension to the JFileChooser concept. It allows the user to select not only files,
 * but uris that can be of any scheme (ftp, http, etc ...), even non standard schemes (for example, yapbam project
 * implements a dropbox scheme).
 * @author Jean-Marc Astesana
 * <BR>License : GPL v3
 */
@SuppressWarnings("serial")
class MultipleURIChooserPanel extends JTabbedPane {
	public static final String SELECTED_URI_PROPERTY = URIChooser.SELECTED_URI_PROPERTY;
	public static final String URI_APPROVED_PROPERTY = URIChooser.URI_APPROVED_PROPERTY;

	private URI selectedURI;
	private boolean isSave;
	/** The last tab that was set up (used to prevent a tab from being setup again when the selected uri is set before this component is made visible) */
	private int lastSetup;
	
	/**
	 * Creates the chooser.
	 */
	public MultipleURIChooserPanel(URIChooser[] choosers) {
		this.lastSetup = -1;
		setTabPlacement(JTabbedPane.TOP);
		for (URIChooser uiChooser:choosers) {
			addTab(uiChooser.getTitle(), uiChooser.getIcon(), (Component)uiChooser, null);
			((Component)uiChooser).addPropertyChangeListener(URIChooser.SELECTED_URI_PROPERTY, new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					URI old = selectedURI;
					selectedURI = (URI) evt.getNewValue();
					if (!NullUtils.areEquals(old, selectedURI))	{
						firePropertyChange(SELECTED_URI_PROPERTY, old, selectedURI);
//					System.out.println (this+" "+SELECTED_URI_PROPERTY+": "+old+" -> "+selectedURI);
					}
				}
			});
			((Component)uiChooser).addPropertyChangeListener(URIChooser.URI_APPROVED_PROPERTY, new PropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					firePropertyChange(URI_APPROVED_PROPERTY, evt.getOldValue(), evt.getNewValue());
				}
			});
		}
		setSaveType(false);
		ChangeListener listener = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				URI old = selectedURI;
				boolean hasSelectedTab = getSelectedComponent()!=null;
				selectedURI = hasSelectedTab?((URIChooser)getSelectedComponent()).getSelectedURI():null;
				if (!NullUtils.areEquals(old, selectedURI)) firePropertyChange(SELECTED_URI_PROPERTY, old, selectedURI);
				if (hasSelectedTab && isShowing()) setUp(getSelectedIndex());
			}
		};
		addChangeListener(listener);
	}
	
	void setUp(int index) {
		if (lastSetup!=index) {
			lastSetup = index;
			((URIChooser)getComponent(index)).setUp();
		}
	}

	public void setSaveType(boolean save) {
		this.isSave = save;
		for (int i = 0; i < this.getTabCount(); i++) {
			URIChooser tab = (URIChooser)this.getComponentAt(i);
			this.setToolTipTextAt(i, tab.getTooltip(save));
			tab.setSaveType(save);
		}
	}
	
	public boolean isSaveType() {
		return isSave;
	}

	/** Gets the currently selected URI.
	 * @return an URI, or null if the user selected nothing.
	 */
	public URI getSelectedURI() {
		return selectedURI;
	}

	/** Sets the current uri.
	 * @param uri
	 */
	public void setSelectedURI(URI uri) {
		if (uri!=null) {
			String scheme = uri.getScheme();
			for (int i=0; i<getComponentCount(); i++) {
				URIChooser panel = (URIChooser)getComponent(i);
				if (panel.getScheme().equals(scheme)) {
					setSelectedIndex(i);
					panel.setSelectedURI(uri);
					break;
				}
			}
		} else {
			((URIChooser)getSelectedComponent()).setSelectedURI(uri);
		}
	}
}
