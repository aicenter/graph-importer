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
package cz.cvut.fel.aic.graphimporter.osm.element;

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
