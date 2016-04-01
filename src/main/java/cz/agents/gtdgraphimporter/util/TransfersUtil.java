package cz.agents.gtdgraphimporter.util;

import com.google.common.collect.ImmutableList;
import cz.agents.basestructures.Edge;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.Node;
import cz.agents.multimodalplanning.comparators.TimeKeyComparator;
import cz.agents.multimodalplanning.goalcheckers.PTStopsGoalChecker;
import cz.agents.multimodalplanning.searchstructures.graphs.MultiModalSearchGraph;
import cz.agents.multimodalplanning.searchstructures.graphs.WalkSearchGraph;
import cz.agents.multimodalplanning.searchstructures.nodes.RoadSearchNode;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.TimeDependentEdge;
import cz.agents.multimodalstructures.edges.VirtualEdge;
import cz.agents.multimodalstructures.nodes.StopNode;
import cz.agents.planningalgorithms.singlecriteria.algorithms.DijkstraSimpleGraphMultipleGoals;
import cz.agents.planningalgorithms.singlecriteria.structures.GraphPath;
import cz.agents.planningalgorithms.singlecriteria.structures.MultiGoalChecker;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Contains methods for generation of transfers. Taken and updated from VirtualEdgeCreator from
 * cz.agents.planner-core(9.0.1).
 *
 * @author Marek Cuchý
 * @author Jan Nykl
 */
public class TransfersUtil {

	private static final Logger LOGGER = Logger.getLogger(TransfersUtil.class);

	/**
	 * Precomputes the transfer edges between public transport stops.
	 *
	 * @param gtdGraph
	 * 		A GTD graph for which the virtual edges should be precomputed.
	 * @param defaultWalkSpeedInKMpH
	 * 		Default walking speed in Kph
	 * @param maxTransferLength
	 * 		Maximum walking distance between two stops, i.e. maximum length of the virtual edge
	 * @param threadPool
	 * 		An executor service that will be used for internal computations.
	 *
	 * @return A collection of the virtual edges
	 */
	public static Collection<? extends VirtualEdge> precomputeTransfers(Graph<Node, TimeDependentEdge> gtdGraph,
																		 float defaultWalkSpeedInKMpH,
																		 int maxTransferLength,
																		 ExecutorService threadPool) {

		long t1 = System.currentTimeMillis();

		Collection<? extends VirtualEdge> edges = precomputeVirtualEdges(gtdGraph, new WalkSearchGraph(gtdGraph,
				defaultWalkSpeedInKMpH, maxTransferLength), input -> input instanceof StopNode, input -> new
				PTStopsGoalChecker<>(input.getId(), gtdGraph), threadPool, defaultWalkSpeedInKMpH);

		long t2 = System.currentTimeMillis();
		LOGGER.info("Precomputing PT transfers took " + (t2 - t1) + " ms. Added edges: " + edges.size() + " Max " +
				"length" +
				" " +
				"in" +
				" m: " + maxTransferLength);

		return edges;
	}

	/**
	 * A generic function for precomputing the virtual edges. The virtual edges are created from all nodes that pass
	 * through the {@code startPointChecker} check to all nodes returned by appropriately created {@code
	 * destinationGoalCheckerCreator}. The length/weight of the virtual edge is base on the shortest path in {@code
	 * subtypeGraph}.
	 *
	 * @param gtdGraph
	 * 		GTD Graph for which the virtual edges should be precomputed.
	 * @param subtypeGraph
	 * 		A graph that is used for precomputing the lengths of the virtual edges.
	 * @param startPointChecker
	 * 		A predicate that checks whether the given point is a starting point of any virtual edge or not
	 * @param destinationGoalCheckerCreator
	 * 		A function that creates a goal checker from the starting point node.
	 * @param threadPool
	 * 		An executor pool that actually executes the graph searches.
	 * @param speedUsedForPrecomputing
	 * 		An internal type of created virtual edges
	 *
	 * @return A collections of newly created virtual edges.
	 */
	public static Collection<VirtualEdge> precomputeVirtualEdges(Graph<?, ?> gtdGraph,
																 MultiModalSearchGraph<RoadSearchNode> subtypeGraph,
																 Predicate<? super Node> startPointChecker,
																 Function<? super Node,
																		 MultiGoalChecker<RoadSearchNode>>
																		 destinationGoalCheckerCreator,
																 ExecutorService threadPool,
																 float speedUsedForPrecomputing) {

		List<VirtualEdge> edges = new ArrayList<>();
		TimeKeyComparator<RoadSearchNode> timeVertexEvaluator = new TimeKeyComparator<>();// we MUST optimalize for
		// time

		List<Future<Map<Integer, GraphPath<RoadSearchNode>>>> futures = new ArrayList<>();

		for (Node node : gtdGraph.getAllNodes()) {
			if (startPointChecker.test(node)) {
				RoadSearchNode startVertex = new RoadSearchNode(node.getId(), 0, 0);
				try {
					MultiGoalChecker<RoadSearchNode> goalChecker = destinationGoalCheckerCreator.apply(node);
					DijkstraSimpleGraphMultipleGoals<RoadSearchNode> g = new DijkstraSimpleGraphMultipleGoals<>
							(subtypeGraph, Collections.singleton(startVertex), timeVertexEvaluator, goalChecker);
					futures.add(threadPool.submit(g));
				} catch (Exception e) {
					LOGGER.warn("An exception has occurred during processing node " + node.getId(), e);
				}
			}
			if (futures.size() == 20) { // clearing futures stack ( reducing memory requirements)
				edges.addAll(emptyFutures(futures, gtdGraph, speedUsedForPrecomputing));
			}
		}
		edges.addAll(emptyFutures(futures, gtdGraph, speedUsedForPrecomputing));

		return edges;
	}

	private static List<VirtualEdge> emptyFutures(List<Future<Map<Integer, GraphPath<RoadSearchNode>>>> futures,
												  Graph<?, ?> graph, float speedUsedForPrecomputing) {
		List<VirtualEdge> addedEdges = new ArrayList<>();

		for (Future<Map<Integer, GraphPath<RoadSearchNode>>> future : futures) {
			try {
				addedEdges.addAll(createVirtualEdges(future.get(), graph, speedUsedForPrecomputing));
			} catch (InterruptedException | ExecutionException e) {
				LOGGER.error(e);
			}
		}
		futures.clear();

		return addedEdges;
	}

	private static List<VirtualEdge> createVirtualEdges(Map<Integer, GraphPath<RoadSearchNode>> pathsMap,
														Graph<?, ?> graph, float speedUsedForPrecomputing) {

		List<VirtualEdge> addedEdges = new ArrayList<>();
		for (GraphPath<RoadSearchNode> path : pathsMap.values()) {
			int time = path.getLastNode().getArrivalTime() - path.getFirstNode().getArrivalTime();
			int length = path.getLastNode().getAdditionalKey();

			ImmutableList.Builder<Edge> edgeList = new ImmutableList.Builder<>();

			List<RoadSearchNode> sequence = path.getNodeSequence();
			for (int i = 0; i < sequence.size() - 1; i++) {
				RoadSearchNode curr = sequence.get(i);
				RoadSearchNode next = sequence.get(i+1);
				edgeList.add(graph.getEdge(curr.nodeId, next.nodeId));
			}

			addedEdges.add(new VirtualEdge(path.getFirstNode().nodeId, path.getLastNode().nodeId,
					speedUsedForPrecomputing, time, ModeOfTransport.WALK, edgeList.build(), length));
		}
		return addedEdges;
	}

}
