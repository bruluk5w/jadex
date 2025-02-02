package jadex.platform.service.transport;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import jadex.base.Starter;
import jadex.bridge.ClassInfo;
import jadex.bridge.ComponentIdentifier;
import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.SFuture;
import jadex.bridge.component.IExecutionFeature;
import jadex.bridge.component.IMessageFeature;
import jadex.bridge.component.IMsgHeader;
import jadex.bridge.component.impl.IInternalMessageFeature;
import jadex.bridge.component.impl.MessageComponentFeature;
import jadex.bridge.component.impl.MsgHeader;
import jadex.bridge.component.impl.RemoteExecutionComponentFeature;
import jadex.bridge.service.IInternalService;
import jadex.bridge.service.IService;
import jadex.bridge.service.IServiceIdentifier;
import jadex.bridge.service.ProvidedServiceInfo;
import jadex.bridge.service.ServiceScope;
import jadex.bridge.service.annotation.OnEnd;
import jadex.bridge.service.annotation.OnInit;
import jadex.bridge.service.annotation.Reference;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.component.BasicServiceInvocationHandler;
import jadex.bridge.service.component.IRequiredServicesFeature;
import jadex.bridge.service.types.address.ITransportAddressService;
import jadex.bridge.service.types.address.TransportAddress;
import jadex.bridge.service.types.cms.SComponentManagementService;
import jadex.bridge.service.types.memstat.IMemstatService;
import jadex.bridge.service.types.security.ISecurityInfo;
import jadex.bridge.service.types.security.ISecurityService;
import jadex.bridge.service.types.serialization.ISerializationServices;
import jadex.bridge.service.types.transport.ITransportInfoService;
import jadex.bridge.service.types.transport.ITransportService;
import jadex.bridge.service.types.transport.PlatformData;
import jadex.commons.Boolean3;
import jadex.commons.ICommand;
import jadex.commons.MethodInfo;
import jadex.commons.SUtil;
import jadex.commons.Tuple2;
import jadex.commons.Tuple3;
import jadex.commons.collection.BiHashMap;
import jadex.commons.collection.IRwMap;
import jadex.commons.collection.LeaseTimeMap;
import jadex.commons.collection.MultiCollection;
import jadex.commons.collection.RwMapWrapper;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.future.IIntermediateFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.future.ISubscriptionIntermediateFuture;
import jadex.commons.future.ITerminableFuture;
import jadex.commons.future.IntermediateFuture;
import jadex.commons.future.SubscriptionIntermediateFuture;
import jadex.commons.future.TerminableFuture;
import jadex.commons.future.TerminationCommand;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.AgentArgument;
import jadex.micro.annotation.AgentFeature;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.OnService;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;

@Agent
@ProvidedServices({
	@ProvidedService(scope=ServiceScope.PLATFORM, type=ITransportService.class, implementation=@Implementation(expression="$pojoagent", proxytype=Implementation.PROXYTYPE_RAW)),
	@ProvidedService(scope=ServiceScope.PLATFORM, type=ITransportInfoService.class, implementation=@Implementation(expression="$pojoagent")),//, proxytype=Implementation.PROXYTYPE_RAW)),
//	@ProvidedService(scope=ServiceScope.PLATFORM, type=IMemstatService.class, implementation=@Implementation(expression="$pojoagent", proxytype=Implementation.PROXYTYPE_RAW))
})
public class AbstractTransportAgent<Con> implements ITransportService, ITransportHandler<Con>, ITransportInfoService, IMemstatService, IInternalService
{
	/** The port, the transport should listen to (&lt;0: don't listen, 0: choose random port, >0: use given port). */
	@AgentArgument
	protected int	port	= 0;
	
	/** Maximum size a message is allowed to have (including header). */
	@AgentArgument
	protected int maxmsgsize = 100*1024*1024;
	
	/** The agent. */
	@Agent
	protected IInternalAccess agent;
	
	/** The transport implementation. */
	protected ITransport<Con> impl;
	
	/** The established connections, multithreaded. */
	protected IRwMap<IComponentIdentifier, Con> establishedconnections;
	
	/** The established connections, reverse lookup, multithreaded. */
	protected IRwMap<Con, IComponentIdentifier> restablishedconnections;
	
