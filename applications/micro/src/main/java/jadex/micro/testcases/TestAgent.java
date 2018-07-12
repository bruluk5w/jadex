package jadex.micro.testcases;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jadex.base.IPlatformConfiguration;
import jadex.base.Starter;
import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.base.test.util.STest;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IArgumentsResultsFeature;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.search.ServiceQuery;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.clock.ITimedObject;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.FutureBarrier;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.future.ITuple2Future;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.AgentKilled;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;
import jadex.micro.annotation.Result;
import jadex.micro.annotation.Results;

@Agent
@RequiredServices(
{
//	@RequiredService(name="msgservice", type=IMessageService.class,
//		binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)),
	@RequiredService(name="cms", type=IComponentManagementService.class, 
		binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)),
	@RequiredService(name="clock", type=IClockService.class, 
		binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM))
})
//@ComponentTypes(
//	@ComponentType(name="receiver", filename="jadex/micro/testcases/stream/ReceiverAgent.class")
//)
@Results(@Result(name="testresults", clazz=Testcase.class))
public abstract class TestAgent	extends RemoteTestBaseAgent
{
	@Agent
	protected IInternalAccess agent;
	
	protected Set<IExternalAccess>	platforms	= new LinkedHashSet<IExternalAccess>();
	
	
	/**
	 *  Cleanup created platforms.
	 */
	@AgentKilled
	public IFuture<Void>	cleanup()
	{
		FutureBarrier<Void>	outer	= new FutureBarrier<Void>();
		outer.addFuture(super.cleanup());
		
		FutureBarrier<Map<String, Object>>	inner	= new FutureBarrier<Map<String,Object>>();
		for(final IExternalAccess platform: platforms)
		{
			inner.addFuture(platform.killComponent());
		}
		platforms	= null;
		outer.addFuture(inner.waitFor());

		return outer.waitFor();
	}
	
	/**
	 *  The agent body.
	 */
	@AgentBody
	public IFuture<Void> body()
	{
//		agent.getLogger().severe("Testagent start: "+agent.getComponentDescription());
		final Future<Void> ret = new Future<Void>();
		
		final Testcase tc = new Testcase();
		tc.setTestCount(getTestCount());
		
		performTests(tc).addResultListener(agent.getFeature(IExecutionFeature.class).createResultListener(new IResultListener<Void>()
		{
			public void resultAvailable(Void result)
			{
//				System.out.println("tests finished: "+agent.getComponentIdentifier());

				//agent.getComponentFeature(IArgumentsFeature.class).getResults().put("testresults", tc);
				agent.getFeature(IArgumentsResultsFeature.class).getResults().put("testresults", tc);
				ret.setResult(null);
//				agent.killComponent()				
			}
			
			public void exceptionOccurred(Exception exception)
			{
//				System.out.println("tests failed: "+agent.getComponentIdentifier());
				
				exception.printStackTrace();
				
				agent.getFeature(IArgumentsResultsFeature.class).getResults().put("testresults", tc);
				ret.setResult(null);
//				agent.killComponent()	
			}
		}));
		
		return ret;
	}
	
