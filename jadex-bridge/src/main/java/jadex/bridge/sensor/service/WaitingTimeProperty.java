package jadex.bridge.sensor.service;

import jadex.bridge.IInternalAccess;
import jadex.bridge.service.IService;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.component.BasicServiceInvocationHandler;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.MethodInfo;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 *  Property for the waiting time of a method or a service as a whole.
 */
public class WaitingTimeProperty extends TimedProperty
{
	/** The name of the property. */
	public static final String NAME = "waiting time";
	
	/** The handler. */
	protected BasicServiceInvocationHandler handler;
	
	/** The listener. */
	protected IMethodInvocationListener listener;
	
	/** The method info. */
	protected MethodInfo method;
	
	/** The clock. */
	protected IClockService clock;
	
	/**
	 *  Create a new property.
	 */
	public WaitingTimeProperty(final IInternalAccess comp, IService service, MethodInfo method)
	{
		super(NAME, comp, true);
		this.method = method;
		
		SServiceProvider.getService(comp.getServiceContainer(), IClockService.class, RequiredServiceInfo.SCOPE_PLATFORM)
			.addResultListener(new IResultListener<IClockService>()
		{
			public void resultAvailable(IClockService result)
			{
				WaitingTimeProperty.this.clock = result;
//				System.out.println("assigned clock");
			}
			
			public void exceptionOccurred(Exception exception)
			{
				comp.getLogger().warning("Could not fetch time service in property.");
			}
		});
		
		if(Proxy.isProxyClass(service.getClass()))
		{
			handler = (BasicServiceInvocationHandler)Proxy.getInvocationHandler(service);
			listener = new UserMethodInvocationListener(new IMethodInvocationListener()
			{
				Map<Object, Long> times = new HashMap<Object, Long>();
				
				public void methodCallStarted(Object proxy, Method method, Object[] args, Object callid)
				{
					if(clock!=null)
						times.put(callid, new Long(clock.getTime()));
				}
				
				public void methodCallFinished(Object proxy, Method method, Object[] args, Object callid)
				{
					if(clock!=null)
					{
						Long start = times.remove(callid);
						// May happen that property is added during ongoing call
						if(start!=null)
						{
							long dur = clock.getTime() - start.longValue();
							setValue(dur);
						}
					}
				}
			});
			handler.addMethodListener(method, listener);
		}
		else
		{
			throw new RuntimeException("Cannot install waiting time listener hook.");
		}
	}
	
	/**
	 *  Measure the value.
	 */
	public Long measureValue()
	{
		return null;
//		throw new UnsupportedOperationException();
	}
	
	/**
	 *  Set the value.
	 */
	public void setValue(Long value) 
	{
		// ema calculatio: EMAt = EMAt-1 +(SF*(Ct-EMAt-1)) SF=2/(n+1)
		if(this.value!=null && value!=null)
		{
			double sf = 2d/(10d+1); // 10 periods per default
			double delta = value-this.value;
			value = new Long((long)(this.value+sf*delta));
		}
		
		if(value!=null)
		{
//			System.out.println("Setting value: "+value);
			super.setValue((long)value);
		}
	}
	
	/**
	 *  Property was removed and should be disposed.
	 */
	public IFuture<Void> dispose()
	{
		if(handler!=null)
			handler.removeMethodListener(method, listener);
		return IFuture.DONE;
	}
}