	/** Commands waiting for a connection to be established, subject to timeouts. */
	protected MultiCollection<IComponentIdentifier, Tuple3<ICommand<Con>, Long, TerminableFuture<Integer>>> commandswaitingforcons = new MultiCollection<>();
	
	/** Connections to be established, subject to timeouts. */
	protected LeaseTimeMap<Con, IComponentIdentifier> handshakingconnections;
	
	// ------------ Cached valued. -----------
	
	/** The local platform ID. */
	protected IComponentIdentifier platformid;
	
	/** Security service. */
	@OnService(query=Boolean3.TRUE, required=Boolean3.TRUE)
	protected ISecurityService secser;
	
	/** Transport address service. */
	@OnService(query=Boolean3.TRUE, required=Boolean3.TRUE)
	protected ITransportAddressService tas;
	
	/** Serialization services. */
	protected ISerializationServices serser;
	
	/** The execution feature. */
	@AgentFeature
	protected IExecutionFeature execfeat;
	
	/** Listeners from transport info service. */
	protected Collection<SubscriptionIntermediateFuture<PlatformData>> infosubscribers;
	
	/** Cleanup interval. */
	protected long cleanupinterval;
	
	/** Next cleanup interval. */
	protected AtomicLong nextcleanup;
	
	/**
	 *  Initialized agent.
	 *  @return Null, when done.
	 */
	//@AgentCreated
	@OnInit
	public IFuture<Void> start()
	{
		// Convert required tas to provided (for speed?) and required secser to raw (TODO: why is RequiredService.proxytype deprecated?
		assert tas!=null;
		assert secser!=null;
		tas	= (ITransportAddressService) ((BasicServiceInvocationHandler)Proxy.getInvocationHandler(tas)).getDomainService();
		secser	= (ISecurityService) ((BasicServiceInvocationHandler)Proxy.getInvocationHandler(secser)).getDomainService();
//		assert !Proxy.isProxyClass(tas.getClass());	// Still provided service proxy as tas is not thread safe
		assert !Proxy.isProxyClass(secser.getClass());
		
		platformid = agent.getId().getRoot();
		cleanupinterval = Starter.getDefaultTimeout(platformid);
		cleanupinterval = cleanupinterval > 0 ? cleanupinterval : 30000;
		nextcleanup = new AtomicLong(System.currentTimeMillis() + cleanupinterval);
		
		handshakingconnections = new LeaseTimeMap<>(Starter.getDefaultTimeout(platformid), new ICommand<Tuple2<Entry<Con, IComponentIdentifier>, Long>>()
		{
			public void execute(Tuple2<Entry<Con, IComponentIdentifier>, Long> entry)
			{
				impl.closeConnection(entry.getFirstEntity().getKey());
			}
		}, false, false, true);
		
		BiHashMap<IComponentIdentifier, Con> econsbimap = new BiHashMap<>();
		RwMapWrapper<IComponentIdentifier, Con> wrappedmap = new RwMapWrapper<>(econsbimap);
		establishedconnections = wrappedmap;
		restablishedconnections = new RwMapWrapper<>(econsbimap.flip(), wrappedmap.getLock());
		
		serser =  (ISerializationServices)Starter.getPlatformValue(platformid, Starter.DATA_SERIALIZATIONSERVICES);
		
		infosubscribers = new ArrayList<SubscriptionIntermediateFuture<PlatformData>>();

		impl = createTransportImpl();
		impl.init(this);
		
		// Set up server, if port given.
		// If port==0 -> any free port
		Future<Void> openportret = new Future<>();
		if(port >= 0)
		{
			impl.openPort(port).addResultListener(new ExceptionDelegationResultListener<Integer, Void>(openportret)
			{
				public void customResultAvailable(Integer iport) throws Exception
				{
					try
					{
						// Announce connection addresses.
						InetAddress[] addresses = SUtil.getNetworkAddresses();
						IComponentIdentifier platformid = agent.getId().getRoot();
						List<TransportAddress> saddresses = new ArrayList<TransportAddress>();
						for(int i = 0; i < addresses.length; i++)
						{
							String addrstr = null;
							if(addresses[i] instanceof Inet6Address)
							{
								addrstr = "[" + addresses[i].getHostAddress() + "]:" + iport;
							}
							else // if (address instanceof Inet4Address)
							{
								addrstr = addresses[i].getHostAddress() + ":" + iport;
							}
							saddresses.add(new TransportAddress(platformid, impl.getProtocolName(), addrstr));
						}
						
						agent.getLogger().info("Platform "+agent.getId().getPlatformName()+" listening to port " + iport + " for " + impl.getProtocolName() + " transport.");
	
						//ITransportAddressService tas = ((IInternalRequiredServicesFeature)agent.getFeature(IRequiredServicesFeature.class)).getRawService(ITransportAddressService.class);
						
	//					System.out.println("Transport addresses: "+agent+", "+saddresses);
						tas.addLocalAddresses(saddresses).addResultListener(new DelegationResultListener<Void>(openportret));
					}
					catch(Exception e)
					{
						openportret.setException(e);
					}
				}
			});
		}
		else
		{
			openportret.setResult(null);
		}
		
		return IFuture.DONE;
	}
	
