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

public class GeoJSONReader extends Importer {

    private static final Logger LOGGER = Logger.getLogger(GeoJSONReader.class);

    private final HashMap<String, Integer> nodes;
    private final Transformer projection;
    private JSONArray features;

    private final File geoJsonFile;

    private final File geoJsonNodeFile;


    private boolean isBothWayOverride = false;

    protected final TmpGraphBuilder<InternalNode, InternalEdge> builder;

    public GeoJSONReader(String geoJsonFile, String geoJsonNodeFile, Transformer projection) {
        this(new File(geoJsonFile), new File(geoJsonNodeFile), projection);
    }


    public GeoJSONReader(File geoJsonFile, File geoJsonNodeFile, Transformer projection) {
        this.projection = projection;
        this.geoJsonFile = geoJsonFile;
        this.geoJsonNodeFile = geoJsonNodeFile;
        this.nodes = new HashMap<>();

        builder = new TmpGraphBuilder<>();
    }


    private void processFeatures() {
        for (Object feature : features) {
            parseFeature((JSONObject) feature);
        }
    }


    int addNode(GPSLocation location, long sourceId) {
        InternalNodeBuilder nodeBuilder = new InternalNodeBuilder(builder.getNodeCount(),
                sourceId, location);
        builder.addNode(nodeBuilder);
        return nodeBuilder.tmpId;
    }


