package cz.agents.gtdgraphimporter.structurebuilders;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import cz.agents.basestructures.*;
import cz.agents.gtdgraphimporter.TransportMode;
import cz.agents.gtdgraphimporter.structurebuilders.edge.EdgeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.node.NodeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.node.RouteNodeBuilder;
import cz.agents.multimodalstructures.nodes.StopNode;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.function.Predicate;

import static java.util.stream.Collectors.*;

/**
 * This builder stores node builders and edge builders with temporary ids and assigns the final id when {@link
 * #createGraph()} is called.
 *
 * @author Marek Cuchý
 */
public class TmpGraphBuilder<TNode extends Node, TEdge extends Edge> {

	private static final Logger LOGGER = Logger.getLogger(TmpGraphBuilder.class);

	private final Map<Integer, NodeBuilder<? extends TNode>> nodes = new LinkedHashMap<>();

	private final Map<EdgeId, EdgeBuilder<? extends TEdge>> edges = new HashMap<>();
	private final ListMultimap<Integer, EdgeBuilder<? extends TEdge>> nodeOutgoingEdges = ArrayListMultimap.create();
	private final ListMultimap<Integer, EdgeBuilder<? extends TEdge>> nodeIncomingEdges = ArrayListMultimap.create();

	private final Map<Long, Integer> longIdToIntId = new HashMap<>();

	public TmpGraphBuilder() {
	}

	public TmpGraphBuilder(TmpGraphBuilder<? extends TNode, ? extends TEdge> builder) {
		nodes.putAll(builder.nodes);
		edges.putAll(builder.edges);
		nodeIncomingEdges.putAll(builder.nodeIncomingEdges);
		nodeOutgoingEdges.putAll(builder.nodeOutgoingEdges);
		longIdToIntId.putAll(builder.longIdToIntId);
	}

	public void addAll(TmpGraphBuilder<? extends TNode, ? extends TEdge> builder) {
		nodes.putAll(builder.nodes);
		edges.putAll(builder.edges);
		nodeIncomingEdges.putAll(builder.nodeIncomingEdges);
		nodeOutgoingEdges.putAll(builder.nodeOutgoingEdges);
		longIdToIntId.putAll(builder.longIdToIntId);
	}

	public void addNode(NodeBuilder<? extends TNode> builder) {
		if (nodes.containsKey(builder.tmpId)) throw new IllegalArgumentException(
				"Graph builder already contains node builder with tmp id: " + builder.tmpId);
		nodes.put(builder.tmpId, builder);
		longIdToIntId.put(builder.sourceId, builder.tmpId);
	}

	public void addEdge(EdgeBuilder<? extends TEdge> builder) {
		EdgeId id = getId(builder);
		if (edges.containsKey(id)) throw new IllegalArgumentException(
				"Graph builder already contains edge builder: [" + builder.getTmpFromId() + ", " +
				builder.getTmpToId() + "]");
		nodeOutgoingEdges.put(builder.getTmpFromId(), builder);
		nodeIncomingEdges.put(builder.getTmpToId(), builder);
		edges.put(id, builder);
	}

	private EdgeId getId(EdgeBuilder<? extends TEdge> builder) {
		return builder.getEdgeId();
	}

