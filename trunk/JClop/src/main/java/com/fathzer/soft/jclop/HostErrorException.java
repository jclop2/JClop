package com.fathzer.soft.jclop;

/** Signals that the remote host had an internal error.
 * @author Jean-Marc Astesana
 */
public class HostErrorException extends JClopException {
	private static final long serialVersionUID = 1L;

	public HostErrorException(Throwable cause) {
		super(cause);
	}
}
