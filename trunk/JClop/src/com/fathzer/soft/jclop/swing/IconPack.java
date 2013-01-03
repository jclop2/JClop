package com.fathzer.soft.jclop.swing;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/** The icon pack used by URIChooser class.
 * @author Jean-Marc Astesana (License : GPL v3)
 */
public final class IconPack {
	public static final IconPack DEFAULT = new IconPack();
	
	private Icon newAccount; 
	private Icon deleteAccount; 
	private Icon synchronize; 
	private Icon linked; 
	private Icon notLinked; 
	
	public IconPack() {}

	public Icon getSynchronize() {
		if (synchronize==null) {
			synchronize = new ImageIcon(IconPack.class.getResource("synchronize.png")); //$NON-NLS-1$
		}
		return synchronize;
	}

	public Icon getNewAccount() {
		if (newAccount==null) {
			newAccount = new ImageIcon(IconPack.class.getResource("new.png")); //$NON-NLS-1$
		}
		return newAccount;
	}

	public Icon getDeleteAccount() {
		if (deleteAccount==null) {
			deleteAccount = new ImageIcon(IconPack.class.getResource("delete.png")); //$NON-NLS-1$
		}
		return deleteAccount;
	}

	public Icon getLinked() {
		if (linked==null) {
			linked = new ImageIcon(IconPack.class.getResource("linked.png")); //$NON-NLS-1$
		}
		return linked;
	}

	public Icon getNotLinked() {
		if (notLinked==null) {
			notLinked = new ImageIcon(IconPack.class.getResource("notLinked.png")); //$NON-NLS-1$
		}
		return notLinked;
	}

	/** Sets the "new Account" button icon
	 * @param icon the icon to set
	 */
	public void setNewAccount(Icon icon) {
		this.newAccount = icon;
	}

	/** Sets the "delete Account" button icon
	 * @param icon the deleteAccount to set
	 */
	public void setDeleteAccount(Icon icon) {
		this.deleteAccount = icon;
	}

	/** Sets the "synchronize" button icon
	 * @param icon the synchronize to set
	 */
	public void setSynchronize(Icon icon) {
		this.synchronize = icon;
	}

	/** Sets the "linked" icon
	 * @param icon the synchronize to set
	 */
	public void setLinked(Icon icon) {
		this.linked = icon;
	}

	/** Sets the "not linked" icon
	 * @param icon the synchronize to set
	 */
	public void setNotLinked(Icon icon) {
		this.notLinked = icon;
	}
	
}
