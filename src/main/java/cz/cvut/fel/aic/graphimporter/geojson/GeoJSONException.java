package cz.cvut.fel.aic.graphimporter.geojson;

import org.json.simple.JSONObject;

/**
 * @author Zdenek Bousa
 */
class GeoJSONException extends Exception {
    GeoJSONException(JSONObject properties, String key) {
        super("Missing key: \'" + key + "\' in GeoJSON properties: " + properties);
    }
}
