package jadex.bridge;

import java.util.Map;

import jadex.bridge.service.types.monitoring.IMonitoringEvent;

/**
 * 
 */
public class BulkMonitoringEvent implements IMonitoringEvent
{
	/** The bulk events. */
	protected IMonitoringEvent[] events;
	
	/**
	 * 
	 */
	public BulkMonitoringEvent()
	{
	}
	
	/**
	 * 
	 */
	public BulkMonitoringEvent(IMonitoringEvent[] events)
	{
		this.events = events;
	}
	
	/**
	 *  Get the source.
	 *  @return The source.
	 */
	public String getSource()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Get the type.
	 *  @return The type.
	 */
	public String getType()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 *  Get the time.
	 *  @return The time.
	 */
	public long getTime()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Get the cause.
	 *  @return The cause.
	 */
	public Cause getCause()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Set the cause.
	 *  @param cause The cause to set.
	 */
	public void setCause(Cause cause)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 *  Get a property.
	 *  @param name The property name.
	 *  @return The property.
	 */
	public Object getProperty(String name)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Get a property.
	 *  @param name The property name.
	 *  @return The property.
	 */
	public Map<String, Object> getProperties()
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Get the bulk events.
	 *  @return The bulk events.
	 */
	public IMonitoringEvent[] getBulkEvents()
	{
		return events!=null? events: new IMonitoringEvent[0];
	}

	/**
	 *  Set the events.
	 *  @param events The events to set.
	 */
	public void setBulkEvents(IMonitoringEvent[] events)
	{
		this.events = events;
	}
}
