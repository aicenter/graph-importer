/* 
 * Copyright (C) 2017 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.graphimporter;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;

import org.apache.log4j.Logger;

import cz.cvut.fel.aic.geographtools.Edge;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.GraphBuilder;
import cz.cvut.fel.aic.geographtools.Node;
import cz.cvut.fel.aic.geographtools.util.StronglyConnectedComponentsFinder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.EdgeBuilder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.TmpGraphBuilder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.client.EdgeFactory;
import cz.cvut.fel.aic.graphimporter.structurebuilders.client.NodeFactory;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalEdge;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalNode;

/**
 * Instead of {@code cz.agents.gtdgraphimporter.GTDGraphBuilder} Lighter version
 * of it. Preparation for RoadEdgeExtended
 *
 * @author Zdenek Bousa
 */
public class GraphCreator<N extends Node, E extends Edge> {
	private static final Logger LOGGER = Logger.getLogger(GraphCreator.class);

	private final NodeFactory<N> nodeFactory;

	private final EdgeFactory<N,E> edgeFactory;

	private final boolean serializationOn;

	private final boolean simplificationOn;

	protected TmpGraphBuilder<InternalNode, InternalEdge> builder;

	private final Importer importer;

	public GraphCreator(boolean serializationOn, boolean simplificationOn, Importer importer,
			NodeFactory<N> nodeFactory, EdgeFactory<N, E> edgeFactory) {
		this.serializationOn = serializationOn;
		this.importer = importer;
		this.simplificationOn = simplificationOn;
		this.nodeFactory = nodeFactory;
		this.edgeFactory = edgeFactory;
	}

	/**
	 * init map
	 *
	 * @return map data with simulation graph
	 */
	public Graph<N, E> getMap() {
		Graph<N, E> graph;

		File serializedEdgesFile = new File(importer.getSerializedGraphName());

		if (serializationOn) {
			try {
				graph = deserializeGraph(serializedEdgesFile);
			} catch (Exception ex) {
				LOGGER.warn("Cannot perform deserialization of the cached graphs:" + ex.getMessage());
				LOGGER.warn("Generating graphs from the OSM");
				graph = build();
				removePreviousSerializedGraph(importer.getSerializedBasePath());
				serializeGraph(graph, serializedEdgesFile);
			}
		} else {
			graph = build();
		}

		return graph;
	}

	/**
	 * Construct road graph
	 *
	 * @return Graph that has one main strong component and might have been
	 *		 simplified (impact on visio - more sharp curves)
	 */
	private Graph<N, E> build() {
		builder = importer.loadGraph();

//		removeMinorComponents(builder);

		// TODO: Simplifier - make switch for Visio and for Simulation.
		// TODO: Properly handle RoadEdgeExtended - find opposite way and uniqueWayId
		LOGGER.debug(
				"Graph [#nodes=" + builder.getNodeCount() + ", #edges=" + builder.getEdgeCount() + "] simplification");

		if (simplificationOn) {
			GraphSimplifier.simplify(builder, Collections.emptySet()); // not working for RoadExtended
		}

		Graph<InternalNode, InternalEdge> graph = builder.createGraph();

		return exportGraph(graph);
	}

	/**
	 * Removes from the {@code osmGraph} all nodes and edges that are not in the
	 * main component for any mode.
	 *
	 * @param osmGraph
	 *			osm graph with multiple strong components
	 */
	private TmpGraphBuilder<InternalNode, InternalEdge> removeMinorComponents(
			TmpGraphBuilder<InternalNode, InternalEdge> osmGraph) {
		LOGGER.debug("Calculating main components for all modes...");
		Set<Integer> mainComponent = getMainComponent(osmGraph);

		Predicate<EdgeBuilder<InternalEdge, InternalNode>> filter = edge -> !mainComponent.contains(edge.getTmpFromId())
				|| !mainComponent.contains(edge.getTmpToId());

		int removedEdges = osmGraph.removeEdges(filter);
		LOGGER.debug("Removed " + removedEdges + " edges.");

		int removedNodes = osmGraph.removeIsolatedNodes();
		LOGGER.debug("Removed " + removedNodes + " nodes.");
		LOGGER.debug("Nodes by degree: ");
		osmGraph.getNodesByDegree().forEach((k, v) -> LOGGER.debug(k + "->" + v.size()));
		return osmGraph;
	}

