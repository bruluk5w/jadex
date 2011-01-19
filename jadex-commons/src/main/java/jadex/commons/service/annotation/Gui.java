package jadex.commons.service.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Gui representation for a service.
 *  
 *  Applicable the type.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Gui
{
//	/**
//	 *  Supply a class name of a class implementing the gui.
//	 */
//	public String value();
	
	/**
	 *  Supply a class implementing the gui.
	 */
	public Class clazz();
}