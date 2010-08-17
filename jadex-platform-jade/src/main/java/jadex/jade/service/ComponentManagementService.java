package jadex.jade.service;

import jade.core.AID;
import jade.wrapper.AgentController;
import jadex.base.fipa.CMSComponentDescription;
import jadex.base.fipa.SearchConstraints;
import jadex.bridge.ComponentIdentifier;
import jadex.bridge.CreationInfo;
import jadex.bridge.IComponentDescription;
import jadex.bridge.IComponentFactory;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentInstance;
import jadex.bridge.IComponentListener;
import jadex.bridge.IComponentManagementService;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IModelInfo;
import jadex.bridge.IMessageService;
import jadex.bridge.ISearchConstraints;
import jadex.commons.Future;
import jadex.commons.IFuture;
import jadex.commons.collection.MultiCollection;
import jadex.commons.collection.SCollection;
import jadex.commons.concurrent.IResultListener;
import jadex.jade.JadeAgentAdapter;
import jadex.jade.Platform;
import jadex.jade.SJade;
import jadex.service.IService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 *  Built-in standalone agent platform, with only basic features.
 *  // todo: what about this property change support? where used?
 */
public class ComponentManagementService implements IComponentManagementService, IService
{
	//-------- constants --------

	/** The component counter. Used for generating unique component ids. */
	public static int compcnt = 0;

	//-------- attributes --------

	/** The service container. */
	protected Platform platform;

	/** The components (id->component adapter). */
	protected Map adapters;
	
	/** The component descriptions (id -> component description). */
	protected Map descs;
	
	/** The cleanup commands for the components (component id -> cleanup command). */
	protected Map ccs;
	
	/** The children of a component (component id -> children ids). */
	protected MultiCollection children;
	
	/** The logger. */
	protected Logger logger;

	/** The listeners. */
	protected MultiCollection listeners;
	
	/** The result (kill listeners). */
	protected Map killresultlisteners;
	
	/** The daemon counter. */
	protected int daemons;
	
	/** The autoshutdown flag. */
	protected boolean autoshutdown;
	
    //-------- constructors --------

    /**
     *  Create a new component execution service.#
     *  @param container	The service container.
     */
    public ComponentManagementService(Platform platform, boolean autoshutdown)
	{
		this.platform = platform;
		this.adapters = Collections.synchronizedMap(SCollection.createHashMap());
		this.descs = Collections.synchronizedMap(SCollection.createLinkedHashMap());
		this.ccs = SCollection.createLinkedHashMap();
		this.children	= SCollection.createMultiCollection();
		this.logger = Logger.getLogger(platform.getName()+".cms");
		this.listeners = SCollection.createMultiCollection();
		this.killresultlisteners = Collections.synchronizedMap(SCollection.createHashMap());
		this.autoshutdown = autoshutdown;
	}
    
    //-------- IComponentManagementService interface --------

	/**
	 *  Create a new component on the platform.
	 *  @param name The component name.
	 *  @param model The model identifier (e.g. file name).
	 *  @param listener The result listener (if any). Will receive the id of the component as result, when the component has been created.
	 *  @param killlistener The kill listener (if any). Will receive the results of the component execution, after the component has terminated.
	 */
	public IFuture createComponent(String name, String model, CreationInfo cinfo, final IResultListener killlistener)
	{
		final Future ret = new Future();
		final CreationInfo info	= cinfo!=null ? cinfo : new CreationInfo();	// Dummy default info, if null.
		IComponentIdentifier aid = null;
		
		// Load the model with fitting factory.
		
		IComponentFactory factory = null;
		String	type	= null;
		Collection facts = platform.getServices(IComponentFactory.class);
		if(facts!=null)
		{
			for(Iterator it=facts.iterator(); factory==null && it.hasNext(); )
			{
				IComponentFactory	cf	= (IComponentFactory)it.next();
				if(cf.isLoadable(model, info.getImports()))
				{
					factory	= cf;
					type	= factory.getComponentType(model, info.getImports());
				}
			}
		}
		if(factory==null)
			throw new RuntimeException("No factory found for component: "+model);
		final IModelInfo lmodel = factory.loadModel(model, info.getImports());
		
		if(name!=null && name.indexOf('@')!=-1)
		{
			ret.setException( new RuntimeException("No '@' allowed in agent name."));
			//throw new RuntimeException("No '@' allowed in agent name.");
		}
		else
		{
			
			if(name==null)
			{
	//				name = generateAgentName(getShortName(model));
				name = generateComponentIdentifier(lmodel.getName()).getLocalName();
			}
	
			List argus = new ArrayList();
			argus.add(lmodel);
			argus.add(info.getConfiguration());//==null? "default": config);
			if(info.getArguments()!=null)
				argus.add(info.getArguments());	// Jadex argument map is supplied as 3rd index in JADE argument array.  
					
			try
			{
				AgentController ac = platform.getPlatformController().createNewAgent(name, "jadex.jade.JadeAgentAdapter", argus.toArray());
				// Hack!!! Bug in JADE not returning created agent's AID.
				// Should do ams_search do get correct AID?
				AID tmp = (AID)platform.getPlatformAgent().clone();
				int idx = tmp.getName().indexOf("@");
				tmp.setName(name + tmp.getName().substring(idx));
				aid = SJade.convertAIDtoFipa(tmp, (IComponentManagementService)platform.getService(IComponentManagementService.class));
				
				// Must use start to call setup and make the agnet
				ac.start();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				ret.setException(e);
				return ret;
			}
			
			final CMSComponentDescription ad = new CMSComponentDescription(aid, type, info.getParent(), info.isMaster(), info.isDaemon());
			
			// Increase daemon cnt
			if(info.isDaemon())
				daemons++;
			
	//		System.out.println("added: "+agentdescs.size()+", "+aid);
			
			IComponentDescription padesc = info.getParent()!=null? (IComponentDescription)descs.get(info.getParent()): null;
			// Suspend when set to suspend or when parent is also suspended or when specified in model.
			Object debugging  = lmodel.getProperties().get("debugging");
			if(info.isSuspend() || (padesc!=null && IComponentDescription.STATE_SUSPENDED.equals(padesc.getState()))
				|| debugging instanceof Boolean && ((Boolean)debugging).booleanValue())
			{
				ad.setState(IComponentDescription.STATE_SUSPENDED);
			}
			else
			{
				ad.setState(IComponentDescription.STATE_ACTIVE);
			}
			descs.put(aid, ad);
			if(info.getParent()!=null)
			{
				children.put(info.getParent(), aid);
			}
			
			// Hack! Busy waiting for platform agent init finished.
			while(!adapters.containsKey(aid))
			{
				System.out.print(".");
				try
				{
					Thread.sleep(100);
				}
				catch(Exception e)
				{
				}
			}
			final JadeAgentAdapter adapter = (JadeAgentAdapter)adapters.get(aid);
			adapter.setComponentDescription(ad);
	
			JadeAgentAdapter pad = null;
			if(info.getParent()!=null)
			{
				pad	= (JadeAgentAdapter)adapters.get(info.getParent());
			}
	
			if(pad!=null)
			{
				final IComponentFactory	cf = factory;
				final JadeAgentAdapter fpad = pad;
				final ComponentIdentifier faid = (ComponentIdentifier)aid;
				pad.getComponentInstance().getExternalAccess(new IResultListener()
				{
					public void resultAvailable(Object source, Object result)
					{
						createComponentInstance(info.getConfiguration(), info.getArguments(), info.isSuspend(), ret,
							killlistener, cf, lmodel, faid, adapter, fpad, ad, (IExternalAccess)result);
					}
					
					public void exceptionOccurred(Object source, Exception exception)
					{
						ret.setException(exception);
					}
				});
			}
			else
			{
				createComponentInstance(info.getConfiguration(), info.getArguments(), info.isSuspend(), ret,
					killlistener, factory, lmodel, (ComponentIdentifier)aid, adapter, null, ad, null);
			}
		}
		
		return ret;
	}	
		
