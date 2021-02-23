package cz.cvut.fel.aic.graphimporter.structurebuilders.internal;

import com.google.common.collect.Lists;
import cz.cvut.fel.aic.geographtools.EdgeId;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;
import cz.cvut.fel.aic.graphimporter.structurebuilders.TmpGraphBuilder;
import java.util.*;
import static java.util.stream.Collectors.toList;

/**
 * @author Zdenek Bousa
 */
public class SimplifiedInternalEdgeBuilder {
	public static final Comparator<SimplifiedInternalEdgeBuilder> LENGTH_COMPARATOR = ((o1, o2) -> Double
			.compare(o1.length, o2.length));
	private int tmpFromId;
	private int tmpToId;

	private final SimplifiedInternalEdgeBuilder.Type type;

	private final LinkedList<Integer> removedNodes = new LinkedList<>();

	private double length;
	private List<GPSLocation> shape;

	private SimplifiedInternalEdgeBuilder(int f, int n, int t, SimplifiedInternalEdgeBuilder.Type type) {
		this.type = type;
		this.tmpFromId = f;
		this.tmpToId = t;
		removedNodes.add(n);
	}

	/**
	 * Adds the {@code endNode} on the appropriate end and add the
	 * {@code removedNode} to the {@code removedNodes}
	 *
	 * @param removedNode
	 * @param endNode
	 */
	public void add(int removedNode, int endNode) {
		if (removedNode == getTmpFromId()) {
			setTmpFromId(endNode);
			removedNodes.addFirst(removedNode);
		} else {
			assert removedNode == getTmpToId() : "Removed node has to be an endpoint.";
			setTmpToId(endNode);
			removedNodes.addLast(removedNode);
		}
	}

	/**
	 * Appends {@code postfix} after this.
	 *
	 * @param postfix
	 */
	public final void append(SimplifiedInternalEdgeBuilder postfix) {
		assert this.getTmpToId() == postfix.getTmpFromId() : "The endpoints must be equal.";
		removedNodes.addLast(this.getTmpToId());
		postfix.removedNodes.forEach(removedNodes::addLast);
		setTmpToId(postfix.getTmpToId());
	}

	/**
	 * Appends reversed {@code postfix} after this.
	 *
	 * @param postfix
	 */
	public final void appendReversed(SimplifiedInternalEdgeBuilder postfix) {
		assert this.getTmpToId() == postfix.getTmpToId() : "The endpoints must be equal.";
		removedNodes.addLast(this.getTmpToId());
		Lists.reverse(postfix.removedNodes).forEach(removedNodes::addLast);
		setTmpToId(postfix.getTmpFromId());
	}

	/**
	 * Prepend reversed {@code prefix} before this.
	 *
	 * @param prefix
	 */
	public void prependReversed(SimplifiedInternalEdgeBuilder prefix) {
		assert this.getTmpFromId() == prefix.getTmpFromId() : "The endpoints must be equal.";
		removedNodes.addFirst(this.getTmpFromId());
		prefix.removedNodes.forEach(removedNodes::addFirst);
		setTmpFromId(prefix.getTmpToId());
	}

	public static SimplifiedInternalEdgeBuilder createOneway(int f, int n, int t) {
		return new SimplifiedInternalEdgeBuilder(f, n, t, SimplifiedInternalEdgeBuilder.Type.ONEWAY);
	}

	public static SimplifiedInternalEdgeBuilder createTwoWay(int f, int n, int t) {
		return new SimplifiedInternalEdgeBuilder(f, n, t, SimplifiedInternalEdgeBuilder.Type.TWO_WAY);
	}

	/**
	 * Get number of nodes.
	 *
	 * @return
	 */
	public int numberOfNodes() {
		assert !removedNodes.isEmpty() : "Removed nodes can't be empty";
		return removedNodes.size() + 2;
	}

	public int numberOfEdges() {
		return removedNodes.size() + 1;
	}

	public boolean isCircle() {
		return getTmpFromId() == getTmpToId();
	}

