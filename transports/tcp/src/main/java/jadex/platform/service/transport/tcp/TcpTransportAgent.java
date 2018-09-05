package jadex.platform.service.transport.tcp;

import java.nio.channels.SocketChannel;

import jadex.commons.Boolean3;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Autostart;
import jadex.platform.service.transport.AbstractTransportAgent2;
import jadex.platform.service.transport.ITransport;

/**
 *  Agent that implements TCP message transport.
 */
@Agent(autostart=@Autostart(value=Boolean3.TRUE, name="tcp", predecessors="jadex.platform.service.address.TransportAddressAgent"))
public class TcpTransportAgent extends AbstractTransportAgent2<SocketChannel>
{
	//-------- abstract methods to be provided by concrete transport --------
	
	/**
	 *  Get the transport implementation
	 */
	public ITransport<SocketChannel>	createTransportImpl()
	{
		return new TcpTransport(maxmsgsize);
	}
}
