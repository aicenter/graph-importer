package cz.agents.gtdgraphimporter.structurebuilders;

import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Node;

/**
 * @author Marek Cuchý
 */
public abstract class NodeBuilder<TNode extends Node> {

	public final int tmpId;
	public final long sourceId;
	public final GPSLocation location;

	public NodeBuilder(int tmpId, long sourceId, GPSLocation location) {
		this.tmpId = tmpId;
		this.sourceId = sourceId;
		this.location = location;
	}

	public abstract TNode buildNode(int id);

	@Override
	public String toString() {
		return "NodeBuilder [" +
				"tmpId=" + tmpId +
				", sourceId=" + sourceId +
				", location=" + location +
				']';
	}
}
