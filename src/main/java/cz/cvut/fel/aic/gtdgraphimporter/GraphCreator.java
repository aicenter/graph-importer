package cz.cvut.fel.aic.gtdgraphimporter;

import cz.cvut.fel.aic.gtdgraphimporter.structurebuilders.TmpGraphBuilder;
import cz.cvut.fel.aic.gtdgraphimporter.structurebuilders.client.EdgeFactory;
import cz.cvut.fel.aic.gtdgraphimporter.structurebuilders.client.NodeFactory;
import cz.cvut.fel.aic.gtdgraphimporter.structurebuilders.edge.EdgeBuilder;
import cz.cvut.fel.aic.gtdgraphimporter.structurebuilders.internal.InternalEdge;
import cz.cvut.fel.aic.gtdgraphimporter.structurebuilders.internal.InternalEdgeBuilder;
import cz.cvut.fel.aic.gtdgraphimporter.structurebuilders.internal.InternalNode;
import cz.cvut.fel.aic.geographtools.Edge;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.GraphBuilder;
import cz.cvut.fel.aic.geographtools.Node;
import cz.cvut.fel.aic.geographtools.util.StronglyConnectedComponentsFinder;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Predicate;

/**
 * Instead of {@link cz.agents.gtdgraphimporter.GTDGraphBuilder}
 * Lighter version of it. Preparation for RoadEdgeExtended
 *
 * @author Zdenek Bousa
 */
public class GraphCreator<N extends Node, E extends Edge>{
    private static final Logger LOGGER = Logger.getLogger(GraphCreator.class);
    
    
    
    
    private final NodeFactory<N> nodeFactory;
    
    private final EdgeFactory<E> edgeFactory;
    
    private final boolean serializationOn;
    
    private final boolean simplificationOn;
    
    protected TmpGraphBuilder<InternalNode, InternalEdge> builder;
    
    private final Importer importer;
    



    public GraphCreator(Transformer projection, boolean serializationOn, 
            boolean simplificationOn, Importer importer, NodeFactory<N> nodeFactory, EdgeFactory<E> edgeFactory) {
        this.serializationOn = serializationOn;
        this.importer = importer;
        this.simplificationOn = serializationOn;
        this.nodeFactory = nodeFactory;
        this.edgeFactory = edgeFactory;
    }
    
    /**
     * init map
     *
     * @return map data with simulation graph
     */
    public Graph<N,E> getMap() {
        Graph<N,E> graph;
        
        String serializedFileName = importer.getSerializationName();
        File serializedFile = new File(serializedFileName);
        
        if(serializationOn){
            try {
                graph = deserializeGraph(serializedFile);
    //            throw new IOException(); // TODO: debug, remove it afterwards
            } catch (Exception ex) {
                LOGGER.warn("Cannot perform deserialization of the cached graphs:" + ex.getMessage());
                LOGGER.warn("Generating graphs from the OSM");
                graph = build();
                serializeGraph(graph, serializedFile);
            }
        }
        else{
            graph = build();        
        }

        return graph;
    }
    
    
    

    /**
     * Construct road graph
     *
     * @return Graph that has one main strong component and might have been simplified (impact on visio - more sharp curves)
     */
    private Graph<N, E> build() {
        builder = importer.loadGraph();
        
        removeMinorComponents(builder);
        
        //TODO: Simplifier - make switch for Visio and for Simulation.
        //TODO: Properly handle RoadEdgeExtended - find opposite way and uniqueWayId
        LOGGER.debug("Graph [#nodes=" + builder.getNodeCount() + ", #edges=" + builder.getEdgeCount() + "] simplification");
        
        if(simplificationOn){
            GraphSimplifier.simplify(builder, Collections.emptySet()); //not working for RoadExtended
        }
        
        Graph<InternalNode,InternalEdge> graph = builder.createGraph();
        
        
        
        return exportGraph(graph);
    }
    

    /**
     * Removes from the {@code osmGraph} all nodes and edges that are not in the main component for any mode.
     *
     * @param osmGraph osm graph with multiple strong components
     */
    private TmpGraphBuilder<InternalNode, InternalEdge> removeMinorComponents(
            TmpGraphBuilder<InternalNode, InternalEdge> osmGraph) {
        LOGGER.debug("Calculating main components for all modes...");
//        SetMultimap<Integer, ModeOfTransport> modesOnNodes = HashMultimap.create();
        Set<Integer> mainComponent = getMainComponent(osmGraph);
//        mainComponent.forEach(i -> modesOnNodes.put(i, mode));
        

        Predicate<EdgeBuilder<? extends InternalEdge>> filter = edge -> {
            InternalEdgeBuilder roadEdgeExtendedBuilder = (InternalEdgeBuilder) edge;
//            roadEdgeExtendedBuilder.intersectModeOfTransports(modesOnNodes.get(roadEdgeExtendedBuilder.getTmpFromId()));
//            roadEdgeExtendedBuilder.intersectModeOfTransports(modesOnNodes.get(roadEdgeExtendedBuilder.getTmpToId()));
//            return roadEdgeExtendedBuilder.getModeOfTransports().isEmpty();
            return !mainComponent.contains(edge.getTmpFromId()) || !mainComponent.contains(edge.getTmpToId());
        };
        
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
//        List<EdgeBuilder<? extends InternalEdge>> feasibleEdges = graph.getFeasibleEdges(mode);
        return getMainComponent(graph.getAllEdges());
    }

    /**
     * Find strong component by size
     */
    private Set<Integer> getMainComponent(Collection<EdgeBuilder<? extends InternalEdge>> edges) {
        Set<Integer> nodeIds = new HashSet<>();
        Map<Integer, Set<Integer>> edgeIds = new HashMap<>();
        for (EdgeBuilder<? extends InternalEdge> edgeExtendedBuilder : edges) {
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
//                "allowedOsmModes=" + allowedOsmModes +
                '}';
    }

    private Graph<N, E> exportGraph(Graph<InternalNode, InternalEdge> graph) {
		GraphBuilder<N,E> finalGraphBuilder = new GraphBuilder<>();

		for (InternalNode internalNode : graph.getAllNodes()) {
			finalGraphBuilder.addNode(nodeFactory.createNode(internalNode));
		}

		for (InternalEdge internalEdge : graph.getAllEdges()) {
			finalGraphBuilder.addEdge(edgeFactory.createEdge(internalEdge));
		}

        return finalGraphBuilder.createGraph();
    }
    
    private void serializeGraph(Graph<N,E> graph, File serializedFile) {
        try (
                OutputStream file = new FileOutputStream(serializedFile);
                OutputStream buffer = new BufferedOutputStream(file);
                ObjectOutput output = new ObjectOutputStream(buffer);
        ) {
            output.writeObject(graph);
        } catch (IOException ex) {
            LOGGER.warn("Graphs serialization failed, " + ex.getMessage());
        }
    }
    
    private Graph<N,E> deserializeGraph(File serializedFile) throws IOException, ClassNotFoundException {
        InputStream file = new FileInputStream(serializedFile);
        InputStream buffer = new BufferedInputStream(file);
        ObjectInput input = new ObjectInputStream(buffer);

        return (Graph<N,E>) input.readObject();
    }
}
