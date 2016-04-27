package cz.agents.gtdgraphimporter.gtfs;

import com.google.common.collect.ImmutableMap;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsException;

import java.time.Duration;

/**
 * Interface of classes providing GTFS data loading.
 *
 * @author Radek Holy
 */
public interface GTFSDataLoader {

	/**
	 * A mapping from GTFS-specific exception type constants to flag providing information, whether given departure
	 * should be added (<code>true</code>) or removed (<code>false</code>).
	 */
	public static final ImmutableMap<Integer, Boolean> GTFS_EXCEPTION_TYPE_TO_AVAILABILITY_FLAG = ImmutableMap.of(1,
																												  true,
																												  2,
																												  false);

	/**
	 * Load public transport agencies information from the database.
	 *
	 * @param dataHandler
	 * 		Object handling iteratively loaded data.
	 *
	 * @throws GtfsException
	 * 		If the source cannot be parsed.
	 */
	public abstract void loadAgencies(final GTFSDataHandler dataHandler) throws GtfsException;

	/**
	 * Load public transport stops information from the database.
	 *
	 * @param dataHandler
	 * 		Object handling iteratively loaded data.
	 *
	 * @throws GtfsException
	 * 		If the source cannot be parsed.
	 */
	public abstract void loadStops(final GTFSDataHandler dataHandler) throws GtfsException;

	/**
	 * Load public transport routes information from the database.
	 *
	 * @param dataHandler
	 * 		Object handling iteratively loaded data.
	 *
	 * @throws GtfsException
	 * 		If the source cannot be parsed.
	 */
	public abstract void loadRoutes(final GTFSDataHandler dataHandler) throws GtfsException;

	/**
	 * Load public transport trip date intervals information from the database.
	 *
	 * @param dataHandler
	 * 		Object handling iteratively loaded data.
	 *
	 * @throws GtfsException
	 * 		If the source cannot be parsed.
	 */
	public abstract void loadDateIntervals(final GTFSDataHandler dataHandler) throws GtfsException;

	/**
	 * Load public transport trip dates information from the database.
	 *
	 * @param dataHandler
	 * 		Object handling iteratively loaded data.
	 *
	 * @throws GtfsException
	 * 		If the source cannot be parsed.
	 */
	public abstract void loadDates(final GTFSDataHandler dataHandler) throws GtfsException;

	/**
	 * Load public transport departures information from the database.
	 *
	 * @param dataHandler
	 * 		Object handling iteratively loaded data.
	 *
	 * @throws GtfsException
	 * 		If the source cannot be parsed.
	 */
	public abstract void loadDepartures(final GTFSDataHandler dataHandler) throws GtfsException;

	/**
	 * Parse duration in format {@code HH:mm:ss}.
	 * 
	 * @param s
	 * @return
	 */
	public static Duration parseDuration(String s) {
		String[] split = s.split(":");
		long seconds = Long.parseLong(split[0]) * 3600 + Long.parseLong(split[1]) * 60 + Long.parseLong(split[2]);
		return Duration.ofSeconds(seconds);
	}
}
