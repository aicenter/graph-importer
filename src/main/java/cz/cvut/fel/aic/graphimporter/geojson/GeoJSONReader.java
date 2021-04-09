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
import cz.cvut.fel.aic.geographtools.UTM;
import cz.cvut.fel.aic.geographtools.util.GPSLocationTools;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import cz.cvut.fel.aic.graphimporter.Importer;
import cz.cvut.fel.aic.graphimporter.structurebuilders.TmpGraphBuilder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalEdge;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalEdgeBuilder;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalNode;
import cz.cvut.fel.aic.graphimporter.structurebuilders.internal.InternalNodeBuilder;
import cz.cvut.fel.aic.graphimporter.util.MD5ChecksumGenerator;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class GeoJSONReader extends Importer {

	private static final Logger LOGGER = Logger.getLogger(GeoJSONReader.class);

	private final HashMap<String, Integer> nodes;
	private final Transformer projection;
	private JSONArray features;

	private final File geoJsonEdgeFile;

	private final File geoJsonNodeFile;

	private final String geoJsonSerializedGraphFile;

	private final String geoJsonSerializedBasePath;

	private boolean isBothWayOverride = false;

	protected final TmpGraphBuilder<InternalNode, InternalEdge> builder;
	
	
	private int removedParallelEdgesCount = 0;
	
	

	public GeoJSONReader(String geoJsonEdgeFile, String geoJsonNodeFile, String geoJsonSerializedGraphFile, Transformer projection) {
		this(new File(geoJsonEdgeFile), new File(geoJsonNodeFile), getSerializedGraphNameWithChecksum(geoJsonSerializedGraphFile, geoJsonEdgeFile), geoJsonSerializedGraphFile, projection);
	}

	public GeoJSONReader(String geoJsonEdgeFile, String geoJsonNodeFile, Transformer projection) {
		this(new File(geoJsonEdgeFile), new File(geoJsonNodeFile), projection);
	}

	public GeoJSONReader(File geoJsonEdgeFile, File geoJsonNodeFile, String geoJsonSerializedGraphFile, String geoJsonSerializedBasePath, Transformer projection) {
		this.projection = projection;
		this.geoJsonEdgeFile = geoJsonEdgeFile;
		this.geoJsonNodeFile = geoJsonNodeFile;
		this.geoJsonSerializedGraphFile = geoJsonSerializedGraphFile;
		this.geoJsonSerializedBasePath = geoJsonSerializedBasePath;
		this.nodes = new HashMap<>();

		builder = new TmpGraphBuilder<>();
	}


	public GeoJSONReader(File geoJsonEdgeFile, File geoJsonNodeFile, Transformer projection) {
		this.projection = projection;
		this.geoJsonEdgeFile = geoJsonEdgeFile;
		this.geoJsonNodeFile = geoJsonNodeFile;
		this.geoJsonSerializedGraphFile =  getSerializedGraphNameWithChecksum(defaultSerializedGraphFile() ,geoJsonEdgeFile);
		this.geoJsonSerializedBasePath = defaultSerializedGraphFile() + ".ser";
		this.nodes = new HashMap<>();

		builder = new TmpGraphBuilder<>();
	}


	private void processFeatures() {
		for (Object feature : features) {
			parseFeature((JSONObject) feature);
		}
	}


	int addNode(GPSLocation location, long sourceId, Map<String, Object> otherParams) {
		InternalNodeBuilder nodeBuilder = new InternalNodeBuilder(builder.getNodeCount(),
				sourceId, location, otherParams);
		builder.addNode(nodeBuilder);
		return nodeBuilder.tmpId;
	}


	void parseFeature(JSONObject feature) {
		JSONObject properties = (JSONObject) feature.get("properties");
		JSONObject geometry = (JSONObject) feature.get("geometry");
		JSONArray coordinates = (JSONArray) geometry.get("coordinates");

		String geometryType = (String) geometry.get("type");
//		Boolean isOneWay = true;
//		if (properties.containsKey("oneway")) {
//			isOneWay = ((String) properties.get("oneway")).equalsIgnoreCase("yes");
//		}
		if (geometryType.equals("LineString")) {
                    
			JSONArray fromLatLon = (JSONArray) coordinates.get(0);
			JSONArray toLatlon = (JSONArray) coordinates.get(coordinates.size() - 1);
			int fromId = getOrCreateNode(fromLatLon, properties);
			int toId = getOrCreateNode(toLatlon, properties);
                        
                        
			addEdge(fromId, toId, properties, coordinates);
//			if (!isOneWay || isBothWayOverride) {
//				addEdge(toId, fromId, properties, coordinates);
//			}
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
			int uniqueWayId = builder.getEdgeCount();
			int oppositeWayUniqueId = -1;
			int lengthCm = tryParseInt(properties, "length");
//		int length = GPSLocationTools.computeDistance(graphBuilder.getNode(fromId).location, graphBuilder.getNode(toId).location);
			Set<TransportMode> modeOfTransports = new HashSet<>();
			modeOfTransports.add(TransportMode.CAR);
			int allowedMaxSpeed = tryParseInt(properties, "maxspeed");
			int lanesCount = tryParseInt(properties, "lanes");
                        
			List<GPSLocation> coordinateList = new ArrayList<>();
			for (int i = 0; i < coordinates.size(); i++) {
				coordinateList.add(getGpsLocation((JSONArray) coordinates.get(i),0));
				assert UTM.checkLocationValidUTM(coordinateList.get(i));
			}
			InternalEdgeBuilder edgeBuilder = new InternalEdgeBuilder(fromId, toId, uniqueWayId, oppositeWayUniqueId,
				lengthCm, modeOfTransports, allowedMaxSpeed, lanesCount, coordinateList, properties);

			if(builder.containsEdge(fromId, toId)){
				removedParallelEdgesCount++;
				InternalEdgeBuilder oldEdgeBuilder = (InternalEdgeBuilder) builder.getEdge(fromId, toId);
				
				double oldRatio = oldEdgeBuilder.getParam("speed_unit").equals("kmh") ? 3.6 : 2.2369362920544;
				double oldSpeedCmPS = (double) oldEdgeBuilder.allowedMaxSpeedInKmh * 1E2 / oldRatio;
				double oldEdgeTravelTime = (double) oldEdgeBuilder.getLengthCm() / oldSpeedCmPS;
				
				double newRatio = properties.get("speed_unit").equals("kmh") ? 3.6 : 2.2369362920544;
				double newSpeedCmPS = (double) allowedMaxSpeed * 1E2 / newRatio;
				double newEdgeTravelTime = (double) lengthCm / newSpeedCmPS;
				
				InternalEdgeBuilder discardedEdge = oldEdgeTravelTime <= newEdgeTravelTime ? edgeBuilder : oldEdgeBuilder;
				String msg = String.format("Disscarded edge: %s from: %s to: %s (old travel time: %s, new travel time: %s, discarded %s)", 
						discardedEdge.uniqueWayID, discardedEdge.getParam("from_osm_id"), discardedEdge.getParam("to_osm_id"),
						oldEdgeTravelTime, newEdgeTravelTime, oldEdgeTravelTime <= newEdgeTravelTime ? "new": "old");
				LOGGER.info(msg);
				if(oldEdgeTravelTime <= newEdgeTravelTime){
					//new edge is longer than current, we dont need it
					return;
				}
				
				//new edge is shorter than current one, we need to switch them
				builder.remove(oldEdgeBuilder);
			}                        
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
//			System.out.println(location +" "+location.lonProjected+" "+location.latProjected);
			Map<String,Object> otherParams = properties;
			addNode(location, sourceId, otherParams);
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

//	public static void main(String[] args) {
//		File geoJsonEdgeFile = new File("/home/martin/projects/skoda/skoda/osm_lines.geojson");
//		Transformer projection = new Transformer(2065);
//		TmpGraphBuilder<SimulationNode, SimulationEdge> builder = new TmpGraphBuilder<SimulationNode, SimulationEdge>();
//		GeoJSONReader reader = null;
//		try (FileReader fr = new FileReader((geoJsonEdgeFile))) {
//			reader = new GeoJSONReader(fr, projection,new HashMap<>());
//		} catch (IOException | ParseException e) {
//			e.printStackTrace();
//		}
//		TmpGraphBuilder<SimulationNode, SimulationEdge> graphBuilder = null;
//
//		graphBuilder = reader.parseGraphFeatures(builder);
//
//		assert graphBuilder != null;
//		Graph<SimulationNode, SimulationEdge> graph = graphBuilder.createGraph();
//		System.out.println(graph);
//
//
//	}

	@Override
	public String getSerializedGraphName() {
		return geoJsonSerializedGraphFile;
	}

	@Override
	public String getSerializedBasePath() {
		return geoJsonSerializedBasePath;
	}

	@Override
	public TmpGraphBuilder<InternalNode, InternalEdge> loadGraph() {
		parseGEOJSON();
		return builder;
	}

	protected void parseGEOJSON() {
		LOGGER.info("Parsing of geojson started - node file: " + geoJsonNodeFile);

		long t1 = System.currentTimeMillis();

		try (FileReader fr = new FileReader(geoJsonNodeFile)) {
			parseFeatures(fr);
			processFeatures();
		} catch (IOException | ParseException e) {
			throw new IllegalStateException("GeoJSON Nodes can't be parsed.", e);
		}

		LOGGER.info("Parsing of geojson started - edge file: " + geoJsonEdgeFile);
		try (FileReader fr = new FileReader(geoJsonEdgeFile)) {
			parseFeatures(fr);
			processFeatures();
		} catch (IOException | ParseException e) {
			throw new IllegalStateException("GeoJSON Edges can't be parsed.", e);
		}


		long t2 = System.currentTimeMillis();
		LOGGER.info("Parsing of GeoJSON finished in " + (t2 - t1) + "ms");
		if(removedParallelEdgesCount > 0){
			LOGGER.info(removedParallelEdgesCount + " parallel edges discarded");
		}
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

	private static String getSerializedGraphNameWithChecksum(String basePath, String edgesFilePath) {
		File edgesFile = new File(edgesFilePath);
		return getSerializedGraphNameWithChecksum(basePath, edgesFile);
	}

	private static String getSerializedGraphNameWithChecksum(String basePath, File edgesFile) {
		return basePath + MD5ChecksumGenerator.getGraphChecksum(edgesFile) + ".ser";
	}

	private static String defaultSerializedGraphFile() {
		return "data/serialized/graph";
	}
	
}