	/**
	 * Main strong component
	 */
	private Set<Integer> getMainComponent(TmpGraphBuilder<InternalNode, InternalEdge> graph) {
		return getMainComponent(graph.getAllEdges());
	}

	/**
	 * Find strong component by size
	 */
	private Set<Integer> getMainComponent(Collection<EdgeBuilder<InternalEdge, InternalNode>> edges) {
		Set<Integer> nodeIds = new HashSet<>();
		Map<Integer, Set<Integer>> edgeIds = new HashMap<>();
		for (EdgeBuilder<InternalEdge, InternalNode> edgeExtendedBuilder : edges) {
			int fromId = edgeExtendedBuilder.getTmpFromId();
			int toId = edgeExtendedBuilder.getTmpToId();
			nodeIds.add(fromId);
			nodeIds.add(toId);
			Set<Integer> outgoing = edgeIds.computeIfAbsent(fromId, k -> new HashSet<>());
			outgoing.add(toId);
		}
		return StronglyConnectedComponentsFinder.getStronglyConnectedComponentsSortedBySize(nodeIds, edgeIds).get(0);
	}

	@Override
	public String toString() {
		return "RoadNetworkGraphBuilder{" +
		// "allowedOsmModes=" + allowedOsmModes +
				'}';
	}

	private Graph<N, E> exportGraph(Graph<? extends InternalNode, ? extends InternalEdge> graph) {
		GraphBuilder<N, E> finalGraphBuilder = new GraphBuilder<>();

		for (InternalNode internalNode : graph.getAllNodes()) {
			finalGraphBuilder.addNode(nodeFactory.createNode(internalNode));
		}

		for (InternalEdge internalEdge : graph.getAllEdges()) {
			finalGraphBuilder.addEdge(edgeFactory.createEdge(internalEdge, finalGraphBuilder));
		}

		return finalGraphBuilder.createGraph();
	}

	private void serializeGraph(Graph<N, E> graph, File serializedFile) {
		try (OutputStream file = new FileOutputStream(serializedFile);
				OutputStream buffer = new BufferedOutputStream(file);
				ObjectOutput output = new ObjectOutputStream(buffer);) {
			output.writeObject(graph);
		} catch (IOException ex) {
			LOGGER.warn("Graphs serialization failed, " + ex.getMessage());
		}
	}

	private Graph<N, E> deserializeGraph(File serializedFile) throws IOException, ClassNotFoundException {
		InputStream file = new FileInputStream(serializedFile);
		InputStream buffer = new BufferedInputStream(file);
		ObjectInput input = new ObjectInputStream(buffer);

		return (Graph<N, E>) input.readObject();
	}

	private void removePreviousSerializedGraph(String basePath) {
		File file = new File(basePath);
		File folder = file.getAbsoluteFile().getParentFile();
		if (folder != null) {
			folder.mkdirs();
		}
		File fList[] = folder.listFiles();
		LOGGER.debug("Current base path: " + basePath);
		LOGGER.debug("Current parent file path: " + file.getAbsoluteFile().getParentFile());

		// Searchs for other .ser graph files
		for (int i = 0; i < fList.length; i++) {
			File serializedFile = fList[i];
			if (serializedFile.getName().endsWith(".ser") || serializedFile.getName().startsWith(file.getName())) {
				LOGGER.debug("Deleting wrong serialized graph file: " + serializedFile.getName());
				serializedFile.delete();
			}
		}
	}
}
