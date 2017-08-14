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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

public class GeoJSONReader extends Importer{
    
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
        String otherTagsString = (String) properties.get("other_tags");
        JSONObject otherTags = parseStringToJSON(otherTagsString);
        Boolean isOneWay = true;
        if (otherTags != null && otherTags.containsKey("oneway")) {
            isOneWay = ((String) otherTags.get("oneway")).equalsIgnoreCase("yes");
        }
        if (geometryType.equals("LineString")) {
            for (int i = 0; i < coordinates.size() - 1; i++) {
                JSONArray fromLatLon = (JSONArray) coordinates.get(i);
                JSONArray toLatlon = (JSONArray) coordinates.get(i + 1);
                int fromId = getOrCreateNode(fromLatLon, properties);
                int toId = getOrCreateNode(toLatlon, properties);
                addEdge(fromId, toId, properties);
                if (!isOneWay || isBothWayOverride) {
                    addEdge(toId, fromId, properties);
                }
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


    void addEdge(int fromId, int toId, JSONObject properties) {
        Long osmId = tryParseLong(properties, "osm_id", 0l);
        int uniqueWayId = builder.getEdgeCount();
        int oppositeWayUniqueId = -1;
        int length = tryParseInt(properties, "length",-1);
//        int length = GPSLocationTools.computeDistance(graphBuilder.getNode(fromId).location, graphBuilder.getNode(toId).location);
        Set<TransportMode> modeOfTransports = new HashSet<>();
        modeOfTransports.add(TransportMode.CAR);
        float allowedMaxSpeedInMpS = tryParseInt(properties, "speed", 15);
        int lanesCount = tryParseInt(properties, "lanes", 1);
        InternalEdgeBuilder edgeBuilder = new InternalEdgeBuilder(fromId, toId, osmId, uniqueWayId, oppositeWayUniqueId,
                length, modeOfTransports, allowedMaxSpeedInMpS, lanesCount);
        builder.addEdge(edgeBuilder);
    }

    private Long tryParseLong(JSONObject properties, String key, long defaultValue) {
        Long value = defaultValue;
        Object valueObject = properties.get(key);

        if (valueObject instanceof Number) {
            value = (long) valueObject;
        } else if (valueObject instanceof String) {
            String valueString = (String) valueObject;
            value = Long.parseLong(valueString);
        }
        return value;
    }

    private int tryParseInt(JSONObject properties, String key, int defaultValue) {
        int value = defaultValue;
        Object valueObject = properties.get(key);
        if (valueObject instanceof Number) {
            value = ((Number) valueObject).intValue();
        } else if (valueObject instanceof String) {
            String valueString = (String) valueObject;
            value = Integer.parseInt(valueString);
        }
        return value;
    }

    private int getOrCreateNode(JSONArray latLon, JSONObject properties) {
        int elevation = 0;
        String coordinatesString = latLon.toString();
        long sourceId = -1;
        if (properties.containsKey("id")) {
            sourceId = (long) properties.get("id");
        }

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

        HashMap<String, Integer> nodesMapping = new HashMap<>();

        long t1 = System.currentTimeMillis();
        try (FileReader fr = new FileReader(geoJsonFile)) {
            parseFeatures(fr);
            processFeatures();
        } catch (IOException | ParseException e) {
            throw new IllegalStateException("GeoJSON can't be parsed.", e);
        }
        try (FileReader fr = new FileReader(geoJsonNodeFile)) {
            parseFeatures(fr);
            processFeatures();
        } catch (IOException | ParseException e) {
            throw new IllegalStateException("GeoJSON can't be parsed.", e);
        }

        long t2 = System.currentTimeMillis();
        LOGGER.info("Parsing of GeoJSON finished in " + (t2 - t1) + "ms");
    }
    
    private void parseFeatures(FileReader fileReader) throws IOException, ParseException{
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(fileReader);
        JSONObject jsonObject = (JSONObject) obj;
        this.features = (JSONArray) jsonObject.get("features");
    }
}
