package cz.agents.gtdgraphimporter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.GraphBuilder;
import cz.agents.basestructures.Node;
import cz.agents.geotools.GPSLocationTools;
import cz.agents.geotools.KDTree;
import cz.agents.geotools.KDTree.ConflictResolverMode;
import cz.agents.geotools.StronglyConnectedComponentsFinder;
import cz.agents.geotools.Transformer;
import cz.agents.gtdgraphimporter.gtfs.GTFSDataLoader;
import cz.agents.gtdgraphimporter.gtfs.GTFSImporter;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsException;
import cz.agents.gtdgraphimporter.gtfs.impl.GTFSDatabaseLoaderPermissiveImpl;
import cz.agents.gtdgraphimporter.osm.OsmGraphBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.TmpGraphBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.edge.EdgeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.edge.RoadEdgeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.edge.VirtualEdgeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.node.NodeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.node.RouteNodeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.node.StopNodeBuilder;
import cz.agents.gtdgraphimporter.util.NodeBuilderKdTreeResolver;
import cz.agents.gtdgraphimporter.util.RoadGraphSimplifier;
import cz.agents.gtdgraphimporter.util.TransfersUtil;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.edges.TimeDependentEdge;
import cz.agents.multimodalstructures.edges.VirtualEdge;
import cz.agents.multimodalstructures.nodes.RoadNode;
import org.apache.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @author Marek Cuchý
 */
public class GTDGraphBuilder {

	private static final Logger LOGGER = Logger.getLogger(GTDGraphBuilder.class);

	/**
	 * Set of all currently supported PT modes that can be loaded from GTFS.
	 */
	public static final Set<ModeOfTransport> PT_MODES = Sets.immutableEnumSet(ModeOfTransport.BUS, ModeOfTransport
			.TRAM, ModeOfTransport.UNDERGROUND, ModeOfTransport.TRAIN, ModeOfTransport.TROLLEYBUS, ModeOfTransport
			.FERRY, ModeOfTransport.OTHER);

	/**
	 * Set of all modes that can be loaded from OSM without any additional information required.
	 */
	public static final Set<ModeOfTransport> OSM_MODES = Sets.immutableEnumSet(ModeOfTransport.WALK, ModeOfTransport
			.TAXI, ModeOfTransport.CAR, ModeOfTransport.MOTORCYCLE, ModeOfTransport.BIKE);

	private final Transformer projection;

	private final Connection connectionToGtfs;
	private final PTSettings ptSettings;

	/**
	 * Modes to be loaded from OSM.
	 */
	private final Set<ModeOfTransport> allowedOsmModes;

	private final OsmGraphBuilder.Builder osmBuilderBuilder;

	public GTDGraphBuilder(Transformer projection, Set<ModeOfTransport> allowedOsmModes,
						   OsmGraphBuilder.Builder osmBuilderBuilder, PTSettings ptSettings,
						   Connection connectionToGtfs) {
		this.projection = projection;
		this.connectionToGtfs = connectionToGtfs;
		this.ptSettings = ptSettings;
		this.allowedOsmModes = allowedOsmModes;
		this.osmBuilderBuilder = osmBuilderBuilder;
	}

	public GTDGraphBuilder(Transformer projection, String osmFile, Set<ModeOfTransport> allowedOsmModes,
						   PTSettings ptSettings, Connection connectionToGtfs) {
		this.projection = projection;
		this.connectionToGtfs = connectionToGtfs;
		this.ptSettings = ptSettings;
		this.allowedOsmModes = allowedOsmModes;
		this.osmBuilderBuilder = new OsmGraphBuilder.Builder(osmFile, projection, allowedOsmModes);
	}

	public GTDGraphBuilder(Transformer projection, File osmFile, Set<ModeOfTransport> allowedOsmModes,
						   PTSettings ptSettings, Connection connectionToGtfs) {
		this.projection = projection;
		this.connectionToGtfs = connectionToGtfs;
		this.ptSettings = ptSettings;
		this.allowedOsmModes = allowedOsmModes;
		this.osmBuilderBuilder = new OsmGraphBuilder.Builder(osmFile, projection, allowedOsmModes);
	}

