package cz.agents.gtdgraphimporter.osm;

import java.util.Map;
import java.util.function.Function;

/**
 * Function returning specific value extracted from tags.
 *
 * @author Marek Cuchý
 */
public interface TagExtractor<TValue> extends Function<Map<String, String>, TValue> {

}
