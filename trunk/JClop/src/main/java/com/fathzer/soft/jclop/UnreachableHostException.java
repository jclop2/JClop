package com.fathzer.soft.jclop;

/** Signals that the remote host is not reachable.
 * <br>The most common cause is "there is no Internet connection".
 * @author Jean-Marc Astesana
 */
public class UnreachableHostException extends JClopException {
	private static final long serialVersionUID = 1L;

	public UnreachableHostException(Throwable cause) {
		super(cause);
	}
}
