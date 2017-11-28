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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Marek Cuchý
 */
public class OsmWay extends OsmElement {

	private final List<Long> nodes = new ArrayList<>();

	public OsmWay(long id) {
		super(id);
	}

	public void addNode(long nodeId) {
		nodes.add(nodeId);
	}

	public List<Long> getNodes() {
		return nodes;
	}

	/**
	 * Remove all nodes not contained in {@code nodeIds} from the way nodes.
	 *
	 * @param nodeIds
	 */
	public void removeMissingNodes(Set<Long> nodeIds) {
		nodes.removeIf(l -> !nodeIds.contains(l));
	}

	@Override
	public String toString() {
		return "OsmWay{" +
				"nodes=" + nodes +
				'}';
	}
}
