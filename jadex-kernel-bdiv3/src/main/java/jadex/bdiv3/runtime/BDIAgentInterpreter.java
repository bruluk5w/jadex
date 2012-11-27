package jadex.bdiv3.runtime;

import jadex.bdiv3.PojoBDIAgent;
import jadex.bdiv3.actions.ExecutePlanStepAction;
import jadex.bdiv3.model.BDIModel;
import jadex.bdiv3.model.MBelief;
import jadex.bdiv3.model.MGoal;
import jadex.bdiv3.model.MPlan;
import jadex.bdiv3.model.MTrigger;
import jadex.bridge.IConditionalComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.service.RequiredServiceBinding;
import jadex.bridge.service.types.cms.IComponentDescription;
import jadex.bridge.service.types.factory.IComponentAdapterFactory;
import jadex.commons.SReflect;
import jadex.commons.Tuple2;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateResultListener;
import jadex.micro.MicroAgent;
import jadex.micro.MicroAgentInterpreter;
import jadex.micro.MicroModel;
import jadex.rules.eca.IAction;
import jadex.rules.eca.ICondition;
import jadex.rules.eca.IEvent;
import jadex.rules.eca.IRule;
import jadex.rules.eca.Rule;
import jadex.rules.eca.RuleSystem;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 */
public class BDIAgentInterpreter extends MicroAgentInterpreter
{
	/** The bdi model. */
	protected BDIModel bdimodel;
	
	/** The rule system. */
	protected RuleSystem rulesystem;
	
	/** The bdi state. */
	protected RCapability capa;
	
	/**
	 *  Create a new agent.
	 */
	public BDIAgentInterpreter(IComponentDescription desc, IComponentAdapterFactory factory, 
		final BDIModel model, Class<?> agentclass, final Map<String, Object> args, final String config, 
		final IExternalAccess parent, RequiredServiceBinding[] bindings, boolean copy, boolean realtime,
		final IIntermediateResultListener<Tuple2<String, Object>> listener, final Future<Void> inited)
	{
		super(desc, factory, model, agentclass, args, config, parent, bindings, copy, realtime, listener, inited);
		this.bdimodel = model;
		this.capa = new RCapability(bdimodel.getCapability());
	}
	
