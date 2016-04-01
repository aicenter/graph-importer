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
	private Set<ModeOfTransport> ModeOfTransports = EnumSet.noneOf(ModeOfTransport.class);

	public RoadEdgeBuilder() {
	}

	public RoadEdgeBuilder(int tmpFromId, int tmpToId) {
		super(tmpFromId, tmpToId);
	}

	public RoadEdgeBuilder(int tmpFromId, int tmpToId, int length) {
		super(tmpFromId, tmpToId, length);
	}

	public RoadEdgeBuilder(int tmpFromId, int tmpToId, float allowedMaxSpeedInMpS, long wayID,
						   Set<ModeOfTransport> ModeOfTransports) {
		super(tmpFromId, tmpToId);
		this.allowedMaxSpeedInMpS = allowedMaxSpeedInMpS;
		this.wayID = wayID;
		this.ModeOfTransports = ModeOfTransports;
	}

	public RoadEdgeBuilder(int tmpFromId, int tmpToId, int length, float allowedMaxSpeedInMpS, long wayID,
						   Set<ModeOfTransport> ModeOfTransports) {
		super(tmpFromId, tmpToId, length);
		this.allowedMaxSpeedInMpS = allowedMaxSpeedInMpS;
		this.wayID = wayID;
		this.ModeOfTransports = EnumSet.copyOf(ModeOfTransports);
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
		return ModeOfTransports;
	}

	public RoadEdgeBuilder addModeOfTransports(Set<ModeOfTransport> ModeOfTransports) {
		this.ModeOfTransports.addAll(ModeOfTransports);
		return this;
	}

	public RoadEdgeBuilder intersectModeOfTransports(Set<ModeOfTransport> ModeOfTransports) {
		this.ModeOfTransports.retainAll(ModeOfTransports);
		return this;
	}

	public RoadEdgeBuilder setModeOfTransports(Set<ModeOfTransport> ModeOfTransports) {
		this.ModeOfTransports = EnumSet.copyOf(ModeOfTransports);
		return this;
	}

	public boolean equalAttributes(RoadEdgeBuilder that) {
		if (Float.compare(that.allowedMaxSpeedInMpS, allowedMaxSpeedInMpS) != 0) return false;
		if (wayID != that.wayID) return false;
		return ModeOfTransports != null ? ModeOfTransports.equals(that.ModeOfTransports) : that.ModeOfTransports == null;
	}

	@Override
	public RoadEdge build(int fromId, int toId) {
		return new RoadEdge(fromId, toId, wayID, ModeOfTransports, allowedMaxSpeedInMpS, getLength());
	}

	@Override
	public boolean checkFeasibility(ModeOfTransport mode) {
		return ModeOfTransports.contains(mode);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		RoadEdgeBuilder that = (RoadEdgeBuilder) o;

		if (Float.compare(that.allowedMaxSpeedInMpS, allowedMaxSpeedInMpS) != 0) return false;
		if (wayID != that.wayID) return false;
		return ModeOfTransports != null ? ModeOfTransports.equals(that.ModeOfTransports) : that.ModeOfTransports == null;

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (allowedMaxSpeedInMpS != +0.0f ? Float.floatToIntBits(allowedMaxSpeedInMpS) : 0);
		result = 31 * result + (int) (wayID ^ (wayID >>> 32));
		result = 31 * result + (ModeOfTransports != null ? ModeOfTransports.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "RoadEdgeBuilder [" +
				"[" + super.toString() + "], " +
				"allowedMaxSpeedInMpS=" + allowedMaxSpeedInMpS +
				", wayID=" + wayID +
				", ModeOfTransports=" + ModeOfTransports +
				']';
	}
}
