package jadex.bridge.modelinfo;

import jadex.commons.IValueFetcher;
import jadex.commons.SReflect;
import jadex.javaparser.SJavaParser;

import java.util.Map;


/**
 * 
 */
public class UnparsedExpression
{
	//-------- attributes --------

	/** The name. */
	protected String name;

	/** The class name. */
	protected String classname;
	
	/** The class. */
	protected Class clazz;
	
	/** The value. */
	protected String value;
	
	/** The language. */
	protected String language;
	
	//-------- constructors --------

	/**
	 *  Create a new expression.
	 */
	public UnparsedExpression()
	{
	}
	
	/**
	 *  Create a new expression.
	 */
	public UnparsedExpression(String name, Class clazz, String value, String language)
	{
		this.name = name;
		this.clazz = clazz;
		this.value = value;
		this.language = language;
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
	 *  Get the clazz name.
	 *  @return The clazz name.
	 */
	public String getClassName()
	{
		return classname;
	}

	/**
	 *  Set the clazz name.
	 *  @param clazz The clazz name.
	 */
	public void setClassName(String classname)
	{
		this.classname = classname;
	}

//	/**
//	 *  Get the clazz.
//	 *  @return The clazz.
//	 */
//	public Class getClazz()
//	{
//		return clazz;
//	}
	
	/**
	 *  Get the clazz.
	 *  @return The clazz.
	 */
	public Class getClazz(ClassLoader classloader, String[] imports)
	{
		if(clazz==null && classname!=null)
		{
			clazz = SReflect.findClass0(classname, imports, classloader);
		}
		return clazz;
	}

	// Excluded to avoid sending per xml 
//	/**
//	 *  Set the clazz.
//	 *  @param clazz The clazz to set.
//	 */
//	public void setClazz(Class clazz)
//	{
//		this.clazz = clazz;
//	}

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

	/**
	 *  Get the language.
	 *  @return The language.
	 */
	public String getLanguage()
	{
		return language;
	}

	/**
	 *  Set the language.
	 *  @param language The language.
	 */
	public void setLanguage(String language)
	{
		this.language = language;
	}
	
	//-------- static helpers --------
	
	/**
	 *  Get a parsed property.
	 *  Handles properties, which may be parsed or unparsed,
	 *  and always returns a parsed property value.
	 *  @param	name	The property name.  
	 *  @return The property value or null if property not defined.
	 */
	public static Object	getProperty(Map properties, String name, String[] imports, IValueFetcher fetcher, ClassLoader classloader)
	{
		// Todo: caching of parsed values?
		Object	ret	= properties!=null ? properties.get(name) : null;
		if(ret instanceof UnparsedExpression)
		{
			// todo: language
			UnparsedExpression	upe	= (UnparsedExpression)ret;
			ret = SJavaParser.evaluateExpression(upe.getValue(), imports, fetcher, classloader);
		}
		return ret;
	}
}
