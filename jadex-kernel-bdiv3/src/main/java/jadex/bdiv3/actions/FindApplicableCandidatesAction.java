package jadex.bdiv3.actions;

import jadex.bdiv3.runtime.impl.APL;
import jadex.bdiv3.runtime.impl.RGoal;
import jadex.bdiv3.runtime.impl.RProcessableElement;
import jadex.bridge.IConditionalComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IExecutionFeature;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

/**
 * 
 */
public class FindApplicableCandidatesAction implements IConditionalComponentStep<Void>
{
	/** The processable element. */
	protected RProcessableElement element;
	
	/**
	 *  Create a new action.
	 */
	public FindApplicableCandidatesAction(RProcessableElement element)
	{
		this.element = element;
	}
	
	/**
	 *  Test if the action is valid.
	 *  @return True, if action is valid.
	 */
	public boolean isValid()
	{
		boolean ret = true;
		
		if(element instanceof RGoal)
		{
			RGoal rgoal = (RGoal)element;
			ret = RGoal.GoalLifecycleState.ACTIVE.equals(rgoal.getLifecycleState())
				&& RGoal.GoalProcessingState.INPROCESS.equals(rgoal.getProcessingState());
		}
			
//		if(!ret)
//			System.out.println("not valid: "+this+" "+element);
		
		return ret;
	}
	
	/**
	 *  Execute the command.
	 *  @param args The argument(s) for the call.
	 *  @return The result of the command.
	 */
	public IFuture<Void> execute(final IInternalAccess ia)
	{
//		if(element!=null && element.toString().indexOf("GoH")!=-1)
//			System.out.println("find applicable candidates: "+element);
		final Future<Void> ret = new Future<Void>();
		
//		System.out.println("find applicable candidates 1: "+element);
		final APL apl = element.getApplicablePlanList();
		apl.build(ia).addResultListener(ia.getComponentFeature(IExecutionFeature.class).createResultListener(new DelegationResultListener<Void>(ret)
		{
			public void customResultAvailable(Void result)
			{
//				System.out.println("find applicable candidates 2: "+element+" "+apl);
				if(apl.isEmpty())
				{
					element.setState(RProcessableElement.State.NOCANDIDATES);
					element.planFinished(ia, null);
//					element.reason(ia);
				}
				else
				{
					element.setState(RProcessableElement.State.APLAVAILABLE);
					ia.getExternalAccess().scheduleStep(new SelectCandidatesAction(element));
				}
				ret.setResult(null);
			}
		}));
		
		return ret;
	}
}
