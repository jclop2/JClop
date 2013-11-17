package com.fathzer.soft.jclop;

/** The result of a synchronization process.
 * @author Jean-Marc Astesana
 * Licence GPL v3
 */
public enum SynchronizationState {
		/** Remote data and local cache are synchronized. */
		SYNCHRONIZED,
		/** A local cache exists, but remote resource doesn't exist. */
		REMOTE_DELETED,
		/** Both remote data and local cache have been modified since their last synchronization. */
		CONFLICT
	}