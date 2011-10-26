package jadex.bpmn;

import jadex.bpmn.model.MBpmnModel;
import jadex.bridge.IResourceIdentifier;
import jadex.commons.AbstractModelLoader;
import jadex.commons.ICacheableModel;
import jadex.commons.ResourceInfo;

/**
 *  Loader for eclipse STP BPMN models (.bpmn files).
 */
public class BpmnModelLoader extends AbstractModelLoader
{
	//-------- constants --------
	
	/** The BPMN file extension. */
	public static final String	FILE_EXTENSION_BPMN	= ".bpmn";
	
	//-------- constructors --------
	
	/**
	 *  Create a new BPMN model loader.
	 */
	public BpmnModelLoader()
	{
		super(new String[]{FILE_EXTENSION_BPMN});
	}

	//-------- methods --------
	
	/**
	 *  Load a BPMN model.
	 *  @param name	The filename or logical name (resolved via imports and extensions).
	 *  @param imports	The imports, if any.
	 */
	public MBpmnModel	loadBpmnModel(String name, String[] imports, ClassLoader classloader, Object context) throws Exception
	{
		MBpmnModel	ret	= (MBpmnModel)loadModel(name, FILE_EXTENSION_BPMN, imports, classloader, context);
		ret.setClassLoader(classloader);
		return ret;
	}
	
	//-------- AbstractModelLoader methods --------
		
	/**
	 *  Load a model.
	 *  @param name	The original name (i.e. not filename).
	 *  @param info	The resource info.
	 */
	protected ICacheableModel doLoadModel(String name, String[] imports, ResourceInfo info, 
		ClassLoader classloader, Object context) throws Exception
	{
		return (ICacheableModel)BpmnXMLReader.read(info, classloader, (IResourceIdentifier)context);
	}
}
