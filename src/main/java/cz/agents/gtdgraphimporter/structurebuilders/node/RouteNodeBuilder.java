package cz.agents.gtdgraphimporter.structurebuilders.node;

import cz.agents.basestructures.GPSLocation;
import cz.agents.multimodalstructures.nodes.RouteNode;
import cz.agents.multimodalstructures.nodes.StopNode;

/**
 * @author Marek Cuchý
 */
public class RouteNodeBuilder extends NodeBuilder<RouteNode> {

	private String routeId;
	private int stopNodeTmpId;
	private StopNode stopNode;

	public RouteNodeBuilder(int tmpId, long sourceId, GPSLocation location) {
		super(tmpId, sourceId, location);
	}

	public RouteNodeBuilder(int tmpId, long sourceId, GPSLocation location, String routeId, int stopNodeTmpId) {
		super(tmpId, sourceId, location);
		this.routeId = routeId;
		this.stopNodeTmpId = stopNodeTmpId;
	}

	public String getRouteId() {
		return routeId;
	}

	public RouteNodeBuilder setRouteId(String routeId) {
		this.routeId = routeId;
		return this;
	}

	public int getStopNodeTmpId() {
		return stopNodeTmpId;
	}

	public RouteNodeBuilder setStopNodeTmpId(int stopNodeTmpId) {
		this.stopNodeTmpId = stopNodeTmpId;
		return this;
	}

		public StopNode getStopNode() {
			return stopNode;
		}

	public RouteNodeBuilder setStopNode(StopNode stopNode) {
		this.stopNode = stopNode;
		return this;
	}

	@Override
	public RouteNode buildNode(int id) {
		return new RouteNode(id, sourceId, location.latE6, location.lonE6, location.latProjected, location
				.lonProjected, location.elevation, stopNode, routeId);
	}

}
