package sodekovs.antworld.movement;

import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.types.clock.IClockService;
import jadex.commons.future.IFuture;
import jadex.extension.envsupport.environment.AbstractTask;
import jadex.extension.envsupport.environment.IEnvironmentSpace;
import jadex.extension.envsupport.environment.ISpaceObject;
import jadex.extension.envsupport.environment.space2d.ContinuousSpace2D;
import jadex.extension.envsupport.environment.space2d.Space2D;
import jadex.extension.envsupport.math.IVector2;
import jadex.extension.envsupport.math.Vector1Double;
import jadex.extension.envsupport.math.Vector2Double;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Move an object towards a destination.
 */
public class MoveTask extends AbstractTask {
	// -------- constants --------

	/** The destination property. */
	public static final String PROPERTY_TYPENAME = "move";

	/** The destination property. */
	public static final String PROPERTY_DESTINATION = "destination";

	/** The scope property. */
	public static final String PROPERTY_SCOPE = "scope";

	/** The speed property of the moving object (units per second). */
	public static final String PROPERTY_SPEED = "speed";

	/** The vision property of the moving object (radius in units). */
	public static final String PROPERTY_VISION = "vision";

	/** The objectId of the calling avatar. */
	public static final String ACTOR_ID = "actor_id";

	/** Denotes that the ISpaceObject should perform a gradient walk on the detected pheromones. */
	public static final String PHEROMONE_GRADIENT_WALK = "pheromone_gradient_walk";

	/** If the ant has reached its intended destination, force him to walk in the same direction till the border of the area. */
	private boolean hasReachedPheromoneDestination = false;

	/** Stop gradient walk, if the ant has reached the border. . */
	private boolean hasReachedBorder = false;

	/** Denotes the gradient between the current location and next location. Is used to determine the walking direction for the pheromone gradient walk. */
	private IVector2 gradient = null;

	// -------- IObjectTask methods --------

