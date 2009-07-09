package jadex.bpmn.examples.helloworld;

import java.util.Timer;
import java.util.TimerTask;

import jadex.bpmn.model.MActivity;
import jadex.bpmn.runtime.AbstractEventIntermediateTimerActivityHandler;
import jadex.bpmn.runtime.BpmnInstance;
import jadex.bpmn.runtime.ProcessThread;
import jadex.bpmn.runtime.ThreadContext;

/**
 *  Simple platform specific timer implementation.
 *  Uses java.util.Timer for testing purposes.
 */
public class EventIntermediateTimerActivityHandler extends	AbstractEventIntermediateTimerActivityHandler
{
	/**
	 *  Template method to be implemented by platform-specific subclasses.
	 *  @param activity	The timing event activity.
	 *  @param instance	The process instance.
	 *  @param thread	The process thread.
	 *  @param context	The thread context.
	 *  @param duration	The duration to wait.
	 */
	public void doWait(final MActivity activity, final BpmnInstance instance, final ProcessThread thread, final ThreadContext context, long duration)
	{
		final Timer	timer	= new Timer();
		timer.schedule(new TimerTask()
		{	
			public void run()
			{
				timer.cancel();
				EventIntermediateTimerActivityHandler.this.notify(activity, instance, thread, context);
			}
		}, duration);
	}
}
