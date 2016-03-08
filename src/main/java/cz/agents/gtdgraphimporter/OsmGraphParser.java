package cz.agents.gtdgraphimporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import cz.agents.basestructures.Node;
import cz.agents.gtdgraphimporter.osm.*;
import cz.agents.gtdgraphimporter.osm.element.OsmNode;
import cz.agents.gtdgraphimporter.osm.element.OsmRelation;
import cz.agents.gtdgraphimporter.osm.element.OsmWay;
import cz.agents.gtdgraphimporter.osm.handler.OsmHandler;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.RoadEdge;
import cz.agents.multimodalstructures.edges.TimeDependentEdge;
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
 * Class parsing OSM XML file using SAX parser. The output of this parser is a {@link GraphBuilder}.
 *
 * @author Marek Cuchý
 */
public class OsmGraphParser implements OsmElementConsumer {

	private static final Logger LOGGER = Logger.getLogger(OsmGraphParser.class);

	/**
	 * URL of the OSM to be parsed
	 */
	private final URL osmUrl;

	/**
	 * Factory for building graph nodes
	 */
	private final NodeFactory nodeFactory;

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
	private final GraphBuilder<Node, TimeDependentEdge> builder = new GraphBuilder<>();

	private int mergedEdges = 0;

	protected OsmGraphParser(URL osmUrl, NodeFactory nodeFactory, TagExtractor<Double> elevationExtractor,
							 TagExtractor<Double> speedExtractor, TagEvaluator parkAndRideEvaluator,
							 Map<ModeOfTransport, TagEvaluator> modeEvaluators, Map<ModeOfTransport, TagEvaluator>
									 oneWayEvaluators, TagEvaluator oppositeDirectionEvaluator) {

		this.osmUrl = osmUrl;
		this.nodeFactory = nodeFactory;
		this.elevationExtractor = elevationExtractor;
		this.speedExtractor = speedExtractor;
		this.parkAndRideEvaluator = parkAndRideEvaluator;
		this.modeEvaluators = modeEvaluators;
		this.oneWayEvaluators = oneWayEvaluators;
		this.oppositeDirectionEvaluator = oppositeDirectionEvaluator;
	}

	public GraphBuilder<Node, TimeDependentEdge> readOsmAndCreateGraphBuilder() {
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
		return builder;
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
		int fromId = builder.getIntIdForSourceId(fromSourceId);
		int toId = builder.getIntIdForSourceId(toSourceId);

		if (builder.containsEdge(fromId, toId)) {
			mergedEdges++;
			mergeEdge(fromId, toId, permittedModes, way);
		} else {
			RoadEdge roadEdge = createRoadEdge(fromId, toId, permittedModes, way);
			builder.addEdge(roadEdge);
		}
	}

	private void mergeEdge(int fromId, int toId, Set<ModeOfTransport> permittedModes, OsmWay way) {
		RoadEdge edge = (RoadEdge) builder.getEdge(fromId, toId);

		//if all permittedModes are not included in the edge, union both modes sets
		if (!edge.checkFeasibility(permittedModes)) {
			for (ModeOfTransport mode : ModeOfTransport.values()) {
				if (edge.checkFeasibility(mode)) permittedModes.add(mode);
			}
			RoadEdge newEdge = createRoadEdge(fromId, toId, permittedModes, way);
			builder.replaceEdge(newEdge);
		}
	}

	private RoadEdge createRoadEdge(int fromId, int toId, Set<ModeOfTransport> permittedModes, OsmWay way) {
		return new RoadEdge(fromId, toId, way.getId(), permittedModes, speedExtractor.apply(way.getTags()).floatValue
				(), (int) calculateLength(fromId, toId));
	}

	private double calculateLength(int fromId, int toId) {
		Node n1 = builder.getNode(fromId);
		Node n2 = builder.getNode(toId);

		int lat = n1.latProjected - n2.latProjected;
		int lon = n1.lonProjected - n2.lonProjected;
		return Math.sqrt(lat * lat + lon * lon);
	}

	private void createAndAddNode(Long nodeId) {
		if (!builder.containsNode(nodeId)) {
			OsmNode osmNode = osmNodes.get(nodeId);
			RoadNode roadNode = nodeFactory.createRoadNode(osmNode.lat, osmNode.lon, elevationExtractor.apply(osmNode
					.getTags()), nodeId, isParkAndRide(osmNode), false);
			builder.addNode(roadNode);
		}
	}

	private boolean isParkAndRide(OsmNode osmNode) {
		return parkAndRideEvaluator.test(osmNode.getTags());
	}

	public String getStatistic() {
		return "Merged edges=" + mergedEdges;
	}

	@Override
	public String toString() {
		return "OsmGraphParser{" +
				"nodes=" + osmNodes.size() + '}';
	}

	public static class Builder {

		private static final ObjectMapper MAPPER = new ObjectMapper();

		private final NodeFactory nodeFactory;
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
		 * @param nodeFactory
		 * @param allowedModes
		 * 		modes that are required to be in the output graph. For each modes there have to be set appropriate mode
		 * 		evaluator. If it isn't set the builder tries to use a default one, but it's possible that it is not
		 * 		defined.
		 */
		public Builder(URL osmUrl, NodeFactory nodeFactory, Set<ModeOfTransport> allowedModes) {
			if (allowedModes.isEmpty()) throw new IllegalArgumentException("Allowed modes can't be empty.");
			this.nodeFactory = nodeFactory;
			this.allowedModes = allowedModes;
			this.osmUrl = osmUrl;
		}

		public Builder(File osmFile, NodeFactory nodeFactory, Set<ModeOfTransport> allowedModes) {
			this(getUrl(osmFile), nodeFactory, allowedModes);
		}

		public Builder(String osmPath, NodeFactory nodeFactory, Set<ModeOfTransport> allowedModes) {
			this(getUrl(osmPath), nodeFactory, allowedModes);
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

		public OsmGraphParser build() {
			loadSpeedExtractorIfNeeded();
			loadModeEvaluatorsIfNeeded();
			loadOneWayEvaluatorsIfNeeded();

			return new OsmGraphParser(osmUrl, nodeFactory, elevationExtractor, speedExtractor, parkAndRideEvaluator,
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
					modeEvaluators.put(mode, defaultEval);
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
			try {
				return new URL(osmPath);
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("Incorrect path: " + osmPath, e);
			}
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
