package cz.agents.gtdgraphimporter.structurebuilders.node;

import cz.agents.basestructures.GPSLocation;
import cz.agents.multimodalstructures.nodes.RoadNode;

/**
 * @author Marek Cuchý
 */
public class RoadNodeBuilder extends NodeBuilder<RoadNode> {

	private boolean isParkAndRide;
	private boolean isBikeSharingStation;

	public RoadNodeBuilder(int tmpId, long sourceId, GPSLocation location) {
		super(tmpId, sourceId, location);
	}



	public boolean isParkAndRide() {
		return isParkAndRide;
	}

	public RoadNodeBuilder setParkAndRide(boolean parkAndRide) {
		isParkAndRide = parkAndRide;
		return this;
	}

	public boolean isBikeSharingStation() {
		return isBikeSharingStation;
	}

	public RoadNodeBuilder setBikeSharingStation(boolean bikeSharingStation) {
		isBikeSharingStation = bikeSharingStation;
		return this;
	}

	@Override
	public RoadNode buildNode(int id) {
		return new RoadNode(id, sourceId, location.latE6, location.lonE6, location.latProjected, location
				.lonProjected, location.elevation, isParkAndRide, isBikeSharingStation);
	}

	@Override
	public String toString() {
		return "RoadNodeBuilder [" +
				"[" + super.toString() + "], " +
				"isParkAndRide=" + isParkAndRide +
				", isBikeSharingStation=" + isBikeSharingStation +
				']';
	}
}
