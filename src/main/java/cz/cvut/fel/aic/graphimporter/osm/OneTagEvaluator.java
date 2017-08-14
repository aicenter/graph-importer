package cz.cvut.fel.aic.graphimporter.osm;

import java.util.Map;

/**
 * @author Marek Cuchý
 */
public class OneTagEvaluator implements TagEvaluator {

	private final String key;
	private final String value;

	public OneTagEvaluator(String key, String value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public boolean test(Map<String, String> tags) {
		String val = tags.get(key);
		if (val == null) return false;
		if (value.equals("*") || value.equals(val)) return true;
		return false;
	}
}