	/**
	 *  Create an instance of a component (step 2 of creation process).
	 */
	protected void createComponentInstance(String config, Map args,
		boolean suspend, Future ret,
		final IResultListener resultlistener, IComponentFactory factory,
		IModelInfo lmodel, final ComponentIdentifier cid,
		JadeAgentAdapter adapter, JadeAgentAdapter pad,
		CMSComponentDescription ad, IExternalAccess parent)
	{
		// Create the component instance.
		IComponentInstance instance = factory.createComponentInstance(adapter, lmodel, config, args, parent);
		adapter.setComponent(instance, lmodel);
		
//			System.out.println("added: "+descs.size()+", "+aid);
		
		// Register component at parent.
		if(pad!=null)
		{
			pad.getComponentInstance().componentCreated(ad, lmodel);
		}

		IComponentListener[]	alisteners;
		synchronized(listeners)
		{
			Set	slisteners	= new HashSet(listeners.getCollection(null));
			slisteners.addAll(listeners.getCollection(cid));
			alisteners	= (IComponentListener[])slisteners.toArray(new IComponentListener[slisteners.size()]);
		}
		// todo: can be called after listener has (concurrently) deregistered
		for(int i=0; i<alisteners.length; i++)
		{
			alisteners[i].componentAdded(ad);
		}
		
		if(resultlistener!=null)
			killresultlisteners.put(cid, resultlistener);
		
		ret.setResult(cid.clone());
		
		if(!suspend)
			adapter.start();
	}
	
	/**
	 *  Destroy (forcefully terminate) an component on the platform.
	 *  @param componentid	The component to destroy.
	 */
	public IFuture destroyComponent(IComponentIdentifier cid)
	{
		final Future ret = new Future();
		
		CMSComponentDescription	desc;
		synchronized(adapters)
		{
			synchronized(descs)
			{
				// Kill subcomponents
				Object[]	achildren	= children.getCollection(cid).toArray();	// Use copy as children may change on destroy.
				for(int i=0; i<achildren.length; i++)
				{
					destroyComponent((IComponentIdentifier)achildren[i]);	// todo: cascading delete with wait.
				}
				
//					System.out.println("killing: "+cid);
				
				JadeAgentAdapter component = (JadeAgentAdapter)adapters.get(cid);
				if(component==null)
				{
					ret.setException(new RuntimeException("Component "+cid+" does not exist."));
					return ret;

					//System.out.println(componentdescs);
					//throw new RuntimeException("Component "+aid+" does not exist.");
				}
				
				// todo: does not work always!!! A search could be issued before components had enough time to kill itself!
				// todo: killcomponent should only be called once for each component?
				desc = (CMSComponentDescription)descs.get(cid);
				if(desc!=null)
				{
					if(!ccs.containsKey(cid))
					{
						CleanupCommand	cc	= new CleanupCommand(cid);
						ccs.put(cid, cc);
						cc.addKillFuture(ret);
						component.killComponent(cc);						
					}
					else
					{
						CleanupCommand	cc	= (CleanupCommand)ccs.get(cid);
						if(cc==null)
							ret.setException(new RuntimeException("No cleanup command for component "+cid+": "+desc.getState()));
						cc.addKillFuture(ret);
					}
				}
			}
		}
		
		return ret;
	}

	/**
	 *  Suspend the execution of an component.
	 *  @param componentid The component identifier.
	 */
	public IFuture suspendComponent(IComponentIdentifier componentid)
	{
		final Future ret = new Future();
		
		CMSComponentDescription ad;
		synchronized(adapters)
		{
			synchronized(descs)
			{
				// Suspend subcomponents
				for(Iterator it=children.getCollection(componentid).iterator(); it.hasNext(); )
				{
					IComponentIdentifier	child	= (IComponentIdentifier)it.next();
					if(IComponentDescription.STATE_ACTIVE.equals(((IComponentDescription)descs.get(child)).getState()))
					{
						suspendComponent(child);	// todo: cascading resume with wait.
					}
				}

				JadeAgentAdapter adapter = (JadeAgentAdapter)adapters.get(componentid);
				ad = (CMSComponentDescription)descs.get(componentid);
				if(adapter==null || ad==null)
				{
					ret.setException(new RuntimeException("Component identifier not registered: "+componentid));
					return ret;
				}
					//throw new RuntimeException("Component Identifier not registered in CES: "+aid);
				if(!IComponentDescription.STATE_ACTIVE.equals(ad.getState())
					/*&& !IComponentDescription.STATE_TERMINATING.equals(ad.getState())*/)
				{
					ret.setException(new RuntimeException("Only active components can be suspended: "+componentid+" "+ad.getState()));
					return ret;
					//throw new RuntimeException("Only active components can be suspended: "+aid+" "+ad.getState());
				}
				
				ad.setState(IComponentDescription.STATE_SUSPENDED);
				
				try
				{
					AgentController ac = platform.getPlatformController().getAgent(((IComponentIdentifier)componentid).getLocalName());
					ac.suspend();
					ret.setResult(null);
				}
				catch(Exception e)
				{
					ret.setException(e);
				}
			}
		}
		
		IComponentListener[]	alisteners;
		synchronized(listeners)
		{
			Set	slisteners	= new HashSet(listeners.getCollection(null));
			slisteners.addAll(listeners.getCollection(componentid));
			alisteners	= (IComponentListener[])slisteners.toArray(new IComponentListener[slisteners.size()]);
		}
		// todo: can be called after listener has (concurrently) deregistered
		for(int i=0; i<alisteners.length; i++)
		{
			alisteners[i].componentChanged(ad);
		}
		
		return ret;
	}
	
	/**
	 *  Resume the execution of an component.
	 *  @param componentid The component identifier.
	 */
	public IFuture resumeComponent(IComponentIdentifier componentid)
	{
		final Future ret = new Future();
		
		CMSComponentDescription ad;
		
		synchronized(adapters)
		{
			synchronized(descs)
			{
				// Resume subcomponents
				for(Iterator it=children.getCollection(componentid).iterator(); it.hasNext(); )
				{
					IComponentIdentifier	child	= (IComponentIdentifier)it.next();
					if(IComponentDescription.STATE_SUSPENDED.equals(((IComponentDescription)descs.get(child)).getState())
						|| IComponentDescription.STATE_WAITING.equals(((IComponentDescription)descs.get(child)).getState()))
					{
						resumeComponent(child);	// todo: cascading resume with wait.
					}
				}

				JadeAgentAdapter adapter = (JadeAgentAdapter)adapters.get(componentid);
				ad = (CMSComponentDescription)descs.get(componentid);
				if(adapter==null || ad==null)
				{
					ret.setException(new RuntimeException("Component identifier not registered: "+componentid));
					return ret;
					//throw new RuntimeException("Component Identifier not registered in CES: "+aid);
				}
				if(!IComponentDescription.STATE_SUSPENDED.equals(ad.getState())
					&& !IComponentDescription.STATE_WAITING.equals(ad.getState()))
				{
					ret.setException(new RuntimeException("Only suspended/waiting components can be resumed: "+componentid+" "+ad.getState()));
					return ret;
					//throw new RuntimeException("Only suspended components can be resumed: "+aid+" "+ad.getState());
				}
				
				ad.setState(IComponentDescription.STATE_ACTIVE);
				
				try
				{
					AgentController ac = platform.getPlatformController().getAgent(((IComponentIdentifier)componentid).getLocalName());
					ac.activate();
					ret.setResult(null);
				}
				catch(Exception e)
				{
					ret.setException(e);
				}
			}
		}
		
		IComponentListener[]	alisteners;
		synchronized(listeners)
		{
			Set	slisteners	= new HashSet(listeners.getCollection(null));
			slisteners.addAll(listeners.getCollection(componentid));
			alisteners	= (IComponentListener[])slisteners.toArray(new IComponentListener[slisteners.size()]);
		}
		// todo: can be called after listener has (concurrently) deregistered
		for(int i=0; i<alisteners.length; i++)
		{
			alisteners[i].componentChanged(ad);
		}
	
		return ret;
	}
	
