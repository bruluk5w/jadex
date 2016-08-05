package jadex.micro.servicecall;

import jadex.bridge.IInternalAccess;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentKilled;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;

/**
 *  Agent providing a raw service.
 */
@ProvidedServices(@ProvidedService(type=IServiceCallService.class,
	implementation=@Implementation(expression="new RawServiceCallService($component.getComponentIdentifier())",
		proxytype=Implementation.PROXYTYPE_RAW)))
@Agent
public class RawServiceAgent
{
	@Agent
	protected IInternalAccess agent;
	
	@AgentKilled
	public void killed()
	{
		System.out.println("killing: "+agent.getComponentIdentifier());
	}
}
