package jadex.bridge.component.impl;

import jadex.base.Starter;
import jadex.bridge.ComponentResultListener;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.IntermediateComponentResultListener;
import jadex.bridge.component.ComponentCreationInfo;
import jadex.bridge.component.IComponentFeature;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.service.IService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.interceptors.FutureFunctionality;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.clock.IClockService;
import jadex.bridge.service.types.clock.ITimedObject;
import jadex.bridge.service.types.execution.IExecutionService;
import jadex.commons.IResultCommand;
import jadex.commons.Tuple2;
import jadex.commons.concurrent.IExecutable;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.commons.future.IResultListener;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *  This feature provides component step execution.
 */
public class ExecutionComponentFeature	extends	AbstractComponentFeature	implements IExecutionFeature, IExecutable
{
	//-------- attributes --------
	
	/** The component steps. */
	protected List<Tuple2<IComponentStep<?>, Future<?>>>	steps;
	
	/** The immediate component steps. */
	protected List<Tuple2<IComponentStep<?>, Future<?>>>	isteps;
	
	//-------- constructors --------
	
	/**
	 *  Create the feature.
	 */
	public ExecutionComponentFeature(IInternalAccess component, ComponentCreationInfo cinfo)
	{
		super(component, cinfo);
	}
	
	//-------- IComponentFeature interface --------
	
	/**
	 *  Get the user interface type of the feature.
	 */
	public Class<?>	getType()
	{
		return IExecutionFeature.class;
	}
	
	/**
	 *  Create an instance of the feature.
	 */
	public IComponentFeature createInstance(IInternalAccess access, ComponentCreationInfo info)
	{
		return new ExecutionComponentFeature(access, info);
	}
	
	//-------- IExecutionFeature interface --------
	
	/**
	 *  Execute a component step.
	 */
	public <T>	IFuture<T> scheduleStep(IComponentStep<T> step)
	{
		final Future ret = createStepFuture(step);
		
		synchronized(this)
		{
			if(steps==null)
			{
				steps	= new LinkedList<Tuple2<IComponentStep<?>,Future<?>>>();
			}
			steps.add(new Tuple2<IComponentStep<?>, Future<?>>(step, ret));
		}

		wakeup();
		
		return ret;
	}
	
	/**
	 *  Execute an immediate component step,
	 *  i.e., the step is executed also when the component is currently suspended.
	 */
	public <T>	IFuture<T> scheduleImmediate(IComponentStep<T> step)
	{
		final Future ret = createStepFuture(step);
		
		synchronized(this)
		{
			if(isteps==null)
			{
				isteps	= new LinkedList<Tuple2<IComponentStep<?>,Future<?>>>();
			}
			isteps.add(new Tuple2<IComponentStep<?>, Future<?>>(step, ret));
		}

		wakeup();
		
		return ret;
	}
	
	/**
	 *  Wait for some time and execute a component step afterwards.
	 */
	public <T> IFuture<T> waitForDelay(final long delay, final IComponentStep<T> step)
	{
		return waitForDelay(delay, step, false);
	}
	
