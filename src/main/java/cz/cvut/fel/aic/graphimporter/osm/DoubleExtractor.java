package cz.cvut.fel.aic.graphimporter.osm;

import com.google.common.primitives.Doubles;

import java.util.Map;

/**
 * @author Marek Cuchý
 */
public class DoubleExtractor implements TagExtractor<Double> {

	private final String tagKey;
	private final double defaultValue;

	public DoubleExtractor(String tagKey, double defaultValue) {
		this.defaultValue = defaultValue;
		this.tagKey = tagKey;
	}

	@Override
	public Double apply(Map<String, String> tags) {
		String value = tags.get(tagKey);
		Double doubleVal = null;
		if (value != null) {
			doubleVal = Doubles.tryParse(value);
		}
		return doubleVal == null ? defaultValue : doubleVal;
	}

	public double applyPrimitive(Map<String, String> tags) {
		return apply(tags);
	}
}
