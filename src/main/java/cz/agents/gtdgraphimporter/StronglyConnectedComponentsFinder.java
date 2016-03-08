package cz.agents.gtdgraphimporter;


import cz.agents.basestructures.Edge;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.Node;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for computing the strongly connected components using Kosaraju's algorithm
 */
public class StronglyConnectedComponentsFinder {

	/**
	 * Computes strongly connected components.
	 *
	 * @param nodeIds Ids of nodes
	 * @param edges   Directed edges in the graph represented as adjacency lists. Stored in Map sourceNode > set of successors
	 * @return List of sets of ids, each set corresponds to one strongly connected component. The sets are sorted according to their size
	 */
	public static List<HashSet<Integer>> getStronglyConnectedComponnentsSortedBySize(Set<Integer> nodeIds,
			Map<Integer, Set<Integer>> edges) {
		return new KosarajuSCCCompouter(nodeIds, edges).getAllStronglyConnectedComponentsSortedBySize();
	}

	/**
	 * Computes strongly connected components for given graph.
	 *
	 * @return List of sets of ids, each set corresponds to one strongly connected component. The sets are sorted according to their size
	 */
	public static List<HashSet<Integer>> getStronglyConnectedComponnentsSortedBySize(
			Graph<? extends Node, ? extends Edge> graph) {

		Set<Integer> nodeIds = graph.getAllNodes().stream().map(node -> node.id).collect(Collectors.toSet());

		Map<Integer, Set<Integer>> outgoing = new HashMap<>();

		for (Edge edge : graph.getAllEdges()) {
			addElementToSetInMapValue(outgoing, edge.fromId, edge.toId);
		}

		return new KosarajuSCCCompouter(nodeIds, outgoing).getAllStronglyConnectedComponentsSortedBySize();
	}

	/**
	 * Computes strongly connected components.
	 *
	 * @param nodes Nodes
	 * @param edges Edges
	 * @return List of sets of ids, each set corresponds to one strongly connected component. The sets are sorted according to their size
	 */
	public static List<HashSet<Integer>> getStronglyConnectedComponnentsSortedBySize(Collection<? extends Node> nodes,
			Collection<? extends Edge> edges) {

		Set<Integer> nodeIds = nodes.stream().map(node -> node.id).collect(Collectors.toSet());

		Map<Integer, Set<Integer>> outgoing = new HashMap<>();

		for (Edge edge : edges) {
			addElementToSetInMapValue(outgoing, edge.fromId, edge.toId);
		}

		return new KosarajuSCCCompouter(nodeIds, outgoing).getAllStronglyConnectedComponentsSortedBySize();
	}

	/**
	 * Computes strongly connected components. Nodes: ((in-degree = 0) && (out-degree = 0)), are not present in any component, because we have no evidence about them from the set of edges
	 *
	 * @param edges Edges
	 * @return List of sets of ids, each set corresponds to one strongly connected component. The sets are sorted according to their size
	 */
	public static List<HashSet<Integer>> getStronglyConnectedComponnentsSortedBySize(Collection<? extends Edge> edges) {

		HashSet<Integer> nodeIds = new HashSet<>();

		Map<Integer, Set<Integer>> outgoing = new HashMap<>();

		for (Edge edge : edges) {
			nodeIds.add(edge.fromId);
			nodeIds.add(edge.toId);
			addElementToSetInMapValue(outgoing, edge.fromId, edge.toId);
		}

		return new KosarajuSCCCompouter(nodeIds, outgoing).getAllStronglyConnectedComponentsSortedBySize();
	}

	private static <K, V> void addElementToSetInMapValue(Map<K, Set<V>> map, K key, V element) {

		if (!map.containsKey(key)) {
			map.put(key, new HashSet<>());
		}

		map.get(key).add(element);
	}

	private static final class KosarajuSCCCompouter {

		private Set<Integer> nodeIds;
		private Map<Integer, Set<Integer>> outgoingEdges;
		private Map<Integer, Set<Integer>> incommingEdges;
		private HashSet<Integer> alreadyInOrdering;
		private Stack<Integer> ordering;
		private HashSet<Integer> open;

