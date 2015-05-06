package jadex.bdiv3x.runtime;

import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.IMessageFeature;

import java.util.logging.Logger;

/**
 *  Dummy class for loading v2 examples using v3x.
 */
public abstract class Plan
{
	/** The internal access. */
	protected IInternalAccess agent;
	
	/**
	 *  The body method is called on the
	 *  instantiated plan instance from the scheduler.
	 */
	public abstract void body();

	/**
	 *  The passed method is called on plan success.
	 */
	public void	passed()
	{
	}

	/**
	 *  The failed method is called on plan failure/abort.
	 */
	public void	failed()
	{
	}

	/**
	 *  The plan was aborted (because of conditional goal
	 *  success or termination from outside).
	 */
	public void aborted()
	{
	}
	
	/**
	 *  Wait for a some time.
	 *  @param duration The duration.
	 */
	public void	waitFor(int timeout)
	{
		agent.getComponentFeature(IExecutionFeature.class).waitForDelay(timeout).get();
	}
	
	/**
	 *  Wait for a message event.
	 *  @param type The message event type.
	 */
	public IMessageEvent waitForMessageEvent(String type)
	{
		return waitForMessageEvent(type, -1);
	}

	/**
	 *  Wait for a message event.
	 *  @param type The message event type.
	 *  @param timeout The timeout.
	 */
	public IMessageEvent waitForMessageEvent(String type, long timeout)
	{
		IMessageFeature mf = agent.getComponentFeature(IMessageFeature.class);
//		mf.
		return null;
	}
	
	/**
	 *  Kill this agent.
	 */
	public void	killAgent()
	{
		agent.killComponent();
	}
	
	/**
	 *  Get the logger.
	 *  @return The logger.
	 */
	public Logger getLogger()
	{
		return agent.getLogger();
	}
	
	/**
	 *  Get the beliefbase.
	 *  @return The beliefbase.
	 */
	public IBeliefbase getBeliefbase()
	{
		return null;
	}
}