	/**
	 *  Execute a step of a suspended component.
	 *  @param componentid The component identifier.
	 *  @param listener Called when the step is finished (result will be the component description).
	 */
	public IFuture stepComponent(IComponentIdentifier componentid)
	{
		final Future ret = new Future();
		
		synchronized(adapters)
		{
			synchronized(descs)
			{
				JadeAgentAdapter adapter = (JadeAgentAdapter)adapters.get(componentid);
				IComponentDescription cd = (IComponentDescription)descs.get(componentid);
				if(adapter==null || cd==null)
				{
					ret.setException(new RuntimeException("Component identifier not registered: "+componentid));
					//throw new RuntimeException("Component Identifier not registered in CES: "+aid);
				}
				if(!IComponentDescription.STATE_SUSPENDED.equals(cd.getState()))
				{
					ret.setException(new RuntimeException("Only suspended components can be stepped: "+componentid+" "+cd.getState()));
					//throw new RuntimeException("Only suspended components can be resumed: "+aid+" "+ad.getState());
				}
				
				adapter.doStep(new IResultListener()
				{
					public void resultAvailable(Object source, Object result)
					{
						ret.setResult(result);
					}
					
					public void exceptionOccurred(Object source, Exception exception)
					{
						ret.setException(exception);
					}
				});
				resumeComponent(componentid);
			}
		}
		
		return ret;
	}

	/**
	 *  Set breakpoints for a component.
	 *  Replaces existing breakpoints.
	 *  To add/remove breakpoints, use current breakpoints from component description as a base.
	 *  @param componentid The component identifier.
	 *  @param breakpoints The new breakpoints (if any).
	 */
	public void setComponentBreakpoints(IComponentIdentifier componentid, String[] breakpoints)
	{
		CMSComponentDescription ad;
		synchronized(descs)
		{
			ad = (CMSComponentDescription)descs.get(componentid);
			ad.setBreakpoints(breakpoints);
		}
		
		IComponentListener[]	alisteners;
		synchronized(listeners)
		{
			Set	slisteners	= new HashSet(listeners.getCollection(null));
			slisteners.addAll(listeners.getCollection(componentid));
			alisteners	= (IComponentListener[])slisteners.toArray(new IComponentListener[slisteners.size()]);
		}
		// todo: can be called after listener has (concurrently) deregistered
		for(int i=0; i<alisteners.length; i++)
		{
			alisteners[i].componentChanged(ad);
		}
	}

	//-------- listener methods --------
	
	/**
     *  Add an component listener.
     *  The listener is registered for component changes.
     *  @param comp  The component to be listened on (or null for listening on all components).
     *  @param listener  The listener to be added.
     */
    public void addComponentListener(IComponentIdentifier comp, IComponentListener listener)
    {
		synchronized(listeners)
		{
			listeners.put(comp, listener);
		}
    }
    
    /**
     *  Remove a listener.
     *  @param comp  The component to be listened on (or null for listening on all components).
     *  @param listener  The listener to be removed.
     */
    public void removeComponentListener(IComponentIdentifier comp, IComponentListener listener)
    {
		synchronized(listeners)
		{
			listeners.remove(comp, listener);
		}
    }
    
    /**
     *  Get the adapter map. 
     *  @return The adapter map.
     */
    public Map getComponentAdapterMap()
    {
    	return adapters;
    }
    
    //-------- helper classes --------

	/**
	 *  Command that is executed on component cleanup.
	 */
	class CleanupCommand implements IResultListener
	{
		protected IComponentIdentifier cid;
		protected List killfutures;
		
		public CleanupCommand(IComponentIdentifier cid)
		{
//			System.out.println("CleanupCommand created");
			this.cid = cid;
		}
		
		public void resultAvailable(Object source, Object result)
		{
//			System.out.println("CleanupCommand: "+result);
			
			IComponentDescription ad = (IComponentDescription)descs.get(cid);
			Map results = null;
			JadeAgentAdapter adapter;
			JadeAgentAdapter pad = null;
			CMSComponentDescription desc;
			boolean shutdown = false;
			synchronized(adapters)
			{
				synchronized(descs)
				{
//					System.out.println("CleanupCommand remove called for: "+cid);
					adapter = (JadeAgentAdapter)adapters.remove(cid);
					if(adapter==null)
						throw new RuntimeException("Component Identifier not registered: "+cid);
					
					results = adapter.getComponentInstance().getResults();
					
					desc = (CMSComponentDescription)descs.remove(cid);
					desc.setState(IComponentDescription.STATE_TERMINATED);
					if(desc.isDaemon())
						daemons--;
					if(adapters.size()-daemons==0)
						shutdown = true;
			
					ccs.remove(cid);
					
					// Stop execution of component.
//					((IExecutionService)platform.getService(IExecutionService.class)).cancel(adapter, null);
					adapter.cleanupAgent();
					
					// Deregister destroyed component at parent.
					if(desc.getParent()!=null)
					{
						children.remove(desc.getParent(), desc.getName());
						pad	= (JadeAgentAdapter)adapters.get(desc.getParent());
					}
				}
			}
			
			// Must be executed out of sync block due to deadlocks
			// agent->cleanupcommand->space.componentRemoved (holds adapter mon -> needs space mone)
			// space executor->general loop->distributed percepts->(holds space mon -> needs adapter mon for getting external access)
			if(pad!=null)
			{
				pad.getComponentInstance().componentDestroyed(desc);
			}
			// else parent has just been killed.
			
//				// Deregister killed component at contexts.
//				IContextService	cs	= (IContextService)container.getService(IContextService.class);
//				if(cs!=null)
//				{
//					IContext[]	contexts	= cs.getContexts(cid);
//					for(int i=0; contexts!=null && i<contexts.length; i++)
//					{
//						((BaseContext)contexts[i]).componentDestroyed(cid);
//					}
//				}

			IComponentListener[] alisteners;
			synchronized(listeners)
			{
				Set	slisteners	= new HashSet(listeners.getCollection(null));
				slisteners.addAll(listeners.getCollection(cid));
				alisteners	= (IComponentListener[])slisteners.toArray(new IComponentListener[slisteners.size()]);
			}
			
			// todo: can be called after listener has (concurrently) deregistered
			for(int i=0; i<alisteners.length; i++)
			{
				try
				{
					alisteners[i].componentRemoved(ad, results);
				}
				catch(Exception e)
				{
					System.out.println("WARNING: Exception when removing component: "+ad+", "+e);
				}
			}
			
			IResultListener reslis = (IResultListener)killresultlisteners.remove(cid);
			if(reslis!=null)
			{
//					System.out.println("result: "+cid+" "+results);
				reslis.resultAvailable(cid, results);
			}
			
//				System.out.println("CleanupCommand end.");
			
			if(killfutures!=null)
			{
				for(int i=0; i<killfutures.size(); i++)
				{
					((Future)killfutures.get(i)).setResult(result);
				}
			}
			
			// Shudown platform when last (non-daemon) component was destroyed
			if(shutdown)
				platform.shutdown(null);
		}
		
		public void exceptionOccurred(Object source, Exception exception)
		{
			resultAvailable(source, cid);
		}
		
		/**
		 *  Add a listener to be informed, when the component has terminated.
		 * @param listener
		 */
		public void	addKillFuture(Future killfuture)
		{
			if(killfutures==null)
				killfutures = new ArrayList();
			killfutures.add(killfuture);
		}
	}
	
	//-------- internal methods --------
    
	/**
	 *  Get the component adapter for a component identifier.
	 *  @param aid The component identifier.
	 *  @param listener The result listener.
	 */
    // Todo: Hack!!! remove?
	public void getComponentAdapter(IComponentIdentifier cid, IResultListener listener)
	{
		if(listener==null)
			throw new RuntimeException("Result listener required.");
		
		listener.resultAvailable(this, adapters.get(cid));
	}
	
