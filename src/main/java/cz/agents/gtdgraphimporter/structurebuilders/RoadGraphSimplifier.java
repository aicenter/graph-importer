package cz.agents.gtdgraphimporter.structurebuilders;

import cz.agents.gtdgraphimporter.structurebuilders.EdgeBuilder.EdgeId;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.nodes.RoadNode;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author Marek Cuchý
 */
public class RoadGraphSimplifier {

	private static final Logger LOGGER = Logger.getLogger(RoadGraphSimplifier.class);

	private final TmpGraphBuilder<RoadNode, RoadEdge> graph;
	private final Set<Integer> notToBeRemovedNodes;

	/**
	 * It contains mapping of the first and last deleted points of SimplificationBuilders to the builders. For example,
	 * a builder representing edges {@code from -> n1 -> n2 -> n3 -> to} as mapped from n1 and n3. All other nodes
	 * don't
	 * have any mapping. LinkedHashMap is used only for its deterministic behavior, therefore it can be replaced.
	 */
	private final Map<Integer, SimplifiedEdgeBuilder> nodeToSimplBuilders = new LinkedHashMap<>();

	private final Set<Integer> nodesToRemove = new LinkedHashSet<>();
	private final Set<EdgeId> edgesToRemove = new LinkedHashSet<>();

	private static final Predicate<RoadNodeBuilder> NOT_DELETE = n -> n.isBikeSharingStation() || n.isParkAndRide();

	private RoadGraphSimplifier(TmpGraphBuilder<RoadNode, RoadEdge> graph, Set<Integer> notToBeRemovedNodes) {
		this.graph = graph;
		this.notToBeRemovedNodes = notToBeRemovedNodes;
		for (NodeBuilder<? extends RoadNode> node : graph.getAllNodes()) {
			RoadNodeBuilder n = (RoadNodeBuilder) node;
			if (NOT_DELETE.test(n)) {
				this.notToBeRemovedNodes.add(n.tmpId);
			}
		}
	}

	public void simplify() {
		Map<Integer, Set<Integer>> inDegree = graph.getNodesByInDegree();
		Map<Integer, Set<Integer>> outDegree = graph.getNodesByOutDegree();

		prepareOneWays(inDegree, outDegree);
		prepareTwoWays(inDegree, outDegree);

		replaceEdges();
	}

	private void replaceEdges() {

		Set<SimplifiedEdgeBuilder> builderSet = new HashSet<>(nodeToSimplBuilders.values());
		List<SimplifiedEdgeBuilder> builderList = new ArrayList<>(builderSet);
		builderList.forEach(e -> e.calculateAndSetLength(graph));
		// Sort for the case of two conflicting simplified edges (results between the same two nodes). The longer one
		// is split
		Collections.sort(builderList, SimplifiedEdgeBuilder.LENGTH_COMPARATOR);

		Map<EdgeId, RoadEdgeBuilder> removedEdges = new HashMap<>();
		for (EdgeId edgeId : edgesToRemove) {
			RoadEdgeBuilder removed = (RoadEdgeBuilder) graph.remove(edgeId);
			if (removed == null) {
				throw new IllegalStateException("Edge doesn't exist.");
			}
			removedEdges.put(edgeId, removed);
		}

		for (SimplifiedEdgeBuilder builder : builderList) {
			List<RoadEdgeBuilder> newEdges;
			//if graph already contains the possible new edges or the edge start and ends in the same node
			if (builder.isCircle()) {
				newEdges = builder.build(3, graph, removedEdges);
			} else if (graph.containsEdge(builder.getTmpFromId(), builder.getTmpToId()) || (!builder.isOneWay() &&
					graph.containsEdge(builder.getTmpToId(), builder.getTmpFromId()))) {
				newEdges = builder.build(2, graph, removedEdges);
			} else {
				newEdges = builder.build(1, graph, removedEdges);
			}
			for (RoadEdgeBuilder newEdge : newEdges) {
				graph.addEdge(newEdge);
				//preserve end nodes from deletion
				nodesToRemove.remove(newEdge.getTmpFromId());
				nodesToRemove.remove(newEdge.getTmpToId());
			}
		}

		for (Integer nodeId : nodesToRemove) {
			if (!graph.removeNode(nodeId)) {
				throw new IllegalStateException("Node doesn't exist.");
			}
		}
	}

