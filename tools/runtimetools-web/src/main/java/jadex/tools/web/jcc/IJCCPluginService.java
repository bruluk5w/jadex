package jadex.tools.web.jcc;

import jadex.bridge.service.annotation.Security;
import jadex.bridge.service.annotation.Service;
import jadex.commons.future.IFuture;

/**
 *  Base interface for JCC plugin services.
 *  Must be extended by all JCC plugin interfaces and services.
 */
@Service
@Security(roles=Security.UNRESTRICTED)
public interface IJCCPluginService
{
	/**
	 *  Get the plugin component (html).
	 *  @return The plugin code.
	 */
	public IFuture<String> getPluginComponent();
	
	/**
	 *  Get the plugin name.
	 *  @return The plugin name.
	 */
	public IFuture<String> getPluginName();
	
	/**
	 *  Get the plugin icon.
	 *  @return The plugin icon.
	 */
	public IFuture<byte[]> getPluginIcon();
	
	/**
	 *  Get the plugin priority.
	 *  @return The plugin priority.
	 */
	public IFuture<Integer> getPriority();
	
	// todo: Response is used to set the mimetype :-(
	// should set in gateway near rest call
	/**
	 *  Load a string-based resource (style or js).
	 *  @param filename The filename.
	 *  @return The text from the file.
	 */
	//public IFuture<String> loadResource(String filename);
	//public IFuture<Response> loadResource(String filename);
	public IFuture<byte[]> loadResource(String filename);
}
