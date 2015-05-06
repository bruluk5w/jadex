package jadex.bdi.testcases.semiautomatic;

import jadex.bdiv3x.runtime.Plan;
import jadex.bridge.service.PublishInfo;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.IProvidedServicesFeature;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.publish.IPublishService;

import java.net.URL;

/**
 *  Plan that creates and searches services. 
 */
public class ServicesPlan extends Plan
{
	/**
	 *  The plan body.
	 */
	public void body()
	{
//		IInternalService service = getInterpreter().getComponentFeature(IProvidedServicesFeature.class).createService(new PrintHelloService(), IPrintHelloService.class, null);
		PublishInfo pi = new PublishInfo("http://localhost:8080/hello/", IPublishService.PUBLISH_RS, IPrintHelloService.class);
//		ProvidedServiceInfo	psi	= new ProvidedServiceInfo();
//		psi.setPublish(new PublishInfo("http://localhost:8080/hello/", IPublishService.PUBLISH_RS, IPrintHelloService.class));
		getInterpreter().getComponentFeature(IProvidedServicesFeature.class).addService("ser", IPrintHelloService.class, new PrintHelloService(), pi, null);
		
		waitFor(500);
		
		// Call service internally
		IPrintHelloService phs = (IPrintHelloService)SServiceProvider.getLocalService(getInterpreter(), 
			IPrintHelloService.class, RequiredServiceInfo.SCOPE_LOCAL);
		phs.printHello();
		
		// Call service via REST
		new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					new URL("http://localhost:8080/hello/printHello").getContent();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}).start();
	}
}

