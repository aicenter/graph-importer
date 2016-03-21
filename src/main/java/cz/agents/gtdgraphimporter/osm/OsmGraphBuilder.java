package cz.agents.gtdgraphimporter.osm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Graph;
import cz.agents.geotools.EPSGProjection;
import cz.agents.geotools.EdgeUtil;
import cz.agents.gtdgraphimporter.osm.element.OsmNode;
import cz.agents.gtdgraphimporter.osm.element.OsmRelation;
import cz.agents.gtdgraphimporter.osm.element.OsmWay;
import cz.agents.gtdgraphimporter.osm.handler.OsmHandler;
import cz.agents.gtdgraphimporter.structurebuilders.NodeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.RoadEdgeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.RoadNodeBuilder;
import cz.agents.gtdgraphimporter.structurebuilders.TmpGraphBuilder;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.nodes.RoadNode;
import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import static java.util.stream.Collectors.toSet;

/**
 * Class parsing OSM XML file using SAX parser. The output of this parser is a non-simplified graph defined by the OSM.
 * There are not done any other operations like simplification and finding of strongly connected components.
 *
 * @author Marek Cuchý
 */
public class OsmGraphBuilder implements OsmElementConsumer {

	private static final Logger LOGGER = Logger.getLogger(OsmGraphBuilder.class);

	/**
	 * URL of the OSM to be parsed
	 */
	private final URL osmUrl;

	/**
	 * Factory for building graph nodes
	 */
	private final EPSGProjection projection;

	/**
	 * Function extracting elevation from node tags
	 */
	private final TagExtractor<Double> elevationExtractor;

	/**
	 * Function extracting max speed from way tags
	 */
	private final TagExtractor<Double> speedExtractor;

	/**
	 * Predicate that says if an element is a park and ride location based on its tags
	 */
	private final TagEvaluator parkAndRideEvaluator;

	/**
	 * Predicate for each mode, allowed in the graph, that says if the mode is allowed on a particular way (edge)
	 */
	private final Map<ModeOfTransport, TagEvaluator> modeEvaluators;

	/**
	 * Predicate for each mode, allowed in the graph, that says if a way (edge) is one-way for the mode
	 */
	private final Map<ModeOfTransport, TagEvaluator> oneWayEvaluators;

	/**
	 * Predicate that says if nodes of a way are in opposite order than they really are. Important only for one-way
	 * edges. Current implementation just reverse the order of the nodes therefore the one-way evaluators must
	 * calculate
	 * with the opposite order tags.
	 */
	private final TagEvaluator oppositeDirectionEvaluator;

	private final Map<Long, OsmNode> osmNodes = new HashMap<>();
	private final TmpGraphBuilder<RoadNode, RoadEdge> builder = new TmpGraphBuilder<>();

	private int mergedEdges = 0;

	protected OsmGraphBuilder(URL osmUrl, EPSGProjection nodeFactory, TagExtractor<Double> elevationExtractor,
							  TagExtractor<Double> speedExtractor, TagEvaluator parkAndRideEvaluator,
							  Map<ModeOfTransport, TagEvaluator> modeEvaluators,
							  Map<ModeOfTransport, TagEvaluator> oneWayEvaluators,
							  TagEvaluator oppositeDirectionEvaluator) {

		this.osmUrl = osmUrl;
		this.projection = nodeFactory;
		this.elevationExtractor = elevationExtractor;
		this.speedExtractor = speedExtractor;
		this.parkAndRideEvaluator = parkAndRideEvaluator;
		this.modeEvaluators = modeEvaluators;
		this.oneWayEvaluators = oneWayEvaluators;
		this.oppositeDirectionEvaluator = oppositeDirectionEvaluator;
	}

	public TmpGraphBuilder<RoadNode, RoadEdge> readOsmAndGetGraphBuilder(){
		parseOSM();
		return builder;
	}

	public Graph<RoadNode, RoadEdge> readOsmAndCreateGraph() {
		parseOSM();
		return builder.createGraph();
	}

