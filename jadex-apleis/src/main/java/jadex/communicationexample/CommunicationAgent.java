package jadex.communicationexample;

import jadex.microkernel.MicroAgent;

import communicationexample.EnvironmentInterface;

import eis.AgentListener;
import eis.iilang.Action;
import eis.iilang.Identifier;
import eis.iilang.Percept;
import eis.jadex.EisSpace;

/**
 *  Simple agent that communicates over the environment.
 */
public class CommunicationAgent extends MicroAgent
{
	//-------- attributes --------
	
	/** The environment interface. */
//	protected EnvironmentInterface ei;
	
	//-------- methods --------
	
	/**
	 *  Called once after agent creation.
	 * /
	public void agentCreated()
	{
		EisSpace eisspace = (EisSpace)getApplicationContext().getSpace("myenv");
		final EnvironmentInterfaceStandard eis = eisspace.getEis();

		try
		{
			eis.associateEntity(getAgentIdentifier().getName(), (String)eis.getFreeEntities().getFirst());
		}
		catch(RelationException e)
		{
			throw new RuntimeException(e);
		}
	}*/
	
	/**
	 *  Execute the functional body of the agent.
	 */
	public void executeBody()
	{
		EisSpace eisspace = (EisSpace)getApplicationContext().getSpace("myenv");
		final EnvironmentInterface eis = (EnvironmentInterface)eisspace.getEis();

		try
		{
			String myname = getAgentIdentifier().getLocalName();
			eis.registerAgent(myname);

			String myentityname = "en"+myname.substring(myname.length()-1);
			eis.associateEntity(myname, myentityname);
			
			eis.attachAgentListener(getAgentIdentifier().getLocalName(),new AgentListener()
			{
				public void handlePercept(String agent, Percept percept)
				{
					System.out.println("Agent received percept: "+percept);
				}
			});
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		
		Runnable run = new Runnable()
		{
			public void run()
			{
				try
				{
					String tell = (String)getArgument("tell")==null? "huhu" : (String)getArgument("tell");
					eis.performAction(getAgentIdentifier().getLocalName(), 
						new Action("tellall", new Identifier(tell)));
				}
				catch(Exception e)
				{
					throw new RuntimeException(e);
				}
			}
		};
		
		// Hack! Should not have to wait.
		waitFor(50, run);
	}
}
