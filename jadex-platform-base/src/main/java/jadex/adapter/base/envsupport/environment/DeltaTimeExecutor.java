package jadex.adapter.base.envsupport.environment;

import jadex.adapter.base.appdescriptor.ApplicationContext;
import jadex.adapter.base.envsupport.dataview.IDataView;
import jadex.adapter.base.envsupport.math.IVector1;
import jadex.adapter.base.envsupport.math.Vector1Long;
import jadex.adapter.base.execution.IExecutionService;
import jadex.bridge.IClockService;
import jadex.bridge.IPlatform;
import jadex.commons.ChangeEvent;
import jadex.commons.IChangeListener;
import jadex.commons.SimplePropertyObject;
import jadex.commons.concurrent.IExecutable;

import java.util.Iterator;

/**
 * Space executor that connects to a clock service and reacts on time deltas.
 */
// Todo: immediate execution of agent actions and percepts?
public class DeltaTimeExecutor extends SimplePropertyObject implements ISpaceExecutor
{
	//-------- attributes --------
	
	/** Current time stamp */
	protected long timestamp;
	
	//-------- constructors--------
	
	/**
	 * Creates a new DeltaTimeExecutor
	 * @param timecoefficient the time coefficient
	 * @param clockservice the clock service
	 */
	public DeltaTimeExecutor()
	{
	}
	
	/**
	 * Creates a new DeltaTimeExecutor
	 * @param timecoefficient the time coefficient
	 * @param clockservice the clock service
	 */
	public DeltaTimeExecutor(final AbstractEnvironmentSpace space)
	{
		setProperty("space", space);
	}
	
	//-------- methods --------
	
	/**
	 *  Start the space executor.
	 */
	public void start()
	{
		final AbstractEnvironmentSpace space = (AbstractEnvironmentSpace)getProperty("space");
		IPlatform	platform	= ((ApplicationContext)space.getContext()).getPlatform();
		final IClockService clockservice = (IClockService)platform.getService(IClockService.class);
		final IExecutionService exeservice = (IExecutionService)platform.getService(IExecutionService.class);
		
		final IExecutable	executable	= new IExecutable()
		{
			public boolean execute()
			{
				long currenttime = clockservice.getTime();
				IVector1 progress = new Vector1Long(currenttime - timestamp);
				timestamp = currenttime;

//				System.out.println("step: "+timestamp+" "+progress);
	
				synchronized(space.getMonitor())
				{
					// Update the environment objects.
					for(Iterator it = space.getSpaceObjectsCollection().iterator(); it.hasNext(); )
					{
						SpaceObject obj = (SpaceObject)it.next();
						obj.updateObject(space, progress);
					}
					
					// Execute the scheduled agent actions.
					space.getAgentActionList().executeActions(null, true);
					
					// Execute the processes.
					Object[] procs = space.getProcesses().toArray();
					for(int i = 0; i < procs.length; ++i)
					{
						ISpaceProcess process = (ISpaceProcess) procs[i];
						process.execute(clockservice, space);
					}
					
					// Update the views.
					for (Iterator it = space.getViews().iterator(); it.hasNext(); )
					{
						IDataView view = (IDataView) it.next();
						view.update(space);
					}

					// Send the percepts to the agents.
					space.getPerceptList().processPercepts(null);
				}
				return false;
			}
		};
		
		this.timestamp = clockservice.getTime();
		
		// Start the processes.
		Object[] procs = space.getProcesses().toArray();
		for(int i = 0; i < procs.length; ++i)
		{
			ISpaceProcess process = (ISpaceProcess) procs[i];
			process.start(clockservice, space);
		}

		clockservice.addChangeListener(new IChangeListener()
		{
			public void changeOccurred(ChangeEvent e)
			{
				exeservice.execute(executable);
			}
		});
	}
}
