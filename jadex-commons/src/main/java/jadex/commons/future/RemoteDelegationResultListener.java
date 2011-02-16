package jadex.commons.future;


/**
 *  Result listener that delegates calls to a future
 *  and can be called from remote.
 */
public class RemoteDelegationResultListener implements IRemoteResultListener
{
	//-------- attributes --------
	
	/** The future to which calls are delegated. */
	protected Future future;
	
	//-------- constructors --------
	
	/**
	 *  Create a new listener.
	 */
	public RemoteDelegationResultListener(Future future)
	{
		this.future = future;
	}
	
	//-------- methods --------
	
	/**
	 *  Called when the result is available.
	 * @param result The result.
	 */
	public final void resultAvailable(Object result)
	{
		try
		{
			customResultAvailable(result);
		}
		catch(Exception e)
		{
			// Could happen that overridden customResultAvailable method
			// first sets result and then throws exception (listener ex are catched).
			future.setExceptionIfUndone(e);
		}
	}
	
	/**
	 *  Called when the result is available.
	 * @param result The result.
	 */
	public void customResultAvailable(Object result)
	{
		future.setResult(result);
	}

	/**
	 *  Called when an exception occurred.
	 * @param exception The exception.
	 */
	public void exceptionOccurred(Exception exception)
	{
		future.setException(exception);
	}
}
