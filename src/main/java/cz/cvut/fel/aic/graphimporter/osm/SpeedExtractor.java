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
import java.util.Collections;
import java.util.Map;

/**
 * @author Marek Cuchý
 */
public class SpeedExtractor implements WayTagExtractor<Double> {

	public static final Double DEFAULT_DEFAULT_SPEED = 50d;

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

	/**
	 * Get speed based only on the {@code highway} tag.
	 *
	 * @param tags
	 *
	 * @return Speed in kmh.
	 */
	private Double getDefault(Map<String, String> tags) {
		String highway = tags.get("highway");
		return mapping.getOrDefault(highway, defaultSpeed);
	}

	private Double getMainSpeedInKmh(Map<String, String> tags) {
		return parseSpeedInKmh(tags.get("maxspeed"));
	}

	@Override
	public Double getForwardValue(Map<String, String> tags) {
		return resolve(tags, "maxspeed:forward");
	}

	@Override
	public Double getBackwardValue(Map<String, String> tags) {
		return resolve(tags, "maxspeed:backward");
	}

	private Double resolve(Map<String, String> tags, String key) {
		Double speed = parseSpeedInKmh(tags.get(key));
		if (speed == null) {
			speed = getMainSpeedInKmh(tags);
			if (speed == null) {
				speed = getDefault(tags);
			}
		}
		return speed / 3.6;
	}

	private Double parseSpeedInKmh(String s) {
		if (s == null) return null;
		return Doubles.tryParse(s);
	}
}
