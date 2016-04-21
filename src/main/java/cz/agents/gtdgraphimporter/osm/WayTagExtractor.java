package cz.agents.gtdgraphimporter.osm;

import java.util.Map;

/**
 * @author Marek Cuchý
 */
public interface WayTagExtractor<TValue> {

	/**
	 * Extracts value specific for the forward direction of a way.
	 *
	 * @param tags
	 *
	 * @return
	 */
	public TValue getForwardValue(Map<String, String> tags);

	/**
	 * Extracts value specific for the backward direction of a way.
	 *
	 * @param tags
	 *
	 * @return
	 */
	public TValue getBackwardValue(Map<String, String> tags);
}
