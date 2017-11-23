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
package cz.cvut.fel.aic.graphimporter.osm;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.TransportMode;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.graphimporter.GraphCreator;
import cz.cvut.fel.aic.graphimporter.Importer;
import cz.cvut.fel.aic.graphimporter.osm.element.OsmNode;
import cz.cvut.fel.aic.graphimporter.osm.element.OsmRelation;
import cz.cvut.fel.aic.graphimporter.osm.element.OsmWay;
import cz.cvut.fel.aic.graphimporter.osm.handler.OsmHandler;
import cz.cvut.fel.aic.graphimporter.structurebuilders.NodeBuilder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.TmpGraphBuilder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalEdge;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalEdgeBuilder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalNode;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalNodeBuilder;
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
import java.util.*;

import static java.util.stream.Collectors.toSet;

/**
 * @author fido
 */
public class OsmImporter extends Importer implements OsmElementConsumer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOGGER = Logger.getLogger(GraphCreator.class);

    static {
        MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }


    private final Map<Long, OsmNode> osmNodes;

    /**
     * Predicate for each mode, allowed in the graph, that says if the mode is allowed on a particular way (edge)
     */
    private final Map<TransportMode, TagEvaluator> modeEvaluators;

    /**
     * Predicate that says if nodes of a way are in opposite order than they really are. Important only for one-way
     * edges. Current implementation just reverse the order of the nodes therefore the one-way evaluators must
     * calculate
     * with the opposite order tags.
     */
    private final TagEvaluator oppositeDirectionEvaluator;

    protected final TmpGraphBuilder<InternalNode, InternalEdge> builder;

    /**
     * Factory for building graph nodes
     */
    private final Transformer projection;

    /**
     * Function extracting elevation from node tags
     */
    private final TagExtractor<Double> elevationExtractor;

    /**
     * Predicate for each mode, allowed in the graph, that says if a way (edge) is one-way for the mode
     */
    private final Map<TransportMode, TagEvaluator> oneWayEvaluators;

    private final File osmFile;

    protected final Set<TransportMode> allowedModes;

    /**
     * Function extracting max speed from way tags
     */
    private WayTagExtractor<Double> speedExtractor;

    /**
     * Function extracting lanes count tag.
     */
    private WayTagExtractor<Integer> lanesCountExtractor;


    private int mergedEdges;


    public OsmImporter(File osmFile, Set<TransportMode> allowedOsmModes, Transformer projection) {
        this.projection = projection;
        this.osmFile = osmFile;
        allowedModes = allowedOsmModes;

        osmNodes = new HashMap<>();
        modeEvaluators = new EnumMap<>(TransportMode.class);
        oppositeDirectionEvaluator = new OneTagEvaluator("oneway", "-1");
        builder = new TmpGraphBuilder<>();
        elevationExtractor = new DoubleExtractor("height", 0);
        oneWayEvaluators = new EnumMap<>(TransportMode.class);
        mergedEdges = 0;
    }

    public TmpGraphBuilder<InternalNode, InternalEdge> loadGraph() {
        loadMissingSettings();
        parseOSM();

        return builder;
    }


    @Override
    public void accept(OsmNode node) {
        osmNodes.put(node.id, node);
    }

    @Override
    public void accept(OsmWay way) {
        way.removeMissingNodes(osmNodes.keySet());

        Set<TransportMode> modesOfTransport = getModesOfTransport(way);

        if (!modesOfTransport.isEmpty()) {
            createEdges(way, modesOfTransport);
        }
    }

    @Override
    public void accept(OsmRelation relation) {
    }

    /**
     * Check for setting
     */
    protected void loadMissingSettings() {
        loadSpeedExtractorIfNeeded();
        loadModeEvaluatorsIfNeeded();
        loadOneWayEvaluatorsIfNeeded();
        loadLaneCountExtractorIfNeeded();
    }

    /**
     * Add missing tag evaluators
     */
    private void loadSpeedExtractorIfNeeded() {
        if (speedExtractor == null) {
            try {
                speedExtractor = MAPPER.readValue(
                        SpeedExtractor.class.getResourceAsStream("default_speed_mapping.json"),
                        SpeedExtractor.class);
            } catch (IOException e) {
                throw new IllegalStateException("Default speed extractor can't be created.", e);
            }
        }
    }

    private void loadLaneCountExtractorIfNeeded() {
        if (lanesCountExtractor == null) {
            lanesCountExtractor = new LanesCountExtractor();
        } else {
            throw new IllegalStateException("Default lanes count extractor can't be created.");
        }
    }

    private void loadModeEvaluatorsIfNeeded() {
        Set<TransportMode> missingModes = Sets.difference(allowedModes, modeEvaluators.keySet());
        for (TransportMode mode : missingModes) {
            InputStream stream = OsmImporter.class.getResourceAsStream("mode/" + mode.name().toLowerCase() +
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
        Set<TransportMode> missingModes = Sets.difference(allowedModes, oneWayEvaluators.keySet());

        //default evaluator for all modes.
        TagEvaluator defaultEval = TagEvaluator.ALWAYS_FALSE;
        for (TransportMode mode : missingModes) {
            InputStream stream = OsmImporter.class.getResourceAsStream("oneway/" + mode.name().toLowerCase() +
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

    /**
     * parser
     */
    private void parseOSM() {
        LOGGER.info("Parsing of OSM started...");

        long t1 = System.currentTimeMillis();

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            XMLReader xmlreader = parser.getXMLReader();
            xmlreader.setContentHandler(new OsmHandler(this));
            xmlreader.parse(new InputSource(osmFile.toURI().toURL().openStream()));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalStateException("OSM can't be parsed.", e);
        }

        LOGGER.info(getStatistic());
        long t2 = System.currentTimeMillis();
        LOGGER.info("Parsing of OSM finished in " + (t2 - t1) + "ms");
        osmNodes.clear();
    }

    /**
     * Stats
     *
     * @return
     */
    public String getStatistic() {
        return "Merged edges=" + mergedEdges;
    }

    /**
     * OSM way modes
     */
    private Set<TransportMode> getModesOfTransport(OsmWay way) {
        Set<TransportMode> ModesOfTransport = EnumSet.noneOf(TransportMode.class);

        for (Map.Entry<TransportMode, TagEvaluator> entry : modeEvaluators.entrySet()) {
            TransportMode mode = entry.getKey();
            if (entry.getValue().test(way.getTags())) {
                ModesOfTransport.add(mode);
            }
        }
        return ModesOfTransport;
    }

    /**
     * Create nodes & edges section
     */
    private void createEdges(OsmWay way, Set<TransportMode> modeOfTransports) {
        List<Long> nodes = way.getNodes();

        //reverse nodes if way is the opposite direction. Have to cooperate with one-way evaluators.
        if (oppositeDirectionEvaluator.test(way.getTags())) {
            nodes = Lists.reverse(nodes);
        }
        nodes.forEach(this::createAndAddNode);

        Set<TransportMode> bidirectionalModes = getBidirectionalModes(way, modeOfTransports);

        //the EdgeType parameters doesn't take into account the possibility of reversed direction - possible fix in
        // the future
        //
        // bidirectionalStatus is used for (int) uniqueWayId and (int) oppositeWayId. If 0, then edge is one-way.
        //If the number is 1, it is a bidirectional edge (in FORWARD) and if the number is 2, then it is the opposite
        // direction of the edge (BACKWARD)
        if (bidirectionalModes.isEmpty()) {
            createAndAddOrMergeEdges(nodes, modeOfTransports, way, EdgeType.FORWARD, 0);
        } else {
            way.addTag("[OsmParser]::bidirectional", "1"); // TODO: do it properly inside WayTagExtractor

            createAndAddOrMergeEdges(nodes, modeOfTransports, way, EdgeType.FORWARD, 1);
            createAndAddOrMergeEdges(Lists.reverse(nodes), bidirectionalModes, way, EdgeType.BACKWARD, 2);
        }
    }

    /**
     * Create node and give it an int number based on builder.getNodeCount() - number of already added nodes
     * in TmpGraphBuilder
     *
     * @param nodeId - source id in OsmNode
     */
    protected void createAndAddNode(long nodeId) {
        if (!builder.containsNode(nodeId)) {
            OsmNode osmNode = osmNodes.get(nodeId);
            InternalNodeBuilder internalNodeBuilder = new InternalNodeBuilder(builder.getNodeCount(),
                    nodeId, getProjectedGPS(osmNode));
            builder.addNode(internalNodeBuilder);
        }
    }

    private GPSLocation getProjectedGPS(double lat, double lon, double elevation) {
        return GPSLocationTools.createGPSLocation(lat, lon, (int) Math.round(elevation), projection);
    }

    private GPSLocation getProjectedGPS(OsmNode osmNode) {
        return getProjectedGPS(osmNode.lat, osmNode.lon, elevationExtractor.apply(osmNode.getTags()));
    }

    /**
     * Create edges for each node in OsmWay
     *
     * @param bidirectionalStatus is used for (int) uniqueWayId and (int) oppositeWayId. If 0, then edge is one-way.
     *                            If the number is 1, it is a bidirectional edge (in FORWARD) and if the number is 2,
     *                            then it is the opposite direction of the edge (BACKWARD)
     */
    private void createAndAddOrMergeEdges(List<Long> nodes, Set<TransportMode> modeOfTransports, OsmWay way,
                                          EdgeType edgeType, int bidirectionalStatus) {
        for (int i = 1; i < nodes.size(); i++) {
            createAndAddOrMergeEdge(nodes.get(i - 1), nodes.get(i), modeOfTransports, way, edgeType, bidirectionalStatus);
        }
    }

    protected void createAndAddOrMergeEdge(long fromSourceId, long toSourceId, Set<TransportMode> modeOfTransports,
                                           OsmWay way, EdgeType edgeType, int bidirectionalStatus) {
        int tmpFromId = builder.getIntIdForSourceId(fromSourceId);
        int tmpToId = builder.getIntIdForSourceId(toSourceId);

        if (builder.containsEdge(tmpFromId, tmpToId)) {
            //edge already built, so add  another mode
            mergedEdges++;
            resolveConflictEdges(tmpFromId, tmpToId, modeOfTransports, way, edgeType);
        } else {
            // begin with new edge
            int uniqueId = builder.getEdgeCount();

            // decide on opposite way
            int oppositeWayUniqueId;
            if (bidirectionalStatus == 1) {
                oppositeWayUniqueId = uniqueId + 1; // opposite direction will follow in construction
            } else if (bidirectionalStatus == 2) {
                oppositeWayUniqueId = uniqueId - 1;
            } else {
                oppositeWayUniqueId = -1;
            }

            // create temporary edge
            InternalEdgeBuilder internalEdgeBuilder = new InternalEdgeBuilder(tmpFromId, tmpToId, way.getId(), uniqueId,
                    oppositeWayUniqueId, (int) calculateLength(tmpFromId, tmpToId), modeOfTransports,
                    extractSpeed(way, edgeType), extractLanesCount(way, edgeType), Arrays.asList(builder.getNode(tmpFromId).location, builder.getNode(tmpToId).location));

            // add edge to TmpGraphBuilder
            builder.addEdge(internalEdgeBuilder);
        }
    }

    protected void resolveConflictEdges(int tmpFromId, int tmpToId, Set<TransportMode> newModeOfTransports,
                                        OsmWay way, EdgeType edgeType) {
        InternalEdgeBuilder internalEdgeBuilder = (InternalEdgeBuilder) builder.getEdge(tmpFromId, tmpToId);
        internalEdgeBuilder.addModeOfTransports(newModeOfTransports);
    }

    protected double calculateLength(int fromId, int toId) {
        NodeBuilder<? extends InternalNode> n1 = builder.getNode(fromId);
        NodeBuilder<? extends InternalNode> n2 = builder.getNode(toId);
        return GPSLocationTools.computeDistance(n1.location, n2.location);
    }

    private Integer extractLanesCount(OsmWay way, EdgeType edgeType) {
        if (EdgeType.BACKWARD == edgeType) {
            return lanesCountExtractor.getBackwardValue(way.getTags());
        } else {
            return lanesCountExtractor.getForwardValue(way.getTags());
        }
    }

    protected float extractSpeed(OsmWay way, EdgeType edgeType) {
        return edgeType.apply(speedExtractor, way.getTags()).floatValue();
    }

    /**
     * Return subset of {@code ModeOfTransports} for which the way is bidirectional (isn't one-way).
     */
    private Set<TransportMode> getBidirectionalModes(OsmWay way, Set<TransportMode> ModeOfTransports) {
        return ModeOfTransports.stream().filter(mode -> isBidirectional(way, mode)).collect(toSet());
    }

    private boolean isBidirectional(OsmWay way, TransportMode mode) {
        return !oneWayEvaluators.get(mode).test(way.getTags());
    }

    @Override
    public String getSerializationName() {
        return osmFile.getName() + ".ser";
    }


    protected enum EdgeType {
        FORWARD {
            @Override
            protected <T> TagExtractor<T> getExtractor(WayTagExtractor<T> extractor) {
                return extractor::getForwardValue;
            }
        },
        BACKWARD {
            @Override
            protected <T> TagExtractor<T> getExtractor(WayTagExtractor<T> extractor) {
                return extractor::getBackwardValue;
            }
        };

        /**
         * Get corresponding tag extractor function.
         *
         * @param extractor
         * @param <T>
         * @return
         */
        protected abstract <T> TagExtractor<T> getExtractor(WayTagExtractor<T> extractor);

        /**
         * Applies corresponding method of the {@code extractor} on the {@code tags}.
         *
         * @param extractor
         * @param tags
         * @param <T>
         * @return
         */
        public <T> T apply(WayTagExtractor<T> extractor, Map<String, String> tags) {
            return getExtractor(extractor).apply(tags);
        }
    }
}
