package jadex.micro.testcases;

import java.util.LinkedHashMap;
import java.util.Map;

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
import jadex.bridge.service.annotation.OnEnd;
import jadex.bridge.service.annotation.OnStart;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.clock.ITimedObject;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.factory.IPlatformComponentAccess;
import jadex.bridge.service.types.security.ISecurityService;
import jadex.commons.SUtil;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.FutureBarrier;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;
import jadex.micro.annotation.Result;
import jadex.micro.annotation.Results;

@Agent
@RequiredServices(
{
//	@RequiredService(name="msgservice", type=IMessageService.class,
//		binding=@Binding(scope=ServiceScope.PLATFORM)),
	@RequiredService(name="clock", type=IClockService.class)
})
//@ComponentTypes(
//	@ComponentType(name="receiver", filename="jadex/micro/testcases/stream/ReceiverAgent.class")
//)
@Results(@Result(name="testresults", clazz=Testcase.class))
public abstract class TestAgent	extends RemoteTestBaseAgent
{
	@Agent
	protected IInternalAccess agent;
	
	protected Map<IComponentIdentifier, IExternalAccess>	platforms	= new LinkedHashMap<>();
	
	
	/**
	 *  Cleanup created platforms.
	 */
	//@AgentKilled
	@OnEnd
	public IFuture<Void>	cleanup()
	{
		if(((IPlatformComponentAccess)agent).getPlatformComponent().debug)
		{
			agent.getLogger().severe("cleanup0");
		}
		
		FutureBarrier<Void>	outer	= new FutureBarrier<Void>();
		outer.addFuture(super.cleanup());
		
		if(((IPlatformComponentAccess)agent).getPlatformComponent().debug)
		{
			agent.getLogger().severe("cleanup1 killing platforms"+platforms.keySet());
		}
		FutureBarrier<Map<String, Object>>	inner	= new FutureBarrier<Map<String,Object>>();
		for(final IExternalAccess platform: platforms.values())
		{
			inner.addFuture(platform.killComponent());
		}
		platforms	= null;
		if(((IPlatformComponentAccess)agent).getPlatformComponent().debug)
		{
			inner.waitFor().addResultListener(new IResultListener<Void>()
			{
				@Override
				public void resultAvailable(Void result)
				{
					agent.getLogger().severe("cleanup2 killed platforms");
				}
				@Override
				public void exceptionOccurred(Exception exception)
				{
					agent.getLogger().severe("cleanup3 failed killing platforms\n"+SUtil.getExceptionStacktrace(exception));
				}
			});
			agent.getLogger().severe("cleanup4 started killing platforms");
		}

		outer.addFuture(inner.waitFor());

		if(((IPlatformComponentAccess)agent).getPlatformComponent().debug)
		{
			outer.waitFor().addResultListener(new IResultListener<Void>()
			{
				@Override
				public void resultAvailable(Void result)
				{
					agent.getLogger().severe("cleanup5 finished");
				}
				@Override
				public void exceptionOccurred(Exception exception)
				{
					agent.getLogger().severe("cleanup6 failed\n"+SUtil.getExceptionStacktrace(exception));
				}
			});
		}

		return outer.waitFor();
	}
	
	/**
	 *  The agent body.
	 */
	//@AgentBody
	@OnStart
	public IFuture<Void> body()
	{
		ISecurityService ss = agent.getLocalService(ISecurityService.class);
		ss.setNetwork(STest.testnetwork_name, STest.testnetwork_pass).get();
		
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
				System.out.println("tests failed: "+agent.getId());
				
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
		
		test(agent.getExternalAccess(), true).addResultListener(agent.createResultListener(
				new ExceptionDelegationResultListener<TestReport, Void>(ret)
		{
			public void customResultAvailable(TestReport result)
			{
//				agent.getLogger().severe("Testagent test local finished: "+agent.getComponentDescription());
				tc.addReport(result);
				setupRemotePlatform(false)
					.addResultListener(new ExceptionDelegationResultListener<IExternalAccess, Void>(ret)
				{
					public void customResultAvailable(final IExternalAccess exta)
					{
//						agent.getLogger().severe("Testagent test remote: "+agent.getComponentDescription());
						test(exta, false).addResultListener(new ExceptionDelegationResultListener<TestReport, Void>(ret)
						{
							public void customResultAvailable(TestReport result)
							{
//								agent.getLogger().severe("Testagent test remote finished: "+agent.getComponentDescription());
								tc.addReport(result);
								ret.setResult(null);
							}
						});
					}
				});
			}
		}));
		
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
		return createPlatform(STest.getDefaultTestConfig(getClass()), args);
	}
	
