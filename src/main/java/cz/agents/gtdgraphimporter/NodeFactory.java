package cz.agents.gtdgraphimporter;

import cz.agents.basestructures.GPSLocation;
import cz.agents.geotools.EPSGProjection;
import cz.agents.multimodalstructures.nodes.RoadNode;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

/**
 * @author Marek Cuchý
 */
public class NodeFactory {

	private final EPSGProjection projection;

	private int nextNodeId = 0;

	public NodeFactory(int srid) throws FactoryException, TransformException {
		this.projection = new EPSGProjection(srid);
	}

	public RoadNode createRoadNode(double lat, double lon, double elevation, long osmId, boolean isParkAndRide,
								   boolean isBikeSharing) {
		GPSLocation gps = getProjectedGPS(lat, lon, elevation);
		return new RoadNode(getNodeId(), osmId, gps.latE6, gps.lonE6, gps.latProjected, gps.lonProjected, gps
				.elevation, isParkAndRide, isBikeSharing);
	}

	private int getNodeId() {
		return nextNodeId++;
	}

	private GPSLocation getProjectedGPS(double lat, double lon, double elevation) {
		int latE6 = (int) (lat * 1E6);
		int lonE6 = (int) (lon * 1E6);
		return projection.getProjectedGPSLocation(latE6, lonE6, (int) Math.round(elevation));
	}
}
