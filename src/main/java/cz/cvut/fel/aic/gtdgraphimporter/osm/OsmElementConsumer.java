package cz.cvut.fel.aic.gtdgraphimporter.osm;

import cz.cvut.fel.aic.gtdgraphimporter.osm.element.OsmNode;
import cz.cvut.fel.aic.gtdgraphimporter.osm.element.OsmRelation;
import cz.cvut.fel.aic.gtdgraphimporter.osm.element.OsmWay;

/**
 * @author Marek Cuchý
 */
public interface OsmElementConsumer {

	public void accept(OsmNode node);
	public void accept(OsmWay way);
	public void accept(OsmRelation relation);
}
