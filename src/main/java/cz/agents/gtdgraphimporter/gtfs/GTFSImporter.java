package cz.agents.gtdgraphimporter.gtfs;

import cz.agents.basestructures.Graph;
import cz.agents.basestructures.Node;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsException;
import cz.agents.gtdgraphimporter.structurebuilders.TmpGraphBuilder;
import cz.agents.multimodalstructures.edges.TimeDependentEdge;
import org.apache.log4j.Logger;

import java.time.LocalDate;

/**
 * This class converts GTFS data to appropriate format.
 *
 * @author Radek Holy
 */
public class GTFSImporter {

	/**
	 * Logging mechanism.
	 */
	private static final Logger LOGGER = Logger.getLogger(GTFSImporter.class);

	/**
	 * GTFS data loading mechanism.
	 */
	private final GTFSDataLoader gtfsDataLoader;

	/**
	 * Convert data to a graph.
	 *
	 * @return The graph.
	 *
	 * @throws GtfsException
	 * 		If the source cannot be parsed.
	 */
	public Graph<Node, TimeDependentEdge> importGraph(GTFSGraphBuilder builder) throws GtfsException {
		return readGtfsAndGetGraphBuilder(builder, 0).createGraph();
	}

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
	public TmpGraphBuilder<Node, TimeDependentEdge> readGtfsAndGetGraphBuilder(GTFSGraphBuilder builder,
																			   int initialTmpNodeId) throws
			GtfsException {
		gtfsDataLoader.loadAgencies(builder);
		LOGGER.trace("GTFS agencies loaded");

		gtfsDataLoader.loadStops(builder);
		LOGGER.trace("GTFS stops loaded");

		gtfsDataLoader.loadRoutes(builder);
		LOGGER.trace("GTFS routes added");

		gtfsDataLoader.loadDateIntervals(builder);
		LOGGER.trace("GTFS Trips added");

		gtfsDataLoader.loadDates(builder);
		LOGGER.trace("GTFS Trips exceptions added");

		gtfsDataLoader.loadDepartures(builder);
		LOGGER.trace("GTFS Trips exceptions added");

		return builder.getGraphBuilder(initialTmpNodeId);
	}

	/**
	 * Convert data to a graph.
	 *
	 * @return The graph.
	 *
	 * @throws GtfsException
	 * 		If the source cannot be parsed.
	 */
	public Graph<Node, TimeDependentEdge> importGraph(final long initialSourceNodeId, final short getOnDurationInS,
													  final short getOffDurationInS, LocalDate epochStart) throws GtfsException {
		return importGraph(new GTFSGraphBuilder(initialSourceNodeId, getOnDurationInS, getOffDurationInS, epochStart));
	}

	/**
	 * Convert data to a TmpGraphBuilder.
	 *
	 * @return The graph.
	 *
	 * @throws GtfsException
	 * 		If the source cannot be parsed.
	 */
	public TmpGraphBuilder<Node, TimeDependentEdge> importGtfsToGraphBuilder(long initialSourceNodeId,
																			 short getOnDurationInS,
																			 short getOffDurationInS,
																			 LocalDate epochStart,
																			 int initialTmpNodeId) throws
			GtfsException {
		return readGtfsAndGetGraphBuilder(new GTFSGraphBuilder(initialSourceNodeId, getOnDurationInS,
				getOffDurationInS, epochStart), initialTmpNodeId);
	}
}
