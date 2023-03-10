package com.fathzer.soft.jclop.swing;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Window;

import javax.swing.JButton;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;





import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.jlocal.Formatter;
import com.fathzer.soft.ajlib.swing.Utils;
import com.fathzer.soft.ajlib.swing.framework.Application;
import com.fathzer.soft.ajlib.swing.table.JTableListener;
import com.fathzer.soft.ajlib.swing.widget.ComboBox;
import com.fathzer.soft.ajlib.swing.widget.TextWidget;
import com.fathzer.soft.ajlib.swing.worker.WorkInProgressFrame;
import com.fathzer.soft.ajlib.utilities.NullUtils;
import com.fathzer.soft.jclop.Account;
import com.fathzer.soft.jclop.Entry;
import com.fathzer.soft.jclop.InvalidConnectionDataException;
import com.fathzer.soft.jclop.Service;
import com.fathzer.soft.jclop.UnreachableHostException;

/** An abstract chooser that provides a generic file chooser panel for every service instance.
 * <br>Limitations: This panel does not support folder creation or browsing. 
 */
@SuppressWarnings("serial")
public abstract class AbstractURIChooserPanel extends JPanel implements URIChooser {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractURIChooserPanel.class);
	private JPanel centerPanel;
	private JTable fileList;
	private JPanel filePanel;
	private JLabel lblFileName;
	private TextWidget fileNameField;
	
	private JLabel lblAccount;
	private JPanel northPanel;
	private JButton refreshButton;
	private JProgressBar progressBar;
	private FilesTableModel filesModel;
	private JScrollPane scrollPane;
	
	private JPanel panel;
	private ComboBox accountsCombo;
	private JButton newButton;
	private JButton deleteButton;
	private Service service;
	private String initedAccountId;
	private boolean linked;
	
	private IconPack icons;
	private URI selectedURI;
	private Entry pendingSelectedEntry;
	private boolean hasPendingSelected;
	private JLabel statusIcon;
	private JPanel panel1;
	
	/** Constructor.
	 * @param service The service that will be used by the chooser.
	 */
	public AbstractURIChooserPanel(Service service) {
		this.service = service;
		this.initedAccountId = null;
		this.linked = false;
		setIconPack(IconPack.DEFAULT);
		this.filesModel = new FilesTableModel();
		setLayout(new BorderLayout(0, 0));
		add(getNorthPanel(), BorderLayout.NORTH);
		add(getCenterPanel(), BorderLayout.CENTER);
	}
	
	@Override
	public void setSaveType(boolean save) {
		this.getFilePanel().setVisible(save);
	}
	
	private boolean isSaveType() {
		return this.getFilePanel().isVisible();
	}

	public URI showOpenDialog(Component parent, String title) {
		setSaveType(false);
		return showDialog(parent, title);
	}
	
	public URI showSaveDialog(Component parent, String title) {
		setSaveType(true);
		return showDialog(parent, title);
	}
	
	public URI showDialog(Component parent, String title) {
		Window owner = Utils.getOwnerWindow(parent);
		URIChooserDialog dialog = new URIChooserDialog(owner, title, new URIChooser[]{this});
		dialog.setSaveDialog(this.getFilePanel().isVisible());
		return dialog.showDialog();
	}
	
	/** Sets the icons used by this panel.
	 * @param pack The icon pack
	 */
	public void setIconPack(IconPack pack) {
		this.icons = pack;
		this.getNewButton().setIcon(getSizedIcon(this.icons.getNewAccount()));
		this.getDeleteButton().setIcon(getSizedIcon(this.icons.getDeleteAccount()));
		this.getRefreshButton().setIcon(getSizedIcon(this.icons.getSynchronize()));
		setStatusIcon();
	}
	
	private Icon getSizedIcon(Icon icon) {
		Image img = ((ImageIcon)icon).getImage();
		int fontSize = getFont().getSize();
		int DEFAULT_FONT_SIZE = 12;
		if (fontSize!=DEFAULT_FONT_SIZE) {
		  Image newimg = img.getScaledInstance(img.getWidth(this)*fontSize/DEFAULT_FONT_SIZE,
		  		img.getHeight(this)*fontSize/DEFAULT_FONT_SIZE, java.awt.Image.SCALE_SMOOTH);
		  icon = new ImageIcon(newimg);
		}
	  return icon;
	}

	private void setStatusIcon() {
		this.getStatusIcon().setIcon(getSizedIcon(this.linked?icons.getLinked():icons.getNotLinked()));
	}

	public void refresh(boolean force) {
		if (hasPendingSelected) {
			if (pendingSelectedEntry==null) {
				getFileNameField().setText(""); //$NON-NLS-1$
			} else {
				Entry entry = pendingSelectedEntry;
				Account account = getService().getAccount(entry.getAccount().getId());
			  //System.out.println("Refresh in hasPendingSelected, selection = "+getAccountsCombo().getSelectedIndex());
				if ((account==null) || (!account.equals(getAccountsCombo().getSelectedItem()))) {
				  //System.out.println("Account changed");
					if (account==null) {
						account = getService().newAccount(entry.getAccount().getId(), entry.getAccount().getDisplayName(), entry.getAccount().getConnectionData());
						boolean old = getAccountsCombo().isActionEnabled(); 
						getAccountsCombo().setActionEnabled(false);
						getAccountsCombo().addItem(account);
						getAccountsCombo().setActionEnabled(old);
					}
					getAccountsCombo().setSelectedItem(account);
				}
			}
		} else {
			if (getService().getAccounts().isEmpty()) {
				doNewAccount();
				return; // The doNewAccount() method will call refresh again
			}
		}

		Account account = (Account) getAccountsCombo().getSelectedItem();
		String accountId = account==null?null:account.getId();
		if (force || hasPendingSelected || (!NullUtils.areEquals(initedAccountId, accountId))) {
			initedAccountId = accountId;
			this.linked = false;
			Collection<Entry> entries = new TreeSet<Entry>();
			if (account!=null) {
				entries.addAll(account.getLocalEntries());
				final Window owner = Utils.getOwnerWindow(this);
				boolean eraseQuota = true;
				try {
					RemoteFileListWorker worker = new RemoteFileListWorker(account);
					worker.setPhase(service.getMessage(MessagePack.CONNECTING, getLocale()), -1);
					WorkInProgressFrame frame = new WorkInProgressFrame(owner, MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.GenericWait.title", getLocale()), ModalityType.APPLICATION_MODAL, worker); //$NON-NLS-1$
					frame.setSize(300, frame.getSize().height);
					frame.setLocationRelativeTo(owner);
					frame.setVisible(true); //$NON-NLS-1$
					entries.addAll(worker.get());
					this.linked = true;
					// Display quota data
					setQuota(account);
					eraseQuota = false;
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} catch (ExecutionException e) {
					if (e.getCause() instanceof UnreachableHostException) {
						getProgressBar().setValue(0);
						getProgressBar().setString(service.getMessage(MessagePack.CONNECTION_ERROR, getLocale()));
						eraseQuota = false;
					} else if (e.getCause() instanceof InvalidConnectionDataException) {
						LOGGER.error("Communication data is invalid !", e);
						System.out.println ("Should do something, connetion data is invalid"); //FIXME
					} else {
						LOGGER.error("communication error", e);
						showError(owner, service.getMessage(MessagePack.COMMUNICATION_ERROR, getLocale()), getLocale());
					}
				} catch (CancellationException e) {
					// The task was cancelled
				}
				if (eraseQuota) {
					setQuota(null);
				}
			}
			getStatusIcon().setVisible(account!=null);
			getProgressBar().setVisible(account!=null);
			// Update the file list
			filesModel.clear();
			for (Entry entry : entries) {
				Entry filtered = filter(entry);
				if (filtered!=null) {
					filesModel.add(entry);
				}
			}
			// Re-select the previously selected one (changing the model erases the selection)
			selectByFileName();
			// Set the sattus icon
			setStatusIcon();
		}
		
		if (hasPendingSelected) {
			hasPendingSelected = false;
			int index = isSaveType()?0:filesModel.indexOf(pendingSelectedEntry);
			if (index>=0) {
				getFileNameField().setText(pendingSelectedEntry.getDisplayName());
			}
		}
	}

	private void setQuota(Account account) {
		if ((account!=null) && (account.getQuota()>0) && (account.getUsed()>=0)) {
			long percentUsed = 100*(account.getUsed()) / account.getQuota(); 
			getProgressBar().setValue((int)percentUsed);
			double remaining = account.getQuota()-account.getUsed();
			String unit = MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Generic.data.unit.bytes", getLocale()); //$NON-NLS-1$
			if (remaining>1024) {
				unit = MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Generic.data.unit.kBytes", getLocale()); //$NON-NLS-1$
				remaining = remaining/1024;
				if (remaining>1024) {
					unit = MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Generic.data.unit.MBytes", getLocale()); //$NON-NLS-1$
					remaining = remaining/1024;
					if (remaining>1024) {
						unit = MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Generic.data.unit.GBytes", getLocale()); //$NON-NLS-1$
						remaining = remaining/1024;
					}
				}
			}
			getProgressBar().setString(Formatter.format(MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Chooser.freeSpace", getLocale()), new DecimalFormat("0.0").format(remaining), unit)); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			getProgressBar().setValue(0);
			getProgressBar().setString("?");
		}
	}
	
	private JPanel getCenterPanel() {
		if (centerPanel == null) {
			centerPanel = new JPanel();
			centerPanel.setLayout(new BorderLayout(0, 0));
			centerPanel.add(getScrollPane(), BorderLayout.CENTER);
			centerPanel.add(getFilePanel(), BorderLayout.SOUTH);
		}
		return centerPanel;
	}
	private JTable getFileList() {
		if (fileList == null) {
			fileList = new com.fathzer.soft.ajlib.swing.table.JTable(filesModel);
			fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			fileList.addMouseListener(new JTableListener(null, new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					AbstractURIChooserPanel.this.firePropertyChange(URI_APPROVED_PROPERTY, false, true);
				}
			}));
			fileList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if (!e.getValueIsAdjusting() && (getFileList().getSelectedRow()!=-1)) {
						getFileNameField().setText((String) filesModel.getValueAt(getFileList().getSelectedRow(), 0));
					}
				}
			});
		}
		return fileList;
	}
	private JPanel getFilePanel() {
		if (filePanel == null) {
			filePanel = new JPanel();
			filePanel.setLayout(new BorderLayout(0, 0));
			filePanel.add(getLblFileName(), BorderLayout.WEST);
			filePanel.add(getFileNameField(), BorderLayout.CENTER);
		}
		return filePanel;
	}
	private JLabel getLblFileName() {
		if (lblFileName == null) {
			lblFileName = new JLabel(MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Chooser.fileName", getLocale()));  //$NON-NLS-1$
		}
		return lblFileName;
	}
	private TextWidget getFileNameField() {
		if (fileNameField == null) {
			fileNameField = new TextWidget();
			fileNameField.setEditable(false);
			fileNameField.addPropertyChangeListener(TextWidget.TEXT_PROPERTY, new PropertyChangeListener() {	
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					int pos = fileNameField.getCaretPosition();
					selectByFileName();
					updateSelectedURI();
					pos = Math.min(pos, fileNameField.getText().length());
					fileNameField.setCaretPosition(pos);
				}
			});
		}
		return fileNameField;
	}
	
	private JLabel getLblAccount() {
		if (lblAccount == null) {
			lblAccount = new JLabel(MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Chooser.account", getLocale())); //$NON-NLS-1$
		}
		return lblAccount;
	}

	private JPanel getNorthPanel() {
		if (northPanel == null) {
			northPanel = new JPanel();
			GridBagLayout gblNorthPanel = new GridBagLayout();
			northPanel.setLayout(gblNorthPanel);
			GridBagConstraints gbcPanel = new GridBagConstraints();
			gbcPanel.weightx = 1.0;
			gbcPanel.fill = GridBagConstraints.BOTH;
			gbcPanel.insets = new Insets(0, 0, 0, 5);
			gbcPanel.gridx = 0;
			gbcPanel.gridy = 0;
			northPanel.add(getPanel(), gbcPanel);
			GridBagConstraints gbcRefreshButton = new GridBagConstraints();
			gbcRefreshButton.fill = GridBagConstraints.VERTICAL;
			gbcRefreshButton.gridheight = 1;
			gbcRefreshButton.gridx = 1;
			gbcRefreshButton.gridy = 0;
			northPanel.add(getRefreshButton(), gbcRefreshButton);
			GridBagConstraints gbcPanel1 = new GridBagConstraints();
			gbcPanel1.fill = GridBagConstraints.BOTH;
			gbcPanel1.gridwidth = 2;
			gbcPanel1.gridx = 0;
			gbcPanel1.gridy = 1;
			northPanel.add(getPanel1(), gbcPanel1);
		}
		return northPanel;
	}
	private JButton getRefreshButton() {
		if (refreshButton == null) {
			refreshButton = new JButton();
			refreshButton.setToolTipText(service.getMessage(MessagePack.REFRESH_TOOLTIP, getLocale()));  //$NON-NLS-1$
			refreshButton.setEnabled(getAccountsCombo().getItemCount()!=0);
			refreshButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					refresh(true);
				}
			});
		}
		return refreshButton;
	}
	private JProgressBar getProgressBar() {
		if (progressBar == null) {
			progressBar = new JProgressBar();
			progressBar.setStringPainted(true);
			progressBar.setString("?");
		}
		return progressBar;
	}

	/** Filters an entry.
	 * <br>By default, this method returns the entry path.
	 * @param entry The entry available in the current folder
	 * @return The entry that will be displayed in the files list, or null to ignore this entry
	 */
	protected Entry filter(Entry entry) {
		return entry;
	}
	
	private JScrollPane getScrollPane() {
		if (scrollPane == null) {
			scrollPane = new JScrollPane();
			scrollPane.setViewportView(getFileList());
			// Do not diplay column names
			getFileList().setTableHeader(null);
			scrollPane.setColumnHeaderView(null);
		}
		return scrollPane;
	}

	@Override
	public URI getSelectedURI() {
		return selectedURI;
	}
	
	@Override
	public void setSelectedURI(URI uri) {
		pendingSelectedEntry = uri==null?null:this.service.getEntry(uri);
		hasPendingSelected = true;
		if (isShowing()) {
			refresh(true);
		}
	}
	
	private JPanel getPanel() {
		if (panel == null) {
			panel = new JPanel();
			GridBagLayout gblPanel = new GridBagLayout();
			panel.setLayout(gblPanel);
			GridBagConstraints gbcLblAccount = new GridBagConstraints();
			gbcLblAccount.fill = GridBagConstraints.BOTH;
			gbcLblAccount.anchor = GridBagConstraints.EAST;
			gbcLblAccount.gridx = 0;
			gbcLblAccount.gridy = 0;
			panel.add(getLblAccount(), gbcLblAccount);
			GridBagConstraints gbcAccountsCombo = new GridBagConstraints();
			gbcAccountsCombo.weightx = 1.0;
			gbcAccountsCombo.fill = GridBagConstraints.BOTH;
			gbcAccountsCombo.gridx = 1;
			gbcAccountsCombo.gridy = 0;
			panel.add(getAccountsCombo(), gbcAccountsCombo);
			GridBagConstraints gbcBtnNewAccount = new GridBagConstraints();
			gbcBtnNewAccount.gridx = 2;
			gbcBtnNewAccount.gridy = 0;
			panel.add(getNewButton(), gbcBtnNewAccount);
			GridBagConstraints gbcDeleteButton = new GridBagConstraints();
			gbcDeleteButton.gridx = 3;
			gbcDeleteButton.gridy = 0;
			panel.add(getDeleteButton(), gbcDeleteButton);
		}
		return panel;
	}
	private ComboBox getAccountsCombo() {
		if (accountsCombo == null) {
			accountsCombo = new ComboBox();
			accountsCombo.setRenderer(new BasicComboBoxRenderer(){
				@Override
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					if (value!=null) {
						value = ((Account)value).getDisplayName();
					}
					return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				}
			});
			Collection<Account> accounts = getService().getAccounts();
			for (Account account : accounts) {
				accountsCombo.addItem(account);
			}

			accountsCombo.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					boolean oneIsSelected = doAccountSelectionChanged();
					refresh(oneIsSelected);
					String name = getFileNameField().getText();
					if ((name.length()>0) && (!oneIsSelected || (!isSaveType() && (selectByFileName()<0)))) {
						getFileNameField().setText(""); // Erases the current selection
					} else {
						updateSelectedURI();
					}
				}
			});
			doAccountSelectionChanged();
		}
		return accountsCombo;
	}
	private JButton getNewButton() {
		if (newButton == null) {
			newButton = new JButton();
			newButton.setToolTipText(MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Chooser.new.tooltip", getLocale())); //$NON-NLS-1$
			int height = getAccountsCombo().getPreferredSize().height;
			newButton.setPreferredSize(new Dimension(height, height));
			newButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					doNewAccount();
				}
			});
		}
		return newButton;
	}
	private JButton getDeleteButton() {
		if (deleteButton == null) {
			deleteButton = new JButton();
			deleteButton.setEnabled(false);
			deleteButton.setToolTipText(service.getMessage(MessagePack.DELETE_TOOLTIP, getLocale())); //$NON-NLS-1$
			int height = getAccountsCombo().getPreferredSize().height;
			deleteButton.setPreferredSize(new Dimension(height, height));
			deleteButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					boolean confirm = JOptionPane.showOptionDialog(Utils.getOwnerWindow(deleteButton), service.getMessage(MessagePack.DELETE_MESSAGE, getLocale()), MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Chooser.delete.message.title", getLocale()), //$NON-NLS-1$ //$NON-NLS-2$
							JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{service.getMessage(MessagePack.DELETE, getLocale()),Application.getString("GenericButton.cancel", getLocale())},1)==0; //$NON-NLS-1$ //$NON-NLS-2$
					if (confirm) {
						Account account = (Account) getAccountsCombo().getSelectedItem();
						getAccountsCombo().removeItemAt(getAccountsCombo().getSelectedIndex());
						getService().delete(account);
						getFileNameField().setEditable(getAccountsCombo().getItemCount()>0);
					}
				}
			});
		}
		return deleteButton;
	}

	/** Create a new account.
	 * <br>This method should ask the user for the account's data then call getService().newAccount to create the new account.
	 * <br>Be aware that duplicate account ids are not allowed. If the user selects an existing account, it is recommended to
	 * update its attributes (serialization data, display name, etc).
	 * @return the new account or an updated existing one or null if the user aborted the creation.
	 * @see Service#newAccount(String, String, java.io.Serializable)
	 */
	protected abstract Account createNewAccount();

	@Override
	public Service getService() {
		return service;
	}

	@Override
	public String getScheme() {
		return service.getScheme();
	}
	
	/** Gets the chooser title.
	 * <br>By default, returns getScheme().
	 * @see #getScheme()
	 * @see AbstractURIChooserPanel#getTitle()
	 */
	@Override
	public String getTitle() {
		return getScheme();
	}

	@Override
	public void setUp() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				refresh(false);
			}
		});
	}

	@Override
	public boolean isSelectedExist() {
		// If the selectedFile exists, it is selected in the file list as there's a listener on the file name field
		return getFileList().getSelectedRow()>=0;
	}
	
	@Override
	public String getDisabledCause() {
		return null;
	}
	
