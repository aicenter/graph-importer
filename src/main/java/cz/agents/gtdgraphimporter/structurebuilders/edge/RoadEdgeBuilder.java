package cz.agents.gtdgraphimporter.structurebuilders.edge;

import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.RoadEdge;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author Marek Cuchý
 */
public class RoadEdgeBuilder extends EdgeBuilder<RoadEdge> {

	private float allowedMaxSpeedInMpS;
	private long wayID;
	private Set<ModeOfTransport> modeOfTransports = EnumSet.noneOf(ModeOfTransport.class);

	public RoadEdgeBuilder() {
	}

	public RoadEdgeBuilder(int tmpFromId, int tmpToId) {
		super(tmpFromId, tmpToId);
	}

	public RoadEdgeBuilder(int tmpFromId, int tmpToId, int length) {
		super(tmpFromId, tmpToId, length);
	}

	public RoadEdgeBuilder(int tmpFromId, int tmpToId, int length, float allowedMaxSpeedInMpS, long wayID,
						   Set<ModeOfTransport> modeOfTransports) {
		super(tmpFromId, tmpToId, length);
		this.allowedMaxSpeedInMpS = allowedMaxSpeedInMpS;
		this.wayID = wayID;
		this.modeOfTransports = EnumSet.copyOf(modeOfTransports);
	}

	public float getAllowedMaxSpeedInMpS() {
		return allowedMaxSpeedInMpS;
	}

	public RoadEdgeBuilder setAllowedMaxSpeedInMpS(float allowedMaxSpeedInMpS) {
		this.allowedMaxSpeedInMpS = allowedMaxSpeedInMpS;
		return this;
	}

	public long getWayID() {
		return wayID;
	}

	public RoadEdgeBuilder setWayID(long wayID) {
		this.wayID = wayID;
		return this;
	}

	public Set<ModeOfTransport> getModeOfTransports() {
		return modeOfTransports;
	}

	public RoadEdgeBuilder addModeOfTransports(Set<ModeOfTransport> ModeOfTransports) {
		this.modeOfTransports.addAll(ModeOfTransports);
		return this;
	}

	public RoadEdgeBuilder intersectModeOfTransports(Set<ModeOfTransport> ModeOfTransports) {
		this.modeOfTransports.retainAll(ModeOfTransports);
		return this;
	}

	public RoadEdgeBuilder setModeOfTransports(Set<ModeOfTransport> ModeOfTransports) {
		this.modeOfTransports = EnumSet.copyOf(ModeOfTransports);
		return this;
	}

	public boolean equalAttributes(RoadEdgeBuilder that) {
		return Float.compare(that.allowedMaxSpeedInMpS, allowedMaxSpeedInMpS) == 0 && wayID == that.wayID &&
			   (modeOfTransports != null ? modeOfTransports.equals(that.modeOfTransports) :
					   that.modeOfTransports == null);
	}

	@Override
	public RoadEdge build(int fromId, int toId) {
		return new RoadEdge(fromId, toId, wayID, modeOfTransports, allowedMaxSpeedInMpS, getLength());
	}

	@Override
	public boolean checkFeasibility(ModeOfTransport mode) {
		return modeOfTransports.contains(mode);
	}

	@Override
	public RoadEdgeBuilder copy(int tmpFromId, int tmpToId, int length) {
		return new RoadEdgeBuilder(tmpFromId, tmpToId, length, allowedMaxSpeedInMpS, wayID, modeOfTransports);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		RoadEdgeBuilder that = (RoadEdgeBuilder) o;

		return Float.compare(that.allowedMaxSpeedInMpS, allowedMaxSpeedInMpS) == 0 && wayID == that.wayID &&
			   (modeOfTransports != null ? modeOfTransports.equals(that.modeOfTransports) :
					   that.modeOfTransports == null);

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (allowedMaxSpeedInMpS != +0.0f ? Float.floatToIntBits(allowedMaxSpeedInMpS) : 0);
		result = 31 * result + (int) (wayID ^ (wayID >>> 32));
		result = 31 * result + (modeOfTransports != null ? modeOfTransports.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "RoadEdgeBuilder [" +
			   "[" + super.toString() + "], " +
			   "allowedMaxSpeedInMpS=" + allowedMaxSpeedInMpS +
			   ", wayID=" + wayID +
			   ", modeOfTransports=" + modeOfTransports +
			   ']';
	}
}
