package com.fathzer.soft.jclop;

/** Signals that the connection data of the account was refused by the server (for example, password if wrong).
 * @author Jean-Marc Astesana
 */
public class InvalidConnectionDataException extends JClopException {
	private static final long serialVersionUID = 1L;

	public InvalidConnectionDataException(Throwable cause) {
		super(cause);
	}

}
