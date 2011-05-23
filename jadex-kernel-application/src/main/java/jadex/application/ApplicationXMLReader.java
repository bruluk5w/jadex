package jadex.application;

import jadex.bridge.modelinfo.Argument;
import jadex.bridge.modelinfo.ComponentInstanceInfo;
import jadex.bridge.modelinfo.ConfigurationInfo;
import jadex.bridge.modelinfo.IArgument;
import jadex.bridge.modelinfo.IModelInfo;
import jadex.bridge.modelinfo.ModelInfo;
import jadex.bridge.modelinfo.SubcomponentTypeInfo;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.bridge.service.ProvidedServiceImplementation;
import jadex.bridge.service.ProvidedServiceInfo;
import jadex.bridge.service.RequiredServiceBinding;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.commons.ResourceInfo;
import jadex.commons.SReflect;
import jadex.commons.Tuple;
import jadex.commons.collection.IndexMap;
import jadex.commons.collection.MultiCollection;
import jadex.javaparser.SJavaParser;
import jadex.xml.AccessInfo;
import jadex.xml.AttributeConverter;
import jadex.xml.AttributeInfo;
import jadex.xml.IContext;
import jadex.xml.IObjectStringConverter;
import jadex.xml.IPostProcessor;
import jadex.xml.IStringObjectConverter;
import jadex.xml.MappingInfo;
import jadex.xml.ObjectInfo;
import jadex.xml.StackElement;
import jadex.xml.SubobjectInfo;
import jadex.xml.TypeInfo;
import jadex.xml.XMLInfo;
import jadex.xml.bean.BeanAccessInfo;
import jadex.xml.bean.BeanObjectReaderHandler;
import jadex.xml.reader.ReadContext;
import jadex.xml.reader.Reader;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;

/**
 *  Reader for loading component XML models into a Java representation states.
 */
public class ApplicationXMLReader
{
	//-------- attributes --------
	
	/** The reader instance. */
	protected Reader reader;
	
	/** The mappings. */
	protected Set[] mappings;
	
	//-------- constructors --------
	
	/**
	 *  Create a new reader.
	 */
	public ApplicationXMLReader(Set[] mappings)
	{
		this.reader = new Reader(new BeanObjectReaderHandler(getXMLMapping(mappings)), false, false, new XMLReporter()
		{
			public void report(String msg, String type, Object info, Location location) throws XMLStreamException
			{
//				System.out.println("XML error: "+msg+", "+type+", "+info+", "+location);
				IContext	context	= (IContext)Reader.READ_CONTEXT.get();
				MultiCollection	report	= (MultiCollection)context.getUserContext();
				String	pos;
				Tuple	stack	= new Tuple(((ReadContext)context).getStack());
				if(stack.getEntities().length>0)
				{
					StackElement	se	= (StackElement)stack.get(stack.getEntities().length-1);
					pos	= " (line "+se.getLocation().getLineNumber()+", column "+se.getLocation().getColumnNumber()+")";
				}
				else
				{
					pos	= " (line 0, column 0)";			
				}
				report.put(stack, msg+pos);
				report.size();
			}
		});
	}
	
	//-------- methods --------
	
	/**
	 *  Read properties from xml.
	 *  @param info	The resource info.
	 *  @param classloader The classloader.
 	 */
	public ApplicationModel read(ResourceInfo rinfo, ClassLoader classloader) throws Exception
	{
		MultiCollection	report	= new MultiCollection(new IndexMap().getAsMap(), LinkedHashSet.class);
		ModelInfo mi = (ModelInfo)reader.read(rinfo.getInputStream(), classloader, report);
		ApplicationModel ret = new ApplicationModel(mi);
		
		if(mi!=null)
		{
			mi.setFilename(rinfo.getFilename());
			mi.setClassloader(classloader);
			mi.setStartable(true);
			ret.setLastModified(rinfo.getLastModified());
//			ret.initModelInfo(report);
			
			// todo: remove
			
			IArgument[] args = ret.getModelInfo().getArguments(); 
			if(args.length>0)
			{
				Map argsmap = new HashMap();
				for(int i=0; i<args.length; i++)
				{
					argsmap.put(args[i].getName(), args[i]);
				}
				ConfigurationInfo[] configs = ret.getModelInfo().getConfigurations();
				for(int i=0; i<configs.length; i++)
				{
					UnparsedExpression[] unexps = configs[i].getArguments();
					for(int j=0; j<unexps.length; j++)
					{
						Object val = SJavaParser.evaluateExpression(unexps[j].getValue(), ret.getModelInfo().getAllImports(), null, ret.getModelInfo().getClassLoader());
						Argument arg = (Argument)argsmap.get(unexps[j].getName());
						arg.setDefaultValue(configs[i].getName(), val);
					}
				}
			}
			
			rinfo.getInputStream().close();
		}
		else
		{
			String errtext = ApplicationModel.buildReport(rinfo.getFilename(), rinfo.getFilename(),
				new String[]{"Component", "Configuration"}, report, null).getErrorText();
			throw new RuntimeException("Model error: "+errtext);
		}
		
		if(report.size()>0)
			System.out.println("Error loading model: "+rinfo.getFilename()+" "+report);
		
		return ret;
	}
	
