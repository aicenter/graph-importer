package cz.agents.gtdgraphimporter.gtfs.impl;

import cz.agents.gtdgraphimporter.gtfs.GTFSDataHandler;
import cz.agents.gtdgraphimporter.gtfs.GTFSDatabaseLoader;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsParseException;
import cz.agents.gtdgraphimporter.gtfs.exceptions.GtfsSQLException;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.Date;

/**
 * This class provides loading of GTFS data from a database while skipping data mistakes.
 * 
 * @author Radek Holy
 */
public final class GTFSDatabaseLoaderPermissiveImpl extends GTFSDatabaseLoader {

	/**
	 * Logging mechanism.
	 */
	private static final Logger logger = Logger.getLogger(GTFSDatabaseLoaderPermissiveImpl.class);

	/**
	 * Construct a new instance.
	 * 
	 * @param connection
	 *            Connection to a database.
	 * @param epsgSrid
	 *            EPSG SRID of coordinates projection.
	 * @param gtfsUnitToMetersMultiplier
	 *            A number used to multiply traveled distance specified in GTFS data to convert it to meters.
	 * @param sqlResultDownloadSize
	 *            Max. size (number of rows) of one batch locally stored (cached) while downloading SQL results.
	 */
	public GTFSDatabaseLoaderPermissiveImpl(final Connection connection, final int epsgSrid,
			final double gtfsUnitToMetersMultiplier, final int sqlResultDownloadSize) {
		super(connection, epsgSrid, gtfsUnitToMetersMultiplier, sqlResultDownloadSize);
	}


	/**
	 * Construct a new instance.
	 * 
	 * @param connection
	 *            Connection to a database.
	 * @param epsgSrid
	 *            EPSG SRID of coordinates projection.
	 * @param gtfsUnitToMetersMultiplier
	 *            A number used to multiply traveled distance specified in GTFS data to convert it to meters.
	 * @param sqlResultDownloadSize
	 *            Max. size (number of rows) of one batch locally stored (cached) while downloading SQL results.
	 * @param pruneBeforeDate
	 *            Min. allowed loaded date (inclusive). This setting should be used rarely to accelerate GTFS loading
	 *            time. In other cases, a GTFS graph filtering mechanism should be used.
	 * @param pruneAfterDate
	 *            Max. allowed loaded date (exclusive). This setting should be used rarely to accelerate GTFS loading
	 *            time. In other cases, a GTFS graph filtering mechanism should be used.
	 */
	public GTFSDatabaseLoaderPermissiveImpl(final Connection connection, final int epsgSrid,
			final double gtfsUnitToMetersMultiplier, final int sqlResultDownloadSize, final Date pruneBeforeDate,
			final Date pruneAfterDate) {
		super(connection, epsgSrid, gtfsUnitToMetersMultiplier, sqlResultDownloadSize, pruneBeforeDate, pruneAfterDate);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void loadAgencies(final GTFSDataHandler dataHandler) throws GtfsSQLException {
		try {
			super.loadAgencies(dataHandler);
		} catch (final GtfsParseException e) {
			throw new IllegalStateException("no parse exception should be propagated", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void loadStops(final GTFSDataHandler dataHandler) throws GtfsSQLException {
		try {
			super.loadStops(dataHandler);
		} catch (final GtfsParseException e) {
			throw new IllegalStateException("no parse exception should be propagated", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void loadRoutes(final GTFSDataHandler dataHandler) throws GtfsSQLException {
		try {
			super.loadRoutes(dataHandler);
		} catch (final GtfsParseException e) {
			throw new IllegalStateException("no parse exception should be propagated", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void loadDateIntervals(final GTFSDataHandler dataHandler) throws GtfsSQLException {
		try {
			super.loadDateIntervals(dataHandler);
		} catch (final GtfsParseException e) {
			throw new IllegalStateException("no parse exception should be propagated", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void loadDates(final GTFSDataHandler dataHandler) throws GtfsSQLException {
		try {
			super.loadDates(dataHandler);
		} catch (final GtfsParseException e) {
			throw new IllegalStateException("no parse exception should be propagated", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void loadDepartures(final GTFSDataHandler dataHandler) throws GtfsSQLException {
		try {
			super.loadDepartures(dataHandler);
		} catch (final GtfsParseException e) {
			throw new IllegalStateException("no parse exception should be propagated", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void handleParseException(final String message, final Throwable exception)
			throws GtfsParseException {

		// TODO count these exceptions instead writing them in the console
		if (!exception.getMessage().equals("no dates set for given service")) {
			logger.warn("Skipped data mistake:", exception);
		}
	}
}
