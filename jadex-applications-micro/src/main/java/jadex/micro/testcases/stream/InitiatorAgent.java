package jadex.micro.testcases.stream;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.IOutputConnection;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.bridge.service.types.message.IMessageService;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.MicroAgent;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.ComponentType;
import jadex.micro.annotation.ComponentTypes;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;
import jadex.micro.annotation.Result;
import jadex.micro.annotation.Results;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 *  Agent that provides a service with a stream.
 */
@Agent
@RequiredServices(
{
	@RequiredService(name="msgservice", type=IMessageService.class, 
		binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)),
	@RequiredService(name="cms", type=IComponentManagementService.class, 
		binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM))
})
@ComponentTypes(
	@ComponentType(name="receiver", filename="jadex/micro/testcases/stream/ReceiverAgent.class")
)
public class InitiatorAgent
{
	@Agent
	protected MicroAgent agent;
	
	/**
	 * 
	 */
	@AgentBody
	public IFuture<Void> body()
	{
		final Future<Void> ret = new Future<Void>();
		IFuture<IComponentManagementService> cmsfut = agent.getServiceContainer().getRequiredService("cms");
		cmsfut.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Void>(ret)
		{
			public void customResultAvailable(IComponentManagementService cms)
			{
				cms.createComponent(null, "receiver", new CreationInfo(agent.getComponentIdentifier()), null)
					.addResultListener(new ExceptionDelegationResultListener<IComponentIdentifier, Void>(ret)
				{
					public void customResultAvailable(final IComponentIdentifier cid) 
					{
						IFuture<IMessageService> msfut = agent.getServiceContainer().getRequiredService("msgservice");
						msfut.addResultListener(new ExceptionDelegationResultListener<IMessageService, Void>(ret)
						{
							public void customResultAvailable(IMessageService ms)
							{
								ms.createOutputConnection(agent.getComponentIdentifier(), cid)
									.addResultListener(new ExceptionDelegationResultListener<IOutputConnection, Void>(ret)
								{
									public void customResultAvailable(final IOutputConnection ocon) 
									{
										final IComponentStep<Void> step = new IComponentStep<Void>()
										{
											final int[] cnt = new int[]{1};
											final int max = 3;
											final IComponentStep<Void> self = this;
											public IFuture<Void> execute(IInternalAccess ia)
											{
												byte[] tosend = new byte[cnt[0]];
												for(int i=0; i<cnt[0]; i++)
													tosend[i] = (byte)cnt[0];
												ocon.send(tosend)
													.addResultListener(new DelegationResultListener<Void>(ret)
												{
													public void customResultAvailable(Void result)
													{
														if(cnt[0]++<max)
														{
															agent.waitFor(1000, self);
														}
														else
														{
															ocon.close();
//															ret.setResult(null);
														}
													}
												});
												return IFuture.DONE;
											}
										};
										agent.waitFor(1000, step);
									}
								});
							}
						});
					}
				});
			}
		});
		
		return ret;
	}
}
