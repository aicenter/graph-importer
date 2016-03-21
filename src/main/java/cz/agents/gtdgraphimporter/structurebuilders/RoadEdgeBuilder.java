package cz.agents.gtdgraphimporter.structurebuilders;

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
	private Set<ModeOfTransport> permittedModes = EnumSet.noneOf(ModeOfTransport.class);

	public RoadEdgeBuilder() {
	}

	public RoadEdgeBuilder(int tmpFromId, int tmpToId) {
		super(tmpFromId, tmpToId);
	}

	public RoadEdgeBuilder(int tmpFromId, int tmpToId, int length) {
		super(tmpFromId, tmpToId, length);
	}

	public RoadEdgeBuilder(int tmpFromId, int tmpToId, float allowedMaxSpeedInMpS, long wayID,
						   Set<ModeOfTransport> permittedModes) {
		super(tmpFromId, tmpToId);
		this.allowedMaxSpeedInMpS = allowedMaxSpeedInMpS;
		this.wayID = wayID;
		this.permittedModes = permittedModes;
	}

	public RoadEdgeBuilder(int tmpFromId, int tmpToId, int length, float allowedMaxSpeedInMpS, long wayID,
						   Set<ModeOfTransport> permittedModes) {
		super(tmpFromId, tmpToId, length);
		this.allowedMaxSpeedInMpS = allowedMaxSpeedInMpS;
		this.wayID = wayID;
		this.permittedModes = EnumSet.copyOf(permittedModes);
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

	public Set<ModeOfTransport> getPermittedModes() {
		return permittedModes;
	}

	public RoadEdgeBuilder addPermittedModes(Set<ModeOfTransport> permittedModes) {
		this.permittedModes.addAll(permittedModes);
		return this;
	}

	public RoadEdgeBuilder intersectPermittedModes(Set<ModeOfTransport> permittedModes) {
		this.permittedModes.retainAll(permittedModes);
		return this;
	}

	public RoadEdgeBuilder setPermittedModes(Set<ModeOfTransport> permittedModes) {
		this.permittedModes = EnumSet.copyOf(permittedModes);
		return this;
	}

	public boolean equalAttributes(RoadEdgeBuilder that) {
		if (Float.compare(that.allowedMaxSpeedInMpS, allowedMaxSpeedInMpS) != 0) return false;
		if (wayID != that.wayID) return false;
		return permittedModes != null ? permittedModes.equals(that.permittedModes) : that.permittedModes == null;
	}

	@Override
	public RoadEdge build(int fromId, int toId) {
		return new RoadEdge(fromId, toId, wayID, permittedModes, allowedMaxSpeedInMpS, getLength());
	}

	@Override
	public boolean checkFeasibility(ModeOfTransport mode) {
		return permittedModes.contains(mode);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		RoadEdgeBuilder that = (RoadEdgeBuilder) o;

		if (Float.compare(that.allowedMaxSpeedInMpS, allowedMaxSpeedInMpS) != 0) return false;
		if (wayID != that.wayID) return false;
		return permittedModes != null ? permittedModes.equals(that.permittedModes) : that.permittedModes == null;

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (allowedMaxSpeedInMpS != +0.0f ? Float.floatToIntBits(allowedMaxSpeedInMpS) : 0);
		result = 31 * result + (int) (wayID ^ (wayID >>> 32));
		result = 31 * result + (permittedModes != null ? permittedModes.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "RoadEdgeBuilder [" +
				"[" + super.toString() + "], " +
				"allowedMaxSpeedInMpS=" + allowedMaxSpeedInMpS +
				", wayID=" + wayID +
				", permittedModes=" + permittedModes +
				']';
	}
}
