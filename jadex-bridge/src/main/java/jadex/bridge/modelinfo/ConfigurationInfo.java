package jadex.bridge.modelinfo;


import jadex.commons.SUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class ConfigurationInfo extends Startable
{
	//-------- attributes --------
	
	/** The name. */
	protected String name;
	
	/** The list of contained components. */
	protected List components;
	
	/** The list of argument default values. */
	protected List arguments;
	
	/** The list of extensions. */
	protected List extensions;
	
	//-------- constructors --------
	
	/**
	 *  Create a new application.
	 */
	public ConfigurationInfo()
	{
		this(null);
	}
	
	/**
	 *  Create a new application.
	 */
	public ConfigurationInfo(String name)
	{
		this.name = name;
	}
	
	//-------- methods --------
	
	/**
	 *  Get the name.
	 *  @return The name.
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 *  Set the name.
	 *  @param name The name to set.
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 *  Add an component.
	 *  @param component The component.
	 */
	public void addComponentInstance(ComponentInstanceInfo component)
	{
		if(components==null)
			components = new ArrayList();
		this.components.add(component);
	}
	
	/**
	 *  Get all components.
	 *  @return The components.
	 */
	public ComponentInstanceInfo[] getComponentInstances()
	{
		return components!=null? (ComponentInstanceInfo[])components.toArray(new ComponentInstanceInfo[components.size()]): new ComponentInstanceInfo[0];
	}
	
	/**
	 *  Get the list of arguments.
	 *  @return The arguments.
	 */
	public UnparsedExpression[] getArguments()
	{
		return arguments!=null? (UnparsedExpression[])arguments.toArray(new UnparsedExpression[arguments.size()]): new UnparsedExpression[0];
	}
	
	/**
	 *  Set the arguments.
	 *  @param arguments The arguments to set.
	 */
	public void setArguments(UnparsedExpression[] arguments)
	{
		this.arguments = SUtil.arrayToList(arguments);
	}

	/**
	 *  Add an argument.
	 *  @param arg The argument.
	 */
	public void addArgument(UnparsedExpression argument)
	{
		if(arguments==null)
			arguments = new ArrayList();
		arguments.add(argument);
	}
	
	/**
	 *  Get the extension names. 
	 */
	public IExtensionInstance[] getExtensions()
	{
		return extensions!=null? (IExtensionInstance[])extensions.toArray(new IExtensionInstance[extensions.size()]): new IExtensionInstance[0];
	}
	
	/**
	 *  Set the extension types.
	 */
	public void setExtensions(Object[] extensions)
	{
		this.extensions = SUtil.arrayToList(extensions);
	}
	
	/**
	 *  Add a extension type.
	 *  @param extension The extension type.
	 */
	public void addExtension(Object extension)
	{
		if(extensions==null)
			extensions = new ArrayList();
		extensions.add(extension);
	}
	
	// Hack! remove when xml schema has been corrected
	/**
	 *  Add a extension type.
	 *  @param extension The extension type.
	 */
	public void addSpace(Object extension)
	{
		addExtension(extension);
	}
}
