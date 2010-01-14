package jadex.adapter.standalone.fipaimpl;

import jadex.bridge.IComponentDescription;
import jadex.bridge.IComponentIdentifier;

import java.io.Serializable;


/**
 *  Java class for concept AMSAgentDescription
 *  of beanynizer_beans_fipa_new ontology.
 */
public class AMSAgentDescription implements IComponentDescription, Cloneable, Serializable
{
	//-------- attributes ----------

	/** Attribute for slot state. */
	protected String state;

	/** Attribute for slot agentidentifier. */
	protected IComponentIdentifier name;

	/** Attribute for slot parent. */
	protected IComponentIdentifier parent;

	/** Attribute for slot ownership. */
	protected String ownership;

	/** The component type. */
	protected String type;

	/** The breakpoints. */
	protected String[] breakpoints;

	//-------- constructors --------

	/**
	 *  Create a new AMSAgentDescription.
	 */
	public AMSAgentDescription()
	{
	}

	/**
	 *  Create a new AMSAgentDescription.
	 */
	public AMSAgentDescription(IComponentIdentifier aid, String type, IComponentIdentifier parent)
	{
		this();
		setName(aid);
		setType(type);
		setParent(parent);
		setState(IComponentDescription.STATE_ACTIVE);
	}

	//-------- accessor methods --------

	/**
	 *  Get the state of this AMSAgentDescription.
	 * @return state
	 */
	public String getState()
	{
		return this.state;
	}

	/**
	 *  Set the state of this AMSAgentDescription.
	 * @param state the value to be set
	 */
	public void setState(String state)
	{
		this.state = state;
	}

	/**
	 *  Get the agentidentifier of this AMSAgentDescription.
	 * @return agentidentifier
	 */
	public IComponentIdentifier getName()
	{
		return this.name;
	}

	/**
	 *  Set the agentidentifier of this AMSAgentDescription.
	 * @param name the value to be set
	 */
	public void setName(IComponentIdentifier name)
	{
		this.name = name;
	}

	/**
	 *  Get the identifier of the parent component (if any).
	 *  @return The parent component identifier.
	 */
	public IComponentIdentifier getParent()
	{
		return this.parent;
	}
	
	/**
	 *  Set the parent of this component description.
	 * @param parent the value to be set
	 */
	public void setParent(IComponentIdentifier parent)
	{
		this.parent = parent;
	}
	
	/**
	 *  Get the ownership of this AMSAgentDescription.
	 * @return ownership
	 */
	public String getOwnership()
	{
		return this.ownership;
	}

	/**
	 *  Set the ownership of this AMSAgentDescription.
	 * @param ownership the value to be set
	 */
	public void setOwnership(String ownership)
	{
		this.ownership = ownership;
	}

	/**
	 *  Get the component type.
	 *  @return The component type name (e.g. 'BDI Agent').
	 */
	public String getType()
	{
		return type;
	}
	
	/**
	 *  Set the component type.
	 *  @param type	The component type name (e.g. 'BDI Agent').
	 */
	public void setType(String type)
	{
		this.type	= type;
	}

	/**
	 *  Get the enabled breakpoints (if any).
	 *  @return The enabled breakpoints.
	 */
	public String[]	getBreakpoints()
	{
		return breakpoints!=null ? breakpoints : new String[0];
	}

	/**
	 *  Set the enabled breakpoints (if any).
	 *  @param breakpoints The enabled breakpoints.
	 */
	public void	setBreakpoints(String[] breakpoints)
	{
		this.breakpoints	= breakpoints;
	}
		
	//-------- methods --------

	/**
	 *  Test if this description equals another description.
	 */
	public boolean equals(Object o)
	{
		return o == this || o instanceof AMSAgentDescription && getName() != null && getName().equals(((AMSAgentDescription)o).getName());
	}

	/**
	 *  Get the hash code of this description.
	 */
	public int hashCode()
	{
		return getName() != null ? getName().hashCode() : 0;
	}

	/**
	 *  Get a string representation of this description.
	 */
	public String toString()
	{
		return "AMSAgentDescription(name=" + getName() + ", state=" + getState() + ", ownership=" + getOwnership() + ")";
	}

	/**
	 *  Clone an agent description.
	 */
	public Object clone()
	{
		try
		{
			AMSAgentDescription ret = (AMSAgentDescription)super.clone();
			ret.setName((AgentIdentifier)((AgentIdentifier)name).clone());
			return ret;
		}
		catch(CloneNotSupportedException e)
		{
			throw new RuntimeException("Cannot clone: " + this);
		}
	}
}
