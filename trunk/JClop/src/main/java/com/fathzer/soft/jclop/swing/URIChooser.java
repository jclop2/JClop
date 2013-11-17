package com.fathzer.soft.jclop.swing;

import java.net.URI;

import javax.swing.Icon;

/** A component that allows the user to select a destination where to save/read his data.
 * <br><b><u>WARNING</u>:Although there is no indication that the class must be a subclass of Component, this is mandatory.</b>
 * @author Jean-Marc Astesana
 * <BR>License : GPL v3
 * @see MultipleURIChooserPanel
 */
public interface URIChooser {
	/** The name of the selected uri property.
	 * <br>This component should fire a property change event when the selected uri is modified.
	 * @see #getSelectedURI() 
	 */
	public static final String SELECTED_URI_PROPERTY = "selectedUri"; //$NON-NLS-1$
	/** The name of the approved uri property.
	 * <br>This component should fire a property change event when the selected uri is approved by the user
	 * (For example, if the user has double clicked an URI). This will cause the dialog to be validated.
	 * <br>Some may wonder why not using the same ActionEvent way as in JFileChooser. The reason is sending
	 * a property change event is far more easy to implement because it is supported directly by JPanel.
	 * fireAction is quite hard to code.
	 */
	public static final String URI_APPROVED_PROPERTY = "uriApproved"; //$NON-NLS-1$
	
	/** Gets the uri scheme (file, ftp, http, ...) that this component supports.
	 * @return a string
	 */
	public String getScheme();

	/** Gets the title of the panel.
	 * <br>This title will be used as tab name by the URIChooser. 
	 * @return a String
	 */
	public String getTitle();
	
	/** Gets the tooltip of the panel.
	 * <br>It will be used as tab tooltip by the URIChooser. 
	 * @param save true to have the save tooltip, false to have the "open" tooltip
	 * @return a String
	 */
	public abstract String getTooltip(boolean save);
	
	/** Gets the icon of the panel.
	 * <br>It will be used as tab icon by the URIChooser. 
	 * @return a String
	 */
	public abstract Icon getIcon();

	/** Gets the currently selected URI.
	 * @return an URI or null if no destination is currently selected
	 */
	public URI getSelectedURI();
	
	/** Sets the currently selected URI.
	 * <br>Please note that this method may be (and usually is) called when the component is not showing yet
	 * (when constructing the dialog containing this chooser).
	 * So, it should not call a remote server (and display a wait dialog) to verify that the URI exists.
	 * A good implementation is to store the URI in a temporary variable and perform the check during the setUp method.
	 * <br>Please note that this could result in getSelectedURI not being updated before the dialog is visible. 
	 * @param uri an URI
	 * @throws IllegalArgumentException if the URI is not supported by the underlying service
	 * @see #setUp()
	 */
	public void setSelectedURI(URI uri);

	/** Sets the dialog type (save or read).
	 * @param save true if the dialog is for saving data, false for reading data.
	 */
	public void setSaveType(boolean save);
	
	/** Sets up the panel.
	 * <br>This method is called the each time the panel is becoming selected.
	 * <br>It is the good place to set up the panel (connect to servers, for instance).
	 * <br>If the set up is a time consuming task, it is a good practice to use SwingUtilities.invokeLater
	 * in this method to perform the setup, in order the component to be displayed fast.
	 */
	public void setUp();
	
	/** Tests whether or not the currently selected URI exists.
	 * @return true if the selected URI exists, false if not or if no URI is selected
	 */
	public boolean isSelectedExist();

	/** Gets the reason why the currently selected URI is invalid.
	 * <br>The chooser may have selected URI that are invalid.
	 * <br>For example, the FileChooserURI, inherited from the java JFileChooser is able to select non existing
	 * file in "open" mode. It also allows to select read or write protected files.
	 * <br>The FileChooserPanel implements this method in order to refuse such URI. 
	 * @return a String or null is the current selected URI is valid.
	 * @see FileChooserPanel
	 */
	public String getDisabledCause();
}