	private void parseOSM() {
		LOGGER.info("Parsing of OSM started...");

		long t1 = System.currentTimeMillis();

		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			XMLReader xmlreader = parser.getXMLReader();
			xmlreader.setContentHandler(new OsmHandler(this));
			xmlreader.parse(new InputSource(osmUrl.openStream()));
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new IllegalStateException("OSM can't be parsed.", e);
		}

		LOGGER.info(getStatistic());
		long t2 = System.currentTimeMillis();
		LOGGER.info("Parsing of OSM finished in " + (t2 - t1) + "ms");
		osmNodes.clear();
	}

	@Override
	public void accept(OsmNode node) {
		osmNodes.put(node.id, node);
	}

	@Override
	public void accept(OsmWay way) {
		way.removeMissingNodes(osmNodes.keySet());

		Set<ModeOfTransport> permittedModes = getPermittedModes(way);

		if (!permittedModes.isEmpty()) {
			createEdges(way, permittedModes);
		}
	}

	@Override
	public void accept(OsmRelation relation) {
	}

	private Set<ModeOfTransport> getPermittedModes(OsmWay way) {
		Set<ModeOfTransport> permittedModes = EnumSet.noneOf(ModeOfTransport.class);

		for (Entry<ModeOfTransport, TagEvaluator> entry : modeEvaluators.entrySet()) {
			ModeOfTransport mode = entry.getKey();
			if (entry.getValue().test(way.getTags())) {
				permittedModes.add(mode);
			}
		}
		return permittedModes;
	}

	private void createEdges(OsmWay way, Set<ModeOfTransport> permittedModes) {
		List<Long> nodes = way.getNodes();

		//reverse nodes if way is the opposite direction. Have to cooperate with one-way evaluators.
		if (oppositeDirectionEvaluator.test(way.getTags())) {
			nodes = Lists.reverse(nodes);
		}
		Set<ModeOfTransport> bidirectionalModes = getBidirectionalModes(way, permittedModes);

		createAndAddNode(nodes.get(0));
		for (int i = 1; i < nodes.size(); i++) {
			createAndAddNode(nodes.get(i));
			createAndAddOrMergeEdge(nodes.get(i - 1), nodes.get(i), permittedModes, way);

			if (!bidirectionalModes.isEmpty()) {
				createAndAddOrMergeEdge(nodes.get(i), nodes.get(i - 1), bidirectionalModes, way);
			}
		}
	}

	/**
	 * Return subset of {@code permittedModes} for which the way is bidirectional (isn't one-way).
	 *
	 * @param way
	 * @param permittedModes
	 *
	 * @return
	 */
	private Set<ModeOfTransport> getBidirectionalModes(OsmWay way, Set<ModeOfTransport> permittedModes) {
		return permittedModes.stream().filter(mode -> isBidirectional(way, mode)).collect(toSet());
	}

	private boolean isBidirectional(OsmWay way, ModeOfTransport mode) {
		return !oneWayEvaluators.get(mode).test(way.getTags());
	}

	private void createAndAddOrMergeEdge(long fromSourceId, long toSourceId, Set<ModeOfTransport> permittedModes,
										 OsmWay way) {
		int tmpFromId = builder.getIntIdForSourceId(fromSourceId);
		int tmpToId = builder.getIntIdForSourceId(toSourceId);

		if (builder.containsEdge(tmpFromId, tmpToId)) {
			mergedEdges++;
			RoadEdgeBuilder edgeBuilder = (RoadEdgeBuilder) builder.getEdge(tmpFromId, tmpToId);
			edgeBuilder.addPermittedModes(permittedModes);
		} else {
			RoadEdgeBuilder roadEdge = createRoadEdgeBuilder(tmpFromId, tmpToId, permittedModes, way);
			builder.addEdge(roadEdge);
		}
	}

	private GPSLocation getProjectedGPS(double lat, double lon, double elevation) {
		int latE6 = (int) (lat * 1E6);
		int lonE6 = (int) (lon * 1E6);
		return projection.getProjectedGPSLocation(latE6, lonE6, (int) Math.round(elevation));
	}

	private RoadEdgeBuilder createRoadEdgeBuilder(int fromId, int toId, Set<ModeOfTransport> permittedModes,
												  OsmWay way) {
		return new RoadEdgeBuilder(fromId, toId, (int) calculateLength(fromId, toId), speedExtractor.apply(way.getTags
				()).floatValue(), way.getId(), permittedModes);
	}

	private double calculateLength(int fromId, int toId) {
		NodeBuilder<? extends RoadNode> n1 = builder.getNode(fromId);
		NodeBuilder<? extends RoadNode> n2 = builder.getNode(toId);
		return EdgeUtil.computeEuclideanDistance(n1.location, n2.location);
	}

	private void createAndAddNode(long nodeId) {
		if (!builder.containsNode(nodeId)) {
			OsmNode osmNode = osmNodes.get(nodeId);
			RoadNodeBuilder roadNodeBuilder = new RoadNodeBuilder(builder.getNodeCount(), nodeId, getProjectedGPS
					(osmNode));
			builder.addNode(roadNodeBuilder);
		}
	}

	private GPSLocation getProjectedGPS(OsmNode osmNode) {
		return getProjectedGPS(osmNode.lat, osmNode.lon, elevationExtractor.apply(osmNode.getTags()));
	}

	private boolean isParkAndRide(OsmNode osmNode) {
		return parkAndRideEvaluator.test(osmNode.getTags());
	}

	public String getStatistic() {
		return "Merged edges=" + mergedEdges;
	}

	@Override
	public String toString() {
		return "OsmGraphBuilder{" +
				"nodes=" + osmNodes.size() + '}';
	}

	public static class Builder {

		private static final ObjectMapper MAPPER = new ObjectMapper();

		private final EPSGProjection nodeFactory;
		private final Set<ModeOfTransport> allowedModes;

		private final URL osmUrl;

		private TagExtractor<Double> elevationExtractor = new DoubleExtractor("height", 0);
		private TagExtractor<Double> speedExtractor;

		private Map<ModeOfTransport, TagEvaluator> modeEvaluators = new EnumMap<>(ModeOfTransport.class);
		private Map<ModeOfTransport, TagEvaluator> oneWayEvaluators = new EnumMap<>(ModeOfTransport.class);
		private TagEvaluator oppositeDirectionEvaluator = new OneTagEvaluator("oneway", "-1");
		private TagEvaluator parkAndRideEvaluator = new OneTagEvaluator("park_and_ride", "yes");

		/**
		 * @param osmUrl
		 * @param projection
		 * @param allowedModes
		 * 		modes that are required to be in the output graph. For each modes there have to be set appropriate mode
		 * 		evaluator. If it isn't set the builder tries to use a default one, but it's possible that it is not
		 * 		defined.
		 */
		public Builder(URL osmUrl, EPSGProjection projection, Set<ModeOfTransport> allowedModes) {
			if (allowedModes.isEmpty()) throw new IllegalArgumentException("Allowed modes can't be empty.");
			this.nodeFactory = projection;
			this.allowedModes = allowedModes;
			this.osmUrl = osmUrl;
		}

		public Builder(File osmFile, EPSGProjection projection, Set<ModeOfTransport> allowedModes) {
			this(getUrl(osmFile), projection, allowedModes);
		}

		public Builder(String osmPath, EPSGProjection projection, Set<ModeOfTransport> allowedModes) {
			this(getUrl(osmPath), projection, allowedModes);
		}

		public Builder setElevationExtractor(TagExtractor<Double> elevationExtractor) {
			this.elevationExtractor = elevationExtractor;
			return this;
		}

		public Builder addModeEvaluators(Map<ModeOfTransport, TagEvaluator> modeEvaluators) {
			this.modeEvaluators.putAll(modeEvaluators);
			return this;
		}

		public Builder addModeEvaluator(ModeOfTransport mode, TagEvaluator modeEvaluator) {
			this.modeEvaluators.put(mode, modeEvaluator);
			return this;
		}

		public Builder addOneWayEvaluators(Map<ModeOfTransport, TagEvaluator> oneWayEvaluators) {
			this.oneWayEvaluators.putAll(oneWayEvaluators);
			return this;
		}

		public Builder addOneWayEvaluators(ModeOfTransport mode, TagEvaluator oneWayEvaluator) {
			this.oneWayEvaluators.put(mode, oneWayEvaluator);
			return this;
		}

		public Builder setOppositeDirectionEvaluator(TagEvaluator oppositeDirectionEvaluator) {
			this.oppositeDirectionEvaluator = oppositeDirectionEvaluator;
			return this;
		}

		public Builder setParkAndRideEvaluator(TagEvaluator parkAndRideEvaluator) {
			this.parkAndRideEvaluator = parkAndRideEvaluator;
			return this;
		}

		public Builder setSpeedExtractor(TagExtractor<Double> speedExtractor) {
			this.speedExtractor = speedExtractor;
			return this;
		}

		public OsmGraphBuilder build() {
			loadSpeedExtractorIfNeeded();
			loadModeEvaluatorsIfNeeded();
			loadOneWayEvaluatorsIfNeeded();

			return new OsmGraphBuilder(osmUrl, nodeFactory, elevationExtractor, speedExtractor, parkAndRideEvaluator,
					modeEvaluators, oneWayEvaluators, oppositeDirectionEvaluator);
		}

		private void loadSpeedExtractorIfNeeded() {
			if (speedExtractor == null) {
				try {
					speedExtractor = MAPPER.readValue(this.getClass().getResourceAsStream("default_speed_mapping" +
							"" +
							".json"), SpeedExtractor.class);
				} catch (IOException e) {
					throw new IllegalStateException("Default speed extractor can't be created.", e);
				}
			}
		}

		private void loadModeEvaluatorsIfNeeded() {
			Set<ModeOfTransport> missingModes = Sets.difference(allowedModes, modeEvaluators.keySet());
			for (ModeOfTransport mode : missingModes) {
				InputStream stream = this.getClass().getResourceAsStream("mode/" + mode.name().toLowerCase() +
						".json");
				if (stream == null) {
					throw new IllegalStateException("Default mode evaluator for " + mode + " isn't defined. You " +
							"have to define it.");
				}
				try {
					modeEvaluators.put(mode, MAPPER.readValue(stream, InclExclTagEvaluator.class));
				} catch (IOException e) {
					throw new IllegalStateException("Default mode evaluator for mode " + mode + " can't be created.",
							e);
				}
			}
		}

		private void loadOneWayEvaluatorsIfNeeded() {
			Set<ModeOfTransport> missingModes = Sets.difference(allowedModes, oneWayEvaluators.keySet());

			//default evaluator for all modes.
			TagEvaluator defaultEval = TagEvaluator.ALWAYS_FALSE;
			for (ModeOfTransport mode : missingModes) {
				InputStream stream = this.getClass().getResourceAsStream("oneway/" + mode.name().toLowerCase() +
						".json");
				if (stream == null) {
					oneWayEvaluators.put(mode, defaultEval);
				} else {
					try {
						oneWayEvaluators.put(mode, MAPPER.readValue(stream, InclExclTagEvaluator.class));
					} catch (IOException e) {
						LOGGER.warn("Default mode evaluator for mode " + mode + " can't be created. Used default " +
								"evaluator for all modes.");
						oneWayEvaluators.put(mode, defaultEval);
					}
				}
			}
		}

		private static URL getUrl(String osmPath) {
			return getUrl(new File(osmPath));
		}

		private static URL getUrl(File osmFile) {
			try {
				return osmFile.toURI().toURL();
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("Incorrect file: " + osmFile, e);
			}
		}
	}
}
