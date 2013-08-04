package jadex.bdiv3.testcases.plans;

import jadex.base.test.TestReport;
import jadex.base.test.Testcase;
import jadex.bdiv3.BDIAgent;
import jadex.bdiv3.annotation.Goal;
import jadex.bdiv3.annotation.Plan;
import jadex.bdiv3.annotation.PlanBody;
import jadex.bdiv3.annotation.PlanPrecondition;
import jadex.bdiv3.annotation.Trigger;
import jadex.bdiv3.runtime.impl.PlanFailureException;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentBody;
import jadex.micro.annotation.Result;
import jadex.micro.annotation.Results;

@Agent
@Results(@Result(name="testresults", clazz=Testcase.class))
public class PlanPreconditionBDI
{
	/** The agent. */
	@Agent
	protected BDIAgent agent;

	protected String res = "";
	
	@Goal
	protected class SomeGoal
	{
	}
	
	/**
	 *  The agent body.
	 */
	@AgentBody
	public void body()
	{
		TestReport tr = new TestReport("#1", "Test if plan precondition works.");
		agent.dispatchTopLevelGoal(new SomeGoal()).get();
		if("AC".equals(res))
		{
			tr.setSucceeded(true);
		}
		else
		{
			tr.setFailed("Wrong plans executed: "+res);
		}
		agent.setResultValue("testresults", new Testcase(1, new TestReport[]{tr}));
		agent.killAgent();
	}
	
	@Plan(trigger=@Trigger(goals=SomeGoal.class), priority=2)
	protected class PlanA
	{
		@PlanPrecondition
		protected boolean precondition()
		{
			return true;
		}
		
		@PlanBody
		protected IFuture<Void> body()
		{
			System.out.println("Plan A");
			res += "A";
			return new Future<Void>(new PlanFailureException());
//			return IFuture.DONE;
		}
	}
	
	@Plan(trigger=@Trigger(goals=SomeGoal.class), priority=1)
	protected class PlanB
	{
		@PlanPrecondition
		protected IFuture<Boolean> precondition()
		{
			return new Future<Boolean>(Boolean.FALSE);
		}
		
		@PlanBody
		protected IFuture<Void> body()
		{
			System.out.println("Plan B");
			res += "B";
			return IFuture.DONE;
		}
	}
	
	@Plan(trigger=@Trigger(goals=SomeGoal.class), priority=0)
	protected class PlanC
	{
		@PlanPrecondition
		protected IFuture<Boolean> precondition()
		{
			return new Future<Boolean>(Boolean.TRUE);
		}
		
		@PlanBody
		protected IFuture<Void> body()
		{
			System.out.println("Plan C");
			res += "C";
			return IFuture.DONE;
		}
	}
}
