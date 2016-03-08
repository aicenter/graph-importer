package cz.agents.gtdgraphimporter.osm.element;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marek Cuchý
 */
public abstract class OsmElement {

	public final long id;

	private final Map<String, String> tags = new HashMap<>();

	public OsmElement(long id) {
		this.id = id;
	}

	public void addTag(String key, String value) {
		tags.put(key, value);
	}

	public void clearTags() {
		tags.clear();
	}

	public Map<String, String> getTags() {
		return tags;
	}

	public long getId() {
		return id;
	}

	@Override
	public String toString() {
		return "" +
				"id=" + id +
				", tags=" + tags
				;
	}
}
