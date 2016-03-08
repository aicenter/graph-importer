package cz.agents.gtdgraphimporter.osm;

import java.util.Map;
import java.util.function.Predicate;

/**
 * @author Marek Cuchý
 */
@FunctionalInterface
public interface TagEvaluator extends Predicate<Map<String, String>> {

	public static TagEvaluator ALWAYS_FALSE = t -> false;
	public static TagEvaluator ALWAYS_TRUE = t -> true;
}
