package jadex.commons.xml;

import java.util.List;

/**
 * 
 */
public interface ITypeConverter
{
	/**
	 *  Convert a string value to another type.
	 *  @param val The string value to convert.
	 */
	public Object convertObject(String val, Object root, ClassLoader classloader);
}
