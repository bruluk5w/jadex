package jadex.commons.concurrent;


/**
 *  Counter listener
 */
public class CounterListener implements IResultListener
{
	//-------- attributes --------
	
	/** The listener to call when all callbacks have been received. */
	protected IResultListener listener;
	
	/** The number of sub listeners to wait for. */
	protected int num;
	
	/** The number of received callbacks. */
	protected int cnt;
	
	/** Boolean indicating if an exception occurred. */
	protected Exception exception;
	
	/**
	 *  Create a new counter listener.
	 *  @param num The number of sub callbacks.
	 */
	public CounterListener(int num, IResultListener listener)
	{
		assert num>0;
		assert listener!=null;
		
		this.num = num;
		this.listener = listener;
	}
	
	/**
	 *  Called when the result is available.
	 *  @param result The result.
	 */
	public synchronized void resultAvailable(Object source, Object result)
	{
//		System.out.println("here: "+cnt+" "+num);
		if(++cnt==num)
		{
//			System.out.println("!!!");
			// todo: what about aggregated result?
			listener.resultAvailable(source, result);
		}
	}
	
	/**
	 *  Called when an exception occurred.
	 *  @param exception The exception.
	 */
	public synchronized void exceptionOccurred(Object source, Exception exception)
	{
		// On first exception transfer exception.
		listener.exceptionOccurred(source, exception);
	}
}