	/**
	 *  The agent body.
	 */
	protected IFuture<Void> performTests(final Testcase tc)
	{
		final Future<Void>	ret	= new Future<Void>();
		
		IFuture<IComponentManagementService>	fut	= agent.getFeature(IRequiredServicesFeature.class).getService("cms");
		fut.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Void>(ret)
		{
			public void customResultAvailable(final IComponentManagementService cms)
			{
//				agent.getLogger().severe("Testagent test local: "+agent.getComponentDescription());
				test(cms, true).addResultListener(new ExceptionDelegationResultListener<TestReport, Void>(ret)
				{
					public void customResultAvailable(TestReport result)
					{
//						agent.getLogger().severe("Testagent test local finished: "+agent.getComponentDescription());
						tc.addReport(result);
						setupRemotePlatform(false)
							.addResultListener(new ExceptionDelegationResultListener<IExternalAccess, Void>(ret)
						{
							public void customResultAvailable(final IExternalAccess exta)
							{
								exta.searchService( new ServiceQuery<>( IComponentManagementService.class))
									.addResultListener(agent.getFeature(IExecutionFeature.class).createResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Void>(ret)
								{
									public void customResultAvailable(IComponentManagementService cms2)
									{
//										agent.getLogger().severe("Testagent test remote: "+agent.getComponentDescription());
										test(cms2, false).addResultListener(new ExceptionDelegationResultListener<TestReport, Void>(ret)
										{
											public void customResultAvailable(TestReport result)
											{
//												agent.getLogger().severe("Testagent test remote finished: "+agent.getComponentDescription());
												tc.addReport(result);
												ret.setResult(null);
											}
										});
									}
								}));
							}
						});
					}
				});
			}
		});
		
		return ret;
	}
	
	/**
	 *  The test count.
	 */
	protected int	getTestCount()
	{
		return 2;
	}

	/**
	 * 
	 */
	protected IFuture<IExternalAccess> createPlatform(final String[] args)
	{
		return createPlatform(STest.getDefaultTestConfig(), args);
	}
	
	/**
	 * 
	 */
	protected IFuture<IExternalAccess> createPlatform(IPlatformConfiguration config, String[] args)
	{
		final Future<IExternalAccess> ret = new Future<IExternalAccess>();
		// Start platform
		Starter.createPlatform(config, args).addResultListener(agent.getFeature(IExecutionFeature.class).createResultListener(
			new DelegationResultListener<IExternalAccess>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				platforms.add(result);
				super.customResultAvailable(result);
			}
		}));
		return ret;
	}

	/**
	 * 
	 */
	protected IFuture<IComponentIdentifier> createComponent(final String filename,
		final IComponentIdentifier root, final  IResultListener<Map<String,Object>> reslis)
	{
		return createComponent(filename, null, null, root, reslis);
	}
	
	/**
	 * 
	 */
	protected IFuture<IComponentIdentifier> createComponent(final String filename, final Map<String, Object> args, 
		final String config, final IComponentIdentifier root, final IResultListener<Map<String,Object>> reslis)
	{
		final Future<IComponentIdentifier> ret = new Future<IComponentIdentifier>();
		
		agent.getFeature(IRequiredServicesFeature.class).searchService(new ServiceQuery<>(IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM))
			.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, IComponentIdentifier>(ret)
		{
			public void customResultAvailable(final IComponentManagementService cms)
			{
//				IResourceIdentifier	rid	= new ResourceIdentifier(
//					new LocalResourceIdentifier(root, agent.getModel().getResourceIdentifier().getLocalIdentifier().getUri()), null);
				boolean	local = root.equals(agent.getId().getRoot());
				CreationInfo ci	= new CreationInfo(local? agent.getId(): root, agent.getModel().getResourceIdentifier());
				ci.setArguments(args);
				ci.setConfiguration(config);
				ITuple2Future<IComponentIdentifier,Map<String,Object>> cmsfut = cms.createComponent(null, filename, ci);
//				cms.createComponent(null, filename, ci, reslis)
				cmsfut.addTuple2ResultListener(new DelegationResultListener<IComponentIdentifier>(ret)
				{
					public void customResultAvailable(IComponentIdentifier result)
					{
						super.customResultAvailable(result);
					}
					
					public void exceptionOccurred(Exception exception)
					{
						exception.printStackTrace();
						super.exceptionOccurred(exception);
					}
				}, reslis);
			}
		});
		
		return ret;
	}

	/**
	 *
	 */
	public static IFuture<IComponentIdentifier> createComponent(final IInternalAccess agent, final String filename, final Map<String, Object> args,
		final String config, final IComponentIdentifier root, final IResultListener<Map<String,Object>> reslis)
	{
		final Future<IComponentIdentifier> ret = new Future<IComponentIdentifier>();

		agent.getFeature(IRequiredServicesFeature.class).searchService(new ServiceQuery<>(IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM))
			.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, IComponentIdentifier>(ret)
		{
			public void customResultAvailable(final IComponentManagementService cms)
			{
//					IResourceIdentifier	rid	= new ResourceIdentifier(
//						new LocalResourceIdentifier(root, agent.getModel().getResourceIdentifier().getLocalIdentifier().getUri()), null);
				boolean	local = root.equals(agent.getId().getRoot());
				CreationInfo ci	= new CreationInfo(local? agent.getId(): root, agent.getModel().getResourceIdentifier());
				ci.setArguments(args);
				ci.setConfiguration(config);
				ITuple2Future<IComponentIdentifier, Map<String, Object>> cmsfut = cms.createComponent(null, filename, ci);
				cmsfut.addTuple2ResultListener(new DelegationResultListener<IComponentIdentifier>(ret)
				{
					public void customResultAvailable(IComponentIdentifier result)
					{
						super.customResultAvailable(result);
					}

					public void exceptionOccurred(Exception exception)
					{
						exception.printStackTrace();
						super.exceptionOccurred(exception);
					}
				}, reslis);
			}
		});

		return ret;
	}
	
	/**
	 * 
	 */
	protected IFuture<Map<String, Object>> destroyComponent(final IComponentIdentifier cid)
	{
		final Future<Map<String, Object>> ret = new Future<Map<String, Object>>();
		
		agent.getFeature(IRequiredServicesFeature.class).searchService(new ServiceQuery<>(IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM))
			.addResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Map<String, Object>>(ret)
		{
			public void customResultAvailable(final IComponentManagementService cms)
			{
				cms.destroyComponent(cid).addResultListener(new DelegationResultListener<Map<String, Object>>(ret));
			}
		});
		
		return ret;
	}
	
	/**
	 *  Setup a local test.
	 */
	protected IFuture<IComponentIdentifier>	setupLocalTest(String filename,  IResultListener<Map<String,Object>> reslis)
	{
		return createComponent(filename, agent.getId().getRoot(), reslis);
	}
	
	/**
	 *  Setup a remote test.
	 */
	protected IFuture<IComponentIdentifier>	setupRemoteTest(final String filename, final String config,
		final  IResultListener<Map<String,Object>> reslis, final boolean remove)
	{
		final Future<IComponentIdentifier>	ret	= new Future<IComponentIdentifier>();
		
		setupRemotePlatform(remove)
			.addResultListener(new ExceptionDelegationResultListener<IExternalAccess, IComponentIdentifier>(ret)
		{
			public void customResultAvailable(final IExternalAccess exta)
			{
//				ComponentIdentifier.getTransportIdentifier(exta)
//					.addResultListener(new ExceptionDelegationResultListener<ITransportComponentIdentifier, IComponentIdentifier>(ret)
//                {
//                    public void customResultAvailable(ITransportComponentIdentifier cid)
//                    {
						createComponent(filename, null, config, exta.getId(), reslis)
							.addResultListener(new DelegationResultListener<IComponentIdentifier>(ret));
//                    }
//                });
			}
		});
		
		return ret;
	}
	
	/**
	 *  Setup a remote test.
	 */
	protected IFuture<IExternalAccess>	setupRemotePlatform(final boolean manualremove)
	{
		final Future<IExternalAccess>	ret	= new Future<IExternalAccess>();
		
//		agent.getLogger().severe("Testagent setup remote platform: "+agent.getComponentDescription());
		createPlatform(null).addResultListener(new DelegationResultListener<IExternalAccess>(ret)
		{
			public void customResultAvailable(final IExternalAccess exta)
			{
				if(manualremove)
					platforms.remove(exta);
				
				createProxies(exta).addResultListener(new ExceptionDelegationResultListener<Void, IExternalAccess>(ret)
				{
					@Override
					public void customResultAvailable(Void result) throws Exception
					{
						ret.setResult(exta);
					}
				});
			}
		});
		
		return ret;
	}
	
	public <T> IFuture<T>	waitForRealtimeDelay(final long delay, final IComponentStep<T> step)
	{
		final Future<T>	ret	= new Future<T>();
		IFuture<IClockService>	clockfut	= agent.getFeature(IRequiredServicesFeature.class).getService("clock");
		clockfut.addResultListener(new ExceptionDelegationResultListener<IClockService, T>(ret)
		{
			public void customResultAvailable(IClockService clock)
			{
				clock.createRealtimeTimer(delay, new ITimedObject()
				{
					public void timeEventOccurred(long currenttime)
					{
						agent.getFeature(IExecutionFeature.class).scheduleStep(step).addResultListener(new DelegationResultListener<T>(ret));
					}
				});
			}
		});
		return ret;
	}
	
	/**
	 *  Perform  the test.
	 *  @param cms	The cms of the platform to test (local or remote).
	 * 	@param local	True when tests runs on local platform. 
	 *  @return	The test result.
	 */
	protected IFuture<TestReport>	test(IComponentManagementService cms, boolean local)
	{
		throw new UnsupportedOperationException("Implement test() or performTests()");
	}	
}
