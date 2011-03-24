package jadex.bridge;

import jadex.bridge.service.IService;
import jadex.commons.IPropertiesProvider;
import jadex.commons.Properties;
import jadex.commons.future.IFuture;

/**
 *  This service allows storing and retrieving settings
 *  for specific components or services.
 */
public interface ISettingsService extends IService
{
	/**
	 *  Register a property provider.
	 *  Settings of registered property providers will be automatically saved
	 *  and restored, when properties are loaded.
	 *  @param id 	A unique id to identify the properties (e.g. component or service name).
	 *  @param provider 	The properties provider.
	 *  @return A future indicating when registration is finished.
	 */
	public IFuture	registerPropertiesProvider(String id, IPropertiesProvider provider);
	
	/**
	 *  Deregister a property provider.
	 *  Settings of a deregistered property provider will be saved
	 *  before the property provider is removed.
	 *  @param id 	A unique id to identify the properties (e.g. component or service name).
	 *  @return A future indicating when registration is finished.
	 */
	public IFuture	deregisterPropertiesProvider(String id);
	
	/**
	 *  Set the properties for a given id.
	 *  Overwrites existing settings (if any).
	 *  @param id 	A unique id to identify the properties (e.g. component or service name).
	 *  @param properties 	The properties to set.
	 *  @return A future indicating when properties have been set.
	 */
	public IFuture	setProperties(String id, Properties props);
	
	/**
	 *  Get the properties for a given id.
	 *  @param id 	A unique id to identify the properties (e.g. component or service name).
	 *  @return A future containing the properties (if any).
	 */
	public IFuture	getProperties(String id);
}
