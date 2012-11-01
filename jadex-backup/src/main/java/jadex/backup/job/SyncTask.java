package jadex.backup.job;



import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class SyncTask extends Task
{
	/** The sync source. */
	protected String source;
	
	/** The entries. */
	protected List<SyncTaskEntry> entries;
	
	/**
	 *  Create a new sync request.
	 */
	public SyncTask()
	{
	}
	
	/**
	 *  Create a new sync request.
	 */
	public SyncTask(String jobid, String source, long date)
	{
		super(jobid, date);
		this.source = source;
	}

	/**
	 *  Add a new sync entry.
	 */
	public void addSyncEntry(SyncTaskEntry se)
	{
		if(entries==null)
			entries = new ArrayList<SyncTaskEntry>();
		entries.add(se);
	}

	/**
	 *  Get the entries.
	 *  @return The entries.
	 */
	public List<SyncTaskEntry> getEntries()
	{
		return entries;
	}

	/**
	 *  Set the entries.
	 *  @param entries The entries to set.
	 */
	public void setEntries(List<SyncTaskEntry> entries)
	{
		this.entries = entries;
	}

	/**
	 *  Get the source.
	 *  @return The source.
	 */
	public String getSource()
	{
		return source;
	}

	/**
	 *  Set the source.
	 *  @param source The source to set.
	 */
	public void setSource(String source)
	{
		this.source = source;
	}
	
	/**
	 *  Get an entry per id.
	 */
	public SyncTaskEntry getEntry(String id)
	{
		SyncTaskEntry ret = null;
		
		if(entries!=null)
		{
			for(SyncTaskEntry entry: entries)
			{
				if(id.equals(entry.getId()))
				{
					ret = entry;
				}
			}
		}
		
		return ret;
	}

	/**
	 *  Get the string representation.
	 */
	public String toString()
	{
		return "SyncTask(source="+source+", date=" + (date==0? sdf.format(date): date) + ")";
	}
}
