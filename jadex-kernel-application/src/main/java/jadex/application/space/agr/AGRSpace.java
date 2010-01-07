package jadex.application.space.agr;

import jadex.application.model.MSpaceInstance;
import jadex.application.runtime.Application;
import jadex.application.runtime.ISpace;
import jadex.bridge.IComponentIdentifier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *  An AGR (agent-group-role) space.
 */
public class AGRSpace implements ISpace
{
	//-------- attributes --------
	
	/** The application. */
	protected Application	context;
	
	/** The groups. */
	protected Map groups;
	
	//-------- methods --------
	
	/**
	 *  Add a group to the space.
	 *  @param group	The group to add. 
	 */
	public synchronized	void addGroup(Group group)
	{
		if(groups==null)
			groups	= new HashMap();
		
		groups.put(group.getName(), group);
	}
	
	/**
	 *  Get a group by name.
	 *  @param name	The name of the group.
	 *  @return	The group (if any).
	 */
	public synchronized Group	getGroup(String name)
	{
		return groups!=null ? (Group)groups.get(name) : null;
	}	

	/**
	 *  Called from application component, when a component was added.
	 *  @param cid	The id of the added component.
	 */
	public synchronized void	componentAdded(IComponentIdentifier cid, String type)
	{
		if(groups!=null)
		{
			for(Iterator it=groups.values().iterator(); it.hasNext(); )
			{
				Group	group	= (Group)it.next();
				String[]	roles	= group.getRolesForType(type);
				for(int r=0; roles!=null && r<roles.length; r++)
				{
					group.assignRole(cid, roles[r]);
				}
			}
		}
	}

	/**
	 *  Called from application component, when a component was removed.
	 *  @param cid	The id of the removed component.
	 */
	public synchronized void	componentRemoved(IComponentIdentifier cid)
	{
		// nothing to do.
	}
	
	/**
	 *  Terminate the space.
	 */
	public void terminate()
	{
		// nothing to do.
	}
	
	public void initSpace(Application context, MSpaceInstance config) throws Exception
	{
//		AGRSpace	ret	= new AGRSpace(getName(), app);
//		this.name	= name;
//		this.context	= context;
		MGroupInstance[]	mgroups	= ((MAGRSpaceInstance)config).getMGroupInstances();
		for(int g=0; g<mgroups.length; g++)
		{
			Group	group	= new Group(mgroups[g].getName());
			this.addGroup(group);
			
			MPosition[]	positions	= mgroups[g].getMPositions();
			for(int p=0; positions!=null && p<positions.length; p++)
			{
				String	at	= positions[p].getAgentType();
				String	rt	= positions[p].getRoleType();
				group.addRoleForType(at, rt);
			}
		}
	}
}