	/**
	 * Executes the task. Handles exceptions. Subclasses should implement doExecute() instead.
	 * 
	 * @param space
	 *            The environment in which the task is executing.
	 * @param obj
	 *            The object that is executing the task.
	 * @param progress
	 *            The time that has passed according to the environment executor.
	 */
	public void execute(final IEnvironmentSpace space, ISpaceObject obj, long progress, IClockService clock) {
		IVector2 destination = (IVector2) getProperty(PROPERTY_DESTINATION);
		final IBDIExternalAccess agent = (IBDIExternalAccess) getProperty(PROPERTY_SCOPE);
		// System.out.println("#mmmoveTask# " + destination);
		double speed = ((Number) obj.getProperty(PROPERTY_SPEED)).doubleValue();
		double maxdist = progress * speed * 0.001;
		// Denotes whether the ISpaceObject should perform a gradient walk
		boolean gradientWalk = (Boolean) getProperty(PHEROMONE_GRADIENT_WALK) == null ? false : true;
		IVector2 loc = (IVector2) obj.getProperty(Space2D.PROPERTY_POSITION);
		// Todo: how to handle border conditions!?
		IVector2 newloc = null;
		// Perform gradient walk: ant has reached intended destination -> force him to walk in the same direction till the border of the area
		if (gradientWalk && hasReachedPheromoneDestination) {
//			System.out.println("#MovTask# location: " + loc + " - " + (Long) getProperty(ACTOR_ID));
			IVector2 tmp = loc.copy();
//			System.out.println("#MovTask# tmpLocation: " + tmp + " - " + (Long) getProperty(ACTOR_ID));
			tmp.add(gradient);
//			System.out.println("#MovTask# tmpLocation+gradient: " + tmp + " - " + (Long) getProperty(ACTOR_ID));
			if (tmp.getXAsDouble() >= 0.80 || tmp.getYAsDouble() >= 0.80) {
				hasReachedBorder = true;
				newloc = loc;
//				System.out.println("#MovTask# Border Test x,y: " + tmp.getXAsDouble() + " - " + tmp.getYAsDouble() + " - " + hasReachedBorder + " - " + (Long) getProperty(ACTOR_ID));
			} else {
				newloc = tmp;
			}
		} else {
			// perform "normal" walk
			// System.out.println("#MovTask# Normal Walk  "+ (Long) getProperty(ACTOR_ID) );
			newloc = ((Space2D) space).getDistance(loc, destination).getAsDouble() <= maxdist ? destination : destination.copy().subtract(loc).normalize().multiply(maxdist).add(loc);

			// HACK: Take first gradient, otherwise ants get to slow, because the last gradient before reaching the goal is often to small.
			if (gradient == null) {
				gradient = loc.subtract(newloc);
				gradient = gradient.multiply(-1.0);
//				System.out.println("TTTTTTTTTTTT: org: " + gradient + " ;mult: " + gradient.multiply(-1.0));				
			}
		}

		// Is there an obstacle that cannot be passed by?
		if (positionHasObstacle(space, newloc)) {
			newloc = new Vector2Double(loc.getXAsDouble(), loc.getYAsDouble() - 0.005);
			// System.out.println("Detected obstacle.Moving 0.005 up");
		}

		// System.out.println("#moveTask locations: " + loc.toString() + " - " + newloc.toString() + " ;diff: " + loc.subtract(newloc));
		((Space2D) space).setPosition(obj.getId(), newloc);

		// // Check access to surrounding agent
		// agent.scheduleStep(new IComponentStep<Void>() {
		// public IFuture<Void> execute(IInternalAccess ia) {
		// IBDIInternalAccess bia = (IBDIInternalAccess) ia;
		// Object[] foodSources = bia.getBeliefbase().getBeliefSet("foodSources").getFacts();
		// System.out.println("#AntAgent# MoveTask: Size of foodSources: " + foodSources.length);
		// return IFuture.DONE;
		// }
		// });

		// Check, whether agent should walk randomly with or without remembering already visited positions.
		// Confer WalkingStrategyEnum for Mapping of int values to semantics.
		int walkingStrategyProperty = (Integer) space.getSpaceObjectsByType("walkingStrategy")[0].getProperty("strategy");
		if (walkingStrategyProperty > 0) {

			// ************************Hack for Agents: *******************************
			final IVector2 newLocation = newloc.copy();
			final DecimalFormat decimalFormat = new DecimalFormat("#0.0000");
			agent.scheduleStep(new IComponentStep<Void>() {
				public IFuture<Void> execute(IInternalAccess ia) {
					IBDIInternalAccess bia = (IBDIInternalAccess) ia;
					ISpaceObject[] homebases = ((ISpaceObject[]) ((Space2D) space).getSpaceObjectsByType("homebase"));
					HashMap map = (HashMap) homebases[0].getProperty("visitedPos");
					synchronized (map) {
						map.put(decimalFormat.format(newLocation.getXAsDouble()), decimalFormat.format(newLocation.getYAsDouble()));
						homebases[0].setProperty("visitedPos", map);
					}

					// Hack: Create trace
					Map<String, Object> props = new HashMap<String, Object>();
					props.put(Space2D.PROPERTY_POSITION, newLocation);
					space.createSpaceObject("trace", props, null);

					return IFuture.DONE;
				}
			});
			// *******************************
		}

		// Produce a pheromone if the ant is carrying a piece of food from a source to a nest.
		long avatarId = (Long) getProperty(ACTOR_ID);
		ISpaceObject so = space.getSpaceObject(avatarId);
		if ((Boolean) so.getProperty("has_food")) {
			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put(Space2D.PROPERTY_POSITION, newloc);
			properties.put("strength", 10);
			space.createSpaceObject("pheromone", properties, null);
		}

		// Process vision at new location.
		double vision = ((Number) obj.getProperty(PROPERTY_VISION)).doubleValue();
		final Set objects = ((Space2D) space).getNearObjects((IVector2) obj.getProperty(Space2D.PROPERTY_POSITION), new Vector1Double(vision));
		// System.out.println("#MoveTask# Vision : " + objects == null ? false : true);
		if (objects != null) {
			agent.scheduleStep(new IComponentStep<Void>() {
				public IFuture<Void> execute(IInternalAccess ia) {
					IBDIInternalAccess bia = (IBDIInternalAccess) ia;
					ArrayList foodSourcesList = new ArrayList();
					ArrayList pheromonesList = new ArrayList();
					for (Iterator it = objects.iterator(); it.hasNext();) {
						final ISpaceObject so = (ISpaceObject) it.next();
						if (so.getType().equals("pheromone")) {
							pheromonesList.add(so);
							// if (!bia.getBeliefbase().getBeliefSet("my_targets").containsFact(so)) {
							// bia.getBeliefbase().getBeliefSet("my_targets").addFact(so);
							// }
							// System.out.println("New target seen: "+scope.getAgentName()+", "+objects[i]);
						} else if (so.getType().equals("food")) {
							foodSourcesList.add(so);
						}
					}

					bia.getBeliefbase().getBeliefSet("pheromones").removeFacts();
					bia.getBeliefbase().getBeliefSet("pheromones").addFacts(pheromonesList.toArray());
					bia.getBeliefbase().getBeliefSet("foodSources").removeFacts();
					bia.getBeliefbase().getBeliefSet("foodSources").addFacts(foodSourcesList.toArray());
					return IFuture.DONE;
				}
			});
			// System.out.println("New target seen: "+scope.getAgentName()+", "+objects[i]);

		}

		// Terminate condition for normal walk
		if ((newloc == destination) && !gradientWalk) {
			setFinished(space, obj, true);
			// System.out.println("#MoveTask# gradientwalk: " + gradientWalk);
			// Start pheromone gradient walk
		} else if ((newloc == destination) && gradientWalk && !hasReachedBorder) {
			hasReachedPheromoneDestination = true;
//			System.out.println("#MoveTask# Has reached init pheromone dest!");
			// Check whether the ant has reached the border of area. If so, stop walking
		} else if (hasReachedBorder) {
//			System.out.println("#MoveTask# Reached Border -> Set Finished! - " + avatarId);
			setFinished(space, obj, true);
		}
	}

	private boolean positionHasObstacle(IEnvironmentSpace space, IVector2 location) {
		return ((ContinuousSpace2D) space).getNearestObject(location, new Vector1Double(0.005), "obstacle") == null ? false : true;

	}
}
