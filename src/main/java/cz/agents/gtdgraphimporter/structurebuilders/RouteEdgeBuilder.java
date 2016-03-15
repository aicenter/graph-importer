package cz.agents.gtdgraphimporter.structurebuilders;

import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.RouteConstantEdge;
import cz.agents.multimodalstructures.edges.RouteEdge;
import cz.agents.multimodalstructures.edges.RouteVaryingEdge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Marek Cuchý
 */
public class RouteEdgeBuilder extends EdgeBuilder<RouteEdge> {

	private final List<Departure> departures = new ArrayList<>();

	private ModeOfTransport mode;

	public RouteEdgeBuilder() {
	}

	public RouteEdgeBuilder(int length) {
		super(length);
	}

	public RouteEdgeBuilder(int tmpFromId, int tmpToId) {
		super(tmpFromId, tmpToId);
	}

	public RouteEdgeBuilder(int tmpFromId, int tmpToId, ModeOfTransport mode) {
		super(tmpFromId, tmpToId);
		this.mode = mode;
	}

	public ModeOfTransport getMode() {
		return mode;
	}

	public RouteEdgeBuilder setMode(ModeOfTransport mode) {
		this.mode = mode;
		return this;
	}

	public void addDeparture(int departure, int duration){
		departures.add(new Departure(departure,duration));
	}

	@Override
	public RouteEdge build(int fromId, int toId) {
		Collections.sort(departures);

		int deps[] = new int[departures.size()];
		int durations[] = new int[departures.size()];

		for (int i = 0; i < departures.size(); i++) {
			Departure d = departures.get(i);
			deps[i] = d.departure;
			durations[i] = d.travelTime;
		}
		if (isAllSame(durations)) {
			return new RouteConstantEdge(fromId, toId, mode, deps, durations[0], getLength());
		} else {
			return new RouteVaryingEdge(fromId, toId, mode, deps, durations, getLength());
		}
	}

	@Override
	public boolean checkFeasibility(ModeOfTransport mode) {
		return this.mode == mode;
	}

	public boolean isEmpty() {
		return departures.isEmpty();
	}

	private boolean isAllSame(int[] array) {
		for (int i = 1; i < array.length; i++) {
			if (array[0] != array[i]) return false;
		}
		return true;
	}

	private static class Departure implements Comparable<Departure> {

		private final int departure;
		private final int travelTime;

		public Departure(int departure, int travelTime) {
			this.departure = departure;
			this.travelTime = travelTime;
		}

		@Override
		public int compareTo(Departure o) {
			return departure - o.departure;
		}
	}
}
