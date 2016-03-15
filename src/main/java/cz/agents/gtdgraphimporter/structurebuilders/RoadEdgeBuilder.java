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

	public RoadEdgeBuilder(int tmpFromId, int tmpToId, int length) {
		super(tmpFromId, tmpToId, length);
	}

	public RoadEdgeBuilder(int tmpFromId, int tmpToId, int length, float allowedMaxSpeedInMpS, long wayID,
						   Set<ModeOfTransport> permittedModes) {
		super(tmpFromId, tmpToId, length);
		this.allowedMaxSpeedInMpS = allowedMaxSpeedInMpS;
		this.wayID = wayID;
		this.permittedModes = permittedModes;
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

	public RoadEdgeBuilder intersectPemittedModes(Set<ModeOfTransport> permittedModes) {
		this.permittedModes.retainAll(permittedModes);
		return this;
	}

	public RoadEdgeBuilder setPermittedModes(Set<ModeOfTransport> permittedModes) {
		this.permittedModes = permittedModes;
		return this;
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
	public String toString() {
		return "RoadEdgeBuilder [" +
				"[" + super.toString() + "], " +
				"allowedMaxSpeedInMpS=" + allowedMaxSpeedInMpS +
				", wayID=" + wayID +
				", permittedModes=" + permittedModes +
				']';
	}
}
