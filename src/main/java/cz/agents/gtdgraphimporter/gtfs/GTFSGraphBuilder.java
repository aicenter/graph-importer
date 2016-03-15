package cz.agents.gtdgraphimporter.gtfs;

import cz.agents.basestructures.GPSLocation;
import cz.agents.basestructures.Graph;
import cz.agents.basestructures.Node;
import cz.agents.geotools.EdgeUtil;
import cz.agents.gtdgraphimporter.structurebuilders.*;
import cz.agents.multimodalstructures.edges.*;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
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

	private static final Logger LOGGER = Logger.getLogger(GTFSGraphBuilder.class);

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
	private final LocalDate epochStart;

	private DateTime epochStartWithTimeZone = null;

	/**
	 * Construct a new instance.
	 *
	 * @param initialSourceNodeId
	 * 		ID for a newly created {@link Node}.
	 * @param getOnDurationInS
	 * 		Duration of getting on in seconds.
	 * @param getOffDurationInS
	 * @param epochStart
	 */
	public GTFSGraphBuilder(final long initialSourceNodeId, final short getOnDurationInS, final short
			getOffDurationInS,
							LocalDate epochStart) {
		LOGGER.warn("The lengths of routes (without appropriate distance information) are just estimated.");
		LOGGER.warn("All departures are handled as if they are defined by exact times (even those defined by " +
				"non-exact" +
				" " + "frequencies).");
		this.newNodeSourceID = initialSourceNodeId;
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
		DateTime epochStart = getEpochStart();

		final Collection<Departure> departures = createDepartures(route, service, tripId, tripHeadsign, startTime,
				timePeriod, endTime, travelTime);
		if (!departures.isEmpty()) {
			RouteEdgeBuilder edgeBuilder = fetchRouteEdgeBuilder(origin, destination, route, distanceInM);
			departures.forEach(departure -> edgeBuilder.addDeparture(getDeparture(epochStart, departure.departureTime)
					, getDuration(departure.travelTime)));
		}
	}

	private int getDuration(ReadablePeriod travelTime) {
		return travelTime.toPeriod().toStandardSeconds().getSeconds();
	}

	private int getDeparture(DateTime epochStart, DateTime departureTime) {
		return (int) (departureTime.minus(epochStart.getMillis()).getMillis() / 1000L);
	}

	private DateTime getEpochStart() {
		if (epochStartWithTimeZone == null) {
			//check if there is only one time zone
			Set<DateTimeZone> timeZones = new HashSet<>(agenciesTimeZones.values());
			if (timeZones.size() > 1) {
				throw new IllegalStateException("The GTFS contains data from more time zones.");
			}

			//convert epoch start to format with time zone of the GTFS
			DateTimeZone timeZone = timeZones.iterator().next();
			this.epochStartWithTimeZone = this.epochStart.toDateTimeAtStartOfDay(timeZone);
		}
		return this.epochStartWithTimeZone;
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
			edgeBuilder = new RouteEdgeBuilder(getOrEstimateDistance(distanceInM, origin, destination));
			this.edgeBuilders.put(key, edgeBuilder);
		}
		assert distanceInM == null || Math.abs(distanceInM - edgeBuilder.getLength()) <= 1 : "Restored edge has " +
				"different length than the " +
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

	public TmpGraphBuilder<Node, TimeDependentEdge> getGraphBuilder(int initialTmpNodeId) {
		newNodeId = initialTmpNodeId;

		// Create the graph.
		final TmpGraphBuilder<Node, TimeDependentEdge> graphBuilder = new TmpGraphBuilder<>();
		final Map<String, StopNodeBuilder> stopNodes = new HashMap<>();
		final Map<RouteNodeKey, RouteNodeBuilder> routeNodes = new HashMap<>();

		for (Entry<EdgeKey, RouteEdgeBuilder> e : edgeBuilders.entrySet()) {
			EdgeKey key = e.getKey();
			RouteEdgeBuilder routeEdgeBuilder = e.getValue();
			//add only edges with some departures
			if (!routeEdgeBuilder.isEmpty()) {
				StopNodeBuilder fromStopNode = fetchStopNode(key.fromStop, stopNodes, graphBuilder);
				StopNodeBuilder toStopNode = fetchStopNode(key.toStop, stopNodes, graphBuilder);

				RouteNodeBuilder fromRouteNode = fetchRouteNode(fromStopNode, key.routeId, routeNodes, graphBuilder);
				RouteNodeBuilder toRouteNode = fetchRouteNode(toStopNode, key.routeId, routeNodes, graphBuilder);

				routeEdgeBuilder.setMode(routes.get(key.routeId).ptMode);
				routeEdgeBuilder.setTmpFromId(fromRouteNode.tmpId);
				routeEdgeBuilder.setTmpToId(toRouteNode.tmpId);

				graphBuilder.addEdge(routeEdgeBuilder);
			}
		}

		agenciesTimeZones.clear();
		agenciesToDetails.clear();
		servicesToDates.clear();
		servicesToDatesIncluded.clear();
		servicesToDatesExcluded.clear();
		stops.clear();
		routes.clear();
		return graphBuilder;
	}

	/**
	 * Construct the graph and clear the builder.
	 *
	 * @return The graph.
	 */
	public final Graph<Node, TimeDependentEdge> flushToGraph() {
		return getGraphBuilder(0).createGraph();
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
	private RouteNodeBuilder fetchRouteNode(StopNodeBuilder stopNode, String routeId,
											Map<RouteNodeKey, RouteNodeBuilder> routeNodes,
											TmpGraphBuilder<Node, TimeDependentEdge> graphBuilder) {
		RouteNodeKey key = new RouteNodeKey(stopNode.getStopId(), routeId);
		if (routeNodes.containsKey(key)) {
			return routeNodes.get(key);
		} else {
			RouteNodeBuilder routeNode = new RouteNodeBuilder(newNodeId++, newNodeSourceID++, stopNode.location,
					routeId, stopNode.tmpId);
			routeNodes.put(key, routeNode);
			graphBuilder.addNode(routeNode);

			//create edges between stop node and route node.
			InnerEdgeBuilder onEdge = new InnerEdgeBuilder(stopNode.tmpId, routeNode.tmpId, getOnDurationInS);
			InnerEdgeBuilder offEdge = new InnerEdgeBuilder(routeNode.tmpId, stopNode.tmpId, getOffDurationInS);

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
	private StopNodeBuilder fetchStopNode(String fromStop, Map<String, StopNodeBuilder> stopNodes,
										  TmpGraphBuilder<Node, TimeDependentEdge> graphBuilder) {
		if (stopNodes.containsKey(fromStop)) {
			return stopNodes.get(fromStop);
		} else {
			Stop stop = stops.get(fromStop);
			GPSLocation loc = stop.location;
			StopNodeBuilder stopNode = new StopNodeBuilder(newNodeId++, newNodeSourceID++, loc, stop.id, stop.name,
					stop.zoneId, stop.wheelchairBoarding);
			stopNodes.put(stop.id, stopNode);
			graphBuilder.addNode(stopNode);
			return stopNode;
		}
	}

	private void checkStop(String stopId) {
		if (!stops.containsKey(stopId)) throw new IllegalStateException("Stop " + stopId + " must be added first.");
	}

	private void checkRoute(String routeId) {
		if (!routes.containsKey(routeId)) throw new IllegalStateException("Route " + routeId + " must be added first" +
				".");
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
