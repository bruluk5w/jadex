package jadex.commons.xml.bean;

import jadex.commons.SReflect;
import jadex.commons.xml.AttributeInfo;
import jadex.commons.xml.BasicTypeConverter;
import jadex.commons.xml.Namespace;
import jadex.commons.xml.SXML;
import jadex.commons.xml.TypeInfo;
import jadex.commons.xml.TypeInfoPathManager;
import jadex.commons.xml.TypeInfoTypeManager;
import jadex.commons.xml.writer.AbstractObjectWriterHandler;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 *  Java bean version for fetching write info for an object. 
 */
public class BeanObjectWriterHandler extends AbstractObjectWriterHandler
{
	//-------- attributes --------
	
	/** The bean introspector. */
//	protected IBeanIntrospector introspector = new BeanReflectionIntrospector();
	protected IBeanIntrospector introspector = new BeanInfoIntrospector();
	
	/** The namespaces by package. */
	protected Map namespacebypackage = new HashMap();
	protected int nscnt;
		
	/** No type infos. */
	protected Set no_typeinfos;
	
	//-------- constructors --------
	
	/**
	 *  Create a new writer.
	 */
	public BeanObjectWriterHandler(Set typeinfos)
	{
		this(false, typeinfos);
	}
	
	/**
	 *  Create a new writer.
	 */
	public BeanObjectWriterHandler(boolean gentypetags, Set typeinfos)
	{
		super(typeinfos);
		this.gentypetags = gentypetags;
	}
	
	//-------- methods --------
	
	/**
	 *  Get the most specific mapping info.
	 *  @param tag The tag.
	 *  @param fullpath The full path.
	 *  @return The most specific mapping info.
	 */
	public TypeInfo getTypeInfo(Object object, QName[] fullpath, Object context)
	{
		Object type = getObjectType(object, context);
		if(no_typeinfos!=null && no_typeinfos.contains(type))
			return null;
			
		TypeInfo ret = super.getTypeInfo(object, fullpath, context);
		// Hack! due to HashMap.Entry is not visible as class
		if(ret==null)
		{
			if(type instanceof Class)
			{
				// Class name not necessary no more
//				Class clazz = (Class)type;
//				type = SReflect.getClassName(clazz);
//				ret = findTypeInfo((Set)typeinfos.get(type), fullpath);
//				if(ret==null)
//				{
				
				// Try if interface or supertype is registered
				List tocheck = new ArrayList();
				tocheck.add(type);
				
				for(int i=0; i<tocheck.size() && ret==null; i++)
				{
					Class clazz = (Class)tocheck.get(i);
					Set tis = titmanager.getTypeInfosByType(clazz);
					ret = titmanager.findTypeInfo(tis, fullpath);
					if(ret==null)
					{
						Class[] interfaces = clazz.getInterfaces();
						for(int j=0; j<interfaces.length; j++)
							tocheck.add(interfaces[j]);
						clazz = clazz.getSuperclass();
						if(clazz!=null)
							tocheck.add(clazz);
					}
				}
				
				// Special case array
				// Requires Object[].class being registered 
				if(ret==null && ((Class)type).isArray())
				{
					ret = titmanager.findTypeInfo(titmanager.getTypeInfosByType(Object[].class), fullpath);
				}
				
				// Add concrete class for same info if it is used
				if(ret!=null)
				{
					TypeInfo ti = new TypeInfo(ret.getSupertype(), ret.getXMLPath(), 
						type, ret.getCommentInfo(), ret.getContentInfo(), 
						ret.getDeclaredAttributeInfos(), ret.getPostProcessor(), ret.getFilter(), 
						ret.getDeclaredSubobjectInfos(), ret.getNamespace());
					
					titmanager.addTypeInfo(ti);
				}
				else
				{
					if(no_typeinfos==null)
						no_typeinfos = new HashSet();
					no_typeinfos.add(type);
				}
			}
		}
		
		return ret;
	}
	
	/**
	 *  Get the object type
	 *  @param object The object.
	 *  @return The object type.
	 */
	public Object getObjectType(Object object, Object context)
	{
		return object.getClass();
	}
	
