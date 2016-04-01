package cz.agents.gtdgraphimporter.structurebuilders.node;

import cz.agents.basestructures.GPSLocation;
import cz.agents.multimodalstructures.additional.WheelchairBoarding;
import cz.agents.multimodalstructures.nodes.StopNode;

/**
 * @author Marek Cuchý
 */
public class StopNodeBuilder extends NodeBuilder<StopNode> {

	private String stopId;
	private String stopName;
	private String zoneId;
	private WheelchairBoarding wheelchairBoarding;

	public StopNodeBuilder(int tmpId, long sourceId, GPSLocation location) {
		super(tmpId, sourceId, location);
	}

	public StopNodeBuilder(int tmpId, long sourceId, GPSLocation location, String stopId, String stopName,
						   String zoneId, WheelchairBoarding wheelchairBoarding) {
		super(tmpId, sourceId, location);
		this.stopId = stopId;
		this.stopName = stopName;
		this.zoneId = zoneId;
		this.wheelchairBoarding = wheelchairBoarding;
	}

	public String getStopId() {
		return stopId;
	}

	public StopNodeBuilder setStopId(String stopId) {
		this.stopId = stopId;
		return this;
	}

	public String getStopName() {
		return stopName;
	}

	public StopNodeBuilder setStopName(String stopName) {
		this.stopName = stopName;
		return this;
	}

	public String getZoneId() {
		return zoneId;
	}

	public StopNodeBuilder setZoneId(String zoneId) {
		this.zoneId = zoneId;
		return this;
	}

	public WheelchairBoarding getWheelchairBoarding() {
		return wheelchairBoarding;
	}

	public StopNodeBuilder setWheelchairBoarding(WheelchairBoarding wheelchairBoarding) {
		this.wheelchairBoarding = wheelchairBoarding;
		return this;
	}

	@Override
	public StopNode buildNode(int id) {
		return new StopNode(id, sourceId, location.latE6, location.lonE6, location.latProjected, location
				.lonProjected, location.elevation, stopId, stopName, zoneId, wheelchairBoarding);
	}
}
