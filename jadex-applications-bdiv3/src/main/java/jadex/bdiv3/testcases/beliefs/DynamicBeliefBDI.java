package jadex.bdiv3.testcases.beliefs;

import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Belief;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.runtime.ChangeEvent;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Result;
import jadex.micro.annotation.Results;

/**
 *  Agent that has two beliefs. 
 *  num2 belief depends on num1 and a plan depends on changes of num2.
 */
@Agent
@Results(@Result(name="testresults", clazz=Testcase.class))
public class DynamicBeliefBDI
{
	/** The agent (injected). */
	@Agent
	protected BDIAgent agent;
	
	/** The belief num1. */
	@Belief
	protected int num1 = 1;
	
	/** The belief num2 depending on num1. */
	@Belief(dynamic=true)
	protected int num2 = num1+1;
	
	/** The test report. */
	protected TestReport	tr	= new TestReport("#1", "Test if dynamic belief works.");
	
	/**
	 *  Plan that reacts on belief changes of num2.
	 */
	@Plan(trigger=@Trigger(factchangeds="num2"))
	protected void successPlan(ChangeEvent event)
	{
		System.out.println("plan activated: num2 changed to "+event.getValue());
		tr.setSucceeded(true);
		agent.setResultValue("testresults", new Testcase(1, new TestReport[]{tr}));
		agent.killAgent();
	}

	/**
	 *  The agent body.
	 */
	@AgentBody
	public void body()
	{
		num1++;
//		num1++;
		agent.scheduleStep(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				if(!tr.isFinished())
					tr.setFailed("Plan was not activated due to belief change.");
				agent.killAgent();
				return IFuture.DONE;
			}
		}, 3000);
	}
}


