package jadex.webservice.examples.rs.hello;

import jadex.extension.rs.publish.JadexXMLBodyReader;
import jadex.xml.bean.JavaWriter;

import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.grizzly.http.server.HttpServer;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import com.sun.jersey.api.representation.Form;

@Path("/hello")
public class Hello
{
	@Context 
	public ResourceConfig rc;
	
	@Context 
	public UriInfo context;
	
    @Context
    public Request request;
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String sayPlainTextHello()
	{
//		System.out.println("hi: "+rc.getProperties().get("hallo"));
		return "Hello Jersey";
	}
	
	@GET
	@Produces(MediaType.TEXT_XML)
	public String sayXMLHello()
	{
		return JavaWriter.objectToXML("Hello Jersey", null);
	}
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sayHTMLHello()
	{
		return "<html><body><h1>Hello Jersey</h1></body></html>";
	}

//	@GET
//	@Path("getXML/{request}")
//	@Produces(MediaType.APPLICATION_XML)
//	public String getXML(@PathParam("request") Request request)
//	{
//		System.out.println("getXML");
//		return "yes";
//	}
	
//	@GET
//	@Path("getJSON/{request}")
//	@Produces(MediaType.APPLICATION_JSON)
//	public String getJSON(@PathParam("request") A a)
//	{
//		System.out.println("getJSON");
//		return "yes";
//	}
	
//	@POST
//	@Path("getJSON")
//	@Consumes(MediaType.APPLICATION_XML)
//	@Produces(MediaType.TEXT_PLAIN)
//	public String getJSON2(@FormParam("request") String a)
//	{
//		System.out.println("getJSON: "+a);
//		A ao = JavaReader.objectFromXML(a, null);
//		return "yes";
//	}
	
	@POST
	@Path("getJSON")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public String getJSON2(A a)
	{
		System.out.println("getJSON: "+a);//+" "+aa);
		System.out.println("getJSON: "+context.getQueryParameters());
//		A ao = JavaReader.objectFromXML(a, null);
		return "yes";
	}
	
	@GET
	@Path("getXML")
	@Produces(MediaType.APPLICATION_XML)
	public A getXML()
	{
		System.out.println("getXML"+context.getQueryParameters());
		return new A("hallo");
	}
		
	
//	/**
//	 *  Main for testing.
//	 */
//	public static void main(String[] args)
//	{
//		try
//		{
////			URI uri = UriBuilder.fromUri("http://localhost/").port(8080).build();
//			URI uri = new URI("http://localhost:8080/");
//			
//			Map<String, Object> props = new HashMap<String, Object>();
//			props.put("com.sun.jersey.config.property.packages", "jadex.webservice.examples.rs.hello, jadex.extension.rs.publish");
//			props.put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
//			props.put("hallo", "hallo");
//			// "com.sun.jersey.config.property.packages", 
//			PackagesResourceConfig config = new PackagesResourceConfig(props);
////			PackagesResourceConfig config = new PackagesResourceConfig(new String[]{"jadex.micro.examples.rs.banking"});
////			config.setPropertiesAndFeatures(props);
//			
//			HttpServer srv = GrizzlyServerFactory.createHttpServer(uri, config);
//			
//			srv.start();
//			
//			ClientConfig cc = new DefaultClientConfig();
////			cc.getClasses().add(JAXBProvider.class);
//			cc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
//			cc.getClasses().add(JadexXMLBodyReader.class);
//			Client client = Client.create(cc);
//			WebResource service = client.resource(uri); 
////			String res = service.path("hello").accept(MediaType.TEXT_PLAIN).get(String.class);
////			String res = service.path("banking").accept(MediaType.TEXT_HTML).get(String.class);
////			System.out.println("Received: "+res);
//			
//			A a = new A("a");
//			a.getB().add("aa");
////			a.setB(new String[]{"aa"});
//			
//			ObjectMapper mapper = new ObjectMapper();
//			
//			// Form parameter can only be of type string 
////			String res = service.path("hello").
//			Form f = new Form();
////			f.add("a", JavaWriter.objectToXML(a, null));
//			System.out.println("jackson json: "+mapper.writeValueAsString(a));
//			
////			Class[] types = {A.class};
////			JSONJAXBContext contextj = new JSONJAXBContext(JSONConfiguration.mapped().build(), types);
////			JSONMarshaller ma = contextj.createJSONMarshaller();
////			StringWriter sw = new StringWriter();
////			ma.marshallToJSON(a, sw);
////			System.out.println("jaxb json: "+sw.toString());
//			
//			// .accept(MediaType.APPLICATION_JSON)
////			service.path("hello/getJSON").type(MediaType.APPLICATION_JSON).post(String.class, a);
//			A res = service.path("hello/getXML").type(MediaType.APPLICATION_XML).get(A.class);
//			System.out.println("got: "+res);
////			String res = service.path("hello/getJSON").type(MediaType.APPLICATION_XML).post(String.class, f);
//			
////			JAXBContext context = JAXBContext.newInstance(A.class);
////			Marshaller m = context.createMarshaller();
////			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
////			m.setProperty(Marshaller.JAXB_ENCODING, Marshaller.)
////			m.marshal(a, System.out);
//			
////			response = service.path("rest").path("todos").type(MediaType.APPLICATION_FORM_URLENCODED)
////			   .post(ClientResponse.class, form);
////			System.out.println("Received: "+res);
//
//			System.in.read();
//			srv.stop();
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//		}
//	}
	
	/**
	 *  Main for testing.
	 */
	public static void main(String[] args)
	{
		try
		{
			ClientConfig cc = new DefaultClientConfig();
			cc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
			cc.getClasses().add(JadexXMLBodyReader.class);
			Client client = Client.create(cc);
			WebResource service = client.resource("http://localhost:8080/banking1/"); 
			ObjectMapper mapper = new ObjectMapper();
//			System.out.println("jackson json: "+mapper.writeValueAsString(a));
			service.path("addTransactionDataJSON").type(MediaType.TEXT_PLAIN).post(Void.class, "hallo");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}

