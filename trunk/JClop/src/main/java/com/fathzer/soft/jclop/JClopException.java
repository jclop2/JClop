package com.fathzer.soft.jclop;

import java.io.IOException;

/** Encapsulates all exceptions that occurs while communication with the remote host.
 * <br>All exceptions throwned by service extends this class.
 * @author Jean-Marc Astesana
 */
public abstract class JClopException extends IOException {
	private static final long serialVersionUID = 1L;

	public JClopException(Throwable cause) {
		super(cause);
	}
}