	/**
	 *  Add method info.
	 */
	public static void addMethodInfos(Map props, String type, String[] names)
	{
		Object ex = props.get(type);
		if(ex!=null)
		{
			List newex = new ArrayList();
			for(Iterator it=SReflect.getIterator(ex); it.hasNext(); )
			{
				newex.add(it.next());
			}
			for(int i=0; i<names.length; i++)
			{
				newex.add(names[i]);
			}
		}
		else
		{
			props.put(type, names);
		}
	}
	
	/**
	 *  Get the XML mapping.
	 */
	public static Set getXMLMapping(Set[] mappings)
	{
		Set types = new HashSet();
		
		// Convert expression directly into value.
		IStringObjectConverter exconv = new IStringObjectConverter()
		{
			public Object convertString(String val, IContext context)
			{
				Object	ret	= null;
				try
				{
					ret	= SJavaParser.evaluateExpression((String)val, ((IModelInfo)context.getRootObject()).getAllImports(), null, context.getClassLoader());
				}
				catch(RuntimeException e)
				{
					Object	se	= new Tuple(((ReadContext)context).getStack());
					MultiCollection	report	= (MultiCollection)context.getUserContext();
					report.put(se, e.toString());
				}
				return  ret;
			}
		};
		
		// Convert expression into parsed expression object.
		IStringObjectConverter pexconv = new IStringObjectConverter()
		{
			public Object convertString(String val, IContext context)
			{
				Object	ret	= null;
				try
				{
					ret	= SJavaParser.parseExpression((String)val, ((IModelInfo)context.getRootObject()).getAllImports(), context.getClassLoader());
				}
				catch(RuntimeException e)
				{
					Object	se	= new Tuple(((ReadContext)context).getStack());
					MultiCollection	report	= (MultiCollection)context.getUserContext();
					report.put(se, e.toString());
				}
				return  ret;
			}
		};
		
		IStringObjectConverter classconv = new IStringObjectConverter()
		{
			public Object convertString(String val, IContext context) throws Exception
			{
				Object ret = val;
				if(val instanceof String)
				{
					ret = SReflect.findClass0((String)val, ((IModelInfo)context.getRootObject()).getAllImports(), context.getClassLoader());
					if(ret==null)
					{
						Object	se	= new Tuple(((ReadContext)context).getStack());
						MultiCollection	report	= (MultiCollection)context.getUserContext();
						report.put(se, "Class not found: "+val);
					}
				}
				return ret;
			}
		};
		
		IObjectStringConverter reclassconv = new IObjectStringConverter()
		{
			public String convertObject(Object val, IContext context)
			{
				String ret = null;
				if(val instanceof Class)
				{
					ret = SReflect.getClassName((Class)val);
					if(ret==null)
					{
						Object	se	= new Tuple(((ReadContext)context).getStack());
						MultiCollection	report	= (MultiCollection)context.getUserContext();
						report.put(se, "Class not found: "+val);
					}
				}
				return ret;
			}
		};
		
		String uri = "http://jadex.sourceforge.net/jadex-application";
		
//		TypeInfo satype = new TypeInfo(null, new ObjectInfo(MStartable.class),
//			new MappingInfo(null, new AttributeInfo[]{
//				new AttributeInfo(new AccessInfo("autoshutdown", "autoShutdown")),
//			}, null));
		
//		Method getname = null; 
//		Method putprop = null;
//		try
//		{
//			getname = UnparsedExpression.class.getMethod("getName", null);
//			putprop = ModelInfo.class.getMethod("addProperty", new Class[]{String.class, Object.class});
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//		}
		
		types.add(new TypeInfo(new XMLInfo(new QName(uri, "applicationtype")), new ObjectInfo(ModelInfo.class), 
			new MappingInfo(null, "description", null,
			new AttributeInfo[]{
			new AttributeInfo(new AccessInfo("autoshutdown", "autoShutdown")),
			new AttributeInfo(new AccessInfo(new QName("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation"), null, AccessInfo.IGNORE_READWRITE))
			}, 
			new SubobjectInfo[]{
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "arguments"), new QName(uri, "argument")}), new AccessInfo(new QName(uri, "argument"), "argument")),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "arguments"), new QName(uri, "result")}), new AccessInfo(new QName(uri, "result"), "result")),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "services"), new QName(uri, "container")}), new AccessInfo(new QName(uri, "container"), "container")),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "services"), new QName(uri, "providedservice")}), new AccessInfo(new QName(uri, "providedservice"), "providedService")),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "services"), new QName(uri, "requiredservice")}), new AccessInfo(new QName(uri, "requiredservice"), "requiredService")),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "componenttype")}), new AccessInfo(new QName(uri, "componenttype"), "subcomponentType")),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "property")}), new AccessInfo(new QName(uri, "property"), "property", null, null)),//, new BeanAccessInfo(putprop, null, "map", getname))),
			new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "applications"), new QName(uri, "application")}), new AccessInfo(new QName(uri, "configuration"), "configuration", null, null))//, new BeanAccessInfo(putprop, null, "map", getname))),
		})));
		
