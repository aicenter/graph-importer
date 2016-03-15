package cz.agents.gtdgraphimporter.structurebuilders;

import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.InnerEdge;

/**
 * @author Marek Cuchý
 */
public class InnerEdgeBuilder extends EdgeBuilder<InnerEdge> {

	public final short duration;

	public InnerEdgeBuilder(int tmpFromId, int tmpToId, short duration) {
		super(tmpFromId, tmpToId);
		this.duration = duration;
	}

	@Override
	public InnerEdge build(int fromId, int toId) {
		return new InnerEdge(fromId, toId, duration);
	}

	@Override
	public boolean checkFeasibility(ModeOfTransport mode) {
		return mode == ModeOfTransport.WALK;
	}
}
