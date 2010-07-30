package deco4mas.examples.agentNegotiation.sma.coordination.negotiationStrategy;

import jadex.bridge.IComponentIdentifier;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import deco4mas.examples.agentNegotiation.common.dataObjects.ServiceProposal;

public interface IUtilityFunction
{
	/**
	 * Benchmark the given Set of ServicePropsals Should use evaluate for every
	 * single proposal
	 */
	public SortedMap<Double, ServiceProposal> benchmarkProposals(Set<ServiceProposal> participants, Long thetime);

	public Double evaluate(Map<String, Double> evaluateVector);
}