	private void prepareTwoWays(Map<Integer, Set<Integer>> inDegree, Map<Integer, Set<Integer>> outDegree) {
		Set<Integer> simplNodes4 = getRemovableNodes(inDegree, outDegree, 2);
		for (Integer nodeId : simplNodes4) {
			List<EdgeBuilder<? extends RoadEdge>> ins = graph.getIncomingEdges(nodeId);
			List<EdgeBuilder<? extends RoadEdge>> outs = graph.getOutgoingEdges(nodeId);

			RoadEdgeBuilder in1 = (RoadEdgeBuilder) ins.get(0);
			RoadEdgeBuilder in2 = (RoadEdgeBuilder) ins.get(1);
			RoadEdgeBuilder out1 = (RoadEdgeBuilder) outs.get(0);
			RoadEdgeBuilder out2 = (RoadEdgeBuilder) outs.get(1);

			//only one is possible
			boolean merge1 = mergeIfPossible(in1, in2, out1, out2);
			boolean merge2 = mergeIfPossible(in1, in2, out2, out1);

			assert !(merge1 && merge2) : "Only one merge should be possible";
		}
	}

	private void prepareOneWays(Map<Integer, Set<Integer>> inDegree, Map<Integer, Set<Integer>> outDegree) {
		Set<Integer> simplNodes2 = getRemovableNodes(inDegree, outDegree, 1);
		for (Integer nodeId : simplNodes2) {
			RoadEdgeBuilder in = (RoadEdgeBuilder) graph.getIncomingEdges(nodeId).get(0);
			RoadEdgeBuilder out = (RoadEdgeBuilder) graph.getOutgoingEdges(nodeId).get(0);
			if (mergable(in, out)) {
				int f = in.getTmpFromId();
				int n = in.getTmpToId();
				int t = out.getTmpToId();

				SimplifiedEdgeBuilder fSimpl = nodeToSimplBuilders.get(f);
				SimplifiedEdgeBuilder tSimpl = nodeToSimplBuilders.get(t);

				if (fSimpl != null && tSimpl != null) {
					assert f == fSimpl.getLastRemovedNode() && n == fSimpl.getTmpToId() && n == tSimpl.getTmpFromId()
							&& t == tSimpl.getFirstRemovedNode() : "Merged edges must be boundary edges of the " +
							"simplifiers.";
					append(fSimpl, tSimpl);
					nullify(n, null);
				} else if (fSimpl != null) {
					addEdge(n, t, fSimpl);
				} else if (tSimpl != null) {
					addEdge(n, f, tSimpl);
				} else {
					SimplifiedEdgeBuilder builder = SimplifiedEdgeBuilder.createOneway(f, n, t);
					nodeToSimplBuilders.put(n, builder);
				}
				nodesToRemove.add(n);
				edgesToRemove.add(in.getEdgeId());
				edgesToRemove.add(out.getEdgeId());
			}
		}
	}

	private boolean mergeIfPossible(RoadEdgeBuilder in1, RoadEdgeBuilder in2, RoadEdgeBuilder out1,
									RoadEdgeBuilder out2) {
		if (areOpposite(in1, out1) && areOpposite(in2, out2) && mergable(in1, out2) && mergable(in2, out1)) {
			int f = in1.getTmpFromId();
			int n = in1.getTmpToId();
			int t = out2.getTmpToId();

			assert (!nodeToSimplBuilders.containsKey(f) || nodeToSimplBuilders.get(f) != null) &&
					(!nodeToSimplBuilders.containsKey(t) || nodeToSimplBuilders.get(t) != null) : "Deleted nodes " +
					"other than the first can't be merged.";

			SimplifiedEdgeBuilder fSimpl = nodeToSimplBuilders.get(f);
			SimplifiedEdgeBuilder tSimpl = nodeToSimplBuilders.get(t);

			if (fSimpl != null && tSimpl != null) {
				assert fSimpl.equalType(tSimpl) : "Types (one-way, two-way) have to be equal.";
				merge(n, fSimpl, tSimpl);
			} else if (fSimpl != null) {
				addEdge(n, t, fSimpl);
			} else if (tSimpl != null) {
				addEdge(n, f, tSimpl);
			} else {
				SimplifiedEdgeBuilder builder = SimplifiedEdgeBuilder.createTwoWay(f, n, t);
				nodeToSimplBuilders.put(n, builder);
			}
			nodesToRemove.add(n);
			edgesToRemove.add(in1.getEdgeId());
			edgesToRemove.add(in2.getEdgeId());
			edgesToRemove.add(out1.getEdgeId());
			edgesToRemove.add(out2.getEdgeId());
			return true;
		}
		return false;
	}