	//@AgentKilled
	@OnEnd
	public IFuture<Void> shutdown()
	{
		impl.shutdown();
		
		List<Con> cons = new ArrayList<>();
		try
		{
			establishedconnections.getWriteLock().lock();
			for(Con con : establishedconnections.values())
			{
				if(con != null)
					cons.add(con);
			}
			establishedconnections.clear();
			cons.addAll(handshakingconnections.keySet());
			handshakingconnections.clear();
			commandswaitingforcons.clear();
		}
		finally
		{
			establishedconnections.getWriteLock().unlock();
		}
		
		for(Con con : cons)
			impl.closeConnection(con);
		
		return IFuture.DONE;
	}
	
	/**
	 *  Send a message.
	 *  
	 *  @param header Message header.
	 *  @param bheader Message header already encoded and encrypted for sending.
	 *  @param body Message body.
	 *  @return Transport priority, when sent. Failure does not need to be returned as message feature uses its own timeouts.
	 *  	Future is terminated by message feature, when another transport has sent the message.
	 */
	public ITerminableFuture<Integer> sendMessage(IMsgHeader header, byte[] bheader, byte[] body)
	{
		cleanup();
		final TerminableFuture<Integer> ret = new TerminableFuture<>();
		Con con = establishedconnections.get(header.getReceiver().getRoot());
		if(con != null)
		{
			impl.sendMessage(con, bheader, body).addResultListener(new DelegationResultListener<>(ret));
		}
		else
		{
			agent.scheduleStep(new IComponentStep<Void>()
			{
				public IFuture<Void> execute(IInternalAccess ia)
				{
					IComponentIdentifier receiverpf = header.getReceiver().getRoot();
					Con con = establishedconnections.get(receiverpf);
					if (con != null && !ret.isDone())
					{
						impl.sendMessage(con, bheader, body).addResultListener(new DelegationResultListener<>(ret));
					}
					else
					{
						boolean createcon = !commandswaitingforcons.containsKey(receiverpf);
						
						boolean[] canceled = new boolean[1];
						final long timeout = System.currentTimeMillis() + cleanupinterval;
						Tuple3<ICommand<Con>, Long, TerminableFuture<Integer>> cmd = new Tuple3<ICommand<Con>, Long, TerminableFuture<Integer>>(new ICommand<Con>()
						{
							public void execute(Con con)
							{
								if (!canceled[0])
								{
									impl.sendMessage(con, bheader, body).addResultListener(new DelegationResultListener<>(ret));
								}
							}
						}, timeout, ret);
						commandswaitingforcons.add(receiverpf, cmd);
						ret.setTerminationCommand(new TerminationCommand()
						{
							public void terminated(Exception reason)
							{
								canceled[0] = true;
							}
						});
						
						if (createcon)
						{
							createNewConnections(receiverpf).catchEx(e ->
							{
								Collection<Tuple3<ICommand<Con>, Long, TerminableFuture<Integer>>> cmds = commandswaitingforcons.remove(receiverpf);
								for (Tuple3<ICommand<Con>, Long, TerminableFuture<Integer>> c : SUtil.notNull(cmds))
									c.getThirdEntity().setExceptionIfUndone(e);
							});
						}
					}
					return IFuture.DONE;
				}
			});
		}
		return ret;
	}
	
