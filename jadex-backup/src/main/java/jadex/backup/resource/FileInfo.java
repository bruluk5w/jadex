package jadex.backup.resource;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;


/**
 *  Public meta information about a file in a resource
 *  used for transfer between resource providers and clients.
 */
public class FileInfo
{
	//-------- attributes --------
	
	/** The file location relative to the resource root (using '/' as separator char). */
	protected String	location;
	
	/** True, if the file is a directory. */
	protected boolean	directory;
	
	/** True, if the file or directory exists. */
	protected boolean	exists;
	
	/** The file size. */
	protected long	size;
	
	/** The cached vector times as map (transferred as string "platform1@time1.platform2@time2..."). */
	protected Map<String, Long>	vtimes;
	
	/** The cached hash code if any (transferred in base 64 at start of vtime string "hash.platform1@time1..."). */
	protected String	hash;
	
	//-------- constructors --------
	
	/**
	 *  Create a file info.
	 */
	public FileInfo()
	{
		// bean constructor.
	}
	
	/**
	 *  Create a file info.
	 */
	public FileInfo(String location, boolean directory, long size, String vtime)
	{
		this.location	= location;
		this.directory	= directory;
		this.exists	= parseExists(vtime);
		this.size	= size;
		this.vtimes	= parseVTime(vtime);
	}
	
	//-------- bean accessors --------
	
	/**
	 *  Get the location.
	 *  @return the location.
	 */
	public String getLocation()
	{
		return location;
	}

	/**
	 *  Set the location.
	 *  @param location The location to set.
	 */
	public void setLocation(String location)
	{
		this.location = location;
	}
	
	/**
	 *  Get the existing state.
	 *  @return True, if the file or directory exists.
	 */
	public boolean isExisting()
	{
		return exists;
	}

	/**
	 *  Set the existing state.
	 *  @param exists True, if the file or directory exists.
	 */
	public void setExisting(boolean exists)
	{
		this.exists = exists;
	}
	
	/**
	 *  Get the directory.
	 *  @return the directory.
	 */
	public boolean isDirectory()
	{
		return directory;
	}

	/**
	 *  Set the directory.
	 *  @param directory The directory to set.
	 */
	public void setDirectory(boolean directory)
	{
		this.directory = directory;
	}
	
	/**
	 *  Get the file size.
	 */
	public long	getSize()
	{
		return size;
	}
	
	/**
	 *  Set the file size.
	 */
	public void	setSize(long size)
	{
		this.size	= size;
	}

	/**
	 *  Get the vector time.
	 */
	public String	getVTime()
	{
		return vtimesToString(vtimes, hash, exists);
	}
	
	/**
	 *  Set the vector time.
	 */
	public void	setVTime(String vtime)
	{
		this.vtimes	= parseVTime(vtime);
	}
	
	/**
	 *  Get the hash code if any.
	 */
	public String getHash()
	{
		return hash;
	}
	
	/**
	 *  Set the hash code.
	 */
	public void setHash(String hash)
	{
		this.hash = hash;
	}

	//-------- methods --------
	
	/**
	 *  Get a part of the vector time.
	 *  @param node	The platform.
	 *  @return The time (negative for an outdated time stamp).
	 */
	public long	getVTime(String node)
	{
		return vtimes.containsKey(node) ? vtimes.get(node).longValue() : 0;
	}
	
	/**
	 *  Update a part of the vector time and 
	 *  invalidate other stored times if hash values differ.
	 *  @param node	The platform.
	 *  @param time	The time.
	 *  @param hash	The new hash.
	 */
	public void	bumpVTime(String node, long time, String hash, boolean exists)
	{
		boolean	change	= this.hash==null || !this.hash.equals(hash) || exists!=this.exists;

		// on change -> invalidate other times and update hash.
		if(change)
		{
			for(String key: vtimes.keySet())
			{
				if(!node.equals(key))
				{
					// Use abs() for handling valid and invalid times
					setVTime(key, -Math.abs(vtimes.get(key).longValue()));
				}
			}
			
			this.hash	= hash;
			this.exists	= exists;
		}
		
		setVTime(node, time);
	}
	
