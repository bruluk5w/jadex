package jadex.bdiv3x.runtime;

import jadex.bdiv3.features.impl.IInternalBDIAgentFeature;
import jadex.bdiv3.model.IBDIModel;
import jadex.bdiv3.model.MBelief;
import jadex.bdiv3.model.MConfigBeliefElement;
import jadex.bdiv3.model.MConfiguration;
import jadex.bdiv3.model.MElement;
import jadex.bdiv3.model.MParameter;
import jadex.bdiv3.runtime.ChangeEvent;
import jadex.bdiv3.runtime.IBeliefListener;
import jadex.bdiv3.runtime.impl.RElement;
import jadex.bdiv3.runtime.wrappers.EventPublisher;
import jadex.bdiv3.runtime.wrappers.ListWrapper;
import jadex.bdiv3x.features.IBDIXAgentFeature;
import jadex.bridge.IInternalAccess;
import jadex.bridge.component.IArgumentsResultsFeature;
import jadex.bridge.modelinfo.UnparsedExpression;
import jadex.commons.IValueFetcher;
import jadex.commons.SReflect;
import jadex.commons.SUtil;
import jadex.javaparser.IMapAccess;
import jadex.javaparser.SJavaParser;
import jadex.javaparser.SimpleValueFetcher;
import jadex.rules.eca.ChangeInfo;
import jadex.rules.eca.Event;
import jadex.rules.eca.RuleSystem;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *  Runtime element for storing beliefs.
 */
public class RBeliefbase extends RElement implements IBeliefbase, IMapAccess
{
	/** The beliefs. */
	protected Map<String, IBelief> beliefs;
	
	/** The belief sets. */
	protected Map<String, IBeliefSet> beliefsets;
	
	/** The local belief names (cached on first access). */
	protected String[]	names;
	
	/** The local belief set names (cached on first access). */
	protected String[]	setnames;
	
	/**
	 *  Create a new beliefbase.
	 */
	public RBeliefbase(IInternalAccess agent)
	{
		super(null, agent);
	}
	