	public GraphBuilder<TNode, TEdge> createGraphBuilder() {
		int id = 0;

		GraphBuilder<TNode, TEdge> builder = new GraphBuilder<>();

		Map<Integer, Integer> tmpToFinalId = new HashMap<>();

		for (NodeBuilder<? extends TNode> nodeBuilder : nodes.values()) {
			//StopNode for route nodes have to be set
			if (nodeBuilder instanceof RouteNodeBuilder) {
				RouteNodeBuilder rnb = (RouteNodeBuilder) nodeBuilder;
				if (!tmpToFinalId.containsKey(rnb.getStopNodeTmpId())) {
					throw new IllegalStateException(
							"Stop node builder must be inserted before the corresponding route node builders.");
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
		return builder;
	}

	public Graph<TNode, TEdge> createGraph() {
		GraphBuilder<TNode, TEdge> builder = createGraphBuilder();
		Graph<TNode, TEdge> g = builder.createGraph();

		Map<Class<?>, Long> edgeCounts = g.getAllEdges().stream().collect(groupingBy(Object::getClass, counting()));
		Map<Class<?>, Long> nodeCounts = g.getAllNodes().stream().collect(groupingBy(Object::getClass, counting()));

		LOGGER.debug("Created graph contains following nodes:");
		nodeCounts.forEach((k, v) -> LOGGER.debug(k.getName() + ": " + v));
		LOGGER.debug("Created graph contains following edges:");
		edgeCounts.forEach((k, v) -> LOGGER.debug(k.getName() + ": " + v));
		return g;
	}

	public void printContent() {
		Map<Class<?>, Long> edgeCounts = getAllEdges().stream().collect(groupingBy(Object::getClass, counting()));
		Map<Class<?>, Long> nodeCounts = getAllNodes().stream().collect(groupingBy(Object::getClass, counting()));

		LOGGER.debug("Graph contains following nodes:");
		nodeCounts.forEach((k, v) -> LOGGER.debug(k.getName() + ": " + v));
		LOGGER.debug("Graph contains following edges:");
		edgeCounts.forEach((k, v) -> LOGGER.debug(k.getName() + ": " + v));
	}

	public int getIntIdForSourceId(long sourceId) {
		return longIdToIntId.get(sourceId);
	}

	public boolean containsEdge(int tmpFromId, int tmpToId) {
		return edges.containsKey(new EdgeId(tmpFromId, tmpToId));
	}

	public EdgeBuilder<? extends TEdge> getEdge(int tmpFromId, int tmpToId) {
		return edges.get(new EdgeId(tmpFromId, tmpToId));
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
		return nodes.keySet().stream().max(Integer::compare).get();
	}

	public int getEdgeCount() {
		return edges.size();
	}

	public Collection<EdgeBuilder<? extends TEdge>> getAllEdges() {
		return edges.values();
	}

	@SuppressWarnings("unchecked")
	public <T extends NodeBuilder<? extends TNode>> List<T> getNodesOfType(Class<T> type) {
		return nodes.values().stream().filter(type::isInstance).map(n -> (T) n).collect(toList());
	}

	/**
	 * Get all edge builders that feasible for given {@code mode}.
	 *
	 * @param mode
	 *
	 * @return
	 */
	public List<EdgeBuilder<? extends TEdge>> getFeasibleEdges(TransportMode mode) {
		return edges.values().stream().filter(e -> e.checkFeasibility(mode)).collect(toList());
	}

	/**
	 * Get all nodes which are endpoints of an edge feasible for given {@code mode}.
	 *
	 * @param mode
	 */
	public Set<NodeBuilder<? extends TNode>> getFeasibleNodes(TransportMode mode) {
		Set<NodeBuilder<? extends TNode>> feasibleNodes = new HashSet<>();
		for (EdgeBuilder<? extends TEdge> edge : edges.values()) {
			if (edge.checkFeasibility(mode)) {
				feasibleNodes.add(nodes.get(edge.getTmpFromId()));
				feasibleNodes.add(nodes.get(edge.getTmpToId()));
			}
		}
		return feasibleNodes;
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
		if (isolatedIds == null || isolatedIds.isEmpty()) return 0;
		nodes.keySet().removeAll(isolatedIds);
		return pre - nodes.size();
	}

	/**
	 * Removes all edges satisfying {@code predicate}.
	 *
	 * @param predicate
	 *
	 * @return
	 */
	public int removeEdges(Predicate<EdgeBuilder<? extends TEdge>> predicate) {
		int pre = edges.size();
		for (Iterator<EdgeBuilder<? extends TEdge>> it = edges.values().iterator(); it.hasNext(); ) {
			EdgeBuilder<? extends TEdge> edge = it.next();
			if (predicate.test(edge)) {
				it.remove();
				nodeIncomingEdges.remove(edge.getTmpToId(), edge);
				nodeOutgoingEdges.remove(edge.getTmpFromId(), edge);
			}
		}
		return pre - edges.size();
	}

	public EdgeBuilder<? extends TEdge> remove(EdgeBuilder<? extends TEdge> edge) {
		return remove(getId(edge));
	}

	public EdgeBuilder<? extends TEdge> remove(EdgeId edgeId) {
		EdgeBuilder<? extends TEdge> edge = edges.remove(edgeId);
		if (edge == null) {
			return null;
		} else {
			nodeIncomingEdges.remove(edge.getTmpToId(), edge);
			nodeOutgoingEdges.remove(edge.getTmpFromId(), edge);
			return edge;
		}
	}

	public int removeNodes(Collection<Integer> removedNodes) {
		int counter = 0;
		for (Integer nodeId : removedNodes) {
			if (removeNode(nodeId)) {
				counter++;
			}
		}
		return counter;
	}

	public boolean removeNode(Integer nodeId) {
		if (nodeIncomingEdges.containsKey(nodeId) || nodeOutgoingEdges.containsKey(nodeId)) {
			throw new IllegalStateException("Node can't be removed because existing edges.");
		}
		return nodes.remove(nodeId) != null;
	}

	public TreeMap<Integer, Set<Integer>> getNodesByDegree() {
		return nodes.keySet()
					.stream()
					.collect(groupingBy(id -> nodeIncomingEdges.get(id).size() + nodeOutgoingEdges.get(id).size(),
										TreeMap::new, toSet()));
	}

	public TreeMap<Integer, Set<Integer>> getNodesByOutDegree() {
		return nodes.keySet()
					.stream()
					.collect(groupingBy(id -> nodeOutgoingEdges.get(id).size(), TreeMap::new, toSet()));
	}

	public TreeMap<Integer, Set<Integer>> getNodesByInDegree() {
		return nodes.keySet()
					.stream()
					.collect(groupingBy(id -> nodeIncomingEdges.get(id).size(), TreeMap::new, toSet()));
	}

	public List<EdgeBuilder<? extends TEdge>> getOutgoingEdges(NodeBuilder<? extends Node> node) {
		return nodeOutgoingEdges.get(node.tmpId);
	}

	public List<EdgeBuilder<? extends TEdge>> getIncomingEdges(NodeBuilder<? extends Node> node) {
		return nodeIncomingEdges.get(node.tmpId);
	}

	public List<EdgeBuilder<? extends TEdge>> getOutgoingEdges(int tmpId) {
		return nodeOutgoingEdges.get(tmpId);
	}

	public List<EdgeBuilder<? extends TEdge>> getIncomingEdges(int tmpId) {
		return nodeIncomingEdges.get(tmpId);
	}

	public Collection<NodeBuilder<? extends TNode>> getAllNodes() {
		return nodes.values();
	}

	public void clear() {
		nodes.clear();
		edges.clear();
		longIdToIntId.clear();
	}

	public boolean containsEdge(EdgeBuilder<?> edge) {
		return containsEdge(edge.getTmpFromId(), edge.getTmpToId());
	}

	public Map<EdgeId, EdgeBuilder<? extends TEdge>> getEdgesById() {
		return Collections.unmodifiableMap(edges);
	}

	public void addEdges(Collection<? extends EdgeBuilder<? extends TEdge>> virtualEdges) {
		virtualEdges.forEach(this::addEdge);
	}
}
