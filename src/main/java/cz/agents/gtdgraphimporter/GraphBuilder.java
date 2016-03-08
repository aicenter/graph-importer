package cz.agents.gtdgraphimporter;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import cz.agents.basestructures.Edge;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.Node;

import java.util.*;

public class GraphBuilder<TNode extends Node, TEdge extends Edge> {

	private final Map<Long, Integer> longIdToIntId = new HashMap<>();
	private final Map<Integer, TNode> nodesByNodeId = new HashMap<>();
	/**
	 * [fromId,toId] -> Edge
	 */
	private final Table<Integer, Integer, TEdge> graph = HashBasedTable.create();

	public GraphBuilder() {
	}

	/**
	 * Return internal <code>int</code> id for the node specified by source id
	 *
	 * @param sourceId
	 * 		<code>long</code>
	 *
	 * @return <code>int</code> id for given node
	 */
	public int getIntIdForSourceId(long sourceId) {
		return longIdToIntId.get(sourceId);
	}

	public boolean containsNode(long sourceId) {
		return longIdToIntId.containsKey(sourceId);
	}

	/**
	 * Add a collection of nodes to the graph
	 *
	 * @param nodes
	 * 		Nodes
	 */
	public void addNodes(Collection<TNode> nodes) {
		nodes.forEach(this::addNode);
	}

	/**
	 * Adds node to graph.
	 *
	 * @param node
	 * 		<code>TNode</code>
	 */
	public void addNode(TNode node) {

		if (nodesByNodeId.containsKey(node.id)) {
			throw new IllegalArgumentException("Node with this int id already present! Can not import node");
		}

		nodesByNodeId.put(node.id, node);
		longIdToIntId.put(node.osmId, node.id);

	}

	/**
	 * Add a collection of edges to the graph
	 *
	 * @param edges
	 * 		Edges
	 */
	public void addEdges(Collection<TEdge> edges) {
		edges.stream().filter(edge -> !containsEdge(edge.fromId, edge.toId)).forEach(this::addEdge);
	}

	/**
	 * Adds edge to graph.
	 *
	 * @param edge
	 * 		<code>TEdge</code>
	 */
	public void addEdge(TEdge edge) { // we may think about "creating edges" inside graph builder because of ids

		assert nodesByNodeId.get(edge.fromId) != null && nodesByNodeId.get(edge.toId) != null : "Node has to be in " +
				"graph builder before inserting edge";
		assert !graph.contains(edge.fromId, edge.toId) : "Edge has not to exist yet";

		graph.put(edge.fromId, edge.toId, edge);
	}

	/**
	 * Creates a final graph and clears the builder structures.
	 *
	 * @return Final <code>Graph</code>
	 */
	public Graph<TNode, TEdge> createGraph() {

		Graph<TNode, TEdge> graph = dumpCurrentGraph();

		this.nodesByNodeId.clear();
		this.longIdToIntId.clear();
		this.graph.clear();

		return graph;
	}

	/**
	 * Creates a final graph and keeps the structures ready for further building of the graph.
	 *
	 * @return current state of the <code>Graph</code>
	 */
	public Graph<TNode, TEdge> dumpCurrentGraph() {
		checkNodeIdsSequence();

		int n = nodesByNodeId.size();

		int[] outgoingPositions = new int[n + 1];
		ArrayList<TEdge> outgoingEdges = new ArrayList<>(n);
		fillEdgeStructures(graph.rowMap(), outgoingPositions, outgoingEdges);

		int[] incomingPositions = new int[n + 1];
		ArrayList<TEdge> incomingEdges = new ArrayList<>(n);
		fillEdgeStructures(graph.columnMap(), incomingPositions, incomingEdges);

		ArrayList<TNode> nodesByNodeIdList = createNodeList();
		return new Graph<>(nodesByNodeIdList, outgoingPositions, outgoingEdges, incomingPositions, incomingEdges);

	}

	/**
	 * @param edgeMap
	 * 		Map of maps where key of the first map is the id of the node for which the structures are filled - {@code
	 * 		to} node for incoming edges and {@code from} node for outgoing edges.
	 * @param positions
	 * @param edges
	 */
	private void fillEdgeStructures(Map<Integer, Map<Integer, TEdge>> edgeMap, int[] positions, ArrayList<TEdge>
			edges) {
		// iterate over all nodes
		int position = 0;
		for (int k = 0; k < nodesByNodeId.size(); k++) {
			// assign position
			positions[k] = position;

			// iterate over edges
			for (TEdge edge : edgeMap.getOrDefault(k, Collections.emptyMap()).values()) {
				edges.add(edge);
				position++;
			}
		}
		// set numberOfNodes+1 of incoming positions to current edge id as indentation
		positions[nodesByNodeId.size()] = position;
	}

	private ArrayList<TNode> createNodeList() {
		ArrayList<TNode> nodesByNodeIdList = new ArrayList<>(nodesByNodeId.size());
		for (int i = 0; i < nodesByNodeId.size(); i++) {
			nodesByNodeIdList.add(nodesByNodeId.get(i));
		}
		return nodesByNodeIdList;
	}

	private void checkNodeIdsSequence() {
		for (int i = 0; i < nodesByNodeId.size(); i++) {
			TNode node = nodesByNodeId.get(i);
			if (node == null) {
				throw new NoSuchElementException(" Node with id " + i + " not present! The sequence of ndoe id must " +
						"start with 0 and end with 'numOfNodes-1'");
			}
		}
	}

	public void replaceEdge(TEdge edge) {
		graph.put(edge.fromId, edge.toId, edge);
	}

	public boolean containsNode(TNode node) {
		return containsNode(node.id);
	}

	public boolean containsNode(int nodeId) {
		return getNode(nodeId) != null;
	}

	public TNode getNode(int nodeId) {
		return nodesByNodeId.get(nodeId);
	}

	public Collection<TNode> getAllNodes() {
		return nodesByNodeId.values();
	}

	public boolean containsEdge(TEdge edge) {
		return containsEdge(edge.fromId, edge.toId);
	}

	public boolean containsEdge(int fromId, int toId) {
		return getEdge(fromId, toId) != null;
	}

	public TEdge getEdge(int fromId, int toId) {
		return graph.get(fromId, toId);
	}

	public Collection<TEdge> getInEdges(TNode node) {
		return getInEdges(node.id);
	}

	public Collection<TEdge> getInEdges(int nodeId) {
		return graph.column(nodeId).values();
	}

	public Collection<TEdge> getOutEdges(TNode node) {
		return getOutEdges(node.id);
	}

	public Collection<TEdge> getOutEdges(int nodeId) {
		return graph.row(nodeId).values();
	}

	public Collection<TEdge> getAllEdges() {
		return graph.values();
	}

}