	/**
	 *  Deliver a received message.
	 *  @param con	The connection.
	 *  @param header	The message header.
	 *  @param body	The message body.
	 */
	public void	messageReceived(Con con, byte[] header, byte[] body)
	{
		cleanup();
		IComponentIdentifier remotepf = restablishedconnections.get(con);
		if(remotepf != null)
		{
			deliverRemoteMessage(agent, secser, serser, remotepf, header, body);
		}
		else
		{
			execfeat.scheduleStep(new IComponentStep<Void>()
			{
				public IFuture<Void> execute(IInternalAccess ia)
				{
					IComponentIdentifier remotepf = restablishedconnections.get(con);
					if (remotepf != null)
					{
						deliverRemoteMessage(agent, secser, serser, remotepf, header, body);
					}
					else
					{
						if (handshakingconnections.containsKey(con) &&
							header != null && header.length == 0 &&
							body != null)
						{
							remotepf = handshakingconnections.get(con);
							if (remotepf == null && body.length > 0)
							{
								// server receiving name
								remotepf = new ComponentIdentifier((new String(body, SUtil.UTF8)).intern());
								boolean notconnected = false;
								if (canDecide(remotepf))
								{
									establishedconnections.getWriteLock().lock();
									try
									{
										notconnected = !establishedconnections.containsKey(remotepf);
										if (notconnected)
											establishedconnections.put(remotepf, null);
									}
									finally
									{
										establishedconnections.getWriteLock().unlock();
									}
								}
								else
								{
									notconnected = true;
								}
								
								if (notconnected)
								{
									final IComponentIdentifier fremotepf = remotepf;
									impl.sendMessage(con, new byte[0], platformid.toString().getBytes(SUtil.UTF8))
										.addResultListener(execfeat.createResultListener(new IResultListener<Integer>()
										{
											public void resultAvailable(Integer result)
											{
												establishConnection(fremotepf, con);
											}
											
											public void exceptionOccurred(Exception exception)
											{
												if (canDecide(fremotepf))
												{
													establishedconnections.remove(fremotepf);
													impl.closeConnection(con);
												}
											}
										}));
								}
								else
								{
									handshakingconnections.remove(con);
									impl.closeConnection(con);
								}
							}
							else if (remotepf != null && body.length > 0)
							{
								// client receiving name
								IComponentIdentifier rcvdpf = new ComponentIdentifier(new String(body, SUtil.UTF8));
								if (rcvdpf.equals(remotepf))
								{
									establishConnection(remotepf, con);
								}
								else
								{
									agent.getLogger().warning("Tried to connect to " + remotepf + ", but answered " + rcvdpf + ".");
									impl.closeConnection(con);
								}
							}
							else
							{
								// What is this? Go away, we don't like you.
								agent.getLogger().warning("Closing connection due to message violating protocol: " + con + " " + body);
								impl.closeConnection(con);
							}
						}
						else
						{
							// What is this? Go away, we don't like you.
							agent.getLogger().warning("Closing connection due to message violating protocol, discovered by sanity check: " + con + " " + body);
							impl.closeConnection(con);
						}
					}
					
					return IFuture.DONE;
				}
			});
		}
	}
	
	/**
	 *  Called when a server connection is established.
	 *  @param con	The connection.
	 */
	public void	connectionEstablished(final Con con)
	{
		execfeat.scheduleStep(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				handshakingconnections.put(con, null);
				return IFuture.DONE;
			}
		});
	}
	
	/**
	 *  Called when a connection is closed.
	 *  @param con	The connection.
	 *  @param e	The exception, if any.
	 */
	public void	connectionClosed(Con con, Exception e)
	{
		final IComponentIdentifier remotepf = restablishedconnections.remove(con);
		agent.scheduleStep(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				if (remotepf == null)
				{
					handshakingconnections.remove(con);
				}
				else
				{
					PlatformData data = new PlatformData(remotepf, impl.getProtocolName(), false);
					for (SubscriptionIntermediateFuture<PlatformData> sub : infosubscribers)
						sub.addIntermediateResult(data);
				}
				
				return IFuture.DONE;
			}
		});