	/**
	 *  
	 */
	public void init()
	{	
		Map<String, Object> args = getAgent().getComponentFeature(IArgumentsResultsFeature.class).getArguments();
		Map<String, MConfigBeliefElement> inibels = new HashMap<String, MConfigBeliefElement>();
		
		String confname = getAgent().getConfiguration();
		if(confname!=null)
		{
			IBDIModel bdimodel = (IBDIModel)getAgent().getModel().getRawModel();
			MConfiguration mconf = bdimodel.getCapability().getConfiguration(confname);
			
			if(mconf!=null)
			{
				// Set initial belief values
				List<MConfigBeliefElement> ibels = mconf.getInitialBeliefs();
				if(ibels!=null)
				{
					for(MConfigBeliefElement ibel: ibels)
					{
						inibels.put(ibel.getName(), ibel);
					}
				}
			}
		}
		
		List<MBelief> mbels = getMCapability().getBeliefs();
		if(mbels!=null)
		{
			for(MBelief mbel: mbels)
			{
				Object	inival	= null;
				boolean	hasinival	= false;
				
				if(MParameter.EvaluationMode.STATIC.equals(mbel.getEvaluationMode()))
//					|| MParameter.EvaluationMode.PUSH.equals(mbel.getEvaluationMode()))
				{
					if(args.containsKey(mbel.getName())) // mbel.isExported() && 
					{	
						inival	= args.get(mbel.getName());
						hasinival	= true;
					}
					else if(inibels.containsKey(mbel.getName()))
					{
						try
						{
							MConfigBeliefElement	inibel	= inibels.get(mbel.getName());
							if(mbel.isMulti(agent.getClassLoader()))
							{
								List<Object>	inivals	= new ArrayList<Object>();
								for(UnparsedExpression upex: inibel.getFacts())
								{
									inivals.add(SJavaParser.parseExpression(upex, getAgent().getModel().getAllImports(), getAgent().getClassLoader()).getValue(getFetcher(getAgent(), inibel)));
								}
								inival	= inivals;
							}
							else if(!inibel.getFacts().isEmpty())
							{
								inival	= SJavaParser.parseExpression(inibel.getFacts().get(0), getAgent().getModel().getAllImports(), getAgent().getClassLoader()).getValue(getFetcher(getAgent(), inibel));								
							}
							hasinival	= true;
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
				}
				
				if(!mbel.isMulti(agent.getClassLoader()))
				{
					if(hasinival)
					{	
						addBelief(new RBelief(mbel, getAgent(), inival));
					}
					else
					{
						addBelief(new RBelief(mbel, getAgent()));
					}
				}
				else
				{
					if(hasinival)
					{
						addBeliefSet(new RBeliefSet(mbel, getAgent(), inival));
					}
					else
					{
						addBeliefSet(new RBeliefSet(mbel, getAgent()));
					}
				}
			}
		}
	}
	
	/**
	 *  Get a belief for a name.
	 *  @param name	The belief name.
	 */
	public IBelief getBelief(String name)
	{
		if(beliefs==null || !beliefs.containsKey(name))
			throw new RuntimeException("Belief not found: "+name);
		return beliefs.get(name);
	}

	/**
	 *  Get a belief set for a name.
	 *  @param name	The belief set name.
	 */
	public IBeliefSet getBeliefSet(String name)
	{
		if(beliefsets==null || !beliefsets.containsKey(name))
			throw new RuntimeException("Beliefset not found: "+name);
		return beliefsets.get(name);
	}

	/**
	 *  Returns <tt>true</tt> if this beliefbase contains a belief with the
	 *  specified name.
	 *  @param name the name of a belief.
	 *  @return <code>true</code> if contained, <code>false</code> is not contained, or
	 *          the specified name refer to a belief set.
	 *  @see #containsBeliefSet(java.lang.String)
	 */
	public boolean containsBelief(String name)
	{
		return beliefs==null? false: beliefs.containsKey(name);
	}

	/**
	 *  Returns <tt>true</tt> if this beliefbase contains a belief set with the
	 *  specified name.
	 *  @param name the name of a belief set.
	 *  @return <code>true</code> if contained, <code>false</code> is not contained, or
	 *          the specified name refer to a belief.
	 *  @see #containsBelief(java.lang.String)
	 */
	public boolean containsBeliefSet(String name)
	{
		return beliefsets==null? false: beliefsets.containsKey(name);
	}

	/**
	 *  Returns the names of all beliefs.
	 *  @return the names of all beliefs.
	 */
	public String[] getBeliefNames()
	{
		if(names==null && beliefs!=null)
		{
			List<String>	lnames	= new ArrayList<String>();
			for(String name: beliefs.keySet())
			{
				if(name.indexOf(MElement.CAPABILITY_SEPARATOR)==-1)
				{
					lnames.add(name);
				}
			}
			names	= lnames.toArray(new String[lnames.size()]);
		}
		
		return names;
	}

	/**
	 *  Returns the names of all belief sets.
	 *  @return the names of all belief sets.
	 */
	public String[] getBeliefSetNames()
	{
		if(setnames==null && beliefsets!=null)
		{
			List<String>	lnames	= new ArrayList<String>();
			for(String name: beliefsets.keySet())
			{
				if(name.indexOf(MElement.CAPABILITY_SEPARATOR)==-1)
				{
					lnames.add(name);
				}
			}
			setnames	= lnames.toArray(new String[lnames.size()]);
		}
		
		return setnames;
	}

	/**
	 *  Add a belief.
	 *  @param bel The belief.
	 */
	public void addBelief(RBelief bel)
	{
		if(beliefs==null)
			beliefs = new HashMap<String, IBelief>();
		beliefs.put(bel.getName(), bel);
	}
	
	/**
	 *  Add a beliefset.
	 *  @param bel The beliefset.
	 */
	public void addBeliefSet(RBeliefSet belset)
	{
		if(beliefsets==null)
			beliefsets = new HashMap<String, IBeliefSet>();
		beliefsets.put(belset.getName(), belset);
	}
	
	/**
	 *  Get an object from the map.
	 *  @param key The key
	 *  @return The value.
	 */
	public Object get(Object key)
	{
		String name = (String)key;
		Object ret = null;
		if(containsBelief(name))
		{
			ret = getBelief(name).getFact();
		}
		else if(containsBeliefSet(name))
		{
			ret = getBeliefSet(name).getFacts();
		}
		else
		{
			throw new RuntimeException("Unknown belief/set: "+name);
		}
		return ret;
	}
	
	/**
	 *  Create a belief with given key and class.
	 *  @param key The key identifying the belief.
	 *  @param clazz The class.
	 *  @deprecated
	 */
//		public void createBelief(String key, Class clazz, int update);

	/**
	 *  Create a belief with given key and class.
	 *  @param key The key identifying the belief.
	 *  @param clazz The class.
	 *  @deprecated
	 */
//		public void createBeliefSet(String key, Class clazz, int update);

	/**
	 *  Delete a belief with given key.
	 *  @param key The key identifying the belief.
	 *  @deprecated
	 */
//		public void deleteBelief(String key);

	/**
	 *  Delete a belief with given key.
	 *  @param key The key identifying the belief.
	 *  @deprecated
	 */
//		public void deleteBeliefSet(String key);

	/**
	 *  Register a new belief.
	 *  @param mbelief The belief model.
	 */
//		public void registerBelief(IMBelief mbelief);

	/**
	 *  Register a new beliefset model.
	 *  @param mbeliefset The beliefset model.
	 */
//		public void registerBeliefSet(IMBeliefSet mbeliefset);

	/**
	 *  Register a new belief reference.
	 *  @param mbeliefref The belief reference model.
	 */
//		public void registerBeliefReference(IMBeliefReference mbeliefref);

	/**
	 *  Register a new beliefset reference model.
	 *  @param mbeliefsetref The beliefset reference model.
	 */
//		public void registerBeliefSetReference(IMBeliefSetReference mbeliefsetref);

	/**
	 *  Deregister a belief model.
	 *  @param mbelief The belief model.
	 */
//		public void deregisterBelief(IMBelief mbelief);

	/**
	 *  Deregister a beliefset model.
	 *  @param mbeliefset The beliefset model.
	 */
//		public void deregisterBeliefSet(IMBeliefSet mbeliefset);

	/**
	 *  Deregister a belief reference model.
	 *  @param mbeliefref The belief reference model.
	 */
//		public void deregisterBeliefReference(IMBeliefReference mbeliefref);

	/**
	 *  Deregister a beliefset reference model.
	 *  @param mbeliefsetref The beliefset reference model.
	 */
//		public void deregisterBeliefSetReference(IMBeliefSetReference mbeliefsetref);

	/**
	 *  static: belief is evaluated once on init, afterwards set manually
	 *  pull: belief is reevaluated on each read access
	 *  push: reevaluates on each event and sets the new value and throws change event
	 *  polling/updaterate: reevaluates in intervals and and sets the new value and throws change event
	 */
	public class RBelief extends RElement implements IBelief
	{
		/** The value. */
		protected Object value;
		
		/** The publisher. */
		protected EventPublisher publisher;

		/**
		 *  Create a new parameter.
		 *  @param modelelement The model element.
		 *  @param name The name.
		 */
		public RBelief(MBelief modelelement, IInternalAccess agent)
		{
			super(modelelement, agent);
			String name = getModelElement().getName();
			this.publisher = new EventPublisher(agent, ChangeEvent.FACTCHANGED+"."+name, (MBelief)getModelElement());
			if(modelelement.getDefaultFact()!=null)
				setFact(SJavaParser.parseExpression(modelelement.getDefaultFact(), agent.getModel().getAllImports(), agent.getClassLoader()).getValue(getFetcher(getAgent(), getModelElement())));
		}
		
		/**
		 *  Create a new parameter.
		 *  @param modelelement The model element.
		 *  @param name The name.
		 */
		public RBelief(MBelief modelelement, IInternalAccess agent, Object value)
		{
			super(modelelement, agent);
			String name = getModelElement().getName();
			this.publisher = new EventPublisher(agent, ChangeEvent.FACTCHANGED+"."+name, (MBelief)getModelElement());
			setFact(value);
		}

		/**
		 *  Get the name.
		 *  @return The name
		 */
		public String getName()
		{
			return getModelElement().getName();
		}
		
		/**
		 *  Set a value of a parameter.
		 *  @param value The new value.
		 */
		public void setFact(Object value)
		{
//			if(getName().equals("myself"))
//				System.out.println("belief set val: "+value);
			Object oldvalue = value;
			this.value = value;
			publisher.entryChanged(oldvalue, value, -1);
//			publisher.unobserveValue(this.value);
//			publisher.getRuleSystem().addEvent(new Event(publisher.getChangeEvent(), new ChangeInfo<Object>(value, this.value, null)));
//			this.value = value;
//			publisher.observeValue(value);
//			publisher.publishToolBeliefEvent();
		}

		/**
		 *  Get the value of a parameter.
		 *  @return The value.
		 */
		public Object getFact()
		{
			Object ret = value;
			// In case of push the last evaluated value is returned
			if(((MBelief)getModelElement()).getDefaultFact()!=null && MParameter.EvaluationMode.PULL.equals(((MBelief)getModelElement()).getEvaluationMode()))
			{
				ret = SJavaParser.parseExpression(((MBelief)getModelElement()).getDefaultFact(), 
					getAgent().getModel().getAllImports(), getAgent().getClassLoader()).getValue(getFetcher(getAgent(), getModelElement()));
			}
			return ret;
		}
		
		/**
		 *  Get the value class.
		 *  @return The valuec class.
		 */
		public Class<?>	getClazz()
		{
			return ((MBelief)getModelElement()).getType(agent.getClassLoader());
		}
		
		/**
		 *  Indicate that the fact of this belief was modified.
		 *  Calling this method causes an internal fact changed
		 *  event that might cause dependent actions.
		 */
		public void modified()
		{
			publisher.entryChanged(value, value, -1);
		}
		
		/**
		 *  Add a belief set listener.
		 *  @param listener The belief set listener.
		 */
		public <T> void addBeliefListener(IBeliefListener<T> listener)
		{
			IBDIXAgentFeature bdif = getAgent().getComponentFeature(IBDIXAgentFeature.class);
			bdif.getBeliefbase().getBelief(getName()).addBeliefListener(listener);
		}
		
		/**
		 *  Remove a belief set listener.
		 *  @param listener The belief set listener.
		 */
		public <T> void removeBeliefListener(IBeliefListener<T> listener)
		{
			IBDIXAgentFeature bdif = getAgent().getComponentFeature(IBDIXAgentFeature.class);
			bdif.getBeliefbase().getBelief(getName()).removeBeliefListener(listener);
		}
	}
	
	/**
	 * 
	 */
	public class RBeliefSet extends RElement implements IBeliefSet
	{
		/** The value. */
		protected ListWrapper<Object> facts;
		
		/**
		 *  Create a new parameter.
		 *  @param modelelement The model element.
		 *  @param name The name.
		 *  @param vals	The values as array, list, iterable...
		 */
		public RBeliefSet(MBelief modelelement, IInternalAccess agent, Object vals)
		{
			super(modelelement, agent);
			
			List<Object>	inifacts	= new ArrayList<Object>();
			String name = modelelement.getName();
			if(vals!=null)
			{
				Iterator<?>	it	= SReflect.getIterator(vals);
				while(it.hasNext())
				{
					inifacts.add(it.next());
				}
			}
			this.facts = new ListWrapper<Object>(inifacts, agent, ChangeEvent.FACTADDED+"."+name, 
				ChangeEvent.FACTREMOVED+"."+name, ChangeEvent.FACTCHANGED+"."+name, getModelElement());
		}
		
		/**
		 *  Create a new parameter.
		 *  @param modelelement The model element.
		 *  @param name The name.
		 */
		public RBeliefSet(MBelief modelelement, IInternalAccess agent)
		{
			super(modelelement, agent);
			
			String name = getModelElement().getName();
			this.facts = new ListWrapper<Object>(evaluateValues(), agent, ChangeEvent.FACTADDED+"."+name, 
				ChangeEvent.FACTREMOVED+"."+name, ChangeEvent.FACTCHANGED+"."+name, getModelElement());
		}
		
		/**
		 *  Evaluate the default values.
		 */
		protected List<Object> evaluateValues()
		{
			MBelief mbel = (MBelief)getModelElement();
			List<Object> tmpfacts = new ArrayList<Object>();
			if(mbel.getDefaultFact()!=null)
			{
				Object tmp = SJavaParser.parseExpression(mbel.getDefaultFact(), agent.getModel().getAllImports(), agent.getClassLoader()).getValue(getFetcher(getAgent(), getModelElement()));
				Iterator<?>	it	= SReflect.getIterator(tmp);
				while(it.hasNext())
				{
					tmpfacts.add(it.next());
				}
			}
			else 
			{
				if(mbel.getDefaultFacts()!=null)
				{
					for(UnparsedExpression uexp: mbel.getDefaultFacts())
					{
						Object fact = SJavaParser.parseExpression(uexp, agent.getModel().getAllImports(), agent.getClassLoader()).getValue(getFetcher(getAgent(), getModelElement()));
						tmpfacts.add(fact);
					}
				}
			}
			return tmpfacts;
		}

		/**
		 *  Get the name.
		 *  @return The name
		 */
		public String getName()
		{
			return getModelElement().getName();
		}
		
		/**
		 *  Add a fact to a belief.
		 *  @param fact The new fact.
		 */
		public void addFact(Object fact)
		{
			internalGetValues().add(fact);
		}

		/**
		 *  Remove a fact to a belief.
		 *  @param fact The new fact.
		 */
		public void removeFact(Object fact)
		{
			internalGetValues().remove(fact);
		}

		/**
		 *  Add facts to a parameter set.
		 */
		public void addFacts(Object[] facts)
		{
			if(facts!=null)
			{
				for(Object fact: facts)
				{
					addFact(fact);
				}
			}
		}

		/**
		 *  Remove all facts from a belief.
		 */
		public void removeFacts()
		{
			internalGetValues().clear();
		}

		/**
		 *  Get a value equal to the given object.
		 *  @param oldval The old value.
		 */
		public Object getFact(Object oldval)
		{
			Object ret = null;
			List<Object> facts = internalGetValues();
			if(facts!=null)
			{
				for(Object fact: facts)
				{
					if(SUtil.equals(fact, oldval))
						ret = fact;
				}
			}
			return ret;
		}

		/**
		 *  Test if a fact is contained in a belief.
		 *  @param fact The fact to test.
		 *  @return True, if fact is contained.
		 */
		public boolean containsFact(Object fact)
		{
			return internalGetValues().contains(fact);
		}

		/**
		 *  Get the facts of a beliefset.
		 *  @return The facts.
		 */
		public Object[]	getFacts()
		{
			Object ret;
			
			List<Object> facts = internalGetValues();
			
			Class<?> type = ((MBelief)getModelElement()).getType(getAgent().getClassLoader());
			int size = facts==null? 0: facts.size();
			ret = type!=null? ret = Array.newInstance(SReflect.getWrappedType(type), size): new Object[size];
			
			if(facts!=null)
			{
				System.arraycopy(facts.toArray(new Object[facts.size()]), 0, ret, 0, facts.size());
			}
			
			return (Object[])ret;
		}

		/**
		 *  Update a fact to a new fact. Searches the old
		 *  value with equals, removes it and stores the new fact.
		 *  @param newfact The new fact.
		 */
//		public void updateFact(Object newfact);

		/**
		 *  Get the number of values currently
		 *  contained in this set.
		 *  @return The values count.
		 */
		public int size()
		{
			return internalGetValues().size();
		}
		
		/**
		 *  Get the value class.
		 *  @return The valuec class.
		 */
		public Class<?>	getClazz()
		{
			return ((MBelief)getModelElement()).getType(agent.getClassLoader());
		}
		
		/**
		 *  Indicate that the fact of this belief was modified.
		 *  Calling this method causes an internal fact changed
		 *  event that might cause dependent actions.
		 */
		public void modified(Object fact)
		{
			if(fact!=null)
			{
				facts.entryChanged(null, fact, facts.indexOf(fact));
			}
			else
			{
				RuleSystem rs = ((IInternalBDIAgentFeature)getAgent().getComponentFeature(IBDIXAgentFeature.class)).getRuleSystem();
				rs.addEvent(new Event(ChangeEvent.BELIEFCHANGED+"."+getName(), new ChangeInfo<Object>(facts, facts, null)));
			}
		}
		
		/**
		 *  Add a belief set listener.
		 *  @param listener The belief set listener.
		 */
		public <T> void addBeliefSetListener(IBeliefListener<T> listener)
		{
			IBDIXAgentFeature bdif = getAgent().getComponentFeature(IBDIXAgentFeature.class);
			bdif.getBeliefbase().getBelief(getName()).addBeliefListener(listener);
		}
		
		/**
		 *  Remove a belief set listener.
		 *  @param listener The belief set listener.
		 */
		public <T> void removeBeliefSetListener(IBeliefListener<T> listener)
		{
			IBDIXAgentFeature bdif = getAgent().getComponentFeature(IBDIXAgentFeature.class);
			bdif.getBeliefbase().getBelief(getName()).removeBeliefListener(listener);
		}
		
		/**
		 * 
		 */
		protected List<Object> internalGetValues()
		{
			// In case of push the last saved/evaluated value is returned
			return MParameter.EvaluationMode.PULL.equals(((MBelief)getModelElement()).getEvaluationMode())? evaluateValues(): facts;
		}
	}
	
	//-------- helper methods --------
	
	/**
	 *  Get the capability-specific fetcher for an element.
	 */
	public static IValueFetcher	getFetcher(final IInternalAccess agent, MElement element)
	{
		return getFetcher(agent, element, null);
	}
	
	/**
	 *  Get the capability-specific fetcher for an element.
	 *  Also creates a new fetcher, if values are given.
	 */
	// Todo: move somewhere else?
	public static IValueFetcher	getFetcher(final IInternalAccess agent, MElement element, Map<String, Object> values)
	{
		IValueFetcher	ret	= agent.getFetcher();
		
		if(element!=null && element.getCapabilityName()!=null)	// Todo: some RElements have no MElement (e.g. expression)
		{
			final RBeliefbase	beliefbase	= ((IInternalBDIAgentFeature)agent.getComponentFeature(IBDIXAgentFeature.class)).getCapability().getBeliefbase();
			final String	prefix	= element.getCapabilityName()+MElement.CAPABILITY_SEPARATOR;
			SimpleValueFetcher	fetcher	= new SimpleValueFetcher(ret);
			fetcher.setValue("$beliefbase", new BeliefbaseWrapper(beliefbase, prefix));
			if(values!=null)
			{
				fetcher.setValues(values);
			}
			ret	= fetcher;
		}
		else if(values!=null && !values.isEmpty())
		{
			SimpleValueFetcher	fetcher	= new SimpleValueFetcher(ret);
			fetcher.setValues(values);
			ret	= fetcher;			
		}
		
		return ret;
	}
	
	/**
	 *  Prepend capability prefix to belief names.
	 */
	public static class BeliefbaseWrapper implements IMapAccess
	{
		//-------- attributes --------
		
		/** The flat belief base. */
		protected RBeliefbase	beliefbase;
		
		/** The full capability prefix. */
		protected String	prefix;
		
		/** The local belief names (cached on first access). */
		protected String[]	names;
		
		/** The local belief set names (cached on first access). */
		protected String[]	setnames;
		
		//-------- constructors --------
		
		/**
		 *  Create a belief base wrapper.
		 */
		public BeliefbaseWrapper(RBeliefbase beliefbase, String prefix)
		{
			this.beliefbase	= beliefbase;
			this.prefix	= prefix;
		}
		
		//-------- IMapAccess methods --------
		
		/**
		 *  Get an object from the map.
		 *  @param name The name
		 *  @return The value.
		 */
		public Object get(Object name)
		{
			return beliefbase.get(prefix+name);
		}
		
		//-------- IBeliefbase methods --------
		
	   /**
		 *  Get a belief for a name.
		 *  @param name	The belief name.
		 */
		public IBelief getBelief(String name)
		{
			return beliefbase.getBelief(prefix+name);
		}

		/**
		 *  Get a belief set for a name.
		 *  @param name	The belief set name.
		 */
		public IBeliefSet getBeliefSet(String name)
		{
			return beliefbase.getBeliefSet(prefix+name);
		}

		/**
		 *  Returns <tt>true</tt> if this beliefbase contains a belief with the
		 *  specified name.
		 *  @param name the name of a belief.
		 *  @return <code>true</code> if contained, <code>false</code> is not contained, or
		 *          the specified name refer to a belief set.
		 *  @see #containsBeliefSet(java.lang.String)
		 */
		public boolean containsBelief(String name)
		{
			return beliefbase.containsBelief(prefix+name);
		}

		/**
		 *  Returns <tt>true</tt> if this beliefbase contains a belief set with the
		 *  specified name.
		 *  @param name the name of a belief set.
		 *  @return <code>true</code> if contained, <code>false</code> is not contained, or
		 *          the specified name refer to a belief.
		 *  @see #containsBelief(java.lang.String)
		 */
		public boolean containsBeliefSet(String name)
		{
			return beliefbase.containsBeliefSet(prefix+name);
		}

		/**
		 *  Returns the names of all beliefs.
		 *  @return the names of all beliefs.
		 */
		public String[] getBeliefNames()
		{
			if(names==null && beliefbase.beliefs!=null)
			{
				List<String>	lnames	= new ArrayList<String>();
				for(String name: beliefbase.beliefs.keySet())
				{
					if(name.startsWith(prefix))
					{
						name	= name.substring(prefix.length());
						if(name.indexOf(MElement.CAPABILITY_SEPARATOR)==-1)
						{
							lnames.add(name);
						}
					}
				}
				names	= lnames.toArray(new String[lnames.size()]);
			}
			
			return names;
		}

		/**
		 *  Returns the names of all belief sets.
		 *  @return the names of all belief sets.
		 */
		public String[] getBeliefSetNames()
		{
			if(setnames==null && beliefbase.beliefsets!=null)
			{
				List<String>	lnames	= new ArrayList<String>();
				for(String name: beliefbase.beliefsets.keySet())
				{
					if(name.startsWith(prefix))
					{
						name	= name.substring(prefix.length());
						if(name.indexOf(MElement.CAPABILITY_SEPARATOR)==-1)
						{
							lnames.add(name);
						}
					}
				}
				setnames	= lnames.toArray(new String[lnames.size()]);
			}
			
			return setnames;
		}
	}
}
