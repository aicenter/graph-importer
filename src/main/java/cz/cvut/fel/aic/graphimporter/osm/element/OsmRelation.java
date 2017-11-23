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

/**
 * @author Marek Cuchý
 */
public class OsmRelation extends OsmElement {

	private final List<Long> nodes = new ArrayList<>();
	private final List<Long> ways = new ArrayList<>();
	private final List<Long> relations = new ArrayList<>();

	public OsmRelation(long id) {
		super(id);
	}

	public void addNode(long nodeId) {
		nodes.add(nodeId);
	}

	public void addWay(long wayId) {
		ways.add(wayId);
	}

	public void addRelation(long relationId) {
		relations.add(relationId);
	}
}
