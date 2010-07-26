package jadex.bdi.model.editable;

import jadex.bdi.model.IMBeliefbase;
import jadex.bdi.model.IMCapability;
import jadex.bdi.model.IMCapabilityReference;
import jadex.bdi.model.IMConfiguration;
import jadex.bdi.model.IMEventbase;
import jadex.bdi.model.IMExpression;
import jadex.bdi.model.IMExpressionbase;
import jadex.bdi.model.IMGoalbase;
import jadex.bdi.model.IMPlanbase;
import jadex.bdi.model.IMPropertybase;

/**
 *  Interface for editable version of capability.
 */
public interface IMECapability extends IMCapability
{
	/**
	 *  Set the package.
	 *  @param The package.
	 */
	public void setPackage(String name);
	
	/**
	 *  Set the imports.
	 *  @param The imports.
	 */
	public void setImports(String[] imports);
	
	/**
	 *  Set if is abstract.
	 *  @param True, if is abstract.
	 */
	public void setAbstract(boolean abs);
	
	/**
	 *  Get the capability references.
	 *  @return The capability references.
	 */
	public void createCapabilityReference(String name, String file);
	
	/**
	 *  Create or get the beliefbase.
	 *  @return The belief base.
	 */
	public IMEBeliefbase createBeliefbase();
	
	/**
	 *  Create or get the beliefbase.
	 *  @return The goalbase.
	 */
	public IMEGoalbase createGoalbase();
	
	/**
	 *  Create or get the planbase.
	 *  @return The planbase.
	 */
	public IMEPlanbase createPlanbase();
	
	/**
	 *  Create or get the eventbase.
	 *  @return The eventbase.
	 */
	public IMEEventbase createEventbase();
	
	/**
	 *  Get the expressionbase.
	 *  @return The expressionbase.
	 */
	public IMEExpressionbase createExpressionbase();
	
	/**
	 *  Get the propertybase.
	 *  @return The propertybase.
	 */
	public IMEPropertybase createPropertybase();
	
	/**
	 *  Get the services.
	 *  @return The services.
	 */
	public IMEExpression createService();
	
	/**
	 *  Create a configuration.
	 *  @return The configuration.
	 */
	public IMEConfiguration createConfiguration();
}
