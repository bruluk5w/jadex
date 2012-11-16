package jadex.backup.resource;

import jadex.bridge.IInputConnection;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.bridge.service.annotation.ServiceStart;
import jadex.bridge.service.types.remote.ServiceOutputConnection;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.IntermediateFuture;
import jadex.micro.IPojoMicroAgent;
import jadex.micro.MicroAgent;

import java.io.File;
import java.io.FileInputStream;

/**
 *  Local (i.e. user-oriented) interface to a resource. 
 */
@Service
public class ResourceService	implements IResourceService
{
	//-------- attributes --------
	
	/** The component. */
	@ServiceComponent
	protected MicroAgent	agent;
	
	/** The resource provider agent. */
	protected ResourceProviderAgent	rpa;
	
	//-------- constructors --------
	
	/**
	 *  Called at startup.
	 */
	@ServiceStart
	public void	start()
	{
		rpa	= (ResourceProviderAgent)((IPojoMicroAgent)agent).getPojoAgent();
	}
	
	//-------- IResourceService interface --------

	/**
	 *  Get the global resource id.
	 *  The global resource id is a unique id that is
	 *  used to identify the same resource on different
	 *  hosts, i.e. provided by different service instances.
	 *  
	 *  The id is static, i.e. it does not change during
	 *  the lifetime of the resource.
	 */
	public String	getResourceId()	
	{
		return rpa.getResource().getResourceId();
	}
	
	/**
	 *  Get the local resource id.
	 *  The local resource id is a unique id that is
	 *  used to identify an individual instance of a
	 *  distributed resource on a specific host.
	 *  
	 *  The id is static, i.e. it does not change during
	 *  the lifetime of the resource.
	 */
	public String	getLocalId()
	{
		return rpa.getResource().getLocalId();
	}
	
	/**
	 *  Get information about a local file or directory.
	 *  @param file	The resource path of the file.
	 *  @return	The file info with all known time stamps or null if the file does no longer exist.
	 */
	public IFuture<FileInfo>	getFileInfo(String file)
	{
		return new Future<FileInfo>(rpa.getResource().getFileInfo(file));
	}
	
	/**
	 *  Get the contents of a directory.
	 *  @param dir	The file info of the directory.
	 *  @return	A list of file infos for files and subdirectories.
	 *  @throws Exception if the supplied file info is outdated.
	 */
	public IIntermediateFuture<FileInfo>	getDirectoryContents(FileInfo dir)
	{
		return new IntermediateFuture<FileInfo>(rpa.getResource().getDirectoryContents(dir));
	}
	
	/**
	 *  Get the contents of a file.
	 *  @param file	The file info of the file.
	 *  @return	A list of plain file names (i.e. without path).
	 *  @throws Exception if the supplied file info is outdated.
	 */
	public IFuture<IInputConnection>	getFileContents(FileInfo file)
	{
		Future<IInputConnection>	ret	= new Future<IInputConnection>();
		try
		{
			File	f	= rpa.getResource().getFile(file.getLocation());
			if(!f.exists() || rpa.getResource().getFileInfo(file.getLocation()).isNewerThan(file))
			{
				throw new RuntimeException("Local resource has changed: "+file.getLocation());
			}

			ServiceOutputConnection	soc	= new ServiceOutputConnection();
			soc.writeFromInputStream(new FileInputStream(rpa.getResource().getFile(file.getLocation())), agent.getExternalAccess());
			ret.setResult(soc.getInputConnection());
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		return ret;
	}
}
