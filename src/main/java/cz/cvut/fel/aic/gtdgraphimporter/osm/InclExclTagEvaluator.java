package cz.cvut.fel.aic.gtdgraphimporter.osm;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Evaluator that allows to define one set of tags where at least one is necessary (not sufficient) for positive
 * evaluation, second set of tags ensuring negative evaluation and third set of tags ensuring negative evaluation unless
 * any of tags in fourth set appears.
 *
 * @author Marek Cuchý
 */
public class InclExclTagEvaluator implements TagEvaluator {

	private final Map<String, Set<String>> include;
	private final Map<String, Set<String>> exclude;
	private final ExclUnless excludeUnless;

	private InclExclTagEvaluator() {
		this(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
	}

	/**
	 * Create evaluator which returns true for tags included at least in one record in {@code include} but not included
	 * in any record in {@code exclude} and also not included in any record in {@code excludeUnless} unless any of the
	 * tags is included in {@code unless}, then {@code excludeUnless} is ignored. All parameters are in format {key1 ->
	 * {v1,v2,v3,...}, key -> ...}
	 *
	 * @param include
	 * 		Map of tags necessary for positive evaluation. At least one has to be contained.
	 * @param exclude
	 * 		Map of tags forbidding positive evaluation. If one is contained, the evaluation is false.
	 * @param excludeUnless
	 * 		Map of tags forbidding positive evaluation. If one is contained and none of the element tags is contained
	 * 		in {@code unless} tag map, the evaluation is false.
	 * @param unless
	 * 		Map of tags reducing the power of {@code excludeUnless}. If any of the tags in {@code unless} is contained
	 * 		in element tags, {@code excludeUnless} is ignored.
	 */
	public InclExclTagEvaluator(Map<String, Set<String>> include, Map<String, Set<String>> exclude, Map<String,
			Set<String>> excludeUnless, Map<String, Set<String>> unless) {
		this.include = include;
		this.exclude = exclude;
		this.excludeUnless = new ExclUnless(excludeUnless, unless);
	}

	@Override
	public boolean test(Map<String, String> tags) {

		boolean included = false;

		for (Entry<String, String> tag : tags.entrySet()) {
			if (contains(exclude, tag) || excludeUnless.test(tag, tags)) {
				return false;
			}

			if (contains(include, tag)) {
				included = true;
			}
		}
		return included;
	}

	private static boolean contains(Map<String, Set<String>> tagCondition, Entry<String, String> tag) {
		Set<String> inclValue = tagCondition.get(tag.getKey());
		return inclValue != null && (inclValue.contains("*") || inclValue.contains(tag.getValue()));
	}

	public Map<String, Set<String>> getInclude() {
		return include;
	}

	public Map<String, Set<String>> getExclude() {
		return exclude;
	}

	public ExclUnless getExcludeUnless() {
		return excludeUnless;
	}

	private static class ExclUnless {

		private final Map<String, Set<String>> exclude;
		private final Map<String, Set<String>> unless;

		private ExclUnless() {
			this(null, null);
		}

		private ExclUnless(Map<String, Set<String>> exclude, Map<String, Set<String>> unless) {
			this.exclude = exclude;
			this.unless = unless;
		}

		public Map<String, Set<String>> getExclude() {
			return exclude;
		}

		public Map<String, Set<String>> getUnless() {
			return unless;
		}

		public boolean test(Entry<String, String> tag, Map<String, String> tags) {
			if (contains(exclude, tag)) {
				for (Entry<String, String> t : tags.entrySet()) {
					if (contains(unless, t)) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
	}
}
