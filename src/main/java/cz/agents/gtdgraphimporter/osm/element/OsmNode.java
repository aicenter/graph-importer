package cz.agents.gtdgraphimporter.osm.element;

/**
 * @author Marek Cuchý
 */
public class OsmNode extends OsmElement {

	public final double lat;
	public final double lon;

	public OsmNode(long id, double lat, double lon) {
		super(id);
		this.lat = lat;
		this.lon = lon;
	}

	@Override
	public String toString() {
		return "OsmNode [" +
				"[" + super.toString() + "], " +
				"lat=" + lat +
				", lon=" + lon +
				']';
	}
}
