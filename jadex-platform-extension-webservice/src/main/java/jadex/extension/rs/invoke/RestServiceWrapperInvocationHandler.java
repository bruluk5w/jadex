package jadex.extension.rs.invoke;

import jadex.bridge.IComponentIdentifier;
import jadex.bridge.IComponentStep;
import jadex.bridge.IExternalAccess;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.search.SServiceProvider;
import jadex.bridge.service.types.cms.CreationInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.commons.SUtil;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.ExceptionDelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.extension.rs.invoke.annotation.ParamMapper;
import jadex.extension.rs.invoke.annotation.ParamMappers;
import jadex.extension.rs.invoke.annotation.ParametersInURL;
import jadex.extension.rs.publish.DefaultRestMethodGenerator;
import jadex.extension.rs.publish.JadexXMLBodyReader;
import jadex.extension.rs.publish.JadexXMLBodyWriter;
import jadex.extension.rs.publish.annotation.ParameterMapper;
import jadex.extension.rs.publish.annotation.ResultMapper;
import jadex.extension.rs.publish.mapper.IValueMapper;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.Value;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.RequestBuilder;
import com.sun.jersey.api.client.UniformInterface;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.representation.Form;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 *  Create a new web service wrapper invocation handler.
 *  
 *  Creates an 'rest service invocation agent' for each method invocation.
 *  Lets this invocation agent call the web service by using the mapping
 *  data to determine details about the service call.
 *  The invocation agent returns the result and terminates itself after the call.
 *  
 *  todo: 
 *  - path parameter support
 *  
 */
@Service // Used here only to pass allow proxy to be used as service (check is delegated to handler)
class RestServiceWrapperInvocationHandler implements InvocationHandler
{
	public static String[] defaultimports = new String[]{"jadex.extension.rs.invoke.*", 
		"jadex.extension.rs.invoke.annotation.*", "jadex.extension.rs.invoke.mapper.*"};
	
	//-------- attributes --------
	
	/** The agent. */
	protected IInternalAccess agent;
	
	/** The annotated service interface. */
	protected Class<?> iface;
	
	//-------- constructors --------
	
	/**
	 *  Create a new service wrapper invocation handler.
	 *  @param agent The internal access of the agent.
	 */
	public RestServiceWrapperInvocationHandler(IInternalAccess agent, Class<?> iface)
	{
		if(agent==null)
			throw new IllegalArgumentException("Agent must not null.");
		if(iface==null)
			throw new IllegalArgumentException("Rest interface must not be null.");
		this.agent = agent;
		this.iface = iface;
	}
	
	//-------- methods --------
	
