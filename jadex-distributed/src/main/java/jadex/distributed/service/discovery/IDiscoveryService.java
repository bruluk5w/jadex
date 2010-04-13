package jadex.distributed.service.discovery;


import java.net.InetAddress;
import java.util.Set;

public interface IDiscoveryService {
	
	/**
	 * A listener uses this method to register this IDiscoveryService. The
	 * listener needs to implement callback methods to update his list of known
	 * machines. The callback methods are declared in the
	 * IDiscoveryServiceListener interface.
	 * @param listener - the object which wants to be notified about new
	 *                   machines
	 */
	public void register(IDiscoveryServiceListener listener);
	
	/**
	 * A listener uses this method to unregister from this IDiscoveryService.
	 * @param listener - the object which wants to unregister from this
	 *                   IDiscoveryService.
	 */
	public void unregister(IDiscoveryServiceListener listener);
	
	/**
	 * Usually called by a registered IDiscoveryListener to get the current
	 * list of known machines. But of course can also be called by an arbitrary object/class.
	 * 
	 * @return a Set of known machines; this is a immutable, read-only snapshot of the actual
	 * set to enable concurrent modification of the actual set while other objects read from
	 * the snapshot; the returned copy prevents any bad thread-based issues.
	 */
	public Set<InetAddress> getMachineAddresses();
}
