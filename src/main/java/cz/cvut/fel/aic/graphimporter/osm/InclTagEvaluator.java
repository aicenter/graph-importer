package cz.cvut.fel.aic.graphimporter.osm;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Marek Cuchý
 */
public class InclTagEvaluator implements TagEvaluator {

	private final Map<String, Set<String>> include;

	private InclTagEvaluator() {
		this.include = null;
	}

	public InclTagEvaluator(Map<String, Set<String>> include) {
		this.include = include;
	}

	@Override
	public boolean test(Map<String, String> tags) {
		for (Entry<String, String> entry : tags.entrySet()) {
			if (include(entry)) return true;
		}
		return false;
	}

	protected static boolean contains(Map<String, Set<String>> tagCondition, Entry<String, String> tag) {
		Set<String> inclValue = tagCondition.get(tag.getKey());
		return inclValue != null && (inclValue.contains("*") || inclValue.contains(tag.getValue()));
	}

	protected boolean include(Entry<String, String> tag) {
		return contains(include, tag);
	}

	public Map<String, Set<String>> getInclude() {
		return include;
	}
}