//		types.add(new TypeInfo(new XMLInfo(new QName(uri, "configuration")), new ObjectInfo(ConfigurationInfo.class, new IPostProcessor()
		types.add(new TypeInfo(new XMLInfo(new QName(uri, "application")), new ObjectInfo(ConfigurationInfo.class, new IPostProcessor()
		{
			public Object postProcess(IContext context, Object object)
			{
				ConfigurationInfo app = (ConfigurationInfo)object;
				IModelInfo mapp = (IModelInfo)context.getRootObject();
				
				UnparsedExpression[] margs = app.getArguments();
				for(int i=0; i<margs.length; i++)
				{
					try
					{
						Argument arg = (Argument)mapp.getArgument(margs[i].getName());
						if(arg==null)
							throw new RuntimeException("Overridden argument not declared in component type: "+margs[i].getName());
						
//						Object val = overridenarg.getParsedValue().getValue(null);
//						arg.setDefaultValue(app.getName(), val);
					}
					catch(RuntimeException e)
					{
						Object	se	= new Tuple(((ReadContext)context).getStack());
						MultiCollection	report	= (MultiCollection)context.getUserContext();
						report.put(se, e.toString());
					}
				}
				
				return null;
			}
			
			public int getPass()
			{
				return 0;
			}
		}), 
			new MappingInfo(null, new AttributeInfo[]{
				new AttributeInfo(new AccessInfo("type", "typeName")),
				new AttributeInfo(new AccessInfo("autoshutdown", "autoShutdown"))},
				new SubobjectInfo[]{
				new SubobjectInfo(new XMLInfo(new QName[]{new QName(uri, "component")}), new AccessInfo(new QName(uri, "component"), "componentInstance")),
			})));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "applicationtype"), new QName(uri, "arguments"), new QName(uri, "argument")}), new ObjectInfo(Argument.class), 
			new MappingInfo(null, "description", new AttributeInfo(new AccessInfo((String)null, "defaultValue"), new AttributeConverter(exconv, null)),
			new AttributeInfo[]{new AttributeInfo(new AccessInfo("class", "typename"))}, null)));
		
		types.add(new TypeInfo(new XMLInfo(new QName(uri, "import")), new ObjectInfo(String.class)));
		
