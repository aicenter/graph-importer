package cz.agents.gtdgraphimporter.structurebuilders;

import cz.agents.basestructures.Edge;
import cz.agents.multimodalstructures.additional.ModeOfTransport;

/**
 * @author Marek Cuchý
 */
public abstract class EdgeBuilder<TEdge extends Edge> {

	private int tmpFromId;
	private int tmpToId;

	private int length;

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
		this.tmpFromId = tmpFromId;
	}

	public int getTmpToId() {
		return tmpToId;
	}

	public void setTmpToId(int tmpToId) {
		this.tmpToId = tmpToId;
	}

	public abstract boolean checkFeasibility(ModeOfTransport mode);

	@Override
	public String toString() {
		return "EdgeBuilder [" +
				"tmpFromId=" + tmpFromId +
				", tmpToId=" + tmpToId +
				", length=" + length +
				']';
	}
}
