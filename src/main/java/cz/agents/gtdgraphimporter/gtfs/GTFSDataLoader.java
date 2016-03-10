package cz.agents.gtdgraphimporter.gtfs;

import com.google.common.collect.ImmutableMap;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsException;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 * Interface of classes providing GTFS data loading.
 * 
 * @author Radek Holy
 */
public interface GTFSDataLoader {

	/**
	 * A formatter for parsing GTFS arrival/departure times to {@link Period}.
	 */
	public static final PeriodFormatter GTFS_ARRIVAL_DEPARTURE_FORMATTER = new PeriodFormatterBuilder().appendHours()
			.appendSeparator(":").appendMinutes().appendSeparator(":").appendSeconds().toFormatter();

	/**
	 * A mapping from GTFS-specific exception type constants to flag providing information, whether given departure
	 * should be added (<code>true</code>) or removed (<code>false</code>).
	 */
	public static final ImmutableMap<Integer, Boolean> GTFS_EXCEPTION_TYPE_TO_AVAILABILITY_FLAG = ImmutableMap.of(1,
			true, 2, false);

	/**
	 * Load public transport agencies information from the database.
	 * 
	 * @param dataHandler
	 *            Object handling iteratively loaded data.
	 * @throws GtfsException
	 *             If the source cannot be parsed.
	 */
	public abstract void loadAgencies(final GTFSDataHandler dataHandler) throws GtfsException;

	/**
	 * Load public transport stops information from the database.
	 * 
	 * @param dataHandler
	 *            Object handling iteratively loaded data.
	 * @throws GtfsException
	 *             If the source cannot be parsed.
	 */
	public abstract void loadStops(final GTFSDataHandler dataHandler) throws GtfsException;

	/**
	 * Load public transport routes information from the database.
	 * 
	 * @param dataHandler
	 *            Object handling iteratively loaded data.
	 * @throws GtfsException
	 *             If the source cannot be parsed.
	 */
	public abstract void loadRoutes(final GTFSDataHandler dataHandler) throws GtfsException;

	/**
	 * Load public transport trip date intervals information from the database.
	 * 
	 * @param dataHandler
	 *            Object handling iteratively loaded data.
	 * @throws GtfsException
	 *             If the source cannot be parsed.
	 */
	public abstract void loadDateIntervals(final GTFSDataHandler dataHandler) throws GtfsException;

	/**
	 * Load public transport trip dates information from the database.
	 * 
	 * @param dataHandler
	 *            Object handling iteratively loaded data.
	 * @throws GtfsException
	 *             If the source cannot be parsed.
	 */
	public abstract void loadDates(final GTFSDataHandler dataHandler) throws GtfsException;

	/**
	 * Load public transport departures information from the database.
	 * 
	 * @param dataHandler
	 *            Object handling iteratively loaded data.
	 * @throws GtfsException
	 *             If the source cannot be parsed.
	 */
	public abstract void loadDepartures(final GTFSDataHandler dataHandler) throws GtfsException;
}
