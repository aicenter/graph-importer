package cz.agents.gtdgraphimporter.structurebuilders;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import cz.agents.basestructures.Edge;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.Node;
import cz.agents.multimodalstructures.nodes.RoadNode;
import cz.agents.multimodalstructures.nodes.StopNode;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

/**
 * This builder stores node builders and edge builders with temporary ids and assigns the final id when {@link
 * #createGraph()} is called.
 *
 * @author Marek Cuchý
 */
public class TmpGraphBuilder<TNode extends Node, TEdge extends Edge> {

	private final Map<Integer, NodeBuilder<? extends TNode>> nodes = new LinkedHashMap<>();
	private final Table<Integer, Integer, EdgeBuilder<? extends TEdge>> edges = HashBasedTable.create();

	private final Map<Long, Integer> longIdToIntId = new HashMap<>();

	public TmpGraphBuilder() {
	}

	public TmpGraphBuilder(TmpGraphBuilder<? extends TNode, ? extends TEdge> builder) {
		nodes.putAll(builder.nodes);
		edges.putAll(builder.edges);
		longIdToIntId.putAll(builder.longIdToIntId);
	}

	public void addNode(NodeBuilder<? extends TNode> builder) {
		if (nodes.containsKey(builder.tmpId))
			throw new IllegalArgumentException("Graph builder already contains node builder with tmp id: " + builder
					.tmpId);
		nodes.put(builder.tmpId, builder);
		longIdToIntId.put(builder.sourceId, builder.tmpId);
	}

	public void addEdge(EdgeBuilder<? extends TEdge> builder) {
		if (edges.contains(builder.getTmpFromId(), builder.getTmpToId()))
			throw new IllegalArgumentException("Graph builder already contains edge builder: [" + builder.getTmpFromId
					() + ", " + builder.getTmpToId() + "]");
		edges.put(builder.getTmpFromId(), builder.getTmpToId(), builder);
	}

	public Graph<TNode, TEdge> createGraph() {
		int id = 0;

		GraphBuilder<TNode, TEdge> builder = new GraphBuilder<>();

		Map<Integer, Integer> tmpToFinalId = new HashMap<>();

		for (NodeBuilder<? extends TNode> nodeBuilder : nodes.values()) {
			//StopNode for route nodes have to be set
			if (nodeBuilder instanceof RouteNodeBuilder) {
				RouteNodeBuilder rnb = (RouteNodeBuilder) nodeBuilder;
				if (!tmpToFinalId.containsKey(rnb.getStopNodeTmpId())) {
					throw new IllegalStateException("Stop node builder must be inserted before the corresponding " +
							"route node builders.");
				}
				rnb.setStopNode((StopNode) builder.getNode(tmpToFinalId.get(rnb.getStopNodeTmpId())));
			}
			tmpToFinalId.put(nodeBuilder.tmpId, id);
			builder.addNode(nodeBuilder.buildNode(id++));
		}

		for (EdgeBuilder<? extends TEdge> edgeBuilder : edges.values()) {
			int fromId = tmpToFinalId.get(edgeBuilder.getTmpFromId());
			int toId = tmpToFinalId.get(edgeBuilder.getTmpToId());
			builder.addEdge(edgeBuilder.build(fromId, toId));
		}
		return builder.createGraph();
	}

	public int getIntIdForSourceId(long sourceId) {
		return longIdToIntId.get(sourceId);
	}

	public boolean containsEdge(int tmpFromId, int tmpToId) {
		return edges.contains(tmpFromId, tmpToId);
	}

	public EdgeBuilder<? extends TEdge> getEdge(int tmpFromId, int tmpToId) {
		return edges.get(tmpFromId, tmpToId);
	}

	public NodeBuilder<? extends TNode> getNode(int tmpId) {
		return nodes.get(tmpId);
	}

	public boolean containsNode(long sourceId) {
		return longIdToIntId.containsKey(sourceId);
	}

	public int getNodeCount() {
		return nodes.size();
	}

	public int getMaxId() {
		return nodes.keySet().stream().max(Integer::max).get();
	}

	public int getEdgeCount() {
		return edges.size();
	}

	public Collection<EdgeBuilder<? extends TEdge>> getAllEdges() {
		return edges.values();
	}

	public void remove(EdgeBuilder<? extends TEdge> edgeBuilder) {
		edges.remove(edgeBuilder.getTmpFromId(), edgeBuilder.getTmpToId());
	}

	/**
	 * Remove all nodes without an incident edge and return number of removed nodes.
	 *
	 * @return
	 */
	public int removeIsolatedNodes() {
		int pre = nodes.size();
		TreeMap<Integer, Set<Integer>> nodesByDegree = getNodesByDegree();
		Set<Integer> isolatedIds = nodesByDegree.get(0);
		if (isolatedIds.isEmpty()) return 0;
		nodes.keySet().removeAll(isolatedIds);
		return pre - nodes.size();
	}

	public TreeMap<Integer, Set<Integer>> getNodesByDegree() {
		Map<Integer, AtomicInteger> nodesDegree = new HashMap<>();
		for (EdgeBuilder<?> edge : edges.values()) {
			AtomicInteger result;
			result = nodesDegree.get(edge.getTmpFromId());
			if (result == null) {
				result = new AtomicInteger(0);
				nodesDegree.put(edge.getTmpFromId(), result);
			}
			result.incrementAndGet();
			result = nodesDegree.get(edge.getTmpToId());
			if (result == null) {
				result = new AtomicInteger(0);
				nodesDegree.put(edge.getTmpToId(), result);
			}
			result.incrementAndGet();
		}

		TreeMap<Integer, Set<Integer>> collect = nodes.keySet().parallelStream().collect(groupingBy(id -> {
			AtomicInteger atomicInteger = nodesDegree.get(id);
			return atomicInteger == null ? 0 : atomicInteger.intValue();
		}, TreeMap::new, toSet()));

		return collect;
	}

	public Map<Integer, EdgeBuilder<? extends TEdge>> getOutgoingEdges(NodeBuilder<? extends RoadNode> node) {
		return edges.row(node.tmpId);
	}

	public Map<Integer, EdgeBuilder<? extends TEdge>> getIncomingEdges(NodeBuilder<? extends RoadNode> node) {
		return edges.column(node.tmpId);
	}

}