	/**
	 *  Get the external access of a component.
	 *  @param cid The component identifier.
	 *  @param listener The result listener (recieves an IExternalAccess object).
	 */
	public IFuture getExternalAccess(IComponentIdentifier cid)
	{
		final Future ret = new Future();	
		JadeAgentAdapter adapter = (JadeAgentAdapter)adapters.get(cid);
		if(adapter==null)
			ret.setException(new RuntimeException("No local component found for component identifier: "+cid));
		else
			adapter.getComponentInstance().getExternalAccess(new IResultListener()
			{
				public void resultAvailable(Object source, Object result)
				{
					ret.setResult(result);
				}
				
				public void exceptionOccurred(Object source, Exception exception)
				{
					ret.setException(exception);
				}
			});
		
		return ret;
	}
	
	/**
	 *  Get the parent component of a component.
	 *  @param cid The component identifier.
	 *  @return The parent component identifier.
	 */
	public IComponentIdentifier getParent(IComponentIdentifier cid)
	{
		CMSComponentDescription desc = (CMSComponentDescription)descs.get(cid);
		return desc!=null? desc.getParent(): null;
	}
	
	/**
	 *  Get the children components of a component.
	 *  @param cid The component identifier.
	 *  @return The children component identifiers.
	 */
	public IComponentIdentifier[] getChildren(IComponentIdentifier cid)
	{
		List ret = (List)children.get(cid);
		return ret!=null? (IComponentIdentifier[])ret.toArray(new IComponentIdentifier[0]): new IComponentIdentifier[0];
			
//		CMSComponentDescription desc = (CMSComponentDescription)descs.get(cid);
//		return desc!=null? desc.getChildren(): null;
	}

	/**
	 *  Create component identifier.
	 *  @param name The name.
	 *  @param local True for local name.
	 *  @param addresses The addresses.
	 *  @return The new component identifier.
	 */
	public IComponentIdentifier createComponentIdentifier(String name)
	{
		return createComponentIdentifier(name, true, null);
	}
	
	/**
	 *  Create component identifier.
	 *  @param name The name.
	 *  @param local True for local name.
	 *  @param addresses The addresses.
	 *  @return The new component identifier.
	 */
	public IComponentIdentifier createComponentIdentifier(String name, boolean local)
	{
		return createComponentIdentifier(name, local, null);
	}
	
	/**
	 *  Create component identifier.
	 *  @param name The name.
	 *  @param local True for local name.
	 *  @param addresses The addresses.
	 *  @return The new component identifier.
	 */
	public IComponentIdentifier createComponentIdentifier(String name, boolean local, String[] addresses)
	{
		if(local)
			name = name + "@" + platform.getName();
		return new ComponentIdentifier(name, addresses, null);		
	}

	/**
	 *  Create a search constraints object.
	 *  @param maxresults The maximum number of results.
	 *  @param maxdepth The maximal search depth.
	 *  @return The search constraints.
	 */
	public ISearchConstraints createSearchConstraints(int maxresults, int maxdepth)
	{
		SearchConstraints	ret	= new SearchConstraints();
		ret.setMaxResults(maxresults);
		ret.setMaxDepth(maxdepth);
		return ret;
	}
	
	/**
	 * Create a component description.
	 * @param id The component identifier.
	 * @param state The state.
	 * @param ownership The ownership.
	 * @param type The component type.
	 * @param parent The parent.
	 * @return The component description.
	 */
	public IComponentDescription createComponentDescription(IComponentIdentifier id, String state, String ownership, String type, IComponentIdentifier parent)
	{
		CMSComponentDescription	ret	= new CMSComponentDescription(id, type, parent, false, false);
		ret.setState(state);
		ret.setOwnership(ownership);
		return ret;
	}
	
	//--------- information methods --------
	
	/**
	 *  Get the component description of a single component.
	 *  @param cid The component identifier.
	 *  @return The component description of this component.
	 */
	public IFuture getComponentDescription(IComponentIdentifier cid)
	{
		Future ret = new Future();		
		ret.setResult(descs.get(cid));	// Todo: synchronize?
//				if(ret!=null)
//				{
//					// Todo: addresses required for communication across platforms.
////					ret.setName(refreshComponentIdentifier(aid));
//					ret	= (IComponentDescription)ret.clone();
//				}
		return ret;
	}
	
	/**
	 *  Get all component descriptions.
	 *  @return The component descriptions of this component.
	 */
	public IFuture getComponentDescriptions()
	{
		Future ret = new Future();
		ret.setResult(descs.values().toArray(new IComponentDescription[0]));	// Todo: synchronize?
		return ret;
	}
	
	/**
	 *  Get the component identifiers.
	 *  @return The component identifiers.
	 *  
	 *  This method should be used with caution when the agent population is large.
	 */
	public IFuture getComponentIdentifiers()
	{
		Future fut = new Future();
		
		IComponentIdentifier[] ret;
		
		synchronized(adapters)
		{
			ret = (IComponentIdentifier[])adapters.keySet().toArray(new IComponentIdentifier[adapters.size()]);
			// Todo: addresses required for inter-platform comm.
//				for(int i=0; i<ret.length; i++)
//					ret[i] = refreshComponentIdentifier(ret[i]); // Hack!
		}
		
		fut.setResult(ret);
		return fut;
	}
	
	/**
	 * Search for components matching the given description.
	 * @return An array of matching component descriptions.
	 */
	public IFuture searchComponents(IComponentDescription adesc, ISearchConstraints con)
	{
		Future ret = new Future();
		
//			System.out.println("search: "+components);
		CMSComponentDescription[] cret;

		// If name is supplied, just lookup description.
		if(adesc!=null && adesc.getName()!=null)
		{
			CMSComponentDescription ad = (CMSComponentDescription)descs.get(adesc.getName());
			if(ad!=null && ad.getName().equals(adesc.getName()))
			{
				// Todo: addresses reuqired for interplatform comm.
//					ad.setName(refreshComponentIdentifier(ad.getName()));
				CMSComponentDescription	desc	= (CMSComponentDescription)ad.clone();
				cret = new CMSComponentDescription[]{desc};
			}
			else
			{
				cret	= new CMSComponentDescription[0];
			}
		}

		// Otherwise search for matching descriptions.
		else
		{
			List	tmp	= new ArrayList();
			synchronized(descs)
			{
				for(Iterator it=descs.values().iterator(); it.hasNext(); )
				{
					CMSComponentDescription	test	= (CMSComponentDescription)it.next();
					if(adesc==null ||
						(adesc.getOwnership()==null || adesc.getOwnership().equals(test.getOwnership()))
						&& (adesc.getParent()==null || adesc.getParent().equals(test.getParent()))
						&& (adesc.getType()==null || adesc.getType().equals(test.getType()))
						&& (adesc.getState()==null || adesc.getState().equals(test.getState())))					
					{
						tmp.add(test);
					}
				}
			}
			cret	= (CMSComponentDescription[])tmp.toArray(new CMSComponentDescription[tmp.size()]);
		}

		ret.setResult(cret);
		return ret;
	}
	
	/**
	 *  Create a component identifier that is allowed on the platform.
	 *  @param name The base name.
	 *  @return The component identifier.
	 */
	public IComponentIdentifier generateComponentIdentifier(String name)
	{
		ComponentIdentifier ret = null;

		synchronized(adapters)
		{
			do
			{
				ret = new ComponentIdentifier(name+(compcnt++)+"@"+platform.getName()); // Hack?!
			}
			while(adapters.containsKey(ret));
		}
		
		IMessageService	ms	= (IMessageService)platform.getService(IMessageService.class);
		if(ms!=null)
			ret.setAddresses(ms.getAddresses());

		return ret;
	}
	
