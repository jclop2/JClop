package com.fathzer.soft.jclop.swing;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import net.astesana.ajlib.swing.dialog.FileChooser;
import net.astesana.ajlib.swing.framework.Application;

/** An AbstractURIChooserPanel that allows the user to select a file.
 * @author Jean-Marc Astesana
 * <BR>License : GPL v3
 */
@SuppressWarnings("serial")
public class FileChooserPanel extends JPanel implements AbstractURIChooserPanel {
	public static final String SCHEME = "file";  //$NON-NLS-1$
	private FileChooser fileChooser;

	/** Constructor.
	 */
	public FileChooserPanel() {
		setLayout(new BorderLayout(0, 0));
		add(getFileChooser(), BorderLayout.CENTER);
		getFileChooser().addPropertyChangeListener(FileChooser.SELECTED_FILE_CHANGED_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				File oldFile = (File) evt.getOldValue();
				URI oldURI = oldFile==null?null:oldFile.toURI();
				File newFile = (File) evt.getNewValue();
				URI newURI;
				if (newFile==null || newFile.isDirectory() || (getFileChooser().getDialogType()==JFileChooser.OPEN_DIALOG && !newFile.isFile())) {
					newURI = null;
				} else {
					newURI = newFile.toURI();
				}
				firePropertyChange(SELECTED_URI_PROPERTY, oldURI, newURI);
			}
		});
	}
	
	@Override
	public String getTitle() {
		return Application.getString("FileChooserPanel.title"); //$NON-NLS-1$
	}

	@Override
	public String getTooltip(boolean save) {
		return Application.getString(save?"FileChooserPanel.tooltip.save":"FileChooserPanel.tooltip.open"); //$NON-NLS-1$
	}

	@Override
	public Icon getIcon() {
		return new ImageIcon(getClass().getResource("computer.png")); //$NON-NLS-1$
	}

	private JFileChooser getFileChooser() {
		if (fileChooser == null) {
			fileChooser = new FileChooser() {
				public void approveSelection() {
					doApproveSelection();
				}
			};
			fileChooser.setControlButtonsAreShown(false);
		}
		return fileChooser;
	}
	
	private void doApproveSelection() {
		firePropertyChange(URI_APPROVED_PROPERTY, false, true);
	}

	@Override
	public URI getSelectedURI() {
		File selectedFile = getFileChooser().getSelectedFile();
		return selectedFile==null?null:selectedFile.toURI();
	}

	@Override
	public void setUp() {}

	@Override
	public void setDialogType(boolean save) {
		this.fileChooser.setDialogType(save?FileChooser.SAVE_DIALOG:FileChooser.OPEN_DIALOG);
	}

	@Override
	public void setSelectedURI(URI uri) {
		if (uri==null) {
			fileChooser.setSelectedFile(null);
		} else {
			File file = new File(uri);
			if (getFileChooser().getDialogType()==JFileChooser.OPEN_DIALOG) {
				if (file.isFile()) { // File exists and is a not a directory
					fileChooser.setSelectedFile(file);
				}
			} else {
				if (!file.isDirectory()) {
					fileChooser.setSelectedFile(file);
				}
			}
		}
	}

	@Override
	public String getScheme() {
		return SCHEME;
	}

	@Override
	public boolean isSelectedExist() {
		return getFileChooser().getSelectedFile().exists();
	}
}
