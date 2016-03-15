/* This code is owned by Umotional s.r.o. (IN: 03974618). All Rights Reserved. */
package cz.agents.gtdgraphimporter.structurebuilders;

import cz.agents.basestructures.Edge;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.Node;

import java.util.*;

public class GraphBuilder<TNode extends Node, TEdge extends Edge> {

	private Map<Long, Integer> longIdToIntId = new HashMap<>();

	private Map<Integer, TNode> nodesByNodeId = new LinkedHashMap<>();
	private Map<EdgeId, TEdge> edgeByFromToNodeIds = new LinkedHashMap<>();
	private Map<Integer, List<TEdge>> nodeOutcomingEdges = new HashMap<>();
	private Map<Integer, List<TEdge>> nodeIncomingEdges = new HashMap<>();

	public GraphBuilder() {
	}

	/**
	 * Return internal <code>int</code> id for the node specified by source id
	 *
	 * @param osmId
	 * 		<code>long</code>
	 *
	 * @return <code>int</code> id for given node
	 */
	public int getIntIdForosmId(long osmId) {
		return longIdToIntId.get(osmId);
	}

	/**
	 * Add a collection of roadNodes to the graph
	 *
	 * @param nodes
	 * 		Nodes
	 */
	public void addNodes(Collection<TNode> nodes) {
		for (TNode node : nodes) {
			addNode(node);
		}
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
		nodeOutcomingEdges.put(node.id, new ArrayList<>());
		nodeIncomingEdges.put(node.id, new ArrayList<>());

		longIdToIntId.put(node.osmId, node.id);

	}

	/**
	 * {@inheritDoc}
	 */
	public boolean containsNode(TNode node) {
		return containsNode(node.id);
	}

	/**
	 * {@inheritDoc}
	 */

	public boolean containsNode(int nodeId) {
		return getNode(nodeId) != null;
	}

