package cz.agents.gtdgraphimporter.gtfs;

import cz.agents.basestructures.Graph;
import cz.agents.basestructures.Node;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsException;
import cz.agents.multimodalstructures.edges.TimeDependentEdge;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

/**
 * This class converts GTFS data to appropriate format.
 *
 * @author Radek Holy
 */
public class GTFSImporter {

	/**
	 * Logging mechanism.
	 */
	private static final Logger logger = Logger.getLogger(GTFSImporter.class);

	/**
	 * GTFS data loading mechanism.
	 */
	private final GTFSDataLoader gtfsDataLoader;

	/**
	 * Construct an instance.
	 *
	 * @param gtfsDataLoader
	 * 		Data loading mechanism.
	 */
	public GTFSImporter(final GTFSDataLoader gtfsDataLoader) {
		this.gtfsDataLoader = gtfsDataLoader;
	}

	/**
	 * Convert data to a graph.
	 *
	 * @return The graph.
	 *
	 * @throws GtfsException
	 * 		If the source cannot be parsed.
	 */
	public Graph<Node, TimeDependentEdge> importGraph(GTFSGraphBuilder builder) throws GtfsException {
		gtfsDataLoader.loadAgencies(builder);
		logger.trace("GTFS agencies loaded");

		gtfsDataLoader.loadStops(builder);
		logger.trace("GTFS stops loaded");

		gtfsDataLoader.loadRoutes(builder);
		logger.trace("GTFS routes added");

		gtfsDataLoader.loadDateIntervals(builder);
		logger.trace("GTFS Trips added");

		gtfsDataLoader.loadDates(builder);
		logger.trace("GTFS Trips exceptions added");

		gtfsDataLoader.loadDepartures(builder);
		logger.trace("GTFS Trips exceptions added");

		return builder.flushToGraph();
	}

	/**
	 * Convert data to a graph.
	 *
	 * @return The graph.
	 *
	 * @throws GtfsException
	 * 		If the source cannot be parsed.
	 */
	public Graph<Node, TimeDependentEdge> importGraph(final long initialNodeId, final short getOnDurationInS,
			final short getOffDurationInS, DateTime epochStart) throws GtfsException {
		return importGraph(new GTFSGraphBuilder(initialNodeId, getOnDurationInS, getOffDurationInS, epochStart));
	}
}
