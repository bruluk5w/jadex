package jadex.micro.examples.mandelbrot;

import jadex.bridge.service.RequiredServiceInfo;
import jadex.bridge.service.types.cms.IComponentManagementService;
import jadex.micro.annotation.Agent;
import jadex.micro.annotation.Binding;
import jadex.micro.annotation.Description;
import jadex.micro.annotation.Implementation;
import jadex.micro.annotation.ProvidedService;
import jadex.micro.annotation.ProvidedServices;
import jadex.micro.annotation.RequiredService;
import jadex.micro.annotation.RequiredServices;

/**
 *  Agent that can process generate requests.
 */
@Description("Agent offering a generate service.")
@ProvidedServices(@ProvidedService(type=IGenerateService.class, implementation=@Implementation(GenerateService.class)))
@RequiredServices({
	@RequiredService(name="displayservice", type=IDisplayService.class),
	//TODO , binding=@Binding(create=true, creationinfo=@CreationInfo(type="Display"))),
	@RequiredService(name="calculateservices", type=ICalculateService.class, multiple=true, binding=@Binding(scope=RequiredServiceInfo.SCOPE_GLOBAL)),
	@RequiredService(name="cmsservice", type=IComponentManagementService.class, binding=@Binding(scope=RequiredServiceInfo.SCOPE_PLATFORM)),
	@RequiredService(name="generateservice", type=IGenerateService.class)
})
@Agent
public class GenerateAgent
{
}