	/**
	 * {@inheritDoc}
	 */
	public TNode getNode(int nodeId) {
		return nodesByNodeId.get(nodeId);
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<TNode> getAllNodes() {
		return nodesByNodeId.values();
	}

	/**
	 * Add a collection of edges to the graph
	 *
	 * @param edges
	 * 		Edges
	 */
	public void addEdges(Collection<? extends TEdge> edges) {
		for (TEdge edge : edges) {
			if (!containsEdge(edge.fromId, edge.toId)) {
				addEdge(edge);
			}
		}
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

		EdgeId edgeId = new EdgeId(edge.fromId, edge.toId);

		assert !edgeByFromToNodeIds.containsKey(edgeId) : "Edge has not to exist yet";

		List<TEdge> outcomingEdgesFromNode = nodeOutcomingEdges.get(edge.fromId);
		List<TEdge> incomingEdgesToNode = nodeIncomingEdges.get(edge.toId);

		outcomingEdgesFromNode.add(edge);
		incomingEdgesToNode.add(edge);

		edgeByFromToNodeIds.put(edgeId, edge);
		nodeOutcomingEdges.put(edge.fromId, outcomingEdgesFromNode);
		nodeIncomingEdges.put(edge.toId, incomingEdgesToNode);
	}

	public boolean containsEdge(TEdge edge) {
		return containsEdge(edge.fromId, edge.toId);
	}

	public boolean containsEdge(int fromId, int toId) {
		return getEdge(fromId, toId) != null;
	}

	public TEdge getEdge(int fromId, int toId) {
		EdgeId edgeId = new EdgeId(fromId, toId);
		return edgeByFromToNodeIds.get(edgeId);
	}

	public List<TEdge> getInEdges(TNode node) {
		return getInEdges(node.id);
	}

	public List<TEdge> getInEdges(int nodeId) {
		return nodeIncomingEdges.get(nodeId);
	}

	public List<TEdge> getOutEdges(TNode node) {
		return getOutEdges(node.id);
	}

	public List<TEdge> getOutEdges(int nodeId) {
		return nodeOutcomingEdges.get(nodeId);
	}

	public Collection<TEdge> getAllEdges() {
		return edgeByFromToNodeIds.values();
	}

	/**
	 * Creates a final graph and clears the builder structures.
	 *
	 * @return Final <code>Graph</code>
	 */
	public Graph<TNode, TEdge> createGraph() {

		Graph<TNode, TEdge> graph = dumpCurrentGraph();

		this.nodesByNodeId = new HashMap<>();
		this.edgeByFromToNodeIds = new HashMap<>();
		this.nodeOutcomingEdges = new HashMap<>();
		this.nodeIncomingEdges = new HashMap<>();
		this.longIdToIntId = new HashMap<>();

		return graph;
	}

	/**
	 * Creates a final graph and keeps the structures ready for further building of the graph.
	 *
	 * @return current state of the <code>Graph</code>
	 */
	public Graph<TNode, TEdge> dumpCurrentGraph() {

		// check node ids sequence
		for (int i = 0; i < nodesByNodeId.keySet().size(); i++) {

			TNode node = nodesByNodeId.get(i);
			if (node == null) {
				throw new NoSuchElementException(" Node with id " + i + " not present! The sequence of ndoe id must " +
						"start with 0 and end with 'numOfNodes-1'");
			}
		}

		ArrayList<TNode> nodesByNodeIdList = new ArrayList<>(nodesByNodeId.keySet().size());
		for (int i = 0; i < nodesByNodeId.keySet().size(); i++) {
			nodesByNodeIdList.add(nodesByNodeId.get(i));
		}

		int[] outgoingPositions = new int[nodesByNodeId.keySet().size() + 1];
		ArrayList<TEdge> outgoingEdges = new ArrayList<>(nodesByNodeId.keySet().size());
		int[] incomingPositions = new int[nodesByNodeId.keySet().size() + 1];
		ArrayList<TEdge> incomingEdges = new ArrayList<>(nodesByNodeId.keySet().size());

		// OUTGOING EDGES
		// iterate over all roadNodes
		int j = 0; // outgoing edges id
		for (int k = 0; k < nodesByNodeIdList.size(); k++) {

			// log.debug("---------");
			// log.debug("NODE: " + allNodesByNodeId[k]);

			// assign outgoing position
			outgoingPositions[k] = j;

			// iterate over outgoing edges
			for (TEdge edge : nodeOutcomingEdges.get(k)) {

				outgoingEdges.add(edge);
				j++;
			}
		}
		// set numberOfNodes+1 of outgoing positions to current edge id as indentation
		outgoingPositions[nodesByNodeId.keySet().size()] = j;

		// INCOMING EDGES
		// iterate over all roadNodes
		int l = 0; // outgoing edges id
		for (int k = 0; k < nodesByNodeIdList.size(); k++) {

			// assign incoming position
			incomingPositions[k] = l;

			// iterate over incoming edges
			for (TEdge edge : nodeIncomingEdges.get(k)) {

				incomingEdges.add(edge);
				l++;
			}
		}
		// set numberOfNodes+1 of incoming positions to current edge id as indentation
		incomingPositions[nodesByNodeId.keySet().size()] = l;

		return new Graph<>(nodesByNodeIdList, outgoingPositions, outgoingEdges, incomingPositions, incomingEdges);

	}

	private class EdgeId {

		private static final long serialVersionUID = 4716865102995519001L;

		public final long fromNodeId;
		public final long toNodeId;

		public EdgeId(long fromNodeId, long toNodeId) {
			super();
			this.fromNodeId = fromNodeId;
			this.toNodeId = toNodeId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (fromNodeId ^ (fromNodeId >>> 32));
			result = prime * result + (int) (toNodeId ^ (toNodeId >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			EdgeId other = (EdgeId) obj;
			if (fromNodeId != other.fromNodeId) return false;
			return toNodeId == other.toNodeId;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return String.format("%d-->%d", this.fromNodeId, this.toNodeId);
		}

		public long getFromNodeId() {
			return fromNodeId;
		}

		public long getToNodeId() {
			return toNodeId;
		}
	}
}