	public Graph<? extends Node, ? extends TimeDependentEdge> build() {
		TmpGraphBuilder<RoadNode, RoadEdge> osmGraph = buildOsmGraph();

		TmpGraphBuilder<Node, TimeDependentEdge> ptGraph;
		try {
			ptGraph = buildPtGraph(osmGraph.getMaxId() + 1);
		} catch (GtfsException e) {
			throw new IllegalStateException("PT graph can't be created.", e);
		}
		if (ptGraph != null) {
			Set<Integer> notToBeRemovedNodes = new HashSet<>();
			List<VirtualEdgeBuilder> virtualEdges = createGtfsOsmMapping(osmGraph, ptGraph, notToBeRemovedNodes);
			RoadGraphSimplifier.simplify(osmGraph, notToBeRemovedNodes);
			ptGraph.addAll(osmGraph);
			ptGraph.addEdges(virtualEdges);
			assert checkOneComponent(osmGraph) : "Graph isn't one component";
			GraphBuilder<Node, TimeDependentEdge> builder = ptGraph.createGraphBuilder();
			Collection<? extends VirtualEdge> transfers = TransfersUtil.precomputeTransfers(builder.dumpCurrentGraph()
					, ptSettings.defaultWalkSpeed, ptSettings.maxPrecomputedTransfersLength, Executors
							.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
			builder.addEdges(Collections.unmodifiableCollection(transfers)); //TODO: remove after added wildcards
			return ptGraph.createGraph();
		} else {
			RoadGraphSimplifier.simplify(osmGraph, Collections.emptySet());
			return osmGraph.createGraph();
		}
	}

	private List<VirtualEdgeBuilder> createGtfsOsmMapping(TmpGraphBuilder<RoadNode, RoadEdge> osmGraph,
														  TmpGraphBuilder<Node, TimeDependentEdge> ptGraph,
														  Set<Integer> notToBeRemovedNodes) {
		List<StopNodeBuilder> stopNodes = ptGraph.getNodesOfType(StopNodeBuilder.class);
		Set<NodeBuilder<? extends RoadNode>> walkNodes = osmGraph.getFeasibleNodes(ModeOfTransport.WALK);

		KDTree<NodeBuilder<?>> kdTree = new KDTree<>(2, new NodeBuilderKdTreeResolver<>(), ConflictResolverMode
				.USE_OLD, NodeBuilder<?>[]::new);
		walkNodes.forEach(kdTree::insert);

		List<VirtualEdgeBuilder> virtualEdges = new ArrayList<>(stopNodes.size() * 2);

		int notMappedStops = 0;
		for (StopNodeBuilder stop : stopNodes) {
			NodeBuilder<?> nearestNode = kdTree.getNearestNode(NodeBuilderKdTreeResolver.getCoords(stop));
			double distance = GPSLocationTools.computeDistanceAsDouble(nearestNode.location, stop.location);

			if (distance <= ptSettings.maxGtfsOsmMappingDistance) {
				distance = Math.max(distance, ptSettings.minGtfsOsmMappingDistance);
				int dist = (int) Math.round(distance);
				int time = (int) Math.round(distance / ptSettings.defaultWalkSpeed);
				virtualEdges.add(new VirtualEdgeBuilder(stop.tmpId, nearestNode.tmpId, dist, ptSettings
						.defaultWalkSpeed, time, ModeOfTransport.WALK));
				virtualEdges.add(new VirtualEdgeBuilder(nearestNode.tmpId, stop.tmpId, dist, ptSettings
						.defaultWalkSpeed, time, ModeOfTransport.WALK));
				notToBeRemovedNodes.add(nearestNode.tmpId);
			} else {
				notMappedStops++;
			}
		}
		LOGGER.debug(notMappedStops + " stops don't have any walk node close enough (" +
					 ptSettings.maxGtfsOsmMappingDistance + " metres)");
		return virtualEdges;
	}

	public Graph<RoadNode, RoadEdge> buildSimplifiedRoadGraph() {
		TmpGraphBuilder<RoadNode, RoadEdge> osmGraph = buildOsmGraph();
		RoadGraphSimplifier.simplify(osmGraph, Collections.emptySet());
		return osmGraph.createGraph();
	}

	public TmpGraphBuilder<Node, TimeDependentEdge> buildPtGraph(int initialNodeId) throws GtfsException {
		if (ptSettings == null || connectionToGtfs == null) {
			LOGGER.info("PT isn't loaded because of missing settings or database connection.");
			return null;
		}

		GTFSDataLoader gtfsLoader = new GTFSDatabaseLoaderPermissiveImpl(connectionToGtfs, projection, 1, 10000,
				ptSettings.pruneBefore, ptSettings.pruneAfter);

		GTFSImporter gtfsImporter = new GTFSImporter(gtfsLoader);
		TmpGraphBuilder<Node, TimeDependentEdge> ptGraph = gtfsImporter.importGtfsToGraphBuilder(ptSettings
				.initialSourceNodeId, ptSettings.getOnDuration, ptSettings.getOffDuration, createLocalDate(ptSettings
				.pruneBefore), initialNodeId);
		ptGraph.printContent();
		List<NodeBuilder<? extends Node>> l = ptGraph.getNodesByDegree().get(2).stream().map(ptGraph::getNode).filter
				(e -> e instanceof RouteNodeBuilder).collect(toList());
		LOGGER.debug(l);
		return ptGraph;
	}

	private TmpGraphBuilder<RoadNode, RoadEdge> buildOsmGraph() {
		TmpGraphBuilder<RoadNode, RoadEdge> osmGraph = osmBuilderBuilder.build().readOsmAndGetGraphBuilder();

		removeMinorComponents(osmGraph);

		return osmGraph;
	}

	/**
	 * Removes from the {@code osmGraph} all nodes and edges that are not in the main component for any mode.
	 *
	 * @param osmGraph
	 */
	private void removeMinorComponents(TmpGraphBuilder<RoadNode, RoadEdge> osmGraph) {
		LOGGER.debug("Calculating main components for all modes...");
		SetMultimap<Integer, ModeOfTransport> modesOnNodes = HashMultimap.create();
		for (ModeOfTransport mode : allowedOsmModes) {
			Set<Integer> mainComponent = getMainComponent(osmGraph, mode);
			mainComponent.forEach(i -> modesOnNodes.put(i, mode));
		}

		Predicate<EdgeBuilder<? extends RoadEdge>> filter = edge -> {
			RoadEdgeBuilder roadEdgeBuilder = (RoadEdgeBuilder) edge;
			roadEdgeBuilder.intersectModeOfTransports(modesOnNodes.get(roadEdgeBuilder.getTmpFromId()));
			roadEdgeBuilder.intersectModeOfTransports(modesOnNodes.get(roadEdgeBuilder.getTmpToId()));
			return roadEdgeBuilder.getModeOfTransports().isEmpty();
		};
		int removedEdges = osmGraph.removeEdges(filter);
		LOGGER.debug("Removed " + removedEdges + " edges.");

		int removedNodes = osmGraph.removeIsolatedNodes();
		LOGGER.debug("Removed " + removedNodes + " nodes.");
		LOGGER.debug("Nodes by degree: ");
		osmGraph.getNodesByDegree().forEach((k, v) -> LOGGER.debug(k + "->" + v.size()));
	}

	/**
	 * @param graph
	 * @param mode
	 *
	 * @return
	 */
	private Set<Integer> getMainComponent(TmpGraphBuilder<RoadNode, RoadEdge> graph, ModeOfTransport mode) {
		List<EdgeBuilder<? extends RoadEdge>> feasibleEdges = graph.getFeasibleEdges(mode);
		return getMainComponent(feasibleEdges);
	}

	private Set<Integer> getMainComponent(Collection<EdgeBuilder<? extends RoadEdge>> edges) {
		Set<Integer> nodeIds = new HashSet<>();
		Map<Integer, Set<Integer>> edgeIds = new HashMap<>();
		for (EdgeBuilder<? extends RoadEdge> edgeBuilder : edges) {
			int fromId = edgeBuilder.getTmpFromId();
			int toId = edgeBuilder.getTmpToId();
			nodeIds.add(fromId);
			nodeIds.add(toId);
			Set<Integer> outgoing = edgeIds.get(fromId);
			if (outgoing == null) {
				outgoing = new HashSet<>();
				edgeIds.put(fromId, outgoing);
			}
			outgoing.add(toId);
		}
		return StronglyConnectedComponentsFinder.getStronglyConnectedComponentsSortedBySize(nodeIds, edgeIds).get(0);
	}

	private LocalDate createLocalDate(Date oldDate) {
		return oldDate.toLocalDate();
	}

	private boolean checkOneComponent(TmpGraphBuilder<RoadNode, RoadEdge> osmGraph) {
		Set<Integer> mainComponent = getMainComponent(osmGraph.getAllEdges());
		Set<Integer> graphNodeIds = osmGraph.getAllNodes().stream().map(n -> n.tmpId).collect(toSet());
		return mainComponent.equals(graphNodeIds);
	}

	public static class PTSettings {

		private final Date pruneBefore;
		private Date pruneAfter;

		private short getOnDuration = 50;
		private short getOffDuration = 10;
		/**
		 * Maximum distance for creating of virtual edge between a stop and its nearest walking node in metres.
		 */
		private double maxGtfsOsmMappingDistance = 1000;

		/**
		 * Minimum distance for creating of virtual edge between a stop and its nearest walking node in metres.
		 */
		private double minGtfsOsmMappingDistance = 1;

		/**
		 * Maximum distance for creating of virtual edges for transfers in metres.
		 */
		private int maxPrecomputedTransfersLength = 1000;

		/**
		 * Default walk speed in m/s. Used for gtfs-osm virtual edges.
		 */
		private float defaultWalkSpeed = 5.0f / 3.6f;

		private long initialSourceNodeId = 1_000_000L;

		//		/**
		//		 * PT modes to be loaded. If set to {@code null} all PT nodes will be loaded.
		//		 */
		//		private Set<ModeOfTransport> allowedPtModes = null;

		public PTSettings(Date pruneBefore) {
			this.pruneBefore = pruneBefore;
		}

		public PTSettings(Date pruneBefore, Date pruneAfter) {
			this.pruneBefore = pruneBefore;
			this.pruneAfter = pruneAfter;
		}

		public PTSettings(String pruneBefore) {
			this.pruneBefore = Date.valueOf(pruneBefore);
		}

		public PTSettings(String pruneBefore, String pruneAfter) {
			this.pruneBefore = Date.valueOf(pruneBefore);
			this.pruneAfter = Date.valueOf(pruneAfter);
		}

		public PTSettings setPruneAfter(Date pruneAfter) {
			this.pruneAfter = pruneAfter;
			return this;
		}

		public PTSettings setGetOnDuration(short getOnDuration) {
			this.getOnDuration = getOnDuration;
			return this;
		}

		public PTSettings setGetOffDuration(short getOffDuration) {
			this.getOffDuration = getOffDuration;
			return this;
		}

		public PTSettings setInitialSourceNodeId(long initialSourceNodeId) {
			this.initialSourceNodeId = initialSourceNodeId;
			return this;
		}

		public PTSettings setMaxGtfsOsmMappingDistance(double maxGtfsOsmMappingDistance) {
			this.maxGtfsOsmMappingDistance = maxGtfsOsmMappingDistance;
			return this;
		}

		public PTSettings setMinGtfsOsmMappingDistance(double minGtfsOsmMappingDistance) {
			this.minGtfsOsmMappingDistance = minGtfsOsmMappingDistance;
			return this;
		}

		public PTSettings setMaxPrecomputedTransfersLength(int maxPrecomputedTransfersLength) {
			this.maxPrecomputedTransfersLength = maxPrecomputedTransfersLength;
			return this;
		}

		public PTSettings setDefaultWalkSpeed(float defaultWalkSpeed) {
			this.defaultWalkSpeed = defaultWalkSpeed;
			return this;
		}

		//		/**
		//		 * Set PT modes to be loaded. If set to {@code null} (default value) all PT nodes will be loaded.
		//		 *
		//		 * @param allowedPtModes
		//		 *
		//		 * @return
		//		 */
		//		public PTSettings setAllowedPtModes(Set<ModeOfTransport> allowedPtModes) {
		//			this.allowedPtModes = allowedPtModes;
		//			return this;
		//		}
	}
}
