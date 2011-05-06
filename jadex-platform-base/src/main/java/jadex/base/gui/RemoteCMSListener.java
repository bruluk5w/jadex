package jadex.base.gui;

import jadex.bridge.ICMSComponentListener;
import jadex.bridge.IComponentDescription;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentManagementService;
import jadex.commons.ChangeEvent;
import jadex.commons.IRemoteChangeListener;
import jadex.commons.SUtil;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 *  The component listener installed at the remote CMS.
 */
public class RemoteCMSListener	implements ICMSComponentListener
{
	//-------- attributes --------
	
	/** The local platform cid used as event source. */
	protected IComponentIdentifier	cid;
	
	/** The id for remote listener deregistration. */
	protected String	id;
	
	/** The CMS used for automatic removal of listener. */
	protected IComponentManagementService	cms;
	
	/** The change listener (proxy) to be informed about important changes. */
	protected IRemoteChangeListener	rcl;
	
	/** The update timer (if any). */
	protected Timer	timer;
	
	/** The added components, if any (cid->desc). */
	protected Map	added;
			
	/** The changed components, if any (cid->desc). */
	protected Map	changed;
			
	/** The removed components, if any (cid->desc). */
	protected Map	removed;
			
	//-------- constructors --------
	
	/**
	 *  Create a CMS listener sending updates to a remote change listener.
	 */
	public RemoteCMSListener(IComponentIdentifier cid, String id, IComponentManagementService cms, IRemoteChangeListener rcl)
	{
		this.cid	= cid;
		this.id	= id;
		this.cms	= cms;
		this.rcl	= rcl;
	}
	
	//-------- ICMSComponentListener interface --------
	
	/**
	 *  Called when a new element has been added.
	 *  @param id The identifier.
	 */
	public synchronized IFuture componentAdded(final IComponentDescription desc)
	{
		if(removed!=null && removed.containsKey(desc.getName()))
		{
			removed.remove(desc.getName());
		}
		else
		{
			if(added==null)
			{
				added	= new LinkedHashMap();
			}
			added.put(desc.getName(), desc);
		}
		
		startTimer();
		return IFuture.DONE;
	}		
	
	/**
	 *  Called when a component has changed its state.
	 *  @param id The identifier.
	 */
	public IFuture componentChanged(IComponentDescription desc)
	{
		if(added!=null && added.containsKey(desc.getName()))
		{
			added.put(desc.getName(), desc);
		}
		else
		{
			if(changed==null)
			{
				changed	= new LinkedHashMap();
			}
			changed.put(desc.getName(), desc);
		}
		
		startTimer();
		return IFuture.DONE;
	}
	
	/**
	 *  Called when a new element has been removed.
	 *  @param id The identifier.
	 */
	public synchronized IFuture componentRemoved(final IComponentDescription desc, Map results)
	{
		if(changed!=null && changed.containsKey(desc.getName()))
		{
			changed.remove(desc.getName());
		}
		
		if(added!=null && added.containsKey(desc.getName()))
		{
			added.remove(desc.getName());
		}
		else
		{
			if(removed==null)
			{
				removed	= new LinkedHashMap();
			}
			removed.put(desc.getName(), desc);
		}
		
		startTimer();
		return IFuture.DONE;
	}

	protected void startTimer()
	{
		if(timer==null)
		{
			timer	= new Timer(true);
			timer.schedule(new TimerTask()
			{
				public void run()
				{
					List	events	= new ArrayList();
					synchronized(RemoteCMSListener.this)
					{
						timer	= null;
						if(removed!=null)
						{
							for(Iterator it=removed.values().iterator(); events.size()<CMSUpdateHandler.MAX_EVENTS && it.hasNext(); )
							{
								events.add(new ChangeEvent(cid, CMSUpdateHandler.EVENT_COMPONENT_REMOVED, it.next()));
								it.remove();
							}
						}
						if(added!=null)
						{
							for(Iterator it=added.values().iterator(); events.size()<CMSUpdateHandler.MAX_EVENTS && it.hasNext(); )
							{
								events.add(new ChangeEvent(cid, CMSUpdateHandler.EVENT_COMPONENT_ADDED, it.next()));
								it.remove();
							}
						}
						if(changed!=null)
						{
							for(Iterator it=changed.values().iterator(); events.size()<CMSUpdateHandler.MAX_EVENTS && it.hasNext(); )
							{
								events.add(new ChangeEvent(cid, CMSUpdateHandler.EVENT_COMPONENT_CHANGED, it.next()));
								it.remove();
							}
						}
						
						if(removed!=null && removed.isEmpty())
							removed	= null;
						if(added!=null && added.isEmpty())
							added	= null;
						if(changed!=null && changed.isEmpty())
							changed	= null;
						
						if(removed!=null || added!=null || changed!=null)
						{
							startTimer();
						}
					}
					
					if(!events.isEmpty())
					{
//						System.out.println("events: "+events.size());
						rcl.changeOccurred(new ChangeEvent(cid, CMSUpdateHandler.EVENT_BULK, events)).addResultListener(new IResultListener()
						{
							public void resultAvailable(Object result)
							{
//								System.out.println("update succeeded: "+cid);
							}
							public void exceptionOccurred(Exception exception)
							{
//								exception.printStackTrace();
								if(cms!=null)
								{
//									System.out.println("Removing listener due to failed update: "+RemoteCMSListener.this.id);
									try
									{
										cms.removeComponentListener(null, RemoteCMSListener.this);
									}
									catch(RuntimeException e)
									{
//										System.out.println("Listener already removed: "+id);
									}
									cms	= null;	// Set to null to avoid multiple removal due to delayed errors. 
								}
							}
						});
					}
				}
			}, CMSUpdateHandler.UPDATE_DELAY);
		}
	}
	
	//-------- methods --------
	
	/**
	 *  Test if two objects are equal.
	 */
	public boolean	equals(Object obj)
	{
		return obj instanceof RemoteCMSListener && SUtil.equals(((RemoteCMSListener)obj).id, id);
	}
	
	/**
	 *  Get the hashcode
	 */
	public int	hashCode()
	{
		return 31+id.hashCode();
	}
}