    void parseFeature(JSONObject feature) {
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
            if (!isOneWay || isBothWayOverride) {
                addEdge(toId, fromId, properties, coordinates);
            }
        } else if (geometryType.equals("Point")) {
            int fromId = getOrCreateNode(coordinates, properties);
        }


    }

    private JSONObject parseStringToJSON(String tagsString) {
        if (tagsString == null) return null;
        tagsString = "{" + tagsString.replaceAll("=>", ":") + "}";
        JSONObject json;
        JSONParser parser = new JSONParser();
        try {
            json = (JSONObject) parser.parse(tagsString);
        } catch (ParseException e) {
            e.printStackTrace();
            json = null;
        }
        return json;
    }


    void addEdge(int fromId, int toId, JSONObject properties, JSONArray coordinates) {
        Long osmId = null;
        try {
            osmId = tryParseLong(properties, "id");
            int uniqueWayId = builder.getEdgeCount();
            int oppositeWayUniqueId = -1;
            int length = tryParseInt(properties, "length");
//        int length = GPSLocationTools.computeDistance(graphBuilder.getNode(fromId).location, graphBuilder.getNode(toId).location);
            Set<TransportMode> modeOfTransports = new HashSet<>();
            modeOfTransports.add(TransportMode.CAR);
            float allowedMaxSpeedInMpS = tryParseFloat(properties, "speed") / 3.6f;
            int lanesCount = tryParseInt(properties, "lanes");
            List<GPSLocation> coordinateList = new ArrayList<>();
            for (int i = 0; i < coordinates.size(); i++) {
                coordinateList.add(getGpsLocation((JSONArray) coordinates.get(i),0));
            }
            InternalEdgeBuilder edgeBuilder = new InternalEdgeBuilder(fromId, toId, osmId, uniqueWayId, oppositeWayUniqueId,
                    length, modeOfTransports, allowedMaxSpeedInMpS, lanesCount, coordinateList);
            builder.addEdge(edgeBuilder);
        } catch (GeoJSONException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private float tryParseFloat(JSONObject properties, String key, float defaultValue) {
        float value;
        try {
            value = tryParseFloat(properties, key);
        } catch (GeoJSONException e) {
            value = defaultValue;
        }
        return value;
    }

    private float tryParseFloat(JSONObject properties, String key) throws GeoJSONException {
        float value;
        Object valueObject = properties.get(key);
        if (valueObject instanceof Number) {
            value = ((Number) valueObject).floatValue();
        } else if (valueObject instanceof String) {
            String valueString = (String) valueObject;
            value = Float.parseFloat(valueString);
        } else {
            throw new GeoJSONException(properties, key);
        }
        return value;
    }

    private Long tryParseLong(JSONObject properties, String key, long defaultValue) {
        Long value;
        try {
            value = tryParseLong(properties, key);
        } catch (GeoJSONException e) {
            value = defaultValue;
        }
        return value;
    }

    private Long tryParseLong(JSONObject properties, String key) throws GeoJSONException {
        Object valueObject = properties.get(key);
        Long value;
        if (valueObject instanceof Number) {
            value = ((Number) valueObject).longValue();
        } else if (valueObject instanceof String) {
            String valueString = (String) valueObject;
            value = Long.parseLong(valueString);
        } else {
            throw new GeoJSONException(properties, key);
        }
        return value;
    }

    private int tryParseInt(JSONObject properties, String key, int defaultValue) {
        int value;
        try {
            value = tryParseInt(properties, key);
        } catch (GeoJSONException e) {
            value = defaultValue;
        }
        return value;
    }

    private int tryParseInt(JSONObject properties, String key) throws GeoJSONException {
        int value;
        Object valueObject = properties.get(key);
        if (valueObject instanceof Number) {
            value = ((Number) valueObject).intValue();
        } else if (valueObject instanceof String) {
            String valueString = (String) valueObject;
            value = Integer.parseInt(valueString);
        } else {
            throw new GeoJSONException(properties, key);
        }
        return value;
    }


    private int getOrCreateNode(JSONArray latLon, JSONObject properties) {
        String nodesIdKey = "node_id";
        int elevation = 0;
        String coordinatesString = latLon.toString();


        long sourceId = tryParseLong(properties, nodesIdKey, -1);


        if (!nodes.containsKey(coordinatesString)) {
            GPSLocation location = getGpsLocation(latLon, elevation);
//            System.out.println(location +" "+location.lonProjected+" "+location.latProjected);
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

    public void setIsBothWayOverride(boolean isBothWayOverride) {
        this.isBothWayOverride = isBothWayOverride;
    }

//    public static void main(String[] args) {
//        File geoJSONFile = new File("/home/martin/projects/skoda/skoda/osm_lines.geojson");
//        Transformer projection = new Transformer(2065);
//        TmpGraphBuilder<SimulationNode, SimulationEdge> builder = new TmpGraphBuilder<SimulationNode, SimulationEdge>();
//        GeoJSONReader reader = null;
//        try (FileReader fr = new FileReader((geoJSONFile))) {
//            reader = new GeoJSONReader(fr, projection,new HashMap<>());
//        } catch (IOException | ParseException e) {
//            e.printStackTrace();
//        }
//        TmpGraphBuilder<SimulationNode, SimulationEdge> graphBuilder = null;
//
//        graphBuilder = reader.parseGraphFeatures(builder);
//
//        assert graphBuilder != null;
//        Graph<SimulationNode, SimulationEdge> graph = graphBuilder.createGraph();
//        System.out.println(graph);
//
//
//    }

    @Override
    public String getSerializationName() {
        return geoJsonFile.getName() + ".ser";
    }

    @Override
    public TmpGraphBuilder<InternalNode, InternalEdge> loadGraph() {
        parseGEOJSON();
        return builder;
    }

    protected void parseGEOJSON() {
        LOGGER.info("Parsing of geojson started...");

        long t1 = System.currentTimeMillis();

        try (FileReader fr = new FileReader(geoJsonNodeFile)) {
            parseFeatures(fr);
            processFeatures();
        } catch (IOException | ParseException e) {
            throw new IllegalStateException("GeoJSON Nodes can't be parsed.", e);
        }

        try (FileReader fr = new FileReader(geoJsonFile)) {
            parseFeatures(fr);
            processFeatures();
        } catch (IOException | ParseException e) {
            throw new IllegalStateException("GeoJSON Edges can't be parsed.", e);
        }


        long t2 = System.currentTimeMillis();
        LOGGER.info("Parsing of GeoJSON finished in " + (t2 - t1) + "ms");
    }

    private void parseFeatures(FileReader fileReader) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(fileReader);
        JSONObject jsonObject = (JSONObject) obj;
        this.features = (JSONArray) jsonObject.get("features");
    }

    private class GeoJSONException extends Exception {
        GeoJSONException(JSONObject properties, String key) {
            super("Missing key: \'" + key + "\' in GeoJSON properties: " + properties);
        }
    }
}
