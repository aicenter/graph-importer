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