//	private void serialize(Account account) {
//		try {
//			account.serialize();
//		} catch (IOException e) {
//			showError(Utils.getOwnerWindow(getNewButton()), MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Error.unableToSerializeAccount", getLocale()), getLocale());
//		}
//	}

	/** Gets the index of the file which name is in the file name field and select/deselect in in the file list.
	 * @return an integer >= 0 if the fileName is in the file list
	 */
	private int selectByFileName() {
		int index = -1;
		for (int rowIndex=0;rowIndex<filesModel.getRowCount();rowIndex++) {
			if (filesModel.getValueAt(rowIndex, 0).equals(fileNameField.getText())) {
				index = rowIndex;
				break;
			}
		}
		ListSelectionModel selectionModel = getFileList().getSelectionModel();
		if (index<0) {
			selectionModel.clearSelection();
		} else {
			selectionModel.setSelectionInterval(index, index);
		}
		return index;
	}

	/** Adjusts button appearance according to the selected account.
	 * @return true if an account is selected 
	 */
	private boolean doAccountSelectionChanged() {
		boolean oneIsSelected = getAccountsCombo().getSelectedIndex()>=0;
		getDeleteButton().setEnabled(oneIsSelected);
		getRefreshButton().setEnabled(oneIsSelected);
		getFileNameField().setEditable(oneIsSelected);
		return oneIsSelected;
	}

	public static void showError(Window owner, String message, Locale locale) {
		JOptionPane.showMessageDialog(owner, message, MessagePack.DEFAULT.getString(MessagePack.ERROR_TITLE, locale), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
	}
	private JLabel getStatusIcon() {
		if (statusIcon == null) {
			statusIcon = new JLabel();
		}
		return statusIcon;
	}
	private JPanel getPanel1() {
		if (panel1 == null) {
			panel1 = new JPanel();
			panel1.setLayout(new BorderLayout(0, 0));
			panel1.add(getStatusIcon(), BorderLayout.WEST);
			panel1.add(getProgressBar());
		}
		return panel1;
	}

	/** Updates the selected URI accordingly to the panel's content.
	 * <br>Fire the appropriate property change event if the uri has changed.
	 */
	public boolean updateSelectedURI() {
		URI old = selectedURI;
		String name = getFileNameField().getText();
		Account account = (Account) getAccountsCombo().getSelectedItem();
		selectedURI = ((account==null) || (name.length()==0))?null:getService().getURI(new Entry(account, name));
		if (!NullUtils.areEquals(selectedURI, old)) {
			firePropertyChange(SELECTED_URI_PROPERTY, old, getSelectedURI());
		}
		return true;
	}

	private void doNewAccount() {
		Account account = null;
		account = createNewAccount();
		if (account!=null) {
			if (!getAccountsCombo().contains(account)) {
				boolean old = getAccountsCombo().isActionEnabled();
				getAccountsCombo().setActionEnabled(false);
				getAccountsCombo().addItem(account);
				getAccountsCombo().setActionEnabled(old);
			}
			getAccountsCombo().setSelectedItem(account);
		}
	}
}
