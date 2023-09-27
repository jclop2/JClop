package com.fathzer.soft.jclop;

/** A class that can report progress of an action and cancel it. 
 */
public interface Cancellable {
	/** This method is called when a new phase of the action is started.
	 * @param phase The phase name
	 * @param max The maximum the progress of the phase can be. A negative value if this maximum is unknown.
	 */
	public void setPhase(String phase, int max);
	
	public void setMax(int length);

	/** This method is called during the phase to notice this Cancellable of the progress of the current phase.
	 * <br>Please note that some phase could never call this method, even if a positive value was passed to {@link #setPhase(String, int)}
	 * @param progress current progress, a value between 0 and the value passed to {@link #setPhase(String, int)}
	 */
	public void reportProgress(int progress);
	
	/** Tests whether the action is cancelled.
	 * @return true if the action is cancelled.
	 */
	public boolean isCancelled();

	/** Sets the task this class should execute to cancel the action.
	 * <br>This method is called before starting the action.
	 * @param cancelTask The task that this class should call to cancel the action
	 */
	public void setCancelAction(Runnable cancelTask);
}
