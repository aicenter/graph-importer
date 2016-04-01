package cz.agents.gtdgraphimporter.structurebuilders.edge;

import cz.agents.basestructures.Edge;
import cz.agents.basestructures.EdgeId;
import cz.agents.multimodalstructures.additional.ModeOfTransport;

/**
 * @author Marek Cuchý
 */
public abstract class EdgeBuilder<TEdge extends Edge> {

	private int tmpFromId;
	private int tmpToId;

	private int length;

	private EdgeId edgeId;

	public EdgeBuilder() {
	}

	public EdgeBuilder(int length) {
		this.length = length;
	}

	public EdgeBuilder(int tmpFromId, int tmpToId) {
		this.tmpFromId = tmpFromId;
		this.tmpToId = tmpToId;
	}

	public EdgeBuilder(int tmpFromId, int tmpToId, int length) {
		this.tmpFromId = tmpFromId;
		this.tmpToId = tmpToId;
		this.length = length;
	}

	public abstract TEdge build(int fromId, int toId);

	public abstract boolean checkFeasibility(ModeOfTransport mode);

	public boolean isCircle(EdgeBuilder<?> edge) {
		return tmpFromId == edge.tmpToId && tmpToId == edge.tmpFromId;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getTmpFromId() {
		return tmpFromId;
	}

	public void setTmpFromId(int tmpFromId) {
		this.edgeId = null;
		this.tmpFromId = tmpFromId;
	}

	public int getTmpToId() {
		return tmpToId;
	}

	public void setTmpToId(int tmpToId) {
		this.edgeId = null;
		this.tmpToId = tmpToId;
	}

	public EdgeId getEdgeId() {
		if (edgeId == null) {
			edgeId = new EdgeId(tmpFromId, tmpToId);
		}
		return edgeId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		EdgeBuilder<?> that = (EdgeBuilder<?>) o;

		if (tmpFromId != that.tmpFromId) return false;
		if (tmpToId != that.tmpToId) return false;
		return length == that.length;

	}

	@Override
	public int hashCode() {
		int result = tmpFromId;
		result = 31 * result + tmpToId;
		result = 31 * result + length;
		return result;
	}

	@Override
	public String toString() {
		return "EdgeBuilder [" +
				"tmpFromId=" + tmpFromId +
				", tmpToId=" + tmpToId +
				", length=" + length +
				']';
	}

}
