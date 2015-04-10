package jadex.launch.test.remotereference;

import jadex.base.Starter;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.BasicService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.future.ISuspendable;
import jadex.commons.future.ThreadSuspendable;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 *  Test if a remote references are correctly transferred and mapped back.
 *  
 *  On platform1 there is a serviceA provider.
 *  On platform2 there is a search component which searches for serviceA and return it as result.
 *  
 *  Tests if the result of the remote search yields the same local service proxy.
 */
public class RemoteReferenceTest //extends TestCase
{
	@Test
	public void	testRemoteReference()
	{
		long timeout	= BasicService.getLocalDefaultTimeout();
		ISuspendable	sus	= 	new ThreadSuspendable();
		
		// Start platform1 with local service. (underscore in name assures both platforms use same password)
		IExternalAccess	platform1	= Starter.createPlatform(new String[]{"-platformname", "testcases_*",
			"-saveonexit", "false", "-welcome", "false", "-autoshutdown", "false",
			"-gui", "false",
//			"-logging", "true",
			"-awareness", "false", "-printpass", "false",
			"-component", "jadex/launch/test/remotereference/LocalServiceProviderAgent.class"}).get(timeout);
		
		// Find local service (as local provided service proxy).
		ILocalService	service1	= SServiceProvider
			.getService(platform1, ILocalService.class, RequiredServiceInfo.SCOPE_PLATFORM).get(timeout);
		
		// Start platform2 with (remote) search service. (underscore in name assures both platforms use same password)
		IExternalAccess	platform2	= Starter.createPlatform(new String[]{"-platformname", "testcases_*",
			"-saveonexit", "false", "-welcome", "false", "-autoshutdown", "false", "-gui", "false", "-awareness", "false", "-printpass", "false",
			"-component", "jadex/launch/test/remotereference/SearchServiceProviderAgent.class"}).get(timeout);
		
		// Connect platforms by creating proxy agents.
		Map<String, Object>	args1	= new HashMap<String, Object>();
		args1.put("component", platform2.getComponentIdentifier());
		IComponentManagementService	cms1	= SServiceProvider
			.getService(platform1, IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM).get(timeout);
		cms1.createComponent(null, "jadex/platform/service/remote/ProxyAgent.class", new CreationInfo(args1), null).get(timeout);
		Map<String, Object>	args2	= new HashMap<String, Object>();
		args2.put("component", platform1.getComponentIdentifier());
		IComponentManagementService	cms2	= SServiceProvider
			.getService(platform2, IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM).get(timeout);
		cms2.createComponent(null, "jadex/platform/service/remote/ProxyAgent.class", new CreationInfo(args2), null).get(timeout);
		
		// Search for remote search service from local platform
		ISearchService	search	= SServiceProvider
			.getService(platform1, ISearchService.class, RequiredServiceInfo.SCOPE_GLOBAL).get(timeout);
		// Invoke service to obtain reference to local service.
		ILocalService	service2	= search.searchService("dummy").get(timeout);
		
		// Remote reference should be mapped back to local provided service proxy.
		Assert.assertSame(service1, service2);

		// Kill platforms and end test case.
		platform1.killComponent().get(timeout);
//		platform2.killComponent().get(sus, timeout);
	}
	
	/**
	 *  Execute in main to have no timeouts.
	 */
	public static void main(String[] args)
	{
		RemoteReferenceTest test = new RemoteReferenceTest();
		test.testRemoteReference();
	}
}