	/**
	 *  Wait for some time and execute a component step afterwards.
	 */
	public <T> IFuture<T> waitForDelay(final long delay, final IComponentStep<T> step, final boolean realtime)
	{
		// todo: remember and cleanup timers in case of component removal.
		
		final Future<T> ret = new Future<T>();
		
		SServiceProvider.getService(getComponent(), IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(createResultListener(new ExceptionDelegationResultListener<IClockService, T>(ret)
		{
			public void customResultAvailable(IClockService cs)
			{
				ITimedObject	to	= new ITimedObject()
				{
					public void timeEventOccurred(long currenttime)
					{
						scheduleStep(step).addResultListener(createResultListener(new DelegationResultListener<T>(ret)));
					}
					
					public String toString()
					{
						return "waitForDelay[Step]("+getComponent().getComponentIdentifier()+")";
					}
				};
				if(realtime)
				{
					cs.createRealtimeTimer(delay, to);
				}
				else
				{
					cs.createTimer(delay, to);					
				}
			}
		}));
		
		return ret;
	}
	
	/**
	 *  Wait for some time.
	 */
	public IFuture<Void> waitForDelay(final long delay)
	{
		return waitForDelay(delay, false);
	}

	/**
	 *  Wait for some time.
	 */
	public IFuture<Void> waitForDelay(final long delay, final boolean realtime)
	{
		final Future<Void> ret = new Future<Void>();
		
		SServiceProvider.getService(getComponent(), IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(createResultListener(new ExceptionDelegationResultListener<IClockService, Void>(ret)
		{
			public void customResultAvailable(IClockService cs)
			{
				ITimedObject	to	=  	new ITimedObject()
				{
					public void timeEventOccurred(long currenttime)
					{
						scheduleStep(new IComponentStep<Void>()
						{
							public IFuture<Void> execute(IInternalAccess ia)
							{
								ret.setResult(null);
								return IFuture.DONE;
							}
						});
					}
					
					public String toString()
					{
						return "waitForDelay("+getComponent().getComponentIdentifier()+")";
					}
				};
				
				if(realtime)
				{
					cs.createRealtimeTimer(delay, to);
				}
				else
				{
					cs.createTimer(delay, to);
				}
			}
		}));
		
		return ret;
	}
	
	// todo:?
//	/**
//	 *  Wait for some time and execute a component step afterwards.
//	 */
//	public IFuture waitForImmediate(long delay, IComponentStep step);
	
	/** Flag to indicate bootstrapping execution of main thread (only for platform, hack???). */
	protected volatile boolean bootstrap;
	
	/** Flag to indicate that the execution service has become available during bootstrapping (only for platform, hack???). */
	protected volatile boolean available;
	
	/**
	 *  Trigger component execution.
	 */
	protected void	wakeup()
	{
		SServiceProvider.getService(component, IExecutionService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new IResultListener<IExecutionService>()
		{
			public void resultAvailable(IExecutionService exe)
			{
				// Hack!!! service is foudn before it is started, grrr.
				if(((IService)exe).isValid().get(null).booleanValue())	// Hack!!! service is raw
				{
					if(bootstrap)
					{
						// Execution service found during bootstrapping execution -> stop bootstrapping as soon as possible.
						available	= true;
					}
					else
					{
						exe.execute(ExecutionComponentFeature.this);
					}
				}
				else
				{
					exceptionOccurred(null);
				}
			}
			
			public void exceptionOccurred(Exception exception)
			{
				// Happens during platform bootstrapping -> execute on platform rescue thread.
				if(!bootstrap)
				{
					bootstrap	= true;
					Starter.scheduleRescueStep(getComponent().getComponentIdentifier().getRoot(), new Runnable()
					{
						public void run()
						{
							boolean	again	= true;
							while(!available && again)
							{
								again	= execute();
							}
							bootstrap	= false;
							
							if(again)
							{					
								// Bootstrapping finished -> do real kickoff
								wakeup();
							}
						}
					});
				}
			}
		});
	}

	/**
	 *  Test if current thread is the component thread.
	 *  @return True if the current thread is the component thread.
	 */
	public boolean isComponentThread()
	{
		// Todo
		return true;
	}
	
	/**
	 *  Create a result listener that is executed on the
	 *  component thread.
	 */
	public <T> IResultListener<T> createResultListener(IResultListener<T> listener)
	{
		return new ComponentResultListener<T>(listener, component);
	}
	
	/**
	 *  Create a result listener that is executed on the
	 *  component thread.
	 */
	public <T> IIntermediateResultListener<T> createResultListener(IIntermediateResultListener<T> listener)
	{
		return new IntermediateComponentResultListener<T>(listener, component);
	}
	
	//-------- IExecutable interface --------
	
	/**
	 *  Execute the executable.
	 *  @return True, if the object wants to be executed again.
	 */
	public boolean execute()
	{
		Tuple2<IComponentStep<?>, Future<?>>	step	= null;
		synchronized(this)
		{
			if(isteps!=null)
			{
				step	= isteps.remove(0);
				if(isteps.isEmpty())
				{
					isteps	= null;
				}
			}
			else if(steps!=null)
			{
				step	= steps.remove(0);
				if(steps.isEmpty())
				{
					steps	= null;
				}
			}
		}
		
		boolean	again;
		
		if(step!=null)
		{
			step.getFirstEntity().execute(component)
				.addResultListener(new DelegationResultListener(step.getSecondEntity()));
			
			synchronized(this)
			{
				again	= isteps!=null || steps!=null;
			}
		}
		else
		{
			again	= false;
		}
		
		return again;
	}
	
	/**
	 *  Create intermediate of direct future.
	 */
	protected Future<?> createStepFuture(IComponentStep<?> step)
	{
		Future<?> ret;
		try
		{
			Method method = step.getClass().getMethod("execute", new Class[]{IInternalAccess.class});
			Class<?> clazz = method.getReturnType();
//			ret = FutureFunctionality.getDelegationFuture(clazz, new FutureFunctionality(getLogger()));
			// Must not be fetched before properties are available!
			ret = FutureFunctionality.getDelegationFuture(clazz, new FutureFunctionality(new IResultCommand<Logger, Void>()
			{
				public Logger execute(Void args)
				{
					return getComponent().getLogger();
				}
			}));
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		
		return ret;
	}
}
