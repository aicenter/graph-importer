package cz.cvut.fel.aic.graphimporter.structurebuilders;

import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.Node;


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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		NodeBuilder<?> that = (NodeBuilder<?>) o;

		if (tmpId != that.tmpId) return false;
		if (sourceId != that.sourceId) return false;
		return location != null ? location.equals(that.location) : that.location == null;

	}

	@Override
	public int hashCode() {
		int result = tmpId;
		result = 31 * result + (int) (sourceId ^ (sourceId >>> 32));
		result = 31 * result + (location != null ? location.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "NodeBuilder [" +
				"tmpId=" + tmpId +
				", sourceId=" + sourceId +
				", location=" + location +
				']';
	}
}