//		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "configuration"), new QName(uri, "arguments"), new QName(uri, "argument")}), new ObjectInfo(UnparsedExpression.class),//, new ExpressionProcessor()), 
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "application"), new QName(uri, "arguments"), new QName(uri, "argument")}), new ObjectInfo(UnparsedExpression.class),//, new ExpressionProcessor()), 
			new MappingInfo(null, null, "value", new AttributeInfo[]{
				new AttributeInfo(new AccessInfo("class", "className"))
			}, null)));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "componenttypes"), new QName(uri, "componenttype")}), new ObjectInfo(SubcomponentTypeInfo.class),
			new MappingInfo(null, new AttributeInfo[]{
				new AttributeInfo(new AccessInfo("autoshutdown", "autoShutdown")),
			}, null)));		
		
		types.add(new TypeInfo(new XMLInfo(new QName(uri, "component")), new ObjectInfo(ComponentInstanceInfo.class),
			new MappingInfo(null, new AttributeInfo[]{
				new AttributeInfo(new AccessInfo("type", "typeName")),
				new AttributeInfo(new AccessInfo("autoshutdown", "autoShutdown")),
				new AttributeInfo(new AccessInfo("number"))
			}, null)));
		
		types.add(new TypeInfo(new XMLInfo(new QName[]{new QName(uri, "component"), new QName(uri, "arguments"), new QName(uri, "argument")}), new ObjectInfo(UnparsedExpression.class),//, new ExpressionProcessor()), 
			new MappingInfo(null, null, "value", new AttributeInfo[]{
				new AttributeInfo(new AccessInfo("class", "className"))
			}, null)));
		
		types.add(new TypeInfo(new XMLInfo(new QName(uri, "providedservice")), new ObjectInfo(ProvidedServiceInfo.class),// new ExpressionProcessor()), 
			new MappingInfo(null, null, "value", new AttributeInfo[]{
//				new AttributeInfo(new AccessInfo("class", "className")),
				new AttributeInfo(new AccessInfo("class", "type"), new AttributeConverter(classconv, reclassconv)),
//				new AttributeInfo(new AccessInfo("implementation", "implementation"))
			}, null)));
		types.add(new TypeInfo(new XMLInfo(new QName(uri, "implementation")), new ObjectInfo(ProvidedServiceImplementation.class),
			new MappingInfo(null, null, "expression", new AttributeInfo[]{
				new AttributeInfo(new AccessInfo("class", "implementation"), new AttributeConverter(classconv, reclassconv)),
			}, null)));
		types.add(new TypeInfo(new XMLInfo(new QName(uri, "requiredservice")), new ObjectInfo(RequiredServiceInfo.class), // new ExpressionProcessor()), 
			new MappingInfo(null, null, "value", new AttributeInfo[]{
				new AttributeInfo(new AccessInfo("class", "type"), new AttributeConverter(classconv, reclassconv))
			}, null)));
		types.add(new TypeInfo(new XMLInfo(new QName(uri, "binding")), new ObjectInfo(RequiredServiceBinding.class), 
				new MappingInfo(null, new AttributeInfo[]{
				new AttributeInfo(new AccessInfo("componentname", "componentName")),
				new AttributeInfo(new AccessInfo("componenttype", "componentType")),
			})));
		
//		types.add(new TypeInfo(new XMLInfo(new QName(uri, "container")), new ObjectInfo(MExpressionType.class, new ExpressionProcessor()), 
//			new MappingInfo(null, null, "value", new AttributeInfo[]{
//				new AttributeInfo(new AccessInfo("class", "className"))
//			}, null)));
					
		types.add(new TypeInfo(new XMLInfo(new QName(uri, "property")), new ObjectInfo(UnparsedExpression.class),//, new ExpressionProcessor()), 
			new MappingInfo(null, null, "value", new AttributeInfo[]{
				new AttributeInfo(new AccessInfo("class", "className"))
			}, null)));
		
		for(int i=0; mappings!=null && i<mappings.length; i++)
		{
			types.addAll(mappings[i]);
		}
				
		return types;
	}

	//-------- helper classes --------
	
//	/**
//	 *  Parse expression text.
//	 */
//	public static class ExpressionProcessor	implements IPostProcessor
//	{
//		// Hack!!! Should be configurable.
//		protected static IExpressionParser	exp_parser	= new JavaCCExpressionParser();
//		
//		/**
//		 *  Parse expression text.
//		 */
//		public Object postProcess(IContext context, Object object)
//		{
//			Object ret = null;
//			
//			ComponentModel cm = (ComponentModel)context.getRootObject();
//			UnparsedExpression exp = (UnparsedExpression)object;
//			
////			String classname = exp.getClassName();
////			if(classname!=null)
////			{
////				try
////				{
////					Class clazz = SReflect.findClass(classname, app.getAllImports(), context.getClassLoader());
////					exp.setClazz(clazz);
////				}
////				catch(Exception e)
////				{
////					Object	se	= new Tuple(((ReadContext)context).getStack().toArray());
////					MultiCollection	report	= (MultiCollection)context.getUserContext();
////					report.put(se, e.toString());
////				}
////			}
//			
//			String lang = exp.getLanguage();
//			String value = exp.getValue(); 
//			if(value!=null)
//			{
//				if(lang==null || "java".equals(lang))
//				{
//					try
//					{
//						IParsedExpression pexp = exp_parser.parseExpression(value, cm.getAllImports(), null, context.getClassLoader());
//						ret = pexp.getValue(null);
////						exp.setParsedValue(pexp);
//					}
//					catch(RuntimeException e)
//					{
//						Object	se	= new Tuple(((ReadContext)context).getStack().toArray());
//						MultiCollection	report	= (MultiCollection)context.getUserContext();
//						report.put(se, e.toString());
//					}
//				}	
//				else
//				{
//					Object	se	= new Tuple(((ReadContext)context).getStack().toArray());
//					MultiCollection	report	= (MultiCollection)context.getUserContext();
//					report.put(se, "Unknown expression language: "+lang);
//				}
//			}
//			
//			return ret;
//		}
//		
//		/**
//		 *  Get the pass number.
//		 *  @return The pass number.
//		 */
//		public int getPass()
//		{
//			return 0;
//		}
//	}
}
