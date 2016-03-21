package cz.agents.gtdgraphimporter.osm;

import cz.agents.gtdgraphimporter.osm.element.OsmNode;
import cz.agents.gtdgraphimporter.osm.element.OsmRelation;
import cz.agents.gtdgraphimporter.osm.element.OsmWay;

/**
 * @author Marek Cuchý
 */
public interface OsmElementConsumer {

	public void accept(OsmNode node);
	public void accept(OsmWay way);
	public void accept(OsmRelation relation);
}