//		System.out.println("CON REMOVED: " + con + " " + e);
	}
	
	/**
	 *  Get the internal access.
	 */
	public IInternalAccess getAccess()
	{
		return agent;
	}
	
	/**
	 * Get the transport implementation
	 */
	public ITransport<Con> createTransportImpl()
	{
		return null;
	}
	
	/**
	 *  Get events about established connections.
	 *  @return Events for connections specified by
	 *  	1: platform id,
	 *  	2: protocol name,
	 *  	3: ready flag (false=connecting, true=connected, null=disconnected).
	 */
	public ISubscriptionIntermediateFuture<PlatformData> subscribeToConnections()
	{
		final SubscriptionIntermediateFuture<PlatformData>	ret	= new SubscriptionIntermediateFuture<PlatformData>(null, true);
		SFuture.avoidCallTimeouts(ret, agent);
		
		agent.scheduleStep(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				ret.setTerminationCommand(new TerminationCommand()
				{
					public void terminated(Exception reason)
					{
						agent.scheduleStep(new IComponentStep<Void>()
						{
							public IFuture<Void> execute(IInternalAccess ia)
							{
								infosubscribers.remove(ret);
								return IFuture.DONE;
							}
						});
					}
				});
				
				infosubscribers.add(ret);
				
				// Add initial data
				List<PlatformData> constat = collectConnectionStatus();
				for (PlatformData data : constat)
				{
					//System.out.println("post connection to subscriber: "+data+", "+ret);
					ret.addIntermediateResult(data);
				}
				
				return IFuture.DONE;
			}
		});
		return ret;
	}
	
	/**
	 *  Get the established connections.
	 *  @return A list of connections specified by
	 *  	1: platform id,
	 *  	2: protocol name,
	 *  	3: ready flag (false=connecting, true=connected).
	 */
	public IIntermediateFuture<PlatformData> getConnections()
	{
		final IntermediateFuture<PlatformData> ret = new IntermediateFuture<>();
		agent.scheduleStep(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				ret.setResult(collectConnectionStatus());
				return IFuture.DONE;
			}
		});
		return ret;
	}
	
	/**
	 *  Get info about stored data like connections and listeners.
	 */
	// For detecting/debugging memory leaks
	public IFuture<Map<String, Object>>	getMemInfo()
	{
		Future<Map<String, Object>> ret = new Future<>();
		agent.scheduleStep(new IComponentStep<Void>()
		{
			public IFuture<Void> execute(IInternalAccess ia)
			{
				Map<String, Object>	meminf	= new LinkedHashMap<String, Object>();
				meminf.put("transport", impl.getProtocolName());
				meminf.put("subscribercnt", infosubscribers!=null ? infosubscribers.size() : 0);
				
				Map<IComponentIdentifier, Con> cons = null;
				Map<Con, IComponentIdentifier> hscons = null;
				try
				{
					establishedconnections.getReadLock().lock();
					cons = new HashMap<>(establishedconnections);
					hscons = new HashMap<>(handshakingconnections);
				}
				finally
				{
					establishedconnections.getReadLock().unlock();
				}
				
				meminf.put("cons", cons);
				meminf.put("hscons", hscons);
				
				ret.setResult(meminf);
				return IFuture.DONE;
			}
		});
		return ret;
	}
	
	/**
	 *  Creates new connections to a remote platform.
	 *  @param remotepf The remote platform ID.
	 */
	protected IFuture<Void> createNewConnections(IComponentIdentifier remotepf)
	{
		Future<Void>	ret	= new Future<Void>();
		try
		{
			assert execfeat.isComponentThread();
			ITransportAddressService tas = agent.getFeature(IRequiredServicesFeature.class).getLocalService(ITransportAddressService.class);
			tas.resolveAddresses(remotepf, impl.getProtocolName()).addResultListener(new ExceptionDelegationResultListener<List<TransportAddress>, Void>(ret)
			{
				public void customResultAvailable(List<TransportAddress> result)
				{
					if (result != null && result.size() > 0)
					{
						for (TransportAddress address : result)
						{
							impl.createConnection(address.getAddress(), remotepf).addResultListener(
								new IResultListener<Con>()
							{
								public void resultAvailable(final Con con)
								{
									if (canDecide(remotepf))
									{
										boolean notconnected = false;
										try
										{
											establishedconnections.getWriteLock().lock();
											notconnected = !establishedconnections.containsKey(remotepf);
											if (notconnected)
												establishedconnections.put(remotepf, null);
										}
										finally
										{
											establishedconnections.getWriteLock().unlock();
										}
										
										if (notconnected)
										{
											impl.sendMessage(con, new byte[0], platformid.toString().getBytes(SUtil.UTF8))
												.addResultListener(execfeat.createResultListener(new IResultListener<Integer>()
												{
													public void resultAvailable(Integer result)
													{
														handshakingconnections.put(con, remotepf);
													}
													
													public void exceptionOccurred(Exception exception)
													{
														establishedconnections.remove(remotepf);
														impl.closeConnection(con);
													}
												}));
										}
									}
									else
									{
										impl.sendMessage(con, new byte[0], platformid.toString().getBytes(SUtil.UTF8))
											.addResultListener(execfeat.createResultListener(new IResultListener<Integer>()
										{
											public void resultAvailable(Integer result)
											{
												handshakingconnections.put(con, remotepf);
											}
											
											public void exceptionOccurred(Exception exception)
											{
												impl.closeConnection(con);
		//										exception.printStackTrace();
											}
										}));
									}
								}
								public void exceptionOccurred(Exception exception)
								{
									// TODO: if finally failed, propagate exception for command cleanup?
	//								exception.printStackTrace();
								}
							});
						}
						
						PlatformData data = new PlatformData(remotepf, impl.getProtocolName(), false);
						for (SubscriptionIntermediateFuture<PlatformData> sub : infosubscribers)
							sub.addIntermediateResult(data);
					}
					else
					{
						ret.setException(new RuntimeException("No transport addresses for: "+remotepf));
					}
				}
			});
		}
		catch(Exception e)
		{
			ret.setException(e);
		}
		return ret;
	}
	
	/**
	 *  Establish a connection after handshake.
	 *  
	 *  @param remotepf The remote platform.
	 *  @param con The connection.
	 */
	protected void establishConnection(IComponentIdentifier remotepf, Con con)
	{
		assert execfeat.isComponentThread();
//		System.out.println("HANDSHAKE DONE FOR " + platformid + " -> " + remotepf + " " + con + " " + canDecide(remotepf));
		
		Collection<Tuple3<ICommand<Con>, Long, TerminableFuture<Integer>>> waitingcmds = commandswaitingforcons.remove(remotepf);
		for (Tuple3<ICommand<Con>, Long, TerminableFuture<Integer>> cmdtup : SUtil.notNull(waitingcmds))
			cmdtup.getFirstEntity().execute(con);
		
		handshakingconnections.remove(con);
		establishedconnections.put(remotepf, con);
		
		PlatformData data = new PlatformData(remotepf, impl.getProtocolName(), true);
		for (SubscriptionIntermediateFuture<PlatformData> sub : infosubscribers)
			sub.addIntermediateResult(data);
	}
	
	/**
	 *  Perform housekeeping operations if necessary.
	 */
	protected void cleanup()
	{
		long val = nextcleanup.get();
		long cur = System.currentTimeMillis();
		if (val < cur)
		{
			if (nextcleanup.compareAndSet(val, cur + cleanupinterval))
			{
				agent.scheduleStep(new IComponentStep<Void>()
				{
					public IFuture<Void> execute(IInternalAccess ia)
					{
//						System.out.println(agent + " running CLEANUP " + platformid);
						handshakingconnections.checkStale();
						
						Set<IComponentIdentifier> keyset = commandswaitingforcons.keySet();
						IComponentIdentifier[] keys = keyset.toArray(new IComponentIdentifier[keyset.size()]);
						for (IComponentIdentifier key : keys)
						{
//						for (Map.Entry<IComponentIdentifier, Collection<Tuple2<ICommand<Con>, Long>>> entry : commandswaitingforcons.entrySet())
//						{
							List<Tuple3<ICommand<Con>, Long, TerminableFuture<Integer>>> coll = new ArrayList<>(commandswaitingforcons.get(key));
							for (Tuple3<ICommand<Con>, Long, TerminableFuture<Integer>> cmd : coll)
							{
								if (cur < cmd.getSecondEntity())
									commandswaitingforcons.removeObject(key, cmd);
							}
						}
						return IFuture.DONE;
					}
				});
			}
		}
	}
	
	/**
	 *  Check if the local platform can decide which connection
	 *  to establish.
	 *  
	 *  @param remotepf Remote platform ID.
	 *  @return True, if the local platform can decide.
	 */
	protected boolean canDecide(IComponentIdentifier remotepf)
	{
		return platformid.toString().compareTo(remotepf.toString()) > 0;
	}
	
	/**
	 *  Collects the current connection status.
	 *  @return The current connection status.
	 */
	protected List<PlatformData> collectConnectionStatus()
	{
//		assert execfeat.isComponentThread();
		
		List<PlatformData> ret = new ArrayList<>();
		
		Map<IComponentIdentifier, Con> cons = null;
//		Map<Con, IComponentIdentifier> hscons = null;
		try
		{
			establishedconnections.getReadLock().lock();
			cons = new HashMap<>(establishedconnections);
//			hscons = new HashMap<>(handshakingconnections);
		}
		finally
		{
			establishedconnections.getReadLock().unlock();
		}
		
//		for(Map.Entry<Con, IComponentIdentifier> entry : hscons.entrySet())
//		{ 
//			if (entry.getValue() != null)
//			{
//				PlatformData data = new PlatformData(entry.getValue(), impl.getProtocolName(), false);
//				ret.add(data);
//			}
//		}
		
		for(Map.Entry<IComponentIdentifier, Con> entry : cons.entrySet())
		{ 
			PlatformData data = new PlatformData(entry.getKey(), impl.getProtocolName(), true);
			ret.add(data);
		}
		
		return ret;
	}
	
	//-------- IInternalService interface -------- 
	
	private IServiceIdentifier sid;
	
	/**
	 *  Get the service identifier.
	 *  @return The service identifier.
	 */
	public IServiceIdentifier getServiceId()
	{
		return sid;
	}
	
	/**
	 *  Get the service type info.
	 *  @return The service type info, if any.
	 */
	// used for checking @provided security settings at runtime
	public ProvidedServiceInfo getServiceInfo()
	{
		return null;
	}
	
	/**
	 *  Test if the service is valid.
	 *  @return True, if service can be used.
	 */
	public IFuture<Boolean> isValid()
	{
		return new Future<Boolean>(true);
	}
		
	/**
	 *  Get the map of properties (considered as constant).
	 *  @return The service property map (if any).
	 */
	public Map<String, Object> getPropertyMap()
	{
		return new HashMap<String, Object>();
	}
	
	/**
	 *  Start the service.
	 *  @return A future that is done when the service has completed starting.  
	 */
	public IFuture<Void>	startService() {return IFuture.DONE;}
	
	/**
	 *  Shutdown the service.
	 *  @return A future that is done when the service has completed its shutdown.  
	 */
	public IFuture<Void>	shutdownService() {return IFuture.DONE;}
	
	/**
	 *  Sets the access for the component.
	 *  @param access Component access.
	 */
	public IFuture<Void> setComponentAccess(@Reference IInternalAccess access) {return IFuture.DONE;}	

	/**
	 *  Set the service identifier.
	 */
	public void setServiceIdentifier(IServiceIdentifier sid)
	{
		this.sid = sid;
	}
	
	/**
	 *  Invoke a method reflectively.
	 *  @param methodname The method name.
	 *  @param argtypes The argument types (can be null if method exists only once).
	 *  @param args The arguments.
	 *  @return The result.
	 */
	public IFuture<Object> invokeMethod(String methodname, ClassInfo[] argtypes, Object[] args, ClassInfo rettype)
	{
		return new Future<Object>(new UnsupportedOperationException());
	}
	
	/**
	 *  Get reflective info about the service methods, args, return types.
	 *  @return The method infos.
	 */
	public IFuture<MethodInfo[]> getMethodInfos()
	{
		Class<?> iface = sid.getServiceType().getType(agent.getClassLoader());
		
		Set<Method> ms = new HashSet<>();
		
		Set<Class<?>> todo = new HashSet<>();
		todo.add(iface);
		todo.add(IService.class);
		while(todo.size()>0)
		{
			Class<?> cur = todo.iterator().next();
			todo.remove(cur);
			ms.addAll(SUtil.arrayToList(cur.getMethods()));
			
			cur = cur.getSuperclass();
			while(cur!=null && cur.getAnnotation(Service.class)==null)
				cur = cur.getSuperclass();
			
			if(cur!=null)
				todo.add(cur);
		}
		
		MethodInfo[] ret = new MethodInfo[ms.size()];
		Iterator<Method> it = ms.iterator();
		for(int i=0; i<ms.size(); i++)
		{
			MethodInfo mi = new MethodInfo(it.next());
			ret[i] = mi;
		}
		
		return new Future<MethodInfo[]>(ret);
	}
	
	/**
	 *  Delivers a remote message to a component.
	 * 
	 *  @param agent Agent performing the delivery.
	 *  @param secser The security service.
	 *  @param cms The component management service.
	 *  @param serser The serialization services.
	 *  @param source Source ID of the message.
	 *  @param header The header of the message.
	 *  @param body The body of the message.
	 */
	public static final void deliverRemoteMessage(final IInternalAccess agent, ISecurityService secser, final ISerializationServices serser, final IComponentIdentifier source, byte[] header, final byte[] body)
	{
		final Logger logger = agent.getLogger();
		// First decrypt.
		secser.decryptAndAuth(source, header).addResultListener(new IResultListener<Tuple2<ISecurityInfo, byte[]>>()
		{
			@Override
			public void resultAvailable(Tuple2<ISecurityInfo, byte[]> tup)
			{
				if(tup.getSecondEntity() != null)
				{
					// Then decode header and deliver to receiver agent.
					final IMsgHeader header = (IMsgHeader)serser.decode(null, agent, tup.getSecondEntity());
					final IComponentIdentifier rec = (IComponentIdentifier)header.getProperty(IMsgHeader.RECEIVER);

//					try
//					{
//						if(rec.getLocalName().equals("rt"))
//							System.out.println("rec msg: "+rec+", "+header);
//					}
//					catch(Throwable t)
//					{
//						t.printStackTrace();
//					}
					
					// Cannot use agent/cms.getExternalAccess(cid) because when remote call
					// is in init the call will be delayed after init has finished (deadlock)
					SComponentManagementService.scheduleStep(rec, new IComponentStep<Void>()
					{
						@Override
						public IFuture<Void> execute(IInternalAccess ia)
						{
//							try
//							{
//								if(rec.getLocalName().equals("rt"))
//									System.out.println("rec msg scheduled: "+rec+", "+header);
//							}
//							catch(Throwable t)
//							{
//								t.printStackTrace();
//							}
							IMessageFeature mf = ia.getFeature0(IMessageFeature.class);
							if(mf instanceof IInternalMessageFeature)
							{
								((IInternalMessageFeature)mf).messageArrived(header, body);
							}
							return IFuture.DONE;
						}
					}).addResultListener(new IResultListener<Void>()
					{
						@Override
						public void resultAvailable(Void result)
						{
							// NOP
						}

						@Override
						public void exceptionOccurred(Exception exception)
						{
//							System.out.println("Could not deliver message from platform " + source + " to " + rec + ": " + exception);
							logger.warning("Could not deliver message from platform " + source + " to " + rec + ": " + exception);
							
							// For undeliverable conversation messages -> send error reply (only for non-error messages). 
							if((header.getProperty(IMsgHeader.CONVERSATION_ID)!=null || header.getProperty(RemoteExecutionComponentFeature.RX_ID)!=null)
								&& header.getProperty(MessageComponentFeature.EXCEPTION)==null)
							{
								agent.getExternalAccess().scheduleStep(new IComponentStep<Void>()
								{
									@Override
									public IFuture<Void> execute(IInternalAccess ia)
									{
										Map<String, Object>	addheaderfields	= ((MsgHeader)header).getProperties();
										addheaderfields.put(MessageComponentFeature.EXCEPTION, exception);
										ia.getFeature(IMessageFeature.class)
											.sendMessage(null, addheaderfields, (IComponentIdentifier)header.getProperty(IMsgHeader.SENDER))
											.addResultListener(new IResultListener<Void>()
											{
												@Override
												public void exceptionOccurred(Exception exception)
												{
													logger.warning("Could send error message to " + header.getProperty(IMsgHeader.SENDER) + ": " + exception);
												}
												
												@Override
												public void resultAvailable(Void result)
												{
													// OK -> ignore
//													System.out.println("Sent error message: "+header.getProperty(IMsgHeader.SENDER) + ", "+exception);
												}
											});
										return IFuture.DONE;
									}
								});
							}
						}
					});
				}
			}

			@Override
			public void exceptionOccurred(Exception exception)
			{
				System.out.println("Could not deliver message from platform " + source + ": " + exception);
				logger.warning("Could not deliver message from platform " + source + ": " + exception);
			}
		});
	}
}