	/**
	 *  Set the state of a component (i.e. update the component description).
	 *  Currently only switching between suspended/waiting is allowed.
	 */
	// hack???
	public void	setComponentState(IComponentIdentifier comp, String state)
	{
		assert IComponentDescription.STATE_SUSPENDED.equals(state)
			|| IComponentDescription.STATE_WAITING.equals(state) : "wrong state: "+comp+", "+state;
		
		CMSComponentDescription	desc	= null;
		synchronized(descs)
		{
			desc	= (CMSComponentDescription)descs.get(comp);
			desc.setState(state);			
		}
		
		IComponentListener[]	alisteners;
		synchronized(listeners)
		{
			Set	slisteners	= new HashSet(listeners.getCollection(null));
			slisteners.addAll(listeners.getCollection(comp));
			alisteners	= (IComponentListener[])slisteners.toArray(new IComponentListener[slisteners.size()]);
		}
		// todo: can be called after listener has (concurrently) deregistered
		for(int i=0; i<alisteners.length; i++)
		{
			try
			{
				alisteners[i].componentChanged(desc);
			}
			catch(Exception e)
			{
				System.out.println("WARNING: Exception when changing component state: "+desc+", "+e);
			}
		}
	}
	
	//-------- IService interface --------
	
	/**
	 *  Start the service.
	 */
	public IFuture startService()
	{
		return new Future(null);
	}

	/**
	 *  Shutdown the service.
	 *  @param listener The listener.
	 */
	public IFuture shutdownService()
	{
		return new Future(null);
	}
}

	
	
