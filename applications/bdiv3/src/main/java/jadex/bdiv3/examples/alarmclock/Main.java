package jadex.bdiv3.examples.alarmclock;

import jadex.base.PlatformConfigurationHandler;
import jadex.base.Starter;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.types.cms.CreationInfo;

/**
 *  Main for starting the example programmatically.
 *  
 *  To start the example via this Main.java Jadex platform 
 *  as well as examples must be in classpath.
 */
public class Main 
{
	/**
	 *  Start a platform and the example.
	 */
	public static void main(String[] args) 
	{
		IExternalAccess platform = Starter.createPlatform(PlatformConfigurationHandler.getDefaultNoGui()).get();
		// note: this does not work as it loads the unenhanced BDI agent class in the JVM
		//CreationInfo ci = new CreationInfo().setFilenameClass(AlarmclockAgent.class);
		CreationInfo ci = new CreationInfo().setFilename("jadex/bdiv3/examples/alarmclock/AlarmclockAgent.class");
		platform.createComponent(ci).get();
	}
}