	/**
	 * 
	 */
	protected IFuture<IExternalAccess> createPlatform(IPlatformConfiguration config, String[] args)
	{
		if(((IPlatformComponentAccess)agent).getPlatformComponent().debug)
		{
			config.setLogging(true);
		}

		final Future<IExternalAccess> ret = new Future<IExternalAccess>();
		// Start platform
		Starter.createPlatform(config, args).addResultListener(agent.getFeature(IExecutionFeature.class).createResultListener(
			new DelegationResultListener<IExternalAccess>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				platforms.put(result.getId(), result);
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
		return createComponent(filename, args, config, root, null, reslis);
	}
	
	/**
	 * 
	 */
	protected IFuture<IComponentIdentifier> createComponent(final String filename, final Map<String, Object> args, 
		final String config, final IComponentIdentifier root, IExternalAccess platform, final IResultListener<Map<String,Object>> reslis)
	{
		final Future<IComponentIdentifier> ret = new Future<IComponentIdentifier>();
		
//		IResourceIdentifier	rid	= new ResourceIdentifier(
//			new LocalResourceIdentifier(root, agent.getModel().getResourceIdentifier().getLocalIdentifier().getUri()), null);
		boolean	local = root.equals(agent.getId().getRoot());
		CreationInfo ci	= new CreationInfo(agent.getModel().getResourceIdentifier());
		ci.setArguments(args);
		ci.setConfiguration(config);
		ci.setFilename(filename);
		IFuture<IExternalAccess> cmsfut = (local ? agent.getExternalAccess() : platform!=null? platform :platforms.get(root)).createComponent(ci);
		cmsfut.addResultListener(new ExceptionDelegationResultListener<IExternalAccess, IComponentIdentifier>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				ret.setResult(result.getId());
				result.waitForTermination().addResultListener(reslis);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				exception.printStackTrace();
				super.exceptionOccurred(exception);
			}
		});
		
		return ret;
	}

	/**
	 *
	 */
	public static IFuture<IComponentIdentifier> createComponent(final IInternalAccess agent, final String filename, final Map<String, Object> args,
		final String config, final IComponentIdentifier root, IExternalAccess platform, final IResultListener<Map<String,Object>> reslis)
	{
		final Future<IComponentIdentifier> ret = new Future<IComponentIdentifier>();

		boolean	local = root.equals(agent.getId().getRoot());
		CreationInfo ci	= new CreationInfo(agent.getModel().getResourceIdentifier());
		ci.setArguments(args);
		ci.setConfiguration(config);
		ci.setFilename(filename);
		IFuture<IExternalAccess> cmsfut = (local? agent.getExternalAccess(): platform).createComponent(ci);
		cmsfut.addResultListener(new ExceptionDelegationResultListener<IExternalAccess, IComponentIdentifier>(ret)
		{
			public void customResultAvailable(IExternalAccess result)
			{
				ret.setResult(result.getId());
				result.waitForTermination().addResultListener(reslis);
			}
			
			public void exceptionOccurred(Exception exception)
			{
				exception.printStackTrace();
				super.exceptionOccurred(exception);
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
		
		agent.getExternalAccess(cid).killComponent().addResultListener(new DelegationResultListener<Map<String, Object>>(ret));
		
		return ret;
	}
	
	/**
	 *  Setup a remote test.
	 */
	protected IFuture<IExternalAccess>	setupRemotePlatform(final boolean manualremove)
	{
		final Future<IExternalAccess>	ret	= new Future<IExternalAccess>();
		
		disableLocalSimulationMode().get();
		
//		agent.getLogger().severe("Testagent setup remote platform: "+agent.getComponentDescription());
		IPlatformConfiguration conf = STest.getDefaultTestConfig(getClass());
		conf.getExtendedPlatformConfiguration().setSimul(false);
		conf.getExtendedPlatformConfiguration().setSimulation(false);
		//conf.getExtendedPlatformConfiguration().setDebugFutures(true);
		createPlatform(conf, null).addResultListener(new DelegationResultListener<IExternalAccess>(ret)
		{
			public void customResultAvailable(final IExternalAccess exta)
			{
				if(manualremove)
					platforms.remove(exta.getId());
				
				ret.setResult(exta);
				
//				createProxies(exta).addResultListener(new ExceptionDelegationResultListener<Void, IExternalAccess>(ret)
//				{
//					@Override
//					public void customResultAvailable(Void result) throws Exception
//					{
//						ret.setResult(exta);
//					}
//				});
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
	protected IFuture<TestReport> test(IExternalAccess platform, boolean local)
	{
		throw new UnsupportedOperationException("Implement test() or performTests()");
	}	
}
