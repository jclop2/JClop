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

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.fathzer.soft.ajlib.swing.dialog.AbstractDialog;
import com.fathzer.soft.ajlib.swing.dialog.FileChooser;
import com.fathzer.soft.ajlib.swing.framework.Application;


@SuppressWarnings("serial")
public class URIChooserDialog extends AbstractDialog<URIChooser[], URI> {
	public interface ConfirmButtonUpdater {
		/** Sets the title of dialog's confirm button.
		 * @param button The confirm button
		 * @param selectedURI The currently selected URI
		 * @param existing true if this URI exists
		 * @return true if the updater has made the whole job, false if the dialog should apply the default naming
		 */
		public boolean update(JButton button, URI selectedURI, boolean existing);
	}
	
	private MultipleURIChooserPanel multiplePanel;
	private boolean saveDialog;
	private boolean confirmIfExisting;
	private ConfirmButtonUpdater updater = null;
	
	/** Constructor.
	 * <br>The created instance is an open dialog. You may call setSaveDialog if you need a save dialog
	 * @param owner The owner window of the dialog
	 * @param title The dialog's title
	 * @param choosers The abstract URI choosers. The elements of this array should be subclasses of java.awt.Component
	 */
	public URIChooserDialog(Window owner, String title, URIChooser[] choosers) {
		super(owner, title, choosers);
		saveDialog = false; // To force setSaveDialog to do something
		this.confirmIfExisting = true;
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
				if ((updater==null) || (!updater.update(getOkButton(), getSelectedURI(), getSelectedPanel().isSelectedExist()))) {
					setOkButtonTitle();
				}
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
		if (getSelectedURI()==null) {
			return MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.URIChooserDialog.noFileSelected", getLocale()); //$NON-NLS-1$
		}
		return getSelectedPanel().getDisabledCause();
	}

	@Override
	protected void confirm() {
		URI selectedURI = getSelectedURI();
		boolean exists = this.confirmIfExisting && selectedURI!=null && this.saveDialog && getSelectedPanel().isSelectedExist();
		if (exists && FileChooser.showSaveDisplayQuestion(this)) return;
		String error = getSelectedPanel().getDisabledCause();
		if (error!=null) {
			JOptionPane.showMessageDialog(this, error, Application.getString("Generic.error", getLocale()), JOptionPane.ERROR_MESSAGE);  //$NON-NLS-1$
			return;
		}
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
			this.saveDialog = save;
			if ((updater==null) || (updater.update(getOkButton(), getSelectedURI(), getSelectedPanel().isSelectedExist()))) {
				setOkButtonTitle();
			}
			for (URIChooser panel : data) {
				panel.setSaveType(save);
			}
		}
	}
	
	/** Sets the ok button title.
	 * <br>This method is called when setSaveDialog attribute is changed.
	 */
	private void setOkButtonTitle() {
		String title;
		if (saveDialog) {
			title = MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.URIChooserDialog.saveButton.title", getLocale()); //$NON-NLS-1$
		} else {
			title = MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.URIChooserDialog.openButton.title", getLocale()); //$NON-NLS-1$
		}
		getOkButton().setText(title);
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

	/* (non-Javadoc)
	 * @see java.awt.Component#setLocale(java.util.Locale)
	 */
	@Override
	public void setLocale(Locale locale) {
		super.setLocale(locale);
		if (data!=null) {
			for (URIChooser chooser : data) {
				((Component)chooser).setLocale(locale);
			}
		}
		//FIXME The wording of buttons should be updated
	}
	
	/** Sets whether there should be a confirm dialog when selecting an existing file in save mode.
	 * @param confirm true (which is the default) to have a confirm dialog, false to skip this dialog
	 */
	public void setConfirmIfExisting(boolean confirm) {
		this.confirmIfExisting = confirm;
	}
	
	public void setConfirmButtonUpdater(ConfirmButtonUpdater updater) {
		this.updater = updater;
	}
}
