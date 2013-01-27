package com.fathzer.soft.jclop.swing;

import javax.swing.AbstractAction;
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
import java.awt.Window;

import javax.swing.JButton;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import net.astesana.ajlib.swing.widget.ComboBox;
import net.astesana.ajlib.swing.Utils;
import net.astesana.ajlib.swing.framework.Application;
import net.astesana.ajlib.swing.table.JTableListener;
import net.astesana.ajlib.swing.widget.TextWidget;
import net.astesana.ajlib.swing.worker.WorkInProgressFrame;
import net.astesana.ajlib.utilities.NullUtils;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import com.fathzer.soft.jclop.Account;
import com.fathzer.soft.jclop.Entry;
import com.fathzer.soft.jclop.InvalidConnectionDataException;
import com.fathzer.soft.jclop.Service;
import com.fathzer.soft.jclop.UnreachableHostException;

@SuppressWarnings("serial")
public abstract class AbstractURIChooserPanel extends JPanel implements URIChooser {
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
	private JPanel panel_1;
	
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
		this.getNewButton().setIcon(this.icons.getNewAccount());
		this.getDeleteButton().setIcon(this.icons.getDeleteAccount());
		this.getRefreshButton().setIcon(this.icons.getSynchronize());
		setStatusIcon();
	}

	private void setStatusIcon() {
		this.getStatusIcon().setIcon(this.linked?icons.getLinked():icons.getNotLinked());
	}

	public void refresh(boolean force) {
		boolean isNewAccount = false;
		if (hasPendingSelected) {
			if (pendingSelectedEntry==null) {
				getFileNameField().setText(""); //$NON-NLS-1$
			} else {
				Entry entry = pendingSelectedEntry;
				boolean old = getAccountsCombo().isActionEnabled(); 
				getAccountsCombo().setActionEnabled(false);
//System.out.println("Refresh in hasPendingSelected, selection = "+getAccountsCombo().getSelectedIndex()); //TODO
				if (!getAccountsCombo().contains(entry.getAccount())) {
					getAccountsCombo().addItem(entry.getAccount());
					isNewAccount = true;
				}
				getAccountsCombo().setSelectedItem(entry.getAccount());
				getAccountsCombo().setActionEnabled(old);
				doAccountSelectionChanged();
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
						System.out.println ("Should do something, connetion data is invalid"); //FIXME
//						showError(owner, MessagePack.COMMUNICATION_ERROR, getLocale());
					} else {
						showError(owner, MessagePack.COMMUNICATION_ERROR, getLocale());
					}
				} catch (CancellationException e) {
					// The task was cancelled
				}
				if (eraseQuota) setQuota(null);
			}
			getStatusIcon().setVisible(account!=null);
			getProgressBar().setVisible(account!=null);
			// Update the file list
			filesModel.clear();
			for (Entry entry : entries) {
				Entry filtered = filter(entry);
				if (filtered!=null) filesModel.add(entry);
			}
			// Re-select the previously selected one (changing the model erases the selection)
			selectByFileName();
			// Set the sattus icon
			setStatusIcon();
		}
		
		if (hasPendingSelected) {
			hasPendingSelected = false;
			int index = isSaveType()?0:filesModel.indexOf(pendingSelectedEntry);
			if (index>=0) getFileNameField().setText(pendingSelectedEntry.getDisplayName());
			if (isNewAccount) {
				serialize(account);
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
			getProgressBar().setString(MessageFormat.format(MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Chooser.freeSpace", getLocale()), new DecimalFormat("0.0").format(remaining), unit)); //$NON-NLS-1$ //$NON-NLS-2$
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
			fileList = new JTable(filesModel);
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
					if (!e.getValueIsAdjusting()) {
						if (getFileList().getSelectedRow()!=-1) {
							getFileNameField().setText((String) filesModel.getValueAt(getFileList().getSelectedRow(), 0));
						}
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
			GridBagLayout gbl_northPanel = new GridBagLayout();
			northPanel.setLayout(gbl_northPanel);
			GridBagConstraints gbc_panel = new GridBagConstraints();
			gbc_panel.weightx = 1.0;
			gbc_panel.fill = GridBagConstraints.BOTH;
			gbc_panel.insets = new Insets(0, 0, 0, 5);
			gbc_panel.gridx = 0;
			gbc_panel.gridy = 0;
			northPanel.add(getPanel(), gbc_panel);
			GridBagConstraints gbc_refreshButton = new GridBagConstraints();
			gbc_refreshButton.gridheight = 1;
			gbc_refreshButton.gridx = 1;
			gbc_refreshButton.gridy = 0;
			northPanel.add(getRefreshButton(), gbc_refreshButton);
			GridBagConstraints gbc_panel_1 = new GridBagConstraints();
			gbc_panel_1.fill = GridBagConstraints.BOTH;
			gbc_panel_1.gridwidth = 2;
			gbc_panel_1.gridx = 0;
			gbc_panel_1.gridy = 1;
			northPanel.add(getPanel_1(), gbc_panel_1);
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

	public URI getSelectedURI() {
		return selectedURI;
	}
	
	public void setSelectedURI(URI uri) {
		pendingSelectedEntry = uri==null?null:this.service.getEntry(uri);
		hasPendingSelected = true;
		if (isShowing()) refresh(true);
	}
	
	private JPanel getPanel() {
		if (panel == null) {
			panel = new JPanel();
			GridBagLayout gbl_panel = new GridBagLayout();
			panel.setLayout(gbl_panel);
			GridBagConstraints gbc_lblAccount = new GridBagConstraints();
			gbc_lblAccount.fill = GridBagConstraints.BOTH;
			gbc_lblAccount.anchor = GridBagConstraints.EAST;
			gbc_lblAccount.gridx = 0;
			gbc_lblAccount.gridy = 0;
			panel.add(getLblAccount(), gbc_lblAccount);
			GridBagConstraints gbc_accountsCombo = new GridBagConstraints();
			gbc_accountsCombo.weightx = 1.0;
			gbc_accountsCombo.fill = GridBagConstraints.BOTH;
			gbc_accountsCombo.gridx = 1;
			gbc_accountsCombo.gridy = 0;
			panel.add(getAccountsCombo(), gbc_accountsCombo);
			GridBagConstraints gbc_btnNewAccount = new GridBagConstraints();
			gbc_btnNewAccount.gridx = 2;
			gbc_btnNewAccount.gridy = 0;
			panel.add(getNewButton(), gbc_btnNewAccount);
			GridBagConstraints gbc_deleteButton = new GridBagConstraints();
			gbc_deleteButton.gridx = 3;
			gbc_deleteButton.gridy = 0;
			panel.add(getDeleteButton(), gbc_deleteButton);
		}
		return panel;
	}
	private ComboBox getAccountsCombo() {
		if (accountsCombo == null) {
			accountsCombo = new ComboBox();
			accountsCombo.setRenderer(new BasicComboBoxRenderer(){
				@Override
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					if (value!=null) value = ((Account)value).getDisplayName();
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
					Account account = createNewAccount();
					if (account!=null) {
						// Test if account is already there
						for (int i = 0; i < getAccountsCombo().getItemCount(); i++) {
							if (((Account)getAccountsCombo().getItemAt(i)).getDisplayName().equals(account.getDisplayName())) {
								getAccountsCombo().setSelectedIndex(i);
								return;
							}
						}
						// Save the account data to disk
						serialize(account);
						getAccountsCombo().addItem(account);
						getAccountsCombo().setSelectedItem(account);
					}
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
						account.delete();
						getFileNameField().setEditable(getAccountsCombo().getItemCount()>0);
					}
				}
			});
		}
		return deleteButton;
	}

	protected abstract Account createNewAccount();

	public Service getService() {
		return service;
	}

	/* (non-Javadoc)
	 * @see net.astesana.ajlib.swing.dialog.urichooser.AbstractURIChooserPanel#getSchemes()
	 */
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

	/* (non-Javadoc)
	 * @see net.astesana.ajlib.swing.dialog.urichooser.AbstractURIChooserPanel#setUp()
	 */
	@Override
	public void setUp() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				refresh(false);
			}
		});
	}

	/* (non-Javadoc)
	 * @see net.astesana.ajlib.swing.dialog.urichooser.AbstractURIChooserPanel#exist(java.net.URI)
	 */
	@Override
	public boolean isSelectedExist() {
		// If the selectedFile exists, it is selected in the file list as there's a listener on the file name field
		return getFileList().getSelectedRow()>=0;
	}
	
	@Override
	public String getDisabledCause() {
		return null;
	}
	
	private void serialize(Account account) {
		try {
			account.serialize();
		} catch (IOException e) {
			showError(Utils.getOwnerWindow(getNewButton()), MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Error.unableToSerializeAccount", getLocale()), getLocale());
		}
	}

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
		JOptionPane.showMessageDialog(owner, message, MessagePack.DEFAULT.getString("com.fathzer.soft.jclop.Error.title", locale), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
	}
	private JLabel getStatusIcon() {
		if (statusIcon == null) {
			statusIcon = new JLabel();
		}
		return statusIcon;
	}
	private JPanel getPanel_1() {
		if (panel_1 == null) {
			panel_1 = new JPanel();
			panel_1.setLayout(new BorderLayout(0, 0));
			panel_1.add(getStatusIcon(), BorderLayout.WEST);
			panel_1.add(getProgressBar());
		}
		return panel_1;
	}

	/** Updates the selected URI accordingly to the panel's content.
	 * <br>Fire the appropriate property change event if the uri has changed.
	 */
	private void updateSelectedURI() {
		URI old = selectedURI;
		String name = getFileNameField().getText();
		Account account = (Account) getAccountsCombo().getSelectedItem();
		selectedURI = ((account==null) || (name.length()==0))?null:getService().getURI(new Entry(account, name));
		if (!NullUtils.areEquals(selectedURI, old)) firePropertyChange(SELECTED_URI_PROPERTY, old, getSelectedURI());
	}
}
