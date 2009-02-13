package jadex.adapter.base.appdescriptor;

/**
 *  Parameter representation.
 */
public class Parameter
{
	//-------- attributes --------

	/** The name. */
	protected String	name;

	/** The value. */
	protected String	value;

	//-------- constructors --------

	/**
	 *  Create a new parameter.
	 */
	public Parameter()
	{
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
	 *  Get the value.
	 *  @return The value.
	 */
	public String getValue()
	{
		return this.value;
	}

	/**
	 *  Set the value.
	 *  @param value The value to set.
	 */
	public void setValue(String value)
	{
		this.value = value;
	}
}
