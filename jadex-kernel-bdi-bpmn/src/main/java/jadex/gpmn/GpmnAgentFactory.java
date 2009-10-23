package jadex.gpmn;

import jadex.bdi.interpreter.BDIAgentFactory;
import jadex.bdi.interpreter.OAVAgentModel;
import jadex.bdi.interpreter.OAVBDIXMLReader;
import jadex.bridge.ILoadableComponentModel;
import jadex.bridge.IPlatform;
import jadex.commons.SGUI;
import jadex.commons.xml.writer.Writer;
import jadex.gpmn.model.MGpmnModel;

import java.io.FileOutputStream;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.UIDefaults;

/**
 *  Factory for creating GPMN agents.
 */
public class GpmnAgentFactory extends BDIAgentFactory
{
	//-------- constants --------
	
	/** The GPMN agent file type. */
	public static final String	FILETYPE_GPMNAGENT	= "GPMN Agent";
	
	/**
	 * The image icons.
	 */
	protected static final UIDefaults icons = new UIDefaults(new Object[]
	{
		"gpmn_agent",	SGUI.makeIcon(GpmnAgentFactory.class, "/jadex/gpmn/images/gpmn_agent.png"),
	});

	//-------- attributes --------
	
	/** The GPMN to OAV converter. */
	protected GpmnBDIConverter converter;

	//-------- constructors --------
	
	/**
	 *  Create a new agent factory.
	 */
	public GpmnAgentFactory(Map props, IPlatform platform)
	{
		super(props, platform);
		converter = new GpmnBDIConverter(loader);
	}
	
	//-------- IAgentFactory interface --------
	
	/**
	 *  Load an agent model.
	 *  @param model The model.
	 *  @return The loaded model.
	 */
	public ILoadableComponentModel loadModel(String model)
	{
		init();

		System.out.println("loading gpmn: "+model);
		try
		{
			MGpmnModel gpmn = GpmnXMLReader.read(model, libservice.getClassLoader(), null);
			OAVAgentModel agent	= converter.convertGpmnModelToBDIAgents(gpmn, libservice.getClassLoader());
			
			FileOutputStream os = new FileOutputStream("wurst.xml");
			Writer writer = OAVBDIXMLReader.getWriter();
			writer.write(agent.getState().getRootObjects().next(), os, libservice.getClassLoader(), agent.getState());
			os.close();
			
			return agent;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	/**
	 *  Test if a model can be loaded by the factory.
	 *  @param model The model.
	 *  @return True, if model can be loaded.
	 */
	public boolean	isLoadable(String model)
	{
		boolean ret = model.toLowerCase().endsWith(".gpmn");
		return ret;
	}
	
	/**
	 *  Test if a model is startable (e.g. an agent).
	 *  @param model The model.
	 *  @return True, if startable (and loadable).
	 */
	public boolean isStartable(String model)
	{
		return isLoadable(model);
	}

	/**
	 *  Get the names of ADF file types supported by this factory.
	 */
	public String[] getFileTypes()
	{
		return new String[]{FILETYPE_GPMNAGENT};
	}

	/**
	 *  Get a default icon for a file type.
	 */
	public Icon getFileTypeIcon(String type)
	{
		return type.equals(FILETYPE_GPMNAGENT) ? icons.getIcon("gpmn_agent") : null;
	}

	/**
	 *  Get the file type of a model.
	 */
	public String getFileType(String model)
	{
		return model.toLowerCase().endsWith(".gpmn") ? FILETYPE_GPMNAGENT : null;
	}
}
