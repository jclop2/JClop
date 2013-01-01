package com.fathzer.soft.jclop.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.Locale;

import javax.swing.JPanel;

import net.astesana.ajlib.swing.dialog.AbstractDialog;
import net.astesana.ajlib.swing.dialog.FileChooser;

@SuppressWarnings("serial")
public class URIChooserDialog extends AbstractDialog<URIChooser[], URI> {
	private MultipleURIChooserPanel multiplePanel;
	private boolean saveDialog;
	
	/** Constructor.
	 * <br>The created instance is an open dialog. You may call setSaveDialog if you need a save dialog
	 * @param owner The owner window of the dialog
	 * @param title The dialog's title
	 * @param choosers The abstract URI choosers. The elements of this array should be subclasses of java.awt.Component
	 */
	public URIChooserDialog(Window owner, String title, URIChooser[] choosers) {
		super(owner, title, choosers);
		saveDialog = false; // To force setSaveDialog to do something
		setSaveDialog(false);
	}
	
	private URIChooser getSelectedPanel() {
		if (multiplePanel==null) return this.data[0];
		return (URIChooser) multiplePanel.getSelectedComponent();
	}

	@Override
	protected JPanel createCenterPane() {
		addWindowListener(new WindowAdapter() {
			/* (non-Javadoc)
			 * @see java.awt.event.WindowAdapter#windowOpened(java.awt.event.WindowEvent)
			 */
			@Override
			public void windowOpened(WindowEvent e) {
				getSelectedPanel().setUp();
			}
		});
		PropertyChangeListener selectListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				updateOkButtonEnabled();
			}
		};
		PropertyChangeListener confirmListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ((Boolean)evt.getNewValue()) {
					confirm();
				}
			}
		};
		JPanel result = new JPanel(new BorderLayout());
		this.multiplePanel = data.length==1 ? null : new MultipleURIChooserPanel(data);
		Component cp = multiplePanel==null ? (Component) data[0] : multiplePanel;
		result.add(cp, BorderLayout.CENTER);
		cp.addPropertyChangeListener(URIChooser.SELECTED_URI_PROPERTY, selectListener);
		cp.addPropertyChangeListener(MultipleURIChooserPanel.URI_APPROVED_PROPERTY, confirmListener);
		return result;
	}

	@Override
	protected URI buildResult() {
		return getSelectedURI();
	}

	@Override
	protected String getOkDisabledCause() {
		if (getSelectedURI()==null) return MessagesPack.getString("URIChooserDialog.noFileSelected", getLocale()); //$NON-NLS-1$
		return null;
	}

	/* (non-Javadoc)
	 * @see net.astesana.ajlib.swing.dialog.AbstractDialog#confirm()
	 */
	@Override
	protected void confirm() {
		URI selectedURI = getSelectedURI();
		boolean exists = selectedURI!=null && this.saveDialog && getSelectedPanel().isSelectedExist();
		if (exists && FileChooser.showSaveDisplayQuestion(this)) return;
		super.confirm();
	}
	
	private URI getSelectedURI() {
		URIChooser panel = getSelectedPanel();
		return panel!=null?panel.getSelectedURI():null;
	}
	
	/** Sets the dialog's type.
	 * @param save true for a save dialog, false for an open dialog
	 */
	public void setSaveDialog(boolean save) {
		if (save!=saveDialog) {
			getOkButton().setText(save?MessagesPack.getString("URIChooserDialog.saveButton.title", getLocale()):MessagesPack.getString("URIChooserDialog.openButton.title", getLocale())); //$NON-NLS-1$ //$NON-NLS-2$
			for (URIChooser panel : data) {
				panel.setSaveType(save);
			}
		}
	}

	/** Shows the dialog and gets its result.
	 * @return an URI
	 */
	public URI showDialog() {
		setVisible(true);
		return getResult();
	}

	/** Sets the current uri.
	 * @param uri an uri or null to clear the currently selected uri
	 */
	public void setSelectedURI(URI uri) {
		URIChooser panel = null;
		if (uri!=null) {
			String scheme = uri.getScheme();
			for (URIChooser aPanel : data) {
				if (aPanel.getScheme().equals(scheme)) {
					panel = aPanel;
				}
			}
			if (panel==null) throw new IllegalArgumentException(); // No panel with this scheme
		} else {
			panel = data[0];
		}
		panel.setSelectedURI(uri);
		if ((multiplePanel!=null) && (uri!=null)) {
			multiplePanel.setSelectedComponent((Component) panel);
		}
	}
	
//	public void setLocale(Locale locale) {
//		super.setLocale(locale);
//		if (data!=null) {
//			System.out.println (getLocale());
//			for (URIChooser chooser : data) {
//				chooser.setLocale(locale);
//			}
//		}
//	}
}
