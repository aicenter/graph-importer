package cz.agents.gtdgraphimporter.gtfs;

import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.Node;
import cz.agents.geotools.EdgeUtil;
import cz.agents.gtdgraphimporter.GraphBuilder;
import cz.agents.multimodalstructures.additional.ModeOfTransport;
import cz.agents.multimodalstructures.edges.*;
import cz.agents.multimodalstructures.nodes.RouteNode;
import cz.agents.multimodalstructures.nodes.StopNode;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.ReadablePeriod;

import java.util.*;
import java.util.Map.Entry;

/**
 * Builder of public transport graphs. It can handle only one timezone. If more timezones are in the data the graph
 * can't be built.
 *
 * @author Radek Holy
 * @author Marek Cuchý
 */
public final class GTFSGraphBuilder extends AbstractGTFSDataHandler {

	private static final Logger log = Logger.getLogger(GTFSGraphBuilder.class);

	/**
	 * Duration of getting on in seconds.
	 */
	private final short getOnDurationInS;

	/**
	 * Duration of getting off in seconds.
	 */
	private final short getOffDurationInS;

	/**
	 * Source ID of the newly created node.
	 */
	private long newNodeSourceID;

	/**
	 * ID of the newly created node.
	 */
	private int newNodeId = 0;

	/**
	 * Route edge builders for each route and each section between two stations.
	 */
	private final Map<EdgeKey, RouteEdgeBuilder> edgeBuilders = new HashMap<>();

	/**
	 * The time which is taken as a point zero for the departures stored in the route edges.
	 */
	private final DateTime epochStart;