	/**
	 *  Create the agent.
	 */
	protected MicroAgent createAgent(Class agentclass, MicroModel model) throws Exception
	{
		MicroAgent ret = null;
		BDIModel bdim = (BDIModel)model;
		
		final Object agent = agentclass.newInstance();
		if(agent instanceof MicroAgent)
		{
			ret = (MicroAgent)agent;
			ret.init(this);
		}
		else // if pojoagent
		{
			PojoBDIAgent pa = new PojoBDIAgent();
			pa.init(this, agent);
			ret = pa;

			Field[] fields = model.getAgentInjections();
			for(int i=0; i<fields.length; i++)
			{
//				if(fields[i].isAnnotationPresent(Agent.class))
//				{
					try
					{
						// todo: cannot use fields as they are from the 'not enhanced' class
						Field field = agent.getClass().getDeclaredField(fields[i].getName());
						field.setAccessible(true);
						field.set(agent, ret);
					}
					catch(Exception e)
					{
						getLogger().warning("Agent injection failed: "+e);
					}
//				}
			}
			
			// Additionally inject agent to hidden agent field
			try
			{
				// todo: cannot use fields as they are from the 'not enhanced' class
				Field field = agent.getClass().getDeclaredField("__agent");
				field.setAccessible(true);
				field.set(agent, ret);
			}
			catch(Exception e)
			{
				getLogger().warning("Hidden agent injection failed: "+e);
			}
		}
		
		// Init rule system
		this.rulesystem = new RuleSystem(agent);
		
		// Inject belief collections.
		Object target = ret instanceof PojoBDIAgent? ((PojoBDIAgent)ret).getPojoAgent(): ret;
		List<MBelief> mbels = bdim.getCapability().getBeliefs();
		for(MBelief mbel: mbels)
		{
			try
			{
				Field f = mbel.getTarget().getField(getClassLoader());
				f.setAccessible(true);
				Object val = f.get(target);
				if(val==null)
				{
					String impl = mbel.getImplClassName();
					if(impl!=null)
					{
						Class<?> implcl = SReflect.findClass(impl, null, getClassLoader());
						val = implcl.newInstance();
					}
					else
					{
						Class<?> cl = f.getClass();
						if(SReflect.isSupertype(List.class, cl))
						{
							val = new ArrayList();
						}
						else if(SReflect.isSupertype(Set.class, cl))
						{
							val = new HashSet();
						}
						else if(SReflect.isSupertype(Map.class, cl))
						{
							val = new HashMap();
						}
					}
				}
				if(val instanceof List)
				{
					String bname = mbel.getName();
					f.set(target, new ListWrapper((List<?>)val, rulesystem, "factadded."+bname, "factremoved."+bname, "factchanged."+bname));
				}
//				else if(val instanceof Set)
//				{
//					String bname = mbel.getName();
//					f.set(target, new SetWrapper((Set<?>)val, rulesystem, "factadded."+bname, "factremoved."+bname, "factchanged."+bname));
//				}
//				else if(val instanceof Map)
//				{
//					String bname = mbel.getName();
//					f.set(target, new MapWrapper((Set<?>)val, rulesystem, "factadded."+bname, "factremoved."+bname, "factchanged."+bname));
//				}
			}
			catch(RuntimeException e)
			{
				throw e;
			}
			catch(Exception e)
			{
				throw new RuntimeException(e);
			}
		}

		// Observe goal types
		List<MGoal> goals = ((BDIModel)model).getCapability().getGoals();
		for(int i=0; i<goals.size(); i++)
		{
			// todo: explicit bdi creation rule
			rulesystem.observeObject(goals.get(i).getTargetClass(getClassLoader()));
		}
		
		// Observe plan types
		List<MPlan> mplans = ((BDIModel)model).getCapability().getPlans();
		for(int i=0; i<mplans.size(); i++)
		{
			final MPlan mplan = mplans.get(i);
			
			IAction<Void> createplan = new IAction<Void>()
			{
				public IFuture<Void> execute(IEvent event, IRule<Void> rule, Object context)
				{
					int idx = event.getType().indexOf(".");
					String evtype = event.getType().substring(0, idx);
					String belname = event.getType().substring(idx+1);
					RPlan rplan = RPlan.createRPlan(mplan, new ChangeEvent(evtype, belname, event.getContent()), getInternalAccess());
					IConditionalComponentStep<Void> action = new ExecutePlanStepAction(null, rplan);
					getInternalAccess().getExternalAccess().scheduleStep(action);
					return IFuture.DONE;
				}
			};
			
			MTrigger trigger = mplan.getTrigger();
			
			List<String> fas = trigger.getFactAddeds();
			if(fas!=null && fas.size()>0)
			{
				Rule<Void> rule = new Rule<Void>("create_plan_factadded_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
				for(String fa: fas)
				{
					rule.addEvent("factadded."+fa);
				}
				rulesystem.getRulebase().addRule(rule);
			}

			List<String> frs = trigger.getFactRemoveds();
			if(frs!=null && frs.size()>0)
			{
				Rule<Void> rule = new Rule<Void>("create_plan_factremoved_"+mplan.getName(), ICondition.TRUE_CONDITION, createplan);
				for(String fr: frs)
				{
					rule.addEvent("factremoved."+fr);
				}
				rulesystem.getRulebase().addRule(rule);
			}
		}
		
		return ret;
	}
	
	/**
	 *  Can be called on the agent thread only.
	 * 
	 *  Main method to perform agent execution.
	 *  Whenever this method is called, the agent performs
	 *  one of its scheduled actions.
	 *  The platform can provide different execution models for agents
	 *  (e.g. thread based, or synchronous).
	 *  To avoid idle waiting, the return value can be checked.
	 *  The platform guarantees that executeAction() will not be called in parallel. 
	 *  @return True, when there are more actions waiting to be executed. 
	 */
	public boolean executeStep()
	{
		// Evaluate condition before executing step.
		if(rulesystem!=null)
		{
			rulesystem.processAllEvents();
		}
		
		if(steps!=null && steps.size()>0)
		{
			System.out.println("steps: "+steps.size()+" "+((Object[])steps.get(0))[0]);
		}
		return super.executeStep();
	}
	
	/**
	 *  Get the rulesystem.
	 *  @return The rulesystem.
	 */
	public RuleSystem getRuleSystem()
	{
		return rulesystem;
	}
	
	/**
	 *  Get the bdimodel.
	 *  @return the bdimodel.
	 */
	public BDIModel getBDIModel()
	{
		return bdimodel;
	}
	
	/**
	 *  Get the state.
	 *  @return the state.
	 */
	public RCapability getCapability()
	{
		return capa;
	}
}