	private void merge(int n, SimplifiedEdgeBuilder fSimpl, SimplifiedEdgeBuilder tSimpl) {
		//different operation for all combinations of simplified edge directions
		if (fSimpl.getTmpToId() == n) {
			if (tSimpl.getTmpFromId() == n) {
				// f -> n -> t
				append(fSimpl, tSimpl);
			} else {
				// f -> n <- t
				assert tSimpl.getTmpToId() == n : "One of the endpoints have to be the deleted node.";
				nullify(fSimpl.getLastRemovedNode(), fSimpl);
				nullify(tSimpl.getLastRemovedNode(), tSimpl);
				fSimpl.appendReversed(tSimpl);
				nodeToSimplBuilders.put(fSimpl.getLastRemovedNode(), fSimpl);
			}
			assert checkCorrectBuilderMapping(fSimpl, tSimpl) : "Simplification builders are not mapped correctly.";
		} else {
			assert fSimpl.getTmpFromId() == n : "One of the endpoints have to be the deleted node.";
			if (tSimpl.getTmpFromId() == n) {
				// f <- n -> t
				nullify(tSimpl.getFirstRemovedNode(), tSimpl);
				nullify(fSimpl.getFirstRemovedNode(), fSimpl);
				tSimpl.prependReversed(fSimpl);
				nodeToSimplBuilders.put(tSimpl.getFirstRemovedNode(), tSimpl);
			} else {
				// f <- n <- t
				assert tSimpl.getTmpToId() == n : "One of the endpoints have to be the deleted node.";
				append(tSimpl, fSimpl);
			}
			assert checkCorrectBuilderMapping(tSimpl, fSimpl) : "Simplification builders are not mapped correctly.";
		}
		nullify(n, null);
	}

	private boolean checkCorrectBuilderMapping(SimplifiedEdgeBuilder mapped, SimplifiedEdgeBuilder deleted) {
		return isMapped(mapped) && isDeleted(deleted);
	}

	private boolean isDeleted(SimplifiedEdgeBuilder deleted) {
		return nodeToSimplBuilders.get(deleted.getFirstRemovedNode()) != deleted && nodeToSimplBuilders.get(deleted
				.getLastRemovedNode()) != deleted;
	}

	private boolean isMapped(SimplifiedEdgeBuilder mapped) {
		return nodeToSimplBuilders.get(mapped.getFirstRemovedNode()) == mapped && nodeToSimplBuilders.get(mapped
				.getLastRemovedNode()) == mapped;
	}

	/**
	 * Set simplification builder mapped to {@code id} to null. It's not necessary for the task, it serves testing
	 * purposes
	 *
	 * @param id
	 * @param builder
	 */
	private void nullify(int id, SimplifiedEdgeBuilder builder) {
		if (builder == null || builder.numberOfNodes() > 3) {
			nodeToSimplBuilders.remove(id);
		}
	}

	private void addEdge(int deletedNode, int endNode, SimplifiedEdgeBuilder builder) {
		if (deletedNode == builder.getTmpFromId()) {
			nullify(builder.getFirstRemovedNode(), builder);
		} else {
			nullify(builder.getLastRemovedNode(), builder);
		}
		builder.add(deletedNode, endNode);
		nodeToSimplBuilders.put(deletedNode, builder);
	}

	/**
	 * Appends {@code postfix} to {@code prefix}.
	 *
	 * @param prefix
	 * @param postfix
	 */
	private void append(SimplifiedEdgeBuilder prefix, SimplifiedEdgeBuilder postfix) {
		nullify(prefix.getLastRemovedNode(), prefix);
		nullify(postfix.getFirstRemovedNode(), postfix);
		prefix.append(postfix);
		nodeToSimplBuilders.put(prefix.getLastRemovedNode(), prefix);
	}

	private boolean areOpposite(EdgeBuilder<?> incoming, EdgeBuilder<?> outgoing) {
		return incoming.getTmpFromId() == outgoing.getTmpToId();
	}

	/**
	 * Get all nodes that have both (in/out) degrees equal to {@code degree} and can be removed from the graph.
	 *
	 * @param inDegree
	 * @param outDegree
	 * @param degree
	 *
	 * @return
	 */
	private Set<Integer> getRemovableNodes(Map<Integer, Set<Integer>> inDegree, Map<Integer, Set<Integer>> outDegree,
										   int degree) {
		Set<Integer> nodes = new HashSet<>(inDegree.get(degree));
		nodes.retainAll(outDegree.get(degree));
		nodes.removeAll(notToBeRemovedNodes);
		return nodes;
	}

	private boolean mergable(RoadEdgeBuilder incoming, RoadEdgeBuilder outgoing) {
		return incoming.equalAttributes(outgoing) && !incoming.isCircle(outgoing);
	}

	public static void simplify(TmpGraphBuilder<RoadNode, RoadEdge> graph, Set<Integer> notToBeRemovedNodes) {
		new RoadGraphSimplifier(graph, notToBeRemovedNodes).simplify();
	}
}
