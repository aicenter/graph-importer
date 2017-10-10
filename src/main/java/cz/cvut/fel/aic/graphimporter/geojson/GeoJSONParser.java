package cz.cvut.fel.aic.graphimporter.geojson;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.*;

/**
 * @author Zdenek Bousa
 */
public class GeoJSONParser {
    static float tryParseFloat(JSONObject properties, String key, float defaultValue) {
        float value;
        try {
            value = tryParseFloat(properties, key);
        } catch (GeoJSONException e) {
            value = defaultValue;
        }
        return value;
    }

    static float tryParseFloat(JSONObject properties, String key) throws GeoJSONException {
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

    static Long tryParseLong(JSONObject properties, String key, long defaultValue) {
        Long value;
        try {
            value = tryParseLong(properties, key);
        } catch (GeoJSONException e) {
            value = defaultValue;
        }
        return value;
    }

    static Long tryParseLong(JSONObject properties, String key) throws GeoJSONException {
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

    static int tryParseInt(JSONObject properties, String key, int defaultValue) {
        int value;
        try {
            value = tryParseInt(properties, key);
        } catch (GeoJSONException e) {
            value = defaultValue;
        }
        return value;
    }

    static int tryParseInt(JSONObject properties, String key) throws GeoJSONException {
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


    static Map<String, Object> tryParseDict(JSONObject properties, String key) throws GeoJSONException {
        Map<String, Object> map = null;
        JSONObject value;
        if (key != null) {
            if (properties != null) {
                value = (JSONObject) properties.get(key);
            } else {
                return null;
            }
        } else {
            value = properties;
        }
        if (value != null) {
            map = getMap(value);
        }
        return map;
    }

    static List<JSONObject> tryParseList(JSONObject properties, String key) throws GeoJSONException {
        JSONArray m = (JSONArray) properties.get(key);
        if (m == null){
            return null;
        }

        List<JSONObject> list = new ArrayList<>();
        for (Object aM : m) {
            if (aM instanceof JSONObject) {
                list.add((JSONObject) aM);
            } else {
                throw new GeoJSONException(properties, key);
            }
        }
        return list;
    }

    static JSONObject parseStringToJSON(String tagsString) {
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

    private static Map<String, Object> getMap(JSONObject object) throws GeoJSONException {
        Map<String, Object> map = new HashMap<>();

        Set keys = object.keySet();
        for (Object keyO : keys) {
            Object value = object.get(keyO);

            if (value instanceof JSONArray) {
                value = getList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = getMap((JSONObject) value);
            } 

            if (keyO instanceof String) {
                map.put((String) keyO, value);
            } else {
                return null;
            }
        }
        return map;
    }

    private static List<Object> getList(JSONArray array) throws GeoJSONException {
        List<Object> list = new ArrayList<>();
        for (Object value : array) {
            if (value instanceof JSONArray) {
                value = getList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = getMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }
}