	/**
	 * Splits the edges to be simplified into defined number of parts (currently
	 * only 1 or 2) and create appropriate simplified edges. If this is a two-way
	 * also the opposite edges are created. Therefore, the number of created edges
	 * is equal to {@code numberOfParts} if this is one-way and
	 * {@code 2*numberOfParts} if this is a two-way.
	 *
	 * @param numberOfParts
	 * @param graph
	 *			Graph from which are retrieved locations of nodes and properties
	 *			of the edges.
	 * @param removedEdges
	 * @return
	 */
	public List<InternalEdgeBuilder> build(int numberOfParts, TmpGraphBuilder<InternalNode, InternalEdge> graph,
			Map<EdgeId, InternalEdgeBuilder> removedEdges) {
		if (numberOfParts < 1)
			throw new IllegalArgumentException("Number of parts must be positive.");
		if (numberOfParts > 3)
			throw new UnsupportedOperationException("More parts than three aren't supported now.");

		int numberOfNodes = numberOfNodes();
		List<Integer> nodes = new ArrayList<>(numberOfNodes);
		nodes.add(getTmpFromId());
		nodes.addAll(removedNodes);
		nodes.add(getTmpToId());

		switch (numberOfParts) {
			case 1 :
				return build(nodes, graph, removedEdges);
			case 2 :
				List<InternalEdgeBuilder> result2 = new ArrayList<>(2);
				result2.addAll(build(nodes.subList(0, numberOfNodes / 2 + 1), graph, removedEdges));
				result2.addAll(build(nodes.subList(numberOfNodes / 2, numberOfNodes), graph, removedEdges));
				return result2;
			case 3 :
				List<InternalEdgeBuilder> result3 = new ArrayList<>(3);
				result3.addAll(build(nodes.subList(0, numberOfNodes / 3 + 1), graph, removedEdges));
				result3.addAll(build(nodes.subList(numberOfNodes / 3, 2 * numberOfNodes / 3 + 1), graph, removedEdges));
				result3.addAll(build(nodes.subList(2 * numberOfNodes / 3, numberOfNodes), graph, removedEdges));
				return result3;
		}
		throw new IllegalStateException("Not reachable");
	}

	private List<InternalEdgeBuilder> build(List<Integer> nodes, TmpGraphBuilder<InternalNode, InternalEdge> graph,
			Map<EdgeId, InternalEdgeBuilder> removedEdges) {
		int length = (int) Math.round(calculateLength(nodes, graph));
		List<GPSLocation> shape = toShape(nodes, graph);
		List<InternalEdgeBuilder> result = new ArrayList<>(2);
		switch (type) {
			case TWO_WAY :
				Collections.reverse(shape);
				result.add(createEdge(Lists.reverse(nodes), length, removedEdges, shape));
			case ONEWAY :
				result.add(createEdge(nodes, length, removedEdges, shape));
		}
		return result;
	}

	private List<GPSLocation> toShape(List<Integer> nodes, TmpGraphBuilder<InternalNode, InternalEdge> graph) {
		return nodes.stream().map(graph::getNode).map(b -> b.location).collect(toList());
	}

	private InternalEdgeBuilder createEdge(List<Integer> nodes, int length,
			Map<EdgeId, InternalEdgeBuilder> removedEdges, List<GPSLocation> shape) {
		int from = nodes.get(0);
		int m = nodes.get(1);
		int to = nodes.get(nodes.size() - 1);
		InternalEdgeBuilder referenceEdge = removedEdges.get(new EdgeId(from, m));
		return createEdge(from, to, length, referenceEdge, shape);
	}

	private InternalEdgeBuilder createEdge(int from, int to, int length, InternalEdgeBuilder referenceEdge,
			List<GPSLocation> shape) {
		return referenceEdge.copy(from, to, length, shape);
	}

	private double calculateLength(List<Integer> nodes, TmpGraphBuilder<InternalNode, InternalEdge> graph) {
		double length = 0;
		GPSLocation prev = graph.getNode(nodes.get(0)).location;
		for (int i = 1; i < nodes.size(); i++) {
			GPSLocation curr = graph.getNode(nodes.get(i)).location;
			length += GPSLocationTools.computeDistanceAsDouble(prev, curr);
			prev = curr;
		}
		return length;
	}

	public double calculateAndSetLength(TmpGraphBuilder<InternalNode, InternalEdge> graph) {
		List<Integer> nodes = new ArrayList<>(numberOfNodes());
		nodes.add(getTmpFromId());
		nodes.addAll(removedNodes);
		nodes.add(getTmpToId());
		double l = calculateLength(nodes, graph);
		this.length = l;
		return l;
	}

	public Integer getLastRemovedNode() {
		return removedNodes.getLast();
	}

	public Integer getFirstRemovedNode() {
		return removedNodes.getFirst();
	}

	public int getTmpFromId() {
		return tmpFromId;
	}

	public void setTmpFromId(int tmpFromId) {
		this.tmpFromId = tmpFromId;
	}

	public int getTmpToId() {
		return tmpToId;
	}

	public void setTmpToId(int tmpToId) {
		this.tmpToId = tmpToId;
	}

	public double getLength() {
		return length;
	}

	public boolean equalType(SimplifiedInternalEdgeBuilder b) {
		return type == b.type;
	}

	public LinkedList<Integer> getRemovedNodes() {
		return removedNodes;
	}

	public boolean isOneWay() {
		return type == SimplifiedInternalEdgeBuilder.Type.ONEWAY;
	}

	public EdgeId getEdgeId() {
		return new EdgeId(tmpFromId, tmpToId);
	}

	public EdgeId getOppositeEdgeId() {
		return new EdgeId(tmpToId, tmpFromId);
	}

	private static enum Type {
		ONEWAY, TWO_WAY
	}
}
