package com.fathzer.soft.jclop.swing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

import com.fathzer.soft.jclop.Account;
import com.fathzer.soft.jclop.Cancellable;
import com.fathzer.soft.jclop.Entry;

import net.astesana.ajlib.swing.worker.Worker;

final class RemoteFileListWorker extends Worker<Collection<Entry>, Void> implements Cancellable {
		private final Account account;
		private final Collection<Entry> result;

		RemoteFileListWorker(Account account) {
			this.account = account;
			this.result = new TreeSet<Entry>();
		}

		@Override
		protected Collection<Entry> doProcessing() throws Exception {
			if (account==null) {
				return new ArrayList<Entry>(0);
			} else {
				result.addAll(account.getLocalFiles());
				if (!isCancelled()) result.addAll(account.getRemoteFiles(this));
				return result;
			}
		}

		/* (non-Javadoc)
		 * @see net.astesana.ajlib.swing.worker.Worker#setPhase(java.lang.String, int)
		 */
		@Override
		public void setPhase(String phase, int phaseLength) {
			super.setPhase(phase, phaseLength);
		}

		@Override
		public void setCancelAction(Runnable action) {
		}
	}