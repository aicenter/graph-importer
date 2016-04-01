package cz.agents.gtdgraphimporter.structurebuilders.edge;

import cz.agents.multimodalstructures.additional.ModeOfTransport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Marek Cuchý
 */
public class RoadEdgeSimplificationBuilder extends RoadEdgeBuilder {

	private final List<RoadEdgeBuilder> toRemove = new ArrayList<>();

	public RoadEdgeSimplificationBuilder(int tmpFromId, int tmpToId, float allowedMaxSpeedInMpS, long wayID,
										 Set<ModeOfTransport> ModeOfTransports) {
		super(tmpFromId, tmpToId, allowedMaxSpeedInMpS, wayID, ModeOfTransports);
	}

	public RoadEdgeSimplificationBuilder(int tmpFromId, int tmpToId, int length, float allowedMaxSpeedInMpS, long
			wayID,
										 Set<ModeOfTransport> ModeOfTransports) {
		super(tmpFromId, tmpToId, length, allowedMaxSpeedInMpS, wayID, ModeOfTransports);
	}

	public RoadEdgeSimplificationBuilder(RoadEdgeBuilder edge) {
		super(edge.getTmpFromId(), edge.getTmpToId(), edge.getAllowedMaxSpeedInMpS(), edge.getWayID(), edge
				.getModeOfTransports());
		toRemove.add(edge);
	}

	public RoadEdgeSimplificationBuilder(RoadEdgeBuilder incoming, RoadEdgeBuilder outgoing) {
		this(incoming);
		addEdge(outgoing);
	}

	public final void addEdge(RoadEdgeBuilder edge) {
		if (!equalAttributes(edge)) {
			throw new IllegalArgumentException("The edge does not have equal attributes.");
		}
		if (getTmpFromId() == edge.getTmpToId()) {
			setTmpFromId(edge.getTmpFromId());
		} else if (getTmpToId() == edge.getTmpFromId()) {
			setTmpToId(edge.getTmpToId());
		} else {
			throw new IllegalStateException("Edge doesn't have any shared node in one direction.");
		}
		if (edge instanceof RoadEdgeSimplificationBuilder) {
			toRemove.addAll(((RoadEdgeSimplificationBuilder) edge).toRemove);
		} else {
			toRemove.add(edge);
		}
	}

	public List<RoadEdgeBuilder> getToRemove() {
		return Collections.unmodifiableList(toRemove);
	}
}
