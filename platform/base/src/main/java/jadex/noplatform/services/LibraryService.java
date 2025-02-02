package jadex.noplatform.services;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IInputConnection;
import jadex.bridge.IResourceIdentifier;
import jadex.bridge.service.annotation.CheckNotNull;
import jadex.bridge.service.annotation.Excluded;
import jadex.bridge.service.annotation.Reference;
import jadex.bridge.service.types.library.ILibraryService;
import jadex.bridge.service.types.library.ILibraryServiceListener;
import jadex.commons.Tuple2;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.ISubscriptionIntermediateFuture;
import jadex.commons.future.SubscriptionIntermediateFuture;

/**
 *  Library service for loading classpath elements.
 */
public class LibraryService extends BaseService implements ILibraryService
{
	/** The pseudo system classpath rid. */
	/*public static final IResourceIdentifier SYSTEMCPRID;
	
	static
	{
		IResourceIdentifier res = null;
		try
		{
			res = new ResourceIdentifier(new LocalResourceIdentifier(new ComponentIdentifier("PSEUDO"), new URL("http://SYSTEMCPRID")), null);
		}
		catch(Exception e)
		{
			// should not happen
			e.printStackTrace();
		}
		SYSTEMCPRID = res;
	}*/
	
	/**
	 *  Create a new asynchronous executor service. 
	 */
	public LibraryService(IComponentIdentifier cid)
	{
		super(cid, ILibraryService.class);
	}
	
	/**
	 *  Start the service.
	 */
	public IFuture<Void> startService()
	{
		return IFuture.DONE;
	}
	
	/**
	 *  Shutdown the service.
	 *  @param listener The listener.
	 */
	public IFuture<Void> shutdownService()
	{
		return IFuture.DONE;
	}

	//-------- rid handling --------

	/**
	 *  Add a new resource identifier.
	 *  @param parid The optional parent rid.
	 *  @param rid The resource identifier.
	 */
	public IFuture<IResourceIdentifier> addResourceIdentifier(IResourceIdentifier parid,
		@CheckNotNull IResourceIdentifier rid, boolean workspace)
	{
		return new Future<>(null);
	}
	
	/**
	 *  Remove a resource identifier.
	 *  @param parid The optional parent rid.
	 *  @param url The resource identifier.
	 */
	public IFuture<Void> removeResourceIdentifier(IResourceIdentifier parid, 
		@CheckNotNull IResourceIdentifier rid)
	{
		return IFuture.DONE;
	}
		
	/**
	 *  Get the removable links.
	 */
	public IFuture<Set<Tuple2<IResourceIdentifier, IResourceIdentifier>>> getRemovableLinks()
	{
		return new Future<>(Collections.EMPTY_SET);
	}
	
	/**
	 *  Get all resource identifiers (does not include rids (urls) of parent loader).
	 *  @return The list of resource identifiers.
	 */
	public IFuture<List<IResourceIdentifier>> getAllResourceIdentifiers()
	{
		return new Future<>(null);
	}
	
	/**
	 *  Get the resource identifier dependencies. Includes also system urls via a pseudo
	 *  rid defined as constant in LibraryService.SYSTEMCPRID.
	 */
	public IFuture<Tuple2<IResourceIdentifier, Map<IResourceIdentifier, List<IResourceIdentifier>>>> getResourceIdentifiers()
	{
		return new Future<>(null);
	}
	
	/** 
	 *  Returns the classloader for a resource identifier.
	 *  @param rid The resource identifier.
	 *  @return The classloader.
	 */
	@Excluded
	public @Reference IFuture<ClassLoader> getClassLoader(IResourceIdentifier rid)
	{
		return new Future<>(this.getClass().getClassLoader());
	}
	
