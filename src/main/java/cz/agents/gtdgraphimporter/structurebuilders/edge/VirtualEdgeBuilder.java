package cz.agents.gtdgraphimporter.structurebuilders.edge;

import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.VirtualEdge;

import java.util.Collections;

/**
 * Builder for building of {@link VirtualEdge} instances. It does NOT support the path attribute. Instead, it insert
 * only empty list.
 *
 * @author Marek Cuchý
 */
public class VirtualEdgeBuilder extends EdgeBuilder<VirtualEdge> {

	/**
	 * speed used for precomputing the time, it is used for weighting the real time, when user insert his speed
	 */
	private float speedUsedForPrecomputingInMpS;
	/**
	 * precomputed time to travel the edge
	 */
	private int precomputedTime;
	/**
	 * mode of transport for which the edge is introduced
	 */
	private ModeOfTransport mode;

	public VirtualEdgeBuilder(int tmpFromId, int tmpToId, int length, float speedUsedForPrecomputingInMpS,
							  int precomputedTime, ModeOfTransport mode) {
		super(tmpFromId, tmpToId, length);
		this.speedUsedForPrecomputingInMpS = speedUsedForPrecomputingInMpS;
		this.precomputedTime = precomputedTime;
		this.mode = mode;
	}

	public VirtualEdgeBuilder setSpeedUsedForPrecomputingInMpS(float speedUsedForPrecomputingInMpS) {
		this.speedUsedForPrecomputingInMpS = speedUsedForPrecomputingInMpS;
		return this;
	}

	public VirtualEdgeBuilder setPrecomputedTime(int precomputedTime) {
		this.precomputedTime = precomputedTime;
		return this;
	}

	public VirtualEdgeBuilder setMode(ModeOfTransport mode) {
		this.mode = mode;
		return this;
	}

	@Override
	public VirtualEdge build(int fromId, int toId) {
		return new VirtualEdge(fromId, toId, speedUsedForPrecomputingInMpS, precomputedTime, mode,
				Collections.emptyList(), getLength());
	}

	@Override
	public boolean checkFeasibility(ModeOfTransport mode) {
		return mode == this.mode;
	}

	@Override
	public VirtualEdgeBuilder copy(int tmpFromId, int tmpToId, int length) {
		return new VirtualEdgeBuilder(tmpFromId, tmpToId, length, speedUsedForPrecomputingInMpS, precomputedTime,
				mode);
	}
}