		private KosarajuSCCCompouter(Set<Integer> nodeIds, Map<Integer, Set<Integer>> edgesAsAdjacencyLists) {
			this.nodeIds = nodeIds;
			this.outgoingEdges = edgesAsAdjacencyLists;
			this.incommingEdges = buildIncommingEdges(edgesAsAdjacencyLists);

			open = new HashSet<>();
			alreadyInOrdering = new HashSet<>();
			ordering = new Stack<>();

		}

		private Map<Integer, Set<Integer>> buildIncommingEdges(Map<Integer, Set<Integer>> edgesAsAdjacencyLists) {

			Map<Integer, Set<Integer>> incommingEdges = new HashMap<>();

			for (Map.Entry<Integer, Set<Integer>> entry : edgesAsAdjacencyLists.entrySet()) {
				Integer fromId = entry.getKey();
				for (Integer toId : entry.getValue()) {
					addElementToSetInMapValue(incommingEdges, toId, fromId);
				}
			}

			return incommingEdges;
		}

		private ArrayList<HashSet<Integer>> getAllStronglyConnectedComponentsSortedBySize() {

			ArrayList<Integer> points = new ArrayList<>(nodeIds);

			int index = 0;
			while (alreadyInOrdering.size() < points.size()) {

				while (index < points.size() && (alreadyInOrdering.contains(points.get(index)))) {
					index++;
				}
				if (index == points.size()) {
					break;
				}
				DFSextwalk(points.get(index), outgoingEdges, incommingEdges);
			}

			HashSet<Integer> alreadyInSomeRegion = new HashSet<>();
			Stack<Integer> stack2 = new Stack<>();
			open = new HashSet<>();

			ArrayList<HashSet<Integer>> components = new ArrayList<>();

			HashSet<Integer> component;

			while (!ordering.isEmpty()) {

				boolean empty = false;
				Integer currentStarting = null;
				do {
					if (ordering.isEmpty()) {
						empty = true;
						break;
					} else {
						currentStarting = ordering.pop();
					}

                    /**/
				} while (alreadyInSomeRegion.contains(currentStarting));
				if (empty) {
					break;
				}
				component = new HashSet<>();
				components.add(component);

				stack2.add(currentStarting);

				while (!stack2.isEmpty()) {
					Integer current = stack2.pop();
					component.add(current);
					alreadyInSomeRegion.add(current);

					ArrayList<Integer> succ = new ArrayList<>();
					if (incommingEdges.get(current) != null) {
						succ.addAll(incommingEdges.get(current));
					}

					for (int i = 0; i < succ.size(); i++) {
						if (!alreadyInSomeRegion.contains(succ.get(i)) && !open.contains(succ.get(i))) {
							stack2.push(succ.get(i));
							open.add(succ.get(i));
						}
					}
				}
			}

			Collections.sort(components, new Comparator<HashSet<Integer>>() {
				@Override
				public int compare(HashSet<Integer> o1, HashSet<Integer> o2) {
					return o2.size() - o1.size();
				}
			});

			return components;

		}

		private void DFSextwalk(Integer root, Map<Integer, Set<Integer>> outgoing,
				Map<Integer, Set<Integer>> incomming) {
			//System.out.println("current point: "+current.id);
			open.add(root);
			//System.out.println("starting walk with: "+root.id);

			Stack<Integer> stack = new Stack<>();
			HashMap<Integer, ArrayList<Integer>> successors = new HashMap<>();

			stack.add(root);

			while (!stack.isEmpty()) {

				Integer current = stack.peek();
				open.add(current);

				if (!successors.containsKey(current)) {

					ArrayList<Integer> succ = new ArrayList<>();

					if (outgoing.get(current) != null) {
						succ.addAll(outgoing.get(current));
					}
					successors.put(current, succ);

				}

				if (!successors.get(current).isEmpty()) {
					Integer next = successors.get(current).remove(0);
					if (!open.contains(next)) {
						stack.push(next);
					}

				} else {
					stack.pop();
					ordering.push(current);
					alreadyInOrdering.add(current);

				}
			}
		}
	}

}
