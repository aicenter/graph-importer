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
