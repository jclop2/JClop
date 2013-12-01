package com.fathzer.soft.jclop.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import com.fathzer.soft.ajlib.swing.dialog.FileChooser;
import com.fathzer.soft.ajlib.utilities.NullUtils;


/** An AbstractURIChooserPanel that allows the user to select a file.
 * @author Jean-Marc Astesana
 * <BR>License : GPL v3
 */
@SuppressWarnings("serial")
public class FileChooserPanel extends JPanel implements URIChooser {
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
//System.out.println ("FileChooser selection changed from "+evt.getOldValue()+" to "+evt.getNewValue());
				File oldFile = (File) evt.getOldValue();
				URI oldURI = oldFile==null?null:oldFile.toURI();
				File newFile = (File) evt.getNewValue();
				URI newURI;
				if (newFile==null || newFile.isDirectory() || (getFileChooser().getDialogType()==JFileChooser.OPEN_DIALOG && !newFile.isFile())) {
					newURI = null;
				} else {
					newURI = newFile.toURI();
				}
				if (!NullUtils.areEquals(oldURI, newURI)) {
					firePropertyChange(SELECTED_URI_PROPERTY, oldURI, newURI);
				}
			}
		});
	}
	
	@Override
	public String getTitle() {
		return MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.FileChooserPanel.title", getLocale()); //$NON-NLS-1$
	}

	@Override
	public String getTooltip(boolean save) {
		return MessagePack.DEFAULT.getString(save?"com.fathzer.soft.jclop.FileChooserPanel.tooltip.save":"com.fathzer.soft.jclop.FileChooserPanel.tooltip.open", getLocale()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public Icon getIcon() {
		return new ImageIcon(getClass().getResource("computer.png")); //$NON-NLS-1$
	}

	private FileChooser getFileChooser() {
		if (fileChooser == null) {
			fileChooser = new FileChooser();
			fileChooser.setSelectionTestEnabled(false);
			fileChooser.setControlButtonsAreShown(false);
			fileChooser.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
						firePropertyChange(URI_APPROVED_PROPERTY, false, true);
					}
				}
			});
		}
		return fileChooser;
	}

	@Override
	public URI getSelectedURI() {
		File selectedFile = getFileChooser().getSelectedFile();
		return selectedFile==null?null:selectedFile.toURI();
	}

	@Override
	public void setUp() {
		// Nothing to do
	}

	@Override
	public void setSaveType(boolean save) {
		this.fileChooser.setDialogType(save?FileChooser.SAVE_DIALOG:FileChooser.OPEN_DIALOG);
	}

	@Override
	public void setSelectedURI(URI uri) {
		if (uri==null) {
			if (fileChooser.getSelectedFile()!=null) {
				fileChooser.setSelectedFiles(null);
			}
		} else {
			File file = new File(uri);
			if (getFileChooser().getDialogType()==JFileChooser.OPEN_DIALOG) {
				if (file.isFile()) {
					// File exists and is a not a directory
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
		File selectedFile = getFileChooser().getSelectedFile();
		return (selectedFile!=null) && selectedFile.exists();
	}

	@Override
	public String getDisabledCause() {
		return getFileChooser().getDisabledCause();
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#setLocale(java.util.Locale)
	 */
	@Override
	public void setLocale(Locale locale) {
		getFileChooser().setLocale(locale);
		super.setLocale(locale);
	}
}
