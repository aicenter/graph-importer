package cz.agents.gtdgraphimporter.osm;

import cz.agents.gtdgraphimporter.osm.TagExtractor;

import java.util.Collections;
import java.util.Map;

/**
 * @author Marek Cuchý
 */
public class SpeedExtractor implements TagExtractor<Double> {

	public static final Double DEFAULT_DEFAULT_SPEED = new Double(50);

	private final Double defaultSpeed;
	private final Map<String, Double> mapping;

	private SpeedExtractor() {
		this(Collections.emptyMap(), DEFAULT_DEFAULT_SPEED);
	}

	/**
	 * @param mapping
	 * 		Mapping between 'highway' tag values and speed. It should contain 'default' mapping.
	 * @param defaultSpeed
	 * 		Speed used for tag values not defined in {@code mapping}.
	 */
	public SpeedExtractor(Map<String, Double> mapping, double defaultSpeed) {
		this.mapping = mapping;
		this.defaultSpeed = defaultSpeed;
		this.mapping.remove("default");
	}

	@Override
	public Double apply(Map<String, String> tags) {
		String highway = tags.get("highway");
		return mapping.getOrDefault(highway, defaultSpeed) / 3.6;
	}

	public Double getDefaultSpeed() {
		return defaultSpeed;
	}

	public Map<String, Double> getMapping() {
		return mapping;
	}

	public double getSpeed(Map<String, String> tags) {
		return apply(tags);
	}
}