	/**
	 * Construct a new instance.
	 *
	 * @param initialNodeId
	 * 		ID for a newly created {@link Node}.
	 * @param getOnDurationInS
	 * 		Duration of getting on in seconds.
	 * @param getOffDurationInS
	 * @param epochStart
	 */
	public GTFSGraphBuilder(final long initialNodeId, final short getOnDurationInS, final short getOffDurationInS,
							DateTime epochStart) {
		log.warn("The lengths of routes (without appropriate distance information) are just estimated.");
		log.warn(
				"All departures are handled as if they are defined by exact times (even those defined by non-exact " +
						"frequencies).");
		this.newNodeSourceID = initialNodeId;
		this.getOnDurationInS = getOnDurationInS;
		this.getOffDurationInS = getOffDurationInS;
		this.epochStart = epochStart;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void addDepartures(final String origin, final String destination, final String route,
									final String service, final String tripId, final String tripHeadsign,
									final ReadablePeriod startTime, final ReadablePeriod timePeriod,
									final ReadablePeriod endTime, final Boolean isExact, final Double distanceInM,
									final ReadablePeriod travelTime) {
		checkStop(origin);
		checkStop(destination);
		checkRoute(route);
		final Collection<Departure> departures = createDepartures(route, service, tripId, tripHeadsign, startTime,
				timePeriod, endTime, travelTime);
		if (!departures.isEmpty()) {
			RouteEdgeBuilder edgeBuilder = fetchRouteEdgeBuilder(origin, destination, route, distanceInM);
			edgeBuilder.addDepartures(departures);
		}
	}

	/**
	 * Get or create edge builder.
	 *
	 * @param origin
	 * 		ID of the origin station.
	 * @param destination
	 * 		ID of the destination station.
	 * @param route
	 * 		ID of the route
	 * @param distanceInM
	 * 		Length of the journey between the stations. It can be {@code null}.
	 *
	 * @return
	 */
	private RouteEdgeBuilder fetchRouteEdgeBuilder(String origin, String destination, String route,
												   Double distanceInM) {
		EdgeKey key = new EdgeKey(origin, destination, route);
		RouteEdgeBuilder edgeBuilder = this.edgeBuilders.get(key);
		if (edgeBuilder == null) {
			edgeBuilder = new RouteEdgeBuilder(origin, destination, route,
					getOrEstimateDistance(distanceInM, origin, destination));
			this.edgeBuilders.put(key, edgeBuilder);
		}
		assert distanceInM == null || Math.abs(
				distanceInM - edgeBuilder.distanceInM) <= 1 : "Restored edge has " + "different length than the " +
				"requested one.";
		return edgeBuilder;
	}

	/**
	 * Retrieves {@code int} distance from {@code distanceInM} or if it is {@code null} estimates the distance by
	 * euclidean distance between {@code origin} and {@code destination}.
	 *
	 * @param distanceInM
	 * @param origin
	 * @param destination
	 *
	 * @return
	 */
	private int getOrEstimateDistance(Double distanceInM, String origin, String destination) {
		if (distanceInM == null) {
			return (int) EdgeUtil.computeEuclideanDistance(stops.get(origin).location, stops.get(destination)
					.location);
		} else {
			return distanceInM.intValue();
		}
	}

	/**
	 * Construct the graph and clear the builder.
	 *
	 * @return The graph.
	 */
	public final Graph<Node, TimeDependentEdge> flushToGraph() {
		//check if there is only one time zone
		if (new HashSet<>(agenciesTimeZones.values()).size() > 1) {
			throw new IllegalStateException("The GTFS contains data from more time zones.");
		}

		// Create the graph.
		final GraphBuilder<Node, TimeDependentEdge> graphBuilder = new GraphBuilder<>();
		final Map<String, StopNode> stopNodes = new HashMap<>();
		final Map<RouteNodeKey, RouteNode> routeNodes = new HashMap<>();

		for (Entry<EdgeKey, RouteEdgeBuilder> e : edgeBuilders.entrySet()) {
			EdgeKey key = e.getKey();
			RouteEdgeBuilder routeEdgeBuilder = e.getValue();
			//add only edges with some departures
			if (!routeEdgeBuilder.isEmpty()) {
				StopNode fromStopNode = fetchStopNode(key.fromStop, stopNodes, graphBuilder);
				StopNode toStopNode = fetchStopNode(key.toStop, stopNodes, graphBuilder);

				RouteNode fromRouteNode = fetchRouteNode(fromStopNode, key.routeId, routeNodes, graphBuilder);
				RouteNode toRouteNode = fetchRouteNode(toStopNode, key.routeId, routeNodes, graphBuilder);

				routeEdgeBuilder.setMode(routes.get(key.routeId).ptMode);
				routeEdgeBuilder.setFromNodeId(fromRouteNode.id);
				routeEdgeBuilder.setToNodeId(toRouteNode.id);

				RouteEdge routeEdge = routeEdgeBuilder.buildEdge(epochStart);
				graphBuilder.addEdge(routeEdge);
			}
		}

		//		agenciesTimeZones.clear();
		//		agenciesToDetails.clear();
		//		servicesToDates.clear();
		//		servicesToDatesIncluded.clear();
		//		servicesToDatesExcluded.clear();
		//		stops.clear();
		//		routes.clear();

		return graphBuilder.createGraph();
	}

	/**
	 * Get or create route node. If the node does not exist, the newly created is added to {@code routeNodes} and
	 * {@code
	 * graphBuilder} and inner edges between the new node and {@code stopNode} are created and added to the builder.
	 *
	 * @param stopNode
	 * @param routeId
	 * @param routeNodes
	 * @param graphBuilder
	 *
	 * @return
	 */
	private RouteNode fetchRouteNode(StopNode stopNode, String routeId, Map<RouteNodeKey, RouteNode> routeNodes,
									 GraphBuilder<Node, TimeDependentEdge> graphBuilder) {
		RouteNodeKey key = new RouteNodeKey(stopNode.stopID, routeId);
		if (routeNodes.containsKey(key)) {
			return routeNodes.get(key);
		} else {
			RouteNode routeNode = new RouteNode(newNodeId++, newNodeSourceID++, stopNode.latE6, stopNode.lonE6,
					stopNode.latProjected, stopNode.lonProjected, stopNode.elevation, stopNode, routeId);
			routeNodes.put(key, routeNode);
			graphBuilder.addNode(routeNode);

			//create edges between stop node and route node.
			InnerEdge onEdge = new InnerEdge(stopNode.id, routeNode.id, getOnDurationInS);
			InnerEdge offEdge = new InnerEdge(routeNode.id, stopNode.id, getOffDurationInS);

			graphBuilder.addEdge(onEdge);
			graphBuilder.addEdge(offEdge);

			return routeNode;
		}
	}

	/**
	 * Get or create stop node. If the node does not exist, the newly created is added to {@code stopNodes} and {@code
	 * graphBuilder}.
	 *
	 * @param fromStop
	 * @param stopNodes
	 * @param graphBuilder
	 *
	 * @return
	 */
	private StopNode fetchStopNode(String fromStop, Map<String, StopNode> stopNodes,
								   GraphBuilder<Node, TimeDependentEdge> graphBuilder) {
		if (stopNodes.containsKey(fromStop)) {
			return stopNodes.get(fromStop);
		} else {
			Stop stop = stops.get(fromStop);
			GPSLocation loc = stop.location;
			StopNode stopNode = new StopNode(newNodeId++, newNodeSourceID++, loc.latE6, loc.lonE6, loc.latProjected,
					loc.lonProjected, loc.elevation, stop.id, stop.name, stop.zoneId, stop.wheelchairBoarding);
			stopNodes.put(stop.id, stopNode);
			graphBuilder.addNode(stopNode);
			return stopNode;
		}
	}

	private void checkStop(String stopId) {
		if (!stops.containsKey(stopId)) throw new IllegalStateException("Stop " + stopId + " must be added first.");
	}

	private void checkRoute(String routeId) {
		if (!routes.containsKey(routeId)) throw new IllegalStateException("Route " + routeId + " must be added first.");
	}

	private static class RouteEdgeBuilder {

		public final String fromStop;
		public final String toStop;
		public final String routeId;
		public final int distanceInM;

		/**
		 * From route node id
		 */
		private int fromNodeId;

		/**
		 * To route node id
		 */
		private int toNodeId;
		private ModeOfTransport mode;

		private List<Departure> departures = new ArrayList<>();

		public RouteEdgeBuilder(String fromStop, String toStop, String routeId, int distanceInM) {
			this.fromStop = fromStop;
			this.toStop = toStop;
			this.routeId = routeId;
			this.distanceInM = distanceInM;
		}

		/**
		 * Builds the route edge. From all departures the {@code epochStart} is
		 *
		 * @param epochStart
		 *
		 * @return
		 */
		public RouteEdge buildEdge(DateTime epochStart) {
			Collections.sort(departures, (d1, d2) -> d1.departureTime.compareTo(d2.departureTime));

			int[] departuresArray = new int[departures.size()];
			int[] durations = new int[departures.size()];

			for (int i = 0; i < departures.size(); i++) {
				Departure departure = departures.get(i);
				departuresArray[i] = (int) (departure.departureTime.minus(epochStart.getMillis()).getMillis() / 1000L);
				durations[i] = departure.travelTime.toPeriod().toStandardSeconds().getSeconds();
			}

			if (isAllSame(durations)) {
				return new RouteConstantEdge(fromNodeId, toNodeId, mode, departuresArray, durations[0], distanceInM);
			} else {
				return new RouteVaryingEdge(fromNodeId, toNodeId, mode, departuresArray, durations, distanceInM);
			}
		}

		private boolean isAllSame(int[] array) {
			for (int i = 0; i < array.length; i++) {
				if (array[0] != array[i]) return false;
			}
			return true;
		}

		/**
		 * Returns true iff there is no departure on this edge.
		 *
		 * @return
		 */
		public boolean isEmpty() {
			return departures.isEmpty();
		}

		public RouteEdgeBuilder setFromNodeId(int fromNodeId) {
			this.fromNodeId = fromNodeId;
			return this;
		}

		public RouteEdgeBuilder setToNodeId(int toNodeId) {
			this.toNodeId = toNodeId;
			return this;
		}

		public RouteEdgeBuilder setMode(ModeOfTransport mode) {
			this.mode = mode;
			return this;
		}

		public RouteEdgeBuilder addDepartures(Collection<Departure> departures) {
			this.departures.addAll(departures);
			return this;
		}

		@Override
		public String toString() {
			return "RouteEdgeBuilder [" +
					"fromStop='" + fromStop + '\'' +
					", toStop='" + toStop + '\'' +
					", routeId='" + routeId + '\'' +
					", distanceInM=" + distanceInM +
					", fromNode=" + fromNodeId +
					", toNode=" + toNodeId +
					", mode=" + mode +
					", departures#=" + departures.size() +
					']';
		}
	}

	private static class EdgeKey {

		public final String fromStop;
		public final String toStop;
		public final String routeId;

		public EdgeKey(String fromStop, String toStop, String routeId) {
			this.fromStop = fromStop;
			this.toStop = toStop;
			this.routeId = routeId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			EdgeKey edgeKey = (EdgeKey) o;

			if (fromStop != null ? !fromStop.equals(edgeKey.fromStop) : edgeKey.fromStop != null) return false;
			if (toStop != null ? !toStop.equals(edgeKey.toStop) : edgeKey.toStop != null) return false;
			return routeId != null ? routeId.equals(edgeKey.routeId) : edgeKey.routeId == null;

		}

		@Override
		public int hashCode() {
			int result = fromStop != null ? fromStop.hashCode() : 0;
			result = 31 * result + (toStop != null ? toStop.hashCode() : 0);
			result = 31 * result + (routeId != null ? routeId.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "EdgeKey [" +
					"fromStop='" + fromStop + '\'' +
					", toStop='" + toStop + '\'' +
					", routeId='" + routeId + '\'' +
					']';
		}
	}

	private class RouteNodeKey {

		public final String stopId;
		public final String routeId;

		public RouteNodeKey(String stopId, String routeId) {
			this.stopId = stopId;
			this.routeId = routeId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			RouteNodeKey that = (RouteNodeKey) o;

			if (stopId != null ? !stopId.equals(that.stopId) : that.stopId != null) return false;
			return routeId != null ? routeId.equals(that.routeId) : that.routeId == null;

		}

		@Override
		public int hashCode() {
			int result = stopId != null ? stopId.hashCode() : 0;
			result = 31 * result + (routeId != null ? routeId.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "RouteNodeKey [" +
					"stopId='" + stopId + '\'' +
					", routeId='" + routeId + '\'' +
					']';
		}
	}
}