//	//-------- constants --------
//
//	/** The agent counter. */
//	public static int agentcnt = 0;
//
//	//-------- attributes --------
//
//	/** The agent platform. */
//	protected Platform platform;
//
//	/** The agents (aid->adapter agent). */
//	protected Map adapters;
//	
//	/** The ams agent descriptions (aid -> ams agent description). */
////	protected Map agentdescs;
//	
//	/** The logger. */
//	protected Logger logger;
//
//	/** The ams listeners. */
//	protected List listeners;
//	
//    //-------- constructors --------
//
//    /**
//     *  Create a new AMS.
//     */
//    public AMS(Platform platform)
//    {
//		this.platform = platform;
//		this.adapters = Collections.synchronizedMap(SCollection.createHashMap());
////		this.agentdescs = Collections.synchronizedMap(SCollection.createHashMap());
//		this.logger = Logger.getLogger("JADE_Platform.ams");
//		this.listeners = SCollection.createArrayList();
//    }
//
//    //-------- IAMS interface methods --------
//    
//    /**
//	 *  Test if the execution service can handle the element (or model).
//	 *  @param element The element (or its filename).
//	 */
//	public boolean isResponsible(Object element)
//	{
//		boolean ret = element instanceof IComponentIdentifier;
//		
//		if(!ret && element instanceof String)
//		{
//			ret = IComponentFactory.ELEMENT_TYPE_AGENT.equals(
//				SComponentFactory.getElementType(platform, (String)element));
//		}
//		
//		return ret;
//	}
//    
//	/**
//	 *  Create a new agent on the platform.
//	 *  Ensures (in non error case) that the aid of
//	 *  the new agent is added to the AMS when call returns.
//	 *  @param name The agent name (null for auto creation)
//	 *  @param model The model name.
//	 *  @param confi The configuration.
//	 *  @param args The arguments map (name->value).
//	 */
//	public void	createElement(String name, String model, String config, Map args, IResultListener lis, Object creator)
//	{
////		System.out.println("Create agent: "+name);
//		final IResultListener listener = lis!=null? lis: DefaultResultListener.getInstance();
//		IComponentIdentifier aid = null;
//		AMSAgentDescription ad = null;
//		
//		if(name!=null && name.indexOf('@')!=-1)
//		{
//			listener.exceptionOccurred(new RuntimeException("No '@' allowed in agent name."));
//			return;
//			//throw new RuntimeException("No '@' allowed in agent name.");
//		}
//		
////		if(platform.isShuttingDown())
////		{
////			listener.exceptionOccurred(new RuntimeException("No new agents may be created when platform is shutting down."));
////			return;
////			//throw new RuntimeException("No new agents may be created when platform is shutting down.");
////		}
//
//		if(name==null)
//		{
//			name = generateAgentName(getShortName(model));
//		}
//
//		List argus = new ArrayList();
//		argus.add(model);
//		argus.add(config);//==null? "default": config);
//		if(args!=null)
//			argus.add(args);	// Jadex argument map is supplied as 3rd index in JADE argument array.  
//				
////		Event e = new Event(-1, new SimpleBehaviour()
////		{
////			String convid = null;
////			boolean done = false;
////			
////			public void action()
////			{
////				System.out.println("Create agent code started.");
////
////				if(convid!=null)
////				{
////					ACLMessage reply = myAgent.receive();
////					if(reply==null)
////					{
////						block();
////					}
////					else
////					{
////						System.out.println("Reply received: "+reply);
////						if(reply.getPerformative()==ACLMessage.INFORM)
////						{
////							IAMSListener[]	alisteners;
////							synchronized(listeners)
////							{
////								alisteners	= (IAMSListener[])listeners.toArray(new IAMSListener[listeners.size()]);
////							}
////							// todo: can be called after listener has (concurrently) deregistered
////							for(int i=0; i<alisteners.length; i++)
////							{
////								alisteners[i].agentAdded(null);
////							}
////							
////							// Hack!!! Bug in JADE not returning created agent's AID.
////							// Should do ams_search do get correct AID?
////							AID aid = (AID)myAgent.getAMS().clone();
////							int idx = aid.getName().indexOf("@");
////							aid.setName(name + aid.getName().substring(idx));
////							IComponentIdentifier ret = SJade.convertAIDtoFipa(aid, (IAMS)platform.getService(IAMS.class));
////							
////							listener.resultAvailable(ret);
////						}
////						else
////						{
////							listener.exceptionOccurred(new AgentCreationException(reply.getContent(), null));
////						}
////						done = true;
////					}
////				}
////				else
////				{
////					try 
////					{
////						CreateAgent ca = new CreateAgent();
////						ca.setAgentName(name!=null? name: getShortName(model));
////						ca.setClassName("jadex.jade.JadeAgentAdapter");
////						ca.setContainer(new ContainerID(myAgent.getContainerController().getContainerName(), null));
////						if(argus!=null)
////						{
////							for(int i=0; i<argus.size(); i++)
////							{
////								Object arg = argus.get(i);
//////											System.out.println(arg);
////								ca.addArguments(arg);
////							}
////						}
////						Action ac = new Action();
////						ac.setActor(myAgent.getAMS());
////						ac.setAction(ca);
////						ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
////						request.addReceiver(myAgent.getAMS());
////						request.setSender(myAgent.getAID());
////						request.setOntology(JADEManagementOntology.getInstance().getName());
////						request.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
////						request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
////						
////						convid = SUtil.createUniqueId(myAgent.getLocalName());
////						request.setConversationId(convid);
////						
////						myAgent.getContentManager().fillContent(request, ac);
////						// ACLMessage reply = FIPAService.doFipaRequestClient(this, request, 10000);
////						myAgent.send(request);
////						block();
////					} 
////					catch (Exception e) 
////					{
////						e.printStackTrace();
////					}
////				}
////			}
////			
////			public boolean done()
////			{
////				return done;
////			}
////		});
////		platform.getPlatformAgentController().putO2AObject(e, AgentController.ASYNC);
//				
//		try
//		{
//			AgentController ac = platform.getPlatformController().createNewAgent(name, "jadex.jade.JadeAgentAdapter", argus.toArray());
//			// Hack!!! Bug in JADE not returning created agent's AID.
//			// Should do ams_search do get correct AID?
//			AID tmp = (AID)platform.getPlatformAgent().clone();
//			int idx = tmp.getName().indexOf("@");
//			tmp.setName(name + tmp.getName().substring(idx));
//			aid = SJade.convertAIDtoFipa(tmp, (IComponentManagementService)platform.getService(IComponentManagementService.class));
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//			listener.exceptionOccurred(this, e);
//			return;
//		}
//		
////		ad	= new AMSAgentDescription(aid);
//		ad = new CMSComponentDescription(aid, type, parent, master);
//		ad.setState(IComponentDescription.STATE_INITIATED);
//		
////		System.out.println("added: "+agentdescs.size()+", "+aid);
//
//		IComponentListener[]	alisteners;
//		synchronized(listeners)
//		{
//			alisteners	= (IComponentListener[])listeners.toArray(new IComponentListener[listeners.size()]);
//		}
//		// todo: can be called after listener has (concurrently) deregistered
//		for(int i=0; i<alisteners.length; i++)
//		{
//			alisteners[i].componentAdded(ad);
//		}
//		
////		System.out.println("Created agent: "+aid);
//		listener.resultAvailable(this, aid); 
//	}
//
//	/**
//	 *  Start a previously created agent on the platform.
//	 *  @param agent The id of the previously created agent.
//	 */
//	public void	startElement(final Object agent, IResultListener lis)
//	{
//		if(agent==null)
//			throw new IllegalArgumentException("Agent identifier must not null.");
//		
//		final IResultListener listener = lis!=null? lis: DefaultResultListener.getInstance();
//		
////		try
////		{
////			Event e = new Event(-1, new SimpleBehaviour()
////			{
////				String convid = null;
////				boolean done = false;
////				
////				public void action()
////				{
////					System.out.println("Start agent code started.");
////
////					if(convid!=null)
////					{
////						ACLMessage reply = myAgent.receive();
////						if(reply==null)
////						{
////							block();
////						}
////						else
////						{
////							System.out.println("Reply received: "+reply);
////							if(reply.getPerformative()==ACLMessage.INFORM)
////							{
////								listener.resultAvailable(null);
////							}
////							else
////							{
////								listener.exceptionOccurred(new RuntimeException("Cannot start agent "+agent+" "+reply.getContent()));
////							}
////							done = true;
////						}
////					}
////					else
////					{
////						try 
////						{
////							jade.domain.FIPAAgentManagement.AMSAgentDescription amsd = new jade.domain.FIPAAgentManagement.AMSAgentDescription();
////							amsd.setName(SJade.convertAIDtoJade(agent));
////							amsd.setState(jade.domain.FIPAAgentManagement.AMSAgentDescription.SUSPENDED);
////							Modify m = new Modify();
////							m.setDescription(amsd);
////							Action ac = new Action();
////							ac.setActor(myAgent.getAMS());
////							ac.setAction(m);
////
////							ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
////							request.addReceiver(myAgent.getAMS());
////							request.setSender(myAgent.getAID());
////							request.setOntology(FIPAManagementOntology.NAME);
////							request.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
////							request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
////					
////							convid = SUtil.createUniqueId(myAgent.getLocalName());
////							request.setConversationId(convid);
////							
////							myAgent.getContentManager().fillContent(request, ac);
////							// ACLMessage reply = FIPAService.doFipaRequestClient(this, request, 10000);
////							myAgent.send(request);
////							block();
////						} 
////						catch (Exception e) 
////						{
////							e.printStackTrace();
////						}
////					}
////				}
////				
////				public boolean done()
////				{
////					return done;
////				}
////			});
////			
//////					platform.getPlatformAgentController().putO2AObject(e, AgentController.ASYNC);
////			platform.getPlatformController().getAgent("platform").putO2AObject(e, AgentController.ASYNC);
////			
//////					AgentController ac = platform.getPlatformController().createNewAgent(getShortName(model), "jadex.jade.JadeAgentAdapter", argus.toArray());
//////					ac.start();
////		}
////		catch(ControllerException e)
////		{
////			e.printStackTrace();
////			throw new RuntimeException(e);
////		}
//				
//		try
//		{
//			AgentController ac = platform.getPlatformController().getAgent(((IComponentIdentifier)agent).getLocalName());
//			ac.start();
//			listener.resultAvailable(null);
//		}
//		catch(Exception e)
//		{
//			listener.exceptionOccurred(e);
//		}
//	}
//	
//	/**
//	 *  Destroy (forcefully terminate) an agent on the platform.
//	 *  @param aid	The agent to destroy.
//	 */
//	public void destroyElement(final Object aid, IResultListener listener)
//	{
//		if(aid==null)
//			throw new IllegalArgumentException("Agent identifier must not null.");
//		if(listener==null)
//			listener = DefaultResultListener.getInstance();
//		
//		JadeAgentAdapter adapter = (JadeAgentAdapter)adapters.get(aid);
//		adapter.killAgent(new CleanupCommand((IComponentIdentifier)aid, listener));
//		
////		try
////		{
////			
////			AgentController ac = platform.getPlatformController().getAgent(aid.getLocalName());
////			ac.kill();
////			listener.resultAvailable(null);
////		}
////		catch(Exception e)
////		{
////			listener.exceptionOccurred(e);
////		}
//	}
//	
//	/**
//	 *  Suspend the execution of an agent.
//	 *  @param aid The agent identifier.
//	 *  // todo: make sure that agent is really suspended an does not execute
//	 *  an action currently.
//	 */
//	public void suspendElement(final Object aid, IResultListener lis)
//	{
//		final IResultListener listener = lis!=null? lis: DefaultResultListener.getInstance();
//		
////		Event e = new Event(-1, new SimpleBehaviour()
////		{
////			String convid = null;
////			boolean done = false;
////			
////			public void action()
////			{
////				System.out.println("Create agent code started.");
////
////				if(convid!=null)
////				{
////					ACLMessage reply = myAgent.receive();
////					if(reply==null)
////					{
////						block();
////					}
////					else
////					{
////						System.out.println("Reply received: "+reply);
////						if(reply.getPerformative()==ACLMessage.INFORM)
////						{
////							listener.resultAvailable(ret);
////						}
////						else
////						{
////							listener.exceptionOccurred(new AgentCreationException(reply.getContent(), null));
////						}
////						done = true;
////					}
////				}
////				else
////				{
////					try 
////					{
////						jade.domain.FIPAAgentManagement.AMSAgentDescription amsd = new jade.domain.FIPAAgentManagement.AMSAgentDescription();
////						amsd.setName(SJade.convertAIDtoJade(aid));
////						amsd.setState(jade.domain.FIPAAgentManagement.AMSAgentDescription.SUSPENDED);
////						Modify m = new Modify();
////						m.setDescription(amsd);
////						Action ac = new Action();
////						ac.setActor(myAgent.getAMS());
////						ac.setAction(m);
////
////						ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
////						request.addReceiver(myAgent.getAMS());
////						request.setSender(myAgent.getAID());
////						request.setOntology(FIPAManagementOntology.NAME);
////						request.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
////						request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
////				
////						convid = SUtil.createUniqueId(myAgent.getLocalName());
////						request.setConversationId(convid);
////						
////						myAgent.getContentManager().fillContent(request, ac);
////						// ACLMessage reply = FIPAService.doFipaRequestClient(this, request, 10000);
////						myAgent.send(request);
////						block();
////					} 
////					catch (Exception e) 
////					{
////						e.printStackTrace();
////					}
////				}
////			}
////			
////			public boolean done()
////			{
////				return done;
////			}
////		});
////		platform.getPlatformController().getAgent("platform").putO2AObject(e, AgentController.ASYNC);
//	
//		try
//		{
//			AgentController ac = platform.getPlatformController().getAgent(((IComponentIdentifier)aid).getLocalName());
//			ac.suspend();
//			listener.resultAvailable(null);
//		}
//		catch(Exception e)
//		{
//			listener.exceptionOccurred(e);
//		}
//	}
//	
//	/**
//	 *  Resume the execution of an agent.
//	 *  @param aid The agent identifier.
//	 */
//	public void resumeElement(Object aid, IResultListener listener)
//	{
//		if(listener==null)
//			listener = DefaultResultListener.getInstance();
//		
//		try
//		{
//			AgentController ac = platform.getPlatformController().getAgent(((IComponentIdentifier)aid).getLocalName());
//			ac.activate();
//			listener.resultAvailable(null);
//		}
//		catch(Exception e)
//		{
//			listener.exceptionOccurred(e);
//		}
//	}
//
//	/**
//	 *  Search for agents matching the given description.
//	 *  @return An array of matching agent descriptions.
//	 */
//	public void	searchAgents(final IComponentDescription adesc, final ISearchConstraints con, IResultListener lis)
//	{
//		final IResultListener listener = lis!=null? lis: DefaultResultListener.getInstance();
//		
//		Event e = new Event(-1, new SimpleBehaviour()
//		{
//			String convid = null;
//			boolean done = false;
//			
//			public void action()
//			{
//				if(convid!=null)
//				{
//					ACLMessage reply = myAgent.receive(MessageTemplate.MatchConversationId(convid));
//					if(reply==null)
//					{
//						block();
//					}
//					else
//					{
//						if(reply.getPerformative()==ACLMessage.INFORM)
//						{
//							try
//							{
//								Result res = (Result)myAgent.getContentManager().extractContent(reply);
//								jade.util.leap.List descs = res.getItems();
//								IComponentDescription[] ret = new IComponentDescription[descs.size()];
//								for(int i=0; i<ret.length; i++)
//								{
//									ret[i] = SJade.convertAMSAgentDescriptiontoFipa(
//										(jade.domain.FIPAAgentManagement.AMSAgentDescription)descs.get(i), AMS.this);
//								}
//								listener.resultAvailable(ret);
//							}
//							catch(Exception e)
//							{
//								listener.exceptionOccurred(e);
//							}
//						}
//						else
//						{
//							listener.exceptionOccurred(new RuntimeException("Search failed."));
//						}
//						done = true;
//					}
//				}
//				else
//				{
//					try 
//					{
//						// Hack !!! Strip addresses/resolvers from aid before search (JADE doesn't store these in AMS and therefore finds no matches, grrr).
//						jade.domain.FIPAAgentManagement.AMSAgentDescription	amsadesc	= SJade.convertAMSAgentDescriptiontoJade(adesc);
//						if(amsadesc.getName()!=null)
//							amsadesc.setName(new AID(amsadesc.getName().getName(), AID.ISGUID));
//						Search search = new Search();
//						search.setDescription(amsadesc);
//						search.setConstraints(SJade.convertSearchConstraintstoJade(
//							con!=null ? con	: createSearchConstraints(-1, 0)));
//						Action ac = new Action();
//						ac.setActor(myAgent.getAMS());
//						ac.setAction(search);
//						ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
//						request.addReceiver(myAgent.getAMS());
//						request.setSender(myAgent.getAID());
//						request.setOntology(FIPAManagementOntology.getInstance().getName());
//						request.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
//						request.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
//						
//						convid = SUtil.createUniqueId(myAgent.getLocalName());
//						request.setConversationId(convid);
//						
//						myAgent.getContentManager().fillContent(request, ac);
//						// ACLMessage reply = FIPAService.doFipaRequestClient(this, request, 10000);
//						myAgent.send(request);
//						block();
//					} 
//					catch (Exception e) 
//					{
//						e.printStackTrace();
//					}
//				}
//			}
//			
//			public boolean done()
//			{
//				return done;
//			}
//		});
//		
//		try
//		{
//			platform.getPlatformController().getAgent("platform").putO2AObject(e, AgentController.ASYNC);
//		}
//		catch(ControllerException ex)
//		{
//			listener.exceptionOccurred(ex);
//		}
//	}
//	
//	/**
//	 *  Test if an agent is currently living on the platform.
//	 *  @param aid The agent identifier.
//	 *  @return True, if agent is hosted on platform.
//	 */
//	public void containsAgent(IComponentIdentifier aid, IResultListener listener)
//	{
//		if(listener==null)
//			throw new RuntimeException("Result listener required.");
//		
//		listener.resultAvailable(adapters.containsKey(aid)? Boolean.TRUE: Boolean.FALSE);
//	}
//	
//	/**
//	 *  Get the agent description of a single agent.
//	 *  @param aid The agent identifier.
//	 *  @return The agent description of this agent.
//	 */
//	public void getAgentDescription(IComponentIdentifier aid, final IResultListener listener)
//	{
//		if(listener==null)
//			throw new RuntimeException("Result listener required.");
//		
//		AMSAgentDescription adesc = new AMSAgentDescription();
//		adesc.setName(aid);
//		searchAgents(adesc, null, new IResultListener()
//		{
//			public void resultAvailable(Object result)
//			{
//				IComponentDescription[] descs = (IComponentDescription[])result;
//				listener.resultAvailable(descs.length==1? descs[0]: null);
//			}
//			public void exceptionOccurred(Exception exception)
//			{
//				listener.exceptionOccurred(exception);
//			}
//		});
//	}
//	
//	/**
//	 *  Get the agent descriptions.
//	 *  @return The agent descriptions.
//	 */
//	public void getAgentDescriptions(IResultListener listener)
//	{
//		if(listener==null)
//			throw new RuntimeException("Result listener required.");
//		
//		AMSAgentDescription adesc = new AMSAgentDescription();
//		SearchConstraints con = new SearchConstraints();
//		con.setMaxResults(Integer.MAX_VALUE);
//		searchAgents(adesc, con, listener);
//	}
//	
//	/**
//	 *  Get the agent adapters.
//	 *  @return The agent adapters.
//	 */
//	public void getAgentIdentifiers(IResultListener listener)
//	{
//		if(listener==null)
//			throw new RuntimeException("Result listener required.");
//		
//		IComponentIdentifier[] ret;
//		
//		synchronized(adapters)
//		{
//			ret = (IComponentIdentifier[])adapters.keySet().toArray(new IComponentIdentifier[adapters.size()]);
//			for(int i=0; i<ret.length; i++)
//				ret[i] = refreshAgentIdentifier(ret[i]); // Hack!
//		}
//		
//		listener.resultAvailable(ret);
//	}
//	
//	/**
//	 *  Get the number of active agents.
//	 *  @return The number of active agents.
//	 */
//	public void getAgentCount(IResultListener listener)
//	{
//		if(listener==null)
//			throw new RuntimeException("Result listener required.");
//		
//		listener.resultAvailable(new Integer(adapters.size()));
//	}
//	
//	/**
//	 *  Get the agent adapters.
//	 *  @return The agent adapters.
//	 * /
//	public void getAgentIdentifiers(final IResultListener listener)
//	{
//		if(listener==null)
//			throw new RuntimeException("Result listener required.");
//		
//		getAgentDescriptions(new IResultListener()
//		{
//			public void resultAvailable(Object result)
//			{
//				IComponentDescription[] descs = (IComponentDescription[])result;
//				IComponentIdentifier[] ret = new IComponentIdentifier[descs.length];
//				for(int i=0; i<descs.length; i++)
//				{
//					ret[i] = descs[i].getName();
//				}
//				listener.resultAvailable(ret);
//			}
//			public void exceptionOccurred(Exception exception)
//			{
//				listener.exceptionOccurred(exception);
//			}
//		});
//	}*/
//	
//	/**
//	 *  Get the number of active agents.
//	 *  @return The number of active agents.
//	 * /
//	public void getAgentCount(final IResultListener listener)
//	{
//		if(listener==null)
//			throw new RuntimeException("Result listener required.");
//		
//		getAgentDescriptions(new IResultListener()
//		{
//			public void resultAvailable(Object result)
//			{
//				listener.resultAvailable(((IComponentDescription[])result).length);
//			}
//			public void exceptionOccurred(Exception exception)
//			{
//				listener.exceptionOccurred(exception);
//			}
//		});
//	}*/
//	
//	/**
//	 *  Get the agent adapter for an agent identifier.
//	 *  @param aid The agent identifier.
//	 *  @return The agent adapter.
//	 */
//	public void getAgentAdapter(IComponentIdentifier aid, IResultListener listener)
//	{
//		if(listener==null)
//			throw new RuntimeException("Result listener required.");
//		
//		listener.resultAvailable(adapters.get(aid));
//	}
//	
//	/**
//	 *  Get the external access of an agent.
//	 *  @param aid The agent identifier.
//	 *  @param listener The result listener.
//	 */
//	public void getExternalAccess(IComponentIdentifier aid, IResultListener listener)
//	{
//		if(listener==null)
//			throw new RuntimeException("Result listener required.");
//		
//		JadeAgentAdapter adapter = (JadeAgentAdapter)adapters.get(aid);
//		if(adapter==null)
//			listener.exceptionOccurred(new RuntimeException("No local agent found for agent identifier: "+aid));
//		else
//			adapter.getKernelAgent().getExternalAccess(listener);
//	}
//
//	
//	//-------- IPlatformService interface methods --------
//	 
//	/**
//	 *  Start the service.
//	 */
//	public void startService()
//	{
//		// nothing to do.
//	}
//	
//	/**
//	 *  Called when the platform shuts down.
//	 *  At this time all agents already had time to kill themselves
//	 *  (as specified in the platform shutdown time).
//	 *  Remaining agents should be discarded.
//	 */
//	public void shutdownService(IResultListener listener)
//	{
//		if(listener==null)
//			listener = DefaultResultListener.getInstance();
//		
//		listener.resultAvailable(null);
//	}
//	
//	/**
//	 *  Create an agent identifier.
//	 *  @param name The name.
//	 *  @param local True for local name.
//	 *  @return The new agent identifier.
//	 */
//	public IComponentIdentifier createComponentIdentifier(String name, boolean local)
//	{
//		if(local)
//			name = name + "@" + platform.getName();
//		return new AgentIdentifier(name);
//	}
//	
//	/**
//	 *  Create an agent identifier.
//	 *  @param name The name.
//	 *  @param local True for local name.
//	 *  @param addresses The addresses.
//	 */
//	public IComponentIdentifier createComponentIdentifier(String name, boolean local, String[] addresses)
//	{
//		if(local)
//			name = name + "@" + platform.getName();
//		return new AgentIdentifier(name, addresses, null);
//	}
//	
//	/**
//	 *  Create a search constraints object.
//	 *  @param maxresults The maximum number of results.
//	 *  @param maxdepth The maximal search depth.
//	 *  @return The search constraints.
//	 */
//	public ISearchConstraints createSearchConstraints(int maxresults, int maxdepth)
//	{
//		SearchConstraints	ret	= new SearchConstraints();
//		ret.setMaxResults(maxresults);
//		ret.setMaxDepth(maxdepth);
//		return ret;
//	}
//
//	/**
//	 *  Create a ams agent description.
//	 *  @param agent The agent.
//	 *  @return The ams agent description.
//	 */
//	public IComponentDescription createAMSAgentDescription(IComponentIdentifier agent)
//	{
//		return new AMSAgentDescription(agent);
//	}
//
//	/**
//	 *  Create a ams agent description.
//	 *  @param agent The agent.
//	 *  @param state The state.
//	 *  @param ownership The ownership.
//	 *  @return The ams agent description.
//	 */
//	public IComponentDescription createAMSAgentDescription(IComponentIdentifier agent, String state, String ownership)
//	{
//		AMSAgentDescription	ret	= new AMSAgentDescription(agent);
//		ret.setState(state);
//		ret.setOwnership(ownership);
//		return ret;
//	}
//
//	/**
//	 *  Shutdown the platform.
//	 *  @param listener The listener.
//	 */
//	public void shutdownPlatform(IResultListener listener)
//	{
//		// todo
////		platform.shutdown(listener);
//	}
//
//	//-------- Helper methods --------
//		
//	/**
//	 *  Get the agent adapters.
//	 *  @return The agent adapters.
//	 */
//	public IComponentAdapter[] getAgentAdapters()
//	{
//		synchronized(adapters)
//		{
//			return (IComponentAdapter[])adapters.values().toArray(new IComponentAdapter[adapters.size()]);
//		}
//	}
//	
//	/**
//	 *  Get the agent adapters.
//	 *  @return The agent adapters.
//	 */
//	public Map getAgentAdapterMap()
//	{
//		return adapters;
//	}
//	
//	/**
//	 *  Copy and refresh local agent identifier.
//	 *  @param aid The agent identifier.
//	 *  @return The refreshed copy of the aid.
//	 */
//	public IComponentIdentifier refreshAgentIdentifier(IComponentIdentifier aid)
//	{
//		IComponentIdentifier	ret	= (IComponentIdentifier)((AgentIdentifier)aid).clone();
//		if(adapters.containsKey(aid))
//		{
//			IMessageService	ms	= (IMessageService)platform.getService(IMessageService.class);
//			if(ms!=null)
//				((AgentIdentifier)ret).setAddresses(ms.getAddresses());
//		}
//		return ret;
//	}
//	
//	/**
//	 *  Create an agent name that is not yet used on the platform.
//	 *  @param typename The type name.
//	 *  @return The agent name.
//	 */
//	protected String generateAgentName(String typename)
//	{
//		AgentIdentifier ret = null;
//
//		synchronized(adapters)
//		{
//			do
//			{
//				ret = new AgentIdentifier(typename+(agentcnt++)+"@"+platform.getName()); // Hack?!
//			}
//			while(adapters.containsKey(ret));
//		}
//		
//		return ret.getLocalName();
//	}
//	
//	//-------- listener methods --------
//	
//	/**
//     *  Add an ams listener.
//     *  The listener is registered for ams changes.
//     *  @param listener  The listener to be added.
//     */
//    public void addElementListener(IComponentListener listener)
//	{
//		synchronized(listeners)
//		{
//			listeners.add(listener);
//		}
//    }
//    
//    /**
//     *  Remove an ams listener.
//     *  @param listener  The listener to be removed.
//     */
//    public void removeElementListener(IComponentListener listener)
//	{
//		synchronized(listeners)
//		{
//			listeners.remove(listener);
//		}
//    }
//
//	/**
//	 *  Get the short type name from a model filename.
//	 *  @param filename The filename.
//	 *  @return The short type name.
//	 */
//	public String getShortName(String filename)
//	{
//		ILoadableComponentModel model = SComponentFactory.loadModel(platform, filename);
////		ILoadableElementModel	model	= platform.getAgentFactory().loadModel(filename);
//		return model.getName();
//	}
//
//	/**
//	 *  Command that is executed on agent cleanup.
//	 */
//	class CleanupCommand implements IResultListener
//	{
//		protected IComponentIdentifier aid;
//		protected IResultListener listener;
//		
//		public CleanupCommand(IComponentIdentifier aid, IResultListener listener)
//		{
//			this.aid = aid;
//			this.listener = listener;
//		}
//		
//		public void resultAvailable(Object result)
//		{
//			synchronized(adapters)
//			{
//				JadeAgentAdapter adapter	= (JadeAgentAdapter)adapters.remove(aid);
//				if(adapter==null)
//					throw new RuntimeException("Agent Identifier not registered in AMS: "+aid);
//				adapter.setState(IComponentDescription.STATE_TERMINATED);
//				
//				// Stop execution of agent.
//				adapter.cleanupAgent();
//			}
//			
//			IComponentListener[]	alisteners;
//			synchronized(listeners)
//			{
//				alisteners	= (IComponentListener[])listeners.toArray(new IComponentListener[listeners.size()]);
//			}
//			// todo: can be called after listener has (concurrently) deregistered
//			for(int i=0; i<alisteners.length; i++)
//			{
//				AMSAgentDescription ad = new AMSAgentDescription();
//				ad.setName(aid);
//				alisteners[i].componentRemoved(ad);
//			}
//			
//			if(listener!=null)
//				listener.resultAvailable(result);
//		}
//		
//		public void exceptionOccurred(Exception exception)
//		{
//			resultAvailable(aid);
//		}
//	}
//}

