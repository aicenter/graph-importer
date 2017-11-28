/* 
 * Copyright (C) 2017 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
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