	/** 
	 *  Returns the classloader for a resource identifier.
	 *  @param rid The resource identifier.
	 *  @param workspace True if workspace resolution is ok.
	 *  @return The classloader.
	 */
	@Excluded
	public @Reference IFuture<ClassLoader> getClassLoader(IResourceIdentifier rid, boolean workspace)
	{
		return new Future<>(this.getClass().getClassLoader());
	}
	
	//-------- remote rid handling --------
	
	/**
	 *  Get a resource as stream (jar).
	 */
	public IFuture<IInputConnection> getResourceAsStream(IResourceIdentifier rid)
	{
		return new Future<>(null);
	}
	
	//-------- url handling --------
	
	/**
	 *  Add a new url as resource identifier.
	 *  @param parid The resource identifier (null for root entry).
	 *  @param url The url.
	 */
	public IFuture<IResourceIdentifier> addURL(IResourceIdentifier parid, @CheckNotNull URL url)
	{
		return new Future<>(null);
	}
	
	/**
	 *  Remove a url.
	 *  @param parid The resource identifier (null for root entry).
	 *  @param url The url.
	 */
	public IFuture<Void> removeURL(IResourceIdentifier parid, @CheckNotNull URL url)
	{
		return new Future<>(null);
	}

	/** 
	 *  Returns the resource identifier for a url.
	 *  @param url The url.
	 *  @return The corresponding resource identifier.
	 */
	public IFuture<IResourceIdentifier> getResourceIdentifier(URL url)
	{
		return new Future<>(null);
	}

	/** 
	 *  Get the top-level resource identifier.
	 *  @param url The url.
	 *  @return The corresponding resource identifier.
	 */
	public IResourceIdentifier getRootResourceIdentifier()
	{
		return null;
	}
	
	/**
	 *  Add a top level url. A top level url will
	 *  be available for all subordinated resources. 
	 *  @param url The url.
	 */
	public IFuture<Void> addTopLevelURL(@CheckNotNull URL url)
	{
		return IFuture.DONE;
	}

	/**
	 *  Remove a top level url. A top level url will
	 *  be available for all subordinated resources. 
	 *  @param url The url.
	 *  
	 *  note: top level url removal will only take 
	 *  effect after restart of the platform.
	 */
	public IFuture<Void> removeTopLevelURL(@CheckNotNull URL url)
	{
		return IFuture.DONE;
	}
	
	/**
	 *  Get other contained (but not directly managed) urls from parent classloaders.
	 *  @return The list of urls.
	 */
	public IFuture<List<URL>> getNonManagedURLs()
	{
		return new Future<>(null);
	}
	
	/**
	 *  Get all urls (managed and non-managed). Uses getAllResourceIdentifiers() 
	 *  for managed and getNonManagedURLs() for unmanaged.
	 *  @return The list of urls.
	 */
	public IFuture<List<URL>> getAllURLs()
	{
		return new Future<>(null);
	}
	
	/**
	 *  todo: support all component models
	 *  
	 *  Get all startable component models (currently only Java classes with @Agent).
	 *  @return The file names of the component models.
	 */
	public IFuture<Collection<String[]>> getComponentModels()
	{
		return new Future<>(null);
	}
	
	public ISubscriptionIntermediateFuture<Collection<String[]>> getComponentModelsAsStream()
	{
		SubscriptionIntermediateFuture<Collection<String[]>> ret = new SubscriptionIntermediateFuture<Collection<String[]>>();
		ret.setResult(null);
		return ret;
	}
	
	//-------- listener methods --------
	
	/**
   *  Add an Library Service listener.
   *  The listener is registered for changes in the loaded library states.
   *  @param listener The listener to be added.
   */
  public IFuture<Void> addLibraryServiceListener(@CheckNotNull ILibraryServiceListener listener)
  {
	  return IFuture.DONE;
  }
  
  /**
   *  Remove an Library Service listener.
   *  @param listener  The listener to be removed.
   */
  public IFuture<Void> removeLibraryServiceListener(@CheckNotNull ILibraryServiceListener listener)
  {
	  return IFuture.DONE;
  }

}
