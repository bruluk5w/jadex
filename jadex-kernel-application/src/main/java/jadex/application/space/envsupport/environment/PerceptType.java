package jadex.application.space.envsupport.environment;

import java.util.Collections;
import java.util.Set;

/**
 *  Class for representation a percept type, which has a name,
 *  a set of object types and a set of agent types.
 */
public class PerceptType
{
	//-------- attributes --------
	
	/** The percept name. */
	protected String name;
	
	/** The object types. */
	protected Set objecttypes;
	
	/** The agent types. */
	protected Set agenttypes;
	
	//-------- methods --------
	
	/**
	 *  Create a new percept type.
	 */
	public PerceptType()
	{
		this(null, null, null);
	}

	
	/**
	 *  Create a new percept type.
	 */
	public PerceptType(String name, Set objecttypes, Set agenttypes)
	{
		this.name = name;
		this.objecttypes = objecttypes==null? Collections.EMPTY_SET: objecttypes;
		this.agenttypes = agenttypes==null? Collections.EMPTY_SET: agenttypes;
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
	 *  @param name the name to set.
	 */
	public void setName(String name)
	{
		this.name = name;
	}


	/**
	 *  Get the object types.
	 *  @return the object types.
	 */
	public Set getObjectTypes()
	{
		return this.objecttypes;
	}


	/**
	 *  Set the object types.
	 *  @param objecttypes the objecttypes to set.
	 */
	public void setObjectTypes(Set objecttypes)
	{
		this.objecttypes = objecttypes;
	}


	/**
	 *  Get the agent types.
	 *  @return The agenttypes.
	 */
	public Set getAgentTypes()
	{
		return this.agenttypes;
	}


	/**
	 *  Set the agent types.
	 *  @param agenttypes the agenttypes to set.
	 */
	public void setAgentTypes(Set agenttypes)
	{
		this.agenttypes = agenttypes;
	}

	/**
	 *  Get the string representation.
	 *  @return The string representation.
	 */
	public String toString()
	{
		return "PerceptType(name="+name+", objecttypes="+objecttypes+", agenttypes="+agenttypes+")";
	}
	
	
}
