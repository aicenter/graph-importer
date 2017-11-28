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
package cz.cvut.fel.aic.graphimporter.geojson;

import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.TransportMode;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.graphimporter.Importer;
import cz.cvut.fel.aic.graphimporter.structurebuilders.TmpGraphBuilder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalEdge;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalEdgeBuilder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalNode;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalNodeBuilder;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static cz.cvut.fel.aic.graphimporter.geojson.GeoJSONParser.*;

public class GeoJSONReader extends Importer {

    private static final Logger LOGGER = Logger.getLogger(GeoJSONReader.class);

    private final HashMap<String, Integer> nodes;
    private final Transformer projection;
    private JSONArray features;

    private final File geoJsonEdgeFile;
    private final File geoJsonNodeFile;


    private final TmpGraphBuilder<InternalNode, InternalEdge> builder;

    public GeoJSONReader(String geoJsonEdgeFile, String geoJsonNodeFile, Transformer projection) {
        this(new File(geoJsonEdgeFile), new File(geoJsonNodeFile), projection);
    }


    public GeoJSONReader(File geoJsonEdgeFile, File geoJsonNodeFile, Transformer projection) {
        this.projection = projection;
        this.geoJsonEdgeFile = geoJsonEdgeFile;
        this.geoJsonNodeFile = geoJsonNodeFile;
        this.nodes = new HashMap<>();
        builder = new TmpGraphBuilder<>();
    }

    @Override
    public String getSerializationName() {
        return geoJsonEdgeFile.getName() + ".ser";
    }

    @Override
    public TmpGraphBuilder<InternalNode, InternalEdge> loadGraph() {
        LOGGER.info("Parsing of geojson started...");

        long t1 = System.currentTimeMillis();

        try (FileReader fr = new FileReader(geoJsonNodeFile)) {
            parseFeatures(fr);
            processFeatures();
        } catch (IOException | ParseException e) {
            throw new IllegalStateException("GeoJSON Nodes can't be parsed.", e);
        }

        try (FileReader fr = new FileReader(geoJsonEdgeFile)) {
            parseFeatures(fr);
            processFeatures();
        } catch (IOException | ParseException e) {
            throw new IllegalStateException("GeoJSON Edges can't be parsed.", e);
        }


        long t2 = System.currentTimeMillis();
        LOGGER.info("Parsing of GeoJSON finished in " + (t2 - t1) + "ms");
        return builder;
    }

    private void processFeatures() {
        for (Object feature : features) {
            parseFeature((JSONObject) feature);
        }
    }


    private int addNode(GPSLocation location, long sourceId) {
        InternalNodeBuilder nodeBuilder = new InternalNodeBuilder(builder.getNodeCount(),
                sourceId, location);
        builder.addNode(nodeBuilder);
        return nodeBuilder.tmpId;
    }

    private void addEdge(int fromId, int toId, JSONObject properties, JSONArray coordinates) {
        try {
            //in case of unknown ID, use negative negative builder edge count
            int uniqueWayId = tryParseInt(properties, "id", -builder.getEdgeCount());
            int oppositeWayUniqueId = tryParseInt(properties, "id_opposite", -1);

            int length = tryParseInt(properties, "length");

            Set<TransportMode> modeOfTransports = new HashSet<>();
            modeOfTransports.add(TransportMode.CAR);

            float allowedMaxSpeedInMpS = tryParseFloat(properties, "speed") / 3.6f;
            int lanesCount = tryParseInt(properties, "lanes", 1);

            List<Map<String, Integer>> lanes = tryParseLanes(properties, "turn:lanes:id");

            List<GPSLocation> coordinateList = new ArrayList<>();
            for (Object coordinate : coordinates) {
                coordinateList.add(getGpsLocation((JSONArray) coordinate, 0));
            }

            // Build edge
            InternalEdgeBuilder edgeBuilder = new InternalEdgeBuilder(fromId, toId, uniqueWayId, oppositeWayUniqueId,
                    length, modeOfTransports, allowedMaxSpeedInMpS, lanesCount, coordinateList, lanes);
            builder.addEdge(edgeBuilder);

        } catch (GeoJSONException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private int getOrCreateNode(JSONArray latLon, JSONObject properties) {
        String nodesIdKey = "node_id";
        int elevation = 0;
        String coordinatesString = latLon.toString();

        long sourceId = tryParseLong(properties, nodesIdKey, -1);

        if (!nodes.containsKey(coordinatesString)) {
            GPSLocation location = getGpsLocation(latLon, elevation);
            addNode(location, sourceId);
            int id = builder.getIntIdForSourceId(sourceId);
            nodes.put(coordinatesString, id);
        }
        return nodes.get(coordinatesString);
    }

    private GPSLocation getGpsLocation(JSONArray latlonArray, int elevation) {
        double lat = (double) latlonArray.get(1);
        double lon = (double) latlonArray.get(0);
        return GPSLocationTools.createGPSLocation(lat, lon, Math.round(elevation), projection);
    }

    private List<Map<String, Integer>> tryParseLanes(JSONObject properties, String key) {
        Map<String, Object> data;
        List<Map<String, Integer>> lanes = new ArrayList<>();
        try {
            List<JSONObject> list = tryParseList(properties, key);
            if (list == null) {
                return null;
            }
            for (JSONObject l : list) {
                Map<String, Integer> lane = new HashMap<>();
                data = tryParseDict(l, null);
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    String keyD = entry.getKey();
                    Object valueD = entry.getValue();
                    Integer valueN = -1;

                    if (valueD instanceof Integer) {
                        valueN = (Integer) valueD;
                    } else if (valueD instanceof Long) {
                        Long val = (Long) valueD;
                        valueN = val.intValue();
                    }

                    lane.put(keyD, valueN);
                }
                lanes.add(lane);
            }
        } catch (GeoJSONException e) {
            LOGGER.warn(e);
            return null;
        }
        return lanes;
    }

    private void parseFeatures(FileReader fileReader) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(fileReader);
        JSONObject jsonObject = (JSONObject) obj;
        this.features = (JSONArray) jsonObject.get("features");
    }

    private void parseFeature(JSONObject feature) {
        JSONObject properties = (JSONObject) feature.get("properties");
        JSONObject geometry = (JSONObject) feature.get("geometry");
        JSONArray coordinates = (JSONArray) geometry.get("coordinates");

        String geometryType = (String) geometry.get("type");
        Boolean isOneWay = true;
        if (properties.containsKey("oneway")) {
            isOneWay = ((String) properties.get("oneway")).equalsIgnoreCase("yes");
        }
        if (geometryType.equals("LineString")) {
            JSONArray fromLatLon = (JSONArray) coordinates.get(0);
            JSONArray toLatlon = (JSONArray) coordinates.get(coordinates.size() - 1);
            int fromId = getOrCreateNode(fromLatLon, properties);
            int toId = getOrCreateNode(toLatlon, properties);
            addEdge(fromId, toId, properties, coordinates);
            if (!isOneWay) {
                addEdge(toId, fromId, properties, coordinates);
            }
        } else if (geometryType.equals("Point")) {
            getOrCreateNode(coordinates, properties);
        }
    }
}