	/**
	 *  Update a part of the vector time.
	 *  @param node	The platform.
	 *  @param time	The time.
	 */
	public void	setVTime(String node, long time)
	{
		vtimes.put(node, new Long(time));
	}
	
	
	/**
	 *  Find out if this file or directory has changed with respect to a target.
	 *  Note that this method might return true in either direction in case of a conflict
	 *  (i.e. both local and target are newer with respect to each other).
	 *  
	 *  @param target	The target file info.
	 */
	protected boolean	isNewerThan(FileInfo fi)
	{
		if(!location.equals(fi.getLocation()))
		{
			throw new IllegalArgumentException("Location differs: "+fi.getLocation());
		}
		
		// Local file has not changed wrt remote when
		// 1: hash values are equal (currently only tested for directories)
		// 2: a locally valid time stamp is found for which a greater or equal time stamp exists remotely as valid or invalid
		
		boolean	changed	= getHash()==null || !getHash().equals(fi.getHash());
		if(changed)
		{
			for(Iterator<String> it=vtimes.keySet().iterator(); changed && it.hasNext(); )
			{
				String node	= it.next();
				// No match (changed stays true) when:
				// locally invalid or abs value is larger.
				long	local	= getVTime(node);
				long	remote	= fi.getVTime(node);
				changed	= local<=0 || local>Math.abs(remote);
			}
		}
		
		return changed;
	}

	/**
	 *  Update the vector times of this file info
	 *  with vector times of another file info,
	 *  if the absolute values are larger or the same but valid instead of invalid.
	 *  @param fi	The remote file info, from which which times should be taken.
	 *  @param valid	Set new valid times as valid (e.g. for update or copy) or all new times as invalid (for override).
	 */
	public void	updateVTimes(FileInfo fi, boolean valid)
	{
		if(!location.equals(fi.getLocation()))
		{
			throw new IllegalArgumentException("Location differs: "+fi.getLocation());
		}
		
		Set<String>	nodes	= new HashSet<String>(vtimes.keySet());
		nodes.addAll(fi.vtimes.keySet());
		
		for(String node: nodes)
		{
			if(Math.abs(getVTime(node))<Math.abs(fi.getVTime(node))
				|| Math.abs(getVTime(node))==fi.getVTime(node))
			{
				setVTime(node, valid ? fi.getVTime(node) : -Math.abs(fi.getVTime(node)));
			}
		}		
	}
	
	//-------- helper methods --------
	
	/**
	 *  Get the existing state.
	 */
	protected static boolean	parseExists(String vtime)
	{
		return !vtime.startsWith("D.");
	}
	
	/**
	 *  Get the hash if any.
	 */
	protected static String	parseHash(String vtime)
	{
		String	ret	= null;
		if(vtime.indexOf('.')<vtime.indexOf('@'))
		{
			ret	= vtime.substring(vtime.startsWith("D.") ? 2 : 0, vtime.indexOf('.'));
		}
		return ret;
	}
	
	/**
	 *  Get the vector time as map.
	 */
	protected static Map<String, Long>	parseVTime(String vtime)
	{
		Map<String, Long>	vtimes	= new LinkedHashMap<String, Long>();
		StringTokenizer	stok	= new StringTokenizer(vtime, "@.", true);
		String	last	= null;
		while(stok.hasMoreTokens())
		{
			String	next	= stok.nextToken();
			if("@".equals(next) && stok.hasMoreTokens())
			{
				vtimes.put(last, new Long(Long.parseLong(stok.nextToken())));
			}
			last	= next;
		}
		return vtimes;
	}
	
	/**
	 *  Get the vector time as string.
	 */
	protected static String	vtimesToString(Map<String, Long> vtimes, String hash, boolean exists)
	{
		StringBuffer	buf	= new StringBuffer();
		
		if(!exists)
		{
			buf.append("D.");
		}
		
		for(String key: vtimes.keySet())
		{
			if(buf.length()>0)
			{
				buf.append('.');
			}
			buf.append(key);
			buf.append("@");
			buf.append(vtimes.get(key));
		}
		
		if(hash!=null)
		{
			if(buf.length()>0)
			{
				buf.insert(0, '.');
			}
			buf.insert(0, hash);
		}

		return buf.toString();
	}
}