	/**
	 *  Get the tag name for an object.
	 */
	public QName getTagName(Object object, Object context)
	{
		Object[] ret = new Object[2];
		Class clazz = object.getClass();
		String clazzname = SReflect.getClassName(clazz);
		Namespace ns;
		int idx = clazzname.lastIndexOf(".");
		String pck = SXML.PROTOCOL_TYPEINFO+clazzname.substring(0, idx);
		String tag = clazzname.substring(idx+1);
		
		// Special case array length
		
		if(clazz.isArray())
			tag = tag.substring(0, tag.length()-2)+"__"+Array.getLength(object);
		
		int cnt;
		
		ns = (Namespace)namespacebypackage.get(pck);
		if(ns==null)
		{
			String prefix = "p"+nscnt;
			ns = new Namespace(prefix, pck);
			namespacebypackage.put(pck, ns);
			nscnt++;
		}
		
		return new QName(ns.getURI(), tag, ns.getPrefix());
	}

	/**
	 *  Get a value from an object.
	 */
	protected Object getValue(Object object, Object attr, Object context, Object info)
	{
		if(attr==AttributeInfo.THIS)
			return object;
		
		Object value = null;
		
		Method method;
		
		BeanAttributeInfo binfo = null;
		if(info instanceof BeanAttributeInfo)
			binfo = (BeanAttributeInfo)info;
		
		if(binfo!=null && binfo.getReadMethod()!=null)
		{
			method = ((BeanAttributeInfo)info).getReadMethod();
		}
		else if(attr instanceof BeanProperty)
		{
			method = ((BeanProperty)attr).getGetter();
		}
		else if(attr instanceof String)
		{
			method = findGetMethod(object, (String)attr, new String[]{"get", "is"});
		}
//		else if(attr instanceof QName)
//		{
//			method = findGetMethod(object, ((QName)attr).getLocalPart(), new String[]{"get", "is"});
//		}
		else
		{
			throw new RuntimeException("Unknown attribute type: "+attr);
		}
		
		// Cache the read method.
//		if(binfo!=null && binfo.getReadMethod()==null)
//		{
//			System.out.println("Remembered: "+method.getName());
//			binfo.setReadMethod(method);
//		}
		
		try
		{	
			value = method.invoke(object, new Object[0]);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		return value;
	}
	
	/**
	 *  Get the property.
	 */
	protected Object getProperty(Object info)
	{
		Object ret = null;
		if(info instanceof AttributeInfo)
		{
			ret = ((AttributeInfo)info).getAttributeIdentifier();
		}
		else if(info instanceof String)
		{
			ret = info;
		}
		
		return ret;
	}
	
	/**
	 *  Get the name of a property.
	 */
	protected String getPropertyName(Object property)
	{
		String ret;
		if(property instanceof BeanProperty)
			ret = ((BeanProperty)property).getName();
		else if(property instanceof String)
			ret = (String)property;
		else if(property instanceof QName)
			ret = ((QName)property).getLocalPart();
		else
			throw new RuntimeException("Unknown property type: "+property);
		return ret;
	}

	/**
	 *  Test if a value is a basic type.
	 */
	protected boolean isBasicType(Object property, Object value)
	{
		return BasicTypeConverter.isBuiltInType(value.getClass());
	}
	
	/**
	 *  Get the properties of an object. 
	 */
	protected Collection getProperties(Object object, Object context)
	{
		return introspector.getBeanProperties(object.getClass()).values();
	}

	/**
	 *  Find a get method with some prefix.
	 *  @param object The object.
	 *  @param name The name.
	 *  @param prefixes The prefixes to test.
	 */
	protected Method findGetMethod(Object object, String name, String[] prefixes)
	{
		Method method = null;
		for(int i=0; i<prefixes.length && method==null; i++)
		{
			String methodname = prefixes[i]+name.substring(0, 1).toUpperCase()+name.substring(1);
			try
			{
				method = object.getClass().getMethod(methodname, new Class[0]);
			}
			catch(Exception e)
			{
				// nop
			}
		}
		if(method==null)
			throw new RuntimeException("No getter found for: "+name);
		
		return method;
	}
	
	/**
	 *  Test if a value is compatible with the defined typeinfo.
	 */
	protected boolean isTypeCompatible(Object object, TypeInfo info, Object context)
	{
		boolean ret = true;
		if(info!=null)
		{
			Class clazz = (Class)info.getTypeInfo();
			ret = clazz.isAssignableFrom(object.getClass());
		}
		return ret;
	}
	
	/**
	 *  Test if a value is decodable to the same type.
	 *  Works for basic (final) types only and checks if the
	 *  two types are of same class.
	 */
	protected boolean isDecodableToSameType(Object property, Object value, Object context)
	{
		boolean ret = true;
		if(value!=null)
		{
			BeanProperty prop = (BeanProperty)property;
			ret = value.getClass().equals(prop.getSetterType());
		}
		return ret;
	}
}

