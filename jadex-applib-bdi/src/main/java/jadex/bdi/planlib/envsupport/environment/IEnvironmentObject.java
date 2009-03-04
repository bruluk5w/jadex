package jadex.bdi.planlib.envsupport.environment;

import jadex.bdi.planlib.envsupport.environment.task.IObjectTask;
import jadex.bdi.planlib.envsupport.math.IVector1;
import jadex.bridge.IClock;

import java.util.Map;

public interface IEnvironmentObject
{
	/**
	 * Returns the type of the object.
	 * 
	 * @return the type
	 */
	public Object getType();
	
	/**
	 * Returns an object's property.
	 * 
	 * @param name name of the property
	 * @return the property
	 */
	public Object getProperty(String name);

	/**
	 * Sets an object's property.
	 * 
	 * @param name name of the property
	 * @param property the property
	 */
	public void setProperty(String name, Object property);

	/**
	 * Returns a copy of all of the object's properties.
	 * 
	 * @return the properties
	 */
	public Map getProperties();

	/**
	 * Adds a new task for the object.
	 * 
	 * @param task new task
	 */
	public void addTask(IObjectTask task);

	/**
	 * Returns a task by its ID.
	 * 
	 * @param taskId ID of the task
	 * @return the task
	 */
	public IObjectTask getTask(Object taskId);

	/**
	 * Removes a task from the object.
	 * 
	 * @param taskId ID of the task
	 */
	public void removeTask(Object taskId);
	
	/**
	 * Removes all tasks from the object.
	 * 
	 */
	public void clearTasks();
	
	/**
	 * Updates the object to the current time.
	 * 
	 * @param clock the clock	
	 * @param deltaT the time difference that has passed
	 */
	public void updateObject(IClock clock, IVector1 deltaT);
	
	/**
	 * Fires an ObjectEvent.
	 * 
	 * @param event the ObjectEvent
	 */
	public void fireObjectEvent(ObjectEvent event);
}