	/**
	 *  Called when a wrapper method is invoked.
	 *  Uses the cms to create a new invocation agent and lets this
	 *  agent call the web service. The result is transferred back
	 *  into the result future of the caller.
	 */
	public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable
	{
		final Future<Object> ret = new Future<Object>();
			
//		IFuture<IComponentManagementService> fut = agent.getServiceContainer().getRequiredService("cms");
		IFuture<IComponentManagementService> fut = SServiceProvider.getService(agent.getServiceContainer(), IComponentManagementService.class, RequiredServiceInfo.SCOPE_PLATFORM);
		fut.addResultListener(agent.createResultListener(new ExceptionDelegationResultListener<IComponentManagementService, Object>(ret)
		{
			public void customResultAvailable(final IComponentManagementService cms)
			{
				CreationInfo ci = new CreationInfo(agent.getComponentIdentifier());
//				cms.createComponent(null, "invocation", ci, null)
				cms.createComponent(null, "jadex/extension/rs/invoke/RestServiceInvocationAgent.class", ci, null)
					.addResultListener(agent.createResultListener(new ExceptionDelegationResultListener<IComponentIdentifier, Object>(ret)
				{
					public void customResultAvailable(IComponentIdentifier cid) 
					{
						cms.getExternalAccess(cid).addResultListener(agent.createResultListener(new ExceptionDelegationResultListener<IExternalAccess, Object>(ret)
						{
							public void customResultAvailable(IExternalAccess exta) 
							{
								exta.scheduleStep(new IComponentStep<Object>()
								{
									public IFuture<Object> execute(IInternalAccess ia)
									{
										Future<Object> re = new Future<Object>();
										
										try
										{
											String baseuri = iface.getAnnotation(Path.class).value();
											Method m = iface.getMethod(method.getName(), method.getParameterTypes());
											Class<?> resttype = DefaultRestMethodGenerator.getDeclaredRestType(m);
											String methodname = m.getAnnotation(Path.class).value();
											
											String[] consumes = SUtil.EMPTY_STRING_ARRAY;
											if(m.isAnnotationPresent(Consumes.class))
												consumes = m.getAnnotation(Consumes.class).value();
											
											String[] produces = SUtil.EMPTY_STRING_ARRAY;
											if(m.isAnnotationPresent(Produces.class))
												produces = m.getAnnotation(Produces.class).value();
											
											// Test if general parameter mapper is given
											Value pmapper = null;
											if(m.isAnnotationPresent(ParameterMapper.class))
												pmapper = m.getAnnotation(ParameterMapper.class).value();
											
											// Otherwise test if parameter specific mappers are given
											List<Object[]> pmappers = null;
											if(pmapper==null)
											{
												pmappers = new ArrayList<Object[]>();
												Annotation[][] anoss = m.getParameterAnnotations();
												for(int i=0; i<anoss.length; i++)
												{
													Annotation[] anos = anoss[i]; 
													for(int j=0; j<anos.length; j++)
													{
														if(anos[j] instanceof ParamMapper)
														{
															pmappers.add(new Object[]{anos[j], new int[]{i}});
														}
													}
												}
												if(m.isAnnotationPresent(ParamMapper.class))
												{
													ParamMapper qpm = m.getAnnotation(ParamMapper.class);
													pmappers.add(new Object[]{qpm, new int[]{-1}});
												}
												if(m.isAnnotationPresent(ParamMappers.class))
												{
													ParamMappers qpms = m.getAnnotation(ParamMappers.class);
													ParamMapper[] mps = qpms.value();
													for(int i=0; i<mps.length; i++)
													{
														pmappers.add(new Object[]{mps[i], new int[]{-1}});
													}
												}
											}
											
											// Test if a result mapper is given
											Value rmapper = null;
											if(m.isAnnotationPresent(ResultMapper.class))
												rmapper = m.getAnnotation(ResultMapper.class).value();
											
											ClientConfig cc = new DefaultClientConfig();
											cc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
											cc.getClasses().add(JadexXMLBodyWriter.class);
											
											Object targetparams = args;
											if(pmapper!=null)
											{
												IValueMapper pm = (IValueMapper)jadex.extension.rs.publish.Value.evaluate(pmapper, defaultimports);
												targetparams = pm.convertValue(args);
											}
											else if(pmappers!=null)
											{
												MultivaluedMap<String, String> mv = new MultivaluedMapImpl();
												for(int i=0; i<pmappers.size(); i++)
												{
													Object[] pm = (Object[])pmappers.get(i);
													ParamMapper qpm = (ParamMapper)pm[0];
													String name = qpm.value();
													Value val = qpm.mapper();
													int[] src = qpm.source().length>0? qpm.source(): (int[])pm[1];
													IValueMapper mapper = (IValueMapper)jadex.extension.rs.publish.Value.evaluate(val, defaultimports);
													List<Object> params = new ArrayList<Object>();
													for(int j=0; j<src.length; j++)
													{
														if(src[j]!=-1)
															params.add(args[src[j]]);
													}
													String p = (String)mapper.convertValue(params.size()==1? params.get(0): params);
													mv.add(name, p);
												}
												targetparams = mv;
											}
											
											Client client = Client.create(cc);
											WebResource wr = client.resource(baseuri); 
											wr = wr.path(methodname);
											
											ClientResponse res = null;
											if(GET.class.equals(resttype))
											{
												wr = wr.queryParams((MultivaluedMap<String, String>)targetparams);
												
												RequestBuilder rb = wr;
												for(int i=0; i<consumes.length; i++)
												{
													rb = rb.type(consumes[i]);
												}
												for(int i=0; i<produces.length; i++)
												{
													rb = rb.accept(produces[i]);
												}

												res = ((UniformInterface)rb).get(ClientResponse.class);
											}
											else 
											{	
												boolean inurl = false;
												if(m.isAnnotationPresent(ParametersInURL.class))
													inurl = m.getAnnotation(ParametersInURL.class).value();
												
												if(inurl)
													wr = wr.queryParams((MultivaluedMap<String, String>)targetparams);
												
												RequestBuilder rb = wr;
												for(int i=0; i<consumes.length; i++)
												{
													rb = rb.type(consumes[i]);
												}
												for(int i=0; i<produces.length; i++)
												{
													rb = rb.accept(produces[i]);
												}
												
												if(POST.class.equals(resttype))
													res = ((UniformInterface)rb).post(ClientResponse.class, inurl? null: targetparams);
												else if(PUT.class.equals(resttype))
													res = ((UniformInterface)rb).put(ClientResponse.class, inurl? null: targetparams);
												else if(HEAD.class.equals(resttype))
													res = ((UniformInterface)rb).put(ClientResponse.class, inurl? null: targetparams);
												else if(OPTIONS.class.equals(resttype))
													res = ((UniformInterface)rb).put(ClientResponse.class, inurl? null: targetparams);
												else if(DELETE.class.equals(resttype))
													res = ((UniformInterface)rb).put(ClientResponse.class, inurl? null: targetparams);
											}
											
											Object targetret = res;
											if(rmapper!=null)
											{
												IValueMapper rm = (IValueMapper)jadex.extension.rs.publish.Value.evaluate(rmapper, defaultimports);
												targetret = rm.convertValue(res);
											}
											
//											System.out.println("result is: "+res);
											re.setResult(targetret);
											ia.killComponent();
										}
										catch(Exception e)
										{
											e.printStackTrace();
											re.setException(e);
										}
										return re;
									}
								}).addResultListener(agent.createResultListener(new DelegationResultListener<Object>(ret)));
							}
						}));
					}
				}));
			}
		}));
			
		return ret;
	}
}