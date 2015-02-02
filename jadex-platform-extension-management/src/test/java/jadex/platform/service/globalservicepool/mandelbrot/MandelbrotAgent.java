package jadex.platform.service.globalservicepool.mandelbrot;

import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Component;
import jadex.micro.annotation.ComponentType;
import jadex.micro.annotation.ComponentTypes;
import jadex.micro.annotation.Configuration;
import jadex.micro.annotation.Configurations;
import jadex.micro.annotation.Imports;
import jadex.micro.annotation.NameValue;
import jadex.platform.service.globalservicepool.GlobalServicePoolAgent;
import jadex.platform.service.servicepool.PoolServiceInfo;

@Imports(
{
	"jadex.platform.service.servicepool.*",
})
@ComponentTypes({
	@ComponentType(name="Generator", clazz=GenerateAgent.class),
	@ComponentType(name="Calculator", clazz=CalculateAgent.class),
	@ComponentType(name="Display", clazz=DisplayAgent.class),
	@ComponentType(name="pool", clazz=GlobalServicePoolAgent.class)
})
//@ProvidedServices(@ProvidedService(type=IMandelbrotService.class, implementation=@Implementation(MandelbrotService.class)))
@Configurations(
	@Configuration(name="default", components={
		@Component(type="Generator"),
		@Component(name="Calculator", type="pool", arguments=
		{
			@NameValue(name="serviceinfos", clazz=PoolServiceInfo[].class, value="new PoolServiceInfo[]{"
				+ "new PoolServiceInfo(\"jadex.platform.service.globalservicepool.mandelbrot.CalculateAgent.class\", ICalculateService.class,"
				+ "new String[]{\"delay\"}, new Object[]{-1})}")
		}),
//		@Component(type="Calculator"),
		@Component(type="Display")
	})
)
@Agent
public class MandelbrotAgent 
{
}
