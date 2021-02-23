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

import com.google.common.primitives.Ints;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * Lanes count extractor.Traffic lanes suitable for vehicles wider than a motorbike.
 * General information - total, forward and backward number of lanes.
 * <p>
 * Does not provide support for reserved and conditional lanes.
 * Additional information:  @see <a href="http://wiki.openstreetmap.org/wiki/Key:lanes">osmWiki</a>
 *
 * @author Zdenek Bousa
 */

public class LanesCountExtractor implements WayTagExtractor<Integer> {
	private static final Logger LOGGER = Logger.getLogger(LanesCountExtractor.class);
	public static final Integer DEFAULT_LANES_COUNT = 1;
	private Integer defaultLanesCount = DEFAULT_LANES_COUNT;

	/**
	 * Constructor will use DEFAULT_LANES_COUNT
	 * <p>
	 * Include this in OsmGraphBuilder
	 * [OsmParser]::bidirectional is important tag to be added to OsmWay tags, before processing this extractor.
	 * [OsmParser]::bidirectional=1 equals True - way is bidirectional
	 * [OsmParser]::bidirectional=0 equals False - way is one-way
	 */
	public LanesCountExtractor() {
		this.defaultLanesCount = DEFAULT_LANES_COUNT;
	}

	/**
	 * Constructor, default value will be divided by 2 on bidirectional edge.
	 * <p>
	 * Include this in OsmGraphBuilder
	 * [OsmParser]::bidirectional is important tag to be added to OsmWay tags, before processing this extractor.
	 * [OsmParser]::bidirectional=1 equals True - way is bidirectional
	 * [OsmParser]::bidirectional=0 equals False - way is one-way
	 *
	 * @param defaultLanesCount - number of default lanes, in case there is no tag in osm.
	 */
	public LanesCountExtractor(Integer defaultLanesCount) {
		this.defaultLanesCount = defaultLanesCount;
	}

	/**
	 * Total number of lanes extractor
	 */
	@Override
	public Integer getForwardValue(Map<String, String> tags) {
		return resolve(tags, "lanes:forward");
	}

	@Override
	public Integer getBackwardValue(Map<String, String> tags) {
		return resolve(tags, "lanes:backward");
	}

	private Integer resolve(Map<String, String> tags, String key) {
		Integer count = parseCount(tags.get(key));
		Integer twoWay;
		try {
			twoWay = parseBiDirectional(tags.get("[OsmParser]::bidirectional")); // tag added in OsmGraphBuilderExtended->line 221
		} catch (Exception exception) {
			LOGGER.warn("Tag [OsmParser]::bidirectional is not presented, all ways with only default " +
					"value [lanes:=x] will be treated as bidirectional");
			twoWay = 1;
		}
		if (count == null) {
			count = getMainValue(tags);
			if (count == null) {
				count = defaultLanesCount; //fallback
			}

			// Based on documentation lanes should be total number of lanes on the road. Therefore for two-way count = number of lanes/2
			// and for one-way count = lanes
			if (twoWay == 1) {
				count = count / 2;
			}

		}
		if (count < 1) {
			count = 1;
		}
		return count;
	}

	private Integer getMainValue(Map<String, String> tags) {
		return parseCount(tags.get("lanes"));
	}


	private Integer parseCount(String s) {
		if (s == null) return null;
		return Ints.tryParse(s);
	}

	private Integer parseBiDirectional(String s) {
		if (s == null) return 0;
		return Ints.tryParse(s);
	}
}
